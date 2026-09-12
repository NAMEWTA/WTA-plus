package org.namewta.profile.person.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.person.domain.ProfilePersonApplication;
import org.apache.ibatis.annotations.Param;
import org.namewta.profile.person.domain.model.read.PersonActiveProjectionRow;
import org.namewta.profile.person.domain.model.read.PersonActiveIdentityMatchRow;
import org.namewta.profile.person.domain.model.read.PersonApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonBindingEventRow;
import org.namewta.profile.person.domain.model.read.PersonBindingRow;
import org.namewta.profile.person.domain.model.read.PersonDocumentTypeRow;
import org.namewta.profile.person.domain.model.read.PersonProfileRow;
import org.namewta.profile.person.domain.model.read.PersonSubmissionRow;
import org.namewta.profile.person.domain.model.read.PersonVersionRow;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * PersonApplicationMapper 持久化映射器，负责本能力的数据映射。
 */
public interface PersonApplicationMapper extends BaseMapperPlus<ProfilePersonApplication, ProfilePersonApplication> {

    /**
     * 定义查询映射（selectActiveIdentityMatches）。
     */
    List<PersonActiveIdentityMatchRow> selectActiveIdentityMatches(
        @Param("fullName") String fullName,
        @Param("documentLastFour") String documentLastFour);

    /**
     * 定义加锁查询映射（lockActiveIdentityMatch）。
     */
    PersonActiveIdentityMatchRow lockActiveIdentityMatch(
        @Param("userId") long userId,
        @Param("personProfileId") long personProfileId,
        @Param("fullName") String fullName,
        @Param("documentLastFour") String documentLastFour);

    /**
     * 定义加锁查询映射（lockMaterialWorkingOwner）。
     */
    Long lockMaterialWorkingOwner(@Param("ownerId") long ownerId);

    /**
     * 定义加锁查询映射（lockMaterialSubmissionOwner）。
     */
    Long lockMaterialSubmissionOwner(@Param("ownerId") long ownerId);

    /**
     * 定义加锁查询映射（lockMaterialImmutableOwner）。
     */
    Long lockMaterialImmutableOwner(@Param("ownerType") String ownerType,
                                    @Param("ownerId") long ownerId);

    /**
     * 定义统计映射（countEditableMaterialWorkingOwner）。
     */
    long countEditableMaterialWorkingOwner(@Param("ownerId") long ownerId);

    /**
     * 定义统计映射（countMaterialSnapshotRelationship）。
     */
    long countMaterialSnapshotRelationship(@Param("sourceType") String sourceType,
                                           @Param("sourceId") long sourceId,
                                           @Param("targetType") String targetType,
                                           @Param("targetId") long targetId);

    /**
     * 定义查询映射（selectOpenByUserId）。
     */
    PersonApplicationRow selectOpenByUserId(@Param("userId") long userId);

    /**
     * 定义加锁查询映射（lockOpenByUserId）。
     */
    PersonApplicationRow lockOpenByUserId(@Param("userId") long userId);

    /**
     * 定义加锁查询映射（lockApplicationById）。
     */
    PersonApplicationRow lockApplicationById(@Param("applicationId") long applicationId);

    /**
     * 定义查询映射（selectDocumentType）。
     */
    PersonDocumentTypeRow selectDocumentType(@Param("code") String documentTypeCode);

    /**
     * 定义查询映射（selectActiveProfileIdByIdentity）。
     */
    Long selectActiveProfileIdByIdentity(@Param("identityKey") String identityKey);

    /**
     * 定义查询映射（selectEffectiveProfileIdByUser）。
     */
    Long selectEffectiveProfileIdByUser(@Param("userId") long userId);

    /**
     * 定义新增映射（insertApplication）。
     */
    int insertApplication(PersonApplicationRow row);

    /**
     * 定义更新映射（updateDraft）。
     */
    int updateDraft(PersonApplicationRow row);

    /**
     * 定义新增映射（insertSubmission）。
     */
    int insertSubmission(PersonSubmissionRow row);

    /**
     * 定义查询映射（selectSubmission）。
     */
    PersonSubmissionRow selectSubmission(@Param("applicationId") long applicationId,
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
    PersonProfileRow lockActiveProfileByIdentity(@Param("identityKey") String identityKey);

    /**
     * 定义加锁查询映射（lockActiveProfileById）。
     */
    PersonProfileRow lockActiveProfileById(@Param("profileId") long profileId);

    /**
     * 定义更新映射（updateIdentityGuard）。
     */
    int updateIdentityGuard(@Param("profileId") long profileId,
                            @Param("identityKey") String identityKey);

    /**
     * 定义加锁查询映射（lockLatestRevokedProfileByIdentity）。
     */
    PersonProfileRow lockLatestRevokedProfileByIdentity(@Param("identityKey") String identityKey);

    /**
     * 定义新增映射（insertIdentityGuard）。
     */
    int insertIdentityGuard(@Param("guardId") long guardId,
                            @Param("identityKey") String identityKey,
                            @Param("profileId") long profileId);

    /**
     * 定义新增映射（insertProfile）。
     */
    int insertProfile(PersonProfileRow row);

    /**
     * 定义查询映射（selectCurrentVersionForUpdate）。
     */
    PersonVersionRow selectCurrentVersionForUpdate(@Param("profileId") long profileId);

    /**
     * 定义置换映射（supersedeVersion）。
     */
    int supersedeVersion(@Param("versionId") long personVersionId);

    /**
     * 定义新增映射（insertVersion）。
     */
    int insertVersion(PersonVersionRow row);

    /**
     * 定义更新映射（updateProfile）。
     */
    int updateProfile(PersonProfileRow row);

    /**
     * 定义加锁查询映射（lockEffectiveBindingByUser）。
     */
    PersonBindingRow lockEffectiveBindingByUser(@Param("userId") long userId);

    /**
     * 定义加锁查询映射（lockEffectiveBindingByProfile）。
     */
    PersonBindingRow lockEffectiveBindingByProfile(@Param("profileId") long personProfileId);

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
    List<PersonActiveProjectionRow> selectActiveProjections(@Param("userIds") Set<Long> userIds);
}
