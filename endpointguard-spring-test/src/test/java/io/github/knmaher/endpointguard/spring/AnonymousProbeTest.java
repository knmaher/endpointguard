package io.github.knmaher.endpointguard.spring;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.knmaher.endpointguard.AuthorizationResult;
import io.github.knmaher.endpointguard.AuthorizationResult.Outcome;
import io.github.knmaher.endpointguard.EndpointDescriptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@SpringBootTest(classes = AnonymousProbeTest.TestApplication.class)
class AnonymousProbeTest {

    @Autowired
    private WebApplicationContext context;

    private AnonymousProbe probe;

    @BeforeEach
    void setUp() {
        probe = new AnonymousProbe(context);
        ApiController.invocations.set(0);
    }

    @Test
    void allowsEndpointPermittedBySecurityFilterChain() {
        AuthorizationResult result = probe("GET /api/public");

        assertThat(result.outcome()).isEqualTo(Outcome.ALLOWED);
        assertThat(result.status()).isNull();
        assertThat(result.detail()).isEqualTo("Anonymous GET /api/public passed the SecurityFilterChain and method security.");
    }

    @Test
    void deniesEndpointRequiringAuthentication() {
        AuthorizationResult result = probe("GET /api/profile");

        assertThat(result.outcome()).isEqualTo(Outcome.DENIED);
        assertThat(result.status()).isEqualTo(401);
        assertThat(result.detail()).isEqualTo(
                "Anonymous GET /api/profile was rejected by the SecurityFilterChain with HTTP 401 Unauthorized.");
    }

    @Test
    void deniesEndpointProtectedOnlyByPreAuthorize() {
        AuthorizationResult result = probe("GET /api/admin");

        assertThat(result.outcome()).isEqualTo(Outcome.DENIED);
        assertThat(result.status()).isEqualTo(401);
        assertThat(result.detail()).contains("rejected by method security");
    }

    @Test
    void deniesEndpointProtectedOnlyBySecured() {
        assertThat(probe("GET /api/secured").outcome()).isEqualTo(Outcome.DENIED);
    }

    @Test
    void allowsEndpointWhosePreAuthorizePermitsEveryone() {
        assertThat(probe("GET /api/open").outcome()).isEqualTo(Outcome.ALLOWED);
    }

    @Test
    void detectsExposedDeleteWithoutInvokingTheHandler() {
        AuthorizationResult result = probe("DELETE /api/exposed/users/{id}");

        assertThat(result.outcome()).isEqualTo(Outcome.ALLOWED);
        assertThat(ApiController.invocations).hasValue(0);
    }

    @Test
    void probesPostWithCsrfTokenAndWithoutBody() {
        AuthorizationResult result = probe("POST /api/exposed/orders");

        assertThat(result.outcome()).isEqualTo(Outcome.ALLOWED);
        assertThat(ApiController.invocations).hasValue(0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void probesAnonymouslyEvenWhenTheTestRunsAsAUser() {
        assertThat(probe("GET /api/profile").outcome()).isEqualTo(Outcome.DENIED);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user");
    }

    @Test
    void requiresSpringSecurity() {
        GenericWebApplicationContext plainContext = new GenericWebApplicationContext();
        plainContext.refresh();

        assertThatIllegalStateException()
                .isThrownBy(() -> new AnonymousProbe(plainContext))
                .withMessageContaining("No springSecurityFilterChain bean");
    }

    private AuthorizationResult probe(String endpoint) {
        EndpointDescriptor descriptor = new SpringMvcEndpointDiscovery(context).discover().stream()
                .filter(candidate -> candidate.toString().equals(endpoint))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Endpoint not discovered: " + endpoint));
        return probe.probe(descriptor);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableMethodSecurity(securedEnabled = true)
    @Import(ApiController.class)
    static class TestApplication {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(requests -> requests
                            .requestMatchers("/api/public", "/api/admin", "/api/secured", "/api/open").permitAll()
                            // Deliberately insecure: everything below /api/exposed is public.
                            .requestMatchers("/api/exposed/**").permitAll()
                            .anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }

    @RestController
    static class ApiController {

        static final AtomicInteger invocations = new AtomicInteger();

        @GetMapping("/api/public")
        String publicEndpoint() {
            return record("public");
        }

        @GetMapping("/api/profile")
        String profile() {
            return record("profile");
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/api/admin")
        String admin() {
            return record("admin");
        }

        @Secured("ROLE_ADMIN")
        @GetMapping("/api/secured")
        String secured() {
            return record("secured");
        }

        @PreAuthorize("permitAll()")
        @GetMapping("/api/open")
        String open() {
            return record("open");
        }

        @DeleteMapping("/api/exposed/users/{id}")
        void deleteUser(@PathVariable UUID id) {
            record("deleted");
        }

        @PostMapping("/api/exposed/orders")
        String createOrder(@RequestBody String order) {
            return record("created");
        }

        private static String record(String value) {
            invocations.incrementAndGet();
            return value;
        }
    }
}
