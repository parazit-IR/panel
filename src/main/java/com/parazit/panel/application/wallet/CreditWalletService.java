package com.parazit.panel.application.wallet;

import com.parazit.panel.application.port.in.wallet.CreditWalletUseCase;
import com.parazit.panel.application.port.in.wallet.GetOrCreateWalletUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.wallet.command.CreditWalletCommand;
import com.parazit.panel.application.wallet.result.WalletOperationResult;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.wallet.Wallet;
import com.parazit.panel.domain.wallet.WalletClosedException;
import com.parazit.panel.domain.wallet.WalletLockedException;
import com.parazit.panel.domain.wallet.WalletOperationOutcome;
import com.parazit.panel.domain.wallet.WalletTransaction;
import com.parazit.panel.domain.wallet.WalletTransactionDirection;
import com.parazit.panel.domain.wallet.repository.WalletRepository;
import com.parazit.panel.domain.wallet.repository.WalletTransactionRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditWalletService implements CreditWalletUseCase {

    private final GetOrCreateWalletUseCase getOrCreateWalletUseCase;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final SystemClockPort clock;

    public CreditWalletService(
            GetOrCreateWalletUseCase getOrCreateWalletUseCase,
            WalletRepository walletRepository,
            WalletTransactionRepository transactionRepository,
            SystemClockPort clock
    ) {
        this.getOrCreateWalletUseCase = Objects.requireNonNull(getOrCreateWalletUseCase, "getOrCreateWalletUseCase must not be null");
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository must not be null");
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public WalletOperationResult credit(CreditWalletCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (command.amount().amount() <= 0) {
            throw new com.parazit.panel.domain.wallet.InvalidWalletAmountException();
        }
        getOrCreateWalletUseCase.getOrCreate(command.userId());
        Wallet wallet = walletRepository.findByUserIdForUpdate(command.userId())
                .orElseThrow(() -> new IllegalStateException("wallet not found"));
        return transactionRepository.findByWalletIdAndIdempotencyKey(wallet.getId(), command.idempotencyKey())
                .map(existing -> replayOrConflict(wallet, existing, command))
                .orElseGet(() -> apply(wallet, command));
    }

    private WalletOperationResult apply(Wallet wallet, CreditWalletCommand command) {
        Money before = wallet.balance();
        Instant now = clock.now();
        try {
            Money after = wallet.credit(command.amount());
            WalletTransaction transaction = WalletTransaction.post(
                    wallet.getId(),
                    wallet.getUserId(),
                    command.type(),
                    WalletTransactionDirection.CREDIT,
                    command.amount(),
                    before,
                    after,
                    command.referenceType(),
                    command.referenceId(),
                    command.idempotencyKey(),
                    command.descriptionCode(),
                    now
            );
            transactionRepository.save(transaction);
            walletRepository.save(wallet);
            return result(transaction, before, after, command.amount(), WalletOperationOutcome.APPLIED, false);
        } catch (WalletLockedException exception) {
            return rejected(wallet, before, command.amount(), WalletOperationOutcome.REJECTED_WALLET_LOCKED, WalletTransactionDirection.CREDIT, now);
        } catch (WalletClosedException exception) {
            return rejected(wallet, before, command.amount(), WalletOperationOutcome.REJECTED_WALLET_CLOSED, WalletTransactionDirection.CREDIT, now);
        }
    }

    private WalletOperationResult replayOrConflict(Wallet wallet, WalletTransaction existing, CreditWalletCommand command) {
        if (!existing.semanticallyMatches(command.type(), WalletTransactionDirection.CREDIT, command.amount(),
                command.referenceType(), command.referenceId(), command.descriptionCode())) {
            return rejected(wallet, wallet.balance(), command.amount(), WalletOperationOutcome.REJECTED_IDEMPOTENCY_CONFLICT,
                    WalletTransactionDirection.CREDIT, clock.now());
        }
        return result(existing, existing.balanceBefore(), existing.balanceAfter(), existing.amount(), WalletOperationOutcome.REPLAYED, true);
    }

    private static WalletOperationResult result(WalletTransaction transaction, Money before, Money after, Money amount,
                                                WalletOperationOutcome outcome, boolean replayed) {
        return new WalletOperationResult(
                transaction.getWalletId(),
                transaction.getId(),
                before,
                after,
                amount,
                transaction.getDirection(),
                outcome,
                replayed,
                transaction.getOccurredAt()
        );
    }

    private static WalletOperationResult rejected(Wallet wallet, Money before, Money amount, WalletOperationOutcome outcome,
                                                  WalletTransactionDirection direction, Instant occurredAt) {
        return new WalletOperationResult(wallet.getId(), null, before, before, amount, direction, outcome, false, occurredAt);
    }
}
