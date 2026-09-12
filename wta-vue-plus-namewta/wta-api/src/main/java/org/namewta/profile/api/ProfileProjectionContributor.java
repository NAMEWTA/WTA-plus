package org.namewta.profile.api;

import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileType;

import java.util.Map;
import java.util.Set;

/**
 * 一类档案对公共 ProfileService 的批量投影贡献端口。
 */
public interface ProfileProjectionContributor {

    ProfileType profileType();

    /**
     * 只返回未注销 current 档案与 active 绑定形成的有效投影。
     */
    Map<Long, ProfileBindingSummary> findActiveBindings(Set<Long> userIds);
}
