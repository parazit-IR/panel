package com.parazit.panel.api.internal.xui.client;

import com.parazit.panel.application.port.in.xui.client.CreateVpnClientUseCase;
import com.parazit.panel.application.port.in.xui.client.DeleteVpnClientUseCase;
import com.parazit.panel.application.port.in.xui.client.DisableVpnClientUseCase;
import com.parazit.panel.application.xui.client.result.CreateVpnClientResult;
import com.parazit.panel.application.xui.client.result.XuiClientLifecycleResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/xui/clients")
public class XuiClientProvisionController {

    private final CreateVpnClientUseCase createUseCase;
    private final DisableVpnClientUseCase disableUseCase;
    private final DeleteVpnClientUseCase deleteUseCase;
    private final XuiClientProvisionApiMapper provisionMapper;
    private final XuiClientLifecycleApiMapper lifecycleMapper;

    public XuiClientProvisionController(
            CreateVpnClientUseCase createUseCase,
            DisableVpnClientUseCase disableUseCase,
            DeleteVpnClientUseCase deleteUseCase,
            XuiClientProvisionApiMapper provisionMapper,
            XuiClientLifecycleApiMapper lifecycleMapper
    ) {
        this.createUseCase = Objects.requireNonNull(createUseCase, "createUseCase must not be null");
        this.disableUseCase = Objects.requireNonNull(disableUseCase, "disableUseCase must not be null");
        this.deleteUseCase = Objects.requireNonNull(deleteUseCase, "deleteUseCase must not be null");
        this.provisionMapper = Objects.requireNonNull(provisionMapper, "provisionMapper must not be null");
        this.lifecycleMapper = Objects.requireNonNull(lifecycleMapper, "lifecycleMapper must not be null");
    }

    @PostMapping
    public ResponseEntity<XuiClientProvisionResponse> create(@Valid @RequestBody CreateXuiClientRequestDto request) {
        CreateVpnClientResult result = createUseCase.create(provisionMapper.toCommand(request));
        XuiClientProvisionResponse response = provisionMapper.toResponse(result);
        if (result.newlyCreated()) {
            return ResponseEntity.created(URI.create("/internal/xui/clients/" + result.provisionId()))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{provisionId}/disable")
    public ResponseEntity<XuiClientLifecycleResponse> disable(
            @PathVariable UUID provisionId,
            @Valid @RequestBody DisableXuiClientRequestDto request
    ) {
        XuiClientLifecycleResult result = disableUseCase.disable(lifecycleMapper.toCommand(provisionId, request));
        return ResponseEntity.ok(lifecycleMapper.toResponse(result));
    }

    @DeleteMapping("/{provisionId}")
    public ResponseEntity<XuiClientLifecycleResponse> delete(
            @PathVariable UUID provisionId,
            @Valid @RequestBody DeleteXuiClientRequestDto request
    ) {
        XuiClientLifecycleResult result = deleteUseCase.delete(lifecycleMapper.toCommand(provisionId, request));
        return ResponseEntity.ok(lifecycleMapper.toResponse(result));
    }
}
