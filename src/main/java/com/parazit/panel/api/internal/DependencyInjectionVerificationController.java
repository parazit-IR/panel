package com.parazit.panel.api.internal;

import com.parazit.panel.application.service.SystemInfoService;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoint for verifying the dependency injection foundation.
 * It may be removed when real application endpoints are introduced.
 */
@RestController
@RequestMapping("/internal/di")
public class DependencyInjectionVerificationController {

    private final SystemInfoService systemInfoService;

    public DependencyInjectionVerificationController(SystemInfoService systemInfoService) {
        this.systemInfoService = Objects.requireNonNull(systemInfoService, "systemInfoService must not be null");
    }

    @GetMapping("/time")
    public SystemTimeResponse currentTime() {
        return new SystemTimeResponse(systemInfoService.currentTime());
    }
}
