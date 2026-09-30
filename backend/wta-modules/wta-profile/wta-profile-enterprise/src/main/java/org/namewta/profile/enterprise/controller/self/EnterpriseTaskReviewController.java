package org.namewta.profile.enterprise.controller.self;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTaskDecisionBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseReviewContextVo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseProfileAccessUrl;
import org.namewta.profile.enterprise.usecase.EnterpriseTaskReviewUseCase;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 当前客户端任务参与者的企业认证审核入口。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/profile/enterprise/review/tasks")
@SaCheckPermission("profile:enterprise:task-review")
public class EnterpriseTaskReviewController {
    private final EnterpriseTaskReviewUseCase useCase;

    /** 通过任务编号读取当前或本人已办的提交快照。 */
    @io.swagger.v3.oas.annotations.Operation(operationId = "getEnterpriseTaskReview")
    @GetMapping("/{taskId}")
    public R<EnterpriseReviewContextVo> review(@PathVariable @Positive long taskId) {
        return R.ok(useCase.review(taskId));
    }

    /** 任务与材料引用同时匹配才签发地址。 */
    @io.swagger.v3.oas.annotations.Operation(operationId = "getEnterpriseTaskMaterial")
    @GetMapping("/{taskId}/materials/{materialRefId}/access-url")
    public R<EnterpriseProfileAccessUrl> material(@PathVariable @Positive long taskId,
                                            @PathVariable @Positive long materialRefId) {
        return R.ok(useCase.material(taskId, materialRefId));
    }

    /** 通过或退回当前任务；请求不接受伪造办理人或引擎控制变量。 */
    @io.swagger.v3.oas.annotations.Operation(operationId = "decideEnterpriseReviewTask")
    @PostMapping("/{taskId}/decision")
    @Log(title = "企业认证任务审核", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> decide(@PathVariable @Positive long taskId, @Valid @RequestBody EnterpriseTaskDecisionBo command) {
        useCase.decide(LoginHelper.getUserId(), taskId, command);
        return R.ok();
    }
}
