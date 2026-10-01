package org.namewta.common.social.oidc;

/** 经协议验证的外部身份。ID Token 仅供服务端退出使用，不得直接返回浏览器。 */
public record OidcIdentity(
        String issuer,
        String subject,
        String sessionId,
        String name,
        String phoneNumber,
        String email,
        String idToken,
        String endSessionEndpoint,
        long authenticatedAt) {
    @Override
    public String toString() {
        return "OidcIdentity[identity=REDACTED]";
    }
}
