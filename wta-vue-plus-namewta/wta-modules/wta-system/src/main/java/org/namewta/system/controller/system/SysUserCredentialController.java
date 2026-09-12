package org.namewta.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.system.domain.bo.password.ResetPasswordCandidateBo;
import org.namewta.system.domain.vo.password.ResetPasswordCandidateVo;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.ISysUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户凭据候选接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/user")
public class SysUserCredentialController {

    private final ISysUserService userService;
    private final PasswordPolicyService passwordPolicyService;

    /**
     * 生成可编辑的永久密码重置候选，不修改用户。
     *
     * @param body     目标用户
     * @param response HTTP 响应
     * @return 合规密码候选
     */
    @SaCheckPermission("system:user:resetPwd")
    @Log(title = "用户密码重置候选", businessType = BusinessType.OTHER,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/resetPwd/candidate")
    public R<ResetPasswordCandidateVo> candidate(@Validated @RequestBody ResetPasswordCandidateBo body,
                                                  HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        userService.checkUserAllowed(body.userId());
        userService.checkUserDataScope(body.userId());
        String password = passwordPolicyService.generateDefaultPassword();
        return R.ok(new ResetPasswordCandidateVo(password));
    }
}
