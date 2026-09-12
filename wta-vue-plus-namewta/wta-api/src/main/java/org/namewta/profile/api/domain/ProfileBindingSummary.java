package org.namewta.profile.api.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * 当前有效档案绑定的非敏感投影。
 *
 * @param profileId  档案 ID
 * @param profileType 档案类型
 * @param verifiedAt 认证完成时间
 */
public record ProfileBindingSummary(Long profileId, ProfileType profileType, Instant verifiedAt) {

    public ProfileBindingSummary {
        if (profileId == null || profileId <= 0) {
            throw new IllegalArgumentException("profileId must be positive");
        }
        Objects.requireNonNull(profileType, "profileType");
        Objects.requireNonNull(verifiedAt, "verifiedAt");
    }
}
