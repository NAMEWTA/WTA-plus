package org.namewta.profile.person.usecase.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.person.service.ProfileMaterialService;
import org.namewta.profile.person.usecase.ProfileMaterialUseCase;
import org.springframework.stereotype.Service;

/**
 * ProfileMaterialUseCaseImpl 应用用例合同，定义入口可调用的业务场景。
 */
@Service
@RequiredArgsConstructor
public class ProfileMaterialUseCaseImpl implements ProfileMaterialUseCase {

    private final ProfileMaterialService service;

    /** 查询个人材料目录树。 */
    @DSTransactional
    @Override
    public java.util.List<ProfileMaterialPort.MaterialNodeView> tree(ProfileMaterialPort.MaterialScope scope,
                                                                      boolean includeDisabled) {
        return service.tree(scope, includeDisabled);
    }
    /** 创建个人材料目录节点。 */
    @DSTransactional
    @Override
    public ProfileMaterialPort.MaterialNodeView createNode(ProfileMaterialPort.MaterialNodeCommand command) {
        return service.createNode(command);
    }
    /** 修改个人材料目录节点。 */
    @DSTransactional
    @Override
    public ProfileMaterialPort.MaterialNodeView updateNode(Long materialNodeId,
                                                           ProfileMaterialPort.MaterialNodeCommand command) {
        return service.updateNode(materialNodeId, command);
    }
    /** 修改个人材料目录节点状态。 */
    @DSTransactional
    @Override
    public void changeStatus(Long materialNodeId, boolean enabled, int expectedVersion) {
        service.changeStatus(materialNodeId, enabled, expectedVersion);
    }
    /** 归档个人材料目录节点。 */
    @DSTransactional
    @Override
    public void archiveNode(Long materialNodeId, int expectedVersion) {
        service.archiveNode(materialNodeId, expectedVersion);
    }
    /** 为个人档案挂载材料。 */
    @DSTransactional
    @Override
    public ProfileMaterialPort.MaterialReferenceView attach(ProfileMaterialPort.MaterialAttachCommand command) {
        return service.attach(command);
    }
    /** 解除个人档案材料挂载。 */
    @DSTransactional
    @Override
    public void detach(ProfileMaterialPort.MaterialOwnerKey owner, Long materialRefId) {
        service.detach(owner, materialRefId);
    }
    /** 查询个人档案材料列表。 */
    @DSTransactional
    @Override
    public java.util.List<ProfileMaterialPort.MaterialReferenceView> list(ProfileMaterialPort.MaterialOwnerKey owner) {
        return service.list(owner);
    }
    /** 获取个人档案材料访问地址。 */
    @DSTransactional
    @Override
    public org.namewta.profile.person.domain.vo.PersonProfileAccessUrl accessUrlView(
        ProfileMaterialPort.MaterialOwnerKey owner, Long materialRefId) {
        return service.accessUrlView(owner, materialRefId);
    }
    /** 校验个人档案必需材料。 */
    @DSTransactional
    @Override
    public void validateRequired(ProfileMaterialPort.MaterialOwnerKey owner, String documentTypeCode,
                                 java.util.Set<String> conditions) {
        service.validateRequired(owner, documentTypeCode, conditions);
    }
    /** 创建个人材料不可变快照。 */
    @DSTransactional
    @Override
    public java.util.List<ProfileMaterialPort.MaterialReferenceView> snapshotImmutable(
        ProfileMaterialPort.MaterialOwnerKey source, ProfileMaterialPort.MaterialOwnerKey target) {
        return service.snapshotImmutable(source, target);
    }
}
