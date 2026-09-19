package org.namewta.profile.enterprise.service;
import org.namewta.profile.enterprise.domain.exception.EnterpriseVerificationException;
import org.namewta.profile.enterprise.domain.verification.EnterpriseApplicationVerificationState;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderAttemptStatus;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderCallbackEnvelope;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderStartCommand;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderStartResult;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationAttempt;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationCallbackOutcome;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationFailureCategory;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationStartAttemptCommand;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerifiedCallback;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVerificationApplicationRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVerificationAttemptRow;
import org.namewta.profile.enterprise.dao.EnterpriseVerificationAttemptDao;
import org.namewta.profile.enterprise.port.provider.EnterpriseVerificationProvider;
import org.namewta.profile.enterprise.port.provider.EnterpriseVerificationProviderRegistryPort;
import org.namewta.profile.enterprise.adapter.codec.EnterpriseVerificationEvidenceCodec;
import org.namewta.profile.enterprise.port.verification.EnterpriseVerificationService;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.util.Optional;
/**
 * 创建企业认证尝试协调器。
 */
@Service
public class EnterpriseVerificationAttemptService implements EnterpriseVerificationService {
    private final EnterpriseVerificationProviderRegistryPort providerRegistry;
    private final EnterpriseVerificationAttemptDao dao;
    private final EnterpriseVerificationEvidenceCodec evidenceCodec;

    /** 使用正式供应商端口与持久化依赖装配认证尝试。 */
    @Autowired
    public EnterpriseVerificationAttemptService(EnterpriseVerificationProviderRegistryPort providerRegistry,
                                                     EnterpriseVerificationAttemptDao dao,
                                                     EnterpriseVerificationEvidenceCodec evidenceCodec) {
        this.providerRegistry = providerRegistry;
        this.dao = dao;
        this.evidenceCodec = evidenceCodec;
    }

    /**
     * 启动认证尝试
     */

    @Override
    public EnterpriseVerificationAttempt startAttempt(EnterpriseVerificationStartAttemptCommand command) {
        EnterpriseApplicationVerificationState application = lockApplication(command.applicationId());
        if (application.submissionId() != command.submissionId()) {
            throw new EnterpriseVerificationException(
                EnterpriseVerificationFailureCategory.STALE_SUBMISSION,
                "Enterprise verification submission is stale");
        }
        if (application.terminal()) {
            throw new EnterpriseVerificationException(
                EnterpriseVerificationFailureCategory.APPLICATION_TERMINAL,
                "Enterprise verification application is terminal");
        }
        EnterpriseVerificationProvider provider = providerRegistry.requireEnabled(application.providerCode());
        int attemptNo = dao.nextAttemptNo(application.applicationId());
        EnterpriseProviderStartResult result = provider.start(new EnterpriseProviderStartCommand(
            application.applicationId(), application.submissionId(), attemptNo, command.requestFingerprint()));
        return append(EnterpriseVerificationAttempt.fromStart(
            application, attemptNo, command.requestFingerprint(), result));
    }
    /**
     * 处理认证回调并更新尝试状态
     */

