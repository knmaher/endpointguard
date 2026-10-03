package io.github.knmaher.endpointguard;

import java.util.Objects;

/**
 * The comparison of an observed {@link AuthorizationResult} with the endpoint's expected access.
 *
 * @param result what the probe observed
 * @param expected what the policy expects
 * @param verdict whether the observation satisfies the expectation
 */
public record PolicyVerdict(AuthorizationResult result, ExpectedAccess expected, Verdict verdict) {

    public enum Verdict {
        /** The observed access matches the expectation. */
        PASSED,
        /** The observed access contradicts the expectation. */
        VIOLATION,
        /** The probe could not determine access, so the expectation cannot be checked. */
        INCONCLUSIVE
    }

    public PolicyVerdict {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(verdict, "verdict");
    }

    public EndpointDescriptor endpoint() {
        return result.endpoint();
    }

    /** Returns {@code true} for a protected endpoint that accepted an anonymous request. */
    public boolean isUnexpectedPublicAccess() {
        return verdict == Verdict.VIOLATION && expected == ExpectedAccess.PROTECTED;
    }

    /** Returns {@code true} for a {@link PublicEndpoint} that rejected an anonymous request. */
    public boolean isPublicEndpointDenied() {
        return verdict == Verdict.VIOLATION && expected == ExpectedAccess.PUBLIC;
    }
}
