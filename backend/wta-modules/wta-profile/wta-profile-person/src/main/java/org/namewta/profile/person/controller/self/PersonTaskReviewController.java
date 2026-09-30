package org.namewta.profile.person.controller.self;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.person.domain.bo.PersonTaskDecisionBo;
import org.namewta.profile.person.domain.vo.PersonReviewContextVo;
import org.namewta.profile.person.domain.vo.PersonProfileAccessUrl;
import org.namewta.profile.person.usecase.PersonTaskReviewUseCase;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 当前客户端任务参与者的个人认证审核入口。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/profile/person/review/tasks")
@SaCheckPermission("profile:person:task-review")
public class PersonTaskReviewController {
    private final PersonTaskReviewUseCase useCase;

    /** 通过任务编号读取当前或本人已办的提交快照。 */
    @io.swagger.v3.oas.annotations.Operation(operationId = "getPersonTaskReview")
    @GetMapping("/{taskId}")
    public R<PersonReviewContextVo> review(@PathVariable @Positive long taskId) {
        return R.ok(useCase.review(taskId));
    }

    /** 任务与材料引用同时匹配才签发地址。 */
    @io.swagger.v3.oas.annotations.Operation(operationId = "getPersonTaskMaterial")
    @GetMapping("/{taskId}/materials/{materialRefId}/access-url")
    public R<PersonProfileAccessUrl> material(@PathVariable @Positive long taskId,
                                            @PathVariable @Positive long materialRefId) {
        return R.ok(useCase.material(taskId, materialRefId));
    }

    /** 通过或退回当前任务；请求不接受伪造办理人或引擎控制变量。 */
    @io.swagger.v3.oas.annotations.Operation(operationId = "decidePersonReviewTask")
    @PostMapping("/{taskId}/decision")
    @Log(title = "个人认证任务审核", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> decide(@PathVariable @Positive long taskId, @Valid @RequestBody PersonTaskDecisionBo command) {
        useCase.decide(LoginHelper.getUserId(), taskId, command);
        return R.ok();
    }
}
