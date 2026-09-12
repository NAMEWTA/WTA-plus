package org.namewta.system.password;

/**
 * sys.user.passwordPolicy v1 内部配置。
 */
import org.namewta.common.core.validation.PasswordPolicyContract;

public record PasswordPolicy(
    Integer version,
    Integer minimumLength,
    Integer maximumLength,
    Boolean requireUppercase,
    Boolean requireLowercase,
    Boolean requireDigit,
    Boolean requireSpecial,
    String allowedSpecialCharacters,
    Generator generator,
    DefaultPassword defaultPassword
) implements PasswordPolicyContract {

    public static final String CONFIG_KEY = "sys.user.passwordPolicy";

    /**
     * 随机密码生成器内部配置。
     */
    public record Generator(
        Integer length,
        String uppercaseCharacters,
        String lowercaseCharacters,
        String digitCharacters,
        String specialCharacters
    ) {
        @Override
        public String toString() {
            return "Generator[length=" + length + ", characterPools=<redacted>]";
        }
    }

    /**
     * 默认候选值配置。
     */
    public record DefaultPassword(PasswordDefaultMode mode, String fixedValue) {
        @Override
        public String toString() {
            return "DefaultPassword[mode=" + mode + ", fixedValue=<redacted>]";
        }
    }

    @Override
    public String toString() {
        return "PasswordPolicy[version=" + version + ", minimumLength=" + minimumLength
            + ", maximumLength=" + maximumLength + ", internals=<redacted>]";
    }
}
