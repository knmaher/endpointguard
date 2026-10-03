package io.github.knmaher.endpointguard.spring;

/**
 * Turns a Spring MVC path pattern into a concrete request path.
 *
 * <p>Every variable and wildcard segment is replaced by {@value #PLACEHOLDER}. The value only
 * has to satisfy Spring Security's request matchers: the handler is never invoked, so it does
 * not need to convert to the handler's parameter type or match a variable's regex.
 */
final class ProbePaths {

    static final String PLACEHOLDER = "1";

    private ProbePaths() {
    }

    static String toRequestPath(String pattern) {
        StringBuilder path = new StringBuilder(pattern.length());
        int depth = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '{') {
                if (depth++ == 0) {
                    path.append(PLACEHOLDER);
                }
            }
            else if (c == '}') {
                depth--;
            }
            else if (depth == 0) {
                path.append(c);
            }
        }
        return replaceWildcardSegments(path.toString());
    }

    private static String replaceWildcardSegments(String path) {
        String[] segments = path.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].equals("*") || segments[i].equals("**")) {
                segments[i] = PLACEHOLDER;
            }
        }
        return String.join("/", segments);
    }
}
