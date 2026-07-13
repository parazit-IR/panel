package com.parazit.panel.application.renewal;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RenewalPlanCompatibilityPolicy {

    public boolean compatible(Subscription subscription, XuiClientProvision provision, Plan plan) {
        Objects.requireNonNull(subscription, "subscription must not be null");
        Objects.requireNonNull(provision, "provision must not be null");
        Objects.requireNonNull(plan, "plan must not be null");
        return subscription.getUserId().equals(provision.getUserId())
                && subscription.getXuiClientProvisionId().equals(provision.getId())
                && provision.getInboundId() > 0
                && !provision.getRemoteClientId().isBlank();
    }
}
