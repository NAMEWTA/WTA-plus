package org.namewta.oidc.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.oidc.domain.bo.OidcKeyBo;
import org.namewta.oidc.domain.bo.OidcServiceBo;
import org.namewta.oidc.domain.vo.OidcKeyVo;
import org.namewta.oidc.domain.vo.OidcLogoutDeliveryVo;
import org.namewta.oidc.domain.vo.OidcServiceVo;
import org.namewta.oidc.usecase.OidcServiceUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 服务、密钥与退出投递管理；所有凭据正文禁止写入操作日志。 */
@RestController
@RequestMapping("/oidc/admin")
@RequiredArgsConstructor
public class OidcServiceController {
    private final OidcServiceUseCase useCase;

    /** 返回生效配置与待维护目标。 */
    @GetMapping("/service")
    @SaCheckPermission("oidc:service:query")
    public R<OidcServiceVo> detail() {
        return R.ok(useCase.detail());
    }

    /** 按版本保存配置，结构变更明确要求停用与重启。 */
    @PostMapping("/service")
    @SaCheckPermission("oidc:service:edit")
    @Log(
            title = "认证服务配置",
            businessType = BusinessType.UPDATE,
            isSaveRequestData = false,
            isSaveResponseData = false)
    public R<OidcServiceVo> save(@Valid @RequestBody OidcServiceBo bo) {
        return R.ok(useCase.save(bo));
    }

    /** 显式终结中央会话并持久预约全局退出。 */
    @PostMapping("/service/prepare-restart")
    @SaCheckPermission("oidc:service:edit")
    @Log(
            title = "认证服务维护退出",
            businessType = BusinessType.UPDATE,
            isSaveRequestData = false,
            isSaveResponseData = false)
    public R<Void> prepareRestart() {
        useCase.prepareRestart();
        return R.ok();
    }

    /** 密钥列表不返回任何密文或明文。 */
    @GetMapping("/keys")
    @SaCheckPermission("oidc:service:query")
    public R<List<OidcKeyVo>> keys() {
        return R.ok(useCase.keys());
    }

    /** 生成并激活新版本，保留旧版本兼容已签凭据。 */
    @PostMapping("/keys/generate")
    @SaCheckPermission("oidc:key:manage")
    @Log(
            title = "生成认证密钥",
            businessType = BusinessType.INSERT,
            isSaveRequestData = false,
            isSaveResponseData = false)
    public R<Void> generate(@Valid @RequestBody OidcKeyBo bo) {
        useCase.generate(bo);
        return R.ok();
    }

    /** 导入私钥或状态键，禁止日志采集请求内容。 */
    @PostMapping("/keys/import")
    @SaCheckPermission("oidc:key:manage")
    @Log(
            title = "导入认证密钥",
            businessType = BusinessType.INSERT,
            isSaveRequestData = false,
            isSaveResponseData = false)
    public R<Void> importKey(@Valid @RequestBody OidcKeyBo bo) {
        useCase.importKey(bo);
        return R.ok();
    }

    /** 退出投递列表。 */
    @GetMapping("/logout-deliveries")
    @SaCheckPermission("oidc:logout:query")
    public PageResult<OidcLogoutDeliveryVo> deliveries(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return useCase.deliveries(pageNum, pageSize);
    }

    /** 只重试失败任务。 */
    @PostMapping("/logout-deliveries/{id}/retry")
    @SaCheckPermission("oidc:logout:retry")
    @Log(
            title = "重试关联应用退出",
            businessType = BusinessType.UPDATE,
            isSaveRequestData = false,
            isSaveResponseData = false)
    public R<Void> retry(@PathVariable Long id) {
        useCase.retry(id);
        return R.ok();
    }
}
