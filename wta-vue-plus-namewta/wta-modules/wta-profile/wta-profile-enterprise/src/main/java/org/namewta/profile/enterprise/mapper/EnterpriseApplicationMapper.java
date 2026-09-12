package org.namewta.profile.enterprise.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.enterprise.domain.ProfileEnterpriseApplication;

import org.apache.ibatis.annotations.Param;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseActiveProjectionRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseApplicationRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseBindingEventRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseBindingRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseDocumentTypeRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseProfileRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseSubmissionRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVersionRow;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * EnterpriseApplicationMapper 持久化映射器，负责本能力的数据映射。
 */
public interface EnterpriseApplicationMapper extends BaseMapperPlus<ProfileEnterpriseApplication, ProfileEnterpriseApplication> {

    /**
     * 定义查询映射（selectOpenByUserId）。
     */
    EnterpriseApplicationRow selectOpenByUserId(@Param("userId") long userId);

    /**
     * 定义加锁查询映射（lockOpenByUserId）。
     */
    EnterpriseApplicationRow lockOpenByUserId(@Param("userId") long userId);

    /**
     * 定义加锁查询映射（lockApplicationById）。
     */
    EnterpriseApplicationRow lockApplicationById(@Param("applicationId") long applicationId);

    /**
     * 定义查询映射（selectDocumentType）。
     */
    EnterpriseDocumentTypeRow selectDocumentType(@Param("code") String documentTypeCode);

    /**
     * 定义查询映射（selectActiveProfileIdByIdentity）。
     */
    Long selectActiveProfileIdByIdentity(@Param("identityKey") String identityKey);

    /**
     * 定义查询映射（selectEffectiveProfileIdByUser）。
     */
    Long selectEffectiveProfileIdByUser(@Param("userId") long userId);

    /**
     * 定义查询映射（selectProbeStatus）。
     */
    String selectProbeStatus(@Param("identityKey") String identityKey);

    /**
     * 定义新增映射（insertApplication）。
     */
    int insertApplication(EnterpriseApplicationRow row);

    /**
     * 定义更新映射（updateDraft）。
     */
    int updateDraft(EnterpriseApplicationRow row);

    /**
     * 定义新增映射（insertSubmission）。
     */
    int insertSubmission(EnterpriseSubmissionRow row);

    /**
     * 定义查询映射（selectSubmission）。
     */
    EnterpriseSubmissionRow selectSubmission(@Param("applicationId") long applicationId,
                                             @Param("submissionSeq") int submissionSeq);

    /**
     * 定义标记映射（markWaiting）。
     */
    int markWaiting(@Param("applicationId") long applicationId,
                    @Param("submissionSeq") int submissionSeq,
                    @Param("expectedVersion") int expectedVersion,
                    @Param("submittedTime") Instant submittedTime);

    /**
     * 定义加锁查询映射（lockActiveProfileByIdentity）。
     */
    EnterpriseProfileRow lockActiveProfileByIdentity(@Param("identityKey") String identityKey);

    /**
     * 定义加锁查询映射（lockLatestRevokedProfileByIdentity）。
     */
    EnterpriseProfileRow lockLatestRevokedProfileByIdentity(@Param("identityKey") String identityKey);

    /**
     * 定义新增映射（insertProfile）。
     */
    int insertProfile(EnterpriseProfileRow row);

    /**
     * 定义查询映射（selectCurrentVersionForUpdate）。
     */
    EnterpriseVersionRow selectCurrentVersionForUpdate(@Param("profileId") long profileId);

    /**
     * 定义置换映射（supersedeVersion）。
     */
    int supersedeVersion(@Param("versionId") long enterpriseVersionId);

    /**
     * 定义新增映射（insertVersion）。
     */
    int insertVersion(EnterpriseVersionRow row);

    /**
     * 定义更新映射（updateProfile）。
     */
    int updateProfile(EnterpriseProfileRow row);

    /**
     * 定义加锁查询映射（lockEffectiveBindingByUser）。
     */
    EnterpriseBindingRow lockEffectiveBindingByUser(@Param("userId") long userId);

    /**
     * 定义加锁查询映射（lockEffectiveBindingByProfile）。
     */
    EnterpriseBindingRow lockEffectiveBindingByProfile(@Param("profileId") long enterpriseProfileId);

    /**
     * 定义新增映射（insertBinding）。
     */
    int insertBinding(EnterpriseBindingRow row);

    /**
     * 定义新增映射（insertBindingEvent）。
     */
    int insertBindingEvent(EnterpriseBindingEventRow row);

    /**
     * 定义完成映射（finishApplication）。
     */
    int finishApplication(@Param("applicationId") long applicationId,
                          @Param("snapshotVersion") int snapshotVersion,
                          @Param("decisionVersion") int decisionVersion,
                          @Param("expectedVersion") int expectedVersion,
                          @Param("finishedTime") Instant finishedTime);

    /**
     * 定义更新映射（updateWorkflowStatus）。
     */
    int updateWorkflowStatus(@Param("applicationId") long applicationId,
                             @Param("snapshotVersion") int snapshotVersion,
                             @Param("status") String status,
                             @Param("expectedVersion") int expectedVersion,
                             @Param("occurredTime") Instant occurredTime);

    /**
     * 定义查询映射（selectActiveProjections）。
     */
    List<EnterpriseActiveProjectionRow> selectActiveProjections(@Param("userIds") Set<Long> userIds);

    /**
     * 定义加锁查询映射（lockMaterialWorkingOwner）。
     */
    Long lockMaterialWorkingOwner(@Param("ownerId") long ownerId);

    /**
     * 定义加锁查询映射（lockMaterialSubmissionOwner）。
     */
    Long lockMaterialSubmissionOwner(@Param("ownerId") long ownerId);

    /**
     * 定义加锁查询映射（lockMaterialSourceOwner）。
     */
    Long lockMaterialSourceOwner(@Param("ownerId") long ownerId);

    /**
     * 定义加锁查询映射（lockMaterialVersionOwner）。
     */
    Long lockMaterialVersionOwner(@Param("ownerId") long ownerId);

    /**
     * 定义统计映射（countEditableMaterialWorkingOwner）。
     */
    long countEditableMaterialWorkingOwner(@Param("ownerId") long ownerId);

    /**
     * 定义统计映射（countWorkingSubmissionRelationship）。
     */
    long countWorkingSubmissionRelationship(@Param("applicationId") long applicationId,
                                            @Param("submissionId") long submissionId);

    /**
     * 定义统计映射（countSubmissionVersionRelationship）。
     */
    long countSubmissionVersionRelationship(@Param("submissionId") long submissionId,
                                            @Param("versionId") long versionId);

    /**
     * 定义统计映射（countSourceVersionRelationship）。
     */
    long countSourceVersionRelationship(@Param("sourceId") long sourceId,
                                        @Param("versionId") long versionId);
}
