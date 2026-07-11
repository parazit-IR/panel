package com.parazit.panel.api.internal.xui.client;

import com.parazit.panel.application.port.in.xui.client.CreateVpnClientUseCase;
import com.parazit.panel.application.xui.client.result.CreateVpnClientResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/xui/clients")
public class XuiClientProvisionController {

    private final CreateVpnClientUseCase useCase;
    private final XuiClientProvisionApiMapper mapper;

    public XuiClientProvisionController(CreateVpnClientUseCase useCase, XuiClientProvisionApiMapper mapper) {
        this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @PostMapping
    public ResponseEntity<XuiClientProvisionResponse> create(@Valid @RequestBody CreateXuiClientRequestDto request) {
        CreateVpnClientResult result = useCase.create(mapper.toCommand(request));
        XuiClientProvisionResponse response = mapper.toResponse(result);
        if (result.newlyCreated()) {
            return ResponseEntity.created(URI.create("/internal/xui/clients/" + result.provisionId()))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }
}
