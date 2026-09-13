package org.namewta.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.core.validate.AddGroup;
import org.namewta.common.core.validate.EditGroup;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.redis.annotation.RepeatSubmit;
import org.namewta.common.web.core.BaseController;
import org.namewta.system.domain.bo.SysClientBo;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.service.ISysClientService;
import org.namewta.system.sso.SsoAppRegistration;
import org.namewta.system.sso.SsoSecretHasher;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 独立 SSO 管理：创建应用与配置交付（数据仍落 sys_client）。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/ssoApp")
public class SysSsoAppController extends BaseController {

    private final ISysClientService sysClientService;

    /**
     * 分页查询 SSO 应用目录。
     *
     * @param bo        条件
     * @param pageQuery 分页
     * @return 分页结果
     */
    @SaCheckPermission("system:ssoApp:list")
    @GetMapping("/list")
    public R<PageResult<SysClientVo>> list(SysClientBo bo, PageQuery pageQuery) {
        return R.ok(sysClientService.queryPageList(bo, pageQuery));
    }

    /**
     * 查询 SSO 应用详情（含是否已配置密钥，不含明文）。
     *
     * @param id 主键
     * @return 详情
     */
    @SaCheckPermission("system:ssoApp:query")
    @GetMapping("/{id}")
    public R<SysClientVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(sysClientService.queryById(id));
    }

    /**
     * 创建 SSO 应用并交付 clientId / 一次性密钥。
     *
     * @param bo 应用
     * @return 含一次性明文的视图
     */
    @SaCheckPermission("system:ssoApp:add")
    @Log(title = "SSO管理创建应用", businessType = BusinessType.INSERT,
        excludeParamNames = {"ssoSecret", "ssoSecretOnce", "clientSecret"}, isSaveResponseData = false)
    @RepeatSubmit()
    @PostMapping()
    public R<SysClientVo> add(@Validated(AddGroup.class) @RequestBody SysClientBo bo) {
        SsoAppRegistration.prepare(bo);
        if (StringUtils.isBlank(bo.getSsoSecret())) {
            bo.setSsoSecret(SsoSecretHasher.generatePlaintext());
        }
        if (!sysClientService.checkClickKeyUnique(bo)) {
            return R.fail("新增 SSO 应用'" + bo.getClientKey() + "'失败，客户端key已存在");
        }
        if (!sysClientService.insertByBo(bo)) {
            return R.fail("新增 SSO 应用失败");
        }
        SysClientVo vo = sysClientService.queryById(bo.getId());
        vo.setSsoSecretOnce(bo.getSsoSecretOnce());
        return R.ok(vo);
    }

    /**
     * 修改 SSO 应用登记（精确回调等）。
     *
     * @param bo 应用
     * @return 视图
     */
    @SaCheckPermission("system:ssoApp:edit")
    @Log(title = "SSO管理修改应用", businessType = BusinessType.UPDATE,
        excludeParamNames = {"ssoSecret", "ssoSecretOnce", "clientSecret"}, isSaveResponseData = false)
    @RepeatSubmit()
    @PostMapping("/update")
    public R<SysClientVo> edit(@Validated(EditGroup.class) @RequestBody SysClientBo bo) {
        SsoAppRegistration.prepare(bo);
        if (!sysClientService.checkClickKeyUnique(bo)) {
            return R.fail("修改 SSO 应用'" + bo.getClientKey() + "'失败，客户端key已存在");
        }
        if (!sysClientService.updateByBo(bo)) {
            return R.fail("修改 SSO 应用失败");
        }
        SysClientVo vo = sysClientService.queryById(bo.getId());
        vo.setSsoSecretOnce(bo.getSsoSecretOnce());
        return R.ok(vo);
    }

    /**
     * 轮换 SSO 密钥，明文只返回一次。
     *
     * @param bo 仅主键
     * @return 含一次性明文
     */
    @SaCheckPermission("system:ssoApp:edit")
    @Log(title = "SSO管理密钥轮换", businessType = BusinessType.UPDATE, isSaveResponseData = false)
    @RepeatSubmit()
    @PostMapping("/rotateSecret")
    public R<SysClientVo> rotateSecret(@RequestBody SysClientBo bo) {
        if (bo.getId() == null) {
            return R.fail("主键不能为空");
        }
        return R.ok(sysClientService.rotateSsoSecret(bo.getId()));
    }
}
