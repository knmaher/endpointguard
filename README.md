# EndpointGuard

Catch accidentally exposed Spring Boot endpoints before they reach production.

EndpointGuard is being built as a test library that verifies authorization by sending requests through an application's real Spring Security filter chain.

## Status

Early development: milestones 1 to 5 are implemented. No artifacts have been published to Maven Central yet; build locally with `./mvnw install`.

## Endpoint discovery

`SpringMvcEndpointDiscovery` reads Spring MVC's own request mapping registry and returns one `EndpointDescriptor` per HTTP method and path:

```java
List<EndpointDescriptor> endpoints = new SpringMvcEndpointDiscovery(applicationContext).discover();
// GET /api/users             -> UserController.getUsers()
// DELETE /api/users/{id}     -> UserController.deleteUser(UUID)
```

- Handlers declared by Spring itself, such as Boot's `/error`, are excluded. Use `includingFrameworkEndpoints()` to include them.
- A mapping without an HTTP method accepts any method, so it is reported for GET, POST, PUT, PATCH, and DELETE.
- Paths are reported as declared in the mappings, relative to the dispatcher servlet. A custom `spring.mvc.servlet.path` is not prepended yet.

## Anonymous probes

`AnonymousProbe` sends an anonymous request for an endpoint through the application's real Spring Security configuration:

```java
AnonymousProbe probe = new AnonymousProbe(webApplicationContext);
AuthorizationResult result = probe.probe(endpoint);
// DENIED  401 - Anonymous GET /api/profile was rejected by the SecurityFilterChain with HTTP 401 Unauthorized.
// ALLOWED     - Anonymous DELETE /api/admin/users/1 passed the SecurityFilterChain and method security.
```

How it stays safe:

- The request passes the `SecurityFilterChain`, then the controller method's `@PreAuthorize`, `@Secured`, and `@RolesAllowed` checks. **The controller method is never invoked**, so probing `DELETE` or `POST` endpoints cannot change application state.
- Path variables are filled with `1`. No request body is sent; the handler never reads one.
- A valid CSRF token is sent, because CSRF protection is not authorization.
- The probe is always anonymous, even inside a test annotated with `@WithMockUser`.

Outcomes:

- `DENIED`: HTTP 401 or 403, or a redirect issued by Spring Security (for example to a login page).
- `ALLOWED`: the request passed every check and would have reached the handler.
- `INCONCLUSIVE`: anything else, reported with the status and reason instead of guessed.

Limitations: authorization checks inside handler code, in deeper service layers, or in servlet filters outside Spring Security are not seen. `@PreAuthorize` expressions that read handler arguments see `null`. If EndpointGuard cannot find Spring Security's method interceptors for an annotated handler, it reports the endpoint as `INCONCLUSIVE` rather than allowed.

## Quick start

Add the test dependency, plus the tiny `endpointguard-annotations` artifact for `@PublicEndpoint`:

```xml
<dependency>
    <groupId>io.github.knmaher</groupId>
    <artifactId>endpointguard-spring-test</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.github.knmaher</groupId>
    <artifactId>endpointguard-annotations</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Add one test class:

```java
class ApiSecurityTest implements EndpointGuardTest {
}
```

It starts the application like `@SpringBootTest`, discovers every endpoint, sends an anonymous request through the real Spring Security configuration, and fails when an endpoint that is not declared `@PublicEndpoint` lets the request through. Annotate the class with `@SpringBootTest(...)` to customize the context.

Example failure:

```text
EndpointGuard

Scanned 2 endpoints for anonymous access.

FAIL DELETE /api/admin/users/{id}

  Security contract violation: anonymous request was accepted.

  Observed:
      Anonymous DELETE /api/admin/users/1 passed the SecurityFilterChain and method security.
  Expected:
      401 Unauthorized or 403 Forbidden (endpoint is not declared @PublicEndpoint)
  Controller:
      AdminController.deleteUser(UUID)
  Possible causes:
      - Endpoint missing from the SecurityFilterChain rules
      - Request matcher broader than intended (for example /api/**)
      - Endpoint unintentionally covered by permitAll()
      - Endpoint is meant to be public: annotate it with @PublicEndpoint

PASS GET    /api/products

------------------------------------------------------------

Endpoints scanned: 2
Passed:            1
Violations:        1
Inconclusive:      0
```

The controller method is never invoked during the scan, so endpoints like the `DELETE` above cannot change data. On success nothing is printed; the report is published as a JUnit report entry that IDEs show next to the test.

`endpointguard-spring-test` expects the application to provide Spring MVC, Spring Security, and `spring-boot-starter-test`; it only brings `spring-security-test` itself, versioned by your Spring Boot dependency management.

## Build

Install JDK 21 or newer, then run:

```sh
./mvnw verify
```

On Windows, use `mvnw.cmd verify`. The wrapper downloads Maven 3.9.11 on its first run; dependency downloads require internet access. Development uses JDK 25; production compilation targets Java 21. CI verifies both JDK 21 and 25.

## Public endpoints

Every endpoint is expected to reject anonymous requests unless it is declared public:

```java
@PublicEndpoint
@GetMapping("/api/products")
List<Product> products() { ... }
```

Put `@PublicEndpoint` on a handler method, or on a controller class to cover all of its handlers. It lives in `endpointguard-annotations` (package `io.github.knmaher.endpointguard.annotation`), which contains only annotations and has no dependencies; because it annotates production code, add that artifact with `compile` scope.

`PublicEndpointPolicy` compares each probe result with the declaration:

| Expected | Probe result | Verdict |
|---|---|---|
| protected | `DENIED` | `PASSED` |
| protected | `ALLOWED` | `VIOLATION`: possibly exposed by accident |
| public | `ALLOWED` | `PASSED` |
| public | `DENIED` | `VIOLATION`: declared public but rejects anonymous requests |
| either | `INCONCLUSIVE` | `INCONCLUSIVE` |

The annotation documents intent only; it does not change Spring Security's behavior.

## Modules

- `endpointguard-annotations`: `@PublicEndpoint`, for production code. No dependencies.
- `endpointguard-core`: framework-independent model, public endpoint policy, and console report.
- `endpointguard-spring-test`: Spring MVC endpoint discovery, anonymous probes, and `EndpointGuardTest`.
- `endpointguard-sample`: small Spring Boot application with public, authenticated, and admin endpoints, verified by `ApiSecurityTest`.

The foundation uses Spring Boot 4.0.8, Spring Framework 7, Spring Security, and JUnit Jupiter 5.14.1. JUnit 5 is explicitly pinned because Boot 4 manages JUnit 6 by default.

## Next steps

1. Sample application scenarios that show a misconfiguration being caught.
2. README polish, then the 0.1.0 release.

The first release focuses on anonymous access. Role matrices and security snapshots come later.

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidance and [SECURITY.md](SECURITY.md) for vulnerability reporting.

## License

[Apache License 2.0](LICENSE).
