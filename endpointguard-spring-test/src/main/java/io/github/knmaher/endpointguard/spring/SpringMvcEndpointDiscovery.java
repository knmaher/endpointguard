package io.github.knmaher.endpointguard.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.github.knmaher.endpointguard.EndpointDescriptor;
import io.github.knmaher.endpointguard.HttpMethod;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Discovers application endpoints from Spring MVC's own request mapping registry.
 *
 * <p>Reads every {@link RequestMappingHandlerMapping} in the context, so it sees exactly the
 * mappings Spring dispatches to, including class-level prefixes and composed annotations.
 *
 * <p>Handlers declared in {@code org.springframework} packages (for example Boot's
 * {@code /error} controller) are excluded unless {@link #includingFrameworkEndpoints()} is used.
 *
 * <p>A mapping that does not restrict the HTTP method accepts any method, so it is reported
 * once for each of {@link #UNRESTRICTED_METHODS}. {@code HEAD} and {@code OPTIONS} are left out
 * because Spring answers them on behalf of the handler.
 */
public final class SpringMvcEndpointDiscovery {

    /** Methods reported for a mapping that declares no HTTP method. */
    public static final Set<HttpMethod> UNRESTRICTED_METHODS = Collections.unmodifiableSet(EnumSet.of(
            HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE));

    private static final String FRAMEWORK_PACKAGE = "org.springframework.";

    private final ApplicationContext context;
    private final boolean includeFrameworkEndpoints;

    public SpringMvcEndpointDiscovery(ApplicationContext context) {
        this(context, false);
    }

    private SpringMvcEndpointDiscovery(ApplicationContext context, boolean includeFrameworkEndpoints) {
        this.context = Objects.requireNonNull(context, "context");
        this.includeFrameworkEndpoints = includeFrameworkEndpoints;
    }

    /** Returns a discovery that also reports handlers declared by Spring itself. */
    public SpringMvcEndpointDiscovery includingFrameworkEndpoints() {
        return new SpringMvcEndpointDiscovery(context, true);
    }

    /**
     * Returns every discovered endpoint, sorted by path and method.
     *
     * @throws IllegalStateException if the context has no Spring MVC request mappings
     */
    public List<EndpointDescriptor> discover() {
        Map<String, RequestMappingHandlerMapping> mappings =
                context.getBeansOfType(RequestMappingHandlerMapping.class);
        if (mappings.isEmpty()) {
            throw new IllegalStateException("""
                    No Spring MVC RequestMappingHandlerMapping found in the application context. \
                    EndpointGuard needs a servlet-based Spring MVC application; \
                    check that spring-webmvc is on the classpath and the test starts a web application context.""");
        }

        List<EndpointDescriptor> endpoints = new ArrayList<>();
        for (RequestMappingHandlerMapping mapping : mappings.values()) {
            mapping.getHandlerMethods().forEach((info, handler) -> {
                if (includeFrameworkEndpoints || !isFrameworkHandler(handler)) {
                    addEndpoints(info, handler, endpoints);
                }
            });
        }
        return endpoints.stream().distinct().sorted().toList();
    }

    private static void addEndpoints(RequestMappingInfo info, HandlerMethod handler, List<EndpointDescriptor> endpoints) {
        Set<HttpMethod> methods = httpMethods(info);
        for (String pattern : info.getPatternValues()) {
            String path = pattern.isEmpty() ? "/" : pattern;
            for (HttpMethod method : methods) {
                endpoints.add(new EndpointDescriptor(method, path, handler.getBeanType(), handler.getMethod()));
            }
        }
    }

    private static Set<HttpMethod> httpMethods(RequestMappingInfo info) {
        Set<RequestMethod> declared = info.getMethodsCondition().getMethods();
        if (declared.isEmpty()) {
            return UNRESTRICTED_METHODS;
        }
        Set<HttpMethod> methods = EnumSet.noneOf(HttpMethod.class);
        for (RequestMethod method : declared) {
            methods.add(HttpMethod.valueOf(method.name()));
        }
        return methods;
    }

    private static boolean isFrameworkHandler(HandlerMethod handler) {
        return handler.getBeanType().getName().startsWith(FRAMEWORK_PACKAGE);
    }
}
