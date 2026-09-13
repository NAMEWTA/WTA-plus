package org.namewta.sso.service;

import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.sso.api.SsoClientView;
import org.namewta.sso.domain.SsoAuthorizationCode;
import org.namewta.sso.domain.SsoOAuthCommands;
import org.namewta.sso.dao.SsoAuthorizationCodeDao;
import org.namewta.sso.port.SsoBusinessTokenPort;
import org.namewta.sso.port.SsoClientCatalogPort;
import org.namewta.sso.port.SsoIdentityPort;
import org.namewta.sso.support.PkceS256;
import org.namewta.system.api.model.LoginUser;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Authorization Code + PKCE 核心协议：签发、换票与负向拒绝。
 */
public class SsoAuthorizationService {

    private final SsoAuthorizationCodeDao codeDao;
    private final SsoClientCatalogPort clientCatalog;
    private final SsoIdentityPort identityPort;
    private final SsoBusinessTokenPort tokenPort;
    private final Clock clock;
    private final Duration codeTtl;

    /**
     * 组装授权服务。
     *
     * @param codeDao       授权码 DAO
     * @param clientCatalog Client 目录
     * @param identityPort  认人与准入
     * @param tokenPort     业务 Token
     * @param clock         时钟
     * @param codeTtl       授权码 TTL
     */
    public SsoAuthorizationService(SsoAuthorizationCodeDao codeDao,
                                   SsoClientCatalogPort clientCatalog,
                                   SsoIdentityPort identityPort,
                                   SsoBusinessTokenPort tokenPort,
                                   Clock clock,
                                   Duration codeTtl) {
        this.codeDao = codeDao;
        this.clientCatalog = clientCatalog;
        this.identityPort = identityPort;
        this.tokenPort = tokenPort;
        this.clock = clock;
        this.codeTtl = codeTtl;
    }

    /**
     * 校验 PKCE/白名单/state 后签发一次性 code。
     *
     * @param command 授权请求
     * @return 登录或回调结果
     */
    public SsoOAuthCommands.AuthorizeResult authorize(SsoOAuthCommands.AuthorizeCommand command) {
        if (command == null) {
            throw new ServiceException("授权请求无效");
        }
        if (!"code".equalsIgnoreCase(StringUtils.trim(command.responseType()))) {
            throw new ServiceException("只支持 response_type=code");
        }
        SsoClientView client = requireEnabledClient(command.clientId());
        requireExactRedirect(client, command.redirectUri());
        PkceS256.requireChallenge(command.codeChallenge());
        PkceS256.requireS256(command.codeChallengeMethod());
        if (StringUtils.isBlank(command.state())) {
            throw new ServiceException("缺少 state");
        }
        if (command.user() == null) {
            return SsoOAuthCommands.AuthorizeResult.needsLogin();
        }
        identityPort.assertClientAccess(command.user().getUserId(), client.getClientId());
        SsoAuthorizationCode code = new SsoAuthorizationCode();
        code.setAuthorizationCodeId(IdGeneratorUtil.nextLongId());
        code.setAuthorizationCode(newCode());
        code.setClientId(client.getClientId());
        code.setRedirectUri(command.redirectUri().trim());
        code.setCodeChallenge(command.codeChallenge().trim());
        code.setState(command.state().trim());
        code.setUserId(command.user().getUserId());
        code.setUsername(command.user().getUsername());
        code.setConsumed(Boolean.FALSE);
        code.setExpireTime(LocalDateTime.now(clock).plus(codeTtl));
        code.setVersion(0);
        codeDao.insert(code);
        return SsoOAuthCommands.AuthorizeResult.redirect(appendQuery(command.redirectUri().trim(), code.getAuthorizationCode(), command.state().trim()));
    }

    /**
     * 用 verifier 换取目标业务 Client 的 Sa-Token。
     *
     * @param command 换票请求
     * @return 令牌
     */
    public SsoBusinessTokenPort.IssuedToken exchange(SsoOAuthCommands.TokenCommand command) {
        if (command == null) {
            throw new ServiceException("换票请求无效");
        }
        if (!"authorization_code".equalsIgnoreCase(StringUtils.trim(command.grantType()))) {
            throw new ServiceException("只支持 grant_type=authorization_code");
        }
        if (StringUtils.isBlank(command.code())) {
            throw new ServiceException("缺少 code");
        }
        SsoAuthorizationCode record = codeDao.findByCode(command.code());
        if (record == null) {
            throw new ServiceException("授权码无效");
        }
        if (Boolean.TRUE.equals(record.getConsumed())) {
            throw new ServiceException("授权码已使用");
        }
        if (record.getExpireTime() == null || !record.getExpireTime().isAfter(LocalDateTime.now(clock))) {
            throw new ServiceException("授权码已过期");
        }
        if (!StringUtils.equals(record.getClientId(), command.clientId())) {
            throw new ServiceException("授权码与客户端不匹配");
        }
        if (!StringUtils.equals(record.getRedirectUri(), StringUtils.trim(command.redirectUri()))) {
            throw new ServiceException("授权码与回调地址不匹配");
        }
        PkceS256.verify(command.codeVerifier(), record.getCodeChallenge());
        if (!codeDao.consumeIfUnconsumed(record.getAuthorizationCode(), record.getVersion())) {
            throw new ServiceException("授权码已使用");
        }
        SsoClientView client = requireEnabledClient(record.getClientId());
        LoginUser loginUser = identityPort.buildLoginUser(record.getUserId(), client.getClientId());
        return tokenPort.issue(loginUser, client);
    }

    /**
     * 只撤销提交的业务令牌。
     *
     * @param token 令牌
     */
    public void revoke(String token) {
        tokenPort.revoke(token);
    }

    private SsoClientView requireEnabledClient(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            throw new ServiceException("缺少 client_id");
        }
        SsoClientView client = clientCatalog.findByClientId(clientId);
        if (client == null || !SystemConstants.NORMAL.equals(client.getStatus())) {
            throw new ServiceException("客户端不可用");
        }
        if (!Boolean.TRUE.equals(client.getSsoEnabled())) {
            throw new ServiceException("客户端未启用 SSO");
        }
        return client;
    }

    private static void requireExactRedirect(SsoClientView client, String redirectUri) {
        if (StringUtils.isBlank(redirectUri)) {
            throw new ServiceException("缺少 redirect_uri");
        }
        String value = redirectUri.trim();
        if (value.contains("*")) {
            throw new ServiceException("SSO 回调白名单禁止通配符");
        }
        List<String> allowed = client.getRedirectUris();
        if (allowed == null || allowed.stream().noneMatch(value::equals)) {
            throw new ServiceException("回调地址未登记");
        }
    }

    private static String newCode() {
        byte[] bytes = new byte[32];
        ThreadLocalRandom.current().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String appendQuery(String redirectUri, String code, String state) {
        String separator = redirectUri.contains("?") ? "&" : "?";
        return redirectUri + separator + "code=" + code + "&state=" + state;
    }

}
