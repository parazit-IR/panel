package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class XuiRemoteClientLookupService {

    private final XuiInboundClient inboundClient;

    public XuiRemoteClientLookupService(XuiInboundClient inboundClient) {
        this.inboundClient = Objects.requireNonNull(inboundClient, "inboundClient must not be null");
    }

    public Optional<XuiClientSnapshot> findVerified(XuiClientProvision provision) {
        Objects.requireNonNull(provision, "provision must not be null");
        Optional<XuiClientSnapshot> client = inboundClient.findClient(
                provision.getInboundId(),
                provision.getRemoteClientId(),
                provision.getRemoteEmail()
        );
        client.ifPresent(snapshot -> verifyIdentity(provision, snapshot));
        return client;
    }

    private static void verifyIdentity(XuiClientProvision provision, XuiClientSnapshot snapshot) {
        if (snapshot.clientId() == null || !snapshot.clientId().equalsIgnoreCase(provision.getRemoteClientId())) {
            throw new XuiRemoteClientIdentityMismatchException();
        }
        if (snapshot.email() != null
                && !snapshot.email().isBlank()
                && !snapshot.email().equalsIgnoreCase(provision.getRemoteEmail())) {
            throw new XuiRemoteClientIdentityMismatchException();
        }
    }
}
