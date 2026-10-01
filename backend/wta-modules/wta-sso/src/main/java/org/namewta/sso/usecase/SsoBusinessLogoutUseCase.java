package org.namewta.sso.usecase;

import lombok.RequiredArgsConstructor;

import org.namewta.sso.port.SsoBusinessTokenPort;
import org.namewta.sso.service.SsoPersistentSessionService;
import org.springframework.stereotype.Service;

/** 持久任务驱动第一方业务票注销，Redis短暂不可用时下轮重试。 */
@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class SsoBusinessLogoutUseCase {
    private final SsoPersistentSessionService sessions;
    private final SsoBusinessTokenPort tokens;

    /** 不持有数据库长事务执行外部Redis调用；注销本身按token幂等。 */
    public void dispatch() {
        for (var row : sessions.pending()) {
            try {
                tokens.revoke(sessions.token(row));
                sessions.delivered(row.getBusinessSessionId());
            } catch (RuntimeException e) {
                log.warn("SSO业务会话注销等待重试 id={}", row.getBusinessSessionId());
            }
        }
    }
}
