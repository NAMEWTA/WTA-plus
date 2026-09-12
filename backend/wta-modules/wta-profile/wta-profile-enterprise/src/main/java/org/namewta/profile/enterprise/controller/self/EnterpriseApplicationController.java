package org.namewta.profile.enterprise.controller.self;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationProbeBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationSaveBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationSubmitBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationProbeVo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationVo;
import org.namewta.profile.enterprise.usecase.EnterpriseApplicationUseCase;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EnterpriseApplicationController HTTP 接口，负责参数校验和响应包装。
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/profile/enterprise/application")
public class EnterpriseApplicationController {

    private final EnterpriseApplicationUseCase service;

    /**
     * 处理 current HTTP 请求。
     */
    @GetMapping
    @SaCheckPermission("profile:enterprise:apply")
    public R<EnterpriseApplicationVo> current() {
        return R.ok(service.current(LoginHelper.getUserId()));
    }

    /**
     * 处理 save HTTP 请求。
     */
    @PostMapping
    @SaCheckPermission("profile:enterprise:apply")
    @Log(title = "保存企业实名认证申请", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<EnterpriseApplicationVo> save(@Valid @RequestBody EnterpriseApplicationSaveBo command) {
        return R.ok(service.save(LoginHelper.getUserId(), command));
    }

    /**
     * 处理 submit HTTP 请求。
     */
    @PostMapping("/submit")
    @SaCheckPermission("profile:enterprise:apply")
    @Log(title = "提交企业实名认证申请", businessType = BusinessType.OTHER,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<EnterpriseApplicationVo> submit(@Valid @RequestBody EnterpriseApplicationSubmitBo command) {
        return R.ok(service.submit(LoginHelper.getUserId(), command.expectedVersion()));
    }

    /**
     * 处理 probe HTTP 请求。
     */
    @PostMapping("/probe")
    @SaCheckPermission("profile:enterprise:apply")
    @Log(title = "探测企业认证状态", businessType = BusinessType.OTHER,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<EnterpriseApplicationProbeVo> probe(@Valid @RequestBody EnterpriseApplicationProbeBo command) {
        return R.ok(service.probe(command));
    }
}
