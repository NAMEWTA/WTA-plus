package org.namewta.oidc.port;

import org.namewta.profile.api.domain.ProfileDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.sso.api.SsoSessionSnapshot;
import org.namewta.system.api.domain.AccountIdentity;

import java.util.Set;

/** 当前 WTA 账户、SSO 会话与核定档案的外部端口。 */
public interface OidcIdentityPort {
    /** 读取可信 SSO 会话快照，缺失或失效时为空。 */
    SsoSessionSnapshot session(String sid);

    /** 读取当前正常且未删除的 WTA 账户。 */
    AccountIdentity account(Long userId);

    /** 删除指定 SSO 会话，不扩大到其他浏览器。 */
    void logout(String sid);

    /** 签发事务锁定中央会话；生产适配器必须实现持久锁。 */
    default void lockSession(String sid) {
        throw new IllegalStateException("中央会话锁适配器未安装");
    }

    /** 仅向资料模块请求已经核准的字段集合。 */
    ProfileDisclosure profile(Long userId, Set<ProfileDisclosureField> fields);

    /** 解析已获准披露的头像地址，缺失头像时为空。 */
    String picture(Long avatarId);
}
