package org.namewta.profile.api;

import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileSummary;
import org.namewta.profile.api.domain.ProfileType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 将个人与企业批量投影组合成稳定公共摘要的基础实现。
 */
public final class CompositeProfileService implements ProfileService {

    private final List<ProfileProjectionContributor> contributors;

    public CompositeProfileService(List<ProfileProjectionContributor> contributors) {
        this.contributors = contributors == null ? List.of() : List.copyOf(contributors);
    }

    @Override
    public Map<Long, ProfileSummary> findByUserIds(Collection<Long> userIds) {
        Set<Long> requestedIds = normalize(userIds);
        if (requestedIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ProfileSummary> summaries = new LinkedHashMap<>();
        requestedIds.forEach(userId -> summaries.put(userId, ProfileSummary.unverified(userId)));
        for (ProfileProjectionContributor contributor : contributors) {
            merge(summaries, requestedIds, Objects.requireNonNull(contributor, "contributor"));
        }
        return Map.copyOf(summaries);
    }

    private static Set<Long> normalize(Collection<Long> userIds) {
        Objects.requireNonNull(userIds, "userIds");
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("userIds must contain only positive values");
            }
            normalized.add(userId);
        }
        return Set.copyOf(normalized);
    }

    private static void merge(Map<Long, ProfileSummary> summaries, Set<Long> requestedIds,
                              ProfileProjectionContributor contributor) {
        ProfileType contributorType = Objects.requireNonNull(contributor.profileType(), "profileType");
        Map<Long, ProfileBindingSummary> projections = contributor.findActiveBindings(requestedIds);
        if (projections == null || projections.isEmpty()) {
            return;
        }
        projections.forEach((userId, binding) -> {
            if (!requestedIds.contains(userId)) {
                throw new IllegalStateException(contributorType + " contributor returned an unrequested user");
            }
            if (binding == null || binding.profileType() != contributorType) {
                throw new IllegalStateException(contributorType + " contributor returned a mismatched projection");
            }
            ProfileSummary current = summaries.get(userId);
            ProfileBindingSummary existing = contributorType == ProfileType.PERSON
                ? current.person() : current.enterprise();
            if (existing != null) {
                throw new IllegalStateException(contributorType + " contributor returned duplicate active bindings");
            }
            summaries.put(userId, contributorType == ProfileType.PERSON
                ? new ProfileSummary(userId, binding, current.enterprise())
                : new ProfileSummary(userId, current.person(), binding));
        });
    }
}
