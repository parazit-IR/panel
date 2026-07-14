package com.parazit.panel.application.port.in.promotion;

import com.parazit.panel.application.promotion.command.ApplyDiscountCodeCommand;
import com.parazit.panel.application.promotion.result.DiscountApplicationResult;

public interface ApplyDiscountCodeUseCase {

    DiscountApplicationResult apply(ApplyDiscountCodeCommand command);
}
