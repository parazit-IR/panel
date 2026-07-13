package com.parazit.panel.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java");

    @Test
    void domainDoesNotDependOnApiOrInfrastructure() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/domain")
                .filter(path -> source(path).contains("com.parazit.panel.api.")
                        || source(path).contains("com.parazit.panel.infrastructure."))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void applicationDoesNotDependOnInfrastructureOrSpringDataRepositories() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/application")
                .filter(path -> source(path).contains("com.parazit.panel.infrastructure.")
                        || source(path).contains("org.springframework.data.jpa.repository")
                        || source(path).contains("JpaRepository"))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void controllersDoNotDependOnSpringDataRepositoriesOrEntities() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/api")
                .filter(path -> source(path).contains("org.springframework.data.jpa.repository")
                        || source(path).contains("JpaRepository")
                        || source(path).contains("com.parazit.panel.infrastructure.persistence")
                        || source(path).contains("jakarta.persistence.Entity"))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void noFieldInjectionOrApplicationContextServiceLocatorUsage() throws IOException {
        List<Path> violations = javaFiles("")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("@Autowired")
                            || source.contains("ApplicationContext")
                            || source.contains("BeanFactory");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void springDataRepositoriesRemainInInfrastructure() throws IOException {
        List<Path> violations = javaFiles("")
                .filter(path -> source(path).contains("extends JpaRepository")
                        && !path.toString().contains("/infrastructure/persistence/"))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void domainRepositoriesDoNotExtendSpringDataTypes() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/domain")
                .filter(path -> source(path).contains("JpaRepository")
                        || source(path).contains("CrudRepository")
                        || source(path).contains("PagingAndSortingRepository"))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void planDomainStaysIndependentFromDeferredModules() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/domain/plan")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.api.")
                            || source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("com.parazit.panel.domain.user.")
                            || source.contains("com.parazit.panel.payment")
                            || source.contains("com.parazit.panel.subscription")
                            || source.contains("com.parazit.panel.telegram")
                            || source.contains("com.parazit.panel.panel");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void planTaskDoesNotAddPublicApiSurfaceOrEntityRelationships() throws IOException {
        List<Path> publicApiViolations = javaFiles("com/parazit/panel/api")
                .filter(path -> source(path).contains("domain.plan")
                        && !path.toString().contains("/api/internal/plan/admin/")
                        && !path.toString().contains("/api/plan/catalog/")
                        && !path.toString().contains("/api/plan/selection/"))
                .toList();
        List<Path> relationshipViolations = javaFiles("com/parazit/panel/domain/plan")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("@OneToOne")
                            || source.contains("@OneToMany")
                            || source.contains("@ManyToOne")
                            || source.contains("@ManyToMany");
                })
                .toList();

        assertThat(publicApiViolations).isEmpty();
        assertThat(relationshipViolations).isEmpty();
    }

    @Test
    void adminPlanControllerDependsOnInputPortsOnly() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/api/internal/plan/admin")
                .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.domain.plan.repository")
                            || source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("SpringData")
                            || source.contains("JpaRepository");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void catalogPlanControllerDependsOnInputPortsOnlyAndDoesNotExposeStatusFilter() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/api/plan/catalog")
                .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.domain.plan.repository")
                            || source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("SpringData")
                            || source.contains("JpaRepository")
                            || source.contains("PlanStatus")
                            || source.contains("@RequestParam(required = false) PlanStatus");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void catalogPlanApiDoesNotReuseAdminDtos() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/api/plan/catalog")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("api.internal.plan.admin")
                            || source.contains("import com.parazit.panel.api.internal.plan.admin.PlanResponse")
                            || source.contains("new PlanResponse(");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void planSelectionControllerDependsOnInputPortsOnlyAndDoesNotExposeEntities() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/api/plan/selection")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.domain.plan.selection.PlanSelection;")
                            || source.contains("com.parazit.panel.domain.plan.repository")
                            || source.contains("com.parazit.panel.domain.plan.selection.repository")
                            || source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("SpringData")
                            || source.contains("JpaRepository");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void planManagementDoesNotAddDeferredOperationalModules() throws IOException {
        List<Path> violations = javaFiles("")
                .filter(path -> !path.toString().contains("/domain/plan/")
                        && !path.toString().contains("/application/plan/")
                        && !path.toString().contains("/api/internal/plan/admin/")
                        && !path.toString().contains("/api/plan/catalog/")
                        && !path.toString().contains("/api/plan/selection/")
                        && !path.toString().contains("/infrastructure/persistence/plan/")
                        && !path.toString().contains("/infrastructure/xui/"))
                .filter(path -> {
                    String source = source(path);
                    return source.contains("PlanPayment")
                            || source.contains("PlanSubscription")
                            || source.contains("PlanOrder")
                            || source.contains("PlanTelegram")
                            || source.contains("ThreeX")
                            || source.contains("3x");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void paymentFoundationKeepsProviderDetailsOutOfBusinessLogic() throws IOException {
        List<Path> providerImplementations = javaFiles("com/parazit/panel")
                .filter(path -> path.getFileName().toString().endsWith("PaymentProcessor.java"))
                .filter(path -> !source(path).contains("interface PaymentProcessor"))
                .filter(path -> !path.toString().contains("/application/payment/zarinpal/ZarinpalPaymentProcessor.java"))
                .filter(path -> !path.toString().contains("/application/payment/manual/ManualCardPaymentProcessor.java"))
                .toList();
        List<Path> businessSwitches = javaFiles("com/parazit/panel/application/payment")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("switch (")
                            || source.contains("switch(")
                            || source.contains("case ZARINPAL")
                            || source.contains("case CARD_TO_CARD");
                })
                .toList();

        assertThat(providerImplementations).isEmpty();
        assertThat(businessSwitches).isEmpty();
    }

    @Test
    void paymentApiDependsOnInputPortsOnly() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/api/internal/payment")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.domain.payment.Payment;")
                            || source.contains("com.parazit.panel.domain.payment.repository")
                            || source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("SpringData")
                            || source.contains("JpaRepository");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void zarinpalGatewayDetailsRemainInInfrastructure() throws IOException {
        List<Path> applicationViolations = javaFiles("com/parazit/panel/application/payment/zarinpal")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("org.springframework.web.client")
                            || source.contains("RestClient")
                            || source.contains("infrastructure.payment.zarinpal.dto")
                            || source.contains("JsonNode");
                })
                .toList();
        List<Path> apiViolations = javaFiles("com/parazit/panel/api/payment/zarinpal")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("domain.payment.zarinpal.ZarinpalPaymentAttempt")
                            || source.contains("repository.");
                })
                .toList();

        assertThat(applicationViolations).isEmpty();
        assertThat(apiViolations).isEmpty();
    }

    @Test
    void manualPaymentKeepsCardStorageAndApprovalOutOfTask29() throws IOException {
        List<Path> persistenceViolations = javaFiles("com/parazit/panel/domain/payment/manual")
                .filter(path -> source(path).contains("cardNumberEncrypted")
                        || source(path).contains("receiptFile")
                        || source(path).contains("receiptUrl")
                        || source(path).contains("receiptStorage"))
                .toList();
        List<Path> approvalViolations = javaFiles("com/parazit/panel/application/payment/manual")
                .filter(path -> !path.toString().contains("/application/payment/manual/review/"))
                .filter(path -> source(path).contains("markApproved")
                        || source(path).contains("APPROVED"))
                .toList();
        List<Path> apiViolations = javaFiles("com/parazit/panel/api/payment/manual")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("domain.payment.manual.ManualCardPaymentInstruction")
                            || source.contains("repository.");
                })
                .toList();

        assertThat(persistenceViolations).isEmpty();
        assertThat(approvalViolations).isEmpty();
        assertThat(apiViolations).isEmpty();
    }

    @Test
    void manualReceiptUploadKeepsStorageAndApprovalSeparated() throws IOException {
        List<Path> commandViolations = javaFiles("com/parazit/panel/application/payment/manual/receipt")
                .filter(path -> source(path).contains("MultipartFile")
                        || source(path).contains("java.nio.file.Path;")
                        || source(path).contains("markApproved")
                        || source(path).contains("PaymentApproved"))
                .toList();
        List<Path> domainViolations = javaFiles("com/parazit/panel/domain/payment/manual/receipt")
                .filter(path -> source(path).contains("@Lob")
                        || source(path).contains("byte[]")
                        || source(path).contains("MultipartFile")
                        || source(path).contains("java.nio.file.Path"))
                .toList();
        List<Path> apiViolations = javaFiles("com/parazit/panel/api/payment/manual/receipt")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("repository.")
                            || source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("storageKey")
                            || source.contains("StorageKey");
                })
                .toList();

        assertThat(commandViolations).isEmpty();
        assertThat(domainViolations).isEmpty();
        assertThat(apiViolations).isEmpty();
    }

    @Test
    void xuiClientFoundationStaysInfrastructureOnly() throws IOException {
        List<Path> portViolations = javaFiles("com/parazit/panel/application/port/out/xui")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("org.springframework.web.client")
                            || source.contains("org.apache.hc.");
                })
                .toList();
        List<Path> apiViolations = javaFiles("com/parazit/panel/api")
                .filter(path -> source(path).contains("application.port.out.xui")
                        || source(path).contains("infrastructure.xui"))
                .toList();

        assertThat(portViolations).isEmpty();
        assertThat(apiViolations).isEmpty();
    }

    @Test
    void xuiInboundDiscoveryKeepsRemoteDetailsInInfrastructure() throws IOException {
        List<Path> applicationViolations = javaFiles("com/parazit/panel/application")
                .filter(path -> !path.toString().contains("/application/provisioning/outbox/ProvisioningOutboxPayloadSerializer.java"))
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.fasterxml.jackson")
                            || source.contains("org.springframework.web.client")
                            || source.contains("infrastructure.xui.dto")
                            || source.contains("JsonNode")
                            || source.contains("RestClient");
                })
                .toList();
        List<Path> apiViolations = javaFiles("com/parazit/panel/api/internal/xui")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("com.parazit.panel.application.port.out.xui")
                            || source.contains("RestClient")
                            || source.contains("settings")
                            || source.contains("streamSettings")
                            || source.contains("privateKey");
                })
                .toList();

        assertThat(applicationViolations).isEmpty();
        assertThat(apiViolations).isEmpty();
    }

    @Test
    void subscriptionDomainAndApplicationStayInsideCleanArchitectureBoundaries() throws IOException {
        List<Path> domainViolations = javaFiles("com/parazit/panel/domain/subscription")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.api.")
                            || source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("org.springframework");
                })
                .toList();
        List<Path> applicationViolations = javaFiles("com/parazit/panel/application/subscription")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("org.springframework.web.client")
                            || source.contains("jakarta.persistence")
                            || source.contains("JpaRepository")
                            || source.contains("RestClient");
                })
                .toList();

        assertThat(domainViolations).isEmpty();
        assertThat(applicationViolations).isEmpty();
    }

    @Test
    void subscriptionControllersDependOnInputPortsAndDoNotExposeEntitiesOrSecrets() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/api")
                .filter(path -> path.toString().contains("/subscription/"))
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.domain.subscription.Subscription;")
                            || source.contains("com.parazit.panel.domain.subscription.repository")
                            || source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("SpringData")
                            || source.contains("JpaRepository")
                            || source.contains("accessTokenHash")
                            || source.contains("tokenHash")
                            || source.contains("privateKey")
                            || source.contains("cookie");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void subscriptionDeliveryDoesNotIntroduceTelegramOrPaymentMutationCoupling() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel")
                .filter(path -> path.toString().contains("/subscription/"))
                .filter(path -> {
                    String source = source(path);
                    return source.contains("org.telegram")
                            || source.contains("TelegramBot")
                            || source.contains("TelegramLongPollingBot")
                            || source.contains("PaymentApprovalService")
                            || source.contains("markPaid")
                            || source.contains("markCompleted");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void subscriptionRenderingDoesNotDependOnApiDtosOrExposeRealityPrivateKey() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/application/subscription/render")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.api.")
                            || source.contains("privateKey")
                            || source.contains("Reality private");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void qrCodeLibraryIsIsolatedToInfrastructure() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel")
                .filter(path -> source(path).contains("com.google.zxing"))
                .filter(path -> !path.toString().contains("/infrastructure/qrcode/"))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void qrControllersDependOnUseCasesAndDoNotInjectGeneratorsOrRepositories() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/api/internal/subscription/delivery")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("QrCodeGenerator")
                            || source.contains("repository.")
                            || source.contains("JpaRepository")
                            || source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("com.parazit.panel.domain.subscription.Subscription;");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void qrDeliveryDoesNotPersistPayloadsOrBytes() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel")
                .filter(path -> path.toString().contains("/qrcode/")
                        || path.toString().contains("/subscription/delivery/")
                        || path.toString().contains("/subscription/model/"))
                .filter(path -> {
                    String source = source(path);
                    return source.contains("@Entity")
                            || source.contains("@Lob")
                            || source.contains("Files.write")
                            || source.contains("java.nio.file.Path")
                            || source.contains("PaymentApprovalService")
                            || source.contains("org.telegram");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void telegramDomainAndApplicationStayInsideCleanArchitectureBoundaries() throws IOException {
        List<Path> domainViolations = javaFiles("com/parazit/panel/domain/telegram")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.api.")
                            || source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("org.springframework.web.client")
                            || source.contains("RestClient");
                })
                .toList();
        List<Path> applicationViolations = javaFiles("com/parazit/panel/application/telegram")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("com.parazit.panel.infrastructure.")
                            || source.contains("org.springframework.web.client")
                            || source.contains("jakarta.persistence")
                            || source.contains("JpaRepository")
                            || source.contains("RestClient");
                })
                .toList();

        assertThat(domainViolations).isEmpty();
        assertThat(applicationViolations).isEmpty();
    }

    @Test
    void telegramHandlersDependOnUseCasesAndDoNotInjectRepositoriesOrPaymentFlows() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/application/telegram/handler")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("repository.")
                            || source.contains("JpaRepository")
                            || source.contains("PaymentApprovalService")
                            || source.contains("CreatePayment")
                            || source.contains("Zarinpal")
                            || source.contains("ManualCardPayment")
                            || source.contains("CreateVpnClient")
                            || source.contains("XuiClientManagementClient");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void telegramDoesNotPersistSecretsUrlsUrisOrQrBytes() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel/domain/telegram")
                .filter(path -> {
                    String source = source(path);
                    return source.contains("accessToken")
                            || source.contains("subscriptionUrl")
                            || source.contains("vless")
                            || source.contains("VLESS")
                            || source.contains("photoBytes")
                            || source.contains("@Lob")
                            || source.contains("byte[]");
                })
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void telegramSdkTypesDoNotLeakOutsideInfrastructure() throws IOException {
        List<Path> violations = javaFiles("com/parazit/panel")
                .filter(path -> source(path).contains("org.telegram"))
                .filter(path -> !path.toString().contains("/infrastructure/telegram/"))
                .toList();

        assertThat(violations).isEmpty();
    }

    private static Stream<Path> javaFiles(String packagePath) throws IOException {
        Path root = packagePath.isBlank() ? MAIN_JAVA : MAIN_JAVA.resolve(packagePath);
        return Files.walk(root)
                .filter(path -> path.toString().endsWith(".java"));
    }

    private static String source(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
