package org.namewta.sso.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;

import org.namewta.sso.service.SsoPersistentSessionService;
import org.springframework.stereotype.Service;

/** 为签发事务提供中央会话锁。 */
@Service
@RequiredArgsConstructor
public class SsoSessionLifecycleUseCase {
    private final SsoPersistentSessionService sessions;
    private final java.util.List<org.namewta.sso.api.SsoSessionRevocationParticipant> participants;
    private final org.namewta.sso.config.SsoProperties properties;

    /** 检查维护门槛，不返回用户清单。 */
    public boolean hasActiveSessions() {
        return sessions.hasActive();
    }

    /** 已停用中央登录时批量结束会话；每批采用独立public用例事务调用。 */
    @DSTransactional
    public void revokeAllSessions() {
        if (properties.isEnabled())
            throw new org.namewta.common.core.exception.ServiceException("请先停用中央认证");
        java.util.List<String> hashes;
        do {
            hashes = sessions.activeHashes(false);
            for (String hash : hashes) {
                sessions.revokeHash(hash);
                participants.forEach(p -> p.revoke(hash));
            }
        } while (!hashes.isEmpty());
    }

    /** 自然过期也持久登记关联应用退出，不能只让Redis静默消失。 */
    @DSTransactional
    public void expire() {
        for (String hash : sessions.activeHashes(true)) {
            sessions.revokeHash(hash);
            participants.forEach(p -> p.revoke(hash));
        }
    }

    /** 必须加入调用方事务，锁一直保持到授权持久化完成。 */
    @DSTransactional
    public void requireActiveLocked(String sid) {
        sessions.requireLocked(sid);
    }
}
