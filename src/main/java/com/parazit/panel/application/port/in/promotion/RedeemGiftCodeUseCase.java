package com.parazit.panel.application.port.in.promotion;

import com.parazit.panel.application.promotion.command.RedeemGiftCodeCommand;
import com.parazit.panel.application.promotion.result.GiftCodeRedemptionResult;

public interface RedeemGiftCodeUseCase {

    GiftCodeRedemptionResult redeem(RedeemGiftCodeCommand command);
}
