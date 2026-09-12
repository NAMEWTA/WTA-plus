package org.namewta.profile.enterprise.dao;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import org.namewta.profile.enterprise.domain.ProfileEnterpriseApplication;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseActiveProjectionRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseApplicationRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseBindingEventRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseBindingRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseDocumentTypeRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseProfileRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseSubmissionRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVersionRow;
import org.namewta.profile.enterprise.mapper.EnterpriseApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 企业申请数据访问对象，统一封装申请查询条件和锁语义。
 *
 * <p>本能力的查询条件、锁语义和 Mapper 调用均收敛在此边界。</p>
 */
@RequiredArgsConstructor
@Repository
public class EnterpriseApplicationDao {

    private final EnterpriseApplicationMapper mapper;

    /**
     * 查询持久化数据（selectOpenByUserId）。
     */
    public EnterpriseApplicationRow selectOpenByUserId(long userId) {
        return mapper.selectOpenByUserId(userId);
    }

    /**
     * 加锁查询持久化数据（lockOpenByUserId）。
     */
    public EnterpriseApplicationRow lockOpenByUserId(long userId) {
        return mapper.lockOpenByUserId(userId);
    }

    /**
     * 加锁查询持久化数据（lockApplicationById）。
     */
    public EnterpriseApplicationRow lockApplicationById(long applicationId) {
        return mapper.lockApplicationById(applicationId);
    }

    /**
     * 查询持久化数据（selectDocumentType）。
     */
    public EnterpriseDocumentTypeRow selectDocumentType(String documentTypeCode) {
        return mapper.selectDocumentType(documentTypeCode);
    }

    /**
     * 查询持久化数据（selectActiveProfileIdByIdentity）。
     */
    public Long selectActiveProfileIdByIdentity(String identityKey) {
        return mapper.selectActiveProfileIdByIdentity(identityKey);
    }

    /**
     * 查询持久化数据（selectEffectiveProfileIdByUser）。
     */
    public Long selectEffectiveProfileIdByUser(long userId) {
        return mapper.selectEffectiveProfileIdByUser(userId);
    }

    /**
     * 查询持久化数据（selectProbeStatus）。
     */
    public String selectProbeStatus(String identityKey) {
        return mapper.selectProbeStatus(identityKey);
    }

    /**
     * 新增持久化数据（insertApplication）。
     */
    public int insertApplication(EnterpriseApplicationRow row) {
        return mapper.insertApplication(row);
    }

    /**
     * 更新持久化数据（updateDraft）。
     */
    public int updateDraft(EnterpriseApplicationRow row) {
        return mapper.updateDraft(row);
    }

    /**
     * 新增持久化数据（insertSubmission）。
     */
    public int insertSubmission(EnterpriseSubmissionRow row) {
        return mapper.insertSubmission(row);
    }

    /**
     * 查询持久化数据（selectSubmission）。
     */
    public EnterpriseSubmissionRow selectSubmission(long applicationId, int submissionSeq) {
        return mapper.selectSubmission(applicationId, submissionSeq);
    }

    /**
     * 标记持久化数据（markWaiting）。
     */
    public int markWaiting(long applicationId, int submissionSeq, int expectedVersion, Instant submittedTime) {
        return mapper.markWaiting(applicationId, submissionSeq, expectedVersion, submittedTime);
    }

    /**
     * 加锁查询持久化数据（lockActiveProfileByIdentity）。
     */
    public EnterpriseProfileRow lockActiveProfileByIdentity(String identityKey) {
        return mapper.lockActiveProfileByIdentity(identityKey);
    }

    /**
     * 加锁查询持久化数据（lockLatestRevokedProfileByIdentity）。
     */
    public EnterpriseProfileRow lockLatestRevokedProfileByIdentity(String identityKey) {
        return mapper.lockLatestRevokedProfileByIdentity(identityKey);
    }

    /**
     * 新增持久化数据（insertProfile）。
     */
    public int insertProfile(EnterpriseProfileRow row) {
        return mapper.insertProfile(row);
    }

    /**
     * 查询持久化数据（selectCurrentVersionForUpdate）。
     */
    public EnterpriseVersionRow selectCurrentVersionForUpdate(long profileId) {
        return mapper.selectCurrentVersionForUpdate(profileId);
    }

