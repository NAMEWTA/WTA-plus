package org.namewta.profile.person.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.person.domain.ProfilePersonBinding;
import org.apache.ibatis.annotations.Param;
import org.namewta.profile.person.domain.model.read.PersonApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonBindingEventRow;
import org.namewta.profile.person.domain.model.read.PersonBindingRow;
import org.namewta.profile.person.domain.model.read.PersonProfileRow;
import org.namewta.profile.person.domain.model.read.PersonSubmissionRow;
import org.namewta.profile.person.domain.model.read.PersonVersionRow;
import org.namewta.profile.person.domain.model.read.PersonRebindCandidateRow;

import java.time.Instant;
import java.time.LocalDate;

/**
 * PersonRebindMapper 持久化映射器，负责本能力的数据映射。
 */
public interface PersonRebindMapper extends BaseMapperPlus<ProfilePersonBinding, ProfilePersonBinding> {

    /**
     * 定义查询映射（selectProbeStatus）。
     */
    String selectProbeStatus(@Param("identityKey") String identityKey);

    /**
     * 定义查询映射（selectExactCandidate）。
     */
    RebindCandidateRow selectExactCandidate(@Param("fullName") String fullName,
                                            @Param("documentTypeCode") String documentTypeCode,
                                            @Param("documentNumber") String documentNumber,
                                            @Param("identityKey") String identityKey,
                                            @Param("gender") String gender,
                                            @Param("birthDate") LocalDate birthDate,
                                            @Param("validFrom") LocalDate validFrom,
                                            @Param("validUntil") LocalDate validUntil);

    /**
     * 定义加锁查询映射（lockOpenApplication）。
     */
    PersonApplicationRow lockOpenApplication(@Param("userId") long userId);

    /**
     * 定义加锁查询映射（lockApplication）。
     */
    PersonApplicationRow lockApplication(@Param("applicationId") long applicationId);

    /**
     * 定义加锁查询映射（lockEffectiveBindingByUser）。
     */
    PersonBindingRow lockEffectiveBindingByUser(@Param("userId") long userId);

    /**
     * 定义加锁查询映射（lockFrozenCandidate）。
     */
    RebindCandidateRow lockFrozenCandidate(@Param("profileId") long profileId,
                                           @Param("bindingId") long bindingId,
                                           @Param("bindingVersion") int bindingVersion);

    /**
     * 定义确认映射（confirmIntent）。
     */
    int confirmIntent(@Param("applicationId") long applicationId,
                      @Param("userId") long userId,
                      @Param("profileId") long profileId,
                      @Param("bindingId") long bindingId,
                      @Param("bindingVersion") int bindingVersion,
                      @Param("expectedVersion") int expectedVersion);

    /**
     * 定义加锁查询映射（lockSubmission）。
     */
    PersonSubmissionRow lockSubmission(@Param("applicationId") long applicationId,
                                        @Param("submissionSeq") int submissionSeq);

    /**
     * 定义加锁查询映射（lockProfile）。
     */
    PersonProfileRow lockProfile(@Param("profileId") long profileId);

    /**
     * 定义加锁查询映射（lockExpectedBinding）。
     */
    PersonBindingRow lockExpectedBinding(@Param("bindingId") long bindingId,
                                         @Param("profileId") long profileId,
                                         @Param("bindingVersion") int bindingVersion);

    /**
     * 定义加锁查询映射（lockCurrentVersion）。
     */
    PersonVersionRow lockCurrentVersion(@Param("profileId") long profileId);

    /**
     * 定义置换映射（supersedeVersion）。
     */
    int supersedeVersion(@Param("versionId") long versionId);

    /**
     * 定义新增映射（insertVersion）。
     */
    int insertVersion(PersonVersionRow row);

    /**
     * 定义更新映射（updateProfile）。
     */
    int updateProfile(PersonProfileRow row);

    /**
     * 定义解除绑定映射（unbind）。
     */
    int unbind(@Param("bindingId") long bindingId,
               @Param("bindingVersion") int bindingVersion,
               @Param("actorUserId") long actorUserId,
               @Param("occurredTime") Instant occurredTime);

    /**
     * 定义新增映射（insertBinding）。
     */
    int insertBinding(PersonBindingRow row);

    /**
     * 定义新增映射（insertBindingEvent）。
     */
    int insertBindingEvent(PersonBindingEventRow row);

    /**
     * 定义完成映射（finishApplication）。
     */
    int finishApplication(@Param("applicationId") long applicationId,
                          @Param("snapshotVersion") int snapshotVersion,
                          @Param("decisionVersion") int decisionVersion,
                          @Param("expectedVersion") int expectedVersion,
                          @Param("finishedTime") Instant finishedTime);

    /** RebindCandidateRow 持久化映射器，负责本能力的数据映射。  Mapper-specific subtype retained for XML result mapping compatibility. */
    class RebindCandidateRow extends PersonRebindCandidateRow {
    }
}
