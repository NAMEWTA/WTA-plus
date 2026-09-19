package org.namewta.profile.enterprise.service;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.profile.enterprise.domain.application.EnterpriseDocumentTypeRule;
import org.namewta.profile.enterprise.domain.application.EnterpriseApplication;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationVo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationSaveBo;
import org.namewta.profile.enterprise.domain.application.EnterpriseDraftUpdate;
import org.namewta.profile.enterprise.domain.application.EnterpriseIdentityFields;
import org.namewta.profile.enterprise.domain.application.EnterpriseApplicationProcessCommand;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationProbeBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationProbeVo;
import org.namewta.profile.enterprise.domain.application.EnterprisePublication;
import org.namewta.profile.enterprise.domain.application.EnterpriseSubmission;
import org.namewta.profile.enterprise.domain.exception.EnterpriseApplicationException;
import org.namewta.profile.enterprise.port.EnterpriseApplicationPublicationPort;
import org.namewta.profile.enterprise.port.gateway.EnterpriseWorkflowGateway;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.enterprise.domain.exception.EnterpriseVerificationException;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationStartAttemptCommand;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseApplicationRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseBindingEventRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseBindingRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseDocumentTypeRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseProfileRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseSubmissionRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVersionRow;
import org.namewta.profile.enterprise.dao.EnterpriseApplicationDao;
import org.namewta.profile.enterprise.port.provider.EnterpriseVerificationProviderRegistryPort;
import org.namewta.profile.enterprise.port.verification.EnterpriseVerificationService;
import org.namewta.system.api.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.core.validation.ValidationFormat;
import org.namewta.common.core.validation.ValidationUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
/**
 * 创建企业申请业务服务。
 */
@Service
public class EnterpriseApplicationService implements EnterpriseApplicationPublicationPort {
    private static final String DEFAULT_PROVIDER_KEY = "profile.enterprise.provider.default";
    private static final String FLOW_CODE_KEY = "profile.enterprise.flowCode";
    private static final Set<String> EDITABLE_STATUSES = Set.of("DRAFT", "BACK", "CANCEL");
    private static final Set<String> EVENT_STATUSES = Set.of("BACK", "CANCEL", "INVALID", "TERMINATION");
    private final EnterpriseApplicationDao dao;
    private final ProfileMaterialPort materials;
    private final EnterpriseVerificationProviderRegistryPort providers;
    private final EnterpriseVerificationService attempts;
    private final EnterpriseWorkflowGateway workflow;
    private final ConfigService configService;
    private final Clock clock;
    /** 创建企业申请业务服务。 */
    @Autowired
    public EnterpriseApplicationService(EnterpriseApplicationDao dao,
                                    ProfileMaterialPort materials,
                                    EnterpriseVerificationProviderRegistryPort providers,
                                    EnterpriseVerificationService attempts,
                                    EnterpriseWorkflowGateway workflow, ConfigService configService) {
        this(dao, materials, providers, attempts, workflow, configService, Clock.systemUTC());
    }
    /** 创建可注入时钟的企业申请业务服务，测试场景据此固定时间。 */
    public EnterpriseApplicationService(EnterpriseApplicationDao dao,
                             ProfileMaterialPort materials,
                             EnterpriseVerificationProviderRegistryPort providers,
                             EnterpriseVerificationService attempts,
                             EnterpriseWorkflowGateway workflow, ConfigService configService, Clock clock) {
        this.dao = dao;
        this.materials = materials;
        this.providers = providers;
        this.attempts = attempts;
        this.workflow = workflow;
        this.configService = configService;
        this.clock = clock;
    }
    /**
     * 查询当前用户的进行中申请
     */
    public EnterpriseApplicationVo current(long userId) {
        requireUserId(userId);
        return findOpenByUserId(userId).map(EnterpriseApplicationVo::from).orElse(null);
    }
    /**
     * 校验申请身份并返回探测结果
     */
    public EnterpriseApplicationProbeVo probe(EnterpriseApplicationProbeBo command) {
        if (command == null) {
            throw failure("ENTERPRISE_PROBE_REQUIRED");
        }
        String identityKey = normalizeCredit(command.unifiedCreditCode());
        if (identityKey == null || !ValidationUtils.isValid(identityKey, ValidationFormat.UNIFIED_SOCIAL_CREDIT_CODE)) {
            throw failure("ENTERPRISE_CREDIT_CODE_INVALID");
        }
        return new EnterpriseApplicationProbeVo(probeStatus(identityKey));
    }
    /**
     * 保存业务申请数据
     */

