package com.parazit.panel.application.wallet;

import com.parazit.panel.application.port.in.wallet.GetOrCreateWalletUseCase;
import com.parazit.panel.application.port.in.wallet.ListWalletTransactionsUseCase;
import com.parazit.panel.application.wallet.command.ListWalletTransactionsCommand;
import com.parazit.panel.application.wallet.result.WalletCreationResult;
import com.parazit.panel.application.wallet.result.WalletTransactionPageResult;
import com.parazit.panel.application.wallet.result.WalletTransactionSummaryResult;
import com.parazit.panel.config.properties.WalletProperties;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.wallet.WalletTransaction;
import com.parazit.panel.domain.wallet.repository.WalletTransactionRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListWalletTransactionsService implements ListWalletTransactionsUseCase {

    private final UserRepository userRepository;
    private final GetOrCreateWalletUseCase getOrCreateWalletUseCase;
    private final WalletTransactionRepository transactionRepository;
    private final WalletProperties properties;

    public ListWalletTransactionsService(
            UserRepository userRepository,
            GetOrCreateWalletUseCase getOrCreateWalletUseCase,
            WalletTransactionRepository transactionRepository,
            WalletProperties properties
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.getOrCreateWalletUseCase = Objects.requireNonNull(getOrCreateWalletUseCase, "getOrCreateWalletUseCase must not be null");
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    @Transactional
    public WalletTransactionPageResult list(ListWalletTransactionsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new IllegalArgumentException("customer account not found"));
        WalletCreationResult wallet = getOrCreateWalletUseCase.getOrCreate(user.getId());
        int size = Math.max(1, Math.min(command.size(), properties.maxHistoryPageSize()));
        int page = Math.max(0, command.page());
        long count = transactionRepository.countByWalletId(wallet.walletId());
        List<WalletTransactionSummaryResult> items = transactionRepository
                .findAllByWalletIdOrderByOccurredAtDesc(wallet.walletId(), page * size, size)
                .stream()
                .map(this::toSummary)
                .toList();
        return new WalletTransactionPageResult(items, page, size, page > 0, ((long) page + 1L) * size < count, count);
    }

    private WalletTransactionSummaryResult toSummary(WalletTransaction transaction) {
        return new WalletTransactionSummaryResult(
                transaction.getId(),
                transaction.getDirection(),
                transaction.getType(),
                transaction.amount(),
                transaction.balanceAfter(),
                transaction.getDescriptionCode(),
                transaction.getOccurredAt()
        );
    }
}
