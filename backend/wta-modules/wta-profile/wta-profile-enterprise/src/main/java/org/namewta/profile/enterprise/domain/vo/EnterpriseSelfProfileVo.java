package org.namewta.profile.enterprise.domain.vo;

import java.time.Instant;

/** 本人当前有效绑定的认证档案。 */
public record EnterpriseSelfProfileVo(long profileId, Instant verifiedAt, EnterpriseSelfIdentityVo identity) {
}
