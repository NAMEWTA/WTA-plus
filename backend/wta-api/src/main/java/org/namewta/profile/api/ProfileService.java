package org.namewta.profile.api;

import org.namewta.profile.api.domain.ProfileSummary;

import java.util.Collection;
import java.util.Map;

/**
 * 跨模块账户档案只读服务。
 */
public interface ProfileService {

    /**
     * 查询单个账户的有效档案摘要。
     */
    default ProfileSummary findByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        ProfileSummary summary = findByUserIds(java.util.List.of(userId)).get(userId);
        return summary == null ? ProfileSummary.unverified(userId) : summary;
    }

    /**
     * 批量查询账户的有效档案摘要。实现必须批量读取，避免逐账户查询。
     */
    Map<Long, ProfileSummary> findByUserIds(Collection<Long> userIds);
}
