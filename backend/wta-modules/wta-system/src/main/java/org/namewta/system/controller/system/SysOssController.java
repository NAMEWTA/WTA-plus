package org.namewta.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.validate.QueryGroup;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.redis.annotation.RepeatSubmit;
import org.namewta.common.web.core.BaseController;
import org.namewta.system.api.OssService;
import org.namewta.system.domain.bo.SysOssBo;
import org.namewta.system.domain.vo.SysOssVo;
import org.namewta.system.oss.migration.OssStorageMigrationService;
import org.namewta.system.service.ISysOssService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 文件上传 控制层
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/resource/oss")
public class SysOssController extends BaseController {

    private final ISysOssService ossService;

    private final OssService publicOssService;

    private final OssStorageMigrationService migrationService;

    /**
     * 分页查询 OSS 对象存储列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return OSS 分页结果
     */
    @SaCheckPermission("system:oss:list")
    @GetMapping("/list")
    public R<PageResult<SysOssVo>> list(@Validated(QueryGroup.class) SysOssBo bo, PageQuery pageQuery) {
        return R.ok(ossService.queryPageList(bo, pageQuery));
    }

    /**
     * 查询OSS对象基于id串
     *
     * @param ossIds OSS对象ID串
     * @return OSS 对象列表
     */
    @SaCheckPermission("system:oss:query")
    @GetMapping("/listByIds/{ossIds}")
    public R<List<SysOssVo>> listByIds(@NotEmpty(message = "主键不能为空")
                                       @PathVariable Long[] ossIds) {
        List<SysOssVo> list = ossService.listByIds(Arrays.asList(ossIds));
        return R.ok(list);
    }

    /**
     * 生成管理面短时下载授权。普通业务应先校验自身业务权限，再调用内部 OssService。
     */
    @SaCheckPermission("system:oss:download")
    @GetMapping("/{ossId}/download-url")
    public R<OssService.OssAccessUrl> downloadUrl(@PathVariable Long ossId) {
        return R.ok(publicOssService.resolveAccessUrl(ossId));
    }

    /**
     * 删除OSS对象存储
     *
     * @param ossIds OSS对象ID串
     * @return 操作结果
     */
    /**
     * 把一个仍在私有配置上的对象复制到所选公开配置，并改写这一行的 service。ossId 不变。
     */
    @SaCheckPermission("system:oss:publish")
    @Log(title = "OSS对象公开", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @RepeatSubmit
    @PostMapping("/{ossId}/publish")
    public R<Long> publish(@PathVariable Long ossId, @Valid @RequestBody PublishRequest request) {
        return R.ok(migrationService.publish(ossId, request.targetConfigKey()));
    }

    /**
     * 用尚未清理来源的工单把 service 拨回私有配置。不删除公开桶里的副本。
     */
    @SaCheckPermission("system:oss:publish")
    @Log(title = "OSS对象恢复私有", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @RepeatSubmit
    @PostMapping("/{ossId}/unpublish")
    public R<Void> unpublish(@PathVariable Long ossId) {
        migrationService.unpublish(ossId);
        return R.ok();
    }

    public record PublishRequest(@NotBlank String targetConfigKey) {
    }

    @SaCheckPermission("system:oss:remove")
    @Log(title = "OSS对象存储", businessType = BusinessType.DELETE, isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{ossIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ossIds) {
        return toAjax(ossService.deleteWithValidByIds(List.of(ossIds), true));
    }

    /**
     * 恢复待删除的 OSS 对象。
     *
     * @param ossIds OSS对象ID串
     * @return 操作结果
     */
    @SaCheckPermission("system:oss:remove")
    @Log(title = "OSS对象存储", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{ossIds}/restore")
    public R<Void> restore(@NotEmpty(message = "主键不能为空")
                           @PathVariable Long[] ossIds) {
        return toAjax(ossService.restoreWithValidByIds(List.of(ossIds)));
    }
}
