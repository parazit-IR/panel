package com.parazit.panel.application.telegram;

import com.parazit.panel.application.port.in.telegram.ProcessTelegramUpdateUseCase;
import com.parazit.panel.application.telegram.handler.TelegramCallbackHandler;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramCommand;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramUpdate;
import com.parazit.panel.application.telegram.model.TelegramUpdateType;
import com.parazit.panel.application.user.result.RegisterUserResult;
import java.util.Objects;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TelegramUpdateProcessor implements ProcessTelegramUpdateUseCase {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateProcessor.class);

    private final ClaimTelegramUpdateTransaction claimTransaction;
    private final CompleteTelegramUpdateTransaction completeTransaction;
    private final FailTelegramUpdateTransaction failTransaction;
    private final RegisterTelegramActorService registerTelegramActorService;
    private final TelegramCommandParser commandParser;
    private final TelegramCommandRouter commandRouter;
    private final TelegramCallbackHandler callbackHandler;
    private final TelegramResponseExecutor responseExecutor;
    private final TelegramFailureClassifier failureClassifier;
    private final TelegramMessageCatalog catalog;

    public TelegramUpdateProcessor(
            ClaimTelegramUpdateTransaction claimTransaction,
            CompleteTelegramUpdateTransaction completeTransaction,
            FailTelegramUpdateTransaction failTransaction,
            RegisterTelegramActorService registerTelegramActorService,
            TelegramCommandParser commandParser,
            TelegramCommandRouter commandRouter,
            TelegramCallbackHandler callbackHandler,
            TelegramResponseExecutor responseExecutor,
            TelegramFailureClassifier failureClassifier,
            TelegramMessageCatalog catalog
    ) {
        this.claimTransaction = Objects.requireNonNull(claimTransaction, "claimTransaction must not be null");
        this.completeTransaction = Objects.requireNonNull(completeTransaction, "completeTransaction must not be null");
        this.failTransaction = Objects.requireNonNull(failTransaction, "failTransaction must not be null");
        this.registerTelegramActorService = Objects.requireNonNull(registerTelegramActorService, "registerTelegramActorService must not be null");
        this.commandParser = Objects.requireNonNull(commandParser, "commandParser must not be null");
        this.commandRouter = Objects.requireNonNull(commandRouter, "commandRouter must not be null");
        this.callbackHandler = Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
        this.responseExecutor = Objects.requireNonNull(responseExecutor, "responseExecutor must not be null");
        this.failureClassifier = Objects.requireNonNull(failureClassifier, "failureClassifier must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
    }

    @Override
    public void process(TelegramUpdate update) {
        Objects.requireNonNull(update, "update must not be null");
        ClaimTelegramUpdateResult claim = claimTransaction.claim(update.updateId(), handlerKey(update), update.receivedAt());
        if (claim != ClaimTelegramUpdateResult.CLAIMED) {
            log.atDebug()
                    .addKeyValue("updateId", update.updateId())
                    .addKeyValue("claim", claim)
                    .log("Telegram update skipped by idempotency state");
            return;
        }
        try {
            TelegramResponsePlan plan = buildPlan(update);
            String fingerprint = responseExecutor.execute(plan, update.updateId());
            completeTransaction.complete(update.updateId(), fingerprint);
            log.atInfo()
                    .addKeyValue("updateId", update.updateId())
                    .addKeyValue("handlerKey", plan.handlerKey())
                    .log("Telegram update processed");
        } catch (RuntimeException exception) {
            TelegramFailureClassification classification = failureClassifier.classify(exception);
            if (classification == TelegramFailureClassification.NON_RETRYABLE) {
                completeTransaction.complete(update.updateId(), "ignored-non-retryable");
                log.atWarn()
                        .addKeyValue("updateId", update.updateId())
                        .addKeyValue("failureClass", classification)
                        .log("Telegram update completed after non-retryable failure");
                return;
            }
            String code = classification == TelegramFailureClassification.UNKNOWN
                    ? "TELEGRAM_SEND_UNKNOWN"
                    : "TELEGRAM_RETRYABLE_FAILURE";
            failTransaction.fail(update.updateId(), code, safeFailure(exception));
            log.atWarn()
                    .addKeyValue("updateId", update.updateId())
                    .addKeyValue("failureClass", classification)
                    .log("Telegram update failed");
            throw exception;
        }
    }

    private TelegramResponsePlan buildPlan(TelegramUpdate update) {
        if (update.actor() == null || update.actor().bot()) {
            return TelegramResponsePlan.empty("ignored:bot-or-missing-actor");
        }
        if (!update.privateChat()) {
            return privateOnlyPlan(update);
        }
        RegisterUserResult registered = registerTelegramActorService.registerOrRefresh(update.actor());
        TelegramInteractionContext context = new TelegramInteractionContext(
                update.updateId(),
                update.actor().telegramUserId(),
                update.chat().chatId(),
                update.chat().type(),
                registered.language().name(),
                update.callbackQuery() == null ? null : update.callbackQuery().sourceMessageId(),
                update.callbackQuery() == null ? null : update.callbackQuery().callbackQueryId(),
                update.receivedAt()
        );
        if (update.type() == TelegramUpdateType.MESSAGE) {
            TelegramCommand command = commandParser.parse(update.message() == null ? "" : update.message().text());
            return commandRouter.route(command, context);
        }
        if (update.type() == TelegramUpdateType.CALLBACK_QUERY) {
            return callbackHandler.handle(context, update.callbackQuery().data());
        }
        return TelegramResponsePlan.empty("ignored:unsupported");
    }

    private TelegramResponsePlan privateOnlyPlan(TelegramUpdate update) {
        if (update.chat() == null) {
            return TelegramResponsePlan.empty("ignored:non-private");
        }
        TelegramInteractionContext context = new TelegramInteractionContext(
                update.updateId(),
                update.actor().telegramUserId(),
                update.chat().chatId(),
                update.chat().type(),
                update.actor().languageCode(),
                null,
                update.callbackQuery() == null ? null : update.callbackQuery().callbackQueryId(),
                update.receivedAt()
        );
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(
                new SendTelegramMessageCommand(
                        update.chat().chatId(),
                        catalog.text(context.language(), "private_only"),
                        TelegramParseMode.NONE,
                        TelegramInlineKeyboard.empty(),
                        true,
                        null
                ),
                false
        )), "ignored:non-private");
    }

    private static String handlerKey(TelegramUpdate update) {
        return switch (update.type()) {
            case MESSAGE -> "message";
            case CALLBACK_QUERY -> "callback";
            case UNSUPPORTED -> "unsupported";
        };
    }

    private static String safeFailure(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String sanitized = message.replace('\r', ' ').replace('\n', ' ');
        return sanitized.length() <= 300 ? sanitized : sanitized.substring(0, 300);
    }
}
