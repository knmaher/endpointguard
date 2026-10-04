package io.github.knmaher.endpointguard.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies every endpoint of a Spring Boot application. Implement it in an otherwise empty test
 * class:
 *
 * <pre>{@code
 * class ApiSecurityTest implements EndpointGuardTest {
 * }
 * }</pre>
 *
 * <p>The application context is started with {@link SpringBootTest @SpringBootTest} defaults.
 * Annotate the test class with {@code @SpringBootTest(...)} to customize it; the class-level
 * annotation takes precedence.
 *
 * <p>The test fails when an endpoint that is not declared
 * {@link io.github.knmaher.endpointguard.annotation.PublicEndpoint @PublicEndpoint} accepts anonymous
 * requests, or a declared public endpoint rejects them. On success the report is published as a
 * JUnit report entry, which IDEs show next to the test result.
 */
@SpringBootTest
public interface EndpointGuardTest {

    @Test
    default void endpointsMatchTheirAuthorizationContract(@Autowired WebApplicationContext context,
            TestReporter reporter) {
        reporter.publishEntry("EndpointGuard", EndpointGuard.render(EndpointGuard.verify(context)));
    }
}
