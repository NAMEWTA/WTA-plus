package org.namewta.system.temporarypassword;

import java.time.Duration;

/**
 * 临时密码不可逆校验值存储。
 */
public interface TemporaryPasswordStore {

    /**
     * 覆盖用户当前临时密码校验值及有效期。
     */
    void store(Long userId, String passwordHash, Duration timeToLive);

    /**
     * 读取用户当前临时密码校验值。
     */
    String read(Long userId);

    /**
     * 仅当存储值仍与已验证值相同时原子删除。
     */
    boolean compareAndDelete(Long userId, String expectedHash);
}
