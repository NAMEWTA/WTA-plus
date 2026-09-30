package org.namewta.oidc.adapter.api;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.usecase.OidcAuthorizationUseCase;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

/** Spring 协议状态持久化 SPI；原始 code 只在本次 request attribute 存在。 */
@Component
@RequiredArgsConstructor
public class OidcAuthorizationStore implements OAuth2AuthorizationService {
    /** 原始授权码仅在单次请求内传递，完成交换后由提供方清除。 */
    public static final String CONSUMED = OidcAuthorizationStore.class.getName() + ".consumed";

    private final OidcAuthorizationUseCase useCase;

    @Override
    /** 在框架验证后协调首次授权保存或一次性消费与签发。 */
    public void save(OAuth2Authorization value) {
        String raw = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a)
            raw = (String) a.getRequest().getAttribute(CONSUMED);
        useCase.save(value, raw);
    }

    @Override
    /** 删除短期上下文或撤销指定授权，不影响其他授权。 */
    public void remove(OAuth2Authorization value) {
        useCase.remove(value);
    }

    @Override
    /** 按协议客户端或授权标识查询当前记录。 */
    public OAuth2Authorization findById(String id) {
        return useCase.find(id);
    }

    @Override
    /** 按凭据摘要和指定类型定位授权，拒绝跨类型匹配。 */
    public OAuth2Authorization findByToken(String token, OAuth2TokenType type) {
        var value = useCase.token(token);
        if (value == null || type == null) return value;
        org.springframework.security.oauth2.core.OAuth2Token found =
                switch (type.getValue()) {
                    case "code" ->
                            value.getToken(OAuth2AuthorizationCode.class) == null
                                    ? null
                                    : value.getToken(OAuth2AuthorizationCode.class).getToken();
                    case "access_token" ->
                            value.getAccessToken() == null
                                    ? null
                                    : value.getAccessToken().getToken();
                    case "id_token" ->
                            value.getToken(
                                                    org.springframework.security.oauth2.core.oidc
                                                            .OidcIdToken.class)
                                            == null
                                    ? null
                                    : value.getToken(
                                                    org.springframework.security.oauth2.core.oidc
                                                            .OidcIdToken.class)
                                            .getToken();
                    default -> null;
                };
        return found != null && token.equals(found.getTokenValue()) ? value : null;
    }
}
