package io.github.knmaher.endpointguard;

import io.github.knmaher.endpointguard.annotation.PublicEndpoint;


import io.github.knmaher.endpointguard.AuthorizationResult.Outcome;
import io.github.knmaher.endpointguard.PolicyVerdict.Verdict;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PublicEndpointPolicyTest {

    private final PublicEndpointPolicy policy = new PublicEndpointPolicy();

    @Test
    void treatsUnannotatedEndpointsAsProtected() throws Exception {
        assertThat(policy.expectedAccess(endpoint(MixedController.class, "profile"))).isEqualTo(ExpectedAccess.PROTECTED);
    }

    @Test
    void readsAnnotationOnHandlerMethod() throws Exception {
        assertThat(policy.expectedAccess(endpoint(MixedController.class, "products"))).isEqualTo(ExpectedAccess.PUBLIC);
    }

    @Test
    void readsAnnotationOnControllerClass() throws Exception {
        assertThat(policy.expectedAccess(endpoint(PublicController.class, "health"))).isEqualTo(ExpectedAccess.PUBLIC);
    }

    @Test
    void inheritsClassAnnotationFromSuperclass() throws Exception {
        assertThat(policy.expectedAccess(endpoint(InheritingController.class, "status"))).isEqualTo(ExpectedAccess.PUBLIC);
    }

    @ParameterizedTest(name = "{0} endpoint, {1} -> {2}")
    @CsvSource({
            "profile,  DENIED,       PASSED",
            "profile,  ALLOWED,      VIOLATION",
            "profile,  INCONCLUSIVE, INCONCLUSIVE",
            "products, ALLOWED,      PASSED",
            "products, DENIED,       VIOLATION",
            "products, INCONCLUSIVE, INCONCLUSIVE"
    })
    void comparesObservedOutcomeWithExpectation(String handler, Outcome outcome, Verdict expected) throws Exception {
        EndpointDescriptor endpoint = endpoint(MixedController.class, handler);
        AuthorizationResult result = new AuthorizationResult(endpoint, outcome, null, "observed");

        PolicyVerdict verdict = policy.evaluate(result);

        assertThat(verdict.verdict()).isEqualTo(expected);
        assertThat(verdict.result()).isSameAs(result);
    }

    @Test
    void distinguishesTheTwoKindsOfViolation() throws Exception {
        PolicyVerdict exposed = policy.evaluate(AuthorizationResult.allowed(endpoint(MixedController.class, "profile"), "x"));
        PolicyVerdict lockedDown = policy.evaluate(AuthorizationResult.denied(endpoint(MixedController.class, "products"), 401, "x"));

        assertThat(exposed.isUnexpectedPublicAccess()).isTrue();
        assertThat(exposed.isPublicEndpointDenied()).isFalse();
        assertThat(lockedDown.isPublicEndpointDenied()).isTrue();
        assertThat(lockedDown.isUnexpectedPublicAccess()).isFalse();
    }

    private static EndpointDescriptor endpoint(Class<?> controller, String handler) throws NoSuchMethodException {
        return new EndpointDescriptor(HttpMethod.GET, "/" + handler, controller, controller.getMethod(handler));
    }

    public static class MixedController {
        @PublicEndpoint
        public void products() {
        }

        public void profile() {
        }
    }

    @PublicEndpoint
    public static class PublicController {
        public void health() {
        }
    }

    public static class InheritingController extends PublicController {
        public void status() {
        }
    }
}
