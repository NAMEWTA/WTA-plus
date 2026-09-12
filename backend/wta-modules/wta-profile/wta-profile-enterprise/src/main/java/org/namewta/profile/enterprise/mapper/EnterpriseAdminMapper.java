package org.namewta.profile.enterprise.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.profile.enterprise.domain.ProfileEnterprise;

import org.apache.ibatis.annotations.Param;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseAdminRows.*;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

/**
 * EnterpriseAdminMapper 持久化映射器，负责本能力的数据映射。
 */
public interface EnterpriseAdminMapper extends BaseMapperPlus<ProfileEnterprise, ProfileEnterprise> {

    /**
     * 定义统计映射（countProfiles）。
     */
    long countProfiles(@Param("enterpriseName") String enterpriseName, @Param("unifiedCreditCode") String unifiedCreditCode,
                       @Param("status") String status);

    /**
     * 定义查询映射（selectProfiles）。
     */
    List<ProfileRow> selectProfiles(@Param("enterpriseName") String enterpriseName,
                                    @Param("unifiedCreditCode") String unifiedCreditCode,
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
    int insertProfile(@Param("profileId") long profileId, @Param("enterpriseName") String enterpriseName,
                      @Param("unifiedCreditCode") String unifiedCreditCode, @Param("enterpriseType") String enterpriseType,
                      @Param("legalRepresentativeName") String legalRepresentativeName,
                      @Param("legalDocumentTypeCode") String legalDocumentTypeCode,
                      @Param("legalDocumentNumber") String legalDocumentNumber,
                      @Param("establishedDate") LocalDate establishedDate,
                      @Param("businessTermFrom") LocalDate businessTermFrom,
                      @Param("businessTermUntil") LocalDate businessTermUntil,
                      @Param("registeredAddress") String registeredAddress, @Param("businessScope") String businessScope,
                      @Param("contactName") String contactName, @Param("contactPhone") String contactPhone,
                      @Param("email") String email, @Param("registeredCapital") BigDecimal registeredCapital,
                      @Param("industryCode") String industryCode, @Param("website") String website,
                      @Param("operatorId") long operatorId,
                      @Param("now") Instant now);

    /**
     * 定义新增映射（insertSource）。
     */
    int insertSource(@Param("sourceId") long sourceId, @Param("profileId") long profileId,
                     @Param("sourceType") String sourceType, @Param("operatorId") long operatorId,
                     @Param("reason") String reason, @Param("enterpriseName") String enterpriseName,
                     @Param("unifiedCreditCode") String unifiedCreditCode, @Param("identityKey") String identityKey,
                     @Param("enterpriseType") String enterpriseType,
                     @Param("legalRepresentativeName") String legalRepresentativeName,
                     @Param("legalDocumentTypeCode") String legalDocumentTypeCode,
                     @Param("legalDocumentNumber") String legalDocumentNumber,
                     @Param("establishedDate") LocalDate establishedDate,
                     @Param("businessTermFrom") LocalDate businessTermFrom,
                     @Param("businessTermUntil") LocalDate businessTermUntil,
                     @Param("registeredAddress") String registeredAddress, @Param("businessScope") String businessScope,
                     @Param("contactName") String contactName, @Param("contactPhone") String contactPhone,
                     @Param("email") String email, @Param("registeredCapital") BigDecimal registeredCapital,
                     @Param("industryCode") String industryCode, @Param("website") String website,
                     @Param("json") String json,
                     @Param("now") Instant now);

    /**
     * 定义新增映射（insertVersion）。
     */
    int insertVersion(@Param("versionId") long versionId, @Param("profileId") long profileId,
                      @Param("versionNo") int versionNo, @Param("sourceType") String sourceType,
                      @Param("sourceId") long sourceId, @Param("enterpriseName") String enterpriseName,
                      @Param("unifiedCreditCode") String unifiedCreditCode, @Param("enterpriseType") String enterpriseType,
                      @Param("legalRepresentativeName") String legalRepresentativeName,
                      @Param("legalDocumentTypeCode") String legalDocumentTypeCode,
                      @Param("legalDocumentNumber") String legalDocumentNumber,
                      @Param("establishedDate") LocalDate establishedDate,
                      @Param("businessTermFrom") LocalDate businessTermFrom,
                      @Param("businessTermUntil") LocalDate businessTermUntil,
                      @Param("registeredAddress") String registeredAddress, @Param("businessScope") String businessScope,
                      @Param("contactName") String contactName, @Param("contactPhone") String contactPhone,
                      @Param("email") String email, @Param("registeredCapital") BigDecimal registeredCapital,
                      @Param("industryCode") String industryCode, @Param("website") String website,
                      @Param("operatorId") long operatorId,
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
                             @Param("enterpriseName") String enterpriseName,
                             @Param("unifiedCreditCode") String unifiedCreditCode,
                             @Param("enterpriseType") String enterpriseType,
                             @Param("legalRepresentativeName") String legalRepresentativeName,
                             @Param("legalDocumentTypeCode") String legalDocumentTypeCode,
                             @Param("legalDocumentNumber") String legalDocumentNumber,
                             @Param("establishedDate") LocalDate establishedDate,
                             @Param("businessTermFrom") LocalDate businessTermFrom,
                             @Param("businessTermUntil") LocalDate businessTermUntil,
                             @Param("registeredAddress") String registeredAddress,
                             @Param("businessScope") String businessScope,
                             @Param("contactName") String contactName, @Param("contactPhone") String contactPhone,
                             @Param("email") String email, @Param("registeredCapital") BigDecimal registeredCapital,
                             @Param("industryCode") String industryCode, @Param("website") String website,
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
