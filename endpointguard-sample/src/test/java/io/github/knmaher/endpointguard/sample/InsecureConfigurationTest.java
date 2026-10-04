package io.github.knmaher.endpointguard.sample;

import io.github.knmaher.endpointguard.PolicyVerdict;
import io.github.knmaher.endpointguard.SecurityReport;
import io.github.knmaher.endpointguard.spring.EndpointGuard;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that EndpointGuard catches the deliberately broken configuration of the
 * {@code insecure} profile. See {@link InsecureConfigurationDemoTest} to watch the build fail.
 */
@SpringBootTest
@ActiveProfiles("insecure")
class InsecureConfigurationTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void detectsEndpointsExposedByMisplacedPermitAll() {
        SecurityReport report = EndpointGuard.scan(context);

        assertThat(report.violations())
                .allMatch(PolicyVerdict::isUnexpectedPublicAccess)
                .extracting(verdict -> verdict.endpoint().toString())
                .containsExactly("DELETE /api/admin/users/{id}", "GET /api/profile");
        assertThat(report.passed())
                .extracting(verdict -> verdict.endpoint().toString())
                .containsExactly("GET /api/admin", "GET /api/public");
    }
}
