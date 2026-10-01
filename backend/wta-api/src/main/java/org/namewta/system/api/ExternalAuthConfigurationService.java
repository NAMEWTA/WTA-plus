package org.namewta.system.api;

import org.namewta.system.api.model.ExternalAuthEntry;
import org.namewta.system.api.model.ExternalAuthRegistration;
import java.util.List;

/** 外部身份源按业务客户端接入的运行时合同；业务 clientId 为 OAuth 字符串。 */
public interface ExternalAuthConfigurationService {
    /** 返回当前启用入口的最小公开投影，不包含外部客户端或凭据。 */
    List<ExternalAuthEntry> listEnabled(String businessClientId);

    /** 返回经数据库复核启用状态的服务端配置；不可序列化到浏览器或日志。 */
    ExternalAuthRegistration require(String providerKey, String businessClientId);

    /** 仅用于验证退出通知：保留停用或逻辑删除接入的历史身份配置，不可用于登录。 */
    ExternalAuthRegistration requireForLogout(long registrationId);

    /** 回调/后续操作复核接入、身份源版本及业务客户端仍可使用。 */
    boolean isCurrent(long id, long version, long providerVersion);
}
