package org.namewta.system.password;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.service.ISysConfigService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 所有密码写入与候选生成共用的策略入口。
 */
@Service
public class PasswordPolicyService {

    private static final List<PasswordCharacterClass> REQUIRED_CLASSES = List.of(
        PasswordCharacterClass.UPPERCASE,
        PasswordCharacterClass.LOWERCASE,
        PasswordCharacterClass.DIGIT,
        PasswordCharacterClass.SPECIAL
    );

    private final ISysConfigService configService;
    private final PasswordPolicyConfigParser parser;
    private final SecureRandom secureRandom;

    public PasswordPolicyService(ISysConfigService configService, PasswordPolicyConfigParser parser) {
        this.configService = configService;
        this.parser = parser;
        this.secureRandom = new SecureRandom();
    }

    /**
     * 获取当前经验证的策略。底层 sys_config 使用集群可确认的 Spring Cache。
     *
     * @return 当前策略
     */
    public PasswordPolicy currentPolicy() {
        return parser.parse(configService.selectConfigByKey(PasswordPolicy.CONFIG_KEY));
    }

    /**
     * 返回全部密码违规，顺序为公共 wire contract 的固定顺序。
     *
     * @param password 待验证明文
     * @return 不含密码正文的违规列表
     */
    public List<PasswordViolation> validate(String password) {
        return PasswordPolicyRules.validate(currentPolicy(), password);
    }

    /**
     * 验证密码并用结构化业务错误拒绝违规值。
     *
     * @param password 待验证明文
     */
    public void validateOrThrow(String password) {
        List<PasswordViolation> violations = validate(password);
        if (!violations.isEmpty()) {
            throw new ServiceException("密码不符合安全策略")
                .setData(Map.of("violations", violations));
        }
    }

    /**
     * 生成新增用户或永久重置的默认候选值。
     *
     * @return 合规候选值
     */
    public String generateDefaultPassword() {
        PasswordPolicy policy = currentPolicy();
        if (policy.defaultPassword().mode() == PasswordDefaultMode.FIXED) {
            return policy.defaultPassword().fixedValue();
        }
        return generate(policy);
    }

    /**
     * 生成临时密码。该路径始终使用安全随机生成器。
     *
     * @return 合规随机密码
     */
    public String generateTemporaryPassword() {
        return generate(currentPolicy());
    }

    /**
     * 返回公开客户端所需的最小策略投影。
     *
     * @return 不含生成器和默认值的投影
     */
    public PasswordPolicyProjection publicProjection() {
        PasswordPolicy policy = currentPolicy();
        return new PasswordPolicyProjection(policy.minimumLength(), policy.maximumLength(), REQUIRED_CLASSES,
            policy.allowedSpecialCharacters());
    }

    private String generate(PasswordPolicy policy) {
        PasswordPolicy.Generator generator = policy.generator();
        List<Character> characters = new ArrayList<>(generator.length());
        characters.add(randomCharacter(generator.uppercaseCharacters()));
        characters.add(randomCharacter(generator.lowercaseCharacters()));
        characters.add(randomCharacter(generator.digitCharacters()));
        characters.add(randomCharacter(generator.specialCharacters()));
        String allCharacters = generator.uppercaseCharacters() + generator.lowercaseCharacters()
            + generator.digitCharacters() + generator.specialCharacters();
        while (characters.size() < generator.length()) {
            characters.add(randomCharacter(allCharacters));
        }
        for (int index = characters.size() - 1; index > 0; index--) {
            int swapIndex = secureRandom.nextInt(index + 1);
            Character value = characters.get(index);
            characters.set(index, characters.get(swapIndex));
            characters.set(swapIndex, value);
        }
        StringBuilder password = new StringBuilder(characters.size());
        characters.forEach(password::append);
        String result = password.toString();
        if (!PasswordPolicyRules.validate(policy, result).isEmpty()) {
            throw new ServiceException("PASSWORD_POLICY_UNAVAILABLE");
        }
        return result;
    }

    private char randomCharacter(String pool) {
        return pool.charAt(secureRandom.nextInt(pool.length()));
    }
}
