package org.namewta.sso.usecase.impl;

import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.api.SsoSessionSnapshot;
import org.namewta.sso.service.SsoSessionService;
import org.namewta.sso.usecase.SsoSessionUseCase;
import org.springframework.stereotype.Service;

/** SSO 会话用例编排。 */
@Service
public class SsoSessionUseCaseImpl implements SsoSessionUseCase {

    private final SsoSessionService sessionService;
    private final org.namewta.sso.service.SsoPersistentSessionService persistent;
    private final java.util.List<org.namewta.sso.api.SsoSessionRevocationParticipant> participants;
    private final org.namewta.sso.config.SsoProperties properties;

    /** 装配持久撤销参与者，所有参与者只写同一主库。 */
    @org.springframework.beans.factory.annotation.Autowired
    public SsoSessionUseCaseImpl(
            SsoSessionService service,
            org.namewta.sso.service.SsoPersistentSessionService persistent,
            java.util.List<org.namewta.sso.api.SsoSessionRevocationParticipant> participants,
            org.namewta.sso.config.SsoProperties properties) {
        this.sessionService = service;
        this.persistent = persistent;
        this.participants = participants;
        this.properties = properties;
    }

    /** 保留显式旧装配；生产始终使用完整持久构造器。 */
    public SsoSessionUseCaseImpl(SsoSessionService service) {
        this(service, null, java.util.List.of(), new org.namewta.sso.config.SsoProperties());
    }

    /** {@inheritDoc} */
    @Override
    @com.baomidou.dynamic.datasource.annotation.DSTransactional
    public String login(String username, String password) {
        if (!properties.isEnabled())
            throw new org.namewta.common.core.exception.ServiceException("中央认证已停用");
        String sid = sessionService.login(username, password);
        if (persistent != null) persistent.create(sid, sessionService.current(sid));
        return sid;
    }

    /** {@inheritDoc} */
    @Override
    public SsoAuthenticatedUser current(String sessionId) {
        return persistent == null
                ? sessionService.current(sessionId)
                : persistent.current(sessionId);
    }

    @Override
    public SsoSessionSnapshot currentSnapshot(String sessionId) {
        if (persistent == null) return sessionService.currentSnapshot(sessionId);
        var user = persistent.current(sessionId);
        return user == null
                ? null
                : new SsoSessionSnapshot(
                        user.getUserId(),
                        user.getUsername(),
                        user.getAuthenticatedAt(),
                        user.getExpiresAt());
    }

    /** {@inheritDoc} */
    @Override
    @com.baomidou.dynamic.datasource.annotation.DSTransactional
    public void logout(String sessionId) {
        if (persistent != null) {
            String hash = persistent.revoke(sessionId);
            if (hash != null) participants.forEach(p -> p.revoke(hash));
        }
        // Redis删除只清理缓存，提交失败仍以主库事实为准；重试不依赖该删除成功。
        if (persistent == null) sessionService.logout(sessionId);
    }
}
