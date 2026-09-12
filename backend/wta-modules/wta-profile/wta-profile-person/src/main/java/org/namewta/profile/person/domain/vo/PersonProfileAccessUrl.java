package org.namewta.profile.person.domain.vo;

import java.time.Instant;

/**
 * 个人档案材料访问地址响应。
 *
 * <p>该响应只描述 Profile 对外的 HTTP 数据，不暴露 system 模块的内部类型。</p>
 *
 * @param accessType 访问类型
 * @param url 访问地址
 * @param expiresAt 私有地址过期时间
 * @param fileName 文件名
 */
public record PersonProfileAccessUrl(String accessType, String url, Instant expiresAt, String fileName) {
}