    @Override
    public EnterpriseVerificationCallbackOutcome handleCallback(String providerCode,
                                                                EnterpriseProviderCallbackEnvelope envelope,
                                                                java.time.Instant receivedAt) {
        EnterpriseVerifiedCallback callback;
        try {
            EnterpriseVerificationProvider provider = providerRegistry.requireEnabled(providerCode);
            callback = provider.authenticate(envelope, receivedAt);
        } catch (EnterpriseVerificationException failure) {
            recordSecurityAudit(null, failure.category(), receivedAt);
            throw failure;
        }
        EnterpriseVerificationAttempt attempt = lockByProviderRequest(providerCode, callback.providerRequestId())
            .orElseThrow(() -> auditedFailure(
                null,
                EnterpriseVerificationFailureCategory.ATTEMPT_NOT_FOUND,
                "Enterprise verification attempt was not found",
                receivedAt));
        EnterpriseApplicationVerificationState application =
            lockApplication(attempt.applicationId());
        if (!application.providerCode().equals(providerCode)) {
            throw auditedFailure(
                application.applicationId(),
                EnterpriseVerificationFailureCategory.PROVIDER_MISMATCH,
                "Enterprise verification provider does not match the fixed application provider",
                receivedAt);
        }
        if (application.terminal()) {
            recordSecurityAudit(
                application.applicationId(), EnterpriseVerificationFailureCategory.LATE_CALLBACK, receivedAt);
            return EnterpriseVerificationCallbackOutcome.LATE_IGNORED;
        }
        if (attempt.status() != EnterpriseProviderAttemptStatus.PENDING) {
            if (attempt.sameCallback(callback)) {
                return EnterpriseVerificationCallbackOutcome.IDEMPOTENT;
            }
            throw auditedFailure(
                application.applicationId(),
                EnterpriseVerificationFailureCategory.CONFLICTING_CALLBACK,
                "Enterprise provider callback conflicts with completed evidence",
                receivedAt);
        }
        complete(attempt.verificationAttemptId(), callback);
        return EnterpriseVerificationCallbackOutcome.ACCEPTED;
    }
    /**
     * 锁定申请记录
     */
    private EnterpriseApplicationVerificationState lockApplication(long applicationId) {
        EnterpriseVerificationApplicationRow row = dao.lockApplication(applicationId);
        if (row == null) {
            throw new EnterpriseVerificationException(
                EnterpriseVerificationFailureCategory.APPLICATION_NOT_FOUND,
                "Enterprise verification application or current submission was not found");
        }
        return new EnterpriseApplicationVerificationState(
            row.getApplicationId(), row.getSubmissionId(), row.getProviderCode(), row.getStatus());
    }
    /**
     * 追加认证尝试记录
     */
    private EnterpriseVerificationAttempt append(EnterpriseVerificationAttempt attempt) {
        EnterpriseVerificationAttemptRow row = toRow(attempt);
        row.setVerificationAttemptId(IdGeneratorUtil.nextLongId());
        try {
            if (dao.insertAttempt(row) != 1) {
                throw providerFailure("Enterprise verification attempt could not be appended");
            }
        } catch (DuplicateKeyException failure) {
            throw new EnterpriseVerificationException(
                EnterpriseVerificationFailureCategory.PROVIDER_FAILURE,
                "Enterprise verification attempt conflicts with an existing provider request",
                failure);
        }
        return toDomain(row);
    }
    /**
     * 按提供方请求编号锁定认证尝试
     */
    private Optional<EnterpriseVerificationAttempt> lockByProviderRequest(String providerCode,
                                                                          String providerRequestId) {
        return Optional.ofNullable(dao.lockByProviderRequest(providerCode, providerRequestId))
            .map(this::toDomain);
    }
    /**
     * 完成认证尝试
     */
    private void complete(long verificationAttemptId, EnterpriseVerifiedCallback callback) {
        String storedEvidence = evidenceCodec.encode(
            callback.callbackDigest(), callback.providerEvidenceJson());
        int updated = dao.completeAttempt(
            verificationAttemptId,
            callback.status().name(),
            callback.normalizedResultJson(),
            storedEvidence,
            callback.errorCode(),
            callback.completedAt());
        if (updated != 1) {
            throw providerFailure("Enterprise verification attempt completion lost its pending fence");
        }
    }
    /**
     * 转换领域对象为持久化读模型
     */
    private EnterpriseVerificationAttemptRow toRow(EnterpriseVerificationAttempt attempt) {
        EnterpriseVerificationAttemptRow row = new EnterpriseVerificationAttemptRow();
        row.setVerificationAttemptId(attempt.verificationAttemptId());
        row.setApplicationId(attempt.applicationId());
        row.setSubmissionId(attempt.submissionId());
        row.setProviderCode(attempt.providerCode());
        row.setProviderRequestId(attempt.providerRequestId());
        row.setRequestFingerprint(attempt.requestFingerprint());
        row.setAttemptNo(attempt.attemptNo());
        row.setStatus(attempt.status().name());
        row.setNormalizedResultJson(attempt.normalizedResultJson());
        row.setProviderEvidenceJson(evidenceCodec.encode(
            attempt.callbackDigest(), attempt.providerEvidenceJson()));
        row.setErrorCode(attempt.errorCode());
        row.setCompletedTime(attempt.completedAt());
        return row;
    }
    /**
     * 转换读模型为领域对象
     */
    private EnterpriseVerificationAttempt toDomain(EnterpriseVerificationAttemptRow row) {
        EnterpriseVerificationEvidenceCodec.DecodedEvidence evidence =
            evidenceCodec.decode(row.getProviderEvidenceJson());
        return new EnterpriseVerificationAttempt(
            row.getVerificationAttemptId(),
            row.getApplicationId(),
            row.getSubmissionId(),
            row.getProviderCode(),
            row.getProviderRequestId(),
            row.getRequestFingerprint(),
            evidence.callbackDigest(),
            row.getAttemptNo(),
            EnterpriseProviderAttemptStatus.valueOf(row.getStatus()),
            row.getNormalizedResultJson(),
            evidence.providerEvidenceJson(),
            row.getErrorCode(),
            row.getCompletedTime());
    }
    /**
     * 构造认证提供方失败结果
     */
    private EnterpriseVerificationException providerFailure(String message) {
        return new EnterpriseVerificationException(EnterpriseVerificationFailureCategory.PROVIDER_FAILURE, message);
    }
    /**
     * 记录已审计的认证失败结果
     */
    private EnterpriseVerificationException auditedFailure(Long applicationId,
                                                           EnterpriseVerificationFailureCategory category,
                                                           String message,
                                                           java.time.Instant occurredAt) {
        recordSecurityAudit(applicationId, category, occurredAt);
        return new EnterpriseVerificationException(category, message);
    }

    /** 写入认证安全审计记录。 */
    private void recordSecurityAudit(Long applicationId, EnterpriseVerificationFailureCategory category,
                                     java.time.Instant occurredAt) {
        String result = category == EnterpriseVerificationFailureCategory.LATE_CALLBACK ? "IGNORED" : "FAILED";
        if (dao.insertSecurityAudit(IdGeneratorUtil.nextLongId(), applicationId, result, category.name(), occurredAt) != 1) {
            throw new EnterpriseVerificationException(EnterpriseVerificationFailureCategory.PROVIDER_FAILURE,
                "Enterprise verification security audit could not be appended");
        }
    }
}
