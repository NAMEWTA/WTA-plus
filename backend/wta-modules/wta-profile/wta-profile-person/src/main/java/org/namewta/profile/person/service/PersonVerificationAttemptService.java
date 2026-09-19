package org.namewta.profile.person.service;
import org.namewta.profile.person.domain.exception.PersonVerificationException;
import org.namewta.profile.person.domain.verification.PersonApplicationVerificationState;
import org.namewta.profile.person.domain.verification.PersonProviderAttemptStatus;
import org.namewta.profile.person.domain.verification.PersonProviderCallbackEnvelope;
import org.namewta.profile.person.domain.verification.PersonProviderStartCommand;
import org.namewta.profile.person.domain.verification.PersonProviderStartResult;
import org.namewta.profile.person.domain.verification.PersonVerificationAttempt;
import org.namewta.profile.person.domain.verification.PersonVerificationCallbackOutcome;
import org.namewta.profile.person.domain.verification.PersonVerificationFailureCategory;
import org.namewta.profile.person.domain.verification.PersonVerificationStartAttemptCommand;
import org.namewta.profile.person.domain.verification.PersonVerifiedCallback;
import org.namewta.profile.person.domain.model.read.PersonVerificationApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonVerificationAttemptRow;
import org.namewta.profile.person.dao.PersonVerificationAttemptDao;
import org.namewta.profile.person.port.provider.PersonVerificationProvider;
import org.namewta.profile.person.port.provider.PersonVerificationProviderRegistryPort;
import org.namewta.profile.person.adapter.codec.PersonVerificationEvidenceCodec;
import org.namewta.profile.person.port.verification.PersonVerificationService;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.util.Optional;
/**
 * 创建个人认证尝试协调器。
 */
@Service
public class PersonVerificationAttemptService implements PersonVerificationService {
    private final PersonVerificationProviderRegistryPort providerRegistry;
    private final PersonVerificationAttemptDao dao;
    private final PersonVerificationEvidenceCodec evidenceCodec;

    /** 使用正式供应商端口与持久化依赖装配认证尝试。 */
    @Autowired
    public PersonVerificationAttemptService(PersonVerificationProviderRegistryPort providerRegistry,
                                                PersonVerificationAttemptDao dao,
                                                PersonVerificationEvidenceCodec evidenceCodec) {
        this.providerRegistry = providerRegistry;
        this.dao = dao;
        this.evidenceCodec = evidenceCodec;
    }

    /**
     * 启动认证尝试
     */

    @Override
    public PersonVerificationAttempt startAttempt(PersonVerificationStartAttemptCommand command) {
        PersonApplicationVerificationState application = lockApplication(command.applicationId());
        if (application.submissionId() != command.submissionId()) {
            throw new PersonVerificationException(
                PersonVerificationFailureCategory.STALE_SUBMISSION,
                "Person verification submission is stale");
        }
        if (application.terminal()) {
            throw new PersonVerificationException(
                PersonVerificationFailureCategory.APPLICATION_TERMINAL,
                "Person verification application is terminal");
        }
        PersonVerificationProvider provider = providerRegistry.requireEnabled(application.providerCode());
        int attemptNo = dao.nextAttemptNo(application.applicationId());
        PersonProviderStartResult result = provider.start(new PersonProviderStartCommand(
            application.applicationId(), application.submissionId(), attemptNo, command.requestFingerprint()));
        return append(PersonVerificationAttempt.fromStart(
            application, attemptNo, command.requestFingerprint(), result));
    }
    /**
     * 处理认证回调并更新尝试状态
     */

