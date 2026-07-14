package com.parazit.panel.application.wallet.topup;

import com.parazit.panel.application.port.in.wallet.CreditWalletUseCase;
import com.parazit.panel.application.port.in.wallet.topup.HandleApprovedWalletTopUpUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.wallet.command.CreditWalletCommand;
import com.parazit.panel.application.wallet.result.WalletOperationResult;
import com.parazit.panel.application.wallet.topup.command.HandleApprovedWalletTopUpCommand;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpApprovalResult;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.wallet.WalletOperationOutcome;
import com.parazit.panel.domain.wallet.WalletTransactionType;
import com.parazit.panel.domain.wallet.topup.WalletTopUpRequest;
import com.parazit.panel.domain.wallet.topup.repository.WalletTopUpRequestRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HandleApprovedWalletTopUpService implements HandleApprovedWalletTopUpUseCase {

    public static final String REFERENCE_TYPE = "WALLET_TOP_UP";
    public static final String DESCRIPTION_CODE = "wallet.topup";

    private final PaymentRepository paymentRepository;
    private final WalletTopUpRequestRepository requestRepository;
    private final CreditWalletUseCase creditWalletUseCase;
    private final SystemClockPort clock;

    public HandleApprovedWalletTopUpService(
            PaymentRepository paymentRepository,
            WalletTopUpRequestRepository requestRepository,
            CreditWalletUseCase creditWalletUseCase,
            SystemClockPort clock
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.requestRepository = Objects.requireNonNull(requestRepository, "requestRepository must not be null");
        this.creditWalletUseCase = Objects.requireNonNull(creditWalletUseCase, "creditWalletUseCase must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public WalletTopUpApprovalResult handle(HandleApprovedWalletTopUpCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Payment payment = paymentRepository.findByIdForUpdate(command.paymentId())
                .orElseThrow(() -> new WalletTopUpException("wallet top-up payment not found"));
        WalletTopUpRequest request = requestRepository.findByIdForUpdate(command.topUpRequestId())
                .orElseThrow(() -> new WalletTopUpException("wallet top-up request not found"));
        validate(payment, request);

        request.markPaymentApproved(payment.getApprovedAt() == null ? clock.now() : payment.getApprovedAt());
        WalletOperationResult walletResult = creditWalletUseCase.credit(new CreditWalletCommand(
                request.getUserId(),
                request.requestedMoney(),
                WalletTransactionType.TOP_UP,
                REFERENCE_TYPE,
                request.getId(),
                idempotencyKey(request),
                DESCRIPTION_CODE
        ));
        if (walletResult.outcome() == WalletOperationOutcome.APPLIED
                || walletResult.outcome() == WalletOperationOutcome.REPLAYED) {
            request.markCredited(walletResult.occurredAt());
            requestRepository.save(request);
            return new WalletTopUpApprovalResult(
                    request.getId(),
                    payment.getId(),
                    walletResult.transactionId(),
                    walletResult.amount(),
                    walletResult.balanceAfter(),
                    walletResult.replayed() ? WalletTopUpApprovalOutcome.ALREADY_CREDITED : WalletTopUpApprovalOutcome.CREDITED,
                    walletResult.replayed(),
                    walletResult.occurredAt()
            );
        }

        request.fail(walletResult.outcome().name());
        requestRepository.save(request);
        return new WalletTopUpApprovalResult(
                request.getId(),
                payment.getId(),
                walletResult.transactionId(),
                walletResult.amount(),
                walletResult.balanceAfter(),
                WalletTopUpApprovalOutcome.REJECTED,
                false,
                walletResult.occurredAt()
        );
    }

    private void validate(Payment payment, WalletTopUpRequest request) {
        if (!payment.targetsWalletTopUp()) {
            throw new WalletTopUpException("payment is not a wallet top-up");
        }
        if (!request.getId().equals(payment.getWalletTopUpRequestId()) || !payment.getUserId().equals(request.getUserId())) {
            throw new WalletTopUpException("wallet top-up payment linkage is invalid");
        }
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new WalletTopUpException("payment is not approved");
        }
        if (payment.getBaseAmount() != request.getRequestedAmount()
                || !payment.getCurrency().equalsIgnoreCase(request.getCurrency())) {
            throw new WalletTopUpException("wallet top-up amount mismatch");
        }
    }

    private static String idempotencyKey(WalletTopUpRequest request) {
        return "wallet-top-up:" + request.getId();
    }
}
