package io.github.knmaher.endpointguard.spring;

import org.junit.jupiter.api.Test;

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
 * {@link io.github.knmaher.endpointguard.PublicEndpoint @PublicEndpoint} accepts anonymous
 * requests, or a declared public endpoint rejects them.
 */
@SpringBootTest
public interface EndpointGuardTest {

    @Test
    default void endpointsMatchTheirAuthorizationContract(@Autowired WebApplicationContext context) {
        EndpointGuard.verify(context);
    }
}
