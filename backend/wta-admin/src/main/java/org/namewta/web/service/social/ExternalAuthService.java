package org.namewta.web.service.social;

import cn.dev33.satoken.stp.StpUtil;

import lombok.RequiredArgsConstructor;

import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;

import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.social.config.properties.SocialLoginConfigProperties;
import org.namewta.common.social.config.properties.SocialProperties;
import org.namewta.common.social.oidc.OidcClientSettings;
import org.namewta.common.social.oidc.OidcIdentity;
import org.namewta.common.social.oidc.OidcProtocolClient;
import org.namewta.common.social.utils.SocialUtils;
import org.namewta.system.api.ExternalAuthConfigurationService;
import org.namewta.system.api.model.ExternalAuthRegistration;
import org.namewta.system.api.model.SocialLoginBody;
import org.namewta.system.domain.policy.UserPhonePolicy;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.service.ClientUserTypeAccessService;
import org.namewta.system.service.ISysClientService;
import org.namewta.web.domain.bo.ExternalAuthorizeBo;
import org.namewta.web.domain.bo.ExternalRegisterBo;
import org.namewta.web.domain.vo.ExternalAuthorizeVo;
import org.namewta.web.domain.vo.LoginVo;
import org.namewta.web.service.IAuthStrategy;
import org.namewta.web.service.SysLoginService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** 将外部认证结果接入既有业务账号、Client 准入和 Sa-Token，会话不依赖 Provider 实现。 */
@Service
@RequiredArgsConstructor
public class ExternalAuthService {
    private final ExternalAuthConfigurationService configurations;
    private final OidcProtocolClient protocol;
    private final ExternalAuthStateStore transactions;
    private final ExternalAuthAccountService accounts;
    private final ExternalAuthSessionStore sessions;
    private final ISysClientService clientService;
    private final ClientUserTypeAccessService clientAccess;
    private final SysLoginService loginService;

    /** 先绑定目的及浏览器，再跳第三方；只允许回到本 App 的相对路由。 */
    public ExternalAuthorizeVo authorize(ExternalAuthorizeBo request) {
        SysClientVo client = requireClient(request.clientId());
        Long userId = null;
        if ("BIND".equals(request.purpose())) {
            requireCurrentClient(client);
            userId = LoginHelper.getUserId();
        }
        ExternalAuthRegistration registration =
                configurations.require(request.providerKey(), request.clientId());
        String returnPath = safeReturnPath(request.returnPath());
        String state = OidcProtocolClient.randomToken();
        String browserKey = OidcProtocolClient.randomToken();
        String nonce = OidcProtocolClient.randomToken();
        String verifier = OidcProtocolClient.randomToken();
        String url =
                "OIDC".equals(registration.protocol())
                        ? protocol.authorize(settings(registration), state, nonce, verifier)
                        : SocialUtils.getAuthRequest(
                                        source(registration), socialProperties(registration))
                                .authorize(state);
        transactions.save(
                state,
                new ExternalAuthStateStore.Transaction(
                        registration.providerKey(),
                        client.getClientId(),
                        registration.id(),
                        registration.version(),
                        registration.providerVersion(),
                        request.purpose(),
                        userId,
                        returnPath,
                        OidcProtocolClient.challenge(browserKey),
                        nonce,
                        verifier));
        return new ExternalAuthorizeVo(url, state, browserKey, 300);
    }

    /** 认证后先解析本地身份，再检查业务准入，不能直接把 Provider Token 当业务令牌。 */
    public LoginVo login(SocialLoginBody request, SysClientVo client) {
        requireTransactionKey(request);
        var transaction =
                transactions.consume(
                        request.getSocialState(),
                        request.getTransactionKey(),
                        client.getClientId(),
                        request.getSource(),
                        "LOGIN",
                        null);
        ExternalAuthRegistration registration = current(transaction);
        OidcIdentity identity = exchange(registration, transaction, request);
        Long userId = accounts.findUser(identity, registration.protocol());
        if (userId == null) {
            if (!"AUTO_REGISTER".equals(registration.firstLoginPolicy())
                    || !Boolean.TRUE.equals(client.getRegisterEnabled())) return bindingRequired();
            String phone = validPhone(identity.phoneNumber());
            if (phone == null) {
                LoginVo result = new LoginVo();
                result.setNextAction("COMPLETE_PROFILE");
                result.setClientId(client.getClientId());
                result.setRequiredFields(List.of("phoneNumber"));
                result.setRegistrationTicket(transactions.pending(transaction, identity));
                return result;
            }
            userId = registerAccount(client, registration, identity, phone);
            if (userId == null) return bindingRequired();
        }
        return signIn(client, registration, identity, userId);
    }

