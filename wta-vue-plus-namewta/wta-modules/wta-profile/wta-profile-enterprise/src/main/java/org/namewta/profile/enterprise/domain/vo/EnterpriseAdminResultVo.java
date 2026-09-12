package org.namewta.profile.enterprise.domain.vo;

/** EnterpriseAdminResultVo 对外返回模型。 */
public record EnterpriseAdminResultVo(String status, long profileId, Long versionId, Long bindingId, int version) {
}
