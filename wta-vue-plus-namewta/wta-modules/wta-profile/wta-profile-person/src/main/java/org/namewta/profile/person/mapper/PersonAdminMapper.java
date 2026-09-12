package org.namewta.profile.person.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.person.domain.ProfilePerson;
import org.apache.ibatis.annotations.Param;
import org.namewta.profile.person.domain.model.read.PersonAdminRows.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * PersonAdminMapper 持久化映射器，负责本能力的数据映射。
 */
public interface PersonAdminMapper extends BaseMapperPlus<ProfilePerson, ProfilePerson> {

    /**
     * 定义统计映射（countProfiles）。
     */
    long countProfiles(@Param("fullName") String fullName, @Param("documentNumber") String documentNumber,
                       @Param("status") String status);

    /**
     * 定义查询映射（selectProfiles）。
     */
    List<ProfileRow> selectProfiles(@Param("fullName") String fullName,
                                    @Param("documentNumber") String documentNumber,
                                    @Param("status") String status,
                                    @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 定义查询映射（selectProfile）。
     */
    ProfileRow selectProfile(@Param("profileId") long profileId);

    /**
     * 定义加锁查询映射（lockProfile）。
     */
    ProfileRow lockProfile(@Param("profileId") long profileId);

    /**
     * 定义查询映射（selectVersions）。
     */
    List<VersionRow> selectVersions(@Param("profileId") long profileId);

    /**
     * 定义加锁查询映射（lockCurrentVersion）。
     */
    VersionRow lockCurrentVersion(@Param("profileId") long profileId);

    /**
     * 定义查询映射（selectBindings）。
     */
    List<BindingRow> selectBindings(@Param("profileId") long profileId);

    /**
     * 定义加锁查询映射（lockEffectiveBinding）。
     */
    BindingRow lockEffectiveBinding(@Param("profileId") long profileId);

    /**
     * 定义统计映射（countEffectiveBindingByUser）。
     */
    int countEffectiveBindingByUser(@Param("userId") long userId);

    /**
     * 定义查询映射（selectSources）。
     */
    List<SourceRow> selectSources(@Param("profileId") long profileId);

    /**
     * 定义查询映射（selectAudits）。
     */
    List<AuditRow> selectAudits(@Param("profileId") long profileId);

    /**
     * 定义查询映射（selectReview）。
     */
    ReviewRow selectReview(@Param("applicationId") long applicationId);

    /**
     * 定义加锁查询映射（lockWaitingApplication）。
     */
    ReviewRow lockWaitingApplication(@Param("applicationId") long applicationId);

    /**
     * 定义标记映射（markOverridePending）。
     */
    int markOverridePending(@Param("applicationId") long applicationId, @Param("decision") String decision,
                            @Param("reason") String reason, @Param("decisionVersion") int decisionVersion,
                            @Param("version") int version, @Param("operatorId") long operatorId,
                            @Param("now") Instant now);

    /**
     * 定义新增映射（insertDecision）。
     */
    int insertDecision(@Param("id") long id, @Param("applicationId") long applicationId,
                       @Param("submissionId") long submissionId, @Param("decisionVersion") int decisionVersion,
                       @Param("decision") String decision, @Param("operatorId") long operatorId,
                       @Param("reason") String reason, @Param("now") Instant now);

    /**
     * 定义恢复映射（resumeWaiting）。
     */
    int resumeWaiting(@Param("applicationId") long applicationId,
                      @Param("decisionVersion") int decisionVersion, @Param("operatorId") long operatorId);

    /**
     * 定义标记映射（markApproved）。
     */
    int markApproved(@Param("applicationId") long applicationId, @Param("operatorId") long operatorId,
                     @Param("reason") String reason, @Param("now") Instant now);

    /**
     * 定义标记映射（markRejected）。
     */
    int markRejected(@Param("applicationId") long applicationId, @Param("decisionVersion") int decisionVersion,
                     @Param("operatorId") long operatorId, @Param("reason") String reason,
                     @Param("now") Instant now);

    /**
     * 定义处理映射（finalizeDecision）。
     */
    int finalizeDecision(@Param("applicationId") long applicationId,
                         @Param("decisionVersion") int decisionVersion,
                         @Param("operatorId") long operatorId, @Param("now") Instant now);

    /**
     * 定义新增映射（insertProfile）。
     */
    int insertProfile(@Param("profileId") long profileId, @Param("fullName") String fullName,
                      @Param("documentType") String documentType, @Param("documentNumber") String documentNumber,
                      @Param("identityKey") String identityKey, @Param("gender") String gender,
                      @Param("birthDate") LocalDate birthDate, @Param("validFrom") LocalDate validFrom,
                      @Param("validUntil") LocalDate validUntil, @Param("operatorId") long operatorId,
                      @Param("now") Instant now);

    /**
     * 定义新增映射（insertSource）。
     */
    int insertSource(@Param("sourceId") long sourceId, @Param("profileId") long profileId,
                     @Param("sourceType") String sourceType, @Param("operatorId") long operatorId,
                     @Param("reason") String reason, @Param("fullName") String fullName,
                     @Param("documentType") String documentType, @Param("documentNumber") String documentNumber,
                     @Param("identityKey") String identityKey, @Param("gender") String gender,
                     @Param("birthDate") LocalDate birthDate, @Param("validFrom") LocalDate validFrom,
                     @Param("validUntil") LocalDate validUntil, @Param("json") String json,
                     @Param("now") Instant now);

    /**
     * 定义新增映射（insertVersion）。
     */
    int insertVersion(@Param("versionId") long versionId, @Param("profileId") long profileId,
                      @Param("versionNo") int versionNo, @Param("sourceType") String sourceType,
                      @Param("sourceId") long sourceId, @Param("fullName") String fullName,
                      @Param("documentType") String documentType, @Param("documentNumber") String documentNumber,
                      @Param("identityKey") String identityKey, @Param("gender") String gender,
                      @Param("birthDate") LocalDate birthDate, @Param("validFrom") LocalDate validFrom,
                      @Param("validUntil") LocalDate validUntil, @Param("operatorId") long operatorId,
                      @Param("now") Instant now);

    /**
     * 定义置换映射（supersedeVersion）。
     */
    int supersedeVersion(@Param("versionId") long versionId, @Param("operatorId") long operatorId,
                         @Param("now") Instant now);

    /**
     * 定义更新映射（updateProfileVersion）。
     */
    int updateProfileVersion(@Param("profileId") long profileId, @Param("versionId") long versionId,
                             @Param("fullName") String fullName, @Param("documentType") String documentType,
                             @Param("documentNumber") String documentNumber, @Param("identityKey") String identityKey,
                             @Param("gender") String gender, @Param("birthDate") LocalDate birthDate,
                             @Param("validFrom") LocalDate validFrom, @Param("validUntil") LocalDate validUntil,
                             @Param("expectedVersion") int expectedVersion, @Param("operatorId") long operatorId,
                             @Param("now") Instant now);

    /**
     * 定义新增映射（insertBinding）。
     */
    int insertBinding(@Param("bindingId") long bindingId, @Param("profileId") long profileId,
                      @Param("userId") long userId, @Param("sourceType") String sourceType,
                      @Param("sourceId") Long sourceId, @Param("operatorId") long operatorId,
                      @Param("now") Instant now);

    /**
     * 定义更新映射（updateBinding）。
     */
    int updateBinding(@Param("bindingId") long bindingId, @Param("sourceStatus") String sourceStatus,
                      @Param("targetStatus") String targetStatus, @Param("expectedVersion") int expectedVersion,
                      @Param("operatorId") long operatorId, @Param("now") Instant now);

    /**
     * 定义新增映射（insertBindingEvent）。
     */
    int insertBindingEvent(@Param("eventId") long eventId, @Param("bindingId") long bindingId,
                           @Param("profileId") long profileId, @Param("userId") long userId,
                           @Param("eventType") String eventType, @Param("bindingVersion") int bindingVersion,
                           @Param("sourceId") Long sourceId, @Param("reason") String reason,
                           @Param("operatorId") long operatorId, @Param("now") Instant now);

    /**
     * 定义克隆映射（cloneVersionMaterials）。
     */
    int cloneVersionMaterials(@Param("versionId") long versionId, @Param("sourceId") long sourceId,
                              @Param("newIdBase") long newIdBase, @Param("operatorId") long operatorId,
                              @Param("now") Instant now);

    /**
     * 定义撤销映射（revokeProfile）。
     */
    int revokeProfile(@Param("profileId") long profileId, @Param("expectedVersion") int expectedVersion,
                      @Param("reason") String reason, @Param("operatorId") long operatorId,
                      @Param("now") Instant now);

    /**
     * 定义新增映射（insertAudit）。
     */
    int insertAudit(@Param("auditId") long auditId, @Param("profileId") Long profileId,
                    @Param("applicationId") Long applicationId, @Param("bindingId") Long bindingId,
                    @Param("operationType") String operationType, @Param("operatorId") long operatorId,
                    @Param("capability") String capability, @Param("reason") String reason,
                    @Param("beforeStatus") String beforeStatus, @Param("afterStatus") String afterStatus,
                    @Param("now") Instant now);
}
