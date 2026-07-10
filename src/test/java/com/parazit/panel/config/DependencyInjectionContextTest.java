package com.parazit.panel.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.api.internal.DependencyInjectionVerificationController;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.service.SystemInfoService;
import com.parazit.panel.infrastructure.time.SystemClockAdapter;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class DependencyInjectionContextTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("panel_di_context_test")
            .withUsername("panel")
            .withPassword("panel");

    private final List<Clock> clocks;
    private final List<SystemClockPort> clockPorts;
    private final List<SystemInfoService> systemInfoServices;
    private final List<DependencyInjectionVerificationController> controllers;
    private final SystemClockPort systemClockPort;

    DependencyInjectionContextTest(
            ObjectProvider<Clock> clocks,
            ObjectProvider<SystemClockPort> clockPorts,
            ObjectProvider<SystemInfoService> systemInfoServices,
            ObjectProvider<DependencyInjectionVerificationController> controllers,
            SystemClockPort systemClockPort
    ) {
        this.clocks = clocks.stream().toList();
        this.clockPorts = clockPorts.stream().toList();
        this.systemInfoServices = systemInfoServices.stream().toList();
        this.controllers = controllers.stream().toList();
        this.systemClockPort = systemClockPort;
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Test
    void contextStartsWithExpectedDependencyInjectionBeans() {
        assertThat(clocks).hasSize(1);
        assertThat(clockPorts).hasSize(1);
        assertThat(systemInfoServices).hasSize(1);
        assertThat(controllers).hasSize(1);
        assertThat(systemClockPort).isInstanceOf(SystemClockAdapter.class);
    }
}
