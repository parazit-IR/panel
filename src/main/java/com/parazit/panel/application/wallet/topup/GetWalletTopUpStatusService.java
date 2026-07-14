package com.parazit.panel.application.wallet.topup;

import com.parazit.panel.application.port.in.wallet.topup.GetWalletTopUpStatusUseCase;
import com.parazit.panel.application.wallet.topup.command.GetWalletTopUpStatusCommand;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpStatusResult;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.wallet.repository.WalletTransactionRepository;
import com.parazit.panel.domain.wallet.topup.WalletTopUpRequest;
import com.parazit.panel.domain.wallet.topup.repository.WalletTopUpRequestRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetWalletTopUpStatusService implements GetWalletTopUpStatusUseCase {

    private final UserRepository userRepository;
    private final WalletTopUpRequestRepository requestRepository;
    private final PaymentRepository paymentRepository;
    private final WalletTransactionRepository transactionRepository;

    public GetWalletTopUpStatusService(
            UserRepository userRepository,
            WalletTopUpRequestRepository requestRepository,
            PaymentRepository paymentRepository,
            WalletTransactionRepository transactionRepository
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.requestRepository = Objects.requireNonNull(requestRepository, "requestRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public WalletTopUpStatusResult get(GetWalletTopUpStatusCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new WalletTopUpException("user not found"));
        WalletTopUpRequest request = requestRepository.findById(command.topUpRequestId())
                .orElseThrow(() -> new WalletTopUpException("wallet top-up request not found"));
        if (!request.getUserId().equals(user.getId())) {
            throw new WalletTopUpException("wallet top-up request not found");
        }
        PaymentStatus paymentStatus = null;
        PaymentMethod paymentMethod = null;
        if (request.getPaymentId() != null) {
            Payment payment = paymentRepository.findById(request.getPaymentId())
                    .orElseThrow(() -> new WalletTopUpException("wallet top-up payment not found"));
            paymentStatus = payment.getStatus();
            paymentMethod = payment.getMethod();
        }
        Money balanceAfter = transactionRepository
                .findByWalletIdAndIdempotencyKey(request.getWalletId(), "wallet-top-up:" + request.getId())
                .map(transaction -> transaction.balanceAfter())
                .orElse(null);
        return new WalletTopUpStatusResult(
                request.getId(),
                request.requestedMoney(),
                request.getStatus(),
                paymentStatus,
                paymentMethod,
                request.getExpiresAt(),
                request.getCreditedAt(),
                balanceAfter
        );
    }
}
