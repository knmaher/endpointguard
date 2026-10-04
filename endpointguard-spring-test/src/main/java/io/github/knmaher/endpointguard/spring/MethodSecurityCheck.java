package io.github.knmaher.endpointguard.spring;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.Advisor;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authorization.method.AuthorizationAdvisor;

/**
 * Runs the method security interceptors that guard a controller method, without invoking it.
 *
 * <p>The interceptors are taken from the controller's own AOP proxy, so {@code @PreAuthorize},
 * {@code @Secured} and {@code @RolesAllowed} are evaluated by the application's configured
 * authorization managers. The invocation handed to them never reaches the target: proceeding
 * past the last interceptor returns {@code null}. Handler arguments are {@code null}; the
 * target object is exposed only so interceptors can inspect its class.
 */
final class MethodSecurityCheck {

    private static final String SPRING_SECURITY_PACKAGE = "org.springframework.security.";

    private final ApplicationContext context;

    MethodSecurityCheck(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Applies the method security interceptors for the handler, throwing whatever they throw
     * (typically an {@code AccessDeniedException}) when access is denied.
     */
    void check(Class<?> controllerClass, Method handler) throws Throwable {
        for (Object bean : context.getBeansOfType(controllerClass).values()) {
            if (bean instanceof Advised advised) {
                List<MethodInterceptor> interceptors = authorizationInterceptors(advised, controllerClass, handler);
                if (!interceptors.isEmpty()) {
                    Object target = advised.getTargetSource().getTarget();
                    new NonInvokingMethodInvocation(target, handler, interceptors).proceed();
                }
                return;
            }
        }
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
