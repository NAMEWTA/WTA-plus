package org.namewta.sso.service;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.dao.SsoSessionDao;
import org.namewta.sso.domain.SsoBusinessSession;
import org.namewta.sso.domain.SsoSession;
import org.namewta.sso.support.SsoBearerTokens;
import org.namewta.sso.support.SsoSessionHashes;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** 中央会话事实和业务票关联；跨模块只能经过公开API。 */
@Service
@RequiredArgsConstructor
public class SsoPersistentSessionService {
    private final SsoSessionDao dao;
    private final SocialSecretCipher cipher;

    /** 记录新中央会话，数据库不保存可冒用的原始Cookie。 */
    public void create(String sid, SsoAuthenticatedUser user) {
        var row = new SsoSession();
        row.setSessionId(IdGeneratorUtil.nextLongId());
        row.setSessionHash(SsoSessionHashes.hash(sid));
        row.setUserId(user.getUserId());
        row.setUsername(user.getUsername());
        row.setAuthenticatedAt(user.getAuthenticatedAt());
        row.setExpiresAt(user.getExpiresAt());
        row.setStatus("ACTIVE");
        row.setVersion(0);
        row.setDelFlag("0");
        dao.insert(row);
    }

    /** 每次读取都核对持久撤销事实，旧Redis缓存不能复活会话。 */
    public SsoAuthenticatedUser current(String sid) {
        if (!SsoBearerTokens.isValid(sid)) return null;
        var row = dao.find(SsoSessionHashes.hash(sid));
        if (!active(row)) return null;
        var user = new SsoAuthenticatedUser(row.getUserId(), row.getUsername());
        user.setAuthenticatedAt(row.getAuthenticatedAt());
        user.setExpiresAt(row.getExpiresAt());
        return user;
    }

    /** 签发及登记期间持有会话行锁，阻止退出枚举后新增授权。 */
    public void requireLocked(String sid) {
        if (!SsoBearerTokens.isValid(sid)) throw new ServiceException("中央会话已失效");
        requireHashLocked(SsoSessionHashes.hash(sid));
    }

    /** 持久授权码只保存摘要，换票时以摘要取得同一行锁。 */
    public void requireHashLocked(String hash) {
        if (hash == null || !active(dao.lock(hash))) throw new ServiceException("中央会话已失效");
    }

    /** 锁定并幂等撤销；返回摘要给同事务协议参与者。 */
    public String revoke(String sid) {
        if (!SsoBearerTokens.isValid(sid)) return null;
        String hash = SsoSessionHashes.hash(sid);
        if (dao.lock(hash) == null) return null;
        dao.revoke(hash);
        return hash;
    }

    /** 在会话锁保护下保存待全退的业务票；不把明文写入数据库。 */
    public void register(String sessionHash, String token, String clientId) {
        requireHashLocked(sessionHash);
        String hash = SsoSessionHashes.hash(token);
        var row = new SsoBusinessSession();
        row.setBusinessSessionId(IdGeneratorUtil.nextLongId());
        row.setSessionHash(sessionHash);
        row.setClientId(clientId);
        row.setTokenHash(hash);
        row.setEncryptedToken(cipher.encrypt("sso-business:" + hash, token));
        row.setStatus("ACTIVE");
        row.setVersion(0);
        row.setDelFlag("0");
        dao.register(row);
    }

    /** 返回维护或自然过期待撤销的会话摘要，不暴露原始cookie。 */
    public java.util.List<String> activeHashes(boolean expired) {
        return dao.active(expired).stream().map(SsoSession::getSessionHash).toList();
    }

    /** 结构切换必须先结束仍有效的会话。 */
    public boolean hasActive() {
        return dao.hasActive();
    }

    /** 维护路径使用已持久化摘要，仍遵循会话行锁序。 */
    public void revokeHash(String hash) {
        if (dao.lock(hash) != null) dao.revoke(hash);
    }

    /** 返回持久重试队列，不依赖进程内事件。 */
    public List<SsoBusinessSession> pending() {
        return dao.pending();
    }

    /** 仅注销适配器消费的服务端令牌。 */
    public String token(SsoBusinessSession row) {
        return cipher.decrypt("sso-business:" + row.getTokenHash(), row.getEncryptedToken());
    }

    /** 注销成功后关闭任务。 */
    public void delivered(Long id) {
        dao.delivered(id);
    }

    /** 固定截止和撤销状态共同定义有效中央会话。 */
    private boolean active(SsoSession row) {
        return row != null
                && "ACTIVE".equals(row.getStatus())
                && row.getExpiresAt().isAfter(Instant.now());
    }
}
