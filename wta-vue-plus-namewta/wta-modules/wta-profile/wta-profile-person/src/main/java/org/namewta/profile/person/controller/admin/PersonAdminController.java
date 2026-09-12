package org.namewta.profile.person.controller.admin;

import jakarta.validation.Valid;

import org.namewta.profile.person.usecase.PersonAdminUseCase;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.person.domain.bo.*;
import org.namewta.profile.person.domain.vo.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PersonAdminController HTTP 接口，负责参数校验和响应包装。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/person/archive")
public class PersonAdminController {

    private final PersonAdminUseCase service;

    /**
     * 处理 page HTTP 请求。
     */
    @GetMapping
    @SaCheckPermission("profile:person:query")
    public R<PageResult<PersonProfileSummaryVo>> page(PersonAdminQueryBo query) {
        return R.ok(service.page(query));
    }

    /**
     * 处理 eligibleUsers HTTP 请求。
     */
    @GetMapping("/eligible-users")
    @SaCheckPermission("profile:person:override")
    public R<List<PersonAccountCandidateVo>> eligibleUsers(@RequestParam String keyword) {
        return R.ok(service.eligibleUsers(keyword));
    }

    /**
     * 处理 detail HTTP 请求。
     */
    @GetMapping("/{profileId}")
    @SaCheckPermission("profile:person:query")
    public R<PersonProfileDetailVo> detail(@PathVariable long profileId) {
        return R.ok(service.detail(profileId));
    }

    /**
     * 处理 reviewContext HTTP 请求。
     */
    @GetMapping("/application/{applicationId}/review-context")
    @SaCheckPermission("profile:person:review")
    public R<PersonReviewContextVo> reviewContext(@PathVariable long applicationId) {
        return R.ok(service.review(applicationId));
    }

    /**
     * 处理 reviewMaterial HTTP 请求。
     */
    @GetMapping("/application/{applicationId}/material/{materialRefId}/access-url")
    @SaCheckPermission("profile:person:review")
    public R<PersonProfileAccessUrl> reviewMaterial(@PathVariable long applicationId,
                                                     @PathVariable long materialRefId) {
        return R.ok(service.reviewMaterial(applicationId, materialRefId));
    }

    /**
     * 处理 material HTTP 请求。
     */
    @GetMapping("/{profileId}/material/{materialRefId}/access-url")
    @SaCheckPermission("profile:person:material")
    public R<PersonProfileAccessUrl> material(@PathVariable long profileId, @PathVariable long materialRefId) {
        return R.ok(service.material(profileId, materialRefId));
    }

    /**
     * 处理 decide HTTP 请求。
     */
    @PostMapping("/application/{applicationId}/decision")
    @SaCheckPermission("profile:person:override")
    @Log(title = "个人档案管理员决定", businessType = BusinessType.OTHER,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<PersonAdminResultVo> decide(@PathVariable long applicationId, @Valid @RequestBody PersonAdminDecisionBo command) {
        return R.ok(service.decide(LoginHelper.getUserId(), applicationId, command));
    }

    /**
     * 处理 create HTTP 请求。
     */
    @PostMapping("/admin-create")
    @SaCheckPermission("profile:person:override")
    @Log(title = "管理员直建个人档案", businessType = BusinessType.INSERT,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<PersonAdminResultVo> create(@Valid @RequestBody PersonAdminCreateBo command) {
        return R.ok(service.create(LoginHelper.getUserId(), command));
    }

    /**
     * 处理 revise HTTP 请求。
     */
    @PostMapping("/{profileId}/revision")
    @SaCheckPermission("profile:person:override")
    @Log(title = "管理员修订个人档案", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<PersonAdminResultVo> revise(@PathVariable long profileId, @Valid @RequestBody PersonAdminReviseBo command) {
        return R.ok(service.revise(LoginHelper.getUserId(), profileId, command));
    }

    /**
     * 处理 assign HTTP 请求。
     */
    @PostMapping("/{profileId}/assign")
    @SaCheckPermission("profile:person:override")
    @Log(title = "管理员指定个人档案账户", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<PersonAdminResultVo> assign(@PathVariable long profileId, @Valid @RequestBody PersonAdminAssignBo command) {
        return R.ok(service.assign(LoginHelper.getUserId(), profileId, command));
    }

    /**
     * 处理 manageBinding HTTP 请求。
     */
    @PostMapping("/{profileId}/binding")
    @SaCheckPermission("profile:person:manage")
    @Log(title = "个人档案绑定处置", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<PersonAdminResultVo> manageBinding(@PathVariable long profileId, @Valid @RequestBody PersonAdminBindingBo command) {
        return R.ok(service.manageBinding(LoginHelper.getUserId(), profileId, command));
    }

    /**
     * 处理 revoke HTTP 请求。
     */
    @PostMapping("/{profileId}/revoke")
    @SaCheckPermission("profile:person:override")
    @Log(title = "注销个人档案", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<PersonAdminResultVo> revoke(@PathVariable long profileId, @Valid @RequestBody PersonAdminRevokeBo command) {
        return R.ok(service.revoke(LoginHelper.getUserId(), profileId, command));
    }
}
