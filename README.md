# EndpointGuard

Catch accidentally exposed Spring Boot endpoints before they reach production.

EndpointGuard is being built as a test library that verifies authorization by sending requests through an application's real Spring Security filter chain.

## Status

Early development: milestones 1 and 2 (endpoint discovery and the endpoint model). Authorization probes are not implemented yet. No artifacts have been published to Maven Central.

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

## Build

Install JDK 21 or newer, then run:

```sh
./mvnw verify
```

On Windows, use `mvnw.cmd verify`. The wrapper downloads Maven 3.9.11 on its first run; dependency downloads require internet access. Development uses JDK 25; production compilation targets Java 21. CI verifies both JDK 21 and 25.

## Modules

- `endpointguard-core`: framework-independent model (`EndpointDescriptor`); policies and reporting come later.
- `endpointguard-spring-test`: Spring MVC endpoint discovery; probes and test integration come later.
- `endpointguard-sample`: minimal Spring Boot application with a context smoke test.

The foundation uses Spring Boot 4.0.8, Spring Framework 7, Spring Security, and JUnit Jupiter 5.14.1. JUnit 5 is explicitly pinned because Boot 4 manages JUnit 6 by default.

## Next steps

1. Probe anonymous authorization and declare intentionally public endpoints.
2. Report violations and skip requests that cannot be executed safely.

The first release focuses on anonymous access. Role matrices and security snapshots come later. Safe request handling is a prerequisite for running probes against business endpoints.

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidance and [SECURITY.md](SECURITY.md) for vulnerability reporting.

## License

[Apache License 2.0](LICENSE).
