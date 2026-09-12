package org.namewta.profile.enterprise.dao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.namewta.profile.enterprise.domain.ProfileEnterprise;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseAdminRows.*;
import org.namewta.profile.enterprise.mapper.EnterpriseAdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 企业管理数据访问对象，统一封装管理端查询条件和锁语义。
 *
 * <p>本能力的查询条件、锁语义和 Mapper 调用均收敛在此边界。</p>
 */
@RequiredArgsConstructor
@Repository
public class EnterpriseAdminDao {

    private final EnterpriseAdminMapper mapper;

    /**
     * 统计持久化数据（countProfiles）。
     */
    public long countProfiles(String enterpriseName, String unifiedCreditCode, String status) {
        return mapper.countProfiles(enterpriseName, unifiedCreditCode, status);
    }

    /**
     * 查询持久化数据（selectProfiles）。
     */
    public List<ProfileRow> selectProfiles(String enterpriseName, String unifiedCreditCode, String status, int limit, int offset) {
        return mapper.selectProfiles(enterpriseName, unifiedCreditCode, status, limit, offset);
    }

    /**
     * 查询持久化数据（selectProfile）。
     */
    public ProfileRow selectProfile(long profileId) {
        return mapper.selectProfile(profileId);
    }

    /**
     * 加锁查询持久化数据（lockProfile）。
     */
    public ProfileRow lockProfile(long profileId) {
        return mapper.lockProfile(profileId);
    }

    /**
     * 查询持久化数据（selectVersions）。
     */
    public List<VersionRow> selectVersions(long profileId) {
        return mapper.selectVersions(profileId);
    }

    /**
     * 加锁查询持久化数据（lockCurrentVersion）。
     */
    public VersionRow lockCurrentVersion(long profileId) {
        return mapper.lockCurrentVersion(profileId);
    }

    /**
     * 查询持久化数据（selectBindings）。
     */
    public List<BindingRow> selectBindings(long profileId) {
        return mapper.selectBindings(profileId);
    }

    /**
     * 加锁查询持久化数据（lockEffectiveBinding）。
     */
    public BindingRow lockEffectiveBinding(long profileId) {
        return mapper.lockEffectiveBinding(profileId);
    }

    /**
     * 统计持久化数据（countEffectiveBindingByUser）。
     */
    public int countEffectiveBindingByUser(long userId) {
        return mapper.countEffectiveBindingByUser(userId);
    }

    /**
     * 查询持久化数据（selectSources）。
     */
    public List<SourceRow> selectSources(long profileId) {
        return mapper.selectSources(profileId);
    }

    /**
     * 查询持久化数据（selectAudits）。
     */
    public List<AuditRow> selectAudits(long profileId) {
        return mapper.selectAudits(profileId);
    }

    /**
     * 查询持久化数据（selectReview）。
     */
    public ReviewRow selectReview(long applicationId) {
        return mapper.selectReview(applicationId);
    }

    /**
     * 加锁查询持久化数据（lockWaitingApplication）。
     */
    public ReviewRow lockWaitingApplication(long applicationId) {
        return mapper.lockWaitingApplication(applicationId);
    }

    /**
     * 标记持久化数据（markOverridePending）。
     */
    public int markOverridePending(long applicationId, String decision, String reason, int decisionVersion, int version, long operatorId, Instant now) {
        return mapper.markOverridePending(applicationId, decision, reason, decisionVersion, version, operatorId, now);
    }

    /**
     * 新增持久化数据（insertDecision）。
     */
    public int insertDecision(long id, long applicationId, long submissionId, int decisionVersion, String decision, long operatorId, String reason, Instant now) {
        return mapper.insertDecision(id, applicationId, submissionId, decisionVersion, decision, operatorId, reason, now);
    }

    /**
     * 恢复持久化数据（resumeWaiting）。
     */
    public int resumeWaiting(long applicationId, int decisionVersion, long operatorId) {
        return mapper.resumeWaiting(applicationId, decisionVersion, operatorId);
    }

    /**
     * 标记持久化数据（markApproved）。
     */
    public int markApproved(long applicationId, long operatorId, String reason, Instant now) {
        return mapper.markApproved(applicationId, operatorId, reason, now);
    }

    /**
     * 标记持久化数据（markRejected）。
     */
    public int markRejected(long applicationId, int decisionVersion, long operatorId, String reason, Instant now) {
        return mapper.markRejected(applicationId, decisionVersion, operatorId, reason, now);
    }

    /**
     * 处理持久化数据（finalizeDecision）。
     */
    public int finalizeDecision(long applicationId, int decisionVersion, long operatorId, Instant now) {
        return mapper.finalizeDecision(applicationId, decisionVersion, operatorId, now);
    }

    /**
     * 新增持久化数据（insertProfile）。
     */
    public int insertProfile(long profileId, String enterpriseName, String unifiedCreditCode, String enterpriseType, String legalRepresentativeName, String legalDocumentTypeCode, String legalDocumentNumber, LocalDate establishedDate, LocalDate businessTermFrom, LocalDate businessTermUntil, String registeredAddress, String businessScope, String contactName, String contactPhone, String email, BigDecimal registeredCapital, String industryCode, String website, long operatorId, Instant now) {
        return mapper.insertProfile(profileId, enterpriseName, unifiedCreditCode, enterpriseType, legalRepresentativeName, legalDocumentTypeCode, legalDocumentNumber, establishedDate, businessTermFrom, businessTermUntil, registeredAddress, businessScope, contactName, contactPhone, email, registeredCapital, industryCode, website, operatorId, now);
    }

