package com.parazit.panel.application.port.in.promotion;

import java.util.UUID;

public interface ReleaseDiscountReservationUseCase {

    void releaseForOrder(UUID orderId);
}
