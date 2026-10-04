package io.github.knmaher.endpointguard;

import io.github.knmaher.endpointguard.annotation.PublicEndpoint;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleReportRendererTest {

    private final PublicEndpointPolicy policy = new PublicEndpointPolicy();
    private final ConsoleReportRenderer renderer = new ConsoleReportRenderer();

    @Test
    void rendersPassesViolationsAndInconclusiveResults() throws Exception {
        EndpointDescriptor products = endpoint(HttpMethod.GET, "/api/products", "products");
        EndpointDescriptor profile = endpoint(HttpMethod.GET, "/api/profile", "profile");
        EndpointDescriptor deleteUser = endpoint(HttpMethod.DELETE, "/api/admin/users/{id}", "deleteUser", UUID.class);
        EndpointDescriptor catalog = endpoint(HttpMethod.GET, "/api/catalog", "catalog");
        EndpointDescriptor export = endpoint(HttpMethod.GET, "/api/export", "export");

        SecurityReport report = new SecurityReport(List.of(
                policy.evaluate(AuthorizationResult.allowed(products,
                        "Anonymous GET /api/products passed the SecurityFilterChain and method security.")),
                policy.evaluate(AuthorizationResult.denied(profile, 401,
                        "Anonymous GET /api/profile was rejected by the SecurityFilterChain with HTTP 401 Unauthorized.")),
                policy.evaluate(AuthorizationResult.allowed(deleteUser,
                        "Anonymous DELETE /api/admin/users/1 passed the SecurityFilterChain and method security.")),
                policy.evaluate(AuthorizationResult.denied(catalog, 401,
                        "Anonymous GET /api/catalog was rejected by the SecurityFilterChain with HTTP 401 Unauthorized.")),
                policy.evaluate(AuthorizationResult.inconclusive(export, 200,
                        "Anonymous GET /api/export was answered by the SecurityFilterChain with HTTP 200 OK, which is neither a rejection nor a pass-through."))));

        assertThat(renderer.render(report)).isEqualTo("""
                EndpointGuard

                Scanned 5 endpoints for anonymous access.

                FAIL DELETE /api/admin/users/{id}

                  Security contract violation: anonymous request was accepted.

                  Observed:
                      Anonymous DELETE /api/admin/users/1 passed the SecurityFilterChain and method security.
                  Expected:
                      401 Unauthorized or 403 Forbidden (endpoint is not declared @PublicEndpoint)
                  Controller:
                      ShopController.deleteUser(UUID)
                  Possible causes:
                      - Endpoint missing from the SecurityFilterChain rules
                      - Request matcher broader than intended (for example /api/**)
                      - Endpoint unintentionally covered by permitAll()
                      - Endpoint is meant to be public: annotate it with @PublicEndpoint

                FAIL GET    /api/catalog

                  Security contract violation: declared @PublicEndpoint, but anonymous request was rejected.

                  Observed:
                      Anonymous GET /api/catalog was rejected by the SecurityFilterChain with HTTP 401 Unauthorized.
                  Expected:
                      anonymous access allowed
                  Controller:
                      ShopController.catalog()
                  Possible causes:
                      - SecurityFilterChain requires authentication for this path
                      - Method security (@PreAuthorize, @Secured, @RolesAllowed) rejects anonymous users
                      - Endpoint is meant to be protected: remove @PublicEndpoint

                WARN GET    /api/export

                  Inconclusive: EndpointGuard could not determine whether anonymous access is allowed.

                  Observed:
                      Anonymous GET /api/export was answered by the SecurityFilterChain with HTTP 200 OK, which is neither a rejection nor a pass-through.
                  Expected:
                      401 Unauthorized or 403 Forbidden
                  Controller:
                      ShopController.export()

                PASS GET    /api/products
                PASS GET    /api/profile

                ------------------------------------------------------------

                Endpoints scanned: 5
                Passed:            2
                Violations:        2
                Inconclusive:      1
                """);
    }

    @Test
    void reportsCountsAndViolationFlag() throws Exception {
        EndpointDescriptor profile = endpoint(HttpMethod.GET, "/api/profile", "profile");

        SecurityReport clean = new SecurityReport(List.of(policy.evaluate(AuthorizationResult.denied(profile, 401, "x"))));
        SecurityReport exposed = new SecurityReport(List.of(policy.evaluate(AuthorizationResult.allowed(profile, "x"))));

        assertThat(clean.hasViolations()).isFalse();
        assertThat(clean.passed()).hasSize(1);
        assertThat(exposed.hasViolations()).isTrue();
        assertThat(renderer.render(clean)).contains("Scanned 1 endpoint for anonymous access.");
    }

    private static EndpointDescriptor endpoint(HttpMethod method, String path, String handler, Class<?>... parameters)
            throws NoSuchMethodException {
        return new EndpointDescriptor(method, path, ShopController.class, ShopController.class.getMethod(handler, parameters));
    }

    public static class ShopController {
        @PublicEndpoint
        public void products() {
        }

        public void profile() {
        }

        public void deleteUser(UUID id) {
        }

        @PublicEndpoint
        public void catalog() {
        }

        public void export() {
        }
    }
}
