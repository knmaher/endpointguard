package io.github.knmaher.endpointguard.spring;

import java.io.IOException;
import java.util.Objects;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import io.github.knmaher.endpointguard.EndpointDescriptor;

/**
 * The last filter of a probe request, placed directly behind Spring Security's filter chain.
 *
 * <p>Reaching this filter means the {@code SecurityFilterChain} let the request through. The
 * filter then runs the controller method's security interceptors and stops: it never continues
 * to the {@code DispatcherServlet}, so no handler is executed. An {@code AccessDeniedException}
 * raised by method security propagates back into Spring Security's
 * {@code ExceptionTranslationFilter}, which answers exactly as it would for a real request.
 */
final class ProbeBoundaryFilter implements Filter {

    static final String ENDPOINT_ATTRIBUTE = ProbeBoundaryFilter.class.getName() + ".endpoint";
    static final String REACHED_ATTRIBUTE = ProbeBoundaryFilter.class.getName() + ".reached";
    static final String ALLOWED_ATTRIBUTE = ProbeBoundaryFilter.class.getName() + ".allowed";

    private final MethodSecurityCheck methodSecurity;

    ProbeBoundaryFilter(MethodSecurityCheck methodSecurity) {
        this.methodSecurity = methodSecurity;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setAttribute(REACHED_ATTRIBUTE, Boolean.TRUE);
        EndpointDescriptor endpoint = Objects.requireNonNull(
                (EndpointDescriptor) request.getAttribute(ENDPOINT_ATTRIBUTE), "probe request without endpoint attribute");
        try {
            methodSecurity.check(endpoint.controllerClass(), endpoint.controllerMethod());
        }
        catch (IOException | ServletException | RuntimeException | Error ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new ServletException("Method security check failed for " + endpoint, ex);
        }
        request.setAttribute(ALLOWED_ATTRIBUTE, Boolean.TRUE);
    }
}
