package org.namewta.profile.person.service;
import org.namewta.profile.person.port.PersonApplicationPublicationPort;
import org.namewta.profile.person.domain.exception.PersonAdminException;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.common.core.domain.PageResult;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialAttachCommand;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.person.domain.bo.*;
import org.namewta.profile.person.domain.vo.*;
import org.namewta.profile.person.domain.model.read.PersonAdminRows.*;
import org.namewta.profile.person.domain.bo.PersonApplicationSaveBo;
import org.namewta.profile.person.domain.application.PersonIdentityFields;
import org.namewta.profile.person.domain.application.PersonPublication;
import org.namewta.profile.person.domain.application.PersonSubmission;
import org.namewta.profile.person.dao.PersonAdminDao;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.system.api.UserService;
import org.namewta.system.api.OssService;
import org.namewta.system.api.domain.UserDTO;
import org.namewta.workflow.api.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.namewta.common.json.utils.JsonUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
/**
 * 创建个人管理业务服务。
 */
@Service
public class PersonAdminService {
    private static final Set<String> DECISIONS = Set.of("APPROVED", "REJECTED");
    private static final Set<String> BINDING_ACTIONS = Set.of("SUSPEND", "RESUME", "UNBIND");
    private final PersonAdminDao dao;
    private final PersonApplicationPublicationPort applications;
    private final ProfileMaterialPort materials;
    private final PersonWorkflowGateway workflow;
    private final UserService users;
    private final Clock clock;
    /** 创建个人管理业务服务，并注入工作流网关。 */
    @Autowired
    public PersonAdminService(PersonAdminDao dao,
                                  PersonApplicationPublicationPort applications, ProfileMaterialPort materials,
                                  PersonWorkflowGateway workflow, UserService users) {
        this(dao, applications, materials, workflow, users, Clock.systemUTC());
    }
    /** 创建使用指定工作流网关的个人管理业务服务。 */
    public PersonAdminService(PersonAdminDao dao, PersonApplicationPublicationPort applications,
                               ProfileMaterialPort materials, PersonWorkflowGateway workflow,
                               UserService users, Clock clock) {
        this.dao = dao;
        this.applications = applications;
        this.materials = materials;
        this.workflow = workflow;
        this.users = users;
        this.clock = clock;
    }
    /** 兼容存量测试适配器使用的工作流服务构造方法。 */
    public PersonAdminService(PersonAdminDao dao, Object jsonMapper,
                           PersonApplicationPublicationPort applications, ProfileMaterialPort materials,
                           WorkflowService workflow, UserService users) {
        this(dao, applications, materials, legacyWorkflow(workflow), users, Clock.systemUTC());
    }
    /** 兼容存量测试适配器使用的可注入时钟构造方法。 */
    public PersonAdminService(PersonAdminDao dao, Object jsonMapper,
                           PersonApplicationPublicationPort applications, ProfileMaterialPort materials,
                           WorkflowService workflow, UserService users, Clock clock) {
        this.dao = dao;
        this.applications = applications;
        this.materials = materials;
        this.workflow = legacyWorkflow(workflow);
        this.users = users;
        this.clock = clock;
    }
    /** 将旧版工作流服务包装为模块端口，保持测试和扩展点兼容。 */
    private static PersonWorkflowGateway legacyWorkflow(WorkflowService workflow) {
        if (workflow == null) {
            return null;
        }
        return new PersonWorkflowGateway() {
            @Override
            public void start(long applicationId, long submissionId, int snapshotVersion) {
                throw new UnsupportedOperationException("旧测试工作流桥不支持启动流程");
            }

            @Override
            public void terminate(String businessId, String reason) {
                workflow.terminateInstance(businessId, reason);
            }
        };
    }
    /**
     * 分页查询档案数据
     */
    public PageResult<PersonProfileSummaryVo> page(PersonAdminQueryBo query) {
        return loadPage(query == null ? new PersonAdminQueryBo(null, null, null, 1, 20) : query);
    }
    /**
     * 查询可用于绑定的账户候选人
     */
    public List<PersonAccountCandidateVo> eligibleUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return users.searchActiveUsers(keyword.strip(), 50).stream()
            .filter(user -> user.getUserId() != null && "0".equals(user.getStatus()))
            .filter(user -> !hasEffectiveBinding(user.getUserId()))
            .limit(20)
            .map(user -> new PersonAccountCandidateVo(user.getUserId(), user.getUserName(), user.getNickName()))
            .toList();
    }
    /**
     * 查询档案详情
     */
    public PersonProfileDetailVo detail(long profileId) {
        PersonProfileDetailVo detail = loadDetail(positive(profileId, "PERSON_PROFILE_INVALID"));
        List<ProfileMaterialPort.MaterialReferenceView> current = detail.versions().isEmpty()
            ? List.of() : materials.list(owner(MaterialOwnerType.VERSION,
            detail.versions().stream().filter(version -> "CURRENT".equals(version.status())).findFirst()
                .orElse(detail.versions().getFirst()).versionId()));
        return new PersonProfileDetailVo(detail.profile(), detail.versions(), detail.bindings(), detail.sources(), detail.audits(), current);
    }
    /**
     * 处理档案审核决定
     */
    public PersonReviewContextVo review(long applicationId) {
        var data = loadReview(positive(applicationId, "PERSON_APPLICATION_INVALID"));
        var owner = owner(MaterialOwnerType.SUBMISSION, data.submissionId());
        return new PersonReviewContextVo(data.applicationId(), data.applicantUserId(), data.status(), data.submissionSeq(),
            data.decisionVersion(), data.version(), data.submissionId(), data.fieldSnapshotJson(),
            data.submittedTime(), materials.list(owner));
    }
    /**
     * 审核材料内容
     */
    public PersonProfileAccessUrl reviewMaterial(long applicationId, long materialRefId) {
        return accessUrl(materials.accessUrl(owner(MaterialOwnerType.SUBMISSION, review(applicationId).submissionId()), materialRefId));
    }
    /**
     * 组装材料数据
     */
    public PersonProfileAccessUrl material(long profileId, long materialRefId) {
        PersonProfileDetailVo detail = detail(profileId);
        PersonProfileVersionVo current = detail.versions().stream().filter(version -> "CURRENT".equals(version.status()))
            .findFirst().orElseThrow(() -> failure("PERSON_PROFILE_VERSION_NOT_FOUND"));
        return accessUrl(materials.accessUrl(owner(MaterialOwnerType.VERSION, current.versionId()), materialRefId));
    }
    /** 将 system OSS 访问合同转换为 Profile HTTP 输出。 */
    private PersonProfileAccessUrl accessUrl(OssService.OssAccessUrl value) {
        return value == null ? null : new PersonProfileAccessUrl(value.accessType(), value.url(), value.expiresAt(), value.fileName());
    }
    /**
     * 提交档案审核决定。
     */

    public PersonAdminResultVo decide(long operatorId, long applicationId, PersonAdminDecisionBo command) {
        String reason = reason(command == null ? null : command.reason());
        String decision = upper(command == null ? null : command.decision());
        if (!DECISIONS.contains(decision)) {
            throw failure("PERSON_ADMIN_DECISION_INVALID");
        }
        if (workflow == null) {
            throw failure("PERSON_ADMIN_WORKFLOW_UNAVAILABLE");
        }
        Instant now = clock.instant();
        var state = beginDecision(positive(applicationId, "PERSON_APPLICATION_INVALID"), decision,
            positive(operatorId, "PERSON_OPERATOR_INVALID"), reason, now);
        try {
            workflow.terminate(Long.toString(applicationId), reason);
        } catch (RuntimeException exception) {
            throw new PersonAdminException("PERSON_ADMIN_WORKFLOW_TERMINATION_FAILED", exception);
        }
        if ("REJECTED".equals(decision)) {
            finalizeRejected(state, operatorId, reason, now);
            return new PersonAdminResultVo("REJECTED", 0L, null, null, state.decisionVersion());
        }
        resumeForApproval(state, operatorId);
        PersonSubmission submission = applications.requireSubmission(applicationId, state.snapshotVersion());
        PersonPublication publication = applications.publishApproved(applicationId, state.snapshotVersion(), now);
        materials.snapshotImmutable(owner(MaterialOwnerType.SUBMISSION, submission.personSubmissionId()),
            owner(MaterialOwnerType.VERSION, publication.personVersionId()));
        finalizeApproved(state, publication.personProfileId(), publication.personVersionId(), operatorId,
            reason, now);
        return new PersonAdminResultVo("APPROVED", publication.personProfileId(), publication.personVersionId(),
            publication.personBindingId(), state.decisionVersion());
    }
    /**
     * 创建业务记录
     */

    public PersonAdminResultVo create(long operatorId, PersonAdminCreateBo command) {
        if (command == null) {
            throw failure("PERSON_ADMIN_CREATE_REQUIRED");
        }
        String reason = reason(command.reason());
        PersonIdentityFields fields = fields(command.identity());
        Instant now = clock.instant();
        var state = beginCreate(fields, positive(operatorId, "PERSON_OPERATOR_INVALID"), reason, now);
        List<PersonAdminMaterialBo> inputs = command.materials() == null ? List.of() : List.copyOf(command.materials());
        for (PersonAdminMaterialBo input : inputs) {
            materials.attach(new MaterialAttachCommand(owner(MaterialOwnerType.SOURCE, state.sourceId()),
                input.ossId(), input.materialNodeId()));
        }
        MaterialOwnerKey source = owner(MaterialOwnerType.SOURCE, state.sourceId());
        materials.validateRequired(source, fields.documentTypeCode(), Set.of("ALWAYS"));
        if (command.bindUserId() != null) {
            requireEligibleUser(command.bindUserId());
        }
        PersonAdminResultVo result = completeCreate(state, command.bindUserId(), operatorId, reason, now);
        materials.snapshotImmutable(source, owner(MaterialOwnerType.VERSION, result.versionId()));
        return result;
    }
    /**
     * 修改已退回申请
     */

    public PersonAdminResultVo revise(long operatorId, long profileId, PersonAdminReviseBo command) {
        if (command == null) {
            throw failure("PERSON_ADMIN_REVISION_REQUIRED");
        }
        String reason = reason(command.reason());
        PersonIdentityFields fields = fields(command.identity());
        Instant now = clock.instant();
        var state = beginRevise(positive(profileId, "PERSON_PROFILE_INVALID"), fields,
            command.expectedVersion(), positive(operatorId, "PERSON_OPERATOR_INVALID"), reason, now);
        MaterialOwnerKey source = owner(MaterialOwnerType.SOURCE, state.sourceId());
        materials.validateRequired(source, fields.documentTypeCode(), Set.of("ALWAYS"));
        PersonAdminResultVo result = completeRevise(state, operatorId, reason, now);
        materials.snapshotImmutable(source, owner(MaterialOwnerType.VERSION, result.versionId()));
        return result;
    }
    /**
     * 管理档案绑定关系
     */

    public PersonAdminResultVo manageBinding(long operatorId, long profileId, PersonAdminBindingBo command) {
        String action = upper(command == null ? null : command.action());
        if (!BINDING_ACTIONS.contains(action)) {
            throw failure("PERSON_BINDING_ACTION_INVALID");
        }
        return manageBindingState(positive(profileId, "PERSON_PROFILE_INVALID"), action,
            command.expectedBindingVersion(), positive(operatorId, "PERSON_OPERATOR_INVALID"),
            reason(command.reason()), clock.instant());
    }
    /**
     * 为目标账户分配档案绑定
     */

    public PersonAdminResultVo assign(long operatorId, long profileId, PersonAdminAssignBo command) {
        if (command == null || command.userId() == null) {
            throw failure("PERSON_BINDING_TARGET_REQUIRED");
        }
        requireEligibleUser(command.userId());
        return assignBinding(positive(profileId, "PERSON_PROFILE_INVALID"), command.userId(),
            positive(operatorId, "PERSON_OPERATOR_INVALID"), reason(command.reason()), clock.instant());
    }
    /**
     * 撤销档案绑定
     */

    public PersonAdminResultVo revoke(long operatorId, long profileId, PersonAdminRevokeBo command) {
        if (command == null) {
            throw failure("PERSON_REVOKE_REQUIRED");
        }
        return revokeProfile(positive(profileId, "PERSON_PROFILE_INVALID"), command.expectedVersion(),
            positive(operatorId, "PERSON_OPERATOR_INVALID"), reason(command.reason()), clock.instant());
    }
    /**
     * 加载档案分页数据
     */
    PageResult<PersonProfileSummaryVo> loadPage(PersonAdminQueryBo query) {
        int page = Math.max(1, query.pageNum());
        int size = query.pageSize() <= 0 ? 20 : Math.min(query.pageSize(), 200);
        String status = upper(query.status());
        if (!status.isEmpty() && !List.of("ACTIVE", "REVOKED").contains(status)) {
            throw failure("PERSON_QUERY_STATUS_INVALID");
        }
        String fullName = text(query.fullName());
        String documentNumber = upper(query.documentNumber());
        long total = dao.countProfiles(fullName, documentNumber, status);
        List<PersonProfileSummaryVo> rows = dao
            .selectProfiles(fullName, documentNumber, status, size, (page - 1) * size)
            .stream().map(this::summary).toList();
        return PageResult.build(rows, total);
    }
    /**
     * 加载档案详情
     */
    PersonProfileDetailVo loadDetail(long profileId) {
        ProfileRow profile = requireProfile(dao.selectProfile(profileId));
        List<PersonProfileVersionVo> versions = dao.selectVersions(profileId).stream()
            .map(this::version).toList();
        return new PersonProfileDetailVo(summary(profile), versions,
            dao.selectBindings(profileId).stream().map(this::binding).toList(),
            dao.selectSources(profileId).stream().map(this::source).toList(),
            dao.selectAudits(profileId).stream().map(this::audit).toList(), List.of());
    }
    /**
     * 加载审核所需数据
     */
    ReviewData loadReview(long applicationId) {
        return reviewData(requireReview(dao.selectReview(applicationId)));
    }
    /**
     * 开始处理审核决定
     */
    public DecisionState beginDecision(long applicationId, String decision, long operatorId,
                                String reason, Instant now) {
        ReviewRow row = requireReview(dao.lockWaitingApplication(applicationId));
        int nextDecision = value(row.getDecisionVersion()) + 1;
        changed(dao.markOverridePending(applicationId, decision, reason, value(row.getDecisionVersion()),
            value(row.getVersion()), operatorId, now), "PERSON_ADMIN_DECISION_CONFLICT");
        changed(dao.insertDecision(IdGeneratorUtil.nextLongId(), applicationId, row.getSubmissionId(), nextDecision,
            decision, operatorId, reason, now), "PERSON_ADMIN_DECISION_CONFLICT");
        audit(null, applicationId, null, "ADMIN_DECISION_PENDING", operatorId, "profile:person:override",
            reason, "WAITING", "OVERRIDE_PENDING", now);
        return new DecisionState(applicationId, row.getSubmissionId(), value(row.getSubmissionSeq()), nextDecision);
    }
    /**
     * 恢复申请并进入审批流程
     */
    public void resumeForApproval(DecisionState state, long operatorId) {
        changed(dao.resumeWaiting(state.applicationId(), state.decisionVersion(), operatorId),
            "PERSON_ADMIN_DECISION_CONFLICT");
    }
    /**
     * 处理finalizeapproved。
     */
    public void finalizeApproved(DecisionState state, long profileId, long versionId, long operatorId,
                          String reason, Instant now) {
        changed(dao.markApproved(state.applicationId(), operatorId, reason, now),
            "PERSON_ADMIN_DECISION_CONFLICT");
        changed(dao.finalizeDecision(state.applicationId(), state.decisionVersion(), operatorId, now),
            "PERSON_ADMIN_DECISION_CONFLICT");
        audit(profileId, state.applicationId(), null, "ADMIN_APPROVE", operatorId, "profile:person:override",
            reason, "OVERRIDE_PENDING", "FINISH", now);
    }
    /**
     * 处理finalizerejected。
     */
    void finalizeRejected(DecisionState state, long operatorId, String reason, Instant now) {
        changed(dao.markRejected(state.applicationId(), state.decisionVersion(), operatorId, reason, now),
            "PERSON_ADMIN_DECISION_CONFLICT");
        changed(dao.finalizeDecision(state.applicationId(), state.decisionVersion(), operatorId, now),
            "PERSON_ADMIN_DECISION_CONFLICT");
        audit(null, state.applicationId(), null, "ADMIN_REJECT", operatorId, "profile:person:override",
            reason, "OVERRIDE_PENDING", "INVALID", now);
    }
    /**
     * 开始创建申请
     */
    CreateState beginCreate(PersonIdentityFields fields, long operatorId, String reason, Instant now) {
        long profileId = IdGeneratorUtil.nextLongId();
        long sourceId = IdGeneratorUtil.nextLongId();
        try {
            changed(dao.insertProfile(profileId, fields.fullName(), fields.documentTypeCode(),
                fields.documentNumber(), fields.identityKey(), fields.gender(), fields.birthDate(), fields.validFrom(),
                fields.validUntil(), operatorId, now), "PERSON_ADMIN_CREATE_CONFLICT");
            insertSource(sourceId, profileId, "ADMIN_CREATE", fields, operatorId, reason, now);
        } catch (DuplicateKeyException exception) {
            throw new PersonAdminException("PERSON_ADMIN_IDENTITY_CONFLICT", exception);
        }
        return new CreateState(profileId, sourceId, fields);
    }
    /**
     * 完成申请创建流程
     */
    PersonAdminResultVo completeCreate(CreateState state, Long bindUserId, long operatorId, String reason,
                                       Instant now) {
        long versionId = IdGeneratorUtil.nextLongId();
        try {
            insertVersion(versionId, state.profileId(), 1, "ADMIN_CREATE", state.sourceId(), state.fields(),
                operatorId, now);
            changed(dao.updateProfileVersion(state.profileId(), versionId, state.fields().fullName(),
                state.fields().documentTypeCode(), state.fields().documentNumber(), state.fields().identityKey(),
                state.fields().gender(), state.fields().birthDate(), state.fields().validFrom(),
                state.fields().validUntil(), 0, operatorId, now), "PERSON_ADMIN_CREATE_CONFLICT");
            Long bindingId = bindUserId == null ? null
                : insertBinding(state.profileId(), bindUserId, "ADMIN_CREATE", state.sourceId(), operatorId,
                    reason, now);
            audit(state.profileId(), null, bindingId, "ADMIN_CREATE", operatorId, "profile:person:override",
                reason, null, "ACTIVE", now);
            return new PersonAdminResultVo("ACTIVE", state.profileId(), versionId, bindingId, 1);
        } catch (DuplicateKeyException exception) {
            throw new PersonAdminException("PERSON_ADMIN_CREATE_CONFLICT", exception);
        }
    }
    /**
     * 开始修改已退回申请
     */
    ReviseState beginRevise(long profileId, PersonIdentityFields fields, int expectedVersion,
                            long operatorId, String reason, Instant now) {
        ProfileRow profile = requireWritable(dao.lockProfile(profileId));
        if (value(profile.getVersion()) != expectedVersion) {
            throw failure("PERSON_PROFILE_VERSION_CONFLICT");
        }
        VersionRow current = dao.lockCurrentVersion(profileId);
        if (current == null) {
            throw failure("PERSON_PROFILE_VERSION_NOT_FOUND");
        }
        long sourceId = IdGeneratorUtil.nextLongId();
        insertSource(sourceId, profileId, "ADMIN_OVERRIDE", fields, operatorId, reason, now);
        dao.cloneVersionMaterials(current.getVersionId(), sourceId, IdGeneratorUtil.nextLongId(), operatorId, now);
        return new ReviseState(profileId, sourceId, value(current.getVersionNo()) + 1,
            expectedVersion, fields);
    }
    /**
     * 完成申请修改流程
     */
    PersonAdminResultVo completeRevise(ReviseState state, long operatorId, String reason, Instant now) {
        VersionRow current = dao.lockCurrentVersion(state.profileId());
        if (current == null || value(current.getVersionNo()) + 1 != state.nextVersionNo()) {
            throw failure("PERSON_PROFILE_VERSION_CONFLICT");
        }
        long versionId = IdGeneratorUtil.nextLongId();
        try {
            changed(dao.supersedeVersion(current.getVersionId(), operatorId, now),
                "PERSON_PROFILE_VERSION_CONFLICT");
            insertVersion(versionId, state.profileId(), state.nextVersionNo(), "ADMIN_OVERRIDE", state.sourceId(),
                state.fields(), operatorId, now);
            changed(dao.updateProfileVersion(state.profileId(), versionId, state.fields().fullName(),
                state.fields().documentTypeCode(), state.fields().documentNumber(), state.fields().identityKey(),
                state.fields().gender(), state.fields().birthDate(), state.fields().validFrom(),
                state.fields().validUntil(), state.profileVersion(), operatorId, now),
                "PERSON_PROFILE_VERSION_CONFLICT");
        } catch (DuplicateKeyException exception) {
            throw new PersonAdminException("PERSON_ADMIN_IDENTITY_CONFLICT", exception);
        }
        audit(state.profileId(), null, null, "ADMIN_OVERRIDE", operatorId, "profile:person:override",
            reason, "ACTIVE", "ACTIVE", now);
        return new PersonAdminResultVo("ACTIVE", state.profileId(), versionId, null, state.profileVersion() + 1);
    }
    /**
     * 变更绑定状态
     */
    PersonAdminResultVo manageBindingState(long profileId, String action, int expectedBindingVersion,
                                           long operatorId, String reason, Instant now) {
        requireWritable(dao.lockProfile(profileId));
        BindingRow binding = dao.lockEffectiveBinding(profileId);
        if (binding == null || value(binding.getBindingVersion()) != expectedBindingVersion) {
            throw failure("PERSON_BINDING_VERSION_CONFLICT");
        }
        String source = switch (action) {
            case "SUSPEND" -> "ACTIVE";
            case "RESUME" -> "SUSPENDED";
            case "UNBIND" -> binding.getStatus();
            default -> throw failure("PERSON_BINDING_ACTION_INVALID");
        };
        String target = switch (action) {
            case "SUSPEND" -> "SUSPENDED";
            case "RESUME" -> "ACTIVE";
            default -> "UNBOUND";
        };
        if (!source.equals(binding.getStatus())) {
            throw failure("PERSON_BINDING_STATE_CONFLICT");
        }
        int nextVersion = expectedBindingVersion + 1;
        changed(dao.updateBinding(binding.getBindingId(), source, target, expectedBindingVersion, operatorId, now),
            "PERSON_BINDING_VERSION_CONFLICT");
        event(binding.getBindingId(), profileId, binding.getUserId(), target, nextVersion, null, reason, operatorId,
            now);
        audit(profileId, null, binding.getBindingId(), "BINDING_" + action, operatorId,
            "profile:person:manage", reason, source, target, now);
        return new PersonAdminResultVo(target, profileId, null, binding.getBindingId(), nextVersion);
    }
    /**
     * 创建账户绑定关系
     */
    PersonAdminResultVo assignBinding(long profileId, long userId, long operatorId, String reason, Instant now) {
        requireWritable(dao.lockProfile(profileId));
        if (dao.lockEffectiveBinding(profileId) != null || dao.countEffectiveBindingByUser(userId) != 0) {
            throw failure("PERSON_BINDING_TARGET_INELIGIBLE");
        }
        try {
            long bindingId = insertBinding(profileId, userId, "ADMIN_OVERRIDE", null, operatorId, reason, now);
            audit(profileId, null, bindingId, "BINDING_ASSIGN", operatorId, "profile:person:override",
                reason, null, "ACTIVE", now);
            return new PersonAdminResultVo("ACTIVE", profileId, null, bindingId, 1);
        } catch (DuplicateKeyException exception) {
            throw new PersonAdminException("PERSON_BINDING_TARGET_INELIGIBLE", exception);
        }
    }
    /**
     * 撤销档案
     */
    PersonAdminResultVo revokeProfile(long profileId, int expectedVersion, long operatorId, String reason,
                                      Instant now) {
        ProfileRow profile = requireWritable(dao.lockProfile(profileId));
        if (value(profile.getVersion()) != expectedVersion) {
            throw failure("PERSON_PROFILE_VERSION_CONFLICT");
        }
        BindingRow binding = dao.lockEffectiveBinding(profileId);
        changed(dao.revokeProfile(profileId, expectedVersion, reason, operatorId, now),
            "PERSON_PROFILE_VERSION_CONFLICT");
        Long bindingId = null;
        if (binding != null) {
            bindingId = binding.getBindingId();
            int next = value(binding.getBindingVersion()) + 1;
            changed(dao.updateBinding(bindingId, binding.getStatus(), "UNBOUND",
                value(binding.getBindingVersion()), operatorId, now), "PERSON_BINDING_VERSION_CONFLICT");
            event(bindingId, profileId, binding.getUserId(), "UNBOUND", next, null, reason, operatorId, now);
        }
        audit(profileId, null, bindingId, "REVOKE", operatorId, "profile:person:override", reason,
            "ACTIVE", "REVOKED", now);
        return new PersonAdminResultVo("REVOKED", profileId, profile.getCurrentVersionId(), bindingId,
            expectedVersion + 1);
    }
    /**
     * 判断用户是否存在生效绑定
     */
    public boolean hasEffectiveBinding(long userId) {
        return dao.countEffectiveBindingByUser(userId) != 0;
    }
    /**
     * 新增材料来源记录
     */
    private void insertSource(long sourceId, long profileId, String sourceType, PersonIdentityFields fields,
                              long operatorId, String reason, Instant now) {
        changed(dao.insertSource(sourceId, profileId, sourceType, operatorId, reason, fields.fullName(),
            fields.documentTypeCode(), fields.documentNumber(), fields.identityKey(), fields.gender(),
            fields.birthDate(), fields.validFrom(), fields.validUntil(), JsonUtils.toJsonString(fields), now),
            "PERSON_ADMIN_SOURCE_CONFLICT");
    }
    /**
     * 新增档案版本记录
     */
    private void insertVersion(long versionId, long profileId, int versionNo, String sourceType, long sourceId,
                               PersonIdentityFields fields, long operatorId, Instant now) {
        changed(dao.insertVersion(versionId, profileId, versionNo, sourceType, sourceId, fields.fullName(),
            fields.documentTypeCode(), fields.documentNumber(), fields.identityKey(), fields.gender(),
            fields.birthDate(), fields.validFrom(), fields.validUntil(), operatorId, now),
            "PERSON_PROFILE_VERSION_CONFLICT");
    }
    /**
     * 新增档案绑定记录
     */
    private long insertBinding(long profileId, long userId, String sourceType, Long sourceId, long operatorId,
                               String reason, Instant now) {
        long bindingId = IdGeneratorUtil.nextLongId();
        changed(dao.insertBinding(bindingId, profileId, userId, sourceType, sourceId, operatorId, now),
            "PERSON_BINDING_CONFLICT");
        event(bindingId, profileId, userId, "ACTIVE", 1, sourceId, reason, operatorId, now);
        return bindingId;
    }
    /**
     * 构造流程事件数据
     */
    private void event(long bindingId, long profileId, long userId, String type, int version, Long sourceId,
                       String reason, long operatorId, Instant now) {
        changed(dao.insertBindingEvent(IdGeneratorUtil.nextLongId(), bindingId, profileId, userId, type, version,
            sourceId, reason, operatorId, now), "PERSON_BINDING_EVENT_CONFLICT");
    }
    /**
     * 记录安全审计信息
     */
    private void audit(Long profileId, Long applicationId, Long bindingId, String operation, long operatorId,
                       String capability, String reason, String before, String after, Instant now) {
        changed(dao.insertAudit(IdGeneratorUtil.nextLongId(), profileId, applicationId, bindingId, operation, operatorId,
            capability, reason, before, after, now), "PERSON_AUDIT_CONFLICT");
    }
    /**
     * 组装档案摘要数据
     */
    private PersonProfileSummaryVo summary(ProfileRow row) {
        return new PersonProfileSummaryVo(row.getProfileId(), row.getPreviousProfileId(), row.getFullName(),
            row.getDocumentTypeCode(), row.getDocumentNumber(), row.getGender(), row.getBirthDate(), row.getStatus(),
            row.getBindingUserId(), row.getBindingStatus(), row.getCreateTime());
    }
    /**
     * 查询档案版本信息
     */
    private PersonProfileVersionVo version(VersionRow row) {
        return new PersonProfileVersionVo(row.getVersionId(), value(row.getVersionNo()), row.getSourceType(),
            row.getSourceId(), row.getFullName(), row.getDocumentTypeCode(), row.getDocumentNumber(), row.getGender(),
            row.getBirthDate(), row.getValidFrom(), row.getValidUntil(), row.getStatus(), row.getPublishedTime());
    }
    /**
     * 查询档案绑定信息
     */
    private PersonProfileBindingVo binding(BindingRow row) {
        return new PersonProfileBindingVo(row.getBindingId(), row.getUserId(), row.getStatus(),
            value(row.getBindingVersion()), row.getSourceType(), row.getSourceId(), row.getBoundTime(),
            row.getUnboundTime());
    }
    /**
     * 解析材料来源
     */
    private PersonProfileSourceVo source(SourceRow row) {
        return new PersonProfileSourceVo(row.getSourceId(), row.getSourceType(), row.getOperatorUserId(),
            row.getReason(), row.getFieldSnapshotJson(), row.getOccurredTime());
    }
    /**
     * 记录安全审计信息
     */
    private PersonProfileAuditVo audit(AuditRow row) {
        return new PersonProfileAuditVo(row.getAuditId(), row.getOperationType(), row.getOperatorUserId(),
            row.getCapability(), row.getReason(), row.getBeforeStatus(), row.getAfterStatus(), row.getResult(),
            row.getFailureCategory(), row.getOccurredTime());
    }
    /**
     * 组装审核数据
     */
    private ReviewData reviewData(ReviewRow row) {
        return new ReviewData(row.getApplicationId(), row.getApplicantUserId(), row.getStatus(),
            value(row.getSubmissionSeq()), value(row.getDecisionVersion()), value(row.getVersion()),
            row.getSubmissionId(), row.getFieldSnapshotJson(), row.getSubmittedTime());
    }
    /**
     * 校验并获取档案记录
     */
    private ProfileRow requireProfile(ProfileRow row) {
        if (row == null) {
            throw failure("PERSON_PROFILE_NOT_FOUND");
        }
        return row;
    }
    /**
     * 校验当前数据允许写入
     */
    private ProfileRow requireWritable(ProfileRow row) {
        row = requireProfile(row);
        if ("REVOKED".equals(row.getStatus())) {
            throw failure("PERSON_PROFILE_REVOKED_READ_ONLY");
        }
        return row;
    }
    /**
     * 校验审核请求有效
     */
    private ReviewRow requireReview(ReviewRow row) {
        if (row == null) {
            throw failure("PERSON_REVIEW_CONTEXT_NOT_FOUND");
        }
        return row;
    }
    /**
     * 处理changed。
     */
    private void changed(int count, String category) {
        if (count != 1) {
            throw failure(category);
        }
    }
    /**
     * 解析字段值
     */
    private int value(Integer value) {
        return value == null ? 0 : value;
    }
    /**
     * 规范化文本内容
     */
    private String text(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
    /**
     * 校验账户符合绑定条件
     */
    private void requireEligibleUser(long userId) {
        UserDTO user = users.selectById(userId);
        if (user == null || !"0".equals(user.getStatus()) || hasEffectiveBinding(userId)) {
            throw failure("PERSON_BINDING_TARGET_INELIGIBLE");
        }
    }
    /**
     * 提取并规范化申请身份字段
     */
    private PersonIdentityFields fields(PersonAdminIdentityBo identity) {
        if (identity == null) {
            throw failure("PERSON_IDENTITY_REQUIRED");
        }
        PersonIdentityFields fields = PersonIdentityFields.normalize(new PersonApplicationSaveBo(identity.fullName(),
            identity.documentTypeCode(), identity.documentNumber(), identity.gender(), identity.birthDate(),
            identity.validFrom(), identity.validUntil(), 0));
        LocalDate today = LocalDate.now(clock);
        if (fields.fullName() == null || fields.documentTypeCode() == null || fields.documentNumber() == null
            || fields.gender() == null || fields.birthDate() == null || fields.birthDate().isAfter(today)
            || (fields.validFrom() != null && fields.validUntil() != null
            && fields.validFrom().isAfter(fields.validUntil()))) {
            throw failure("PERSON_IDENTITY_INVALID");
        }
        var rule = applications.findDocumentType(fields.documentTypeCode())
            .orElseThrow(() -> failure("PERSON_DOCUMENT_TYPE_UNAVAILABLE"));
        if (!Pattern.matches(rule.numberPattern(), fields.documentNumber())
            || (rule.validityRequired() && (fields.validFrom() == null || fields.validUntil() == null))
            || (fields.validFrom() != null && fields.validUntil() != null
            && (fields.validFrom().isAfter(today) || fields.validUntil().isBefore(today)))) {
            throw failure("PERSON_IDENTITY_INVALID");
        }
        return fields;
    }
    /**
     * 解析材料所有者
     */
    private MaterialOwnerKey owner(MaterialOwnerType type, long id) {
        return new MaterialOwnerKey(ProfileType.PERSON, type, id);
    }
    /**
     * 校验正数编号
     */
    private long positive(long value, String category) {
        if (value <= 0) {
            throw failure(category);
        }
        return value;
    }
    /**
     * 处理reason。
     */
    private String reason(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 500) {
            throw failure("PERSON_ADMIN_REASON_REQUIRED");
        }
        return value.strip();
    }
    /**
     * 转换为大写文本
     */
    private String upper(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }
    /**
     * 构造业务失败异常
     */
    private PersonAdminException failure(String category) {
        return new PersonAdminException(category);
    }
    /**
     * 承载ReviewData业务规则的领域服务。
     */
    record ReviewData(long applicationId, long applicantUserId, String status, int submissionSeq,
                      int decisionVersion, int version, long submissionId, String fieldSnapshotJson,
                      Instant submittedTime) {
    }
    /**
     * 承载DecisionState业务规则的领域服务。
     */
    public record DecisionState(long applicationId, long submissionId, int snapshotVersion, int decisionVersion) {
    }
    /**
     * 承载CreateState业务规则的领域服务。
     */
    record CreateState(long profileId, long sourceId, PersonIdentityFields fields) {
    }
    /**
     * 承载ReviseState业务规则的领域服务。
     */
    record ReviseState(long profileId, long sourceId, int nextVersionNo, int profileVersion,
                       PersonIdentityFields fields) {
    }
}
