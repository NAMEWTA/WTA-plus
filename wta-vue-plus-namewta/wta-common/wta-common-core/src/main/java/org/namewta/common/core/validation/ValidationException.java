package org.namewta.common.core.validation;

import java.util.List;

/** Exception for imperative validation callers. */
public final class ValidationException extends IllegalArgumentException {
    private final List<ValidationIssue> violations;

    public ValidationException(List<ValidationIssue> violations) {
        super(violations == null || violations.isEmpty() ? "Validation failed" : violations.get(0).code());
        this.violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public List<ValidationIssue> violations() {
        return violations;
    }
}
