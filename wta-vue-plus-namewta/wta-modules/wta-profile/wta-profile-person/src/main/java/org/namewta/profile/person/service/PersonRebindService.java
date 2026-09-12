package org.namewta.profile.person.service;
import org.namewta.profile.person.domain.exception.PersonRebindException;
import org.namewta.profile.person.dao.PersonRebindDao;
import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.person.domain.model.read.PersonRebindCandidateRow;
import org.namewta.profile.person.event.PersonReboundEvent;
import org.namewta.profile.person.port.provider.PersonVerificationProviderRegistryPort;
import org.namewta.profile.person.port.verification.PersonVerificationService;
import org.namewta.profile.person.port.notification.PersonRebindNotificationPort;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.person.domain.application.PersonDocumentTypeRule;
import org.namewta.profile.person.domain.application.PersonApplication;
import org.namewta.profile.person.domain.bo.PersonApplicationSaveBo;
import org.namewta.profile.person.domain.application.PersonIdentityFields;
import org.namewta.profile.person.domain.application.PersonRebindPublication;
import org.namewta.profile.person.domain.application.PersonRebindProcessCommand;
import org.namewta.profile.person.domain.application.PersonSubmission;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.profile.person.domain.model.read.PersonApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonBindingEventRow;
import org.namewta.profile.person.domain.model.read.PersonBindingRow;
import org.namewta.profile.person.domain.model.read.PersonDocumentTypeRow;
import org.namewta.profile.person.domain.model.read.PersonProfileRow;
import org.namewta.profile.person.domain.model.read.PersonSubmissionRow;
import org.namewta.profile.person.domain.model.read.PersonVersionRow;
import org.namewta.profile.person.domain.bo.PersonRebindConfirmBo;
import org.namewta.profile.person.domain.vo.PersonRebindConfirmationVo;
import org.namewta.profile.person.domain.bo.PersonRebindIdentityBo;
import org.namewta.profile.person.domain.bo.PersonRebindMatchBo;
import org.namewta.profile.person.domain.vo.PersonRebindMatchVo;
import org.namewta.profile.person.domain.bo.PersonRebindProbeBo;
import org.namewta.profile.person.domain.vo.PersonRebindProbeVo;
import org.namewta.profile.person.domain.vo.PersonRebindSubmissionVo;
import org.namewta.profile.person.domain.bo.PersonRebindSubmitBo;
import org.namewta.profile.person.domain.vo.PersonRebindUnbindVo;
import org.namewta.profile.person.domain.exception.PersonVerificationException;
import org.namewta.profile.person.domain.verification.PersonVerificationStartAttemptCommand;
import org.namewta.system.api.UserService;
import org.namewta.system.api.ConfigService;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
/**
 * 创建个人换绑业务服务。
 */