    public EnterpriseApplicationVo save(long userId, EnterpriseApplicationSaveBo command) {
        requireUserId(userId);
        EnterpriseIdentityFields fields = EnterpriseIdentityFields.normalize(command);
        validateDraft(fields);
        Optional<EnterpriseApplication> current = findOpenByUserId(userId);
        if (current.isPresent() && !current.get().editable()) {
            throw failure("ENTERPRISE_APPLICATION_READ_ONLY");
        }
        String providerCode = current.map(EnterpriseApplication::providerCode).orElseGet(this::defaultProvider);
        requireProviderEnabled(providerCode);
        Long accountProfileId = findEffectiveProfileIdByUser(userId);
        accountProfileId = positiveId(accountProfileId);
        Long identityProfileId = fields.identityKey() == null
            ? null : findActiveProfileIdByIdentity(fields.identityKey());
        identityProfileId = positiveId(identityProfileId);
        Long targetProfileId = accountProfileId == null ? identityProfileId : accountProfileId;
        EnterpriseApplication saved = saveDraft(userId, providerCode,
            new EnterpriseDraftUpdate(fields, targetProfileId, command.expectedVersion()));
        return EnterpriseApplicationVo.from(saved);
    }
    /**
     * 提交申请并启动后续流程
     */

    public EnterpriseApplicationVo submit(long userId, int expectedVersion) {
        requireUserId(userId);
        EnterpriseApplication application = lockOpenByUserId(userId);
        if (application.applicantUserId() != userId || !EDITABLE_STATUSES.contains(application.status())) {
            throw failure("ENTERPRISE_APPLICATION_NOT_EDITABLE");
        }
        if (application.version() != expectedVersion) {
            throw failure("ENTERPRISE_APPLICATION_VERSION_CONFLICT");
        }
        validateComplete(application.fields());
        requireProviderEnabled(application.providerCode());
        requireSubmissionAllowed(userId, application.targetProfileId(),
            application.fields().identityKey());
        MaterialOwnerKey working = owner(MaterialOwnerType.WORKING, application.enterpriseApplicationId());
        Set<String> qualifiers = new HashSet<>();
        qualifiers.add("ALWAYS");
        if (!application.fields().handlerIsLegalRepresentative()) {
            qualifiers.add("HANDLER_NOT_LEGAL_REPRESENTATIVE");
        }
        materials.validateRequired(working, "*", Set.copyOf(qualifiers));
        int snapshotVersion = application.submissionSeq() + 1;
        Instant submittedTime = clock.instant();
        EnterpriseSubmission submission = insertSubmission(
            application, application.fields(), snapshotVersion, submittedTime);
        materials.snapshotImmutable(working, owner(MaterialOwnerType.SUBMISSION,
            submission.enterpriseSubmissionId()));
        EnterpriseApplication waiting = markWaiting(application.enterpriseApplicationId(), snapshotVersion,
            expectedVersion, submittedTime);
        startVerificationAttempt(new EnterpriseVerificationStartAttemptCommand(application.enterpriseApplicationId(),
            submission.enterpriseSubmissionId(), fingerprint(application.fields(), snapshotVersion)));
        workflow.start(application.enterpriseApplicationId(), submission.enterpriseSubmissionId(), snapshotVersion);
        return EnterpriseApplicationVo.from(waiting);
    }
    /**
     * 处理工作流状态事件
     */

