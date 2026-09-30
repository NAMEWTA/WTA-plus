package org.namewta.profile.api;

import java.util.Set;
import org.namewta.profile.api.domain.ProfileDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.profile.api.domain.ProfileType;

/** 各档案子域的显式字段发布贡献端口；仅能返回自身子域的当前有效投影。 */
public interface ProfileDisclosureContributor {
    /** 返回本贡献者的档案子域。 */
    ProfileType profileType();

    /**
     * 读取已授权主体和字段；没有有效绑定时只返回获准的 false 认证状态。
     * @param userId 已认证账户主键
     * @param fields 本子域已核准字段
     * @return 自身子域的投影，不得返回其他主体或其他子域数据
     */
    ProfileDisclosure findByUserId(Long userId, Set<ProfileDisclosureField> fields);
}

