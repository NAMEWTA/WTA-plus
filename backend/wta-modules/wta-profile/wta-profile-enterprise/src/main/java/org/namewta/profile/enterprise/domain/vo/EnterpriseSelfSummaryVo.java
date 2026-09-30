package org.namewta.profile.enterprise.domain.vo;

/** 个人中心使用的认证状态、进行中申请与有效档案。 */
public record EnterpriseSelfSummaryVo(String status, String returnReason,
    EnterpriseApplicationVo currentApplication, EnterpriseSelfProfileVo certifiedProfile) {
}
