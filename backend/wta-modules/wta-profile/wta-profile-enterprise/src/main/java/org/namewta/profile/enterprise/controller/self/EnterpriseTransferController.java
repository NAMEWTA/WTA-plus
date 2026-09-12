package org.namewta.profile.enterprise.controller.self;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferConfirmBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferSendBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseTransferVo;
import org.namewta.profile.enterprise.usecase.EnterpriseTransferUseCase;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EnterpriseTransferController HTTP 接口，负责参数校验和响应包装。
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/profile/enterprise/transfer")
public class EnterpriseTransferController {

    private final EnterpriseTransferUseCase service;

    /**
     * 处理 send HTTP 请求。
     */
    @PostMapping("/send")
    @SaCheckPermission("profile:enterprise:apply")
    @Log(title = "发送企业负责人转移验证码", businessType = BusinessType.OTHER,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<EnterpriseTransferVo> send(@Valid @RequestBody EnterpriseTransferSendBo command) {
        return R.ok(service.send(LoginHelper.getUserId(), command));
    }

    /**
     * 处理 confirm HTTP 请求。
     */
    @PostMapping("/confirm")
    @SaCheckPermission("profile:enterprise:apply")
    @Log(title = "确认企业负责人转移", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<EnterpriseTransferVo> confirm(@Valid @RequestBody EnterpriseTransferConfirmBo command) {
        return R.ok(service.confirm(LoginHelper.getUserId(), command));
    }

    /**
     * 处理 unbind HTTP 请求。
     */
    @PostMapping("/unbind")
    @SaCheckPermission("profile:enterprise:apply")
    @Log(title = "企业负责人自行解绑", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<EnterpriseTransferVo> unbind() {
        return R.ok(service.unbind(LoginHelper.getUserId()));
    }
}
