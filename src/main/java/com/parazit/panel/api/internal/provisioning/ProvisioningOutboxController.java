package com.parazit.panel.api.internal.provisioning;

import com.parazit.panel.application.port.in.provisioning.outbox.GetProvisioningOutboxUseCase;
import com.parazit.panel.application.port.in.provisioning.outbox.ListProvisioningOutboxUseCase;
import com.parazit.panel.application.port.in.provisioning.outbox.RetryProvisioningOutboxUseCase;
import com.parazit.panel.application.provisioning.outbox.command.RetryProvisioningOutboxCommand;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/internal/admin/provisioning/outbox", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProvisioningOutboxController {

    private final ListProvisioningOutboxUseCase listUseCase;
    private final GetProvisioningOutboxUseCase getUseCase;
    private final RetryProvisioningOutboxUseCase retryUseCase;
    private final ProvisioningOutboxApiMapper mapper;

    public ProvisioningOutboxController(
            ListProvisioningOutboxUseCase listUseCase,
            GetProvisioningOutboxUseCase getUseCase,
            RetryProvisioningOutboxUseCase retryUseCase,
            ProvisioningOutboxApiMapper mapper
    ) {
        this.listUseCase = Objects.requireNonNull(listUseCase, "listUseCase must not be null");
        this.getUseCase = Objects.requireNonNull(getUseCase, "getUseCase must not be null");
        this.retryUseCase = Objects.requireNonNull(retryUseCase, "retryUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @GetMapping
    public List<ProvisioningOutboxResponse> list(@RequestParam(required = false) Integer limit) {
        return listUseCase.list(mapper.toListQuery(limit))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{eventId}")
    public ProvisioningOutboxResponse get(@PathVariable UUID eventId) {
        return mapper.toResponse(getUseCase.get(mapper.toGetQuery(eventId)));
    }

    @PostMapping("/{eventId}/retry")
    public ProvisioningOutboxResponse retry(@PathVariable UUID eventId) {
        return mapper.toResponse(retryUseCase.retry(new RetryProvisioningOutboxCommand(eventId)));
    }
}
