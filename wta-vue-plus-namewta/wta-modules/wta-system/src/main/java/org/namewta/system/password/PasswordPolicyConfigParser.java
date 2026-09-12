package org.namewta.system.password;

import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.exception.ServiceException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * 解析并完整验证密码策略配置，失败时不暴露配置正文。
 */
@Component
@Slf4j
public class PasswordPolicyConfigParser {

    private static final int VERSION = 1;
    private static final int MINIMUM_ALLOWED_LENGTH = 8;
    private static final int MAXIMUM_ALLOWED_LENGTH = 30;
    private static final int CONFIG_VALUE_LIMIT = 500;

    private final JsonMapper jsonMapper;

    public PasswordPolicyConfigParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * 解析有效的 v1 策略。
     *
     * @param value sys_config 配置值
     * @return 已完整验证的策略
     */
    public PasswordPolicy parse(String value) {
        try {
            if (value == null || value.isBlank() || value.length() > CONFIG_VALUE_LIMIT) {
                throw new IllegalArgumentException("invalid config size");
            }
            PasswordPolicy policy = jsonMapper.readValue(value, PasswordPolicy.class);
            validate(policy);
            return policy;
        } catch (RuntimeException e) {
            log.warn("密码策略配置不可用，configKey={}，exception={}", PasswordPolicy.CONFIG_KEY,
                e.getClass().getSimpleName());
            throw unavailable(e);
        }
    }

    private static void validate(PasswordPolicy policy) {
        if (policy == null || !Integer.valueOf(VERSION).equals(policy.version())
            || policy.minimumLength() == null || policy.maximumLength() == null
            || policy.minimumLength() < MINIMUM_ALLOWED_LENGTH
            || policy.maximumLength() > MAXIMUM_ALLOWED_LENGTH
            || policy.minimumLength() > policy.maximumLength()) {
            throw new IllegalArgumentException("invalid policy bounds");
        }
        if (!Boolean.TRUE.equals(policy.requireUppercase()) || !Boolean.TRUE.equals(policy.requireLowercase())
            || !Boolean.TRUE.equals(policy.requireDigit()) || !Boolean.TRUE.equals(policy.requireSpecial())) {
            throw new IllegalArgumentException("all character classes are required");
        }
        String allowedSpecials = policy.allowedSpecialCharacters();
        if (allowedSpecials == null || allowedSpecials.isEmpty() || !uniqueAsciiSpecials(allowedSpecials)) {
            throw new IllegalArgumentException("invalid allowed special characters");
        }
        PasswordPolicy.Generator generator = policy.generator();
        if (generator == null || generator.length() == null
            || generator.length() < policy.minimumLength() || generator.length() > policy.maximumLength()
            || generator.length() < 4) {
            throw new IllegalArgumentException("invalid generator length");
        }
        validatePool(generator.uppercaseCharacters(), PasswordPolicyRules::isUppercase);
        validatePool(generator.lowercaseCharacters(), PasswordPolicyRules::isLowercase);
        validatePool(generator.digitCharacters(), PasswordPolicyRules::isDigit);
        validatePool(generator.specialCharacters(), ch -> allowedSpecials.indexOf(ch) >= 0);

        PasswordPolicy.DefaultPassword defaultPassword = policy.defaultPassword();
        if (defaultPassword == null || defaultPassword.mode() == null) {
            throw new IllegalArgumentException("missing default password mode");
        }
        if (defaultPassword.mode() == PasswordDefaultMode.FIXED) {
            if (defaultPassword.fixedValue() == null
                || !PasswordPolicyRules.validate(policy, defaultPassword.fixedValue()).isEmpty()) {
                throw new IllegalArgumentException("invalid fixed default");
            }
        } else if (defaultPassword.fixedValue() != null) {
            throw new IllegalArgumentException("random mode cannot contain fixed value");
        }
    }

    private static void validatePool(String pool, IntPredicate predicate) {
        if (pool == null || pool.isEmpty() || pool.chars().anyMatch(predicate.negate()) || !unique(pool)) {
            throw new IllegalArgumentException("invalid generator pool");
        }
    }

    private static boolean uniqueAsciiSpecials(String value) {
        return value.chars().allMatch(ch -> ch >= 33 && ch <= 126
            && !PasswordPolicyRules.isUppercase(ch) && !PasswordPolicyRules.isLowercase(ch)
            && !PasswordPolicyRules.isDigit(ch)) && unique(value);
    }

    private static boolean unique(String value) {
        Set<Integer> characters = new HashSet<>();
        return value.chars().allMatch(characters::add);
    }

    private static ServiceException unavailable(RuntimeException cause) {
        return new ServiceException("PASSWORD_POLICY_UNAVAILABLE", cause);
    }
}
