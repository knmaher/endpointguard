package io.github.knmaher.endpointguard.spring;

import io.github.knmaher.endpointguard.AuthorizationResult;
import io.github.knmaher.endpointguard.AuthorizationResult.Outcome;
import io.github.knmaher.endpointguard.EndpointDescriptor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simulates a Spring Security version whose method security interceptors EndpointGuard cannot
 * find: the controller is registered as a ready-made singleton, so it is never proxied although
 * method security is enabled. The probe must not report the endpoint as open.
 */
@SpringBootTest(classes = AnonymousProbeUnsupportedMethodSecurityTest.TestApplication.class)
@ContextConfiguration(initializers = AnonymousProbeUnsupportedMethodSecurityTest.UnproxiedController.class)
class AnonymousProbeUnsupportedMethodSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void reportsInconclusiveInsteadOfAllowedWhenMethodSecurityCannotBeEvaluated() {
        EndpointDescriptor admin = new SpringMvcEndpointDiscovery(context).discover().getFirst();

        AuthorizationResult result = new AnonymousProbe(context).probe(admin);

        assertThat(result.outcome()).isEqualTo(Outcome.INCONCLUSIVE);
        assertThat(result.detail())
                .contains("AdminController.admin carries a method security annotation")
                .contains("found no Spring Security method interceptor");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableMethodSecurity
    static class TestApplication {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }

    static class UnproxiedController implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            context.getBeanFactory().registerSingleton("adminController", new AdminController());
        }
    }

    @RestController
    static class AdminController {

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/api/admin")
        String admin() {
            return "admin";
        }
    }
}
