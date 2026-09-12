package org.namewta.profile.person.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.person.domain.ProfileMaterialNode;
import org.apache.ibatis.annotations.Param;
import org.namewta.profile.person.domain.model.read.MaterialNodeRow;
import org.namewta.profile.person.domain.model.read.MaterialReferenceRow;
import org.namewta.profile.person.domain.model.read.MaterialRequirementRow;
import org.namewta.profile.person.domain.model.read.MaterialTagCountRow;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * ProfileMaterialMapper 持久化映射器，负责本能力的数据映射。
 */
public interface ProfileMaterialMapper extends BaseMapperPlus<ProfileMaterialNode, ProfileMaterialNode> {

    /**
     * 定义查询映射（selectNodes）。
     */
    List<MaterialNodeRow> selectNodes(@Param("scope") String scope,
                                      @Param("includeDisabled") boolean includeDisabled);

    /**
     * 定义查询映射（selectNode）。
     */
    MaterialNodeRow selectNode(@Param("materialNodeId") long materialNodeId);

    /**
     * 定义加锁查询映射（lockNode）。
     */
    MaterialNodeRow lockNode(@Param("materialNodeId") long materialNodeId);

    /**
     * 定义新增映射（insertNode）。
     */
    int insertNode(@Param("materialNodeId") long materialNodeId, @Param("parentId") long parentId,
                   @Param("nodeType") String nodeType, @Param("nodeDepth") int nodeDepth,
                   @Param("profileType") String profileType, @Param("materialTagCode") String materialTagCode,
                   @Param("nodeName") String nodeName, @Param("systemRequired") String systemRequired,
                   @Param("orderNum") int orderNum);

    /**
     * 定义更新映射（updateNode）。
     */
    int updateNode(@Param("materialNodeId") long materialNodeId, @Param("parentId") long parentId,
                   @Param("nodeDepth") int nodeDepth, @Param("profileType") String profileType,
                   @Param("materialTagCode") String materialTagCode, @Param("nodeName") String nodeName,
                   @Param("systemRequired") String systemRequired, @Param("orderNum") int orderNum,
                   @Param("expectedVersion") int expectedVersion);

    /**
     * 定义统计映射（countChildren）。
     */
    long countChildren(@Param("id") long materialNodeId);

    /**
     * 定义统计映射（countReferences）。
     */
    long countReferences(@Param("id") long materialNodeId);

    /**
     * 定义更新映射（updateStatus）。
     */
    int updateStatus(@Param("id") long materialNodeId, @Param("status") String status,
                     @Param("expectedVersion") int expectedVersion);

    /**
     * 定义归档映射（archiveNode）。
     */
    int archiveNode(@Param("id") long materialNodeId, @Param("expectedVersion") int expectedVersion);

    /**
     * 定义统计映射（countAttached）。
     */
    long countAttached(@Param("profileType") String profileType, @Param("ownerType") String ownerType,
                       @Param("ownerId") long ownerId);

    /**
     * 定义新增映射（insertReference）。
     */
    int insertReference(MaterialReferenceRow row);

    /**
     * 定义加锁查询映射（lockReference）。
     */
    MaterialReferenceRow lockReference(@Param("id") long materialRefId);

    /**
     * 定义解除关联映射（detachReference）。
     */
    int detachReference(@Param("id") long materialRefId, @Param("detachedTime") Instant detachedTime);

    /**
     * 定义查询映射（selectReferences）。
     */
    List<MaterialReferenceRow> selectReferences(@Param("profileType") String profileType,
                                                @Param("ownerType") String ownerType,
                                                @Param("ownerId") long ownerId);

    /**
     * 定义查询映射（selectRequirements）。
     */
    List<MaterialRequirementRow> selectRequirements(@Param("profileType") String profileType,
                                                    @Param("documentTypeCode") String documentTypeCode,
                                                    @Param("conditions") Set<String> conditions);

    /**
     * 定义查询映射（selectAttachedCounts）。
     */
    List<MaterialTagCountRow> selectAttachedCounts(@Param("profileType") String profileType,
                                                   @Param("ownerType") String ownerType,
                                                   @Param("ownerId") long ownerId);
}
