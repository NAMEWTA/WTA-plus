package org.namewta.notify.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.web.core.BaseController;
import org.namewta.notify.domain.bo.NotifyChannelAccountBo;
import org.namewta.notify.domain.bo.NotifySceneBindingBo;
import org.namewta.notify.domain.bo.NotifyTestSendBo;
import org.namewta.notify.domain.vo.NotifyChannelAccountVo;
import org.namewta.notify.domain.vo.NotifySceneBindingVo;
import org.namewta.notify.usecase.NotifyConfigUseCase;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知中心邮件/短信渠道配置。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/notify/config")
public class NotifyConfigController extends BaseController {
    private final NotifyConfigUseCase configUseCase;

    /**
     * 分页查询渠道账号。
     *
     * @param channel   渠道
     * @param pageQuery 分页
     * @return 账号分页
     */
    @SaCheckPermission("notify:config:list")
    @GetMapping("/account/list")
    public R<PageResult<NotifyChannelAccountVo>> listAccounts(String channel, PageQuery pageQuery) {
        return R.ok(configUseCase.pageAccounts(channel, pageQuery.getPageNum(), pageQuery.getPageSize()));
    }

    /**
     * 账号详情，不回显密钥。
     *
     * @param accountId 主键
     * @return 账号
     */
    @SaCheckPermission("notify:config:query")
    @GetMapping("/account/{accountId}")
    public R<NotifyChannelAccountVo> getAccount(@PathVariable Long accountId) {
        return R.ok(configUseCase.getAccount(accountId));
    }

    /**
     * 新增渠道账号。
     *
     * @param bo 账号
     * @return 结果
     */
    @SaCheckPermission("notify:config:add")
    @Log(title = "通知渠道账号", businessType = BusinessType.INSERT, isSaveRequestData = false)
    @PostMapping("/account")
    public R<Void> addAccount(@Valid @RequestBody NotifyChannelAccountBo bo) {
        return toAjax(configUseCase.addAccount(bo));
    }

    /**
     * 修改渠道账号。密钥留空则保持原值。
     *
     * @param bo 账号
     * @return 结果
     */
    @SaCheckPermission("notify:config:edit")
    @Log(title = "通知渠道账号", businessType = BusinessType.UPDATE, isSaveRequestData = false)
    @PostMapping("/account/edit")
    public R<Void> editAccount(@Valid @RequestBody NotifyChannelAccountBo bo) {
        return toAjax(configUseCase.updateAccount(bo));
    }

    /**
     * 启停渠道账号。
     *
     * @param bo 含主键与 enabled
     * @return 结果
     */
    @SaCheckPermission("notify:config:edit")
    @Log(title = "通知渠道账号", businessType = BusinessType.UPDATE, isSaveRequestData = false)
    @PostMapping("/account/changeStatus")
    public R<Void> changeStatus(@RequestBody NotifyChannelAccountBo bo) {
        return toAjax(configUseCase.changeStatus(bo.getAccountId(), bo.getEnabled()));
    }

    /**
     * 删除未被绑定的渠道账号。
     *
     * @param accountId 主键
     * @return 结果
     */
    @SaCheckPermission("notify:config:remove")
    @Log(title = "通知渠道账号", businessType = BusinessType.DELETE, isSaveRequestData = false)
    @PostMapping("/account/remove")
    public R<Void> removeAccount(@RequestBody Long accountId) {
        return toAjax(configUseCase.removeAccount(accountId));
    }

    /**
     * 列出播种场景及当前渠道绑定。
     *
     * @param channel 渠道
     * @return 场景
     */
    @SaCheckPermission("notify:config:list")
    @GetMapping("/scene/list")
    public R<List<NotifySceneBindingVo>> listScenes(String channel) {
        return R.ok(configUseCase.listScenes(channel));
    }

    /**
     * 保存场景渠道绑定与热配文案。
     *
     * @param bo 绑定
     * @return 结果
     */
    @SaCheckPermission("notify:config:edit")
    @Log(title = "通知场景绑定", businessType = BusinessType.UPDATE, isSaveRequestData = false)
    @PostMapping("/scene/save")
    public R<Void> saveBinding(@Valid @RequestBody NotifySceneBindingBo bo) {
        return toAjax(configUseCase.saveBinding(bo));
    }

    /**
     * 账号级测试发送。
     *
     * @param bo 账号与收件人
     * @return 提交状态
     */
    @SaCheckPermission("notify:config:test")
    @Log(title = "通知测试发送", businessType = BusinessType.OTHER, isSaveRequestData = false)
    @PostMapping("/test/account")
    public R<String> testAccount(@Valid @RequestBody NotifyTestSendBo bo) {
        return R.ok(configUseCase.testAccount(bo.getAccountId(), bo.getSceneCode(), bo.getTarget()));
    }

    /**
     * 模板级测试发送。
     *
     * @param bo 场景、渠道与收件人
     * @return 提交状态
     */
    @SaCheckPermission("notify:config:test")
    @Log(title = "通知测试发送", businessType = BusinessType.OTHER, isSaveRequestData = false)
    @PostMapping("/test/template")
    public R<String> testTemplate(@Valid @RequestBody NotifyTestSendBo bo) {
        return R.ok(configUseCase.testTemplate(bo.getSceneCode(), bo.getChannel(), bo.getTarget()));
    }
}
