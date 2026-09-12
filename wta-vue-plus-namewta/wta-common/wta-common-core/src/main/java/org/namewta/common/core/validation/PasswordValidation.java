package org.namewta.common.core.validation;

import java.util.Map;

/** Password validation result with stable machine-readable reasons. */
public final class PasswordValidation {
    private PasswordValidation() {
    }

    public static ValidationResult validate(PasswordPolicyContract policy, String password) {
        if (policy == null) {
            return ValidationResult.invalid(new ValidationIssue("validation.password.policy.unavailable"));
        }
        String value = password == null ? "" : password;
        var issues = new java.util.ArrayList<ValidationIssue>();
        if (policy.minimumLength() == null || policy.maximumLength() == null) {
            return ValidationResult.invalid(new ValidationIssue("validation.password.policy.unavailable"));
        }
        if (value.length() < policy.minimumLength()) {
            issues.add(issue("validation.password.tooShort", Map.of("min", policy.minimumLength())));
        }
        if (value.length() > policy.maximumLength()) {
            issues.add(issue("validation.password.tooLong", Map.of("max", policy.maximumLength())));
        }
        if (Boolean.TRUE.equals(policy.requireUppercase()) && value.chars().noneMatch(PasswordValidation::uppercase)) {
            issues.add(issue("validation.password.missingUppercase"));
        }
        if (Boolean.TRUE.equals(policy.requireLowercase()) && value.chars().noneMatch(PasswordValidation::lowercase)) {
            issues.add(issue("validation.password.missingLowercase"));
        }
        if (Boolean.TRUE.equals(policy.requireDigit()) && value.chars().noneMatch(PasswordValidation::digit)) {
            issues.add(issue("validation.password.missingDigit"));
        }
        String specials = policy.allowedSpecialCharacters() == null ? "" : policy.allowedSpecialCharacters();
        if (Boolean.TRUE.equals(policy.requireSpecial()) && value.chars().noneMatch(ch -> specials.indexOf(ch) >= 0)) {
            issues.add(issue("validation.password.missingSpecial", Map.of("specials", specials)));
        }
        if (value.chars().anyMatch(ch -> !uppercase(ch) && !lowercase(ch) && !digit(ch) && specials.indexOf(ch) < 0)) {
            issues.add(issue("validation.password.disallowedCharacter", Map.of("specials", specials)));
        }
        return ValidationResult.invalid(issues);
    }

    private static ValidationIssue issue(String code) {
        return new ValidationIssue(code, Map.of(), null);
    }

    private static ValidationIssue issue(String code, Map<String, Object> args) {
        return new ValidationIssue(code, args, null);
    }

    private static boolean uppercase(int ch) { return ch >= 'A' && ch <= 'Z'; }
    private static boolean lowercase(int ch) { return ch >= 'a' && ch <= 'z'; }
    private static boolean digit(int ch) { return ch >= '0' && ch <= '9'; }
}
