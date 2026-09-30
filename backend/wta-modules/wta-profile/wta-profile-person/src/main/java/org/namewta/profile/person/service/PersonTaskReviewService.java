package org.namewta.profile.person.service;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.person.dao.PersonTaskReviewDao;
import org.namewta.profile.person.domain.bo.PersonTaskDecisionBo;
import org.namewta.profile.person.domain.exception.PersonApplicationException;
import org.namewta.profile.person.domain.model.read.PersonTaskReviewRow;
import org.namewta.profile.person.domain.vo.PersonReviewContextVo;
import org.namewta.profile.person.domain.vo.PersonProfileAccessUrl;
import org.namewta.profile.person.port.gateway.PersonTaskReviewGateway;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileTaskMaterialPort;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.workflow.api.domain.WorkflowTaskReviewContext;
import org.springframework.stereotype.Service;
import java.util.Objects;

/** 审核仅作用于任务绑定的提交版本；不能用任务身份读取其他申请。 */
@Service
@RequiredArgsConstructor
public class PersonTaskReviewService {
    private final PersonTaskReviewDao dao;
    private final PersonTaskReviewGateway workflow;
    private final ProfileTaskMaterialPort materials;

    /** 返回已授权的当前或历史审核快照。 */
    public PersonReviewContextVo review(long taskId) {
        var row = snapshot(taskId, false);
        return new PersonReviewContextVo(row.getApplicationId(), row.getApplicantUserId(), row.getStatus(),
            row.getSubmissionSeq(), row.getDecisionVersion(), row.getVersion(), row.getSubmissionId(),
            row.getFieldSnapshotJson(), row.getSubmittedTime(), materials.listForTask(owner(row), taskId));
    }

    /** 根据任务和提交归属签发材料地址。 */
    public PersonProfileAccessUrl material(long taskId, long materialRefId) {
        var row = snapshot(taskId, false);
        var access = materials.accessUrlForTask(owner(row), materialRefId, taskId);
        return access == null ? null : new PersonProfileAccessUrl(
            access.accessType(), access.url(), access.expiresAt(), access.fileName());
    }

    /** 在用例事务内锁定快照并办理，退回原因与流程迁移一起提交。 */
    public void decide(long operatorId, long taskId, PersonTaskDecisionBo command) {
        if (operatorId <= 0 || command == null || command.reason() == null || command.reason().isBlank()
            || command.reason().length() > 500
            || !("APPROVE".equals(command.decision()) || "RETURN".equals(command.decision()))) {
            throw failure("REVIEW_DECISION_INVALID");
        }
        var row = snapshot(taskId, true);
        if (!Objects.equals(row.getSubmissionSeq(), command.snapshotVersion())) {
            throw failure("REVIEW_SNAPSHOT_CONFLICT");
        }
        String reason = command.reason().strip();
        if ("RETURN".equals(command.decision())) {
            if (dao.recordReturnReason(row.getApplicationId(), command.snapshotVersion(), reason, operatorId) != 1) {
                throw failure("REVIEW_SNAPSHOT_CONFLICT");
            }
            workflow.returnToApplicant(taskId, reason);
        } else {
            workflow.approve(taskId, reason);
        }
    }

    /** 从已验证任务解析业务主键，再查固定提交，读操作不回落到最新申请。 */
    private PersonTaskReviewRow snapshot(long taskId, boolean writable) {
        WorkflowTaskReviewContext context = workflow.context(taskId, writable);
        long applicationId;
        try {
            applicationId = Long.parseLong(context.businessId());
        } catch (NumberFormatException exception) {
            throw new PersonApplicationException("PERSON_REVIEW_TASK_MISMATCH", exception);
        }
        if (applicationId <= 0 || context.submissionId() == null || context.snapshotVersion() == null) {
            throw failure("REVIEW_TASK_MISMATCH");
        }
        var row = writable ? dao.lockReview(applicationId, context.submissionId())
            : dao.selectReview(applicationId, context.submissionId());
        if (row == null || !Objects.equals(row.getSubmissionSeq(), context.snapshotVersion())
            || !Objects.equals(row.getSubmissionId(), context.submissionId())) {
            throw failure("REVIEW_SNAPSHOT_CONFLICT");
        }
        return row;
    }

    /** 将任务绑定提交转换为材料所有者。 */
    private MaterialOwnerKey owner(PersonTaskReviewRow row) {
        return new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.SUBMISSION, row.getSubmissionId());
    }

    /** 生成本子域的稳定错误码。 */
    private PersonApplicationException failure(String code) {
        return new PersonApplicationException("PERSON_" + code);
    }
}
