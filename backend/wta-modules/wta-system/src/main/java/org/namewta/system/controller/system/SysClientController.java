package org.namewta.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.validate.AddGroup;
import org.namewta.common.core.validate.EditGroup;
import org.namewta.common.excel.utils.ExcelBuilder;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.redis.annotation.RepeatSubmit;
import org.namewta.common.web.core.BaseController;
import org.namewta.system.domain.bo.SysClientBo;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.service.ISysClientService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户端管理
 *
 * @date 2023-06-18
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/client")
public class SysClientController extends BaseController {

    private final ISysClientService sysClientService;

    /**
     * 分页查询客户端管理列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 客户端分页数据
     */
    @SaCheckPermission("system:client:list")
    @GetMapping("/list")
    public R<PageResult<SysClientVo>> list(SysClientBo bo, PageQuery pageQuery) {
        return R.ok(sysClientService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出客户端管理列表，便于离线审计与配置核查。
     *
     * @param bo       查询条件
     * @param response 响应流
     */
    @SaCheckPermission("system:client:export")
    @Log(title = "客户端管理", businessType = BusinessType.EXPORT, isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/export")
    public void export(SysClientBo bo, HttpServletResponse response) {
        List<SysClientVo> list = sysClientService.queryList(bo);
        ExcelBuilder.of(list, SysClientVo.class).sheetName("客户端管理").toResponse(response);
    }

    /**
     * 获取单个客户端的详细配置信息。
     *
     * @param id 主键
     * @return 客户端详情
     */
    @SaCheckPermission("system:client:query")
    @GetMapping("/{id}")
    public R<SysClientVo> getInfo(@NotNull(message = "主键不能为空")
                                  @PathVariable Long id) {
        return R.ok(sysClientService.queryById(id));
    }

    /**
     * 新增客户端配置，入库前先校验客户端 key 是否唯一。
     *
     * @param bo 客户端信息
     * @return 操作结果
     */
    @SaCheckPermission("system:client:add")
    @Log(title = "客户端管理", businessType = BusinessType.INSERT, excludeParamNames = {"ssoSecret", "ssoSecretOnce", "clientSecret"}, isSaveRequestData = false, isSaveResponseData = false)
    @RepeatSubmit()
    @PostMapping()
    public R<SysClientVo> add(@Validated(AddGroup.class) @RequestBody SysClientBo bo) {
        if (!sysClientService.checkClickKeyUnique(bo)) {
            return R.fail("新增客户端'" + bo.getClientKey() + "'失败，客户端key已存在");
        }
        if (!sysClientService.insertByBo(bo)) {
            return R.fail("新增客户端失败");
        }
        SysClientVo vo = sysClientService.queryById(bo.getId());
        vo.setSsoSecretOnce(bo.getSsoSecretOnce());
        return R.ok(vo);
    }

    /**
     * 修改客户端配置，避免重复占用同一个客户端 key。
     *
     * @param bo 客户端信息
     * @return 操作结果
     */
    @SaCheckPermission("system:client:edit")
    @Log(title = "客户端管理", businessType = BusinessType.UPDATE, excludeParamNames = {"ssoSecret", "ssoSecretOnce", "clientSecret"}, isSaveRequestData = false, isSaveResponseData = false)
    @RepeatSubmit()
    @PostMapping("/update")
    public R<SysClientVo> edit(@Validated(EditGroup.class) @RequestBody SysClientBo bo) {
        if (!sysClientService.checkClickKeyUnique(bo)) {
            return R.fail("修改客户端'" + bo.getClientKey() + "'失败，客户端key已存在");
        }
        if (!sysClientService.updateByBo(bo)) {
            return R.fail("修改客户端失败");
        }
        SysClientVo vo = sysClientService.queryById(bo.getId());
        vo.setSsoSecretOnce(bo.getSsoSecretOnce());
        return R.ok(vo);
    }

    /**
     * 轮换 SSO 密钥，明文只返回一次。
     *
     * @param bo 仅使用主键
     * @return 含一次性明文的客户端视图
     */
    @SaCheckPermission("system:client:edit")
    @Log(title = "客户端SSO密钥轮换", businessType = BusinessType.UPDATE, isSaveResponseData = false, isSaveRequestData = false)
    @RepeatSubmit()
    @PostMapping("/sso/rotate-secret")
    public R<SysClientVo> rotateSsoSecret(@RequestBody SysClientBo bo) {
        if (bo.getId() == null) {
            return R.fail("主键不能为空");
        }
        return R.ok(sysClientService.rotateSsoSecret(bo.getId()));
    }

    /**
     * 完成自有 App SSO 接入（须已在 SSO 管理登记精确回调）。
     *
     * @param bo 主键与 authMode
     * @return 接入后的视图
     */
    @SaCheckPermission("system:client:edit")
    @Log(title = "客户端SSO接入", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @RepeatSubmit()
    @PostMapping("/sso/bind")
    public R<SysClientVo> bindSso(@RequestBody SysClientBo bo) {
        if (bo.getId() == null) {
            return R.fail("主键不能为空");
        }
        return R.ok(sysClientService.bindSsoAccess(bo.getId(), bo.getSsoAuthMode()));
    }

    /**
     * 修改客户端启停状态。
     *
     * @param bo 客户端状态信息
     * @return 操作结果
     */
    @SaCheckPermission("system:client:edit")
    @Log(title = "客户端管理", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysClientBo bo) {
        return toAjax(sysClientService.updateClientStatus(bo.getClientId(), bo.getStatus()));
    }

    /**
     * 批量删除客户端配置。
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("system:client:remove")
    @Log(title = "客户端管理", businessType = BusinessType.DELETE, isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(sysClientService.deleteWithValidByIds(List.of(ids), true));
    }
}
