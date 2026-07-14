package com.parazit.panel.application.port.in.promotion;

import com.parazit.panel.application.promotion.command.RemoveDiscountCodeCommand;
import com.parazit.panel.application.promotion.result.DiscountApplicationResult;

public interface RemoveDiscountCodeUseCase {

    DiscountApplicationResult remove(RemoveDiscountCodeCommand command);
}