    /** 待补填身份只从服务端短期凭据恢复，禁止客户端指定任意外部身份。 */
    public LoginVo register(ExternalRegisterBo request) {
        String phone = UserPhonePolicy.requirePhone(request.phoneNumber());
        SysClientVo client = requireClient(request.clientId());
        var pending =
                transactions.consumeRegistration(
                        request.registrationTicket(),
                        request.transactionKey(),
                        client.getClientId());
        var registration = current(pending.transaction());
        if (!"AUTO_REGISTER".equals(registration.firstLoginPolicy())
                || !Boolean.TRUE.equals(client.getRegisterEnabled())) return bindingRequired();
        Long userId = registerAccount(client, registration, pending.identity(), phone);
        return userId == null
                ? bindingRequired()
                : signIn(client, registration, pending.identity(), userId);
    }

    /** 绑定只能由发起事务时的同一业务用户完成。 */
    public void bind(SocialLoginBody request) {
        requireTransactionKey(request);
        SysClientVo client = requireClient(request.getClientId());
        requireCurrentClient(client);
        Long userId = LoginHelper.getUserId();
        var transaction =
                transactions.consume(
                        request.getSocialState(),
                        request.getTransactionKey(),
                        client.getClientId(),
                        request.getSource(),
                        "BIND",
                        userId);
        var registration = current(transaction);
        OidcIdentity identity = exchange(registration, transaction, request);
        try {
            accounts.bind(userId, registration.providerKey(), registration.protocol(), identity);
        } catch (DuplicateKeyException e) {
            throw new ServiceException("该第三方身份已被绑定，请刷新后重试");
        }
    }

    /** 新旧解绑入口均使用当前用户身份以及统一账号事务与分布式锁。 */
    public void unbind(Long bindingId) {
        StpUtil.checkLogin();
        accounts.unbind(LoginHelper.getUserId(), bindingId);
    }

    public void backchannel(long registrationId, String logoutToken) {
        var registration = configurations.requireForLogout(registrationId);
        if (!"OIDC".equals(registration.protocol())) throw new ServiceException("不支持该退出通知");
        sessions.revoke(registration, protocol.logoutSession(settings(registration), logoutToken));
    }

    /** 使用当前应用可见的身份源名称展示绑定，旧身份源缺失时保留稳定标识。 */
    public List<ExternalAuthAccountService.Binding> bindings() {
        StpUtil.checkLogin();
        Object clientId = StpUtil.getExtra(LoginHelper.CLIENT_KEY);
        Map<String, String> names = new java.util.HashMap<>();
        if (clientId instanceof String value) {
            configurations
                    .listEnabled(value)
                    .forEach(entry -> names.put(entry.providerKey(), entry.name()));
        }
        return accounts.bindings(LoginHelper.getUserId()).stream()
                .map(
                        row ->
                                new ExternalAuthAccountService.Binding(
                                        row.id(),
                                        row.providerKey(),
                                        names.getOrDefault(row.providerKey(), row.name()),
                                        row.userName(),
                                        row.nickName(),
                                        row.createTime()))
                .toList();
    }

    /** 旧跳转合同无法承载浏览器事务凭据；切换后必须由当前登录页重新发起。 */
    public String legacyAuthorize(String providerKey, String clientId) {
        throw new ServiceException("旧第三方登录入口已停用，请更新客户端并从当前应用登录页面重新发起登录");
    }

    private static void requireTransactionKey(SocialLoginBody request) {
        if (request.getTransactionKey() == null || request.getTransactionKey().isBlank())
            throw new ServiceException("第三方登录缺少浏览器授权事务，请更新客户端并从当前应用登录页面重新发起登录");
    }

    private ExternalAuthRegistration current(ExternalAuthStateStore.Transaction transaction) {
        if (!configurations.isCurrent(
                transaction.registrationId(), transaction.version(), transaction.providerVersion()))
            throw new ServiceException("认证配置已变更，请重新发起登录");
        ExternalAuthRegistration registration =
                configurations.require(transaction.providerKey(), transaction.clientId());
        if (registration.id() != transaction.registrationId()
                || registration.version() != transaction.version()
                || registration.providerVersion() != transaction.providerVersion())
            throw new ServiceException("认证配置已变更，请重新登录");
        return registration;
    }

    private OidcIdentity exchange(
            ExternalAuthRegistration registration,
            ExternalAuthStateStore.Transaction transaction,
            SocialLoginBody request) {
        if ("OIDC".equals(registration.protocol()))
            return protocol.exchange(
                    settings(registration),
                    request.getSocialCode(),
                    transaction.verifier(),
                    transaction.nonce());
        AuthResponse<AuthUser> response =
                SocialUtils.loginAuth(
                        source(registration),
                        request.getSocialCode(),
                        request.getSocialState(),
                        socialProperties(registration));
        if (!response.ok() || response.getData() == null)
            throw new ServiceException("第三方登录失败，请重新登录");
        AuthUser user = response.getData();
        if (user.getUuid() == null || user.getUuid().isBlank())
            throw new ServiceException("第三方身份无效");
        return new OidcIdentity(
                "justauth:" + registration.protocol(),
                user.getUuid(),
                null,
                user.getNickname(),
                null,
                user.getEmail(),
                null,
                null,
                java.time.Instant.now().getEpochSecond());
    }

