package io.github.knmaher.endpointguard;

import java.util.List;
import java.util.Objects;

import io.github.knmaher.endpointguard.PolicyVerdict.Verdict;

/**
 * The verdicts for every scanned endpoint, ordered by endpoint.
 *
 * @param verdicts one verdict per endpoint
 */
public record SecurityReport(List<PolicyVerdict> verdicts) {

    public SecurityReport {
        Objects.requireNonNull(verdicts, "verdicts");
        verdicts = verdicts.stream()
                .sorted((a, b) -> a.endpoint().compareTo(b.endpoint()))
                .toList();
    }

    public List<PolicyVerdict> passed() {
        return withVerdict(Verdict.PASSED);
    }

    public List<PolicyVerdict> violations() {
        return withVerdict(Verdict.VIOLATION);
    }

    public List<PolicyVerdict> inconclusive() {
        return withVerdict(Verdict.INCONCLUSIVE);
    }

    public boolean hasViolations() {
        return !violations().isEmpty();
    }

    private List<PolicyVerdict> withVerdict(Verdict verdict) {
        return verdicts.stream().filter(candidate -> candidate.verdict() == verdict).toList();
    }
}
