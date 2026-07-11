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
                        && !path.toString().contains("/api/internal/plan/admin/"))
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
    void planManagementDoesNotAddDeferredOperationalModules() throws IOException {
        List<Path> violations = javaFiles("")
                .filter(path -> !path.toString().contains("/domain/plan/")
                        && !path.toString().contains("/application/plan/")
                        && !path.toString().contains("/api/internal/plan/admin/")
                        && !path.toString().contains("/infrastructure/persistence/plan/"))
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
