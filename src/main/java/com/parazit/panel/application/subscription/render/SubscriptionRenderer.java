package com.parazit.panel.application.subscription.render;

import com.parazit.panel.application.subscription.model.RenderedSubscription;
import com.parazit.panel.application.subscription.model.SubscriptionContent;

public interface SubscriptionRenderer {

    RenderedSubscription renderPlain(SubscriptionContent content);

    RenderedSubscription renderBase64(SubscriptionContent content);
}
