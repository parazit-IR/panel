package com.parazit.panel.application.port.in.subscription.delivery;

import com.parazit.panel.application.subscription.delivery.GenerateVlessConfigQrCommand;
import com.parazit.panel.application.subscription.delivery.QrCodeImageResult;

public interface GenerateVlessConfigQrCodeUseCase {

    QrCodeImageResult generate(GenerateVlessConfigQrCommand command);
}

