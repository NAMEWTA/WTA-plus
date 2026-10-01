package org.namewta.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.validate.AddGroup;
import org.namewta.common.core.validate.EditGroup;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.redis.annotation.RepeatSubmit;
import org.namewta.system.domain.bo.ExternalAuthRemoveBo;
import org.namewta.system.domain.bo.ExternalAuthLegacyImportBo;
import org.namewta.system.domain.vo.ExternalAuthLegacyImportResultVo;
import org.namewta.system.domain.bo.SysAuthRegistrationBo;
import org.namewta.system.domain.vo.SysAuthRegistrationVo;
import org.namewta.system.service.ISysExternalAuthConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 外部身份接入管理接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/auth/registration")
public class SysAuthRegistrationController {
    private final ISysExternalAuthConfigService service;

    /** 分页查询。 */
    @GetMapping("/list")
    @SaCheckPermission("system:authRegistration:list")
    public R<PageResult<SysAuthRegistrationVo>> list(SysAuthRegistrationBo bo, PageQuery page) {
        service.requireAdminClient();
        return R.ok(service.registrations(bo, page));
    }

    /** 跨 App 目录仅供 Admin 接入配置；不改变普通 RBAC 的 Client 隔离。 */
    @GetMapping("/client-options")
    @SaCheckPermission("system:authRegistration:list")
    public R<java.util.List<org.namewta.system.domain.vo.ExternalAuthClientOptionVo>> clientOptions(
        @RequestParam(required = false) String keyword, @RequestParam(required = false) java.util.List<String> clientIds) {
        service.requireAdminClient();
        return R.ok(service.clientOptions(keyword, clientIds));
    }

    /** 仅具有接入管理权限时也能选择身份源，不要求完整身份源配置读取权限。 */
    @GetMapping("/provider-options")
    @SaCheckPermission("system:authRegistration:list")
    public R<java.util.List<org.namewta.system.domain.vo.ExternalAuthProviderOptionVo>> providerOptions(
        @RequestParam(required = false) String keyword, @RequestParam(required = false) @Positive Long selectedId) {
        service.requireAdminClient();
        return R.ok(service.providerOptions(keyword, selectedId));
    }

    /** 登记在对方控制台的公开参数；不包含密钥。 */
    @GetMapping("/{id}/connection-info")
    @SaCheckPermission("system:authRegistration:list")
    public R<org.namewta.system.domain.vo.ExternalAuthConnectionInfoVo> connectionInfo(@PathVariable @Positive long id) {
        service.requireAdminClient();
        return R.ok(service.connectionInfo(id));
    }

    /** 详情不包含密钥及密文。 */
    @GetMapping("/{id}")
    @SaCheckPermission("system:authRegistration:list")
    public R<SysAuthRegistrationVo> detail(@PathVariable @Positive long id) {
        service.requireAdminClient();
        return R.ok(service.registration(id));
    }

    /** 新增配置。 */
    @PostMapping("/add")
    @SaCheckPermission("system:authRegistration:add")
    @RepeatSubmit
    @Log(title = "外部身份接入", businessType = BusinessType.INSERT, isSaveRequestData = false, isSaveResponseData = false)
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SysAuthRegistrationBo bo) {
        service.requireAdminClient();
        return R.ok(service.saveRegistration(bo, true));
    }

    /** 按请求版本编辑配置。 */
    @PostMapping("/edit")
    @SaCheckPermission("system:authRegistration:edit")
    @RepeatSubmit
    @Log(title = "外部身份接入", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public R<Long> edit(@Validated(EditGroup.class) @RequestBody SysAuthRegistrationBo bo) {
        service.requireAdminClient();
        return R.ok(service.saveRegistration(bo, false));
    }

    /** 按请求版本删除配置。 */
    @PostMapping("/remove")
    @SaCheckPermission("system:authRegistration:remove")
    @Log(title = "外部身份接入", businessType = BusinessType.DELETE, isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> remove(@Valid @RequestBody ExternalAuthRemoveBo bo) {
        service.requireAdminClient();
        service.removeRegistration(bo);
        return R.ok();
    }

    /** 一次性预览或导入旧 justauth.type，默认只预览且从不回显凭据。 */
    @PostMapping("/importLegacy")
    @SaCheckPermission({"system:authProvider:add", "system:authRegistration:add"})
    @RepeatSubmit
    @Log(title = "旧外部身份配置导入", businessType = BusinessType.INSERT, isSaveRequestData = false, isSaveResponseData = false)
    public R<ExternalAuthLegacyImportResultVo> importLegacy(@Valid @RequestBody ExternalAuthLegacyImportBo bo) {
        service.requireAdminClient();
        return R.ok(service.importLegacy(bo));
    }

    /** 刷新共享缓存，由下一请求从数据库重建。 */
    @PostMapping("/refresh")
    @SaCheckPermission("system:authRegistration:edit")
    @Log(title = "外部身份接入缓存刷新", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> refresh() {
        service.requireAdminClient();
        service.refresh();
        return R.ok();
    }
}
