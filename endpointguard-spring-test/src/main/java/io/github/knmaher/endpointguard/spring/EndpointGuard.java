package io.github.knmaher.endpointguard.spring;

import java.util.Objects;

import io.github.knmaher.endpointguard.ConsoleReportRenderer;
import io.github.knmaher.endpointguard.PublicEndpointPolicy;
import io.github.knmaher.endpointguard.SecurityReport;

import org.springframework.web.context.WebApplicationContext;

/**
 * Entry point: discovers every endpoint of an application, probes anonymous access through its
 * real Spring Security configuration, and checks the results against the public endpoint policy.
 */
public final class EndpointGuard {

    private EndpointGuard() {
    }

    /** Scans the application and returns the report without failing. */
    public static SecurityReport scan(WebApplicationContext context) {
        Objects.requireNonNull(context, "context");
        AnonymousProbe probe = new AnonymousProbe(context);
        PublicEndpointPolicy policy = new PublicEndpointPolicy();
        return new SecurityReport(new SpringMvcEndpointDiscovery(context).discover().stream()
                .map(probe::probe)
                .map(policy::evaluate)
                .toList());
    }

    /**
     * Scans the application and throws an {@link AssertionError} carrying the rendered report
     * when any endpoint violates its authorization contract. Nothing is printed on success.
     */
    public static SecurityReport verify(WebApplicationContext context) {
        SecurityReport report = scan(context);
        if (report.hasViolations()) {
            throw new AssertionError(render(report));
        }
        return report;
    }

    /** Renders a report as plain text, as used in failure messages. */
    public static String render(SecurityReport report) {
        return new ConsoleReportRenderer().render(report);
    }
}
