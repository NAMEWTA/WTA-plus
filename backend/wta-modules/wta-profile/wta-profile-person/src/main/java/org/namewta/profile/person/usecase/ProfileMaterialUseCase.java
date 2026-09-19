package org.namewta.profile.person.usecase;

import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.person.domain.vo.ProfileMaterialRequirementVo;
import org.namewta.profile.person.domain.vo.PersonProfileAccessUrl;

import java.util.List;
import java.util.Set;

/**
 * ProfileMaterialUseCase 应用用例合同，定义入口可调用的业务场景。
 */
public interface ProfileMaterialUseCase {
    /** 查询申请条件对应的材料提示，不替代提交时的必填校验。 */
    List<ProfileMaterialRequirementVo> requiredMaterials(
        ProfileType profileType, String documentTypeCode,
        boolean handlerIsLegalRepresentative);

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
    PersonProfileAccessUrl accessUrlView(ProfileMaterialPort.MaterialOwnerKey owner, Long materialRefId);
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
