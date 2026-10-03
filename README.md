# EndpointGuard

Catch accidentally exposed Spring Boot endpoints before they reach production.

EndpointGuard is being built as a test library that verifies authorization by sending requests through an application's real Spring Security filter chain.

## Status

Early development: milestones 1 to 4 (endpoint discovery, the endpoint model, anonymous probes, and the public endpoint policy). Reporting and JUnit integration are not implemented yet. No artifacts have been published to Maven Central.

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

Limitations: authorization checks inside handler code, in deeper service layers, or in servlet filters outside Spring Security are not seen. `@PreAuthorize` expressions that read handler arguments see `null`.

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

Put `@PublicEndpoint` on a handler method, or on a controller class to cover all of its handlers. It lives in `endpointguard-core`, which has no dependencies; because it annotates production code, add that artifact with `compile` scope.

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

- `endpointguard-core`: framework-independent model, `@PublicEndpoint`, and the public endpoint policy; reporting comes later.
- `endpointguard-spring-test`: Spring MVC endpoint discovery and anonymous probes; JUnit integration comes later.
- `endpointguard-sample`: minimal Spring Boot application with a context smoke test.

The foundation uses Spring Boot 4.0.8, Spring Framework 7, Spring Security, and JUnit Jupiter 5.14.1. JUnit 5 is explicitly pinned because Boot 4 manages JUnit 6 by default.

## Next steps

1. Report violations with the affected controller and likely causes.
2. Provide `@EndpointGuardTest` so a single annotated test class verifies every endpoint and fails the build on violations.

The first release focuses on anonymous access. Role matrices and security snapshots come later.

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidance and [SECURITY.md](SECURITY.md) for vulnerability reporting.

## License

[Apache License 2.0](LICENSE).
