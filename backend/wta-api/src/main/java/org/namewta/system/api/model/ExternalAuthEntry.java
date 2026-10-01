package org.namewta.system.api.model;

/** 浏览器可见的外部登录入口，不包含注册凭据或私有策略。 */
public record ExternalAuthEntry(String providerKey, String name, String icon, String protocol) { }
