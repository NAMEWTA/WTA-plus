package org.namewta.profile.person.dao;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;
import org.apache.ibatis.annotations.Param;
import org.namewta.profile.person.domain.ProfilePersonBinding;
import org.namewta.profile.person.domain.model.read.PersonApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonBindingEventRow;
import org.namewta.profile.person.domain.model.read.PersonBindingRow;
import org.namewta.profile.person.domain.model.read.PersonProfileRow;
import org.namewta.profile.person.domain.model.read.PersonSubmissionRow;
import org.namewta.profile.person.domain.model.read.PersonVersionRow;
import org.namewta.profile.person.domain.model.read.PersonRebindCandidateRow;
import org.namewta.profile.person.mapper.PersonRebindMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 个人换绑数据访问对象，统一封装换绑查询条件和锁语义。
 *
 * <p>本能力的查询条件、锁语义和 Mapper 调用均收敛在此边界。</p>
 */
@RequiredArgsConstructor
@Repository
public class PersonRebindDao {

    private final PersonRebindMapper mapper;

    /**
     * 查询持久化数据（selectProbeStatus）。
     */
    public String selectProbeStatus(String identityKey) {
        return mapper.selectProbeStatus(identityKey);
    }

    /**
     * 查询持久化数据（selectExactCandidate）。
     */
    public PersonRebindCandidateRow selectExactCandidate(String fullName, String documentTypeCode, String documentNumber, String identityKey, String gender, LocalDate birthDate, LocalDate validFrom, LocalDate validUntil) {
        return mapper.selectExactCandidate(fullName, documentTypeCode, documentNumber, identityKey, gender, birthDate, validFrom, validUntil);
    }

    /**
     * 加锁查询持久化数据（lockOpenApplication）。
     */
    public PersonApplicationRow lockOpenApplication(long userId) {
        return mapper.lockOpenApplication(userId);
    }

    /**
     * 加锁查询持久化数据（lockApplication）。
     */
    public PersonApplicationRow lockApplication(long applicationId) {
        return mapper.lockApplication(applicationId);
    }

    /**
     * 加锁查询持久化数据（lockEffectiveBindingByUser）。
     */
    public PersonBindingRow lockEffectiveBindingByUser(long userId) {
        return mapper.lockEffectiveBindingByUser(userId);
    }

    /**
     * 加锁查询持久化数据（lockFrozenCandidate）。
     */
    public PersonRebindCandidateRow lockFrozenCandidate(long profileId, long bindingId, int bindingVersion) {
        return mapper.lockFrozenCandidate(profileId, bindingId, bindingVersion);
    }

    /**
     * 确认持久化数据（confirmIntent）。
     */
    public int confirmIntent(long applicationId, long userId, long profileId, long bindingId, int bindingVersion, int expectedVersion) {
        return mapper.confirmIntent(applicationId, userId, profileId, bindingId, bindingVersion, expectedVersion);
    }

    /**
     * 加锁查询持久化数据（lockSubmission）。
     */
    public PersonSubmissionRow lockSubmission(long applicationId, int submissionSeq) {
        return mapper.lockSubmission(applicationId, submissionSeq);
    }

    /**
     * 加锁查询持久化数据（lockProfile）。
     */
    public PersonProfileRow lockProfile(long profileId) {
        return mapper.lockProfile(profileId);
    }

    /**
     * 加锁查询持久化数据（lockExpectedBinding）。
     */
    public PersonBindingRow lockExpectedBinding(long bindingId, long profileId, int bindingVersion) {
        return mapper.lockExpectedBinding(bindingId, profileId, bindingVersion);
    }

    /**
     * 加锁查询持久化数据（lockCurrentVersion）。
     */
    public PersonVersionRow lockCurrentVersion(long profileId) {
        return mapper.lockCurrentVersion(profileId);
    }

    /**
     * 置换持久化数据（supersedeVersion）。
     */
    public int supersedeVersion(long versionId) {
        return mapper.supersedeVersion(versionId);
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
     * 解除绑定持久化数据（unbind）。
     */
    public int unbind(long bindingId, int bindingVersion, long actorUserId, Instant occurredTime) {
        return mapper.unbind(bindingId, bindingVersion, actorUserId, occurredTime);
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
}
