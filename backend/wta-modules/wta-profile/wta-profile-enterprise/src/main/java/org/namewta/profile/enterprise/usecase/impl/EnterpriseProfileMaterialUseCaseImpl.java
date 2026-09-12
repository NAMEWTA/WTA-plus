package org.namewta.profile.enterprise.usecase.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.enterprise.usecase.ProfileMaterialUseCase;
import org.namewta.profile.enterprise.service.EnterpriseMaterialService;
import org.springframework.stereotype.Service;

/**
 * EnterpriseProfileMaterialUseCaseImpl 应用用例合同，定义入口可调用的业务场景。
 */
@Service
@RequiredArgsConstructor
public class EnterpriseProfileMaterialUseCaseImpl implements ProfileMaterialUseCase {

    private final EnterpriseMaterialService delegate;

    /** 查询企业材料目录树。 */
    @DSTransactional
    @Override
    public java.util.List<ProfileMaterialPort.MaterialNodeView> tree(ProfileMaterialPort.MaterialScope scope,
                                                                      boolean includeDisabled) {
        return delegate.tree(scope, includeDisabled);
    }
    /** 创建企业材料目录节点。 */
    @DSTransactional
    @Override
    public ProfileMaterialPort.MaterialNodeView createNode(ProfileMaterialPort.MaterialNodeCommand command) {
        return delegate.createNode(command);
    }
    /** 修改企业材料目录节点。 */
    @DSTransactional
    @Override
    public ProfileMaterialPort.MaterialNodeView updateNode(Long materialNodeId,
                                                           ProfileMaterialPort.MaterialNodeCommand command) {
        return delegate.updateNode(materialNodeId, command);
    }
    /** 修改企业材料目录节点状态。 */
    @DSTransactional
    @Override
    public void changeStatus(Long materialNodeId, boolean enabled, int expectedVersion) {
        delegate.changeStatus(materialNodeId, enabled, expectedVersion);
    }
    /** 归档企业材料目录节点。 */
    @DSTransactional
    @Override
    public void archiveNode(Long materialNodeId, int expectedVersion) {
        delegate.archiveNode(materialNodeId, expectedVersion);
    }
    /** 为企业档案挂载材料。 */
    @DSTransactional
    @Override
    public ProfileMaterialPort.MaterialReferenceView attach(ProfileMaterialPort.MaterialAttachCommand command) {
        return delegate.attach(command);
    }
    /** 解除企业档案材料挂载。 */
    @DSTransactional
    @Override
    public void detach(ProfileMaterialPort.MaterialOwnerKey owner, Long materialRefId) {
        delegate.detach(owner, materialRefId);
    }
    /** 查询企业档案材料列表。 */
    @DSTransactional
    @Override
    public java.util.List<ProfileMaterialPort.MaterialReferenceView> list(ProfileMaterialPort.MaterialOwnerKey owner) {
        return delegate.list(owner);
    }
    /** 获取企业档案材料访问地址。 */
    @DSTransactional
    @Override
    public org.namewta.profile.enterprise.domain.vo.EnterpriseProfileAccessUrl accessUrlView(
        ProfileMaterialPort.MaterialOwnerKey owner, Long materialRefId) {
        return delegate.accessUrlView(owner, materialRefId);
    }
    /** 校验企业档案必需材料。 */
    @DSTransactional
    @Override
    public void validateRequired(ProfileMaterialPort.MaterialOwnerKey owner, String documentTypeCode,
                                 java.util.Set<String> conditions) {
        delegate.validateRequired(owner, documentTypeCode, conditions);
    }
    /** 创建企业材料不可变快照。 */
    @DSTransactional
    @Override
    public java.util.List<ProfileMaterialPort.MaterialReferenceView> snapshotImmutable(
        ProfileMaterialPort.MaterialOwnerKey source, ProfileMaterialPort.MaterialOwnerKey target) {
        return delegate.snapshotImmutable(source, target);
    }
}
