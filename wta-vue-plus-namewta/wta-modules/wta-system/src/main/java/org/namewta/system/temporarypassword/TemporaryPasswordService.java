package org.namewta.system.temporarypassword;

import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.system.password.PasswordPolicyService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 临时密码签发、校验与单次消费服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemporaryPasswordService {

    public static final long EXPIRES_IN_SECONDS = 60;
    private static final Duration TIME_TO_LIVE = Duration.ofSeconds(EXPIRES_IN_SECONDS);

    private final PasswordPolicyService passwordPolicyService;
    private final TemporaryPasswordStore temporaryPasswordStore;

    /**
     * 为用户签发新的临时密码；重新签发会覆盖旧值并重置 TTL。
     */
    public IssuedPassword issue(Long userId) {
        String password = passwordPolicyService.generateTemporaryPassword();
        temporaryPasswordStore.store(userId, BCrypt.hashpw(password), TIME_TO_LIVE);
        return new IssuedPassword(password, EXPIRES_IN_SECONDS);
    }

    /**
     * 校验候选值但不消费。Redis 故障和无值均失败关闭。
     */
    public Optional<VerifiedPassword> verify(Long userId, String candidate) {
        try {
            String passwordHash = temporaryPasswordStore.read(userId);
            if (passwordHash == null || !BCrypt.checkpw(candidate, passwordHash)) {
                return Optional.empty();
            }
            return Optional.of(new VerifiedPassword(userId, passwordHash));
        } catch (RuntimeException exception) {
            log.warn("临时密码校验存储不可用: userId={}, error={}", userId,
                exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * 原子消费此前已验证的存储值；重新签发、过期或并发赢家都会令消费失败。
     */
    public boolean consume(VerifiedPassword verifiedPassword) {
        try {
            return temporaryPasswordStore.compareAndDelete(
                verifiedPassword.userId, verifiedPassword.passwordHash);
        } catch (RuntimeException exception) {
            log.warn("临时密码消费存储不可用: userId={}, error={}", verifiedPassword.userId,
                exception.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * 一次展示的签发结果。
     */
    public record IssuedPassword(String password, long expiresInSeconds) {
        @Override
        public String toString() {
            return "IssuedPassword[password=<redacted>, expiresInSeconds=" + expiresInSeconds + "]";
        }
    }

    /**
     * 仅在认证请求内部存活的校验凭据。
     */
    public static final class VerifiedPassword {
        private final Long userId;
        private final String passwordHash;

        private VerifiedPassword(Long userId, String passwordHash) {
            this.userId = userId;
            this.passwordHash = passwordHash;
        }

        @Override
        public String toString() {
            return "VerifiedPassword[userId=" + userId + ", passwordHash=<redacted>]";
        }
    }
}
