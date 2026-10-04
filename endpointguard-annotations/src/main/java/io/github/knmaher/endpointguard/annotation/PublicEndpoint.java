package io.github.knmaher.endpointguard.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that an endpoint is intentionally reachable without authentication.
 *
 * <p>EndpointGuard treats every endpoint as protected by default. Place this annotation on a
 * controller method, or on a controller class to cover all of its handler methods, to state that
 * anonymous access is expected. EndpointGuard then verifies the declaration in both directions:
 * an annotated endpoint that rejects anonymous requests is reported as well, so the annotation
 * stays accurate.
 *
 * <p>The annotation only documents intent. It does not change Spring Security's behavior.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface PublicEndpoint {
}
