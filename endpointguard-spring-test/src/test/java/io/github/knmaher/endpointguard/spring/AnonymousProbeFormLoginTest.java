package io.github.knmaher.endpointguard.spring;

import io.github.knmaher.endpointguard.AuthorizationResult;
import io.github.knmaher.endpointguard.AuthorizationResult.Outcome;
import io.github.knmaher.endpointguard.EndpointDescriptor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AnonymousProbeFormLoginTest.TestApplication.class)
class AnonymousProbeFormLoginTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void treatsRedirectToLoginAsDenied() {
        EndpointDescriptor dashboard = new SpringMvcEndpointDiscovery(context).discover().getFirst();

        AuthorizationResult result = new AnonymousProbe(context).probe(dashboard);

        assertThat(result.outcome()).isEqualTo(Outcome.DENIED);
        assertThat(result.status()).hasValue(302);
        assertThat(result.detail()).contains("redirected by the SecurityFilterChain to").contains("/login");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(DashboardController.class)
    static class TestApplication {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                    .formLogin(Customizer.withDefaults())
                    .build();
        }
    }

    @RestController
    static class DashboardController {

        @GetMapping("/dashboard")
        String dashboard() {
            return "dashboard";
        }
    }
}
