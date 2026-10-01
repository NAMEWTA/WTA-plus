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
import org.namewta.system.domain.bo.SysAuthProviderBo;
import org.namewta.system.domain.vo.SysAuthProviderVo;
import org.namewta.system.service.ISysExternalAuthConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 外部身份源管理接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/auth/provider")
public class SysAuthProviderController {
    private final ISysExternalAuthConfigService service;

    /** 分页查询。 */
    @GetMapping("/list")
    @SaCheckPermission("system:authProvider:list")
    public R<PageResult<SysAuthProviderVo>> list(SysAuthProviderBo bo, PageQuery page) {
        return R.ok(service.providers(bo, page));
    }

    /** 详情不包含密钥及密文。 */
    @GetMapping("/{id}")
    @SaCheckPermission("system:authProvider:list")
    public R<SysAuthProviderVo> detail(@PathVariable @Positive long id) {
        return R.ok(service.provider(id));
    }

    /** 新增配置。 */
    @PostMapping("/add")
    @SaCheckPermission("system:authProvider:add")
    @RepeatSubmit
    @Log(title = "外部身份源", businessType = BusinessType.INSERT, isSaveRequestData = false, isSaveResponseData = false)
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SysAuthProviderBo bo) {
        return R.ok(service.saveProvider(bo, true));
    }

    /** 按请求版本编辑配置。 */
    @PostMapping("/edit")
    @SaCheckPermission("system:authProvider:edit")
    @RepeatSubmit
    @Log(title = "外部身份源", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public R<Long> edit(@Validated(EditGroup.class) @RequestBody SysAuthProviderBo bo) {
        return R.ok(service.saveProvider(bo, false));
    }

    /** 按请求版本删除配置。 */
    @PostMapping("/remove")
    @SaCheckPermission("system:authProvider:remove")
    @Log(title = "外部身份源", businessType = BusinessType.DELETE, isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> remove(@Valid @RequestBody ExternalAuthRemoveBo bo) {
        service.removeProvider(bo);
        return R.ok();
    }

    /** 刷新共享缓存，由下一请求从数据库重建。 */
    @PostMapping("/refresh")
    @SaCheckPermission("system:authProvider:edit")
    @Log(title = "外部身份源缓存刷新", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> refresh() {
        service.refresh();
        return R.ok();
    }
}
