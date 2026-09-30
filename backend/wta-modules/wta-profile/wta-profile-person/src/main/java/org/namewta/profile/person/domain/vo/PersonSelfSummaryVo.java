package org.namewta.profile.person.domain.vo;

/** 个人中心使用的认证状态、进行中申请与有效档案。 */
public record PersonSelfSummaryVo(String status, String returnReason,
    PersonApplicationVo currentApplication, PersonSelfProfileVo certifiedProfile) {
}
