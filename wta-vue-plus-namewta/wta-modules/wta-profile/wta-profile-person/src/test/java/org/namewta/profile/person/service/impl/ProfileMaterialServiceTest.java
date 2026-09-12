package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.domain.exception.ProfileMaterialException;
import org.namewta.profile.person.domain.material.MaterialNode;
import org.namewta.profile.person.domain.material.MaterialOwner;
import org.namewta.profile.person.domain.material.MaterialReference;
import org.namewta.profile.person.domain.material.MaterialRequirement;
import org.namewta.profile.person.domain.model.read.MaterialNodeRow;
import org.namewta.profile.person.domain.model.read.MaterialReferenceRow;
import org.namewta.profile.person.domain.model.read.MaterialRequirementRow;
import org.namewta.profile.person.domain.model.read.MaterialTagCountRow;
import org.namewta.profile.person.mapper.ProfileMaterialMapper;
import org.namewta.profile.person.port.security.ProfileMaterialAccessPolicy;
import org.namewta.profile.person.usecase.impl.ProfileMaterialUseCaseImpl;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.ResolvedMaterialOwner;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.SnapshotRelationship;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialAttachCommand;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialNodeCommand;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialNodeType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialScope;
import org.namewta.system.api.OssService;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class ProfileMaterialServiceTest {

    private final ProfileMaterialMapper mapper = mock(ProfileMaterialMapper.class);
    private final OssService ossService = mock(OssService.class);
    private final ProfileMaterialAccessPolicy accessPolicy = mock(ProfileMaterialAccessPolicy.class);
    private final ProfileMaterialOwnerContributor ownerContributor = mock(ProfileMaterialOwnerContributor.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);
    private ProfileMaterialServiceImpl service;

    @BeforeEach
    void setUp() {
        when(ownerContributor.profileType()).thenReturn(ProfileType.PERSON);
        when(ownerContributor.isWorkingEditable(any())).thenReturn(true);
        service = new ProfileMaterialServiceImpl(
            mapper, ossService, accessPolicy, List.of(ownerContributor), clock);
    }

    @Test
    void ownerLockingReadOperationsKeepTransactionBoundaries() throws Exception {
        for (String method : List.of("list", "accessUrl", "validateRequired")) {
            Class<?>[] parameters = switch (method) {
                case "list" -> new Class<?>[]{MaterialOwnerKey.class};
                case "accessUrl" -> new Class<?>[]{MaterialOwnerKey.class, Long.class};
                default -> new Class<?>[]{MaterialOwnerKey.class, String.class, Set.class};
            };
            String useCaseMethod = "accessUrl".equals(method) ? "accessUrlView" : method;
            assertThat(ProfileMaterialUseCaseImpl.class.getMethod(useCaseMethod, parameters))
                .matches(value -> value.isAnnotationPresent(DSTransactional.class));
        }
    }

    @Test
    void attachesOneValidatedFileAndReconcilesTheNewReference() {
        MaterialOwnerKey owner = new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.WORKING, 91L);
        when(ownerContributor.lockOwner(owner)).thenReturn(Optional.of(new ResolvedMaterialOwner(owner, 41L)));
        when(accessPolicy.requireAttach(new MaterialOwner(owner, 41L))).thenReturn(41L);
        when(mapper.selectNode(501L)).thenReturn(tagRow(501L, ProfileType.PERSON, "ID_FRONT", false));
        when(mapper.countAttached("PERSON", "WORKING", 91L)).thenReturn(9L);
        when(ossService.objectMetadata(71L)).thenReturn(new OssService.OssObjectMetadata(
            71L, "profile/front.jpg", "front.jpg", ".jpg", 10L * 1024 * 1024, "image/jpeg", 41L));
        when(mapper.insertReference(any())).thenReturn(1);

        var result = service.attach(new MaterialAttachCommand(owner, 71L, 501L));

        assertThat(result.ossId()).isEqualTo(71L);
        assertThat(result.materialTagCode()).isEqualTo("ID_FRONT");
        verify(accessPolicy).requireAttach(new MaterialOwner(owner, 41L));
        verify(ossService).reconcileReferences("profile_material_ref", String.valueOf(result.materialRefId()),
            Set.of(), Set.of(71L));
    }

    @Test
    void rejectsCountSizeExtensionMimeAndTagFailuresBeforeWriting() {
        MaterialOwnerKey owner = new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.WORKING, 92L);
        when(ownerContributor.lockOwner(owner)).thenReturn(Optional.of(new ResolvedMaterialOwner(owner, 42L)));
        when(accessPolicy.requireAttach(new MaterialOwner(owner, 42L))).thenReturn(42L);
        when(mapper.selectNode(502L)).thenReturn(tagRow(502L, ProfileType.ENTERPRISE, "LICENSE", false));
        when(mapper.countAttached("PERSON", "WORKING", 92L)).thenReturn(10L);

        assertThatThrownBy(() -> service.attach(new MaterialAttachCommand(owner, 72L, 502L)))
            .isInstanceOf(ProfileMaterialException.class)
            .hasMessageContaining("MATERIAL_TAG_NOT_APPLICABLE");

        when(mapper.selectNode(502L)).thenReturn(tagRow(502L, ProfileType.PERSON, "ID_FRONT", false));
        assertThatThrownBy(() -> service.attach(new MaterialAttachCommand(owner, 72L, 502L)))
            .hasMessageContaining("MATERIAL_COUNT_LIMIT");

        when(mapper.countAttached("PERSON", "WORKING", 92L)).thenReturn(0L);
        when(ossService.objectMetadata(72L)).thenReturn(new OssService.OssObjectMetadata(
            72L, "profile/front.png", "front.png", ".png", 1024, "image/png", 999L));
        assertThatThrownBy(() -> service.attach(new MaterialAttachCommand(owner, 72L, 502L)))
            .hasMessageContaining("MATERIAL_OSS_OWNER_MISMATCH");

        when(ossService.objectMetadata(72L)).thenReturn(new OssService.OssObjectMetadata(
            72L, "profile/front.png", "front.png", ".png", 10L * 1024 * 1024 + 1, "image/png", 42L));
        assertThatThrownBy(() -> service.attach(new MaterialAttachCommand(owner, 72L, 502L)))
            .hasMessageContaining("MATERIAL_FILE_TOO_LARGE");

        when(ossService.objectMetadata(72L)).thenReturn(new OssService.OssObjectMetadata(
            72L, "profile/front.exe", "front.exe", ".exe", 10, "application/pdf", 42L));
        assertThatThrownBy(() -> service.attach(new MaterialAttachCommand(owner, 72L, 502L)))
            .hasMessageContaining("MATERIAL_FILE_TYPE_INVALID");

        verify(mapper, never()).insertReference(any());
        verify(ossService, never()).reconcileReferences(any(), any(), any(), any());
    }

    @Test
    void administratorAttachesImmutableSourceEvidenceUsingOwnOssObject() {
        MaterialOwnerKey owner = new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.SOURCE, 98L);
        MaterialOwner resolved = new MaterialOwner(owner, null);
        when(ownerContributor.lockOwner(owner)).thenReturn(Optional.of(new ResolvedMaterialOwner(owner, null)));
        when(accessPolicy.requireAttach(resolved)).thenReturn(99L);
        when(mapper.selectNode(506L)).thenReturn(tagRow(506L, ProfileType.PERSON, "ID_FRONT", false));
        when(mapper.countAttached("PERSON", "SOURCE", 98L)).thenReturn(0L);
        when(ossService.objectMetadata(76L)).thenReturn(new OssService.OssObjectMetadata(
            76L, "profile/admin-front.jpg", "admin-front.jpg", ".jpg", 1024L, "image/jpeg", 99L));
        when(mapper.insertReference(any())).thenReturn(1);

        var result = service.attach(new MaterialAttachCommand(owner, 76L, 506L));

        assertThat(result.immutableEvidence()).isTrue();
        verify(accessPolicy).requireAttach(resolved);
        verify(ownerContributor, never()).isWorkingEditable(owner);
        verify(ossService).reconcileReferences("profile_material_ref", String.valueOf(result.materialRefId()),
            Set.of(), Set.of(76L));
    }

    @Test
    void detachesOnlyMutableWorkingEvidenceAndNeverDeletesTheObject() {
        MaterialOwnerKey owner = new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.WORKING, 93L);
        when(ownerContributor.lockOwner(owner)).thenReturn(Optional.of(new ResolvedMaterialOwner(owner, 43L)));
        when(mapper.lockReference(801L)).thenReturn(referenceRow(801L, owner, 73L, false));
        when(mapper.detachReference(801L, Instant.parse("2026-09-01T12:00:00Z"))).thenReturn(1);

        service.detach(owner, 801L);

        verify(mapper).detachReference(801L, Instant.parse("2026-09-01T12:00:00Z"));
        verify(ossService).reconcileReferences("profile_material_ref", "801", Set.of(73L), Set.of());

        when(mapper.lockReference(802L)).thenReturn(referenceRow(802L, owner, 74L, true));
        assertThatThrownBy(() -> service.detach(owner, 802L))
            .hasMessageContaining("IMMUTABLE_MATERIAL");
    }

    @Test
    void rejectsInvalidTreeShapesAndProtectsSystemRequiredTags() {
        MaterialNode root = new MaterialNode(1L, 0L, MaterialNodeType.CATEGORY, 1,
            MaterialScope.PERSON, null, "个人材料", false, true, 0, 0);
        when(mapper.selectNode(1L)).thenReturn(nodeRow(root));

        assertThatThrownBy(() -> service.createNode(new MaterialNodeCommand(
            0L, MaterialNodeType.CATEGORY, MaterialScope.PERSON, null, "重复根", false, 1, 0)))
            .hasMessageContaining("MATERIAL_ROOT_SCOPE_FIXED");

        assertThatThrownBy(() -> service.createNode(new MaterialNodeCommand(
            1L, MaterialNodeType.CATEGORY, MaterialScope.ENTERPRISE, null, "错误分类", false, 1, 0)))
            .hasMessageContaining("MATERIAL_TREE_PROFILE_MISMATCH");

        MaterialNode protectedTag = tag(2L, ProfileType.PERSON, "ID_FRONT", true);
        when(mapper.lockNode(2L)).thenReturn(nodeRow(protectedTag));
        assertThatThrownBy(() -> service.changeStatus(2L, false, 0))
            .hasMessageContaining("SYSTEM_MATERIAL_TAG_PROTECTED");
        assertThatThrownBy(() -> service.archiveNode(2L, 0))
            .hasMessageContaining("SYSTEM_MATERIAL_TAG_PROTECTED");

        MaterialNode ordinaryTag = tag(3L, ProfileType.PERSON, "OPTIONAL_OLD", false);
        when(mapper.lockNode(3L)).thenReturn(nodeRow(ordinaryTag));
        assertThatThrownBy(() -> service.updateNode(3L, new MaterialNodeCommand(
            1L, MaterialNodeType.TAG, MaterialScope.PERSON, "OPTIONAL_NEW", "新名称", false, 1, 0)))
            .hasMessageContaining("MATERIAL_TAG_CODE_IMMUTABLE");
    }

    @Test
    void validatesEveryRequiredTagAgainstTheCurrentAttachedSet() {
        MaterialOwnerKey owner = new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.WORKING, 94L);
        when(ownerContributor.lockOwner(owner)).thenReturn(Optional.of(new ResolvedMaterialOwner(owner, 44L)));
        when(mapper.selectRequirements("PERSON", "CN_RESIDENT_ID", Set.of("ALWAYS")))
            .thenReturn(List.of(new MaterialRequirementRow("ID_FRONT", 1),
                new MaterialRequirementRow("ID_BACK", 1)));
        when(mapper.selectAttachedCounts("PERSON", "WORKING", 94L))
            .thenReturn(List.of(new MaterialTagCountRow("ID_FRONT", 1L)));

        assertThatThrownBy(() -> service.validateRequired(owner, "CN_RESIDENT_ID", Set.of("ALWAYS")))
            .hasMessageContaining("MISSING_REQUIRED_MATERIAL:ID_BACK");
    }

    @Test
    void rejectsWorkingMaterialWritesWhenTheApplicationIsNotEditable() {
        MaterialOwnerKey owner = new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.WORKING, 95L);
        when(ownerContributor.lockOwner(owner)).thenReturn(Optional.of(new ResolvedMaterialOwner(owner, 45L)));
        when(ownerContributor.isWorkingEditable(owner)).thenReturn(false);

        assertThatThrownBy(() -> service.attach(new MaterialAttachCommand(owner, 75L, 505L)))
            .hasMessageContaining("MATERIAL_OWNER_READ_ONLY");
        assertThatThrownBy(() -> service.detach(owner, 805L))
            .hasMessageContaining("MATERIAL_OWNER_READ_ONLY");

        verify(mapper, never()).insertReference(any());
        verify(mapper, never()).detachReference(anyLong(), any());
    }

    @Test
    void publishesOnlyRelatedSubmissionEvidenceToItsProfileVersion() {
        MaterialOwnerKey source = new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.SUBMISSION, 96L);
        MaterialOwnerKey target = new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.VERSION, 97L);
        MaterialOwner sourceOwner = new MaterialOwner(source, 46L);
        when(ownerContributor.lockOwner(source)).thenReturn(Optional.of(new ResolvedMaterialOwner(source, 46L)));
        when(ownerContributor.lockOwner(target)).thenReturn(Optional.of(new ResolvedMaterialOwner(target, null)));
        when(ownerContributor.hasSnapshotRelationship(new SnapshotRelationship(source, target))).thenReturn(true);
        when(mapper.selectReferences("PERSON", "SUBMISSION", 96L))
            .thenReturn(List.of(referenceRow(806L, source, 76L, true)));
        when(mapper.insertReference(any())).thenReturn(1);

        var result = service.snapshotImmutable(source, target);

        assertThat(result).singleElement().extracting(ProfileMaterialPort.MaterialReferenceView::owner)
            .isEqualTo(target);
        verify(ownerContributor).hasSnapshotRelationship(new SnapshotRelationship(source, target));
        verify(accessPolicy, never()).requireWrite(sourceOwner);
        verify(ossService).reconcileReferences(eq("profile_material_ref"), any(), eq(Set.of()), eq(Set.of(76L)));
    }

    private MaterialNode tag(long id, ProfileType type, String code, boolean systemRequired) {
        return new MaterialNode(id, 1L, MaterialNodeType.TAG, 2, MaterialScope.valueOf(type.name()), code, code,
            systemRequired, true, 0, 0);
    }

    private MaterialReference reference(long id, MaterialOwnerKey owner, long ossId, boolean immutable) {
        return new MaterialReference(id, owner, ossId, 501L, "ID_FRONT", "正面", "front.jpg",
            1024L, ".jpg", "image/jpeg", true, immutable, Instant.parse("2026-09-01T11:00:00Z"), null, 0);
    }

    private MaterialNodeRow tagRow(long id, ProfileType type, String code, boolean systemRequired) {
        return nodeRow(tag(id, type, code, systemRequired));
    }

    private MaterialNodeRow nodeRow(MaterialNode node) {
        return new MaterialNodeRow(node.materialNodeId(), node.parentId(), node.nodeType().name(), node.nodeDepth(),
            node.scope().name(), node.materialTagCode(), node.nodeName(), node.systemRequired() ? "Y" : "N",
            node.enabled() ? "0" : "1", node.orderNum(), node.version());
    }

    private MaterialReferenceRow referenceRow(long id, MaterialOwnerKey owner, long ossId, boolean immutable) {
        MaterialReference reference = reference(id, owner, ossId, immutable);
        return new MaterialReferenceRow(reference.materialRefId(), owner.ownerType().name(), owner.ownerId(),
            owner.profileType().name(), reference.ossId(), reference.materialNodeId(), reference.materialTagCode(),
            reference.materialTagName(), reference.fileName(), reference.fileSize(), reference.fileExtension(),
            reference.mimeType(), "ATTACHED", immutable ? "Y" : "N", reference.attachedTime(), null, 0);
    }
}
