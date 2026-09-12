package org.namewta.profile.person.domain.vo;

/** PersonAdminResultVo 对外返回模型。 */
public record PersonAdminResultVo(String status, long profileId, Long versionId, Long bindingId, int version) {
}
