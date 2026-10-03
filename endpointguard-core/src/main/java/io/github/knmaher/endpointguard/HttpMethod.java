package io.github.knmaher.endpointguard;

/**
 * HTTP methods EndpointGuard reasons about.
 *
 * <p>Deliberately independent of Spring's {@code HttpMethod} so the core model can be
 * used by reporting code without a Spring dependency.
 */
public enum HttpMethod {
    GET,
    HEAD,
    POST,
    PUT,
    PATCH,
    DELETE,
    OPTIONS,
    TRACE
}
