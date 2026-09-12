package org.namewta.profile.enterprise.usecase;

import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.enterprise.domain.vo.EnterpriseProfileAccessUrl;

import java.util.List;
import java.util.Set;

/**
 * ProfileMaterialUseCase 应用用例合同，定义入口可调用的业务场景。
 */
public interface ProfileMaterialUseCase {
    /**
     * 编排 tree 应用用例。
     */
    List<ProfileMaterialPort.MaterialNodeView> tree(ProfileMaterialPort.MaterialScope scope, boolean includeDisabled);
    /**
     * 编排 createNode 应用用例。
     */
    ProfileMaterialPort.MaterialNodeView createNode(ProfileMaterialPort.MaterialNodeCommand command);
    /**
     * 编排 updateNode 应用用例。
     */
    ProfileMaterialPort.MaterialNodeView updateNode(Long materialNodeId, ProfileMaterialPort.MaterialNodeCommand command);
    /**
     * 编排 changeStatus 应用用例。
     */
    void changeStatus(Long materialNodeId, boolean enabled, int expectedVersion);
    /**
     * 编排 archiveNode 应用用例。
     */
    void archiveNode(Long materialNodeId, int expectedVersion);
    /**
     * 编排 attach 应用用例。
     */
    ProfileMaterialPort.MaterialReferenceView attach(ProfileMaterialPort.MaterialAttachCommand command);
    /**
     * 编排 detach 应用用例。
     */
    void detach(ProfileMaterialPort.MaterialOwnerKey owner, Long materialRefId);
    /**
     * 编排 list 应用用例。
     */
    List<ProfileMaterialPort.MaterialReferenceView> list(ProfileMaterialPort.MaterialOwnerKey owner);
    /**
     * 编排 accessUrlView 应用用例。
     */
    default EnterpriseProfileAccessUrl accessUrlView(ProfileMaterialPort.MaterialOwnerKey owner, Long materialRefId) {
        throw new UnsupportedOperationException("旧适配器不支持文件访问地址入口");
    }
    /**
     * 编排 validateRequired 应用用例。
     */
    void validateRequired(ProfileMaterialPort.MaterialOwnerKey owner, String documentTypeCode, Set<String> conditions);
    /**
     * 编排 snapshotImmutable 应用用例。
     */
    List<ProfileMaterialPort.MaterialReferenceView> snapshotImmutable(
        ProfileMaterialPort.MaterialOwnerKey source, ProfileMaterialPort.MaterialOwnerKey target);
}
