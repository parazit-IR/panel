package com.parazit.panel.api.internal.xui.client;

import com.parazit.panel.application.xui.client.command.AddVpnClientTrafficCommand;
import com.parazit.panel.application.xui.client.command.ChangeVpnClientIpLimitCommand;
import com.parazit.panel.application.xui.client.command.EnableVpnClientCommand;
import com.parazit.panel.application.xui.client.command.RenewVpnClientCommand;
import com.parazit.panel.application.xui.client.command.ReplaceVpnClientTrafficLimitCommand;
import com.parazit.panel.application.xui.client.command.ResetVpnClientTrafficCommand;
import com.parazit.panel.application.xui.client.command.SynchronizeVpnClientCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class XuiClientUpdateApiMapper {

    public RenewVpnClientCommand toCommand(UUID provisionId, RenewXuiClientRequestDto request) {
        return new RenewVpnClientCommand(request.operationId(), request.telegramUserId(), provisionId, request.durationDays(), request.renewalMode());
    }

    public ReplaceVpnClientTrafficLimitCommand toCommand(UUID provisionId, ReplaceXuiClientTrafficRequestDto request) {
        return new ReplaceVpnClientTrafficLimitCommand(request.operationId(), request.telegramUserId(), provisionId, request.trafficLimitBytes());
    }

    public AddVpnClientTrafficCommand toCommand(UUID provisionId, AddXuiClientTrafficRequestDto request) {
        return new AddVpnClientTrafficCommand(request.operationId(), request.telegramUserId(), provisionId, request.additionalTrafficBytes());
    }

    public EnableVpnClientCommand toCommand(UUID provisionId, EnableXuiClientRequestDto request) {
        return new EnableVpnClientCommand(request.operationId(), request.telegramUserId(), provisionId);
    }

    public ChangeVpnClientIpLimitCommand toCommand(UUID provisionId, ChangeXuiClientIpLimitRequestDto request) {
        return new ChangeVpnClientIpLimitCommand(request.operationId(), request.telegramUserId(), provisionId, request.ipLimit());
    }

    public ResetVpnClientTrafficCommand toCommand(UUID provisionId, ResetXuiClientTrafficRequestDto request) {
        return new ResetVpnClientTrafficCommand(request.operationId(), request.telegramUserId(), provisionId);
    }

    public SynchronizeVpnClientCommand toCommand(UUID provisionId, SynchronizeXuiClientRequestDto request) {
        return new SynchronizeVpnClientCommand(request.operationId(), request.telegramUserId(), provisionId);
    }

    public XuiClientUpdateResponse toResponse(XuiClientUpdateResult result) {
        return new XuiClientUpdateResponse(
                result.operationId(),
                result.provisionId(),
                result.inboundId(),
                result.remoteClientId(),
                result.remoteEmail(),
                result.provisionStatus(),
                result.operationType(),
                result.operationStatus(),
                result.enabled(),
                result.trafficLimitBytes(),
                result.uploadBytes(),
                result.downloadBytes(),
                result.totalConsumedBytes(),
                result.remainingBytes(),
                result.expiresAt(),
                result.ipLimit(),
                result.synchronizedAt(),
                result.changed()
        );
    }
}
