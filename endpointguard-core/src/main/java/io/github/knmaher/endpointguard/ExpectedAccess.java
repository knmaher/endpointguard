package io.github.knmaher.endpointguard;

/** The anonymous access an endpoint is expected to allow. */
public enum ExpectedAccess {
    /** Declared with {@link io.github.knmaher.endpointguard.annotation.PublicEndpoint @PublicEndpoint}: anonymous requests must be allowed. */
    PUBLIC,
    /** The default: anonymous requests must be rejected. */
    PROTECTED
}
