package org.namewta.profile.enterprise.service;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.ResolvedMaterialOwner;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.SnapshotRelationship;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.enterprise.dao.EnterpriseApplicationDao;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseActiveProjectionRow;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
/**
 * 企业模块对外 API SPI 所需的只读业务服务。
 *
 * <p>该服务集中完成 owner 查询和读模型转换，API 适配器只能通过对应 UseCase 调用它。</p>
 */
@Service
public class EnterpriseProfileApiService {
    private final EnterpriseApplicationDao dao;
    /** 创建企业模块 API 只读服务。 */
    public EnterpriseProfileApiService(EnterpriseApplicationDao dao) {
        this.dao = dao;
    }
    /** 查询当前生效的企业档案绑定。 */
    public Map<Long, ProfileBindingSummary> findActiveBindings(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProfileBindingSummary> result = new LinkedHashMap<>();
        for (EnterpriseActiveProjectionRow row : dao.selectActiveProjections(userIds)) {
            ProfileBindingSummary previous = result.put(row.getUserId(), new ProfileBindingSummary(
                row.getEnterpriseProfileId(), ProfileType.ENTERPRISE, row.getVerifiedAt()));
            if (previous != null) {
                throw new IllegalStateException("Duplicate active enterprise projection");
            }
        }
        return Map.copyOf(result);
    }
    /** 锁定并解析企业材料所有者。 */
    public Optional<ResolvedMaterialOwner> lockMaterialOwner(MaterialOwnerKey owner) {
        requireEnterprise(owner);
        Long value = switch (owner.ownerType()) {
            case WORKING -> dao.lockMaterialWorkingOwner(owner.ownerId());
            case SUBMISSION -> dao.lockMaterialSubmissionOwner(owner.ownerId());
            case SOURCE -> dao.lockMaterialSourceOwner(owner.ownerId());
            case VERSION -> dao.lockMaterialVersionOwner(owner.ownerId());
        };
        if (value == null) {
            return Optional.empty();
        }
        Long applicantUserId = switch (owner.ownerType()) {
            case WORKING, SUBMISSION -> value;
            case SOURCE, VERSION -> null;
        };
        return Optional.of(new ResolvedMaterialOwner(owner, applicantUserId));
    }
    /** 判断企业工作中材料所有者是否可编辑。 */
    public boolean isWorkingEditable(MaterialOwnerKey owner) {
        requireEnterprise(owner);
        if (owner.ownerType() != MaterialOwnerType.WORKING) {
            throw new IllegalArgumentException("editable owner must use WORKING");
        }
        return dao.countEditableMaterialWorkingOwner(owner.ownerId()) > 0;
    }
    /** 判断企业材料快照关系是否存在。 */
    public boolean hasSnapshotRelationship(SnapshotRelationship relationship) {
        if (relationship == null) {
            throw new IllegalArgumentException("relationship must not be null");
        }
        requireEnterprise(relationship.source());
        requireEnterprise(relationship.target());
        MaterialOwnerKey source = relationship.source();
        MaterialOwnerKey target = relationship.target();
        if (source.ownerType() == MaterialOwnerType.WORKING
            && target.ownerType() == MaterialOwnerType.SUBMISSION) {
            return dao.countWorkingSubmissionRelationship(source.ownerId(), target.ownerId()) > 0;
        }
        if (source.ownerType() == MaterialOwnerType.SUBMISSION
            && target.ownerType() == MaterialOwnerType.VERSION) {
            return dao.countSubmissionVersionRelationship(source.ownerId(), target.ownerId()) > 0;
        }
        if (source.ownerType() == MaterialOwnerType.SOURCE
            && target.ownerType() == MaterialOwnerType.VERSION) {
            return dao.countSourceVersionRelationship(source.ownerId(), target.ownerId()) > 0;
        }
        return false;
    }
    /** 校验材料所有者属于企业模块。 */
    private void requireEnterprise(MaterialOwnerKey owner) {
        if (owner == null || owner.profileType() != ProfileType.ENTERPRISE) {
            throw new IllegalArgumentException("material owner must use ENTERPRISE profileType");
        }
    }
}
