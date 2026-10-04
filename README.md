# EndpointGuard 🔐

Catch accidentally exposed Spring Boot endpoints before they reach production.

Your application has 80 REST endpoints. Are you certain all 80 have the correct authorization rules?

EndpointGuard verifies them automatically. It discovers every Spring MVC endpoint, sends an anonymous request through your **real** Spring Security configuration, and fails the build when an endpoint you did not declare public lets the request through.

> **Status:** pre-release. Nothing is published to Maven Central yet; build it locally with `./mvnw install` (see [Building from source](#building-from-source)).

## Quick start

**1. Add the dependencies.** The test library, plus the tiny annotations artifact for production code:

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

**2. Add one test class:**

```java
class ApiSecurityTest implements EndpointGuardTest {
}
```

**3. Declare the endpoints that are meant to be public.** Everything else must reject anonymous requests:

```java
@PublicEndpoint
@GetMapping("/api/products")
List<Product> products() { ... }
```

**4. Run your tests.** When an endpoint is exposed, the build fails:

```text
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

The `DELETE` handler above was **not executed** during the scan. EndpointGuard never invokes controller methods, so it cannot change your data.

## Supported versions

| | Supported |
|---|---|
| Java | 21 or newer (CI runs 21 and 25) |
| Spring Boot | 4.0.x (built against 4.0.8) |
| Spring Framework | 7.0 |
| Spring Security | 7.0 |
| Web stack | Spring MVC (servlet). WebFlux is not supported. |
| Test framework | JUnit Jupiter, as managed by Spring Boot 4 (JUnit 6) |

Spring Boot 3 support may come later as a separate artifact if there is demand.

## Why EndpointGuard

Authorization bugs rarely look wrong in code review. A controller can look perfectly secure while one broad rule in the security configuration exposes it:

```java
.requestMatchers("/api/**").permitAll()              // meant to open "the API"...
.requestMatchers("/api/admin/**").hasRole("ADMIN")   // ...so this rule never applies
```

Reading annotations or configuration cannot tell you what Spring Security actually does with a request. EndpointGuard asks it: every check runs through the application's own `SecurityFilterChain` and method security. That matters even more when code is generated faster than it is reviewed. AI writes code; EndpointGuard verifies authorization.

## How it works

1. **Discover.** Spring MVC's own request mapping registry lists every endpoint: one entry per HTTP method and path. Spring's internal handlers (such as `/error`) are skipped. A mapping without an HTTP method is checked for GET, POST, PUT, PATCH, and DELETE, because any of them reaches it.
2. **Probe.** For each endpoint, an anonymous request goes through the real `SecurityFilterChain`, then through the controller method's `@PreAuthorize`, `@Secured`, and `@RolesAllowed` checks. The request then stops; the controller method is never called.
3. **Compare.** The result is checked against the declared intent: protected by default, public only with `@PublicEndpoint`.
4. **Report.** Every endpoint gets a line; violations explain what happened, what was expected, which controller method is affected, and the likely causes.

Probe details:

- Path variables are filled with `1`; no request body is sent.
- A valid CSRF token is sent, because CSRF protection is not authorization.
- Probes are always anonymous, even inside a test that runs with `@WithMockUser`.

| Probe result | Meaning |
|---|---|
| `DENIED` | HTTP 401 or 403, or a redirect issued by Spring Security (for example to a login page) |
| `ALLOWED` | The request passed every check and would have reached the controller |
| `INCONCLUSIVE` | Anything else, reported with the status and the reason instead of guessed |

| Declared | Probe result | Verdict |
|---|---|---|
| protected (default) | `DENIED` | pass |
| protected (default) | `ALLOWED` | **violation**: possibly exposed by accident |
| `@PublicEndpoint` | `ALLOWED` | pass |
| `@PublicEndpoint` | `DENIED` | **violation**: declared public but rejects anonymous requests |
| either | `INCONCLUSIVE` | reported, does not fail the build |

## Public endpoints

Put `@PublicEndpoint` on a handler method, or on a controller class to cover all of its handlers (subclasses inherit it). It lives in `endpointguard-annotations` (package `io.github.knmaher.endpointguard.annotation`), which contains only annotations and has no dependencies, so it is safe on the production classpath.

The annotation documents intent only. It does not change Spring Security's behavior; EndpointGuard checks that the two agree, in both directions, so a stale annotation is caught too.

## Configuration

Version 0.1 is deliberately configuration-free. What you can adjust:

- **The application context.** `EndpointGuardTest` uses `@SpringBootTest` defaults. Annotate your test class with `@SpringBootTest(...)`, `@ActiveProfiles`, `@TestPropertySource`, and so on; they take precedence.
- **Programmatic use.** Call the entry points directly from any test with a `WebApplicationContext`:

  ```java
  SecurityReport report = EndpointGuard.scan(context);   // returns the report
  EndpointGuard.verify(context);                         // throws AssertionError on violations
  ```

- **Output.** On success nothing is printed; the report is published as a JUnit report entry that IDEs show next to the test. On failure the full report is the assertion message.

## See it catch a misconfiguration

The sample application has a correct security configuration and the deliberately broken one shown above (Spring profile `insecure`). Run EndpointGuard against it:

```sh
./mvnw -pl endpointguard-sample -am verify -Dendpointguard.demo=insecure
```

The build fails with two violations:

```text
PASS GET    /api/admin
FAIL DELETE /api/admin/users/{id}
  ...
FAIL GET    /api/profile
  ...
PASS GET    /api/public

Endpoints scanned: 4
Passed:            2
Violations:        2
```

`GET /api/admin` still passes because `@PreAuthorize` protects it as a second layer; `DELETE /api/admin/users/{id}` relied on the URL rules alone. Without the flag the demo is skipped, and `InsecureConfigurationTest` asserts these exact findings so the regular build stays green.

## Limitations

- **Anonymous access only.** Version 0.1 checks whether endpoints reject anonymous requests. Role-based checks (USER vs. ADMIN) are planned for 0.2.
- **Web-layer authorization only.** Checks inside handler code, in deeper service layers, or in servlet filters outside Spring Security are not seen. An endpoint protected only that way is reported as allowed.
- **Method security without arguments.** `@PreAuthorize` expressions that read handler arguments (for example `#id == principal.id`) see `null`. If Spring Security's method interceptors cannot be found for an annotated handler, the endpoint is reported `INCONCLUSIVE`, never allowed.
- **Placeholder path values.** Path variables are filled with `1`. Security rules that match specific values or patterns of a path variable may not match the probe as they would a real request.
- **Servlet path.** A custom `spring.mvc.servlet.path` is not prepended to reported paths.
- **Inconclusive results do not fail the build.** They are listed in the report so you can investigate.
- **Not a security scanner.** EndpointGuard verifies your authorization contract. It does not replace Spring Security, penetration testing, or static analysis.

## Modules

| Artifact | Scope | Purpose |
|---|---|---|
| `endpointguard-annotations` | compile | `@PublicEndpoint`. No dependencies. |
| `endpointguard-spring-test` | test | Discovery, probes, and `EndpointGuardTest`. Expects the application to provide Spring MVC, Spring Security, and `spring-boot-starter-test`; brings only `spring-security-test`, versioned by your Spring Boot dependency management. |
| `endpointguard-core` | (transitive) | Framework-independent model, policy, and report. |
| `endpointguard-sample` | not published | Sample application with secure and insecure configurations. |

## Building from source

Install JDK 21 or newer, then:

```sh
./mvnw verify        # build and run all tests
./mvnw install       # make the snapshot available to your own projects
```

On Windows, use `mvnw.cmd`. The wrapper downloads Maven 3.9.11 on its first run.

## Roadmap

- **0.1:** anonymous access verification (this release).
- **0.2:** role-based authorization matrix (anonymous, USER, ADMIN, ...).
- **0.3:** security contract snapshots that flag authorization changes in pull requests.
- Later: JSON and SARIF output, GitHub Code Scanning integration.

## Contributing

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

To report a vulnerability, see [SECURITY.md](SECURITY.md).

## License

[Apache License 2.0](LICENSE).
