package org.namewta.system.password;

import java.util.List;

/**
 * 可向未认证客户端公开的密码规则。
 */
public record PasswordPolicyProjection(
    int minimumLength,
    int maximumLength,
    List<PasswordCharacterClass> requiredCharacterClasses,
    String allowedSpecialCharacters
) {
    public PasswordPolicyProjection {
        requiredCharacterClasses = List.copyOf(requiredCharacterClasses);
    }
}
