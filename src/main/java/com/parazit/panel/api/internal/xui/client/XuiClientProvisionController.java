package com.parazit.panel.api.internal.xui.client;

import com.parazit.panel.application.port.in.xui.client.CreateVpnClientUseCase;
import com.parazit.panel.application.port.in.xui.client.DeleteVpnClientUseCase;
import com.parazit.panel.application.port.in.xui.client.DisableVpnClientUseCase;
import com.parazit.panel.application.port.in.xui.client.AddVpnClientTrafficUseCase;
import com.parazit.panel.application.port.in.xui.client.ChangeVpnClientIpLimitUseCase;
import com.parazit.panel.application.port.in.xui.client.ChangeVpnClientTrafficLimitUseCase;
import com.parazit.panel.application.port.in.xui.client.EnableVpnClientUseCase;
import com.parazit.panel.application.port.in.xui.client.RenewVpnClientUseCase;
import com.parazit.panel.application.port.in.xui.client.ResetVpnClientTrafficUseCase;
import com.parazit.panel.application.port.in.xui.client.SynchronizeVpnClientUseCase;
import com.parazit.panel.application.xui.client.result.CreateVpnClientResult;
import com.parazit.panel.application.xui.client.result.XuiClientLifecycleResult;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final RenewVpnClientUseCase renewUseCase;
    private final ChangeVpnClientTrafficLimitUseCase trafficLimitUseCase;
    private final AddVpnClientTrafficUseCase addTrafficUseCase;
    private final EnableVpnClientUseCase enableUseCase;
    private final ChangeVpnClientIpLimitUseCase ipLimitUseCase;
    private final ResetVpnClientTrafficUseCase resetTrafficUseCase;
    private final SynchronizeVpnClientUseCase synchronizeUseCase;
    private final XuiClientProvisionApiMapper provisionMapper;
    private final XuiClientLifecycleApiMapper lifecycleMapper;
    private final XuiClientUpdateApiMapper updateMapper;

    public XuiClientProvisionController(
            CreateVpnClientUseCase createUseCase,
            DisableVpnClientUseCase disableUseCase,
            DeleteVpnClientUseCase deleteUseCase,
            RenewVpnClientUseCase renewUseCase,
            ChangeVpnClientTrafficLimitUseCase trafficLimitUseCase,
            AddVpnClientTrafficUseCase addTrafficUseCase,
            EnableVpnClientUseCase enableUseCase,
            ChangeVpnClientIpLimitUseCase ipLimitUseCase,
            ResetVpnClientTrafficUseCase resetTrafficUseCase,
            SynchronizeVpnClientUseCase synchronizeUseCase,
            XuiClientProvisionApiMapper provisionMapper,
            XuiClientLifecycleApiMapper lifecycleMapper,
            XuiClientUpdateApiMapper updateMapper
    ) {
        this.createUseCase = Objects.requireNonNull(createUseCase, "createUseCase must not be null");
        this.disableUseCase = Objects.requireNonNull(disableUseCase, "disableUseCase must not be null");
        this.deleteUseCase = Objects.requireNonNull(deleteUseCase, "deleteUseCase must not be null");
        this.renewUseCase = Objects.requireNonNull(renewUseCase, "renewUseCase must not be null");
        this.trafficLimitUseCase = Objects.requireNonNull(trafficLimitUseCase, "trafficLimitUseCase must not be null");
        this.addTrafficUseCase = Objects.requireNonNull(addTrafficUseCase, "addTrafficUseCase must not be null");
        this.enableUseCase = Objects.requireNonNull(enableUseCase, "enableUseCase must not be null");
        this.ipLimitUseCase = Objects.requireNonNull(ipLimitUseCase, "ipLimitUseCase must not be null");
        this.resetTrafficUseCase = Objects.requireNonNull(resetTrafficUseCase, "resetTrafficUseCase must not be null");
        this.synchronizeUseCase = Objects.requireNonNull(synchronizeUseCase, "synchronizeUseCase must not be null");
        this.provisionMapper = Objects.requireNonNull(provisionMapper, "provisionMapper must not be null");
        this.lifecycleMapper = Objects.requireNonNull(lifecycleMapper, "lifecycleMapper must not be null");
        this.updateMapper = Objects.requireNonNull(updateMapper, "updateMapper must not be null");
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

    @PostMapping("/{provisionId}/renew")
    public ResponseEntity<XuiClientUpdateResponse> renew(
            @PathVariable UUID provisionId,
            @Valid @RequestBody RenewXuiClientRequestDto request
    ) {
        XuiClientUpdateResult result = renewUseCase.renew(updateMapper.toCommand(provisionId, request));
        return ResponseEntity.ok(updateMapper.toResponse(result));
    }

    @PutMapping("/{provisionId}/traffic-limit")
    public ResponseEntity<XuiClientUpdateResponse> replaceTrafficLimit(
            @PathVariable UUID provisionId,
            @Valid @RequestBody ReplaceXuiClientTrafficRequestDto request
    ) {
        XuiClientUpdateResult result = trafficLimitUseCase.replaceTrafficLimit(updateMapper.toCommand(provisionId, request));
        return ResponseEntity.ok(updateMapper.toResponse(result));
    }

    @PostMapping("/{provisionId}/traffic/add")
    public ResponseEntity<XuiClientUpdateResponse> addTraffic(
            @PathVariable UUID provisionId,
            @Valid @RequestBody AddXuiClientTrafficRequestDto request
    ) {
        XuiClientUpdateResult result = addTrafficUseCase.addTraffic(updateMapper.toCommand(provisionId, request));
        return ResponseEntity.ok(updateMapper.toResponse(result));
    }

    @PostMapping("/{provisionId}/enable")
    public ResponseEntity<XuiClientUpdateResponse> enable(
            @PathVariable UUID provisionId,
            @Valid @RequestBody EnableXuiClientRequestDto request
    ) {
        XuiClientUpdateResult result = enableUseCase.enable(updateMapper.toCommand(provisionId, request));
        return ResponseEntity.ok(updateMapper.toResponse(result));
    }

    @PutMapping("/{provisionId}/ip-limit")
    public ResponseEntity<XuiClientUpdateResponse> changeIpLimit(
            @PathVariable UUID provisionId,
            @Valid @RequestBody ChangeXuiClientIpLimitRequestDto request
    ) {
        XuiClientUpdateResult result = ipLimitUseCase.changeIpLimit(updateMapper.toCommand(provisionId, request));
        return ResponseEntity.ok(updateMapper.toResponse(result));
    }

    @PostMapping("/{provisionId}/traffic/reset")
    public ResponseEntity<XuiClientUpdateResponse> resetTraffic(
            @PathVariable UUID provisionId,
            @Valid @RequestBody ResetXuiClientTrafficRequestDto request
    ) {
        XuiClientUpdateResult result = resetTrafficUseCase.resetTraffic(updateMapper.toCommand(provisionId, request));
        return ResponseEntity.ok(updateMapper.toResponse(result));
    }

    @PostMapping("/{provisionId}/synchronize")
    public ResponseEntity<XuiClientUpdateResponse> synchronize(
            @PathVariable UUID provisionId,
            @Valid @RequestBody SynchronizeXuiClientRequestDto request
    ) {
        XuiClientUpdateResult result = synchronizeUseCase.synchronize(updateMapper.toCommand(provisionId, request));
        return ResponseEntity.ok(updateMapper.toResponse(result));
    }
}
