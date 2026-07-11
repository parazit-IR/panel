package com.parazit.panel.application.xui.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.xui.inbound.result.XuiInboundResult;
import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ListXuiInboundsServiceTest {

    @Test
    void returnsMappedListAndEmptyList() {
        StubInboundClient client = new StubInboundClient(List.of(inbound(7, "main")));
        ListXuiInboundsService service = new ListXuiInboundsService(client, new XuiInboundResultMapper());

        List<XuiInboundResult> results = service.list();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().id()).isEqualTo(7);
        assertThat(results.getFirst().clientCount()).isZero();
        assertThat(client.calls).isEqualTo(1);

        client.inbounds = List.of();
        assertThat(service.list()).isEmpty();
    }

    static XuiInboundSnapshot inbound(long id, String remark) {
        return new XuiInboundSnapshot(
                id,
                remark,
                "VLESS",
                443,
                true,
                null,
                0,
                0,
                0,
                null,
                List.of(),
                "tcp",
                "REALITY",
                "vpn.example.test",
                "pk",
                "sid"
        );
    }

    static class StubInboundClient implements XuiInboundClient {

        private List<XuiInboundSnapshot> inbounds;
        private int calls;

        StubInboundClient(List<XuiInboundSnapshot> inbounds) {
            this.inbounds = inbounds;
        }

        @Override
        public List<XuiInboundSnapshot> getInbounds() {
            calls++;
            return inbounds;
        }

        @Override
        public Optional<XuiInboundSnapshot> getInboundById(long inboundId) {
            calls++;
            return inbounds.stream()
                    .filter(inbound -> inbound.id() == inboundId)
                    .findFirst();
        }

        @Override
        public Optional<com.parazit.panel.application.xui.model.XuiClientSnapshot> findClient(
                long inboundId,
                String clientId,
                String email
        ) {
            return getInboundById(inboundId)
                    .stream()
                    .flatMap(inbound -> inbound.clients().stream())
                    .filter(client -> clientId != null && clientId.equalsIgnoreCase(client.clientId())
                            || email != null && email.equalsIgnoreCase(client.email()))
                    .findFirst();
        }
    }
}
