package org.namewta.oidc.adapter.gateway;


import org.namewta.oidc.port.OidcIdentityPort;
import org.namewta.profile.api.ProfileDisclosureService;
import org.namewta.profile.api.domain.ProfileDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.sso.api.SsoSessionAccess;
import org.namewta.sso.api.SsoSessionLifecycle;
import org.namewta.sso.api.SsoSessionSnapshot;
import org.namewta.system.api.AccountIdentityService;
import org.namewta.system.api.OssService;
import org.namewta.system.api.domain.AccountIdentity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 仅经 wta-api 读取各领域当前事实。 */
@Component
public class WtaOidcIdentityAdapter implements OidcIdentityPort {
    private final ObjectProvider<SsoSessionAccess> sessions;
    private final AccountIdentityService accounts;
    private final ProfileDisclosureService profiles;
    private final ObjectProvider<OssService> oss;
    private final ObjectProvider<SsoSessionLifecycle> lifecycle;

    /** 生产构造器显式注入持久会话锁公共API。 */
    @org.springframework.beans.factory.annotation.Autowired
    public WtaOidcIdentityAdapter(
            ObjectProvider<SsoSessionAccess> sessions,
            AccountIdentityService accounts,
            ProfileDisclosureService profiles,
            ObjectProvider<OssService> oss,
            ObjectProvider<SsoSessionLifecycle> lifecycle) {
        this.sessions = sessions;
        this.accounts = accounts;
        this.profiles = profiles;
        this.oss = oss;
        this.lifecycle = lifecycle;
    }

    /** 保留旧显式装配合同；缺持久锁的实例不能签发新凭据。 */
    public WtaOidcIdentityAdapter(
            ObjectProvider<SsoSessionAccess> sessions,
            AccountIdentityService accounts,
            ProfileDisclosureService profiles,
            ObjectProvider<OssService> oss) {
        this(sessions, accounts, profiles, oss, null);
    }

    /** 通过公共API持有中央会话锁，缺失实现时停止签发。 */
    @Override
    public void lockSession(String sid) {
        var api = lifecycle == null ? null : lifecycle.getIfAvailable();
        if (api == null) throw new IllegalStateException("中央会话锁不可用");
        api.requireActiveLocked(sid);
    }

    /** 读取可信 SSO 会话快照，缺失或失效时为空。 */
    public SsoSessionSnapshot session(String sid) {
        var access = sessions.getIfAvailable();
        return access == null ? null : access.current(sid);
    }

    /** 读取当前正常且未删除的 WTA 账户。 */
    public AccountIdentity account(Long id) {
        return accounts.findActiveById(id);
    }

    /** 删除指定 SSO 会话，不扩大到其他浏览器。 */
    public void logout(String sid) {
        var access = sessions.getIfAvailable();
        if (access != null) access.logout(sid);
    }

    /** 仅向资料模块请求已经核准的字段集合。 */
    public ProfileDisclosure profile(Long id, Set<ProfileDisclosureField> fields) {
        return profiles.findByUserId(id, fields);
    }

    /** 解析已获准披露的头像地址，缺失头像时为空。 */
    public String picture(Long id) {
        return id == null || oss.getIfAvailable() == null
                ? null
                : oss.getObject().selectUrlByIds(id.toString());
    }
}
