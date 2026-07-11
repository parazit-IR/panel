package com.parazit.panel.application.xui.inbound;

import com.parazit.panel.application.port.in.xui.inbound.FindEligibleXuiInboundUseCase;
import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.xui.inbound.query.FindEligibleXuiInboundQuery;
import com.parazit.panel.application.xui.inbound.result.XuiInboundResult;
import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import java.util.Comparator;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FindEligibleXuiInboundService implements FindEligibleXuiInboundUseCase {

    private static final Logger log = LoggerFactory.getLogger(FindEligibleXuiInboundService.class);

    private final XuiInboundClient inboundClient;
    private final XuiInboundEligibilityPolicy eligibilityPolicy;
    private final XuiInboundResultMapper mapper;

    public FindEligibleXuiInboundService(
            XuiInboundClient inboundClient,
            XuiInboundEligibilityPolicy eligibilityPolicy,
            XuiInboundResultMapper mapper
    ) {
        this.inboundClient = Objects.requireNonNull(inboundClient, "inboundClient must not be null");
        this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy, "eligibilityPolicy must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public XuiInboundResult findEligible(FindEligibleXuiInboundQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        XuiInboundSnapshot selected = inboundClient.getInbounds()
                .stream()
                .filter(inbound -> matches(query.protocol(), inbound.protocol()))
                .filter(inbound -> matches(query.securityType(), inbound.securityType()))
                .filter(inbound -> matches(query.streamNetwork(), inbound.streamNetwork()))
                .filter(inbound -> !Boolean.TRUE.equals(query.requireEnabled()) || inbound.enabled())
                .filter(eligibilityPolicy::isEligible)
                .min(Comparator.comparingLong(XuiInboundSnapshot::id))
                .orElseThrow(XuiEligibleInboundNotFoundException::new);

        log.atInfo()
                .addKeyValue("inboundId", selected.id())
                .addKeyValue("protocol", selected.protocol())
                .log("Selected eligible Xui inbound");
        return mapper.toResult(selected);
    }

    private static boolean matches(String expected, String actual) {
        return expected == null || (actual != null && actual.equalsIgnoreCase(expected));
    }
}
