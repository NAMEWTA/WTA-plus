package org.namewta.profile.person.controller.self;

import jakarta.validation.Valid;

import org.namewta.profile.person.domain.vo.PersonApplicationVo;
import org.namewta.profile.person.domain.bo.PersonApplicationSaveBo;
import org.namewta.profile.person.domain.bo.PersonApplicationSubmitBo;
import org.namewta.profile.person.usecase.PersonApplicationUseCase;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.satoken.utils.LoginHelper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PersonApplicationController HTTP 接口，负责参数校验和响应包装。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/person/application")
public class PersonApplicationController {

    private final PersonApplicationUseCase service;

    /**
     * 处理 current HTTP 请求。
     */
    @GetMapping
    @SaCheckPermission("profile:person:apply")
    public R<PersonApplicationVo> current() {
        return R.ok(service.current(LoginHelper.getUserId()));
    }

    /**
     * 处理 save HTTP 请求。
     */
    @PostMapping
    @SaCheckPermission("profile:person:apply")
    @Log(title = "保存个人实名认证申请", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<PersonApplicationVo> save(@Valid @RequestBody PersonApplicationSaveBo command) {
        return R.ok(service.save(LoginHelper.getUserId(), command));
    }

    /**
     * 处理 submit HTTP 请求。
     */
    @PostMapping("/submit")
    @SaCheckPermission("profile:person:apply")
    @Log(title = "提交个人实名认证申请", businessType = BusinessType.OTHER,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<PersonApplicationVo> submit(@Valid @RequestBody PersonApplicationSubmitBo command) {
        return R.ok(service.submit(LoginHelper.getUserId(), command.expectedVersion()));
    }
}
