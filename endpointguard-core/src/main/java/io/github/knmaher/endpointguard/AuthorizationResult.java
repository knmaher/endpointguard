package io.github.knmaher.endpointguard;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The observed outcome of sending a probe request to an endpoint.
 *
 * @param endpoint the probed endpoint
 * @param outcome whether the request was let through to the handler
 * @param status the HTTP status Spring Security answered with, when it rejected or
 *        short-circuited the request; {@code null} when the request was let through or
 *        no response was produced
 * @param detail a human-readable explanation of how the outcome was determined
 */
public record AuthorizationResult(
        EndpointDescriptor endpoint,
        Outcome outcome,
        @Nullable Integer status,
        String detail) {

    public enum Outcome {
        /** The request passed every authorization check and would have reached the handler. */
        ALLOWED,
        /** Spring Security rejected the request. */
        DENIED,
        /** The probe could not tell whether the request was authorized. */
        INCONCLUSIVE
    }

    public AuthorizationResult {
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(detail, "detail");
    }

    public static AuthorizationResult allowed(EndpointDescriptor endpoint, String detail) {
        return new AuthorizationResult(endpoint, Outcome.ALLOWED, null, detail);
    }

    public static AuthorizationResult denied(EndpointDescriptor endpoint, int status, String detail) {
        return new AuthorizationResult(endpoint, Outcome.DENIED, status, detail);
    }

    public static AuthorizationResult inconclusive(EndpointDescriptor endpoint, @Nullable Integer status, String detail) {
        return new AuthorizationResult(endpoint, Outcome.INCONCLUSIVE, status, detail);
    }
}
