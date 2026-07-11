package com.parazit.panel.application.xui.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.xui.inbound.query.FindEligibleXuiInboundQuery;
import com.parazit.panel.application.xui.inbound.result.XuiInboundResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class FindEligibleXuiInboundServiceTest {

    @Test
    void selectsLowestIdEligibleInboundDeterministically() {
        FindEligibleXuiInboundService service = service(
                ListXuiInboundsServiceTest.inbound(9, "secondary"),
                ListXuiInboundsServiceTest.inbound(7, "primary")
        );

        XuiInboundResult result = service.findEligible(new FindEligibleXuiInboundQuery(null, null, null, true));

        assertThat(result.id()).isEqualTo(7);
    }

    @Test
    void appliesQueryFiltersAndThrowsWhenNoneEligible() {
        FindEligibleXuiInboundService service = service(
                ListXuiInboundsServiceTest.inbound(7, "primary")
        );

        assertThat(service.findEligible(new FindEligibleXuiInboundQuery("vless", "reality", "tcp", true)).id())
                .isEqualTo(7);
        assertThatThrownBy(() -> service.findEligible(new FindEligibleXuiInboundQuery("vmess", null, null, true)))
                .isInstanceOf(XuiEligibleInboundNotFoundException.class);
    }

    private static FindEligibleXuiInboundService service(com.parazit.panel.application.xui.model.XuiInboundSnapshot... inbounds) {
        return new FindEligibleXuiInboundService(
                new ListXuiInboundsServiceTest.StubInboundClient(List.of(inbounds)),
                new XuiInboundEligibilityPolicy(),
                new XuiInboundResultMapper()
        );
    }
}
