package org.namewta.profile.person.usecase;

import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.ResolvedMaterialOwner;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.SnapshotRelationship;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityLock;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityMatch;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityQuery;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 个人模块跨模块 API 适配用例合同。
 *
 * <p>该合同只编排个人只读服务，避免 API 适配器越过 UseCase 访问 DAO。</p>
 */
public interface PersonProfileApiUseCase {

    /** 查询当前生效的个人档案绑定。 */
    Map<Long, ProfileBindingSummary> findActiveBindings(Set<Long> userIds);

    /** 锁定并解析个人材料所有者。 */
    Optional<ResolvedMaterialOwner> lockMaterialOwner(MaterialOwnerKey owner);

    /** 判断个人工作中材料所有者是否可编辑。 */
    boolean isWorkingEditable(MaterialOwnerKey owner);

    /** 判断个人材料快照关系是否存在。 */
    boolean hasSnapshotRelationship(SnapshotRelationship relationship);

    /** 查询个人有效身份精确匹配。 */
    List<ActiveIdentityMatch> findActiveExactMatches(ActiveIdentityQuery query);

    /** 锁定个人有效身份精确匹配。 */
    Optional<ActiveIdentityMatch> lockActiveExactMatch(ActiveIdentityLock query);
}
