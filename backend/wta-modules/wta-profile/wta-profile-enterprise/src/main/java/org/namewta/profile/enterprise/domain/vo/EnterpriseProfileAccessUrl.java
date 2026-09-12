package org.namewta.profile.enterprise.domain.vo;

import java.time.Instant;

/**
 * 企业档案材料访问地址响应。
 *
 * @param accessType 访问类型
 * @param url 访问地址
 * @param expiresAt 私有地址过期时间
 * @param fileName 文件名
 */
public record EnterpriseProfileAccessUrl(String accessType, String url, Instant expiresAt, String fileName) {
}
