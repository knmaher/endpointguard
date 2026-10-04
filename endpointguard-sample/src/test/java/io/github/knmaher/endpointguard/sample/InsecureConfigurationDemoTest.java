package io.github.knmaher.endpointguard.sample;

import io.github.knmaher.endpointguard.spring.EndpointGuardTest;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import org.springframework.test.context.ActiveProfiles;

/**
 * Runs EndpointGuard against the broken configuration so the build fails, exactly as it would in
 * a real project. Skipped by default; run it with:
 *
 * <pre>
 * ./mvnw -pl endpointguard-sample -am verify -Dendpointguard.demo=insecure
 * </pre>
 */
@ActiveProfiles("insecure")
@EnabledIfSystemProperty(named = "endpointguard.demo", matches = "insecure")
class InsecureConfigurationDemoTest implements EndpointGuardTest {
}
