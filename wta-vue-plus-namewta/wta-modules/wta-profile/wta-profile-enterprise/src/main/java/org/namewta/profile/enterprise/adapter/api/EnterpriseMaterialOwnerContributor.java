package org.namewta.profile.enterprise.adapter.api;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.enterprise.usecase.EnterpriseProfileApiUseCase;
import org.springframework.stereotype.Component;
import java.util.Optional;
/**
 * 企业档案材料 owner 的模块内权威解析器。
 */
@Component
public class EnterpriseMaterialOwnerContributor implements ProfileMaterialOwnerContributor {
    private final EnterpriseProfileApiUseCase useCase;
    /**
     * 处理enterprisematerialownercontributor。
     */
    public EnterpriseMaterialOwnerContributor(EnterpriseProfileApiUseCase useCase) {
        this.useCase = useCase;
    }
    /**
     * 返回材料所属档案类型
     */
    @Override
    public ProfileType profileType() {
        return ProfileType.ENTERPRISE;
    }
    /**
     * 锁定材料所有者记录
     */
    @Override
    public Optional<ResolvedMaterialOwner> lockOwner(MaterialOwnerKey owner) {
        return useCase.lockMaterialOwner(owner);
    }
    /**
     * 判断档案是否处于可编辑状态
     */
    @Override
    public boolean isWorkingEditable(MaterialOwnerKey owner) {
        return useCase.isWorkingEditable(owner);
    }
    /**
     * 判断快照是否具有关联关系
     */
    @Override
    public boolean hasSnapshotRelationship(SnapshotRelationship relationship) {
        return useCase.hasSnapshotRelationship(relationship);
    }
}
