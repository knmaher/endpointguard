package io.github.knmaher.endpointguard.spring;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.knmaher.endpointguard.PolicyVerdict;
import io.github.knmaher.endpointguard.PolicyVerdict.Verdict;
import io.github.knmaher.endpointguard.PublicEndpoint;
import io.github.knmaher.endpointguard.PublicEndpointPolicy;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Discovery, anonymous probe and public endpoint policy against one real application.
 */
@SpringBootTest(classes = PublicEndpointPolicyIntegrationTest.TestApplication.class)
class PublicEndpointPolicyIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void passesIntentionalPublicEndpointsAndFailsUnintentionalOnes() {
        AnonymousProbe probe = new AnonymousProbe(context);
        PublicEndpointPolicy policy = new PublicEndpointPolicy();

        Map<String, PolicyVerdict> verdicts = new SpringMvcEndpointDiscovery(context).discover().stream()
                .map(probe::probe)
                .map(policy::evaluate)
                .collect(Collectors.toMap(verdict -> verdict.endpoint().toString(), verdict -> verdict));

        assertThat(verdicts).extractingFromEntries(entry -> entry(entry.getKey(), entry.getValue().verdict()))
                .containsExactlyInAnyOrder(
                        entry("GET /api/products", Verdict.PASSED),
                        entry("GET /api/health", Verdict.PASSED),
                        entry("GET /api/profile", Verdict.PASSED),
                        entry("GET /api/admin", Verdict.PASSED),
                        entry("DELETE /api/admin/users/{id}", Verdict.VIOLATION),
                        entry("GET /api/catalog", Verdict.VIOLATION));

        assertThat(verdicts.get("DELETE /api/admin/users/{id}").isUnexpectedPublicAccess()).isTrue();
        assertThat(verdicts.get("GET /api/catalog").isPublicEndpointDenied()).isTrue();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableMethodSecurity
    @Import({ShopController.class, HealthController.class, AdminController.class})
    static class TestApplication {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(requests -> requests
                            .requestMatchers("/api/products", "/api/health").permitAll()
                            // Meant for GET /api/admin, which @PreAuthorize protects, but it also
                            // exposes every other admin endpoint.
                            .requestMatchers("/api/admin/**").permitAll()
                            .anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }

    @RestController
    static class ShopController {

        @PublicEndpoint
        @GetMapping("/api/products")
        String products() {
            return "products";
        }

        @GetMapping("/api/profile")
        String profile() {
            return "profile";
        }

        // Declared public, but the security configuration still requires authentication.
        @PublicEndpoint
        @GetMapping("/api/catalog")
        String catalog() {
            return "catalog";
        }
    }

    @PublicEndpoint
    @RestController
    static class HealthController {

        @GetMapping("/api/health")
        String health() {
            return "ok";
        }
    }

    @RestController
    static class AdminController {

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/api/admin")
        String admin() {
            return "admin";
        }

        @DeleteMapping("/api/admin/users/{id}")
        void deleteUser(@PathVariable UUID id) {
        }
    }
}
