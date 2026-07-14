package com.parazit.panel.application.telegram;

import com.parazit.panel.application.port.in.telegram.ProcessTelegramUpdateUseCase;
import com.parazit.panel.application.telegram.handler.TelegramCallbackHandler;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.menu.TelegramMainMenuAction;
import com.parazit.panel.application.telegram.menu.TelegramMainMenuHandler;
import com.parazit.panel.application.telegram.menu.TelegramMainMenuTextRouter;
import com.parazit.panel.application.telegram.menu.TelegramMenuMetrics;
import com.parazit.panel.application.telegram.service.TelegramServiceSearchHandler;
import com.parazit.panel.application.telegram.wallet.TelegramWalletHandler;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramCommand;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramUpdate;
import com.parazit.panel.application.telegram.model.TelegramUpdateType;
import java.util.Optional;
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
    private final TelegramMainMenuTextRouter mainMenuTextRouter;
    private final TelegramMainMenuHandler mainMenuHandler;
    private final TelegramMenuMetrics metrics;
    private final TelegramServiceSearchHandler serviceSearchHandler;
    private final TelegramWalletHandler walletHandler;

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
            TelegramMessageCatalog catalog,
            TelegramMainMenuTextRouter mainMenuTextRouter,
            TelegramMainMenuHandler mainMenuHandler,
            TelegramMenuMetrics metrics,
            TelegramServiceSearchHandler serviceSearchHandler,
            TelegramWalletHandler walletHandler
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
        this.mainMenuTextRouter = Objects.requireNonNull(mainMenuTextRouter, "mainMenuTextRouter must not be null");
        this.mainMenuHandler = Objects.requireNonNull(mainMenuHandler, "mainMenuHandler must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        this.serviceSearchHandler = Objects.requireNonNull(serviceSearchHandler, "serviceSearchHandler must not be null");
        this.walletHandler = Objects.requireNonNull(walletHandler, "walletHandler must not be null");
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
                update.actor().firstName(),
                update.callbackQuery() == null ? null : update.callbackQuery().sourceMessageId(),
                update.callbackQuery() == null ? null : update.callbackQuery().callbackQueryId(),
                update.receivedAt()
        );
        if (update.type() == TelegramUpdateType.CALLBACK_QUERY) {
            return callbackHandler.handle(context, update.callbackQuery().data());
        }
        if (update.type() == TelegramUpdateType.MESSAGE) {
            String text = update.message() == null ? "" : update.message().text();
            TelegramCommand command = commandParser.parse(text);
            if (command != TelegramCommand.UNKNOWN) {
                if (command == TelegramCommand.MENU || command == TelegramCommand.CANCEL) {
                    serviceSearchHandler.clear(context.telegramUserId());
                    walletHandler.clearTopUpSession(context.telegramUserId());
                }
                return commandRouter.route(command, context);
            }
            Optional<TelegramResponsePlan> topUpPlan = walletHandler.handleAmountIfAwaiting(context, text);
            if (topUpPlan.isPresent()) {
                return topUpPlan.get();
            }
            Optional<TelegramResponsePlan> searchPlan = serviceSearchHandler.handleIfAwaiting(context, text);
            if (searchPlan.isPresent()) {
                return searchPlan.get();
            }
            Optional<TelegramMainMenuAction> action = mainMenuTextRouter.route(context.language(), text);
            if (action.isPresent()) {
                return mainMenuHandler.handle(context, action.get());
            }
            metrics.recordUnknownMessage(context.chatType().name());
            return mainMenuHandler.showHome(context, "telegram.error.unknown_message", "message:unknown");
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
                update.actor().firstName(),
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
