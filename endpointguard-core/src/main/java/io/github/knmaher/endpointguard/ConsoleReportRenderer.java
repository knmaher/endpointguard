package io.github.knmaher.endpointguard;

import java.util.List;
import java.util.Objects;

/**
 * Renders a {@link SecurityReport} as plain text for test output and CI logs.
 *
 * <p>Every endpoint gets one line. Violations and inconclusive results are followed by the
 * observed behavior, the expectation, the handler and the most likely causes, so the problem can
 * be fixed without enabling debug logging.
 *
 * <p>Output is plain ASCII so it renders the same in every console, including CI runners whose
 * default encoding is not UTF-8.
 */
public final class ConsoleReportRenderer {

    private static final String RULE = "-".repeat(60);
    private static final String INDENT = "  ";
    private static final String DETAIL_INDENT = "      ";

    public String render(SecurityReport report) {
        Objects.requireNonNull(report, "report");
        int methodWidth = report.verdicts().stream()
                .mapToInt(verdict -> verdict.endpoint().method().name().length())
                .max()
                .orElse(0);

        StringBuilder out = new StringBuilder();
        out.append("EndpointGuard\n\n");
        out.append("Scanned ").append(count(report.verdicts().size(), "endpoint")).append(" for anonymous access.\n\n");

        for (PolicyVerdict verdict : report.verdicts()) {
            out.append(symbol(verdict)).append(' ')
                    .append(pad(verdict.endpoint().method().name(), methodWidth)).append(' ')
                    .append(verdict.endpoint().path()).append('\n');
            switch (verdict.verdict()) {
                case PASSED -> { }
                case VIOLATION -> appendViolation(out, verdict);
                case INCONCLUSIVE -> appendInconclusive(out, verdict);
            }
        }

        out.append('\n').append(RULE).append("\n\n");
        int width = String.valueOf(report.verdicts().size()).length();
        out.append("Endpoints scanned: ").append(pad(report.verdicts().size(), width)).append('\n');
        out.append("Passed:            ").append(pad(report.passed().size(), width)).append('\n');
        out.append("Violations:        ").append(pad(report.violations().size(), width)).append('\n');
        out.append("Inconclusive:      ").append(pad(report.inconclusive().size(), width)).append('\n');
        return out.toString();
    }

    private static void appendViolation(StringBuilder out, PolicyVerdict verdict) {
        if (verdict.isUnexpectedPublicAccess()) {
            appendBlock(out, verdict,
                    "Security contract violation: anonymous request was accepted.",
                    "401 Unauthorized or 403 Forbidden (endpoint is not declared @PublicEndpoint)",
                    List.of(
                            "Endpoint missing from the SecurityFilterChain rules",
                            "Request matcher broader than intended (for example /api/**)",
                            "Endpoint unintentionally covered by permitAll()",
                            "Endpoint is meant to be public: annotate it with @PublicEndpoint"));
        }
        else {
            appendBlock(out, verdict,
                    "Security contract violation: declared @PublicEndpoint, but anonymous request was rejected.",
                    "anonymous access allowed",
                    List.of(
                            "SecurityFilterChain requires authentication for this path",
                            "Method security (@PreAuthorize, @Secured, @RolesAllowed) rejects anonymous users",
                            "Endpoint is meant to be protected: remove @PublicEndpoint"));
        }
    }

    private static void appendInconclusive(StringBuilder out, PolicyVerdict verdict) {
        appendBlock(out, verdict,
                "Inconclusive: EndpointGuard could not determine whether anonymous access is allowed.",
                verdict.expected() == ExpectedAccess.PUBLIC ? "anonymous access allowed" : "401 Unauthorized or 403 Forbidden",
                List.of());
    }

    private static void appendBlock(StringBuilder out, PolicyVerdict verdict, String problem, String expected,
            List<String> causes) {
        out.append('\n');
        out.append(INDENT).append(problem).append("\n\n");
        out.append(INDENT).append("Observed:\n").append(DETAIL_INDENT).append(verdict.result().detail()).append('\n');
        out.append(INDENT).append("Expected:\n").append(DETAIL_INDENT).append(expected).append('\n');
        out.append(INDENT).append("Controller:\n").append(DETAIL_INDENT).append(verdict.endpoint().handlerSignature()).append('\n');
        if (!causes.isEmpty()) {
            out.append(INDENT).append("Possible causes:\n");
            for (String cause : causes) {
                out.append(DETAIL_INDENT).append("- ").append(cause).append('\n');
            }
        }
        out.append('\n');
    }

    private static String symbol(PolicyVerdict verdict) {
        return switch (verdict.verdict()) {
            case PASSED -> "PASS";
            case VIOLATION -> "FAIL";
            case INCONCLUSIVE -> "WARN";
        };
    }

    private static String count(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    private static String pad(String value, int width) {
        return value + " ".repeat(Math.max(0, width - value.length()));
    }

    private static String pad(int value, int width) {
        String text = String.valueOf(value);
        return " ".repeat(Math.max(0, width - text.length())) + text;
    }
}
