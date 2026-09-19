package org.namewta.profile.person.domain.vo;

/** 当前申请条件下的必填材料标签和最少份数，仅供页面提示。 */
public record ProfileMaterialRequirementVo(String materialTagCode, int minimumCount) {
}
