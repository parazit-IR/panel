package com.parazit.panel.application.port.in.promotion;

import com.parazit.panel.application.promotion.command.FinalizeDiscountRedemptionCommand;

public interface FinalizeDiscountRedemptionUseCase {

    void finalizeDiscount(FinalizeDiscountRedemptionCommand command);
}
