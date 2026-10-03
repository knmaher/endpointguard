# Changelog

## Unreleased

- Initialize the Maven reactor with core, Spring test, and sample modules.
- Target Java 21 with Spring Boot 4 and JUnit 5.
- Add Maven Wrapper and Java 21/25 CI.
- Add project, contribution, and security documentation.
- Add the `EndpointDescriptor` model in core, independent of Spring.
- Discover Spring MVC endpoints through `SpringMvcEndpointDiscovery`, excluding Spring's own handlers by default.