    private Long registerAccount(
            SysClientVo client,
            ExternalAuthRegistration registration,
            OidcIdentity identity,
            String phone) {
        try {
            return accounts.register(
                    client, registration.providerKey(), registration.protocol(), identity, phone);
        } catch (DuplicateKeyException e) {
            // 被并发请求抢先创建时，事务已回滚；只接受同一个精确外部身份。
            return accounts.findUser(identity, registration.protocol());
        }
    }

    private LoginVo signIn(
            SysClientVo client,
            ExternalAuthRegistration registration,
            OidcIdentity identity,
            Long userId) {
        return accounts.withBoundIdentity(
                userId,
                identity,
                registration.protocol(),
                () -> {
                    if (!configurations.isCurrent(
                            registration.id(),
                            registration.version(),
                            registration.providerVersion()))
                        throw new ServiceException("认证配置已变更，请重新登录");
                    var user = accounts.requireUser(userId);
                    var userType = clientAccess.requireLoginAccess(userId, client);
                    var loginUser = loginService.buildLoginUser(user, client, userType);
                    return sessions.issue(
                            registration,
                            identity,
                            sessionId -> {
                                String authSource =
                                        "OIDC".equals(registration.protocol()) ? "OIDC" : "SOCIAL";
                                var parameter =
                                        IAuthStrategy.buildLoginParameter(
                                                client,
                                                p -> {
                                                    p.setExtra(
                                                            ExternalAuthSessionStore.SOURCE_EXTRA,
                                                            authSource);
                                                    if (sessionId != null)
                                                        p.setExtra(
                                                                ExternalAuthSessionStore
                                                                        .SESSION_EXTRA,
                                                                sessionId);
                                                });
                                LoginHelper.login(loginUser, parameter);
                                LoginVo result = new LoginVo();
                                result.setAccessToken(StpUtil.getTokenValue());
                                result.setExpireIn(StpUtil.getTokenTimeout());
                                result.setClientId(client.getClientId());
                                result.setAuthSource(authSource);
                                return result;
                            });
                });
    }

    private SysClientVo requireClient(String clientId) {
        SysClientVo client = clientService.queryByClientId(clientId);
        if (client == null
                || !SystemConstants.NORMAL.equals(client.getStatus())
                || client.getGrantType() == null
                || Arrays.stream(client.getGrantType().split(","))
                        .map(String::strip)
                        .noneMatch("social"::equals)) throw new ServiceException("当前应用未启用第三方登录");
        return client;
    }

    private static void requireCurrentClient(SysClientVo client) {
        StpUtil.checkLogin();
        var user = LoginHelper.getLoginUser();
        if (user == null || !Objects.equals(user.getClientPk(), client.getId()))
            throw new ServiceException("绑定操作必须在当前应用内完成");
    }

    public static OidcClientSettings settings(ExternalAuthRegistration registration) {
        return new OidcClientSettings(
                registration.issuer(),
                registration.externalClientId(),
                registration.clientSecret(),
                registration.redirectUri(),
                registration.scopes(),
                registration.options().getOrDefault("authenticationMethod", "client_secret_basic"));
    }

    private static String source(ExternalAuthRegistration registration) {
        return registration.protocol().toLowerCase(Locale.ROOT);
    }

    /** 保留 JustAuth 已实现的平台适配器，只改变其配置来源。 */
    public static SocialProperties socialProperties(ExternalAuthRegistration registration) {
        SocialLoginConfigProperties config = new SocialLoginConfigProperties();
        config.setClientId(registration.externalClientId());
        config.setClientSecret(registration.clientSecret());
        config.setRedirectUri(registration.redirectUri());
        config.setScopes(registration.scopes());
        config.setServerUrl(registration.options().get("serverUrl"));
        config.setTenantId(registration.options().get("tenantId"));
        config.setAgentId(registration.options().get("agentId"));
        config.setAlipayPublicKey(registration.options().get("alipayPublicKey"));
        config.setStackOverflowKey(registration.options().get("stackOverflowKey"));
        config.setCodingGroupName(registration.options().get("codingGroupName"));
        config.setUnionId(Boolean.valueOf(registration.options().getOrDefault("unionId", "false")));
        SocialProperties properties = new SocialProperties();
        properties.setType(Map.of(source(registration), config));
        return properties;
    }

    static String safeReturnPath(String path) {
        if (path == null || path.isBlank()) return "/";
        if (!path.startsWith("/")
                || path.startsWith("//")
                || path.contains("\\")
                || path.chars().anyMatch(c -> c < 32)
                || path.toLowerCase(Locale.ROOT).matches(".*%(?:2f|5c|0d|0a).*"))
            throw new ServiceException("登录返回路径无效");
        return path;
    }

    private static String validPhone(String phone) {
        try {
            return UserPhonePolicy.requirePhone(phone);
        } catch (ServiceException e) {
            return null;
        }
    }

    private static LoginVo bindingRequired() {
        LoginVo result = new LoginVo();
        result.setNextAction("BIND_REQUIRED");
        result.setMessage("请先使用本地账号登录，再在账号设置中绑定该SSO身份；没有本地账号时可先注册。");
        return result;
    }
}
