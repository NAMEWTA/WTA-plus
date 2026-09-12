package org.namewta.profile.enterprise.usecase.impl;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.ResolvedMaterialOwner;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.SnapshotRelationship;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.enterprise.service.EnterpriseProfileApiService;
import org.namewta.profile.enterprise.usecase.EnterpriseProfileApiUseCase;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 企业模块跨模块 API 适配用例实现。 */
@Service
@RequiredArgsConstructor
public class EnterpriseProfileApiUseCaseImpl implements EnterpriseProfileApiUseCase {

    private final EnterpriseProfileApiService service;

    /** 查询当前生效的企业档案绑定。 */
    @DSTransactional
    @Override
    public Map<Long, ProfileBindingSummary> findActiveBindings(Set<Long> userIds) {
        return service.findActiveBindings(userIds);
    }

    /** 锁定并解析企业材料所有者。 */
    @DSTransactional
    @Override
    public Optional<ResolvedMaterialOwner> lockMaterialOwner(MaterialOwnerKey owner) {
        return service.lockMaterialOwner(owner);
    }

    /** 判断企业工作中材料所有者是否可编辑。 */
    @DSTransactional
    @Override
    public boolean isWorkingEditable(MaterialOwnerKey owner) {
        return service.isWorkingEditable(owner);
    }

    /** 判断企业材料快照关系是否存在。 */
    @DSTransactional
    @Override
    public boolean hasSnapshotRelationship(SnapshotRelationship relationship) {
        return service.hasSnapshotRelationship(relationship);
    }
}
