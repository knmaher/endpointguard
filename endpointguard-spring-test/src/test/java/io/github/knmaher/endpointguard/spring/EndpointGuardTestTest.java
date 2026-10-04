package io.github.knmaher.endpointguard.spring;

import java.util.UUID;

import io.github.knmaher.endpointguard.annotation.PublicEndpoint;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Runs test classes implementing {@link EndpointGuardTest} the way a user's build would.
 */
class EndpointGuardTestTest {

    @Test
    void passesForCorrectlySecuredApplication() {
        Events tests = run(SecuredApplicationTest.class);

        tests.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }

    @Test
    void failsWithReportWhenProtectedEndpointIsExposed() {
        Events tests = run(InsecureApplicationTest.class);

        tests.assertStatistics(stats -> stats.started(1).failed(1));
        Throwable failure = tests.failed().stream().findFirst().orElseThrow()
                .getRequiredPayload(org.junit.platform.engine.TestExecutionResult.class)
                .getThrowable().orElseThrow();
        assertThat(failure)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("FAIL DELETE /api/admin/users/{id}")
                .hasMessageContaining("Security contract violation: anonymous request was accepted.")
                .hasMessageContaining("AdminController.deleteUser(UUID)")
                .hasMessageContaining("PASS GET    /api/products")
                .hasMessageContaining("Violations:        1");
    }

    private static Events run(Class<?> testClass) {
        return EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(testClass))
                .execute()
                .testEvents();
    }

    @SpringBootTest(classes = SecuredApplication.class)
    static class SecuredApplicationTest implements EndpointGuardTest {
    }

    @SpringBootTest(classes = InsecureApplication.class)
    static class InsecureApplicationTest implements EndpointGuardTest {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({ProductController.class, AdminController.class})
    static class SecuredApplication {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(requests -> requests
                            .requestMatchers("/api/products").permitAll()
                            .anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({ProductController.class, AdminController.class})
    static class InsecureApplication {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(requests -> requests
                            .requestMatchers("/api/products").permitAll()
                            .requestMatchers("/api/admin/**").permitAll()
                            .anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }

    @RestController
    static class ProductController {

        @PublicEndpoint
        @GetMapping("/api/products")
        String products() {
            return "products";
        }
    }

    @RestController
    static class AdminController {

        @DeleteMapping("/api/admin/users/{id}")
        void deleteUser(@PathVariable UUID id) {
        }
    }
}
