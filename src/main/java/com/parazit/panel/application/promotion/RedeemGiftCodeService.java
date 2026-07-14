package com.parazit.panel.application.promotion;

import com.parazit.panel.application.port.in.promotion.RedeemGiftCodeUseCase;
import com.parazit.panel.application.port.in.wallet.CreditWalletUseCase;
import com.parazit.panel.application.port.in.wallet.GetOrCreateWalletUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.promotion.PromotionCodeHasher;
import com.parazit.panel.application.promotion.command.RedeemGiftCodeCommand;
import com.parazit.panel.application.promotion.result.GiftCodeRedemptionOutcome;
import com.parazit.panel.application.promotion.result.GiftCodeRedemptionResult;
import com.parazit.panel.application.wallet.command.CreditWalletCommand;
import com.parazit.panel.application.wallet.result.WalletOperationResult;
import com.parazit.panel.domain.promotion.GiftCode;
import com.parazit.panel.domain.promotion.GiftCodeRejectionReason;
import com.parazit.panel.domain.promotion.PromotionRedemption;
import com.parazit.panel.domain.promotion.PromotionRedemptionStatus;
import com.parazit.panel.domain.promotion.repository.GiftCodeRepository;
import com.parazit.panel.domain.promotion.repository.PromotionRedemptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.wallet.Wallet;
import com.parazit.panel.domain.wallet.WalletOperationOutcome;
import com.parazit.panel.domain.wallet.WalletTransactionType;
import com.parazit.panel.domain.wallet.repository.WalletRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedeemGiftCodeService implements RedeemGiftCodeUseCase {

    public static final String REFERENCE_TYPE = "GIFT_CODE_REDEMPTION";
    public static final String DESCRIPTION_CODE = "wallet.gift_code";

    private static final List<PromotionRedemptionStatus> COUNTED_GIFT_STATUSES = List.of(PromotionRedemptionStatus.APPLIED);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final GetOrCreateWalletUseCase getOrCreateWalletUseCase;
    private final CreditWalletUseCase creditWalletUseCase;
    private final GiftCodeRepository giftCodeRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final PromotionCodeNormalizer normalizer;
    private final PromotionCodeHasher hasher;
    private final GiftCodeEligibilityPolicy eligibilityPolicy;
    private final SystemClockPort clock;

    public RedeemGiftCodeService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            GetOrCreateWalletUseCase getOrCreateWalletUseCase,
            CreditWalletUseCase creditWalletUseCase,
            GiftCodeRepository giftCodeRepository,
            PromotionRedemptionRepository redemptionRepository,
            PromotionCodeNormalizer normalizer,
            PromotionCodeHasher hasher,
            GiftCodeEligibilityPolicy eligibilityPolicy,
            SystemClockPort clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository must not be null");
        this.getOrCreateWalletUseCase = Objects.requireNonNull(getOrCreateWalletUseCase, "getOrCreateWalletUseCase must not be null");
        this.creditWalletUseCase = Objects.requireNonNull(creditWalletUseCase, "creditWalletUseCase must not be null");
        this.giftCodeRepository = Objects.requireNonNull(giftCodeRepository, "giftCodeRepository must not be null");
        this.redemptionRepository = Objects.requireNonNull(redemptionRepository, "redemptionRepository must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
        this.hasher = Objects.requireNonNull(hasher, "hasher must not be null");
        this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy, "eligibilityPolicy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public GiftCodeRedemptionResult redeem(RedeemGiftCodeCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userRepository.findByTelegramUserIdForUpdate(command.telegramUserId())
                .orElseThrow(() -> new PromotionException("telegram.promotion.invalid_gift_code"));
        getOrCreateWalletUseCase.getOrCreate(user.getId());
        Wallet wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new PromotionException("telegram.promotion.invalid_gift_code"));
        String hash = hasher.hashNormalizedCode(normalizer.normalize(command.rawCode()));
        GiftCode code = giftCodeRepository.findByCodeHashForUpdate(hash)
                .orElseThrow(() -> new PromotionException("telegram.promotion.invalid_gift_code"));
        PromotionRedemption existing = redemptionRepository.findByUserIdAndGiftCodeId(user.getId(), code.getId()).orElse(null);
        if (existing != null && existing.getStatus() == PromotionRedemptionStatus.APPLIED) {
            return replay(existing);
        }
        GiftCodeRejectionReason decision = eligibilityPolicy.evaluate(code, wallet, clock.now());
        if (decision != GiftCodeRejectionReason.NONE) {
            throw new PromotionException(decision == GiftCodeRejectionReason.USER_LIMIT_REACHED
                    ? "telegram.promotion.gift_user_limit"
                    : "telegram.promotion.invalid_gift_code");
        }
        long userUses = redemptionRepository.countByUserIdAndGiftCodeIdAndStatusIn(user.getId(), code.getId(), COUNTED_GIFT_STATUSES);
        if (userUses >= code.getPerUserUsageLimit()) {
            throw new PromotionException("telegram.promotion.gift_user_limit");
        }
        code.reserveUse();
        PromotionRedemption redemption = redemptionRepository.save(PromotionRedemption.applyGift(
                code.getId(),
                user.getId(),
                wallet.getId(),
                code.creditMoney(),
                "gift-code:" + user.getId() + ":" + code.getId(),
                clock.now()
        ));
        WalletOperationResult walletResult = creditWalletUseCase.credit(new CreditWalletCommand(
                user.getId(),
                code.creditMoney(),
                WalletTransactionType.GIFT_CODE,
                REFERENCE_TYPE,
                redemption.getId(),
                "gift-code:" + redemption.getId(),
                DESCRIPTION_CODE
        ));
        if (walletResult.outcome() != WalletOperationOutcome.APPLIED && walletResult.outcome() != WalletOperationOutcome.REPLAYED) {
            throw new PromotionException("telegram.promotion.invalid_gift_code");
        }
        redemption.attachWalletTransaction(walletResult.transactionId());
        giftCodeRepository.save(code);
        redemptionRepository.save(redemption);
        return new GiftCodeRedemptionResult(
                redemption.getId(),
                walletResult.amount(),
                walletResult.balanceBefore(),
                walletResult.balanceAfter(),
                walletResult.replayed() ? GiftCodeRedemptionOutcome.ALREADY_REDEEMED : GiftCodeRedemptionOutcome.REDEEMED,
                walletResult.replayed(),
                walletResult.occurredAt()
        );
    }

    private GiftCodeRedemptionResult replay(PromotionRedemption redemption) {
        return new GiftCodeRedemptionResult(
                redemption.getId(),
                redemption.giftMoney(),
                null,
                null,
                GiftCodeRedemptionOutcome.ALREADY_REDEEMED,
                true,
                redemption.getRedeemedAt()
        );
    }
}
