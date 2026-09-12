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
import org.namewta.system.domain.bo.SysUserTypeBo;
import org.namewta.system.domain.vo.SysUserTypeRelVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.service.ISysUserTypeRelService;
import org.namewta.system.service.ISysUserTypeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 登录域管理
 *
 * @author NAMEWTA
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/userType")
public class SysUserTypeController extends BaseController {

    private final ISysUserTypeService userTypeService;
    private final ISysUserTypeRelService userTypeRelService;

    /**
     * 分页查询登录域列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 登录域分页数据
     */
    @SaCheckPermission("system:userType:list")
    @GetMapping("/list")
    public R<PageResult<SysUserTypeVo>> list(SysUserTypeBo bo, PageQuery pageQuery) {
        return R.ok(userTypeService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出登录域列表。
     *
     * @param bo       查询条件
     * @param response 响应流
     */
    @SaCheckPermission("system:userType:export")
    @Log(title = "登录域管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysUserTypeBo bo, HttpServletResponse response) {
        List<SysUserTypeVo> list = userTypeService.queryList(bo);
        ExcelBuilder.of(list, SysUserTypeVo.class).sheetName("登录域").toResponse(response);
    }

    /**
     * 获取登录域详细信息。
     *
     * @param userTypeId 主键
     * @return 登录域详情
     */
    @SaCheckPermission("system:userType:query")
    @GetMapping("/{userTypeId:\\d+}")
    public R<SysUserTypeVo> getInfo(@NotNull(message = "主键不能为空")
                                    @PathVariable Long userTypeId) {
        return R.ok(userTypeService.queryById(userTypeId));
    }

    /**
     * 新增登录域。编码创建后只读。
     *
     * @param bo 登录域信息
     * @return 操作结果
     */
    @SaCheckPermission("system:userType:add")
    @Log(title = "登录域管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysUserTypeBo bo) {
        if (!userTypeService.checkUserTypeCodeUnique(bo)) {
            return R.fail("新增登录域'" + bo.getUserTypeName() + "'失败，登录域编码已存在");
        }
        return toAjax(userTypeService.insertByBo(bo));
    }

    /**
     * 修改登录域名称、状态、排序与备注。编码不可改。
     *
     * @param bo 登录域信息
     * @return 操作结果
     */
    @SaCheckPermission("system:userType:edit")
    @Log(title = "登录域管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysUserTypeBo bo) {
        return toAjax(userTypeService.updateByBo(bo));
    }

    /**
     * 修改登录域启停状态。
     *
     * @param bo 登录域状态信息
     * @return 操作结果
     */
    @SaCheckPermission("system:userType:edit")
    @Log(title = "登录域管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysUserTypeBo bo) {
        return toAjax(userTypeService.updateStatus(bo.getUserTypeId(), bo.getStatus()));
    }

    /**
     * 批量删除登录域。仍被引用时拒绝删除。
     *
     * @param userTypeIds 主键串
     * @return 操作结果
     */
    @SaCheckPermission("system:userType:remove")
    @Log(title = "登录域管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userTypeIds:\\d+(?:,\\d+)*}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] userTypeIds) {
        return toAjax(userTypeService.deleteWithValidByIds(List.of(userTypeIds)));
    }

    /**
     * 获取启用中的登录域下拉列表。
     *
     * @return 登录域列表
     */
    @SaCheckPermission("system:userType:query")
    @GetMapping("/options")
    public R<List<SysUserTypeVo>> options() {
        return R.ok(userTypeService.options());
    }

    /**
     * 查询指定用户拥有的登录域。
     *
     * @param userId 用户ID
     * @return 登录域关系列表
     */
    @SaCheckPermission("system:user:query")
    @GetMapping("/user/{userId}")
    public R<List<SysUserTypeRelVo>> listByUser(@PathVariable Long userId) {
        return R.ok(userTypeRelService.selectByUserId(userId));
    }

}
