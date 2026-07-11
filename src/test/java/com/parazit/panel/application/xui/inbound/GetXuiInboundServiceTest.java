package com.parazit.panel.application.xui.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.xui.inbound.query.GetXuiInboundByIdQuery;
import com.parazit.panel.application.xui.inbound.query.GetXuiInboundByRemarkQuery;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetXuiInboundServiceTest {

    @Test
    void getsByIdAndRemark() {
        ListXuiInboundsServiceTest.StubInboundClient client = new ListXuiInboundsServiceTest.StubInboundClient(
                List.of(ListXuiInboundsServiceTest.inbound(7, "Reality Main"))
        );
        GetXuiInboundService service = new GetXuiInboundService(client, new XuiInboundResultMapper());

        assertThat(service.getById(new GetXuiInboundByIdQuery(7)).remark()).isEqualTo("Reality Main");
        assertThat(service.getByRemark(new GetXuiInboundByRemarkQuery("reality main")).id()).isEqualTo(7);
    }

    @Test
    void throwsForMissingAndDuplicateRemark() {
        GetXuiInboundService missing = new GetXuiInboundService(
                new ListXuiInboundsServiceTest.StubInboundClient(List.of()),
                new XuiInboundResultMapper()
        );

        assertThatThrownBy(() -> missing.getById(new GetXuiInboundByIdQuery(99)))
                .isInstanceOf(XuiInboundNotFoundException.class);
        assertThatThrownBy(() -> missing.getByRemark(new GetXuiInboundByRemarkQuery("missing")))
                .isInstanceOf(XuiInboundNotFoundException.class);

        GetXuiInboundService duplicate = new GetXuiInboundService(
                new ListXuiInboundsServiceTest.StubInboundClient(List.of(
                        ListXuiInboundsServiceTest.inbound(7, "main"),
                        ListXuiInboundsServiceTest.inbound(8, "MAIN")
                )),
                new XuiInboundResultMapper()
        );

        assertThatThrownBy(() -> duplicate.getByRemark(new GetXuiInboundByRemarkQuery("main")))
                .isInstanceOf(XuiInboundAmbiguousException.class);
    }

    @Test
    void rejectsInvalidQueries() {
        assertThatThrownBy(() -> new GetXuiInboundByIdQuery(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GetXuiInboundByRemarkQuery(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
