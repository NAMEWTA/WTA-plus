package org.namewta.web.domain.vo;

/** 浏览器仅保存本站事务凭据；PKCE verifier 和客户端 Secret 留在服务端。 */
public record ExternalAuthorizeVo(
        String authorizationUrl, String state, String transactionKey, long expiresIn) {}