@Service
public class PersonRebindService {
    private static final Set<String> EDITABLE_STATUSES = Set.of("DRAFT", "BACK", "CANCEL");
    private static final Set<String> GENDERS = Set.of("MALE", "FEMALE", "UNKNOWN");
    private final PersonRebindDao dao;
    private final PersonApplicationDao applicationDao;
    private final ProfileMaterialPort materials;
    private final PersonVerificationProviderRegistryPort providers;
    private final PersonVerificationService attempts;
    private final PersonWorkflowGateway workflow;
    private final UserService users;
    private final Clock clock;
    private final ConfigService configService;
    private final PersonRebindNotificationPort notifications;
    private final ApplicationEventPublisher events;
    /** 创建个人换绑业务服务。 */
    public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao,
                               Object jsonMapper,
                               ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers,
                               PersonVerificationService attempts, PersonWorkflowGateway workflow,
                               UserService users) {
        this(dao, applicationDao, jsonMapper, materials, providers, attempts, workflow, users,
            Clock.systemUTC());
    }
    /** 创建可注入时钟的个人换绑业务服务，测试场景据此固定时间。 */
    public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao,
                        Object jsonMapper,
                        ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers,
                        PersonVerificationService attempts, PersonWorkflowGateway workflow,
                        UserService users, Clock clock) {
        this(dao, applicationDao, jsonMapper, materials, providers, attempts, workflow, users, clock,
            null, null, null);
    }
    /** 创建可处理工作流事件的个人换绑业务服务。 */
    @Autowired
    public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao,
                               ProfileMaterialPort materials,
                               PersonVerificationProviderRegistryPort providers,
                               PersonVerificationService attempts, PersonWorkflowGateway workflow,
                               UserService users, ConfigService configService,
                               PersonRebindNotificationPort notifications,
                               ApplicationEventPublisher events) {
        this(dao, applicationDao, null, materials, providers, attempts, workflow, users,
            Clock.systemUTC(), configService, notifications, events);
    }
    /** 兼容存量测试适配器的完整构造方法，jsonMapper 参数已由统一 JsonUtils 接管。 */
    @Deprecated
    public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao,
                               Object jsonMapper, ProfileMaterialPort materials,
                               PersonVerificationProviderRegistryPort providers,
                               PersonVerificationService attempts, PersonWorkflowGateway workflow,
                               UserService users, ConfigService configService,
                               PersonRebindNotificationPort notifications,
                               ApplicationEventPublisher events) {
        this(dao, applicationDao, jsonMapper, materials, providers, attempts, workflow, users,
            Clock.systemUTC(), configService, notifications, events);
    }
    /** 初始化个人换绑服务依赖。 */
    private PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao,
                        Object jsonMapper, ProfileMaterialPort materials,
                        PersonVerificationProviderRegistryPort providers,
                        PersonVerificationService attempts, PersonWorkflowGateway workflow,
                        UserService users, Clock clock, ConfigService configService,
                        PersonRebindNotificationPort notifications, ApplicationEventPublisher events) {
        this.dao = dao;
        this.applicationDao = applicationDao;
                this.materials = materials;
        this.providers = providers;
        this.attempts = attempts;
        this.workflow = workflow;
        this.users = users;
        this.clock = clock;
        this.configService = configService;
        this.notifications = notifications;
        this.events = events;
    }
    /** 兼容存量测试适配器使用的具体注册表构造方法。 */
    @Deprecated
    public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao,
                               Object jsonMapper, ProfileMaterialPort materials,
                               org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry providers,
                               PersonVerificationService attempts, PersonWorkflowGateway workflow,
                               UserService users, Clock clock) {
        this(dao, applicationDao, jsonMapper, materials, (PersonVerificationProviderRegistryPort) providers,
            attempts, workflow, users, clock, null, null, null);
    }
    /**
     * 校验申请身份并返回探测结果
     */
    public PersonRebindProbeVo probe(PersonRebindProbeBo command) {
        if (command == null) {
            throw failure("PERSON_REBIND_PROBE_INVALID");
        }
        String type = upper(command.documentTypeCode());
        String number = upper(command.documentNumber());
        if (type == null || number == null) {
            throw failure("PERSON_REBIND_PROBE_INVALID");
        }
        return new PersonRebindProbeVo(probeStatus(type + ":" + number));
    }
    /**
     * 匹配身份数据
     */
    public PersonRebindMatchVo match(long userId, PersonRebindMatchBo command) {
        PersonIdentityFields fields = safeIdentity(command == null ? null : command.identity());
        if (fields == null || findEffectiveProfileIdByUser(userId) != null) {
            return unavailable();
        }
        Optional<PersonApplication> current = findOpenByUserId(userId);
        if (current.isEmpty() || !EDITABLE_STATUSES.contains(current.get().status())
            || !same(current.get().fields(), fields)) {
            return unavailable();
        }
        Optional<PersonRebindCandidateRow> candidate = findExactCandidate(fields);
        if (candidate.isEmpty() || candidate.get().getOldUserId() == userId) {
            return unavailable();
        }
        return new PersonRebindMatchVo("REBIND_AVAILABLE", maskedPhone(candidate.get().getOldUserId()));
    }
    /**
     * 确认当前业务操作
     */

    public PersonRebindConfirmationVo confirm(long userId, PersonRebindConfirmBo command) {
        requireUserId(userId);
        if (command == null) {
            throw failure("PERSON_REBIND_NOT_AVAILABLE");
        }
        PersonIdentityFields fields = requireIdentity(command.identity());
        PersonApplicationRow application = lockOpenApplication(userId);
        if (!EDITABLE_STATUSES.contains(application.getStatus())
            || intValue(application.getVersion()) != command.expectedVersion()
            || !same(application, fields)) {
            throw failure("PERSON_REBIND_NOT_AVAILABLE");
        }
        requireApplicantUnbound(userId);
        PersonRebindCandidateRow candidate = findExactCandidate(fields)
            .filter(value -> value.getOldUserId() != userId)
            .orElseThrow(() -> failure("PERSON_REBIND_NOT_AVAILABLE"));
        candidate = lockCandidate(candidate, fields);
        int version = confirm(application, candidate, command.expectedVersion());
        return new PersonRebindConfirmationVo("CONFIRMED", maskedPhone(candidate.getOldUserId()), version);
    }
    /**
     * 提交申请并启动后续流程
     */

    public PersonRebindSubmissionVo submit(long userId, PersonRebindSubmitBo command) {
        requireUserId(userId);
        if (command == null) {
            throw failure("PERSON_REBIND_SUBMIT_INVALID");
        }
        PersonApplication application = lockOpenByUserId(userId);
        if (application.applicantUserId() != userId || !EDITABLE_STATUSES.contains(application.status())
            || application.version() != command.expectedVersion()) {
            throw failure("PERSON_REBIND_VERSION_CONFLICT");
        }
        validateComplete(application.fields());
        requireProviderEnabled(application.providerCode());
        requireApplicantUnbound(userId);
        requireFrozenCandidate(application);
        MaterialOwnerKey working = owner(MaterialOwnerType.WORKING, application.personApplicationId());
        materials.validateRequired(working, application.fields().documentTypeCode(), Set.of("ALWAYS"));
        int snapshotVersion = application.submissionSeq() + 1;
        Instant submittedTime = clock.instant();
        PersonSubmission submission = insertSubmission(application, application.fields(),
            snapshotVersion, submittedTime);
        materials.snapshotImmutable(working, owner(MaterialOwnerType.SUBMISSION,
            submission.personSubmissionId()));
        PersonApplication waiting = markWaiting(application.personApplicationId(), snapshotVersion,
            command.expectedVersion(), submittedTime);
        startVerificationAttempt(new PersonVerificationStartAttemptCommand(application.personApplicationId(),
            submission.personSubmissionId(), fingerprint(application.fields(), snapshotVersion)));
        workflow.start(application.personApplicationId(), submission.personSubmissionId(), snapshotVersion);
        return new PersonRebindSubmissionVo(waiting.status(), waiting.submissionSeq(), waiting.version());
    }
    /**
     * 解除档案绑定
     */

    public PersonRebindUnbindVo unbind(long userId) {
        requireUserId(userId);
        unbindBinding(userId, clock.instant());
        return new PersonRebindUnbindVo("UNBOUND");
    }
    /**
     * 发布换绑后的档案
     */

    public Optional<PersonRebindPublication> publishApprovedRebind(long applicationId, int snapshotVersion,
                                                                   Instant finishedTime) {
        PersonApplicationRow application = dao.lockApplication(applicationId);
        if (application == null || !"WAITING".equals(application.getStatus())
            || intValue(application.getSubmissionSeq()) != snapshotVersion) {
            return Optional.empty();
        }
        if (!"Y".equals(application.getRebindIntent())) {
            return Optional.empty();
        }
        PersonSubmissionRow submission = dao.lockSubmission(applicationId, snapshotVersion);
        if (submission == null || !"Y".equals(submission.getRebindIntent())) {
            throw failure("PERSON_REBIND_SNAPSHOT_INVALID");
        }
        requireFrozenSnapshot(application, submission);
        PersonProfileRow profile = dao.lockProfile(submission.getTargetProfileId());
        PersonBindingRow oldBinding = dao.lockExpectedBinding(submission.getExpectedBindingId(),
            submission.getTargetProfileId(), submission.getExpectedBindingVersion());
        if (profile == null || oldBinding == null || !same(submission, profile)) {
            throw failure("PERSON_REBIND_BINDING_CHANGED");
        }
        if (dao.lockEffectiveBindingByUser(application.getApplicantUserId()) != null) {
            throw failure("PERSON_REBIND_APPLICANT_ALREADY_BOUND");
        }
        try {
            PersonVersionRow currentVersion = dao.lockCurrentVersion(profile.getPersonProfileId());
            int nextVersion = currentVersion == null ? 1 : intValue(currentVersion.getVersionNo()) + 1;
            if (currentVersion != null) {
                requireChanged(dao.supersedeVersion(currentVersion.getPersonVersionId()),
                    "PERSON_REBIND_PROFILE_VERSION_CONFLICT");
            }
            PersonVersionRow version = version(profile.getPersonProfileId(), nextVersion, submission, finishedTime);
            requireChanged(dao.insertVersion(version), "PERSON_REBIND_PROFILE_VERSION_CONFLICT");
            PersonProfileRow updated = updatedProfile(profile, version.getPersonVersionId(), submission);
            requireChanged(dao.updateProfile(updated), "PERSON_REBIND_PROFILE_VERSION_CONFLICT");
            int oldBindingVersion = intValue(oldBinding.getBindingVersion());
            requireChanged(dao.unbind(oldBinding.getPersonBindingId(), oldBindingVersion,
                application.getApplicantUserId(), finishedTime), "PERSON_REBIND_BINDING_CHANGED");
            requireChanged(dao.insertBindingEvent(bindingEvent(oldBinding, "UNBOUND",
                oldBindingVersion + 1, submission.getPersonSubmissionId(), "PERSON_REBIND_APPROVED", finishedTime)),
                "PERSON_REBIND_BINDING_EVENT_CONFLICT");
            PersonBindingRow newBinding = binding(profile.getPersonProfileId(), application.getApplicantUserId(),
                submission.getPersonSubmissionId(), finishedTime);
            requireChanged(dao.insertBinding(newBinding), "PERSON_REBIND_BINDING_CONFLICT");
            requireChanged(dao.insertBindingEvent(bindingEvent(newBinding, "ACTIVE", 1,
                submission.getPersonSubmissionId(), "PERSON_REBIND_APPROVED", finishedTime)),
                "PERSON_REBIND_BINDING_EVENT_CONFLICT");
            requireChanged(dao.finishApplication(applicationId, snapshotVersion,
                intValue(application.getDecisionVersion()), intValue(application.getVersion()), finishedTime),
                "PERSON_REBIND_DECISION_CONFLICT");
            return Optional.of(new PersonRebindPublication(submission.getPersonSubmissionId(), version.getPersonVersionId(),
                new PersonReboundEvent(profile.getPersonProfileId(), applicationId, oldBinding.getUserId())));
        } catch (DuplicateKeyException exception) {
            throw failure("PERSON_REBIND_PUBLICATION_CONFLICT", exception);
        }
    }
    /** 处理工作流换绑事件并编排发布、材料快照和事务后通知。 */

    public void handleProcess(PersonRebindProcessCommand command) {
        if (command == null || configService == null || !expectedFlowCode().equals(command.flowCode())
            || !"FINISH".equals(normalizeStatus(command.status()))
            || "REJECT".equals(normalizeDecision(command.decision()))) {
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
        publishApprovedRebind(applicationId, snapshotVersion,
            command.finishedTime() == null ? clock.instant() : command.finishedTime()).ifPresent(publication -> {
                materials.snapshotImmutable(owner(MaterialOwnerType.SUBMISSION, publication.personSubmissionId()),
                    owner(MaterialOwnerType.VERSION, publication.personVersionId()));
                if (notifications != null) {
                    notifications.stage(publication.event());
                }
                if (events != null) {
                    events.publishEvent(publication.event());
                }
            });
    }
    /** 读取个人换绑流程编码。 */
    private String expectedFlowCode() {
        String value = configService.getConfigValue("profile.person.flowCode");
        return value == null ? "" : value.strip();
    }
    /** 规范化工作流状态。 */
    private String normalizeStatus(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }
    /** 规范化工作流决定。 */
    private String normalizeDecision(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }
    /** 解析正整数业务编号。 */
    private Long positiveLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    /**
     * 查询申请探测状态
     */
    public String probeStatus(String identityKey) {
        return dao.selectProbeStatus(identityKey);
    }
    /**
     * 查询精确匹配的候选身份
     */
    public Optional<PersonRebindCandidateRow> findExactCandidate(PersonIdentityFields fields) {
        return Optional.ofNullable(dao.selectExactCandidate(fields.fullName(), fields.documentTypeCode(),
            fields.documentNumber(), fields.identityKey(), fields.gender(), fields.birthDate(),
            fields.validFrom(), fields.validUntil()));
    }
    /**
     * 锁定未完成申请
     */
    public PersonApplicationRow lockOpenApplication(long userId) {
        PersonApplicationRow application = dao.lockOpenApplication(userId);
        if (application == null) {
            throw failure("PERSON_REBIND_APPLICATION_REQUIRED");
        }
        return application;
    }
    /**
     * 校验申请人尚未绑定档案
     */
    public void requireApplicantUnbound(long userId) {
        if (dao.lockEffectiveBindingByUser(userId) != null) {
            throw failure("PERSON_REBIND_APPLICANT_ALREADY_BOUND");
        }
    }
    /**
     * 确认当前业务操作
     */
    public int confirm(PersonApplicationRow application, PersonRebindCandidateRow candidate,
                int expectedVersion) {
        int changed = dao.confirmIntent(application.getPersonApplicationId(), application.getApplicantUserId(),
            candidate.getPersonProfileId(), candidate.getPersonBindingId(), candidate.getBindingVersion(),
            expectedVersion);
        requireChanged(changed, "PERSON_REBIND_VERSION_CONFLICT");
        return expectedVersion + 1;
    }
    /**
     * 锁定身份候选记录
     */
    public PersonRebindCandidateRow lockCandidate(
        PersonRebindCandidateRow candidate, PersonIdentityFields fields) {
        PersonRebindCandidateRow locked = dao.lockFrozenCandidate(candidate.getPersonProfileId(),
            candidate.getPersonBindingId(), candidate.getBindingVersion());
        if (locked == null || !same(fields, locked)) {
            throw failure("PERSON_REBIND_NOT_AVAILABLE");
        }
        return locked;
    }
    /**
     * 校验候选身份已冻结
     */
    public PersonRebindCandidateRow requireFrozenCandidate(PersonApplication application) {
        if (!application.rebindIntent() || application.targetProfileId() == null
            || application.expectedBindingId() == null || application.expectedBindingVersion() == null) {
            throw failure("PERSON_REBIND_CONFIRMATION_REQUIRED");
        }
        PersonRebindCandidateRow candidate = dao.lockFrozenCandidate(
            application.targetProfileId(), application.expectedBindingId(), application.expectedBindingVersion());
        if (candidate == null || !same(application.fields(), candidate)) {
            throw failure("PERSON_REBIND_BINDING_CHANGED");
        }
        return candidate;
    }
    /**
     * 解除指定绑定关系
     */
    public void unbindBinding(long userId, Instant occurredTime) {
        PersonBindingRow binding = dao.lockEffectiveBindingByUser(userId);
        if (binding == null) {
            throw failure("PERSON_BINDING_NOT_FOUND");
        }
        int nextVersion = intValue(binding.getBindingVersion()) + 1;
        requireChanged(dao.unbind(binding.getPersonBindingId(), intValue(binding.getBindingVersion()),
            userId, occurredTime), "PERSON_BINDING_VERSION_CONFLICT");
        requireChanged(dao.insertBindingEvent(bindingEvent(binding, "UNBOUND", nextVersion,
            "SELF_SERVICE", binding.getPersonBindingId(), "PERSON_SELF_UNBOUND", occurredTime)),
            "PERSON_BINDING_EVENT_CONFLICT");
    }
    /**
     * 按用户查询未完成申请
     */
    public Optional<PersonApplication> findOpenByUserId(long userId) {
        return Optional.ofNullable(applicationDao.selectOpenByUserId(userId)).map(this::application);
    }
    /**
     * 按用户查询当前有效档案编号
     */
    public Long findEffectiveProfileIdByUser(long userId) {
        return applicationDao.selectEffectiveProfileIdByUser(userId);
    }
    /**
     * 按用户锁定未完成申请
     */
    public PersonApplication lockOpenByUserId(long userId) {
        PersonApplicationRow row = applicationDao.lockOpenByUserId(userId);
        if (row == null) {
            throw failure("PERSON_APPLICATION_NOT_FOUND");
        }
        return application(row);
    }
    /**
     * 查询证件类型配置
     */
    public Optional<PersonDocumentTypeRule> findDocumentType(String documentTypeCode) {
        PersonDocumentTypeRow row = applicationDao.selectDocumentType(documentTypeCode);
        return Optional.ofNullable(row).map(value -> new PersonDocumentTypeRule(value.getDocumentTypeCode(),
            value.getNumberPattern(), "Y".equals(value.getValidityRequired())));
    }
    /**
     * 新增申请提交记录
     */
    public PersonSubmission insertSubmission(PersonApplication application, PersonIdentityFields fields,
                                      int submissionSeq, Instant submittedTime) {
        PersonSubmissionRow row = submissionRow(application, fields, submissionSeq, submittedTime);
        try {
            requireChanged(applicationDao.insertSubmission(row), "PERSON_SUBMISSION_CONFLICT");
        } catch (DuplicateKeyException exception) {
            throw failure("PERSON_SUBMISSION_CONFLICT", exception);
        }
        return new PersonSubmission(row.getPersonSubmissionId(), row.getPersonApplicationId(),
            intValue(row.getSubmissionSeq()), row.getApplicantUserId(), fields(row), row.getProviderCode(),
            row.getSubmittedTime());
    }
    /**
     * 标记申请为等待处理
     */
    public PersonApplication markWaiting(long applicationId, int submissionSeq, int expectedVersion,
                                  Instant submittedTime) {
        requireChanged(applicationDao.markWaiting(applicationId, submissionSeq, expectedVersion, submittedTime),
            "PERSON_APPLICATION_VERSION_CONFLICT");
        PersonApplicationRow row = applicationDao.lockApplicationById(applicationId);
        if (row == null) {
            throw failure("PERSON_APPLICATION_NOT_FOUND");
        }
        return application(row);
    }
    /**
     * 校验材料快照已冻结
     */
    private void requireFrozenSnapshot(PersonApplicationRow application, PersonSubmissionRow submission) {
        if (!Objects.equals(application.getTargetProfileId(), submission.getTargetProfileId())
            || !Objects.equals(application.getExpectedBindingId(), submission.getExpectedBindingId())
            || !Objects.equals(application.getExpectedBindingVersion(), submission.getExpectedBindingVersion())
            || !same(application, submission)) {
            throw failure("PERSON_REBIND_SNAPSHOT_INVALID");
        }
    }
    /**
     * 处理same。
     */
    public boolean same(PersonIdentityFields fields, PersonRebindCandidateRow candidate) {
        return Objects.equals(fields.fullName(), candidate.getFullName())
            && Objects.equals(fields.documentTypeCode(), candidate.getDocumentTypeCode())
            && Objects.equals(fields.documentNumber(), candidate.getDocumentNumber())
            && Objects.equals(fields.identityKey(), candidate.getIdentityKey())
            && Objects.equals(fields.gender(), candidate.getGender())
            && Objects.equals(fields.birthDate(), candidate.getBirthDate())
            && Objects.equals(fields.validFrom(), candidate.getValidFrom())
            && Objects.equals(fields.validUntil(), candidate.getValidUntil());
    }
    /**
     * 处理same。
     */
    public boolean same(PersonApplicationRow application, PersonIdentityFields fields) {
        return Objects.equals(application.getFullName(), fields.fullName())
            && Objects.equals(application.getDocumentTypeCode(), fields.documentTypeCode())
            && Objects.equals(application.getDocumentNumber(), fields.documentNumber())
            && Objects.equals(application.getIdentityKey(), fields.identityKey())
            && Objects.equals(application.getGender(), fields.gender())
            && Objects.equals(application.getBirthDate(), fields.birthDate())
            && Objects.equals(application.getValidFrom(), fields.validFrom())
            && Objects.equals(application.getValidUntil(), fields.validUntil());
    }
    /**
     * 处理same。
     */
    private boolean same(PersonApplicationRow application, PersonSubmissionRow submission) {
        return Objects.equals(application.getFullName(), submission.getFullName())
            && Objects.equals(application.getDocumentTypeCode(), submission.getDocumentTypeCode())
            && Objects.equals(application.getDocumentNumber(), submission.getDocumentNumber())
            && Objects.equals(application.getIdentityKey(), submission.getIdentityKey())
            && Objects.equals(application.getGender(), submission.getGender())
            && Objects.equals(application.getBirthDate(), submission.getBirthDate())
            && Objects.equals(application.getValidFrom(), submission.getValidFrom())
            && Objects.equals(application.getValidUntil(), submission.getValidUntil());
    }
    /**
     * 处理same。
     */
    private boolean same(PersonSubmissionRow submission, PersonProfileRow profile) {
        return Objects.equals(submission.getFullName(), profile.getFullName())
            && Objects.equals(submission.getDocumentTypeCode(), profile.getDocumentTypeCode())
            && Objects.equals(submission.getDocumentNumber(), profile.getDocumentNumber())
            && Objects.equals(submission.getIdentityKey(), profile.getIdentityKey())
            && Objects.equals(submission.getGender(), profile.getGender())
            && Objects.equals(submission.getBirthDate(), profile.getBirthDate())
            && Objects.equals(submission.getValidFrom(), profile.getValidFrom())
            && Objects.equals(submission.getValidUntil(), profile.getValidUntil());
    }
    /**
     * 查询档案版本信息
     */
    private PersonVersionRow version(long profileId, int versionNo, PersonSubmissionRow submission,
                                     Instant finishedTime) {
        PersonVersionRow row = new PersonVersionRow();
        row.setPersonVersionId(org.namewta.common.mybatis.utils.IdGeneratorUtil.nextLongId());
        row.setPersonProfileId(profileId);
        row.setVersionNo(versionNo);
        row.setSourceType("USER_SUBMISSION");
        row.setSourceId(submission.getPersonSubmissionId());
        row.setPublishedTime(finishedTime);
        copy(row, submission);
        return row;
    }
    /**
     * 构造更新后的档案对象
     */
    private PersonProfileRow updatedProfile(PersonProfileRow profile, long versionId,
                                            PersonSubmissionRow submission) {
        PersonProfileRow row = new PersonProfileRow();
        row.setPersonProfileId(profile.getPersonProfileId());
        row.setCurrentVersionId(versionId);
        row.setVersion(profile.getVersion());
        copy(row, submission);
        return row;
    }
    /**
     * 查询档案绑定信息
     */
    private PersonBindingRow binding(long profileId, long userId, long sourceId, Instant boundTime) {
        PersonBindingRow row = new PersonBindingRow();
        row.setPersonBindingId(org.namewta.common.mybatis.utils.IdGeneratorUtil.nextLongId());
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
    private PersonBindingEventRow bindingEvent(PersonBindingRow binding, String eventType, int bindingVersion,
                                                long sourceId, String reason, Instant occurredTime) {
        return bindingEvent(binding, eventType, bindingVersion, "USER_SUBMISSION", sourceId, reason, occurredTime);
    }
    /**
     * 查询绑定事件记录
     */
    private PersonBindingEventRow bindingEvent(PersonBindingRow binding, String eventType, int bindingVersion,
                                                String sourceType, long sourceId, String reason,
                                                Instant occurredTime) {
        PersonBindingEventRow row = new PersonBindingEventRow();
        row.setPersonBindingEventId(org.namewta.common.mybatis.utils.IdGeneratorUtil.nextLongId());
        row.setPersonBindingId(binding.getPersonBindingId());
        row.setPersonProfileId(binding.getPersonProfileId());
        row.setUserId(binding.getUserId());
        row.setEventType(eventType);
        row.setBindingVersion(bindingVersion);
        row.setSourceType(sourceType);
        row.setSourceId(sourceId);
        row.setReason(reason);
        row.setOccurredTime(occurredTime);
        return row;
    }
    /**
     * 转换申请提交读模型
     */
    private PersonSubmissionRow submissionRow(PersonApplication application, PersonIdentityFields fields,
                                              int submissionSeq, Instant submittedTime) {
        PersonSubmissionRow row = new PersonSubmissionRow();
        row.setPersonSubmissionId(org.namewta.common.mybatis.utils.IdGeneratorUtil.nextLongId());
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
     * 校验申请确实发生变更
     */
    private void requireChanged(int changed, String category) {
        if (changed != 1) {
            throw failure(category);
        }
    }
    /**
     * 安全读取身份信息
     */
    private PersonIdentityFields safeIdentity(PersonRebindIdentityBo command) {
        try {
            PersonIdentityFields fields = normalize(command);
            validateComplete(fields);
            return fields;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    /**
     * 校验身份信息完整有效
     */
    private PersonIdentityFields requireIdentity(PersonRebindIdentityBo command) {
        try {
            PersonIdentityFields fields = normalize(command);
            validateComplete(fields);
            return fields;
        } catch (RuntimeException exception) {
            throw failure("PERSON_REBIND_NOT_AVAILABLE");
        }
    }
    /**
     * 规范化输入数据
     */
    private PersonIdentityFields normalize(PersonRebindIdentityBo command) {
        if (command == null) {
            throw failure("PERSON_REBIND_IDENTITY_REQUIRED");
        }
        return PersonIdentityFields.normalize(new PersonApplicationSaveBo(command.fullName(), command.documentTypeCode(),
            command.documentNumber(), command.gender(), command.birthDate(), command.validFrom(),
            command.validUntil(), 0));
    }
    /**
     * 校验申请信息完整性
     */
    private void validateComplete(PersonIdentityFields fields) {
        if (fields.fullName() == null || fields.documentTypeCode() == null || fields.documentNumber() == null
            || fields.gender() == null || fields.birthDate() == null || fields.fullName().length() > 100
            || fields.documentNumber().length() > 128 || !GENDERS.contains(fields.gender())) {
            throw failure("PERSON_REBIND_IDENTITY_INVALID");
        }
        PersonDocumentTypeRule rule = findDocumentType(fields.documentTypeCode())
            .orElseThrow(() -> failure("PERSON_REBIND_IDENTITY_INVALID"));
        if (!Pattern.matches(rule.numberPattern(), fields.documentNumber())) {
            throw failure("PERSON_REBIND_IDENTITY_INVALID");
        }
        if (rule.validityRequired() && (fields.validFrom() == null || fields.validUntil() == null)) {
            throw failure("PERSON_REBIND_IDENTITY_INVALID");
        }
        LocalDate today = LocalDate.now(clock);
        if (fields.birthDate().isAfter(today)
            || fields.validFrom() != null && fields.validUntil() != null
            && (fields.validFrom().isAfter(fields.validUntil()) || fields.validFrom().isAfter(today)
            || fields.validUntil().isBefore(today))) {
            throw failure("PERSON_REBIND_IDENTITY_INVALID");
        }
    }
    /**
     * 校验认证提供方已启用
     */
    private void requireProviderEnabled(String providerCode) {
        try {
            providers.requireEnabled(providerCode);
        } catch (PersonVerificationException exception) {
            throw new PersonRebindException("PERSON_PROVIDER_UNAVAILABLE", exception);
        }
    }
    /**
     * 启动认证流程并记录尝试
     */
    private void startVerificationAttempt(PersonVerificationStartAttemptCommand command) {
        try {
            attempts.startAttempt(command);
        } catch (PersonVerificationException exception) {
            throw new PersonRebindException("PERSON_PROVIDER_UNAVAILABLE", exception);
        }
    }
    /**
     * 生成脱敏手机号
     */
    private String maskedPhone(long oldUserId) {
        String phone = text(users.selectPhonenumberById(oldUserId));
        if (phone == null || phone.length() < 8) {
            return "****";
        }
        return phone.substring(0, 3) + "*".repeat(phone.length() - 7)
            + phone.substring(phone.length() - 4);
    }
    /**
     * 计算身份字段指纹
     */
    private String fingerprint(PersonIdentityFields fields, int snapshotVersion) {
        String canonical = fields.identityKey() + "\n" + snapshotVersion + "\nREBIND";
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
     * 处理same。
     */
    private boolean same(PersonIdentityFields first, PersonIdentityFields second) {
        return first.equals(second);
    }
    /**
     * 返回不可用状态
     */
    private PersonRebindMatchVo unavailable() {
        return new PersonRebindMatchVo("NOT_AVAILABLE", null);
    }
    /**
     * 转换为大写文本
     */
    private String upper(String value) {
        String normalized = text(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
    /**
     * 规范化文本内容
     */
    private String text(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }
    /**
     * 解析整数值
     */
    private int intValue(Integer value) {
        return value == null ? 0 : value;
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
    private PersonRebindException failure(String category) {
        return new PersonRebindException(category);
    }
    /**
     * 构造业务失败异常
     */
    private PersonRebindException failure(String category, Throwable cause) {
        return new PersonRebindException(category, cause);
    }
}
