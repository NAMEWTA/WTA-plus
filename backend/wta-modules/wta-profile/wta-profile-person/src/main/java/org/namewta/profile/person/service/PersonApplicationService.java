package org.namewta.profile.person.service;
import org.namewta.profile.person.domain.application.PersonDocumentTypeRule;
import org.namewta.profile.person.domain.application.PersonApplication;
import org.namewta.profile.person.domain.vo.PersonApplicationVo;
import org.namewta.profile.person.domain.bo.PersonApplicationSaveBo;
import org.namewta.profile.person.domain.application.PersonDraftUpdate;
import org.namewta.profile.person.domain.application.PersonIdentityFields;
import org.namewta.profile.person.domain.application.PersonApplicationProcessCommand;
import org.namewta.profile.person.domain.application.PersonPublication;
import org.namewta.profile.person.domain.application.PersonSubmission;
import org.namewta.profile.person.domain.application.PersonActiveProjection;
import org.namewta.profile.person.domain.model.read.PersonApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonBindingEventRow;
import org.namewta.profile.person.domain.model.read.PersonBindingRow;
import org.namewta.profile.person.domain.model.read.PersonDocumentTypeRow;
import org.namewta.profile.person.domain.model.read.PersonProfileRow;
import org.namewta.profile.person.domain.model.read.PersonSubmissionRow;
import org.namewta.profile.person.domain.model.read.PersonVersionRow;
import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.person.port.provider.PersonVerificationProviderRegistryPort;
import org.namewta.profile.person.port.verification.PersonVerificationService;
import org.namewta.profile.person.domain.exception.PersonApplicationException;
import org.namewta.profile.person.port.PersonApplicationPublicationPort;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.person.domain.exception.PersonVerificationException;
import org.namewta.profile.person.domain.verification.PersonVerificationStartAttemptCommand;
import org.namewta.system.api.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.namewta.common.json.utils.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
/**
 * 创建个人申请业务服务。
 */