    public void handleProcess(EnterpriseApplicationProcessCommand command) {
        if (command == null || !expectedFlowCode().equals(command.flowCode())) {
            return;
        }
        Long applicationId = positiveLong(command.businessId());
        if (applicationId == null) {
            return;
        }
        Integer snapshotVersion = command.snapshotVersion();
        if (snapshotVersion == null) {
            snapshotVersion = workflow.persistedSnapshotVersionByInstanceId(command.processInstanceId());
        }
        if (snapshotVersion == null) {
            return;
        }
        EnterpriseApplication application = lockById(applicationId);
        if (application.submissionSeq() != snapshotVersion || !"WAITING".equals(application.status())) {
            return;
        }
        String status = normalizeStatus(command.status());
        if ("FINISH".equals(status)) {
            if ("REJECT".equals(normalizeDecision(command.decision()))) {
                updateWorkflowStatus(applicationId, snapshotVersion, "INVALID",
                    application.version(), occurredTime(command.occurredTime()));
                return;
            }
            EnterpriseSubmission submission = requireSubmission(applicationId, snapshotVersion);
            EnterprisePublication publication = publishApproved(applicationId, snapshotVersion,
                occurredTime(command.occurredTime()));
            materials.snapshotImmutable(owner(MaterialOwnerType.SUBMISSION, submission.enterpriseSubmissionId()),
                owner(MaterialOwnerType.VERSION, publication.enterpriseVersionId()));
        } else if (EVENT_STATUSES.contains(status)) {
            updateWorkflowStatus(applicationId, snapshotVersion, status,
                application.version(), occurredTime(command.occurredTime()));
        }
    }
    /** 取得事件时间并提供当前时间兜底。 */
    private Instant occurredTime(Instant value) {
        return value == null ? clock.instant() : value;
    }
    /**
     * 按用户查询未完成申请
     */
    public Optional<EnterpriseApplication> findOpenByUserId(long userId) {
        return Optional.ofNullable(dao.selectOpenByUserId(userId)).map(this::application);
    }
    /**
     * 按用户锁定未完成申请
     */
    public EnterpriseApplication lockOpenByUserId(long userId) {
        return requireApplication(dao.lockOpenByUserId(userId));
    }
    /**
     * 按编号锁定申请记录
     */
    public EnterpriseApplication lockById(long applicationId) {
        return requireApplication(dao.lockApplicationById(applicationId));
    }
    /**
     * 查询证件类型配置
     */
    @Override
    public Optional<EnterpriseDocumentTypeRule> findDocumentType(String documentTypeCode) {
        EnterpriseDocumentTypeRow row = dao.selectDocumentType(documentTypeCode);
        return Optional.ofNullable(row).map(value -> new EnterpriseDocumentTypeRule(value.getDocumentTypeCode(),
            value.getNumberPattern(), "Y".equals(value.getValidityRequired())));
    }
    /**
     * 按身份查询生效档案编号
     */
    public Long findActiveProfileIdByIdentity(String identityKey) {
        return identityKey == null ? null : dao.selectActiveProfileIdByIdentity(identityKey);
    }
    /**
     * 按用户查询当前有效档案编号
     */
    public Long findEffectiveProfileIdByUser(long userId) {
        return dao.selectEffectiveProfileIdByUser(userId);
    }
    /**
     * 查询申请探测状态
     */
    public String probeStatus(String identityKey) {
        return dao.selectProbeStatus(identityKey);
    }
    /**
     * 保存申请草稿
     */
    public EnterpriseApplication saveDraft(long userId, String providerCode, EnterpriseDraftUpdate update) {
        EnterpriseApplicationRow current = dao.lockOpenByUserId(userId);
        try {
            if (current == null) {
                if (update.expectedVersion() != 0) {
                    throw failure("ENTERPRISE_APPLICATION_VERSION_CONFLICT");
                }
                EnterpriseApplicationRow inserted = draftRow(IdGeneratorUtil.nextLongId(), userId, providerCode, update);
                requireChanged(dao.insertApplication(inserted), "ENTERPRISE_APPLICATION_CREATE_CONFLICT");
            } else {
                if (!editable(current.getStatus()) || intValue(current.getVersion()) != update.expectedVersion()) {
                    throw failure("ENTERPRISE_APPLICATION_VERSION_CONFLICT");
                }
                EnterpriseApplicationRow changed = draftRow(current.getEnterpriseApplicationId(), userId,
                    current.getProviderCode(), update);
                changed.setVersion(update.expectedVersion());
                requireChanged(dao.updateDraft(changed), "ENTERPRISE_APPLICATION_VERSION_CONFLICT");
            }
        } catch (DuplicateKeyException exception) {
            throw failure("ENTERPRISE_APPLICATION_IDENTITY_CONFLICT", exception);
        }
        return requireApplication(dao.selectOpenByUserId(userId));
    }
    /**
     * 校验申请允许提交
     */
    public void requireSubmissionAllowed(long userId, Long targetProfileId, String identityKey) {
        if (identityKey == null || identityKey.isBlank()) {
            throw failure("ENTERPRISE_IDENTITY_REQUIRED");
        }
        EnterpriseBindingRow userBinding = dao.lockEffectiveBindingByUser(userId);
        if (userBinding != null) {
            throw failure("ENTERPRISE_ACCOUNT_ALREADY_RESPONSIBLE");
        }
        EnterpriseProfileRow profile = dao.lockActiveProfileByIdentity(identityKey);
        EnterpriseBindingRow profileBinding = profile == null
            ? null : dao.lockEffectiveBindingByProfile(profile.getEnterpriseProfileId());
        if (profileBinding != null) {
            throw failure("ENTERPRISE_RESPONSIBLE_ALREADY_BOUND");
        }
        if (profile != null && (targetProfileId == null
            || !profile.getEnterpriseProfileId().equals(targetProfileId))) {
            throw failure("ENTERPRISE_IDENTITY_CONFLICT");
        }
    }
    /**
     * 新增申请提交记录
     */
    public EnterpriseSubmission insertSubmission(EnterpriseApplication application, EnterpriseIdentityFields fields,
                                          int submissionSeq, Instant submittedTime) {
        EnterpriseSubmissionRow row = submissionRow(application, fields, submissionSeq, submittedTime);
        try {
            requireChanged(dao.insertSubmission(row), "ENTERPRISE_SUBMISSION_CONFLICT");
        } catch (DuplicateKeyException exception) {
            throw failure("ENTERPRISE_SUBMISSION_CONFLICT", exception);
        }
        return submission(row);
    }
    /**
     * 校验并获取提交记录
     */
    @Override
    public EnterpriseSubmission requireSubmission(long applicationId, int submissionSeq) {
        EnterpriseSubmissionRow row = dao.selectSubmission(applicationId, submissionSeq);
        if (row == null) {
            throw failure("ENTERPRISE_SUBMISSION_NOT_FOUND");
        }
        return submission(row);
    }
    /**
     * 标记申请为等待处理
     */
    public EnterpriseApplication markWaiting(long applicationId, int submissionSeq, int expectedVersion,
                                      Instant submittedTime) {
        requireChanged(dao.markWaiting(applicationId, submissionSeq, expectedVersion, submittedTime),
            "ENTERPRISE_APPLICATION_VERSION_CONFLICT");
        return requireApplication(dao.lockApplicationById(applicationId));
    }
    /**
     * 发布已审核通过的档案
     */
    @Override

