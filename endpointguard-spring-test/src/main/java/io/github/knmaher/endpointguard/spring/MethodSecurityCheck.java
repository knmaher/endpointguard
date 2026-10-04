package io.github.knmaher.endpointguard.spring;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.Advisor;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.security.authorization.method.AuthorizationAdvisor;

/**
 * Runs the method security interceptors that guard a controller method, without invoking it.
 *
 * <p>The interceptors are taken from the controller's own AOP proxy, so {@code @PreAuthorize},
 * {@code @Secured} and {@code @RolesAllowed} are evaluated by the application's configured
 * authorization managers. The invocation handed to them never reaches the target: proceeding
 * past the last interceptor returns {@code null}. Handler arguments are {@code null}; the
 * target object is exposed only so interceptors can inspect its class.
 *
 * <p>Spring Security registers these interceptors as an internal detail that has changed between
 * versions (tested with Spring Security 7.0). To avoid silently reporting a protected endpoint as
 * open after such a change, a handler that carries a method security annotation in an application
 * with method security enabled, but on which no interceptor is found, fails the check instead.
 */
final class MethodSecurityCheck {

    private static final String SPRING_SECURITY_PACKAGE = "org.springframework.security.";

    private static final Set<String> METHOD_SECURITY_ANNOTATIONS = Set.of(
            "org.springframework.security.access.prepost.PreAuthorize",
            "org.springframework.security.access.prepost.PostAuthorize",
            "org.springframework.security.access.annotation.Secured",
            "jakarta.annotation.security.RolesAllowed",
            "jakarta.annotation.security.DenyAll",
            "jakarta.annotation.security.PermitAll");

    private final ApplicationContext context;

    private @Nullable Boolean methodSecurityEnabled;

    MethodSecurityCheck(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Applies the method security interceptors for the handler, throwing whatever they throw
     * (typically an {@code AccessDeniedException}) when access is denied.
     */
    void check(Class<?> controllerClass, Method handler) throws Throwable {
        Advised advised = findAdvisedController(controllerClass);
        List<MethodInterceptor> interceptors = advised != null
                ? authorizationInterceptors(advised, controllerClass, handler)
                : List.of();

        if (interceptors.isEmpty()) {
            if (hasMethodSecurityAnnotation(controllerClass, handler) && isMethodSecurityEnabled()) {
                throw new IllegalStateException("""
                        %s.%s carries a method security annotation and method security is enabled, \
                        but EndpointGuard found no Spring Security method interceptor on the controller. \
                        This Spring Security version may register its interceptors differently than \
                        EndpointGuard expects; please report it at https://github.com/knmaher/endpointguard/issues"""
                        .formatted(controllerClass.getSimpleName(), handler.getName()));
            }
            return;
        }
        new NonInvokingMethodInvocation(advised.getTargetSource().getTarget(), handler, interceptors).proceed();
    }

    private @Nullable Advised findAdvisedController(Class<?> controllerClass) {
        for (Object bean : context.getBeansOfType(controllerClass).values()) {
            if (bean instanceof Advised advised) {
                return advised;
            }
        }
        return null;
    }

    private boolean isMethodSecurityEnabled() {
        if (methodSecurityEnabled == null) {
            methodSecurityEnabled = context.getBeansOfType(Advisor.class, false, false).values().stream()
                    .anyMatch(advisor -> advisor.getAdvice() instanceof MethodInterceptor interceptor
                            && isSpringSecurity(advisor, interceptor));
        }
        return methodSecurityEnabled;
    }

    private static boolean hasMethodSecurityAnnotation(Class<?> controllerClass, Method handler) {
        return Stream.of(handler, controllerClass)
                .flatMap(element -> MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY).stream())
                .anyMatch(annotation -> METHOD_SECURITY_ANNOTATIONS.contains(annotation.getType().getName()));
    }

    private static List<MethodInterceptor> authorizationInterceptors(Advised advised, Class<?> controllerClass, Method handler) {
        List<MethodInterceptor> interceptors = new ArrayList<>();
        for (Advisor advisor : advised.getAdvisors()) {
            if (advisor instanceof PointcutAdvisor pointcutAdvisor
                    && pointcutAdvisor.getAdvice() instanceof MethodInterceptor interceptor
                    && isSpringSecurity(advisor, interceptor)
                    && AopUtils.canApply(pointcutAdvisor.getPointcut(), controllerClass)
                    && pointcutAdvisor.getPointcut().getMethodMatcher().matches(handler, controllerClass)) {
                interceptors.add(interceptor);
            }
        }
        return interceptors;
    }

    /**
     * Spring Security 7 registers its interceptors behind wrappers (an advisor wrapper around a
     * deferring interceptor), so the {@link AuthorizationAdvisor} type alone is not enough.
     */
    private static boolean isSpringSecurity(Advisor advisor, MethodInterceptor interceptor) {
        return advisor instanceof AuthorizationAdvisor
                || interceptor instanceof AuthorizationAdvisor
                || interceptor.getClass().getName().startsWith(SPRING_SECURITY_PACKAGE);
    }

    private static final class NonInvokingMethodInvocation implements MethodInvocation {

        private final @Nullable Object target;
        private final Method method;
        private final Object[] arguments;
        private final List<MethodInterceptor> interceptors;
        private int next;

        NonInvokingMethodInvocation(@Nullable Object target, Method method, List<MethodInterceptor> interceptors) {
            this.target = target;
            this.method = method;
            this.arguments = new Object[method.getParameterCount()];
            this.interceptors = interceptors;
        }

        @Override
        public @Nullable Object proceed() throws Throwable {
            if (next == interceptors.size()) {
                return null;
            }
            return interceptors.get(next++).invoke(this);
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object[] getArguments() {
            return arguments;
        }

        @Override
        public @Nullable Object getThis() {
            return target;
        }

        @Override
        public AccessibleObject getStaticPart() {
            return method;
        }
    }
}