    /**
     * 新增持久化数据（insertSource）。
     */
    public int insertSource(long sourceId, long profileId, String sourceType, long operatorId, String reason, String enterpriseName, String unifiedCreditCode, String identityKey, String enterpriseType, String legalRepresentativeName, String legalDocumentTypeCode, String legalDocumentNumber, LocalDate establishedDate, LocalDate businessTermFrom, LocalDate businessTermUntil, String registeredAddress, String businessScope, String contactName, String contactPhone, String email, BigDecimal registeredCapital, String industryCode, String website, String json, Instant now) {
        return mapper.insertSource(sourceId, profileId, sourceType, operatorId, reason, enterpriseName, unifiedCreditCode, identityKey, enterpriseType, legalRepresentativeName, legalDocumentTypeCode, legalDocumentNumber, establishedDate, businessTermFrom, businessTermUntil, registeredAddress, businessScope, contactName, contactPhone, email, registeredCapital, industryCode, website, json, now);
    }

    /**
     * 新增持久化数据（insertVersion）。
     */
    public int insertVersion(long versionId, long profileId, int versionNo, String sourceType, long sourceId, String enterpriseName, String unifiedCreditCode, String enterpriseType, String legalRepresentativeName, String legalDocumentTypeCode, String legalDocumentNumber, LocalDate establishedDate, LocalDate businessTermFrom, LocalDate businessTermUntil, String registeredAddress, String businessScope, String contactName, String contactPhone, String email, BigDecimal registeredCapital, String industryCode, String website, long operatorId, Instant now) {
        return mapper.insertVersion(versionId, profileId, versionNo, sourceType, sourceId, enterpriseName, unifiedCreditCode, enterpriseType, legalRepresentativeName, legalDocumentTypeCode, legalDocumentNumber, establishedDate, businessTermFrom, businessTermUntil, registeredAddress, businessScope, contactName, contactPhone, email, registeredCapital, industryCode, website, operatorId, now);
    }

    /**
     * 置换持久化数据（supersedeVersion）。
     */
    public int supersedeVersion(long versionId, long operatorId, Instant now) {
        return mapper.supersedeVersion(versionId, operatorId, now);
    }

    /**
     * 更新持久化数据（updateProfileVersion）。
     */
    public int updateProfileVersion(long profileId, long versionId, String enterpriseName, String unifiedCreditCode, String enterpriseType, String legalRepresentativeName, String legalDocumentTypeCode, String legalDocumentNumber, LocalDate establishedDate, LocalDate businessTermFrom, LocalDate businessTermUntil, String registeredAddress, String businessScope, String contactName, String contactPhone, String email, BigDecimal registeredCapital, String industryCode, String website, int expectedVersion, long operatorId, Instant now) {
        return mapper.updateProfileVersion(profileId, versionId, enterpriseName, unifiedCreditCode, enterpriseType, legalRepresentativeName, legalDocumentTypeCode, legalDocumentNumber, establishedDate, businessTermFrom, businessTermUntil, registeredAddress, businessScope, contactName, contactPhone, email, registeredCapital, industryCode, website, expectedVersion, operatorId, now);
    }

    /**
     * 新增持久化数据（insertBinding）。
     */
    public int insertBinding(long bindingId, long profileId, long userId, String sourceType, Long sourceId, long operatorId, Instant now) {
        return mapper.insertBinding(bindingId, profileId, userId, sourceType, sourceId, operatorId, now);
    }

    /**
     * 更新持久化数据（updateBinding）。
     */
    public int updateBinding(long bindingId, String sourceStatus, String targetStatus, int expectedVersion, long operatorId, Instant now) {
        return mapper.updateBinding(bindingId, sourceStatus, targetStatus, expectedVersion, operatorId, now);
    }

    /**
     * 新增持久化数据（insertBindingEvent）。
     */
    public int insertBindingEvent(long eventId, long bindingId, long profileId, long userId, String eventType, int bindingVersion, Long sourceId, String reason, long operatorId, Instant now) {
        return mapper.insertBindingEvent(eventId, bindingId, profileId, userId, eventType, bindingVersion, sourceId, reason, operatorId, now);
    }

    /**
     * 克隆持久化数据（cloneVersionMaterials）。
     */
    public int cloneVersionMaterials(long versionId, long sourceId, long newIdBase, long operatorId, Instant now) {
        return mapper.cloneVersionMaterials(versionId, sourceId, newIdBase, operatorId, now);
    }

    /**
     * 撤销持久化数据（revokeProfile）。
     */
    public int revokeProfile(long profileId, int expectedVersion, String reason, long operatorId, Instant now) {
        return mapper.revokeProfile(profileId, expectedVersion, reason, operatorId, now);
    }

    /**
     * 新增持久化数据（insertAudit）。
     */
    public int insertAudit(long auditId, Long profileId, Long applicationId, Long bindingId, String operationType, long operatorId, String capability, String reason, String beforeStatus, String afterStatus, Instant now) {
        return mapper.insertAudit(auditId, profileId, applicationId, bindingId, operationType, operatorId, capability, reason, beforeStatus, afterStatus, now);
    }
}
