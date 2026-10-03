# Changelog

## Unreleased

- Initialize the Maven reactor with core, Spring test, and sample modules.
- Target Java 21 with Spring Boot 4 and JUnit 5.
- Add Maven Wrapper and Java 21/25 CI.
- Add project, contribution, and security documentation.
- Add the `EndpointDescriptor` model in core, independent of Spring.
- Discover Spring MVC endpoints through `SpringMvcEndpointDiscovery`, excluding Spring's own handlers by default.
- Probe anonymous access with `AnonymousProbe`: requests pass the real `SecurityFilterChain` and controller method security, but the controller method is never invoked.
- Add `@PublicEndpoint` and the protected-by-default `PublicEndpointPolicy`, which turns probe results into `PASSED`, `VIOLATION`, or `INCONCLUSIVE` verdicts.
- Add `EndpointGuardTest`: a test class implementing it verifies every endpoint and fails with a readable report on violations.
- Add `SecurityReport`, `ConsoleReportRenderer`, and the `EndpointGuard.scan`/`verify` entry points.
- The sample application now has public, authenticated, and admin endpoints verified by `ApiSecurityTest`.
