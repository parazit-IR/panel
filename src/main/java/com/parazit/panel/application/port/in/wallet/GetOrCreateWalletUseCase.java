package com.parazit.panel.application.port.in.wallet;

import com.parazit.panel.application.wallet.result.WalletCreationResult;
import java.util.UUID;

public interface GetOrCreateWalletUseCase {

    WalletCreationResult getOrCreate(UUID userId);
}
