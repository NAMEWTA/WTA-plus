package org.namewta.profile.person.dao;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import org.namewta.profile.person.domain.ProfileMaterialNode;
import org.namewta.profile.person.domain.model.read.MaterialNodeRow;
import org.namewta.profile.person.domain.model.read.MaterialReferenceRow;
import org.namewta.profile.person.domain.model.read.MaterialRequirementRow;
import org.namewta.profile.person.domain.model.read.MaterialTagCountRow;
import org.namewta.profile.person.mapper.ProfileMaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 档案材料数据访问对象，统一封装材料查询条件和锁语义。
 *
 * <p>本能力的查询条件、锁语义和 Mapper 调用均收敛在此边界。</p>
 */
@RequiredArgsConstructor
@Repository
public class ProfileMaterialDao {

    private final ProfileMaterialMapper mapper;

    /**
     * 查询持久化数据（selectNodes）。
     */
    public List<MaterialNodeRow> selectNodes(String scope, boolean includeDisabled) {
        return mapper.selectNodes(scope, includeDisabled);
    }

    /**
     * 查询持久化数据（selectNode）。
     */
    public MaterialNodeRow selectNode(long materialNodeId) {
        return mapper.selectNode(materialNodeId);
    }

    /**
     * 加锁查询持久化数据（lockNode）。
     */
    public MaterialNodeRow lockNode(long materialNodeId) {
        return mapper.lockNode(materialNodeId);
    }

    /**
     * 新增持久化数据（insertNode）。
     */
    public int insertNode(long materialNodeId, long parentId, String nodeType, int nodeDepth, String profileType, String materialTagCode, String nodeName, String systemRequired, int orderNum) {
        return mapper.insertNode(materialNodeId, parentId, nodeType, nodeDepth, profileType, materialTagCode, nodeName, systemRequired, orderNum);
    }

    /**
     * 更新持久化数据（updateNode）。
     */
    public int updateNode(long materialNodeId, long parentId, int nodeDepth, String profileType, String materialTagCode, String nodeName, String systemRequired, int orderNum, int expectedVersion) {
        return mapper.updateNode(materialNodeId, parentId, nodeDepth, profileType, materialTagCode, nodeName, systemRequired, orderNum, expectedVersion);
    }

    /**
     * 统计持久化数据（countChildren）。
     */
    public long countChildren(long materialNodeId) {
        return mapper.countChildren(materialNodeId);
    }

    /**
     * 统计持久化数据（countReferences）。
     */
    public long countReferences(long materialNodeId) {
        return mapper.countReferences(materialNodeId);
    }

    /**
     * 更新持久化数据（updateStatus）。
     */
    public int updateStatus(long materialNodeId, String status, int expectedVersion) {
        return mapper.updateStatus(materialNodeId, status, expectedVersion);
    }

    /**
     * 归档持久化数据（archiveNode）。
     */
    public int archiveNode(long materialNodeId, int expectedVersion) {
        return mapper.archiveNode(materialNodeId, expectedVersion);
    }

    /**
     * 统计持久化数据（countAttached）。
     */
    public long countAttached(String profileType, String ownerType, long ownerId) {
        return mapper.countAttached(profileType, ownerType, ownerId);
    }

    /**
     * 新增持久化数据（insertReference）。
     */
    public int insertReference(MaterialReferenceRow row) {
        return mapper.insertReference(row);
    }

    /**
     * 加锁查询持久化数据（lockReference）。
     */
    public MaterialReferenceRow lockReference(long materialRefId) {
        return mapper.lockReference(materialRefId);
    }

    /**
     * 解除关联持久化数据（detachReference）。
     */
    public int detachReference(long materialRefId, Instant detachedTime) {
        return mapper.detachReference(materialRefId, detachedTime);
    }

    /**
     * 查询持久化数据（selectReferences）。
     */
    public List<MaterialReferenceRow> selectReferences(String profileType, String ownerType, long ownerId) {
        return mapper.selectReferences(profileType, ownerType, ownerId);
    }

    /**
     * 查询持久化数据（selectRequirements）。
     */
    public List<MaterialRequirementRow> selectRequirements(String profileType, String documentTypeCode, Set<String> conditions) {
        return mapper.selectRequirements(profileType, documentTypeCode, conditions);
    }

    /**
     * 查询持久化数据（selectAttachedCounts）。
     */
    public List<MaterialTagCountRow> selectAttachedCounts(String profileType, String ownerType, long ownerId) {
        return mapper.selectAttachedCounts(profileType, ownerType, ownerId);
    }
}
