package org.namewta.web.service.social;

import cn.dev33.satoken.stp.StpUtil;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.common.social.oidc.OidcIdentity;
import org.namewta.system.api.model.ExternalAuthRegistration;
import org.namewta.web.domain.vo.LoginVo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

/** 无 sid 登录也必须完成退出资料保存，失败不能留下已签发但未登记的业务令牌。 */
@Tag("dev")
class ExternalAuthSessionStoreTest {

    @Test
    void missingSidDoesNotBypassSessionFailureCompensation() {
        var store = new ExternalAuthSessionStore(new SocialSecretCipher(""));
        AtomicReference<String> sessionId = new AtomicReference<>();
        try (var tokens = mockStatic(StpUtil.class)) {
            assertThatThrownBy(() -> store.issue(registration("OIDC"), identity(null), key -> {
                sessionId.set(key);
                return token();
            })).isInstanceOf(IllegalStateException.class).hasMessageContaining("AUTH_CONFIG_ROOT_KEY");
            assertThat(sessionId.get()).matches("[A-Za-z0-9_-]{43}");
            tokens.verify(() -> StpUtil.logoutByTokenValue("owned-business-token"));
        }
    }

    @Test
    void justAuthStillIssuesWithoutAnOidcSessionOrRootKey() {
        var store = new ExternalAuthSessionStore(new SocialSecretCipher(""));
        var result = store.issue(registration("GITHUB"), identity(null), key -> {
            assertThat(key).isNull();
            return token();
        });
        assertThat(result.getAccessToken()).isEqualTo("owned-business-token");
        assertThat(result.isGlobalLogoutAvailable()).isFalse();
    }

    private static ExternalAuthRegistration registration(String protocol) {
        return new ExternalAuthRegistration(1, 1, 1, "external", protocol, "https://issuer.test",
                "External", null, "business-home", "external-home", "secret",
                "https://home.test/social-callback", "https://home.test/logout/callback",
                List.of("openid"), "BIND_ONLY", Map.of());
    }

    private static OidcIdentity identity(String sid) {
        return new OidcIdentity("https://issuer.test", "subject", sid, "User", null, null,
                "owned-id-token", "https://issuer.test/logout", 1);
    }

    private static LoginVo token() {
        var token = new LoginVo();
        token.setAccessToken("owned-business-token");
        token.setExpireIn(3600L);
        return token;
    }
}