    /**
     * 置换持久化数据（supersedeVersion）。
     */
    public int supersedeVersion(long enterpriseVersionId) {
        return mapper.supersedeVersion(enterpriseVersionId);
    }

    /**
     * 新增持久化数据（insertVersion）。
     */
    public int insertVersion(EnterpriseVersionRow row) {
        return mapper.insertVersion(row);
    }

    /**
     * 更新持久化数据（updateProfile）。
     */
    public int updateProfile(EnterpriseProfileRow row) {
        return mapper.updateProfile(row);
    }

    /**
     * 加锁查询持久化数据（lockEffectiveBindingByUser）。
     */
    public EnterpriseBindingRow lockEffectiveBindingByUser(long userId) {
        return mapper.lockEffectiveBindingByUser(userId);
    }

    /**
     * 加锁查询持久化数据（lockEffectiveBindingByProfile）。
     */
    public EnterpriseBindingRow lockEffectiveBindingByProfile(long enterpriseProfileId) {
        return mapper.lockEffectiveBindingByProfile(enterpriseProfileId);
    }

    /**
     * 新增持久化数据（insertBinding）。
     */
    public int insertBinding(EnterpriseBindingRow row) {
        return mapper.insertBinding(row);
    }

    /**
     * 新增持久化数据（insertBindingEvent）。
     */
    public int insertBindingEvent(EnterpriseBindingEventRow row) {
        return mapper.insertBindingEvent(row);
    }

    /**
     * 完成持久化数据（finishApplication）。
     */
    public int finishApplication(long applicationId, int snapshotVersion, int decisionVersion, int expectedVersion, Instant finishedTime) {
        return mapper.finishApplication(applicationId, snapshotVersion, decisionVersion, expectedVersion, finishedTime);
    }

    /**
     * 更新持久化数据（updateWorkflowStatus）。
     */
    public int updateWorkflowStatus(long applicationId, int snapshotVersion, String status, int expectedVersion, Instant occurredTime) {
        return mapper.updateWorkflowStatus(applicationId, snapshotVersion, status, expectedVersion, occurredTime);
    }

    /**
     * 查询持久化数据（selectActiveProjections）。
     */
    public List<EnterpriseActiveProjectionRow> selectActiveProjections(Set<Long> userIds) {
        return mapper.selectActiveProjections(userIds);
    }

    /**
     * 加锁查询持久化数据（lockMaterialWorkingOwner）。
     */
    public Long lockMaterialWorkingOwner(long ownerId) {
        return mapper.lockMaterialWorkingOwner(ownerId);
    }

    /**
     * 加锁查询持久化数据（lockMaterialSubmissionOwner）。
     */
    public Long lockMaterialSubmissionOwner(long ownerId) {
        return mapper.lockMaterialSubmissionOwner(ownerId);
    }

    /**
     * 加锁查询持久化数据（lockMaterialSourceOwner）。
     */
    public Long lockMaterialSourceOwner(long ownerId) {
        return mapper.lockMaterialSourceOwner(ownerId);
    }

    /**
     * 加锁查询持久化数据（lockMaterialVersionOwner）。
     */
    public Long lockMaterialVersionOwner(long ownerId) {
        return mapper.lockMaterialVersionOwner(ownerId);
    }

    /**
     * 统计持久化数据（countEditableMaterialWorkingOwner）。
     */
    public long countEditableMaterialWorkingOwner(long ownerId) {
        return mapper.countEditableMaterialWorkingOwner(ownerId);
    }

    /**
     * 统计持久化数据（countWorkingSubmissionRelationship）。
     */
    public long countWorkingSubmissionRelationship(long applicationId, long submissionId) {
        return mapper.countWorkingSubmissionRelationship(applicationId, submissionId);
    }

    /**
     * 统计持久化数据（countSubmissionVersionRelationship）。
     */
    public long countSubmissionVersionRelationship(long submissionId, long versionId) {
        return mapper.countSubmissionVersionRelationship(submissionId, versionId);
    }

    /**
     * 统计持久化数据（countSourceVersionRelationship）。
     */
    public long countSourceVersionRelationship(long sourceId, long versionId) {
        return mapper.countSourceVersionRelationship(sourceId, versionId);
    }
}
