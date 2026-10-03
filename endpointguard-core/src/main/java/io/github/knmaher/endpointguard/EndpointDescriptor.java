package io.github.knmaher.endpointguard;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A single HTTP method and path combination served by an application controller method.
 *
 * <p>A controller method mapped to several paths or methods is represented by one
 * descriptor per combination, because each combination can be secured differently.
 *
 * @param method the HTTP method
 * @param path the path pattern as declared in the mapping, for example {@code /api/users/{id}}
 * @param controllerClass the user-declared controller class (never a proxy)
 * @param controllerMethod the handler method
 */
public record EndpointDescriptor(
        HttpMethod method,
        String path,
        Class<?> controllerClass,
        Method controllerMethod) implements Comparable<EndpointDescriptor> {

    private static final Comparator<EndpointDescriptor> ORDER = Comparator
            .comparing(EndpointDescriptor::path)
            .thenComparing(EndpointDescriptor::method)
            .thenComparing(EndpointDescriptor::handlerSignature);

    public EndpointDescriptor {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(controllerClass, "controllerClass");
        Objects.requireNonNull(controllerMethod, "controllerMethod");
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("path must start with '/': " + path);
        }
    }

    /**
     * Returns a short, human-readable handler reference such as
     * {@code AdminUserController.deleteUser(UUID)}.
     */
    public String handlerSignature() {
        String parameters = Arrays.stream(controllerMethod.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        return controllerClass.getSimpleName() + "." + controllerMethod.getName() + "(" + parameters + ")";
    }

    /** Orders by path, then HTTP method, then handler, so reports are deterministic. */
    @Override
    public int compareTo(EndpointDescriptor other) {
        return ORDER.compare(this, other);
    }

    /** Returns {@code METHOD /path}, for example {@code DELETE /api/users/{id}}. */
    @Override
    public String toString() {
        return method + " " + path;
    }
}
