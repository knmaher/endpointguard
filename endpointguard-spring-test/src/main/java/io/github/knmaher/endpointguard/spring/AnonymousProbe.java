package io.github.knmaher.endpointguard.spring;

import java.util.Objects;
import java.util.OptionalInt;

import io.github.knmaher.endpointguard.AuthorizationResult;
import io.github.knmaher.endpointguard.EndpointDescriptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Sends an anonymous request for an endpoint through the application's real Spring Security
 * configuration and reports whether it was let through.
 *
 * <p>The request passes the {@code SecurityFilterChain} and the controller method's method
 * security ({@code @PreAuthorize}, {@code @Secured}, {@code @RolesAllowed}). The controller
 * method itself is never invoked, so probing {@code DELETE} or {@code POST} endpoints cannot
 * change application state. A valid CSRF token is always sent, because CSRF protection is not
 * authorization: an anonymous attacker can usually obtain a token.
 *
 * <p>Out of scope: checks performed inside the handler or deeper service layers, and servlet
 * filters outside Spring Security.
 */
public final class AnonymousProbe {

    private final MockMvc mockMvc;

    /**
     * @throws IllegalStateException if the context has no {@code springSecurityFilterChain}
     */
    public AnonymousProbe(WebApplicationContext context) {
        Objects.requireNonNull(context, "context");
        if (!context.containsBean("springSecurityFilterChain")) {
            throw new IllegalStateException("""
                    No springSecurityFilterChain bean found in the application context. \
                    EndpointGuard verifies the application's real Spring Security configuration; \
                    check that Spring Security is configured for this test context.""");
        }
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .addFilters(new ProbeBoundaryFilter(new MethodSecurityCheck(context)))
                .build();
    }

    public AuthorizationResult probe(EndpointDescriptor endpoint) {
        Objects.requireNonNull(endpoint, "endpoint");
        String path = ProbePaths.toRequestPath(endpoint.path());

        SecurityContext callerContext = TestSecurityContextHolder.getContext();
        TestSecurityContextHolder.clearContext();
        try {
            MvcResult result = mockMvc.perform(request(HttpMethod.valueOf(endpoint.method().name()), path)
                            .accept(MediaType.APPLICATION_JSON)
                            .requestAttr(ProbeBoundaryFilter.ENDPOINT_ATTRIBUTE, endpoint)
                            .with(csrf()))
                    .andReturn();
            return classify(endpoint, path, result.getRequest(), result.getResponse());
        }
        catch (Exception ex) {
            return AuthorizationResult.inconclusive(endpoint, OptionalInt.empty(),
                    "Probe request " + endpoint.method() + " " + path + " failed: " + describe(ex));
        }
        finally {
            TestSecurityContextHolder.setContext(callerContext);
        }
    }

    private static AuthorizationResult classify(EndpointDescriptor endpoint, String path,
            MockHttpServletRequest request, MockHttpServletResponse response) {
        int status = response.getStatus();
        String description = "Anonymous " + endpoint.method() + " " + path;

        if (request.getAttribute(ProbeBoundaryFilter.ALLOWED_ATTRIBUTE) != null) {
            return AuthorizationResult.allowed(endpoint,
                    description + " passed the SecurityFilterChain and method security.");
        }
        if (status == 401 || status == 403) {
            String where = request.getAttribute(ProbeBoundaryFilter.REACHED_ATTRIBUTE) != null
                    ? "method security" : "the SecurityFilterChain";
            return AuthorizationResult.denied(endpoint, status,
                    description + " was rejected by " + where + " with HTTP " + statusText(status) + ".");
        }
        if (request.getAttribute(ProbeBoundaryFilter.REACHED_ATTRIBUTE) == null && isRedirect(status)) {
            return AuthorizationResult.denied(endpoint, status,
                    description + " was redirected by the SecurityFilterChain to "
                            + response.getHeader(HttpHeaders.LOCATION) + " (HTTP " + statusText(status) + ").");
        }
        return AuthorizationResult.inconclusive(endpoint, OptionalInt.of(status),
                description + " was answered by the SecurityFilterChain with HTTP " + statusText(status)
                        + ", which is neither a rejection nor a pass-through.");
    }

    private static boolean isRedirect(int status) {
        return status >= 300 && status < 400;
    }

    private static String statusText(int status) {
        HttpStatus known = HttpStatus.resolve(status);
        return known != null ? status + " " + known.getReasonPhrase() : String.valueOf(status);
    }

    private static String describe(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + (root.getMessage() != null ? ": " + root.getMessage() : "");
    }
}
