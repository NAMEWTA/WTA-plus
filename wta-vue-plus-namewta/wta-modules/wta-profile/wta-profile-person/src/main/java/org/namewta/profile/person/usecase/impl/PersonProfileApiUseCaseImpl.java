package org.namewta.profile.person.usecase.impl;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.ResolvedMaterialOwner;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.SnapshotRelationship;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityLock;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityMatch;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityQuery;
import org.namewta.profile.person.service.PersonProfileApiService;
import org.namewta.profile.person.usecase.PersonProfileApiUseCase;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 个人模块跨模块 API 适配用例实现。 */
@Service
@RequiredArgsConstructor
public class PersonProfileApiUseCaseImpl implements PersonProfileApiUseCase {

    private final PersonProfileApiService service;

    /** 查询当前生效的个人档案绑定。 */
    @DSTransactional
    @Override
    public Map<Long, ProfileBindingSummary> findActiveBindings(Set<Long> userIds) {
        return service.findActiveBindings(userIds);
    }

    /** 锁定并解析个人材料所有者。 */
    @DSTransactional
    @Override
    public Optional<ResolvedMaterialOwner> lockMaterialOwner(MaterialOwnerKey owner) {
        return service.lockMaterialOwner(owner);
    }

    /** 判断个人工作中材料所有者是否可编辑。 */
    @DSTransactional
    @Override
    public boolean isWorkingEditable(MaterialOwnerKey owner) {
        return service.isWorkingEditable(owner);
    }

    /** 判断个人材料快照关系是否存在。 */
    @DSTransactional
    @Override
    public boolean hasSnapshotRelationship(SnapshotRelationship relationship) {
        return service.hasSnapshotRelationship(relationship);
    }

    /** 查询个人有效身份精确匹配。 */
    @DSTransactional
    @Override
    public List<ActiveIdentityMatch> findActiveExactMatches(ActiveIdentityQuery query) {
        return service.findActiveExactMatches(query);
    }

    /** 锁定个人有效身份精确匹配。 */
    @DSTransactional
    @Override
    public Optional<ActiveIdentityMatch> lockActiveExactMatch(ActiveIdentityLock query) {
        return service.lockActiveExactMatch(query);
    }
}
