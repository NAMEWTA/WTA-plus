package org.namewta.profile.person.domain.vo;

import java.time.Instant;

/** 本人当前有效绑定的认证档案。 */
public record PersonSelfProfileVo(long profileId, Instant verifiedAt, PersonSelfIdentityVo identity) {
}
