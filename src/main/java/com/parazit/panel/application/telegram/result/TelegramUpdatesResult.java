package com.parazit.panel.application.telegram.result;

import com.parazit.panel.application.telegram.model.TelegramUpdate;
import java.util.List;

public record TelegramUpdatesResult(List<TelegramUpdate> updates) {

    public TelegramUpdatesResult {
        updates = updates == null ? List.of() : List.copyOf(updates);
    }
}
