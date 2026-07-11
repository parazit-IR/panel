package com.parazit.panel.api.internal.xui.inbound;

import com.parazit.panel.application.port.in.xui.inbound.FindEligibleXuiInboundUseCase;
import com.parazit.panel.application.port.in.xui.inbound.GetXuiInboundUseCase;
import com.parazit.panel.application.port.in.xui.inbound.ListXuiInboundsUseCase;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/xui/inbounds")
public class XuiInboundController {

    private final ListXuiInboundsUseCase listUseCase;
    private final GetXuiInboundUseCase getUseCase;
    private final FindEligibleXuiInboundUseCase findEligibleUseCase;
    private final XuiInboundApiMapper mapper;

    public XuiInboundController(
            ListXuiInboundsUseCase listUseCase,
            GetXuiInboundUseCase getUseCase,
            FindEligibleXuiInboundUseCase findEligibleUseCase,
            XuiInboundApiMapper mapper
    ) {
        this.listUseCase = Objects.requireNonNull(listUseCase, "listUseCase must not be null");
        this.getUseCase = Objects.requireNonNull(getUseCase, "getUseCase must not be null");
        this.findEligibleUseCase = Objects.requireNonNull(findEligibleUseCase, "findEligibleUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @GetMapping
    public List<XuiInboundResponse> list() {
        return mapper.toResponse(listUseCase.list());
    }

    @GetMapping("/{inboundId}")
    public XuiInboundResponse getById(@PathVariable long inboundId) {
        return mapper.toResponse(getUseCase.getById(mapper.toGetByIdQuery(inboundId)));
    }

    @GetMapping("/by-remark/{remark}")
    public XuiInboundResponse getByRemark(@PathVariable String remark) {
        return mapper.toResponse(getUseCase.getByRemark(mapper.toGetByRemarkQuery(remark)));
    }

    @GetMapping("/eligible")
    public XuiInboundResponse findEligible(
            @RequestParam(required = false) String protocol,
            @RequestParam(required = false, name = "security") String security,
            @RequestParam(required = false, name = "network") String network
    ) {
        return mapper.toResponse(findEligibleUseCase.findEligible(
                mapper.toFindEligibleQuery(protocol, security, network)
        ));
    }
}
