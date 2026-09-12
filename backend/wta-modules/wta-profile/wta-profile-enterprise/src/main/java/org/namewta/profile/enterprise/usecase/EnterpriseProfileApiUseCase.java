package org.namewta.profile.enterprise.usecase;

import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.ResolvedMaterialOwner;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.SnapshotRelationship;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 企业模块跨模块 API 适配用例合同。 */
public interface EnterpriseProfileApiUseCase {

    /** 查询当前生效的企业档案绑定。 */
    Map<Long, ProfileBindingSummary> findActiveBindings(Set<Long> userIds);

    /** 锁定并解析企业材料所有者。 */
    Optional<ResolvedMaterialOwner> lockMaterialOwner(MaterialOwnerKey owner);

    /** 判断企业工作中材料所有者是否可编辑。 */
    boolean isWorkingEditable(MaterialOwnerKey owner);

    /** 判断企业材料快照关系是否存在。 */
    boolean hasSnapshotRelationship(SnapshotRelationship relationship);
}
