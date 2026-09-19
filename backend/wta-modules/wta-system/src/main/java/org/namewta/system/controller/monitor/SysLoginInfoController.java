package org.namewta.system.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.lock.annotation.Lock4j;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.common.excel.utils.ExcelBuilder;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.redis.annotation.RepeatSubmit;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.common.web.core.BaseController;
import org.namewta.system.domain.bo.SysLoginInfoBo;
import org.namewta.system.domain.vo.SysLoginInfoVo;
import org.namewta.system.service.ISysLoginInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统访问记录
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/monitor/loginInfo")
public class SysLoginInfoController extends BaseController {

    private final ISysLoginInfoService loginInfoService;

    /**
     * 分页查询系统访问记录。
     *
     * @param loginInfo 查询条件
     * @param pageQuery 分页参数
     * @return 登录日志分页结果
     */
    @SaCheckPermission("monitor:logininfo:list")
    @GetMapping("/list")
    public R<PageResult<SysLoginInfoVo>> list(SysLoginInfoBo loginInfo, PageQuery pageQuery) {
        return R.ok(loginInfoService.selectPageLoginInfoList(loginInfo, pageQuery));
    }

    /**
     * 导出系统访问记录列表。
     *
     * @param loginInfo 查询条件
     * @param response  HTTP 响应
     */
    @Log(title = "登录日志", businessType = BusinessType.EXPORT)
    @SaCheckPermission("monitor:logininfo:export")
    @PostMapping("/export")
    public void export(SysLoginInfoBo loginInfo, HttpServletResponse response) {
        List<SysLoginInfoVo> list = loginInfoService.selectLoginInfoList(loginInfo);
        ExcelBuilder.of(list, SysLoginInfoVo.class).sheetName("登录日志").toResponse(response);
    }

    /**
     * 批量删除登录日志
     *
     * @param infoIds 日志ids
     * @return 操作结果
     */
    @SaCheckPermission("monitor:logininfo:remove")
    @Log(title = "登录日志", businessType = BusinessType.DELETE)
    @PostMapping("/{infoIds}")
    public R<Void> remove(@PathVariable Long[] infoIds) {
        return toAjax(loginInfoService.deleteLoginInfoByIds(infoIds));
    }

    /**
     * 清空系统访问记录。
     *
     * @return 操作结果
     */
    @SaCheckPermission("monitor:logininfo:remove")
    @Log(title = "登录日志", businessType = BusinessType.CLEAN)
    @Lock4j
    @PostMapping("/clean")
    public R<Void> clean() {
        loginInfoService.cleanLoginInfo();
        return R.ok();
    }

    /**
     * 清除指定用户的登录失败锁定状态。
     *
     * @param userName 用户名
     * @return 操作结果
     */
    @SaCheckPermission("monitor:logininfo:unlock")
    @Log(title = "账户解锁", businessType = BusinessType.OTHER)
    @RepeatSubmit()
    @PostMapping("/unlock/{userName}")
    public R<Void> unlock(@PathVariable("userName") String userName) {
        String loginName = CacheNames.PWD_ERR_CNT_KEY + userName;
        if (RedisUtils.hasKey(loginName)) {
            RedisUtils.deleteObject(loginName);
        }
        return R.ok();
    }

}
