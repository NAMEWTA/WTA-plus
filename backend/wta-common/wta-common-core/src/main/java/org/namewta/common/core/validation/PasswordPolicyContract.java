package org.namewta.common.core.validation;

/**
 * Password policy view consumed by the common validation facade.
 * Implementations may keep their own transport/configuration type.
 */
public interface PasswordPolicyContract {
    Integer minimumLength();

    Integer maximumLength();

    Boolean requireUppercase();

    Boolean requireLowercase();

    Boolean requireDigit();

    Boolean requireSpecial();

    String allowedSpecialCharacters();
}
