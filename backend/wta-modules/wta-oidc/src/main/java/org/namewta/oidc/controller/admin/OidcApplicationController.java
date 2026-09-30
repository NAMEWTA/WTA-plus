package org.namewta.oidc.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.domain.*;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.oidc.domain.bo.*;
import org.namewta.oidc.domain.vo.*;
import org.namewta.oidc.usecase.OidcApplicationUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 第三方应用管理；每项变更独立执行后端权限检查。 */
@RestController
@RequestMapping("/oidc/admin")
@RequiredArgsConstructor
public class OidcApplicationController {
    private final OidcApplicationUseCase useCase;

    @GetMapping("/applications")
    @SaCheckPermission("oidc:application:list")
    /** 按名称分页查询管理者有权查看的应用。 */
    public PageResult<OidcApplicationVo> list(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return useCase.page(name, pageNum, pageSize);
    }

    @GetMapping("/applications/{id}")
    @SaCheckPermission("oidc:application:query")
    /** 返回应用非机密详情。 */
    public R<OidcApplicationVo> detail(@PathVariable Long id) {
        return R.ok(useCase.detail(id));
    }

    @PostMapping("/applications/create")
    @SaCheckPermission("oidc:application:add")
    @Log(
            title = "创建OIDC应用",
            businessType = BusinessType.INSERT,
            isSaveRequestData = false,
            isSaveResponseData = false)
    /** 创建第三方应用；响应中的客户端明文密钥仅本次可见，禁止记录正文。 */
    public R<OidcApplicationSecretVo> create(@Valid @RequestBody OidcApplicationBo bo) {
        return R.ok(useCase.create(bo));
    }

    @PostMapping("/applications/update")
    @SaCheckPermission("oidc:application:edit")
    @Log(
            title = "修改OIDC应用",
            businessType = BusinessType.UPDATE,
            isSaveRequestData = false,
            isSaveResponseData = false)
    /** 校验输入和乐观版本后更新允许编辑的字段。 */
    public R<OidcApplicationVo> update(@Valid @RequestBody OidcApplicationBo bo) {
        return R.ok(useCase.update(bo));
    }

    @PostMapping("/applications/{id}/status")
    @SaCheckPermission("oidc:application:edit")
    @Log(
            title = "OIDC应用状态",
            businessType = BusinessType.UPDATE,
            isSaveRequestData = false,
            isSaveResponseData = false)
    /** 更新应用启用状态，停用时撤销既有授权。 */
    public R<OidcApplicationVo> status(@PathVariable Long id, @Valid @RequestBody OidcStatusBo bo) {
        return R.ok(useCase.status(id, bo));
    }

    @PostMapping("/applications/{id}/rotate-secret")
    @SaCheckPermission("oidc:application:rotate")
    @Log(
            title = "重置OIDC应用密钥",
            businessType = BusinessType.UPDATE,
            isSaveRequestData = false,
            isSaveResponseData = false)
    /** 生成新的应用密钥，旧密钥与既有授权立即失效。 */
    public R<OidcApplicationSecretVo> rotate(
            @PathVariable Long id, @Valid @RequestBody OidcVersionBo bo) {
        return R.ok(useCase.rotate(id, bo));
    }

    @PostMapping("/applications/{id}/delete")
    @SaCheckPermission("oidc:application:remove")
    @Log(
            title = "删除OIDC应用",
            businessType = BusinessType.DELETE,
            isSaveRequestData = false,
            isSaveResponseData = false)
    /** 按乐观版本删除应用并终止其既有授权。 */
    public R<Void> delete(@PathVariable Long id, @Valid @RequestBody OidcVersionBo bo) {
        useCase.delete(id, bo);
        return R.ok();
    }

    @GetMapping("/fields")
    @SaCheckPermission("oidc:application:list")
    /** 返回可授权身份字段目录，不返回实际资料。 */
    public R<List<OidcFieldVo>> fields() {
        return R.ok(useCase.fields());
    }

    @GetMapping("/provider")
    @SaCheckPermission("oidc:application:list")
    /** 返回发行方公开接入地址及当前签发就绪状态。 */
    public R<OidcProviderVo> provider() {
        return R.ok(useCase.provider());
    }
}
