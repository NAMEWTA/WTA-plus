package org.namewta.common.core.validation;

import java.util.List;
import java.util.Optional;

/** 格式校验结果。 */
public record ValidationResult(boolean valid, ValidationIssue issue, List<ValidationIssue> violations) {
    public ValidationResult(boolean valid, ValidationIssue issue) {
        this(valid, issue, issue == null ? List.of() : List.of(issue));
    }

    public ValidationResult {
        violations = violations == null ? List.of() : List.copyOf(violations);
        if (valid && issue != null) {
            throw new IllegalArgumentException("A valid result cannot contain an issue");
        }
        if (!valid && issue == null && violations.isEmpty()) {
            throw new IllegalArgumentException("An invalid result must contain an issue");
        }
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, null, List.of());
    }

    public static ValidationResult invalid(String code) {
        return new ValidationResult(false, new ValidationIssue(code));
    }

    public static ValidationResult invalid(ValidationIssue issue) {
        return new ValidationResult(false, issue, List.of(issue));
    }

    public static ValidationResult invalid(List<ValidationIssue> issues) {
        List<ValidationIssue> copy = issues == null ? List.of() : List.copyOf(issues);
        if (copy.isEmpty()) return ok();
        return new ValidationResult(false, copy.get(0), copy);
    }

    public Optional<ValidationIssue> optionalIssue() {
        return Optional.ofNullable(issue);
    }
}
