package com.parazit.panel.application.port.in.subscription.delivery;

import com.parazit.panel.application.subscription.delivery.GenerateSubscriptionUrlQrCommand;
import com.parazit.panel.application.subscription.delivery.QrCodeImageResult;

public interface GenerateSubscriptionUrlQrCodeUseCase {

    QrCodeImageResult generate(GenerateSubscriptionUrlQrCommand command);
}

