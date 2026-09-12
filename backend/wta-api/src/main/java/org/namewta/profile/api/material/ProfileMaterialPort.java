package org.namewta.profile.api.material;

import org.namewta.profile.api.domain.ProfileType;
import org.namewta.system.api.OssService;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 个人与企业档案共享的材料目录和证据端口。
 */
public interface ProfileMaterialPort {

    List<MaterialNodeView> tree(MaterialScope scope, boolean includeDisabled);

    MaterialNodeView createNode(MaterialNodeCommand command);

    MaterialNodeView updateNode(Long materialNodeId, MaterialNodeCommand command);

    void changeStatus(Long materialNodeId, boolean enabled, int expectedVersion);

    void archiveNode(Long materialNodeId, int expectedVersion);

    MaterialReferenceView attach(MaterialAttachCommand command);

    void detach(MaterialOwnerKey owner, Long materialRefId);

    List<MaterialReferenceView> list(MaterialOwnerKey owner);

    OssService.OssAccessUrl accessUrl(MaterialOwnerKey owner, Long materialRefId);

    void validateRequired(MaterialOwnerKey owner, String documentTypeCode, Set<String> conditions);

    List<MaterialReferenceView> snapshotImmutable(MaterialOwnerKey source, MaterialOwnerKey target);

    enum MaterialNodeType {
        CATEGORY,
        TAG
    }

    enum MaterialScope {
        PERSON,
        ENTERPRISE,
        COMMON
    }

    enum MaterialOwnerType {
        WORKING,
        SUBMISSION,
        SOURCE,
        VERSION
    }

    record MaterialOwnerKey(ProfileType profileType, MaterialOwnerType ownerType, Long ownerId) {
        public MaterialOwnerKey {
            Objects.requireNonNull(profileType, "profileType");
            Objects.requireNonNull(ownerType, "ownerType");
            if (ownerId == null || ownerId <= 0) {
                throw new IllegalArgumentException("ownerId must be positive");
            }
        }
    }

    record MaterialNodeCommand(Long parentId, MaterialNodeType nodeType, MaterialScope scope,
                               String materialTagCode, String nodeName, boolean systemRequired,
                               int orderNum, int expectedVersion) {
        public MaterialNodeCommand {
            parentId = parentId == null ? 0L : parentId;
            Objects.requireNonNull(nodeType, "nodeType");
            Objects.requireNonNull(scope, "scope");
        }
    }

    record MaterialAttachCommand(MaterialOwnerKey owner, Long ossId, Long materialNodeId) {
        public MaterialAttachCommand {
            Objects.requireNonNull(owner, "owner");
            if (ossId == null || ossId <= 0 || materialNodeId == null || materialNodeId <= 0) {
                throw new IllegalArgumentException("ossId and materialNodeId must be positive");
            }
        }
    }

    record MaterialNodeView(Long materialNodeId, Long parentId, MaterialNodeType nodeType, int nodeDepth,
                            MaterialScope scope, String materialTagCode, String nodeName,
                            boolean systemRequired, boolean enabled, int orderNum, int version,
                            List<MaterialNodeView> children) {
        public MaterialNodeView {
            children = children == null ? List.of() : List.copyOf(children);
        }
    }

    record MaterialReferenceView(Long materialRefId, MaterialOwnerKey owner, Long ossId,
                                 Long materialNodeId, String materialTagCode, String materialTagName,
                                 String fileName, long fileSize, String fileExtension, String mimeType,
                                 boolean attached, boolean immutableEvidence, Instant attachedTime,
                                 Instant detachedTime, int version) {
    }
}
