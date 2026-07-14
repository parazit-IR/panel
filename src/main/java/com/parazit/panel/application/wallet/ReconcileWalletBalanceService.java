package com.parazit.panel.application.wallet;

import com.parazit.panel.application.port.in.wallet.ReconcileWalletBalanceUseCase;
import com.parazit.panel.application.wallet.command.ReconcileWalletBalanceCommand;
import com.parazit.panel.application.wallet.result.WalletReconciliationResult;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.wallet.Wallet;
import com.parazit.panel.domain.wallet.repository.WalletRepository;
import com.parazit.panel.domain.wallet.repository.WalletTransactionRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconcileWalletBalanceService implements ReconcileWalletBalanceUseCase {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public ReconcileWalletBalanceService(
            WalletRepository walletRepository,
            WalletTransactionRepository transactionRepository
    ) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository must not be null");
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public WalletReconciliationResult reconcile(ReconcileWalletBalanceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Wallet wallet = walletRepository.findById(command.walletId())
                .orElseThrow(() -> new IllegalArgumentException("wallet not found"));
        long calculated = Math.subtractExact(
                transactionRepository.sumCreditsByWalletId(wallet.getId()),
                transactionRepository.sumDebitsByWalletId(wallet.getId())
        );
        Money ledgerBalance = new Money(calculated, wallet.currencyCode());
        return new WalletReconciliationResult(
                wallet.getId(),
                wallet.balance(),
                ledgerBalance,
                wallet.getBalanceAmount() == calculated,
                transactionRepository.countByWalletId(wallet.getId())
        );
    }
}
