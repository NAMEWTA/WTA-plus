package org.namewta.profile.enterprise.controller.admin;

import org.namewta.profile.enterprise.usecase.EnterpriseAdminUseCase;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.enterprise.domain.bo.*;
import org.namewta.profile.enterprise.domain.vo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EnterpriseAdminController HTTP 接口，负责参数校验和响应包装。
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/profile/enterprise/archive")
public class EnterpriseAdminController {

    private final EnterpriseAdminUseCase service;

    /**
     * 处理 page HTTP 请求。
     */
    @GetMapping
    @SaCheckPermission("profile:enterprise:query")
    public R<PageResult<EnterpriseProfileSummaryVo>> page(EnterpriseAdminQueryBo query) {
        return R.ok(service.page(query));
    }

    /**
     * 处理 eligibleUsers HTTP 请求。
     */
    @GetMapping("/eligible-users")
    @SaCheckPermission("profile:enterprise:override")
    public R<List<EnterpriseAccountCandidateVo>> eligibleUsers(@RequestParam String keyword) {
        return R.ok(service.eligibleUsers(keyword));
    }

    /**
     * 处理 detail HTTP 请求。
     */
    @GetMapping("/{profileId}")
    @SaCheckPermission("profile:enterprise:query")
    public R<EnterpriseProfileDetailVo> detail(@Positive @PathVariable long profileId) {
        return R.ok(service.detail(profileId));
    }

    /**
     * 处理 reviewContext HTTP 请求。
     */
    @GetMapping("/application/{applicationId}/review-context")
    @SaCheckPermission("profile:enterprise:review")
    public R<EnterpriseReviewContextVo> reviewContext(@Positive @PathVariable long applicationId) {
        return R.ok(service.review(applicationId));
    }

    /**
     * 处理 reviewMaterial HTTP 请求。
     */
    @GetMapping("/application/{applicationId}/material/{materialRefId}/access-url")
    @SaCheckPermission("profile:enterprise:review")
    public R<EnterpriseProfileAccessUrl> reviewMaterial(@Positive @PathVariable long applicationId,
                                                     @Positive @PathVariable long materialRefId) {
        return R.ok(service.reviewMaterial(applicationId, materialRefId));
    }

    /**
     * 处理 material HTTP 请求。
     */
    @GetMapping("/{profileId}/material/{materialRefId}/access-url")
    @SaCheckPermission("profile:enterprise:material")
    public R<EnterpriseProfileAccessUrl> material(@Positive @PathVariable long profileId,
                                               @Positive @PathVariable long materialRefId) {
        return R.ok(service.material(profileId, materialRefId));
    }

    /**
     * 处理 decide HTTP 请求。
     */
    @PostMapping("/application/{applicationId}/decision")
    @SaCheckPermission("profile:enterprise:override")
    @Log(title = "企业档案管理员决定", businessType = BusinessType.OTHER,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<EnterpriseAdminResultVo> decide(@Positive @PathVariable long applicationId,
                                             @Valid @RequestBody EnterpriseAdminDecisionBo command) {
        return R.ok(service.decide(LoginHelper.getUserId(), applicationId, command));
    }

    /**
     * 处理 create HTTP 请求。
     */
    @PostMapping("/admin-create")
    @SaCheckPermission("profile:enterprise:override")
    @Log(title = "管理员直建企业档案", businessType = BusinessType.INSERT,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<EnterpriseAdminResultVo> create(@Valid @RequestBody EnterpriseAdminCreateBo command) {
        return R.ok(service.create(LoginHelper.getUserId(), command));
    }

    /**
     * 处理 revise HTTP 请求。
     */
    @PostMapping("/{profileId}/revision")
    @SaCheckPermission("profile:enterprise:override")
    @Log(title = "管理员修订企业档案", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<EnterpriseAdminResultVo> revise(@Positive @PathVariable long profileId,
                                             @Valid @RequestBody EnterpriseAdminReviseBo command) {
        return R.ok(service.revise(LoginHelper.getUserId(), profileId, command));
    }

    /**
     * 处理 assign HTTP 请求。
     */
    @PostMapping("/{profileId}/assign")
    @SaCheckPermission("profile:enterprise:override")
    @Log(title = "管理员指定企业档案负责人", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<EnterpriseAdminResultVo> assign(@Positive @PathVariable long profileId,
                                             @Valid @RequestBody EnterpriseAdminAssignBo command) {
        return R.ok(service.assign(LoginHelper.getUserId(), profileId, command));
    }

    /**
     * 处理 manageBinding HTTP 请求。
     */
    @PostMapping("/{profileId}/binding")
    @SaCheckPermission("profile:enterprise:manage")
    @Log(title = "企业档案绑定处置", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<EnterpriseAdminResultVo> manageBinding(@Positive @PathVariable long profileId,
                                                    @Valid @RequestBody EnterpriseAdminBindingBo command) {
        return R.ok(service.manageBinding(LoginHelper.getUserId(), profileId, command));
    }

    /**
     * 处理 revoke HTTP 请求。
     */
    @PostMapping("/{profileId}/revoke")
    @SaCheckPermission("profile:enterprise:override")
    @Log(title = "注销企业档案", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<EnterpriseAdminResultVo> revoke(@Positive @PathVariable long profileId,
                                             @Valid @RequestBody EnterpriseAdminRevokeBo command) {
        return R.ok(service.revoke(LoginHelper.getUserId(), profileId, command));
    }
}
