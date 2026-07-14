package com.parazit.panel.application.renewal;

import com.parazit.panel.application.renewal.result.RenewalApplicationTarget;
import com.parazit.panel.application.xui.client.XuiClientUpdateUnknownException;
import com.parazit.panel.application.xui.client.XuiRemoteClientIdentityMismatchException;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RenewalRemoteVerificationPolicy {

    public void verifyIdentity(XuiClientProvision provision, XuiClientSnapshot remote) {
        Objects.requireNonNull(provision, "provision must not be null");
        Objects.requireNonNull(remote, "remote must not be null");
        if (remote.clientId() == null || !remote.clientId().equalsIgnoreCase(provision.getRemoteClientId())) {
            throw new XuiRemoteClientIdentityMismatchException();
        }
        if (remote.email() != null
                && !remote.email().isBlank()
                && !remote.email().equalsIgnoreCase(provision.getRemoteEmail())) {
            throw new XuiRemoteClientIdentityMismatchException();
        }
    }

    public void verifyTarget(RenewalApplicationTarget target, XuiClientSnapshot remote) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(remote, "remote must not be null");
        if (remote.expiryTime() == null || !remote.expiryTime().equals(target.desiredExpiryAt())) {
            throw new XuiClientUpdateUnknownException("Xui renewal expiry verification failed");
        }
        if (remote.totalTrafficLimitBytes() != target.desiredTotalTrafficBytes()) {
            throw new XuiClientUpdateUnknownException("Xui renewal traffic verification failed");
        }
        if (target.resetUsage() && (remote.uploadBytes() != 0 || remote.downloadBytes() != 0)) {
            throw new XuiClientUpdateUnknownException("Xui renewal traffic reset verification failed");
        }
    }
}
