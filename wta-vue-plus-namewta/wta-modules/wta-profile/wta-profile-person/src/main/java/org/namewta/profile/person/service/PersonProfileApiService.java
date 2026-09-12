package org.namewta.profile.person.service;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.ResolvedMaterialOwner;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.SnapshotRelationship;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.api.person.PersonIdentityLookupService;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityLock;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityMatch;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityQuery;
import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.person.domain.model.read.PersonActiveIdentityMatchRow;
import org.namewta.profile.person.domain.model.read.PersonActiveProjectionRow;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
/**
 * 个人模块对外 SPI 所需的只读业务服务。
 *
 * <p>该服务是 API 适配器与 DAO 之间的唯一业务边界，负责把读模型转换为公开合同类型。</p>
 */
@Service
public class PersonProfileApiService {
    private final PersonApplicationDao dao;
    /** 创建个人模块 API 只读服务。 */
    public PersonProfileApiService(PersonApplicationDao dao) {
        this.dao = dao;
    }
    /** 查询当前生效的个人档案绑定。 */
    public Map<Long, ProfileBindingSummary> findActiveBindings(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProfileBindingSummary> result = new LinkedHashMap<>();
        for (PersonActiveProjectionRow row : dao.selectActiveProjections(userIds)) {
            ProfileBindingSummary previous = result.put(row.getUserId(), new ProfileBindingSummary(
                row.getPersonProfileId(), ProfileType.PERSON, row.getVerifiedAt()));
            if (previous != null) {
                throw new IllegalStateException("Duplicate active person projection");
            }
        }
        return Map.copyOf(result);
    }
    /** 锁定并解析个人材料所有者。 */
    public Optional<ResolvedMaterialOwner> lockMaterialOwner(MaterialOwnerKey owner) {
        requirePerson(owner);
        if (owner.ownerType() == MaterialOwnerType.WORKING) {
            return Optional.ofNullable(dao.lockMaterialWorkingOwner(owner.ownerId()))
                .map(userId -> new ResolvedMaterialOwner(owner, userId));
        }
        if (owner.ownerType() == MaterialOwnerType.SUBMISSION) {
            return Optional.ofNullable(dao.lockMaterialSubmissionOwner(owner.ownerId()))
                .map(userId -> new ResolvedMaterialOwner(owner, userId));
        }
        return dao.lockMaterialImmutableOwner(owner.ownerType().name(), owner.ownerId()) != null
            ? Optional.of(new ResolvedMaterialOwner(owner, null))
            : Optional.empty();
    }
    /** 判断个人工作中材料所有者是否可编辑。 */
    public boolean isWorkingEditable(MaterialOwnerKey owner) {
        requirePerson(owner);
        if (owner.ownerType() != MaterialOwnerType.WORKING) {
            throw new IllegalArgumentException("material owner must be WORKING");
        }
        return dao.countEditableMaterialWorkingOwner(owner.ownerId()) == 1;
    }
    /** 判断个人材料快照关系是否存在。 */
    public boolean hasSnapshotRelationship(SnapshotRelationship relationship) {
        requirePerson(relationship.source());
        requirePerson(relationship.target());
        return dao.countMaterialSnapshotRelationship(
            relationship.source().ownerType().name(), relationship.source().ownerId(),
            relationship.target().ownerType().name(), relationship.target().ownerId()) == 1;
    }
    /** 查询个人有效身份精确匹配。 */
    public List<ActiveIdentityMatch> findActiveExactMatches(ActiveIdentityQuery query) {
        return dao.selectActiveIdentityMatches(query.fullName(), query.documentLastFour()).stream()
            .map(this::match)
            .toList();
    }
    /** 锁定个人有效身份精确匹配。 */
    public Optional<ActiveIdentityMatch> lockActiveExactMatch(ActiveIdentityLock query) {
        return Optional.ofNullable(dao.lockActiveIdentityMatch(
                query.userId(), query.personProfileId(), query.fullName(), query.documentLastFour()))
            .map(this::match);
    }
    /** 将身份读模型转换为公开结果。 */
    private ActiveIdentityMatch match(PersonActiveIdentityMatchRow row) {
        return new ActiveIdentityMatch(row.getUserId(), row.getPersonProfileId());
    }
    /** 校验材料所有者属于个人模块。 */
    private void requirePerson(MaterialOwnerKey owner) {
        if (owner == null || owner.profileType() != ProfileType.PERSON) {
            throw new IllegalArgumentException("material owner must use PERSON profileType");
        }
    }
}
