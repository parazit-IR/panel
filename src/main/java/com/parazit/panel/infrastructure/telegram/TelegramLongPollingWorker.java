package com.parazit.panel.infrastructure.telegram;

import com.parazit.panel.application.port.in.telegram.ProcessTelegramUpdateUseCase;
import com.parazit.panel.application.port.out.telegram.TelegramBotClient;
import com.parazit.panel.application.telegram.TelegramPollingStateTransaction;
import com.parazit.panel.application.telegram.command.GetTelegramUpdatesCommand;
import com.parazit.panel.application.telegram.model.TelegramUpdate;
import com.parazit.panel.config.properties.TelegramBotProperties;
import com.parazit.panel.config.properties.TelegramUpdateMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TelegramLongPollingWorker {

    private static final Logger log = LoggerFactory.getLogger(TelegramLongPollingWorker.class);
    private static final long ADVISORY_LOCK_KEY = 4_104_035L;

    private final TelegramBotProperties properties;
    private final TelegramBotClient client;
    private final ProcessTelegramUpdateUseCase processor;
    private final TelegramPollingStateTransaction pollingStateTransaction;
    private final JdbcTemplate jdbcTemplate;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TelegramLongPollingWorker(
            TelegramBotProperties properties,
            TelegramBotClient client,
            ProcessTelegramUpdateUseCase processor,
            TelegramPollingStateTransaction pollingStateTransaction,
            JdbcTemplate jdbcTemplate
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.processor = Objects.requireNonNull(processor, "processor must not be null");
        this.pollingStateTransaction = Objects.requireNonNull(pollingStateTransaction, "pollingStateTransaction must not be null");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    @Scheduled(fixedDelayString = "${app.telegram.bot.polling-backoff:PT2S}")
    public void poll() {
        if (!properties.enabled() || properties.updateMode() != TelegramUpdateMode.LONG_POLLING) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        boolean locked = false;
        try {
            locked = Boolean.TRUE.equals(jdbcTemplate.queryForObject("select pg_try_advisory_lock(?)", Boolean.class, ADVISORY_LOCK_KEY));
            if (!locked) {
                log.debug("Telegram long polling skipped because another poller owns the advisory lock");
                return;
            }
            long offset = pollingStateTransaction.currentOffset();
            List<TelegramUpdate> updates = client.getUpdates(new GetTelegramUpdatesCommand(
                            offset,
                            properties.updateBatchSize(),
                            properties.pollingTimeout(),
                            properties.allowedUpdates()
                    ))
                    .updates()
                    .stream()
                    .sorted(Comparator.comparingLong(TelegramUpdate::updateId))
                    .toList();
            for (TelegramUpdate update : updates) {
                processor.process(update);
                pollingStateTransaction.advanceAfter(update.updateId());
            }
        } catch (RuntimeException exception) {
            log.atWarn()
                    .addKeyValue("botIdentity", properties.botIdentity())
                    .log("Telegram long polling cycle failed");
        } finally {
            if (locked) {
                jdbcTemplate.queryForObject("select pg_advisory_unlock(?)", Boolean.class, ADVISORY_LOCK_KEY);
            }
            running.set(false);
        }
    }
}
