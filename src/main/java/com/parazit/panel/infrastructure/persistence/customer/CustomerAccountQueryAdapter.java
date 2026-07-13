package com.parazit.panel.infrastructure.persistence.customer;

import com.parazit.panel.application.customer.result.CustomerAccountProjection;
import com.parazit.panel.application.customer.result.CustomerAccountStatistics;
import com.parazit.panel.application.port.out.customer.CustomerAccountQueryPort;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.subscription.SubscriptionStatus;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.infrastructure.persistence.payment.SpringDataPaymentRepository;
import com.parazit.panel.infrastructure.persistence.subscription.SpringDataSubscriptionRepository;
import com.parazit.panel.infrastructure.persistence.user.SpringDataUserRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class CustomerAccountQueryAdapter implements CustomerAccountQueryPort {

    private final SpringDataUserRepository userRepository;
    private final SpringDataSubscriptionRepository subscriptionRepository;
    private final SpringDataPaymentRepository paymentRepository;

    public CustomerAccountQueryAdapter(
            SpringDataUserRepository userRepository,
            SpringDataSubscriptionRepository subscriptionRepository,
            SpringDataPaymentRepository paymentRepository
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
    }

    @Override
    public Optional<CustomerAccountProjection> findByTelegramUserId(long telegramUserId) {
        return userRepository.findByTelegramUserId(telegramUserId).map(this::toProjection);
    }

    @Override
    public CustomerAccountStatistics loadStatistics(UUID userId) {
        UUID requiredUserId = Objects.requireNonNull(userId, "userId must not be null");
        long total = subscriptionRepository.countByUserId(requiredUserId);
        long active = subscriptionRepository.countByUserIdAndStatus(requiredUserId, SubscriptionStatus.ACTIVE);
        long expired = subscriptionRepository.countByUserIdAndStatus(requiredUserId, SubscriptionStatus.EXPIRED);
        long paid = paymentRepository.countByUserIdAndStatusIn(requiredUserId, List.of(PaymentStatus.APPROVED));
        long pending = paymentRepository.countByUserIdAndStatusIn(requiredUserId, List.of(
                PaymentStatus.CREATED,
                PaymentStatus.WAITING_FOR_PAYMENT,
                PaymentStatus.RECEIPT_SUBMITTED,
                PaymentStatus.WAITING_FOR_REVIEW,
                PaymentStatus.PROCESSING,
                PaymentStatus.UNKNOWN
        ));
        return new CustomerAccountStatistics(total, active, expired, paid, pending);
    }

    private CustomerAccountProjection toProjection(User user) {
        String displayName = (user.getFirstName() + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return new CustomerAccountProjection(
                user.getId(),
                user.getTelegramUserId(),
                displayName,
                user.getUsername(),
                user.getCreatedAt(),
                user.getLanguage().name(),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }
}
