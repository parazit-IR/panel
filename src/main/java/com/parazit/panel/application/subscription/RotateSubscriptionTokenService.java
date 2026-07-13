package com.parazit.panel.application.subscription;

import com.parazit.panel.application.port.in.subscription.RotateSubscriptionTokenUseCase;
import com.parazit.panel.application.port.out.security.GeneratedSubscriptionToken;
import com.parazit.panel.application.port.out.security.SubscriptionTokenGenerator;
import com.parazit.panel.application.subscription.command.RotateSubscriptionTokenCommand;
import com.parazit.panel.application.subscription.result.CreateSubscriptionResult;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RotateSubscriptionTokenService implements RotateSubscriptionTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RotateSubscriptionTokenService.class);

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTokenGenerator tokenGenerator;
    private final SubscriptionResultMapper mapper;

    public RotateSubscriptionTokenService(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionTokenGenerator tokenGenerator,
            SubscriptionResultMapper mapper
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator, "tokenGenerator must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public CreateSubscriptionResult rotate(RotateSubscriptionTokenCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new UserNotFoundException(command.telegramUserId()));
        Subscription subscription = subscriptionRepository.findByIdForUpdate(command.subscriptionId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.subscriptionId()));
        if (!subscription.getUserId().equals(user.getId())) {
            throw new SubscriptionOwnershipException();
        }
        if (subscription.isTerminal()) {
            throw new SubscriptionNotAccessibleException("Terminal subscription token cannot be rotated");
        }
        GeneratedSubscriptionToken token = tokenGenerator.generate();
        subscription.rotateToken(token.tokenHash(), token.safePrefix());
        Subscription saved = subscriptionRepository.save(subscription);
        log.atInfo()
                .addKeyValue("subscriptionId", saved.getId())
                .addKeyValue("tokenVersion", saved.getTokenVersion())
                .log("Subscription token rotated");
        return mapper.toCreateResult(saved, token.rawToken(), true);
    }
}
