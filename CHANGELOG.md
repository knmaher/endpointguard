# Changelog

## Unreleased

- Initialize the Maven reactor with core, Spring test, and sample modules.
- Target Java 21 with Spring Boot 4 and JUnit Jupiter.
- Add Maven Wrapper and Java 21/25 CI.
- Add project, contribution, and security documentation.
- Add the `EndpointDescriptor` model in core, independent of Spring.
- Discover Spring MVC endpoints through `SpringMvcEndpointDiscovery`, excluding Spring's own handlers by default.
- Probe anonymous access with `AnonymousProbe`: requests pass the real `SecurityFilterChain` and controller method security, but the controller method is never invoked.
- Add `@PublicEndpoint` and the protected-by-default `PublicEndpointPolicy`, which turns probe results into `PASSED`, `VIOLATION`, or `INCONCLUSIVE` verdicts.
- Add `EndpointGuardTest`: a test class implementing it verifies every endpoint and fails with a readable report on violations.
- Add `SecurityReport`, `ConsoleReportRenderer`, and the `EndpointGuard.scan`/`verify` entry points.
- The sample application now has public, authenticated, and admin endpoints verified by `ApiSecurityTest`.
- Move `@PublicEndpoint` into the dependency-free `endpointguard-annotations` artifact (package `io.github.knmaher.endpointguard.annotation`).
- Spring, JUnit, and servlet dependencies of `endpointguard-spring-test` are now `provided`, so EndpointGuard never imposes versions on applications.
- Adopt JSpecify null-safety annotations; `AuthorizationResult.status` is a nullable `Integer`.
- `EndpointGuard.verify` no longer prints; `EndpointGuardTest` publishes the report as a JUnit report entry.
- Report `INCONCLUSIVE` instead of `ALLOWED` when method security is enabled but its interceptors cannot be found.
- Sample application: a deliberately broken `insecure` profile (misplaced `/api/**` permitAll), a test asserting EndpointGuard catches it, and an opt-in demo (`-Dendpointguard.demo=insecure`) that fails the build.
- Remove the `junit.version` override, which did not pin JUnit Jupiter (Spring Boot 4 manages JUnit 6) and redirected JUnit 4 to a non-existent version.
- Restructure the README around the quick start, supported versions, configuration, and limitations; update the security policy.