@Service
public class PersonApplicationService implements PersonApplicationPublicationPort {
    private static final String DEFAULT_PROVIDER_KEY = "profile.person.provider.default";
    private static final String FLOW_CODE_KEY = "profile.person.flowCode";
    private static final Set<String> EDITABLE_STATUSES = Set.of("DRAFT", "BACK", "CANCEL");
    private static final Set<String> EVENT_STATUSES = Set.of("BACK", "CANCEL", "INVALID", "TERMINATION");
    private static final Set<String> GENDERS = Set.of("MALE", "FEMALE", "UNKNOWN");
    private final PersonApplicationDao dao;
    private final ProfileMaterialPort materials;
    private final PersonVerificationProviderRegistryPort providers;
    private final PersonVerificationService attempts;
    private final PersonWorkflowGateway workflow;
    private final ConfigService configService;
    private final Clock clock;
    /** 创建个人申请业务服务。 */
    @Autowired
    public PersonApplicationService(PersonApplicationDao dao,
                                    ProfileMaterialPort materials,
                                    PersonVerificationProviderRegistryPort providers,
                                    PersonVerificationService attempts,
                                    PersonWorkflowGateway workflow, ConfigService configService) {
        this(dao, materials, providers, attempts, workflow, configService, Clock.systemUTC());
    }
    /** 创建可注入时钟的个人申请业务服务，测试场景据此固定时间。 */
    public PersonApplicationService(PersonApplicationDao dao,
                             ProfileMaterialPort materials,
                             PersonVerificationProviderRegistryPort providers,
                             PersonVerificationService attempts,
                             PersonWorkflowGateway workflow, ConfigService configService, Clock clock) {
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
    public PersonApplicationVo current(long userId) {
        requireUserId(userId);
        return findOpenByUserId(userId).map(PersonApplicationVo::from).orElse(null);
    }
    /**
     * 保存业务申请数据
     */

    public PersonApplicationVo save(long userId, PersonApplicationSaveBo command) {
        requireUserId(userId);
        PersonIdentityFields fields = PersonIdentityFields.normalize(command);
        validateDraft(fields);
        Optional<PersonApplication> current = findOpenByUserId(userId);
        if (current.isPresent() && !current.get().editable()) {
            throw failure("PERSON_APPLICATION_READ_ONLY");
        }
        String providerCode = current.map(PersonApplication::providerCode).orElseGet(this::defaultProvider);
        requireProviderEnabled(providerCode);
        Long accountProfileId = findEffectiveProfileIdByUser(userId);
        Long identityProfileId = fields.identityKey() == null
            ? null : findActiveProfileIdByIdentity(fields.identityKey());
        Long targetProfileId = accountProfileId == null ? identityProfileId : accountProfileId;
        PersonApplication saved = saveDraft(userId, providerCode,
            new PersonDraftUpdate(fields, targetProfileId, command.expectedVersion()));
        return PersonApplicationVo.from(saved);
    }
    /**
     * 提交申请并启动后续流程
     */

    public PersonApplicationVo submit(long userId, int expectedVersion) {
        requireUserId(userId);
        PersonApplication application = lockOpenByUserId(userId);
        if (application.applicantUserId() != userId || !EDITABLE_STATUSES.contains(application.status())) {
            throw failure("PERSON_APPLICATION_NOT_EDITABLE");
        }
        if (application.version() != expectedVersion) {
            throw failure("PERSON_APPLICATION_VERSION_CONFLICT");
        }
        validateComplete(application.fields());
        requireProviderEnabled(application.providerCode());
        requireSubmissionAllowed(userId, application.targetProfileId(),
            application.fields().identityKey());
        MaterialOwnerKey working = owner(MaterialOwnerType.WORKING, application.personApplicationId());
        materials.validateRequired(working, application.fields().documentTypeCode(), Set.of("ALWAYS"));
        int snapshotVersion = application.submissionSeq() + 1;
        Instant submittedTime = clock.instant();
        PersonSubmission submission = insertSubmission(
            application, application.fields(), snapshotVersion, submittedTime);
        materials.snapshotImmutable(working, owner(MaterialOwnerType.SUBMISSION,
            submission.personSubmissionId()));
        PersonApplication waiting = markWaiting(application.personApplicationId(), snapshotVersion,
            expectedVersion, submittedTime);
        startVerificationAttempt(new PersonVerificationStartAttemptCommand(application.personApplicationId(),
            submission.personSubmissionId(), fingerprint(application.fields(), snapshotVersion)));
        workflow.start(application.personApplicationId(), submission.personSubmissionId(), snapshotVersion);
        return PersonApplicationVo.from(waiting);
    }
    /**
     * 处理工作流状态事件
     */

    public void handleProcess(PersonApplicationProcessCommand command) {
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
        PersonApplication application = lockById(applicationId);
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
            PersonSubmission submission = requireSubmission(applicationId, snapshotVersion);
            PersonPublication publication = publishApproved(applicationId, snapshotVersion,
                occurredTime(command.occurredTime()));
            materials.snapshotImmutable(owner(MaterialOwnerType.SUBMISSION, submission.personSubmissionId()),
                owner(MaterialOwnerType.VERSION, publication.personVersionId()));
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
    public Optional<PersonApplication> findOpenByUserId(long userId) {
        return Optional.ofNullable(dao.selectOpenByUserId(userId)).map(this::application);
    }
    /**
     * 按用户锁定未完成申请
     */
    public PersonApplication lockOpenByUserId(long userId) {
        return requireApplication(dao.lockOpenByUserId(userId));
    }
    /**
     * 按编号锁定申请记录
     */
    public PersonApplication lockById(long applicationId) {
        return requireApplication(dao.lockApplicationById(applicationId));
    }
    /**
     * 查询证件类型配置
     */
    @Override
    public Optional<PersonDocumentTypeRule> findDocumentType(String documentTypeCode) {
        PersonDocumentTypeRow row = dao.selectDocumentType(documentTypeCode);
        return Optional.ofNullable(row).map(value -> new PersonDocumentTypeRule(value.getDocumentTypeCode(),
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
     * 保存申请草稿
     */
    public PersonApplication saveDraft(long userId, String providerCode, PersonDraftUpdate update) {
        PersonApplicationRow current = dao.lockOpenByUserId(userId);
        try {
            if (current == null) {
                if (update.expectedVersion() != 0) {
                    throw failure("PERSON_APPLICATION_VERSION_CONFLICT");
                }
                PersonApplicationRow inserted = draftRow(IdGeneratorUtil.nextLongId(), userId, providerCode, update);
                requireChanged(dao.insertApplication(inserted), "PERSON_APPLICATION_CREATE_CONFLICT");
            } else {
                if (!editable(current.getStatus()) || intValue(current.getVersion()) != update.expectedVersion()) {
                    throw failure("PERSON_APPLICATION_VERSION_CONFLICT");
                }
                PersonApplicationRow changed = draftRow(current.getPersonApplicationId(), userId,
                    current.getProviderCode(), update);
                changed.setVersion(update.expectedVersion());
                requireChanged(dao.updateDraft(changed), "PERSON_APPLICATION_VERSION_CONFLICT");
            }
        } catch (DuplicateKeyException exception) {
            throw failure("PERSON_APPLICATION_IDENTITY_CONFLICT", exception);
        }
        return requireApplication(dao.selectOpenByUserId(userId));
    }
    /**
     * 校验申请允许提交
     */
    public void requireSubmissionAllowed(long userId, Long targetProfileId, String identityKey) {
        if (identityKey == null || identityKey.isBlank()) {
            throw failure("PERSON_IDENTITY_REQUIRED");
        }
        PersonProfileRow profile = dao.lockActiveProfileByIdentity(identityKey);
        PersonBindingRow userBinding = dao.lockEffectiveBindingByUser(userId);
        if (userBinding != null) {
            if ("SUSPENDED".equals(userBinding.getStatus())) {
                throw failure("PERSON_BINDING_SUSPENDED");
            }
            if (targetProfileId == null || !userBinding.getPersonProfileId().equals(targetProfileId)) {
                throw failure("PERSON_ACCOUNT_ALREADY_BOUND");
            }
            if (profile != null && !profile.getPersonProfileId().equals(targetProfileId)) {
                throw failure("PERSON_IDENTITY_CONFLICT");
            }
            return;
        }
        PersonBindingRow profileBinding = profile == null
            ? null : dao.lockEffectiveBindingByProfile(profile.getPersonProfileId());
        if (profileBinding != null && profileBinding.getUserId() != userId) {
            throw failure("PERSON_REBIND_CONFIRMATION_REQUIRED");
        }
        if (profileBinding != null && "SUSPENDED".equals(profileBinding.getStatus())) {
            throw failure("PERSON_BINDING_SUSPENDED");
        }
    }
    /**
     * 新增申请提交记录
     */
    public PersonSubmission insertSubmission(PersonApplication application, PersonIdentityFields fields,
                                      int submissionSeq, Instant submittedTime) {
        PersonSubmissionRow row = submissionRow(application, fields, submissionSeq, submittedTime);
        try {
            requireChanged(dao.insertSubmission(row), "PERSON_SUBMISSION_CONFLICT");
        } catch (DuplicateKeyException exception) {
            throw failure("PERSON_SUBMISSION_CONFLICT", exception);
        }
        return submission(row);
    }
    /**
     * 校验并获取提交记录
     */
    @Override
    public PersonSubmission requireSubmission(long applicationId, int submissionSeq) {
        PersonSubmissionRow row = dao.selectSubmission(applicationId, submissionSeq);
        if (row == null) {
            throw failure("PERSON_SUBMISSION_NOT_FOUND");
        }
        return submission(row);
    }
    /**
     * 标记申请为等待处理
     */
    public PersonApplication markWaiting(long applicationId, int submissionSeq, int expectedVersion,
                                  Instant submittedTime) {
        requireChanged(dao.markWaiting(applicationId, submissionSeq, expectedVersion, submittedTime),
            "PERSON_APPLICATION_VERSION_CONFLICT");
        return requireApplication(dao.lockApplicationById(applicationId));
    }
    /**
     * 发布已审核通过的档案
     */
    @Override

    public PersonPublication publishApproved(long applicationId, int snapshotVersion, Instant finishedTime) {
        PersonApplicationRow application = dao.lockApplicationById(applicationId);
        if (application == null || !"WAITING".equals(application.getStatus())
            || intValue(application.getSubmissionSeq()) != snapshotVersion) {
            throw failure("PERSON_APPLICATION_DECISION_CONFLICT");
        }
        if ("Y".equals(application.getRebindIntent())) {
            throw failure("PERSON_REBIND_NOT_SUPPORTED_BY_THIS_COMMAND");
        }
        PersonSubmissionRow submission = dao.selectSubmission(applicationId, snapshotVersion);
        if (submission == null) {
            throw failure("PERSON_SUBMISSION_NOT_FOUND");
        }
        try {
            PublicationTarget target = publicationTarget(application.getApplicantUserId(), submission);
            PersonVersionRow currentVersion = dao.selectCurrentVersionForUpdate(target.profile().getPersonProfileId());
            int nextVersion = currentVersion == null ? 1 : intValue(currentVersion.getVersionNo()) + 1;
            if (currentVersion != null) {
                requireChanged(dao.supersedeVersion(currentVersion.getPersonVersionId()),
                    "PERSON_PROFILE_VERSION_CONFLICT");
            }
            PersonVersionRow version = versionRow(target.profile().getPersonProfileId(), nextVersion,
                submission, finishedTime);
            requireChanged(dao.insertVersion(version), "PERSON_PROFILE_VERSION_CONFLICT");
            PersonProfileRow updatedProfile = profileRow(target.profile().getPersonProfileId(),
                target.profile().getPreviousProfileId(), version.getPersonVersionId(), submission,
                target.profile().getVersion());
            requireChanged(dao.updateProfile(updatedProfile), "PERSON_PROFILE_VERSION_CONFLICT");
            PersonBindingRow binding = target.binding();
            if (binding == null) {
                binding = bindingRow(target.profile().getPersonProfileId(), application.getApplicantUserId(),
                    submission.getPersonSubmissionId(), finishedTime);
                requireChanged(dao.insertBinding(binding), "PERSON_BINDING_CONFLICT");
                requireChanged(dao.insertBindingEvent(bindingEvent(binding, submission.getPersonSubmissionId(),
                    finishedTime)), "PERSON_BINDING_EVENT_CONFLICT");
            }
            requireChanged(dao.finishApplication(applicationId, snapshotVersion,
                intValue(application.getDecisionVersion()), intValue(application.getVersion()), finishedTime),
                "PERSON_APPLICATION_DECISION_CONFLICT");
            return new PersonPublication(target.profile().getPersonProfileId(), version.getPersonVersionId(),
                binding.getPersonBindingId(), target.successor());
        } catch (DuplicateKeyException exception) {
            throw failure("PERSON_PUBLICATION_CONFLICT", exception);
        }
    }
    /**
     * 更新workflowstatus。
     */
    public void updateWorkflowStatus(long applicationId, int snapshotVersion, String status,
                              int expectedVersion, Instant occurredTime) {
        requireChanged(dao.updateWorkflowStatus(applicationId, snapshotVersion, status,
            expectedVersion, occurredTime), "PERSON_APPLICATION_DECISION_CONFLICT");
    }
    /**
     * 查询生效档案投影
     */
    public List<PersonActiveProjection> findActiveProjections(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return dao.selectActiveProjections(userIds).stream().map(row -> new PersonActiveProjection(
            row.getUserId(), row.getPersonProfileId(), row.getVerifiedAt())).toList();
    }
    /**
     * 解析发布目标
     */
    private PublicationTarget publicationTarget(long userId, PersonSubmissionRow submission) {
        PersonProfileRow profile = dao.lockActiveProfileByIdentity(submission.getIdentityKey());
        PersonBindingRow userBinding = dao.lockEffectiveBindingByUser(userId);
        if (userBinding != null) {
            if ("SUSPENDED".equals(userBinding.getStatus())) {
                throw failure("PERSON_BINDING_SUSPENDED");
            }
            if (submission.getTargetProfileId() == null
                || !userBinding.getPersonProfileId().equals(submission.getTargetProfileId())) {
                throw failure("PERSON_ACCOUNT_ALREADY_BOUND");
            }
            if (profile != null && !profile.getPersonProfileId().equals(submission.getTargetProfileId())) {
                throw failure("PERSON_IDENTITY_CONFLICT");
            }
            PersonProfileRow target = profile == null
                ? dao.lockActiveProfileById(submission.getTargetProfileId()) : profile;
            if (target == null) {
                throw failure("PERSON_PROFILE_NOT_FOUND");
            }
            PersonBindingRow targetBinding = dao.lockEffectiveBindingByProfile(target.getPersonProfileId());
            if (targetBinding == null || !targetBinding.getPersonBindingId().equals(userBinding.getPersonBindingId())) {
                throw failure("PERSON_BINDING_CONFLICT");
            }
            if (!submission.getIdentityKey().equals(target.getIdentityKey())) {
                requireChanged(dao.updateIdentityGuard(target.getPersonProfileId(), submission.getIdentityKey()),
                    "PERSON_IDENTITY_CONFLICT");
            }
            return new PublicationTarget(target, targetBinding, false);
        }
        PersonBindingRow profileBinding = profile == null
            ? null : dao.lockEffectiveBindingByProfile(profile.getPersonProfileId());
        if (profileBinding != null && profileBinding.getUserId() != userId) {
            throw failure("PERSON_REBIND_CONFIRMATION_REQUIRED");
        }
        if (profileBinding != null && "SUSPENDED".equals(profileBinding.getStatus())) {
            throw failure("PERSON_BINDING_SUSPENDED");
        }
        if (profile != null) {
            return new PublicationTarget(profile, profileBinding, false);
        }
        PersonProfileRow revoked = dao.lockLatestRevokedProfileByIdentity(submission.getIdentityKey());
        long profileId = IdGeneratorUtil.nextLongId();
        PersonProfileRow created = profileRow(profileId,
            revoked == null ? null : revoked.getPersonProfileId(), null, submission, 0);
        requireChanged(dao.insertIdentityGuard(IdGeneratorUtil.nextLongId(), submission.getIdentityKey(), profileId),
            "PERSON_IDENTITY_CONFLICT");
        requireChanged(dao.insertProfile(created), "PERSON_IDENTITY_CONFLICT");
        return new PublicationTarget(created, null, revoked != null);
    }
    /**
     * 转换申请草稿读模型
     */
    private PersonApplicationRow draftRow(long applicationId, long userId, String providerCode,
                                          PersonDraftUpdate update) {
        PersonApplicationRow row = new PersonApplicationRow();
        row.setPersonApplicationId(applicationId);
        row.setApplicantUserId(userId);
        row.setTargetProfileId(update.targetProfileId());
        row.setProviderCode(providerCode);
        copy(row, update.fields());
        return row;
    }
    /**
     * 转换申请提交读模型
     */
    private PersonSubmissionRow submissionRow(PersonApplication application, PersonIdentityFields fields,
                                              int submissionSeq, Instant submittedTime) {
        PersonSubmissionRow row = new PersonSubmissionRow();
        row.setPersonSubmissionId(IdGeneratorUtil.nextLongId());
        row.setPersonApplicationId(application.personApplicationId());
        row.setSubmissionSeq(submissionSeq);
        row.setApplicantUserId(application.applicantUserId());
        row.setProviderCode(application.providerCode());
        row.setRebindIntent(application.rebindIntent() ? "Y" : "N");
        row.setTargetProfileId(application.targetProfileId());
        row.setExpectedBindingId(application.expectedBindingId());
        row.setExpectedBindingVersion(application.expectedBindingVersion());
        row.setFieldSnapshotJson(JsonUtils.toJsonString(fields));
        row.setSubmittedTime(submittedTime);
        copy(row, fields);
        return row;
    }
    /**
     * 转换档案读模型
     */
    private PersonProfileRow profileRow(long profileId, Long previousProfileId, Long currentVersionId,
                                        PersonSubmissionRow submission, Integer version) {
        PersonProfileRow row = new PersonProfileRow();
        row.setPersonProfileId(profileId);
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
    private PersonVersionRow versionRow(long profileId, int versionNo, PersonSubmissionRow submission,
                                        Instant publishedTime) {
        PersonVersionRow row = new PersonVersionRow();
        row.setPersonVersionId(IdGeneratorUtil.nextLongId());
        row.setPersonProfileId(profileId);
        row.setVersionNo(versionNo);
        row.setSourceType("USER_SUBMISSION");
        row.setSourceId(submission.getPersonSubmissionId());
        row.setStatus("CURRENT");
        row.setPublishedTime(publishedTime);
        copy(row, submission);
        return row;
    }
    /**
     * 转换绑定读模型
     */
    private PersonBindingRow bindingRow(long profileId, long userId, long sourceId, Instant boundTime) {
        PersonBindingRow row = new PersonBindingRow();
        row.setPersonBindingId(IdGeneratorUtil.nextLongId());
        row.setPersonProfileId(profileId);
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
    private PersonBindingEventRow bindingEvent(PersonBindingRow binding, long sourceId, Instant occurredTime) {
        PersonBindingEventRow row = new PersonBindingEventRow();
        row.setPersonBindingEventId(IdGeneratorUtil.nextLongId());
        row.setPersonBindingId(binding.getPersonBindingId());
        row.setPersonProfileId(binding.getPersonProfileId());
        row.setUserId(binding.getUserId());
        row.setEventType("ACTIVE");
        row.setBindingVersion(binding.getBindingVersion());
        row.setSourceType("USER_SUBMISSION");
        row.setSourceId(sourceId);
        row.setReason("PERSON_VERIFICATION_APPROVED");
        row.setOccurredTime(occurredTime);
        return row;
    }
    /**
     * 处理application。
     */
    private PersonApplication application(PersonApplicationRow row) {
        return new PersonApplication(row.getPersonApplicationId(), row.getApplicantUserId(),
            row.getTargetProfileId(), row.getStatus(), fields(row), row.getProviderCode(),
            intValue(row.getSubmissionSeq()), "Y".equals(row.getRebindIntent()), row.getExpectedBindingId(),
            row.getExpectedBindingVersion(), intValue(row.getDecisionVersion()), intValue(row.getVersion()),
            row.getSubmittedTime(), row.getFinishedTime());
    }
    /**
     * 组装申请提交数据
     */
    private PersonSubmission submission(PersonSubmissionRow row) {
        return new PersonSubmission(row.getPersonSubmissionId(), row.getPersonApplicationId(),
            intValue(row.getSubmissionSeq()), row.getApplicantUserId(), fields(row), row.getProviderCode(),
            row.getSubmittedTime());
    }
    /**
     * 提取并规范化申请身份字段
     */
    private PersonIdentityFields fields(PersonApplicationRow row) {
        return new PersonIdentityFields(row.getFullName(), row.getDocumentTypeCode(), row.getDocumentNumber(),
            row.getIdentityKey(), row.getGender(), row.getBirthDate(), row.getValidFrom(), row.getValidUntil());
    }
    /**
     * 提取并规范化申请身份字段
     */
    private PersonIdentityFields fields(PersonSubmissionRow row) {
        return new PersonIdentityFields(row.getFullName(), row.getDocumentTypeCode(), row.getDocumentNumber(),
            row.getIdentityKey(), row.getGender(), row.getBirthDate(), row.getValidFrom(), row.getValidUntil());
    }
    /**
     * 复制领域数据并替换指定字段
     */
    private void copy(PersonApplicationRow target, PersonIdentityFields fields) {
        target.setFullName(fields.fullName());
        target.setDocumentTypeCode(fields.documentTypeCode());
        target.setDocumentNumber(fields.documentNumber());
        target.setIdentityKey(fields.identityKey());
        target.setGender(fields.gender());
        target.setBirthDate(fields.birthDate());
        target.setValidFrom(fields.validFrom());
        target.setValidUntil(fields.validUntil());
    }
    /**
     * 复制领域数据并替换指定字段
     */
    private void copy(PersonSubmissionRow target, PersonIdentityFields fields) {
        target.setFullName(fields.fullName());
        target.setDocumentTypeCode(fields.documentTypeCode());
        target.setDocumentNumber(fields.documentNumber());
        target.setIdentityKey(fields.identityKey());
        target.setGender(fields.gender());
        target.setBirthDate(fields.birthDate());
        target.setValidFrom(fields.validFrom());
        target.setValidUntil(fields.validUntil());
    }
    /**
     * 复制领域数据并替换指定字段
     */
    private void copy(PersonProfileRow target, PersonSubmissionRow source) {
        target.setFullName(source.getFullName());
        target.setDocumentTypeCode(source.getDocumentTypeCode());
        target.setDocumentNumber(source.getDocumentNumber());
        target.setIdentityKey(source.getIdentityKey());
        target.setGender(source.getGender());
        target.setBirthDate(source.getBirthDate());
        target.setValidFrom(source.getValidFrom());
        target.setValidUntil(source.getValidUntil());
    }
    /**
     * 复制领域数据并替换指定字段
     */
    private void copy(PersonVersionRow target, PersonSubmissionRow source) {
        target.setFullName(source.getFullName());
        target.setDocumentTypeCode(source.getDocumentTypeCode());
        target.setDocumentNumber(source.getDocumentNumber());
        target.setIdentityKey(source.getIdentityKey());
        target.setGender(source.getGender());
        target.setBirthDate(source.getBirthDate());
        target.setValidFrom(source.getValidFrom());
        target.setValidUntil(source.getValidUntil());
    }
    /**
     * 校验并获取申请记录
     */
    private PersonApplication requireApplication(PersonApplicationRow row) {
        if (row == null) {
            throw failure("PERSON_APPLICATION_NOT_FOUND");
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
     * 规范化审核决定
     */
    private String normalizeDecision(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }
    /**
     * 校验申请草稿字段
     */
    private void validateDraft(PersonIdentityFields fields) {
        if (length(fields.fullName()) > 100 || length(fields.documentNumber()) > 128) {
            throw failure("PERSON_FIELD_TOO_LONG");
        }
        if (fields.gender() != null && !GENDERS.contains(fields.gender())) {
            throw failure("PERSON_GENDER_INVALID");
        }
        if (fields.documentTypeCode() != null) {
            PersonDocumentTypeRule rule = documentRule(fields.documentTypeCode());
            if (fields.documentNumber() != null && !Pattern.matches(rule.numberPattern(), fields.documentNumber())) {
                throw failure("PERSON_DOCUMENT_NUMBER_INVALID");
            }
        }
        validateDates(fields, false);
    }
    /**
     * 校验申请信息完整性
     */
    private void validateComplete(PersonIdentityFields fields) {
        validateDraft(fields);
        if (fields.fullName() == null || fields.documentTypeCode() == null || fields.documentNumber() == null
            || fields.gender() == null || fields.birthDate() == null) {
            throw failure("PERSON_FIELDS_INCOMPLETE");
        }
        PersonDocumentTypeRule rule = documentRule(fields.documentTypeCode());
        if (!Pattern.matches(rule.numberPattern(), fields.documentNumber())) {
            throw failure("PERSON_DOCUMENT_NUMBER_INVALID");
        }
        if (rule.validityRequired() && (fields.validFrom() == null || fields.validUntil() == null)) {
            throw failure("PERSON_DOCUMENT_VALIDITY_REQUIRED");
        }
        validateDates(fields, true);
    }
    /**
     * 校验日期范围
     */
    private void validateDates(PersonIdentityFields fields, boolean complete) {
        LocalDate today = LocalDate.now(clock);
        if (fields.birthDate() != null && fields.birthDate().isAfter(today)) {
            throw failure("PERSON_BIRTH_DATE_INVALID");
        }
        if (fields.validFrom() != null && fields.validUntil() != null
            && fields.validFrom().isAfter(fields.validUntil())) {
            throw failure("PERSON_DOCUMENT_VALIDITY_INVALID");
        }
        if (complete && fields.validFrom() != null && fields.validUntil() != null
            && (fields.validFrom().isAfter(today) || fields.validUntil().isBefore(today))) {
            throw failure("PERSON_DOCUMENT_EXPIRED");
        }
    }
    /**
     * 解析证件类型校验规则
     */
    private PersonDocumentTypeRule documentRule(String documentTypeCode) {
        return findDocumentType(documentTypeCode)
            .orElseThrow(() -> failure("PERSON_DOCUMENT_TYPE_UNAVAILABLE"));
    }
    /**
     * 解析默认认证提供方
     */
    private String defaultProvider() {
        String providerCode = configService.getConfigValue(DEFAULT_PROVIDER_KEY);
        if (providerCode == null || providerCode.isBlank()) {
            throw failure("PERSON_PROVIDER_NOT_CONFIGURED");
        }
        return providerCode.strip();
    }
    /**
     * 校验认证提供方已启用
     */
    private void requireProviderEnabled(String providerCode) {
        try {
            providers.requireEnabled(providerCode);
        } catch (PersonVerificationException exception) {
            throw new PersonApplicationException("PERSON_PROVIDER_UNAVAILABLE", exception);
        }
    }
    /**
     * 启动认证流程并记录尝试
     */
    private void startVerificationAttempt(PersonVerificationStartAttemptCommand command) {
        try {
            attempts.startAttempt(command);
        } catch (PersonVerificationException exception) {
            throw new PersonApplicationException("PERSON_PROVIDER_UNAVAILABLE", exception);
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
    private String fingerprint(PersonIdentityFields fields, int snapshotVersion) {
        String canonical = fields.identityKey() + "\n" + snapshotVersion;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
    /**
     * 解析材料所有者
     */
    private MaterialOwnerKey owner(MaterialOwnerType type, long ownerId) {
        return new MaterialOwnerKey(ProfileType.PERSON, type, ownerId);
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
            throw failure("PERSON_USER_INVALID");
        }
    }
    /**
     * 构造业务失败异常
     */
    private PersonApplicationException failure(String category) {
        return new PersonApplicationException(category);
    }
    /**
     * 构造业务失败异常
     */
    private PersonApplicationException failure(String category, Throwable cause) {
        return new PersonApplicationException(category, cause);
    }
    /**
     * 承载PublicationTarget业务规则的领域服务。
     */
    private record PublicationTarget(PersonProfileRow profile, PersonBindingRow binding, boolean successor) {
    }
}
