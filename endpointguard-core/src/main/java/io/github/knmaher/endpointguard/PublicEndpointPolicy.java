package io.github.knmaher.endpointguard;

import java.util.Objects;

/**
 * Protected-by-default policy: an endpoint must reject anonymous requests unless its handler
 * method or controller class is annotated with {@link PublicEndpoint}.
 */
public final class PublicEndpointPolicy {

    public ExpectedAccess expectedAccess(EndpointDescriptor endpoint) {
        Objects.requireNonNull(endpoint, "endpoint");
        boolean declaredPublic = endpoint.controllerMethod().isAnnotationPresent(PublicEndpoint.class)
                || endpoint.controllerClass().isAnnotationPresent(PublicEndpoint.class);
        return declaredPublic ? ExpectedAccess.PUBLIC : ExpectedAccess.PROTECTED;
    }

    public PolicyVerdict evaluate(AuthorizationResult result) {
        Objects.requireNonNull(result, "result");
        ExpectedAccess expected = expectedAccess(result.endpoint());
        return new PolicyVerdict(result, expected, verdict(expected, result.outcome()));
    }

    private static PolicyVerdict.Verdict verdict(ExpectedAccess expected, AuthorizationResult.Outcome outcome) {
        return switch (outcome) {
            case INCONCLUSIVE -> PolicyVerdict.Verdict.INCONCLUSIVE;
            case ALLOWED -> expected == ExpectedAccess.PUBLIC ? PolicyVerdict.Verdict.PASSED : PolicyVerdict.Verdict.VIOLATION;
            case DENIED -> expected == ExpectedAccess.PROTECTED ? PolicyVerdict.Verdict.PASSED : PolicyVerdict.Verdict.VIOLATION;
        };
    }
}
