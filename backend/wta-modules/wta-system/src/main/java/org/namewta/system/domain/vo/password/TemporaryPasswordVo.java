package org.namewta.system.domain.vo.password;

/**
 * 一次性临时密码签发响应。
 *
 * @param password         仅本次响应展示的明文
 * @param expiresInSeconds 有效秒数
 */
public record TemporaryPasswordVo(String password, long expiresInSeconds) {
    @Override
    public String toString() {
        return "TemporaryPasswordVo[password=<redacted>, expiresInSeconds=" + expiresInSeconds + "]";
    }
}
