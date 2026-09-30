package org.namewta.oidc.domain.vo;

/** 浏览器登录事务的最小展示信息。 */
public record OidcLoginContextVo(String applicationName, boolean forceLogin) {}
