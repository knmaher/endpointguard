# EndpointGuard

Catch accidentally exposed Spring Boot endpoints before they reach production.

EndpointGuard is being built as a test library that verifies authorization by sending requests through an application's real Spring Security filter chain.

## Status

Early development: milestone 0 (repository and build foundation). Endpoint discovery and authorization probes are not implemented yet. No artifacts have been published to Maven Central.

## Build

Install JDK 21 or newer, then run:

```sh
./mvnw verify
```

On Windows, use `mvnw.cmd verify`. The wrapper downloads Maven 3.9.11 on its first run; dependency downloads require internet access. Development uses JDK 25; production compilation targets Java 21. CI verifies both JDK 21 and 25.

## Modules

- `endpointguard-core`: framework-independent models, policies, and reporting (reserved for upcoming milestones).
- `endpointguard-spring-test`: Spring MVC discovery and test integration (reserved for upcoming milestones).
- `endpointguard-sample`: minimal Spring Boot application with a context smoke test.

The foundation uses Spring Boot 4.0.8, Spring Framework 7, Spring Security, and JUnit Jupiter 5.14.1. JUnit 5 is explicitly pinned because Boot 4 manages JUnit 6 by default.

## Next steps

1. Discover application REST endpoints through Spring MVC mappings.
2. Model endpoints independently of Spring mapping objects.
3. Probe anonymous authorization and declare intentionally public endpoints.
4. Report violations and skip requests that cannot be executed safely.

The first release focuses on anonymous access. Role matrices and security snapshots come later. Safe request handling is a prerequisite for running probes against business endpoints.

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidance and [SECURITY.md](SECURITY.md) for vulnerability reporting.

## License

[Apache License 2.0](LICENSE).
