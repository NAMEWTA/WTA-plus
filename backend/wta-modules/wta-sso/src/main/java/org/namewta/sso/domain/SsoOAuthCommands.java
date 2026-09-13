package org.namewta.sso.domain;

import org.namewta.sso.api.SsoAuthenticatedUser;

/**
 * OAuth 授权与换票命令。
 */
public final class SsoOAuthCommands {

    private SsoOAuthCommands() {
    }

    /**
     * 授权请求。
     */
    public record AuthorizeCommand(String responseType, String clientId, String redirectUri, String state,
                                   String codeChallenge, String codeChallengeMethod, SsoAuthenticatedUser user) {
    }

    /**
     * 授权结果。
     */
    public record AuthorizeResult(boolean loginRequired, String redirectUri) {
        /**
         * 需要先登录。
         *
         * @return 结果
         */
        public static AuthorizeResult needsLogin() {
            return new AuthorizeResult(true, null);
        }

        /**
         * 回调重定向。
         *
         * @param redirectUri 回调
         * @return 结果
         */
        public static AuthorizeResult redirect(String redirectUri) {
            return new AuthorizeResult(false, redirectUri);
        }
    }

    /**
     * 换票请求。
     */
    public record TokenCommand(String grantType, String code, String redirectUri, String clientId, String codeVerifier) {
    }
}
