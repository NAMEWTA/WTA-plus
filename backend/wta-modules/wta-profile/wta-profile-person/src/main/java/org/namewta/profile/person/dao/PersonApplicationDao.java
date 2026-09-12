package org.namewta.profile.person.dao;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import org.namewta.profile.person.domain.ProfilePersonApplication;
import org.namewta.profile.person.domain.model.read.PersonActiveIdentityMatchRow;
import org.namewta.profile.person.domain.model.read.PersonActiveProjectionRow;
import org.namewta.profile.person.domain.model.read.PersonApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonBindingEventRow;
import org.namewta.profile.person.domain.model.read.PersonBindingRow;
import org.namewta.profile.person.domain.model.read.PersonDocumentTypeRow;
import org.namewta.profile.person.domain.model.read.PersonProfileRow;
import org.namewta.profile.person.domain.model.read.PersonSubmissionRow;
import org.namewta.profile.person.domain.model.read.PersonVersionRow;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 个人申请数据访问对象，统一封装申请查询条件和锁语义。
 *
 * <p>本能力的查询条件、锁语义和 Mapper 调用均收敛在此边界。</p>
 */
@RequiredArgsConstructor
@Repository
public class PersonApplicationDao {

    private final PersonApplicationMapper mapper;

    /**
     * 查询持久化数据（selectActiveIdentityMatches）。
     */
    public List<PersonActiveIdentityMatchRow> selectActiveIdentityMatches(String fullName, String documentLastFour) {
        return mapper.selectActiveIdentityMatches(fullName, documentLastFour);
    }

    /**
     * 加锁查询持久化数据（lockActiveIdentityMatch）。
     */
    public PersonActiveIdentityMatchRow lockActiveIdentityMatch(long userId, long personProfileId, String fullName, String documentLastFour) {
        return mapper.lockActiveIdentityMatch(userId, personProfileId, fullName, documentLastFour);
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
     * 加锁查询持久化数据（lockMaterialImmutableOwner）。
     */
    public Long lockMaterialImmutableOwner(String ownerType, long ownerId) {
        return mapper.lockMaterialImmutableOwner(ownerType, ownerId);
    }

    /**
     * 统计持久化数据（countEditableMaterialWorkingOwner）。
     */
    public long countEditableMaterialWorkingOwner(long ownerId) {
        return mapper.countEditableMaterialWorkingOwner(ownerId);
    }

    /**
     * 统计持久化数据（countMaterialSnapshotRelationship）。
     */
    public long countMaterialSnapshotRelationship(String sourceType, long sourceId, String targetType, long targetId) {
        return mapper.countMaterialSnapshotRelationship(sourceType, sourceId, targetType, targetId);
    }

    /**
     * 查询持久化数据（selectOpenByUserId）。
     */
    public PersonApplicationRow selectOpenByUserId(long userId) {
        return mapper.selectOpenByUserId(userId);
    }

    /**
     * 加锁查询持久化数据（lockOpenByUserId）。
     */
    public PersonApplicationRow lockOpenByUserId(long userId) {
        return mapper.lockOpenByUserId(userId);
    }

    /**
     * 加锁查询持久化数据（lockApplicationById）。
     */
    public PersonApplicationRow lockApplicationById(long applicationId) {
        return mapper.lockApplicationById(applicationId);
    }

    /**
     * 查询持久化数据（selectDocumentType）。
     */
    public PersonDocumentTypeRow selectDocumentType(String documentTypeCode) {
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
     * 新增持久化数据（insertApplication）。
     */
    public int insertApplication(PersonApplicationRow row) {
        return mapper.insertApplication(row);
    }

    /**
     * 更新持久化数据（updateDraft）。
     */
    public int updateDraft(PersonApplicationRow row) {
        return mapper.updateDraft(row);
    }

    /**
     * 新增持久化数据（insertSubmission）。
     */
    public int insertSubmission(PersonSubmissionRow row) {
        return mapper.insertSubmission(row);
    }

    /**
     * 查询持久化数据（selectSubmission）。
     */
    public PersonSubmissionRow selectSubmission(long applicationId, int submissionSeq) {
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
    public PersonProfileRow lockActiveProfileByIdentity(String identityKey) {
        return mapper.lockActiveProfileByIdentity(identityKey);
    }

    /**
     * 加锁查询持久化数据（lockActiveProfileById）。
     */
    public PersonProfileRow lockActiveProfileById(long profileId) {
        return mapper.lockActiveProfileById(profileId);
    }

    /**
     * 更新持久化数据（updateIdentityGuard）。
     */
    public int updateIdentityGuard(long profileId, String identityKey) {
        return mapper.updateIdentityGuard(profileId, identityKey);
    }

    /**
     * 加锁查询持久化数据（lockLatestRevokedProfileByIdentity）。
     */
    public PersonProfileRow lockLatestRevokedProfileByIdentity(String identityKey) {
        return mapper.lockLatestRevokedProfileByIdentity(identityKey);
    }

    /**
     * 新增持久化数据（insertIdentityGuard）。
     */
    public int insertIdentityGuard(long guardId, String identityKey, long profileId) {
        return mapper.insertIdentityGuard(guardId, identityKey, profileId);
    }

    /**
     * 新增持久化数据（insertProfile）。
     */
    public int insertProfile(PersonProfileRow row) {
        return mapper.insertProfile(row);
    }

    /**
     * 查询持久化数据（selectCurrentVersionForUpdate）。
     */
    public PersonVersionRow selectCurrentVersionForUpdate(long profileId) {
        return mapper.selectCurrentVersionForUpdate(profileId);
    }

    /**
     * 置换持久化数据（supersedeVersion）。
     */
    public int supersedeVersion(long personVersionId) {
        return mapper.supersedeVersion(personVersionId);
    }

    /**
     * 新增持久化数据（insertVersion）。
     */
    public int insertVersion(PersonVersionRow row) {
        return mapper.insertVersion(row);
    }

    /**
     * 更新持久化数据（updateProfile）。
     */
    public int updateProfile(PersonProfileRow row) {
        return mapper.updateProfile(row);
    }

    /**
     * 加锁查询持久化数据（lockEffectiveBindingByUser）。
     */
    public PersonBindingRow lockEffectiveBindingByUser(long userId) {
        return mapper.lockEffectiveBindingByUser(userId);
    }

    /**
     * 加锁查询持久化数据（lockEffectiveBindingByProfile）。
     */
    public PersonBindingRow lockEffectiveBindingByProfile(long personProfileId) {
        return mapper.lockEffectiveBindingByProfile(personProfileId);
    }

    /**
     * 新增持久化数据（insertBinding）。
     */
    public int insertBinding(PersonBindingRow row) {
        return mapper.insertBinding(row);
    }

    /**
     * 新增持久化数据（insertBindingEvent）。
     */
    public int insertBindingEvent(PersonBindingEventRow row) {
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
    public List<PersonActiveProjectionRow> selectActiveProjections(Set<Long> userIds) {
        return mapper.selectActiveProjections(userIds);
    }
}