    public EnterprisePublication publishApproved(long applicationId, int snapshotVersion, Instant finishedTime) {
        EnterpriseApplicationRow application = dao.lockApplicationById(applicationId);
        if (application == null || !"WAITING".equals(application.getStatus())
            || intValue(application.getSubmissionSeq()) != snapshotVersion) {
            throw failure("ENTERPRISE_APPLICATION_DECISION_CONFLICT");
        }
        EnterpriseSubmissionRow submission = dao.selectSubmission(applicationId, snapshotVersion);
        if (submission == null) {
            throw failure("ENTERPRISE_SUBMISSION_NOT_FOUND");
        }
        try {
            PublicationTarget target = publicationTarget(application.getApplicantUserId(), submission);
            EnterpriseVersionRow currentVersion = dao.selectCurrentVersionForUpdate(
                target.profile().getEnterpriseProfileId());
            int nextVersion = currentVersion == null ? 1 : intValue(currentVersion.getVersionNo()) + 1;
            if (currentVersion != null) {
                requireChanged(dao.supersedeVersion(currentVersion.getEnterpriseVersionId()),
                    "ENTERPRISE_PROFILE_VERSION_CONFLICT");
            }
            EnterpriseVersionRow version = versionRow(target.profile().getEnterpriseProfileId(), nextVersion,
                submission, finishedTime);
            requireChanged(dao.insertVersion(version), "ENTERPRISE_PROFILE_VERSION_CONFLICT");
            EnterpriseProfileRow updatedProfile = profileRow(target.profile().getEnterpriseProfileId(),
                target.profile().getPreviousProfileId(), version.getEnterpriseVersionId(), submission,
                target.profile().getVersion());
            requireChanged(dao.updateProfile(updatedProfile), "ENTERPRISE_PROFILE_VERSION_CONFLICT");
            EnterpriseBindingRow binding = bindingRow(target.profile().getEnterpriseProfileId(),
                application.getApplicantUserId(), submission.getEnterpriseSubmissionId(), finishedTime);
            requireChanged(dao.insertBinding(binding), "ENTERPRISE_BINDING_CONFLICT");
            requireChanged(dao.insertBindingEvent(bindingEvent(binding, submission.getEnterpriseSubmissionId(),
                finishedTime)), "ENTERPRISE_BINDING_EVENT_CONFLICT");
            requireChanged(dao.finishApplication(applicationId, snapshotVersion,
                intValue(application.getDecisionVersion()), intValue(application.getVersion()), finishedTime),
                "ENTERPRISE_APPLICATION_DECISION_CONFLICT");
            return new EnterprisePublication(target.profile().getEnterpriseProfileId(),
                version.getEnterpriseVersionId(), binding.getEnterpriseBindingId(), target.successor());
        } catch (DuplicateKeyException exception) {
            throw failure("ENTERPRISE_PUBLICATION_CONFLICT", exception);
        }
    }
    /**
     * 更新workflowstatus。
     */
    public void updateWorkflowStatus(long applicationId, int snapshotVersion, String status,
                              int expectedVersion, Instant occurredTime) {
        requireChanged(dao.updateWorkflowStatus(applicationId, snapshotVersion, status,
            expectedVersion, occurredTime), "ENTERPRISE_APPLICATION_DECISION_CONFLICT");
    }
    /**
     * 解析发布目标
     */
    private PublicationTarget publicationTarget(long userId, EnterpriseSubmissionRow submission) {
        EnterpriseBindingRow userBinding = dao.lockEffectiveBindingByUser(userId);
        if (userBinding != null) {
            throw failure("ENTERPRISE_ACCOUNT_ALREADY_RESPONSIBLE");
        }
        EnterpriseProfileRow profile = dao.lockActiveProfileByIdentity(submission.getIdentityKey());
        EnterpriseBindingRow profileBinding = profile == null
            ? null : dao.lockEffectiveBindingByProfile(profile.getEnterpriseProfileId());
        if (profileBinding != null) {
            throw failure("ENTERPRISE_RESPONSIBLE_ALREADY_BOUND");
        }
        if (profile != null) {
            return new PublicationTarget(profile, false);
        }
        EnterpriseProfileRow revoked = dao.lockLatestRevokedProfileByIdentity(submission.getIdentityKey());
        long profileId = IdGeneratorUtil.nextLongId();
        EnterpriseProfileRow created = profileRow(profileId,
            revoked == null ? null : revoked.getEnterpriseProfileId(), null, submission, 0);
        requireChanged(dao.insertProfile(created), "ENTERPRISE_IDENTITY_CONFLICT");
        return new PublicationTarget(created, revoked != null);
    }
    /**
     * 转换申请草稿读模型
     */
    private EnterpriseApplicationRow draftRow(long applicationId, long userId, String providerCode,
                                              EnterpriseDraftUpdate update) {
        EnterpriseApplicationRow row = new EnterpriseApplicationRow();
        row.setEnterpriseApplicationId(applicationId);
        row.setApplicantUserId(userId);
        row.setTargetProfileId(update.targetProfileId());
        row.setProviderCode(providerCode);
        copy(row, update.fields());
        return row;
    }
    /**
     * 转换申请提交读模型
     */
    private EnterpriseSubmissionRow submissionRow(EnterpriseApplication application, EnterpriseIdentityFields fields,
                                                  int submissionSeq, Instant submittedTime) {
        EnterpriseSubmissionRow row = new EnterpriseSubmissionRow();
        row.setEnterpriseSubmissionId(IdGeneratorUtil.nextLongId());
        row.setEnterpriseApplicationId(application.enterpriseApplicationId());
        row.setSubmissionSeq(submissionSeq);
        row.setApplicantUserId(application.applicantUserId());
        row.setProviderCode(application.providerCode());
        row.setTargetProfileId(application.targetProfileId());
        row.setFieldSnapshotJson(JsonUtils.toJsonString(fields));
        row.setSubmittedTime(submittedTime);
        copy(row, fields);
        return row;
    }
    /**
     * 转换档案读模型
     */
    private EnterpriseProfileRow profileRow(long profileId, Long previousProfileId, Long currentVersionId,
                                            EnterpriseSubmissionRow submission, Integer version) {
        EnterpriseProfileRow row = new EnterpriseProfileRow();
        row.setEnterpriseProfileId(profileId);
        row.setPreviousProfileId(previousProfileId);
        row.setCurrentVersionId(currentVersionId);
        row.setVersion(version);
        row.setStatus("ACTIVE");
        copy(row, submission);
        return row;
    }
    /**
     * 转换档案版本读模型
     */
    private EnterpriseVersionRow versionRow(long profileId, int versionNo, EnterpriseSubmissionRow submission,
                                            Instant publishedTime) {
        EnterpriseVersionRow row = new EnterpriseVersionRow();
        row.setEnterpriseVersionId(IdGeneratorUtil.nextLongId());
        row.setEnterpriseProfileId(profileId);
        row.setVersionNo(versionNo);
        row.setSourceType("USER_SUBMISSION");
        row.setSourceId(submission.getEnterpriseSubmissionId());
        row.setStatus("CURRENT");
        row.setPublishedTime(publishedTime);
        copy(row, submission);
        return row;
    }
    /**
     * 转换绑定读模型
     */
    private EnterpriseBindingRow bindingRow(long profileId, long userId, long sourceId, Instant boundTime) {
        EnterpriseBindingRow row = new EnterpriseBindingRow();
        row.setEnterpriseBindingId(IdGeneratorUtil.nextLongId());
        row.setEnterpriseProfileId(profileId);
        row.setUserId(userId);
        row.setStatus("ACTIVE");
        row.setBindingVersion(1);
        row.setSourceType("USER_SUBMISSION");
        row.setSourceId(sourceId);
        row.setBoundTime(boundTime);
        return row;
    }
    /**
     * 查询绑定事件记录
     */
    private EnterpriseBindingEventRow bindingEvent(EnterpriseBindingRow binding, long sourceId, Instant occurredTime) {
        EnterpriseBindingEventRow row = new EnterpriseBindingEventRow();
        row.setEnterpriseBindingEventId(IdGeneratorUtil.nextLongId());
        row.setEnterpriseBindingId(binding.getEnterpriseBindingId());
        row.setEnterpriseProfileId(binding.getEnterpriseProfileId());
        row.setUserId(binding.getUserId());
        row.setEventType("ACTIVE");
        row.setBindingVersion(binding.getBindingVersion());
        row.setSourceType("USER_SUBMISSION");
        row.setSourceId(sourceId);
        row.setReason("ENTERPRISE_VERIFICATION_APPROVED");
        row.setOccurredTime(occurredTime);
        return row;
    }
    /**
     * 处理application。
     */
    private EnterpriseApplication application(EnterpriseApplicationRow row) {
        return new EnterpriseApplication(row.getEnterpriseApplicationId(), row.getApplicantUserId(),
            row.getTargetProfileId(), row.getStatus(), fields(row), row.getProviderCode(),
            intValue(row.getSubmissionSeq()), intValue(row.getDecisionVersion()), intValue(row.getVersion()),
            row.getSubmittedTime(), row.getFinishedTime());
    }
    /**
     * 组装申请提交数据
     */
    private EnterpriseSubmission submission(EnterpriseSubmissionRow row) {
        return new EnterpriseSubmission(row.getEnterpriseSubmissionId(), row.getEnterpriseApplicationId(),
            intValue(row.getSubmissionSeq()), row.getApplicantUserId(), fields(row), row.getProviderCode(),
            row.getSubmittedTime());
    }
    /**
     * 提取并规范化申请身份字段
     */
    private EnterpriseIdentityFields fields(EnterpriseApplicationRow row) {
        return new EnterpriseIdentityFields(row.getEnterpriseName(), row.getUnifiedCreditCode(), row.getIdentityKey(),
            row.getEnterpriseType(), row.getLegalRepresentativeName(), row.getLegalDocumentTypeCode(),
            row.getLegalDocumentNumber(), "Y".equals(row.getHandlerIsLegalRepresentative()),
            row.getEstablishedDate(), row.getBusinessTermFrom(), row.getBusinessTermUntil(),
            row.getRegisteredAddress(), row.getBusinessScope(), row.getContactName(), row.getContactPhone(),
            row.getEmail(), row.getRegisteredCapital(), row.getIndustryCode(), row.getWebsite());
    }
    /**
     * 提取并规范化申请身份字段
     */
    private EnterpriseIdentityFields fields(EnterpriseSubmissionRow row) {
        return new EnterpriseIdentityFields(row.getEnterpriseName(), row.getUnifiedCreditCode(), row.getIdentityKey(),
            row.getEnterpriseType(), row.getLegalRepresentativeName(), row.getLegalDocumentTypeCode(),
            row.getLegalDocumentNumber(), "Y".equals(row.getHandlerIsLegalRepresentative()),
            row.getEstablishedDate(), row.getBusinessTermFrom(), row.getBusinessTermUntil(),
            row.getRegisteredAddress(), row.getBusinessScope(), row.getContactName(), row.getContactPhone(),
            row.getEmail(), row.getRegisteredCapital(), row.getIndustryCode(), row.getWebsite());
    }
    /**
     * 复制领域数据并替换指定字段
     */
    private void copy(EnterpriseApplicationRow target, EnterpriseIdentityFields fields) {
        target.setEnterpriseName(fields.enterpriseName());
        target.setUnifiedCreditCode(fields.unifiedCreditCode());
        target.setIdentityKey(fields.identityKey());
        target.setEnterpriseType(fields.enterpriseType());
        target.setLegalRepresentativeName(fields.legalRepresentativeName());
        target.setLegalDocumentTypeCode(fields.legalDocumentTypeCode());
        target.setLegalDocumentNumber(fields.legalDocumentNumber());
        target.setHandlerIsLegalRepresentative(fields.handlerIsLegalRepresentative() ? "Y" : "N");
        copyOptional(target, fields);
    }
    /**
     * 复制领域数据并替换指定字段
     */
    private void copy(EnterpriseSubmissionRow target, EnterpriseIdentityFields fields) {
        target.setEnterpriseName(fields.enterpriseName());
        target.setUnifiedCreditCode(fields.unifiedCreditCode());
        target.setIdentityKey(fields.identityKey());
        target.setEnterpriseType(fields.enterpriseType());
        target.setLegalRepresentativeName(fields.legalRepresentativeName());
        target.setLegalDocumentTypeCode(fields.legalDocumentTypeCode());
        target.setLegalDocumentNumber(fields.legalDocumentNumber());
        target.setHandlerIsLegalRepresentative(fields.handlerIsLegalRepresentative() ? "Y" : "N");
        copyOptional(target, fields);
    }
    /**
     * 复制领域数据并替换指定字段
     */
    private void copy(EnterpriseProfileRow target, EnterpriseSubmissionRow source) {
        target.setEnterpriseName(source.getEnterpriseName());
        target.setUnifiedCreditCode(source.getUnifiedCreditCode());
        target.setEnterpriseType(source.getEnterpriseType());
        target.setLegalRepresentativeName(source.getLegalRepresentativeName());
        target.setLegalDocumentTypeCode(source.getLegalDocumentTypeCode());
        target.setLegalDocumentNumber(source.getLegalDocumentNumber());
        copyOptional(target, source);
    }
    /**
     * 复制领域数据并替换指定字段
     */
    private void copy(EnterpriseVersionRow target, EnterpriseSubmissionRow source) {
        target.setEnterpriseName(source.getEnterpriseName());
        target.setUnifiedCreditCode(source.getUnifiedCreditCode());
        target.setEnterpriseType(source.getEnterpriseType());
        target.setLegalRepresentativeName(source.getLegalRepresentativeName());
        target.setLegalDocumentTypeCode(source.getLegalDocumentTypeCode());
        target.setLegalDocumentNumber(source.getLegalDocumentNumber());
        copyOptional(target, source);
    }
    /**
     * 复制可选字段值
     */
    private void copyOptional(EnterpriseApplicationRow target, EnterpriseIdentityFields fields) {
        target.setEstablishedDate(fields.establishedDate());
        target.setBusinessTermFrom(fields.businessTermFrom());
        target.setBusinessTermUntil(fields.businessTermUntil());
        target.setRegisteredAddress(fields.registeredAddress());
        target.setBusinessScope(fields.businessScope());
        target.setContactName(fields.contactName());
        target.setContactPhone(fields.contactPhone());
        target.setEmail(fields.email());
        target.setRegisteredCapital(fields.registeredCapital());
        target.setIndustryCode(fields.industryCode());
        target.setWebsite(fields.website());
    }
    /**
     * 复制可选字段值
     */
    private void copyOptional(EnterpriseSubmissionRow target, EnterpriseIdentityFields fields) {
        target.setEstablishedDate(fields.establishedDate());
        target.setBusinessTermFrom(fields.businessTermFrom());
        target.setBusinessTermUntil(fields.businessTermUntil());
        target.setRegisteredAddress(fields.registeredAddress());
        target.setBusinessScope(fields.businessScope());
        target.setContactName(fields.contactName());
        target.setContactPhone(fields.contactPhone());
        target.setEmail(fields.email());
        target.setRegisteredCapital(fields.registeredCapital());
        target.setIndustryCode(fields.industryCode());
        target.setWebsite(fields.website());
    }
    /**
     * 复制可选字段值
     */
    private void copyOptional(EnterpriseProfileRow target, EnterpriseSubmissionRow source) {
        target.setEstablishedDate(source.getEstablishedDate());
        target.setBusinessTermFrom(source.getBusinessTermFrom());
        target.setBusinessTermUntil(source.getBusinessTermUntil());
        target.setRegisteredAddress(source.getRegisteredAddress());
        target.setBusinessScope(source.getBusinessScope());
        target.setContactName(source.getContactName());
        target.setContactPhone(source.getContactPhone());
        target.setEmail(source.getEmail());
        target.setRegisteredCapital(source.getRegisteredCapital());
        target.setIndustryCode(source.getIndustryCode());
        target.setWebsite(source.getWebsite());
    }
    /**
     * 复制可选字段值
     */
    private void copyOptional(EnterpriseVersionRow target, EnterpriseSubmissionRow source) {
        target.setEstablishedDate(source.getEstablishedDate());
        target.setBusinessTermFrom(source.getBusinessTermFrom());
        target.setBusinessTermUntil(source.getBusinessTermUntil());
        target.setRegisteredAddress(source.getRegisteredAddress());
        target.setBusinessScope(source.getBusinessScope());
        target.setContactName(source.getContactName());
        target.setContactPhone(source.getContactPhone());
        target.setEmail(source.getEmail());
        target.setRegisteredCapital(source.getRegisteredCapital());
        target.setIndustryCode(source.getIndustryCode());
        target.setWebsite(source.getWebsite());
    }
    /**
     * 校验并获取申请记录
     */
    private EnterpriseApplication requireApplication(EnterpriseApplicationRow row) {
        if (row == null) {
            throw failure("ENTERPRISE_APPLICATION_NOT_FOUND");
        }
        return application(row);
    }
    /**
     * 校验申请确实发生变更
     */
    private void requireChanged(int changed, String category) {
        if (changed != 1) {
            throw failure(category);
        }
    }
    /**
     * 判断申请是否允许编辑
     */
    private boolean editable(String status) {
        return "DRAFT".equals(status) || "BACK".equals(status) || "CANCEL".equals(status);
    }
    /**
     * 解析整数值
     */
    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }
    /**
     * 构造业务失败异常
     */
    private EnterpriseApplicationException failure(String category, Throwable cause) {
        return new EnterpriseApplicationException(category, cause);
    }
    /**
     * 承载PublicationTarget业务规则的领域服务。
     */
    private record PublicationTarget(EnterpriseProfileRow profile, boolean successor) {
    }
    /**
     * 规范化审核决定
     */
    private String normalizeDecision(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }
    /**
     * 校验申请草稿字段
     */
    private void validateDraft(EnterpriseIdentityFields fields) {
        if (length(fields.enterpriseName()) > 255 || length(fields.unifiedCreditCode()) > 64
            || length(fields.enterpriseType()) > 64 || length(fields.legalRepresentativeName()) > 100
            || length(fields.legalDocumentNumber()) > 128 || length(fields.registeredAddress()) > 500
            || length(fields.contactName()) > 100 || length(fields.contactPhone()) > 64
            || length(fields.email()) > 255 || length(fields.industryCode()) > 64 || length(fields.website()) > 500) {
            throw failure("ENTERPRISE_FIELD_TOO_LONG");
        }
        if (fields.unifiedCreditCode() != null && !ValidationUtils.isValid(fields.unifiedCreditCode(), ValidationFormat.UNIFIED_SOCIAL_CREDIT_CODE)) {
            throw failure("ENTERPRISE_CREDIT_CODE_INVALID");
        }
        if (fields.legalDocumentTypeCode() != null) {
            EnterpriseDocumentTypeRule rule = documentRule(fields.legalDocumentTypeCode());
            if (fields.legalDocumentNumber() != null
                && !Pattern.matches(rule.numberPattern(), fields.legalDocumentNumber())) {
                throw failure("ENTERPRISE_LEGAL_DOCUMENT_NUMBER_INVALID");
            }
        }
        if (fields.email() != null && !ValidationUtils.isValid(fields.email(), ValidationFormat.EMAIL)) {
            throw failure("ENTERPRISE_EMAIL_INVALID");
        }
        if (fields.registeredCapital() != null && fields.registeredCapital().signum() < 0) {
            throw failure("ENTERPRISE_REGISTERED_CAPITAL_INVALID");
        }
        validateDates(fields, false);
    }
    /**
     * 校验申请信息完整性
     */
    private void validateComplete(EnterpriseIdentityFields fields) {
        validateDraft(fields);
        if (fields.enterpriseName() == null || fields.unifiedCreditCode() == null || fields.enterpriseType() == null
            || fields.legalRepresentativeName() == null || fields.legalDocumentTypeCode() == null
            || fields.legalDocumentNumber() == null || fields.establishedDate() == null
            || fields.registeredAddress() == null || fields.businessScope() == null) {
            throw failure("ENTERPRISE_FIELDS_INCOMPLETE");
        }
        EnterpriseDocumentTypeRule rule = documentRule(fields.legalDocumentTypeCode());
        if (!Pattern.matches(rule.numberPattern(), fields.legalDocumentNumber())) {
            throw failure("ENTERPRISE_LEGAL_DOCUMENT_NUMBER_INVALID");
        }
        validateDates(fields, true);
    }
    /**
     * 校验日期范围
     */
    private void validateDates(EnterpriseIdentityFields fields, boolean complete) {
        LocalDate today = LocalDate.now(clock);
        if (fields.establishedDate() != null && fields.establishedDate().isAfter(today)) {
            throw failure("ENTERPRISE_ESTABLISHED_DATE_INVALID");
        }
        if (fields.businessTermFrom() != null && fields.businessTermUntil() != null
            && fields.businessTermFrom().isAfter(fields.businessTermUntil())) {
            throw failure("ENTERPRISE_BUSINESS_TERM_INVALID");
        }
        if (complete && fields.businessTermFrom() != null && fields.businessTermUntil() != null
            && (fields.businessTermFrom().isAfter(today) || fields.businessTermUntil().isBefore(today))) {
            throw failure("ENTERPRISE_BUSINESS_TERM_EXPIRED");
        }
    }
    /**
     * 解析证件类型校验规则
     */
    private EnterpriseDocumentTypeRule documentRule(String documentTypeCode) {
        return findDocumentType(documentTypeCode)
            .orElseThrow(() -> failure("ENTERPRISE_DOCUMENT_TYPE_UNAVAILABLE"));
    }
    /**
     * 解析默认认证提供方
     */
    private String defaultProvider() {
        String providerCode = configService.getConfigValue(DEFAULT_PROVIDER_KEY);
        if (providerCode == null || providerCode.isBlank()) {
            throw failure("ENTERPRISE_PROVIDER_NOT_CONFIGURED");
        }
        return providerCode.strip();
    }
    /**
     * 校验认证提供方已启用
     */
    private void requireProviderEnabled(String providerCode) {
        try {
            providers.requireEnabled(providerCode);
        } catch (EnterpriseVerificationException exception) {
            throw new EnterpriseApplicationException("ENTERPRISE_PROVIDER_UNAVAILABLE", exception);
        }
    }
    /**
     * 启动认证流程并记录尝试
     */
    private void startVerificationAttempt(EnterpriseVerificationStartAttemptCommand command) {
        try {
            attempts.startAttempt(command);
        } catch (EnterpriseVerificationException exception) {
            throw new EnterpriseApplicationException("ENTERPRISE_PROVIDER_UNAVAILABLE", exception);
        }
    }
    /**
     * 处理expectedflowcode。
     */
    private String expectedFlowCode() {
        String flowCode = configService.getConfigValue(FLOW_CODE_KEY);
        return flowCode == null ? "" : flowCode.strip();
    }
    /**
     * 计算身份字段指纹
     */
    private String fingerprint(EnterpriseIdentityFields fields, int snapshotVersion) {
        String canonical = fields.identityKey() + "\n" + snapshotVersion;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
    /**
     * 规范化统一社会信用代码
     */
    private String normalizeCredit(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip().toUpperCase(Locale.ROOT);
    }
    /**
     * 解析材料所有者
     */
    private MaterialOwnerKey owner(MaterialOwnerType type, long ownerId) {
        return new MaterialOwnerKey(ProfileType.ENTERPRISE, type, ownerId);
    }
    /**
     * 生成档案版本快照
     */
    /**
     * 校验正数长整型编号
     */
    private Long positiveLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    /**
     * 校验正数档案编号
     */
    private Long positiveId(Long value) {
        return value != null && value > 0 ? value : null;
    }
    /**
     * 规范化业务状态
     */
    private String normalizeStatus(String status) {
        return status == null ? "" : status.strip().toUpperCase(java.util.Locale.ROOT);
    }
    /**
     * 校验文本长度
     */
    private int length(String value) {
        return value == null ? 0 : value.length();
    }
    /**
     * 校验用户编号有效
     */
    private void requireUserId(long userId) {
        if (userId <= 0) {
            throw failure("ENTERPRISE_USER_INVALID");
        }
    }
    /**
     * 构造业务失败异常
     */
    private EnterpriseApplicationException failure(String category) {
        return new EnterpriseApplicationException(category);
    }
}