    @Override
    public PersonVerificationCallbackOutcome handleCallback(String providerCode,
                                                            PersonProviderCallbackEnvelope envelope,
                                                            java.time.Instant receivedAt) {
        PersonVerifiedCallback callback;
        try {
            PersonVerificationProvider provider = providerRegistry.requireEnabled(providerCode);
            callback = provider.authenticate(envelope, receivedAt);
        } catch (PersonVerificationException failure) {
            recordSecurityAudit(null, failure.category(), receivedAt);
            throw failure;
        }
        PersonVerificationAttempt attempt = lockByProviderRequest(providerCode, callback.providerRequestId())
            .orElseThrow(() -> auditedFailure(
                null,
                PersonVerificationFailureCategory.ATTEMPT_NOT_FOUND,
                "Person verification attempt was not found",
                receivedAt));
        PersonApplicationVerificationState application =
            lockApplication(attempt.applicationId());
        if (!application.providerCode().equals(providerCode)) {
            throw auditedFailure(
                application.applicationId(),
                PersonVerificationFailureCategory.PROVIDER_MISMATCH,
                "Person verification provider does not match the fixed application provider",
                receivedAt);
        }
        if (application.terminal()) {
            recordSecurityAudit(
                application.applicationId(), PersonVerificationFailureCategory.LATE_CALLBACK, receivedAt);
            return PersonVerificationCallbackOutcome.LATE_IGNORED;
        }
        if (attempt.status() != PersonProviderAttemptStatus.PENDING) {
            if (attempt.sameCallback(callback)) {
                return PersonVerificationCallbackOutcome.IDEMPOTENT;
            }
            throw auditedFailure(
                application.applicationId(),
                PersonVerificationFailureCategory.CONFLICTING_CALLBACK,
                "Person provider callback conflicts with completed evidence",
                receivedAt);
        }
        complete(attempt.verificationAttemptId(), callback);
        return PersonVerificationCallbackOutcome.ACCEPTED;
    }
    /**
     * 锁定申请记录
     */
    private PersonApplicationVerificationState lockApplication(long applicationId) {
        PersonVerificationApplicationRow row = dao.lockApplication(applicationId);
        if (row == null) {
            throw new PersonVerificationException(
                PersonVerificationFailureCategory.APPLICATION_NOT_FOUND,
                "Person verification application or current submission was not found");
        }
        return new PersonApplicationVerificationState(
            row.getApplicationId(), row.getSubmissionId(), row.getProviderCode(), row.getStatus());
    }
    /**
     * 追加认证尝试记录
     */
    private PersonVerificationAttempt append(PersonVerificationAttempt attempt) {
        PersonVerificationAttemptRow row = toRow(attempt);
        row.setVerificationAttemptId(IdGeneratorUtil.nextLongId());
        try {
            if (dao.insertAttempt(row) != 1) {
                throw providerFailure("Person verification attempt could not be appended");
            }
        } catch (DuplicateKeyException failure) {
            throw new PersonVerificationException(
                PersonVerificationFailureCategory.PROVIDER_FAILURE,
                "Person verification attempt conflicts with an existing provider request",
                failure);
        }
        return toDomain(row);
    }
    /**
     * 按提供方请求编号锁定认证尝试
     */
    private Optional<PersonVerificationAttempt> lockByProviderRequest(String providerCode,
                                                                       String providerRequestId) {
        return Optional.ofNullable(dao.lockByProviderRequest(providerCode, providerRequestId))
            .map(this::toDomain);
    }
    /**
     * 完成认证尝试
     */
    private void complete(long verificationAttemptId, PersonVerifiedCallback callback) {
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
            throw providerFailure("Person verification attempt completion lost its pending fence");
        }
    }
    /**
     * 转换领域对象为持久化读模型
     */
    private PersonVerificationAttemptRow toRow(PersonVerificationAttempt attempt) {
        PersonVerificationAttemptRow row = new PersonVerificationAttemptRow();
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
    private PersonVerificationAttempt toDomain(PersonVerificationAttemptRow row) {
        PersonVerificationEvidenceCodec.DecodedEvidence evidence =
            evidenceCodec.decode(row.getProviderEvidenceJson());
        return new PersonVerificationAttempt(
            row.getVerificationAttemptId(), row.getApplicationId(), row.getSubmissionId(),
            row.getProviderCode(), row.getProviderRequestId(), row.getRequestFingerprint(),
            evidence.callbackDigest(), row.getAttemptNo(), PersonProviderAttemptStatus.valueOf(row.getStatus()),
            row.getNormalizedResultJson(), evidence.providerEvidenceJson(), row.getErrorCode(),
            row.getCompletedTime());
    }
    /**
     * 构造认证提供方失败结果
     */
    private PersonVerificationException providerFailure(String message) {
        return new PersonVerificationException(PersonVerificationFailureCategory.PROVIDER_FAILURE, message);
    }
    /**
     * 记录已审计的认证失败结果
     */
    private PersonVerificationException auditedFailure(Long applicationId,
                                                       PersonVerificationFailureCategory category,
                                                       String message,
                                                       java.time.Instant occurredAt) {
        recordSecurityAudit(applicationId, category, occurredAt);
        return new PersonVerificationException(category, message);
    }

    /** 写入认证安全审计记录。 */
    private void recordSecurityAudit(Long applicationId, PersonVerificationFailureCategory category,
                                     java.time.Instant occurredAt) {
        String result = category == PersonVerificationFailureCategory.LATE_CALLBACK ? "IGNORED" : "FAILED";
        if (dao.insertSecurityAudit(IdGeneratorUtil.nextLongId(), applicationId, result, category.name(), occurredAt) != 1) {
            throw new PersonVerificationException(PersonVerificationFailureCategory.PROVIDER_FAILURE,
                "Person verification security audit could not be appended");
        }
    }
}
