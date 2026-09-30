package org.namewta.profile.api.domain;

/**
 * 一个账户的当前档案字段投影；当前模型每类最多一个有效绑定。
 * @param userId 已认证账户主键
 * @param person 已核准个人字段；未请求个人字段时为 null
 * @param enterprise 已核准企业字段；未请求企业字段时为 null
 */
public record ProfileDisclosure(Long userId, PersonDisclosure person, EnterpriseDisclosure enterprise) {
}

