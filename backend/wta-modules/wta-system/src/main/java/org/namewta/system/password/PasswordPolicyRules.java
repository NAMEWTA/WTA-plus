package org.namewta.system.password;

import java.util.ArrayList;
import java.util.List;

final class PasswordPolicyRules {

    private PasswordPolicyRules() {
    }

    static List<PasswordViolation> validate(PasswordPolicy policy, String password) {
        String value = password == null ? "" : password;
        List<PasswordViolation> violations = new ArrayList<>();
        if (value.length() < policy.minimumLength()) {
            violations.add(new PasswordViolation("PASSWORD_TOO_SHORT",
                "密码长度不能少于 " + policy.minimumLength() + " 位"));
        }
        if (value.length() > policy.maximumLength()) {
            violations.add(new PasswordViolation("PASSWORD_TOO_LONG",
                "密码长度不能超过 " + policy.maximumLength() + " 位"));
        }
        if (Boolean.TRUE.equals(policy.requireUppercase()) && value.chars().noneMatch(PasswordPolicyRules::isUppercase)) {
            violations.add(new PasswordViolation("PASSWORD_MISSING_UPPERCASE", "密码必须包含大写英文字母"));
        }
        if (Boolean.TRUE.equals(policy.requireLowercase()) && value.chars().noneMatch(PasswordPolicyRules::isLowercase)) {
            violations.add(new PasswordViolation("PASSWORD_MISSING_LOWERCASE", "密码必须包含小写英文字母"));
        }
        if (Boolean.TRUE.equals(policy.requireDigit()) && value.chars().noneMatch(PasswordPolicyRules::isDigit)) {
            violations.add(new PasswordViolation("PASSWORD_MISSING_DIGIT", "密码必须包含数字"));
        }
        if (Boolean.TRUE.equals(policy.requireSpecial())
            && value.chars().noneMatch(ch -> policy.allowedSpecialCharacters().indexOf(ch) >= 0)) {
            violations.add(new PasswordViolation("PASSWORD_MISSING_SPECIAL",
                "密码必须包含特殊字符：" + policy.allowedSpecialCharacters()));
        }
        if (value.chars().anyMatch(ch -> !isUppercase(ch) && !isLowercase(ch) && !isDigit(ch)
            && policy.allowedSpecialCharacters().indexOf(ch) < 0)) {
            violations.add(new PasswordViolation("PASSWORD_CONTAINS_DISALLOWED_CHARACTER",
                "密码只能包含英文字母、数字和以下特殊字符：" + policy.allowedSpecialCharacters()));
        }
        return List.copyOf(violations);
    }

    static boolean isUppercase(int ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    static boolean isLowercase(int ch) {
        return ch >= 'a' && ch <= 'z';
    }

    static boolean isDigit(int ch) {
        return ch >= '0' && ch <= '9';
    }
}
