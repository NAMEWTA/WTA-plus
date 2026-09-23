package org.namewta.notify.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.notify.domain.vo.NotifyInboxMessageVo;
import org.namewta.notify.domain.vo.NotifyInboxPageVo;
import org.namewta.notify.usecase.NotifyInboxUseCase;
import org.springframework.web.bind.annotation.*;

/**
 * 通知中心用户收件箱接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notify/inbox")
@SaCheckLogin
public class NotifyInboxController {
    private final NotifyInboxUseCase inboxUseCase;

    /** 查询当前用户的有界收件箱页面。 */
    @GetMapping
    public R<NotifyInboxPageVo> list(@RequestParam(required = false) Integer pageNum,
                                     @RequestParam(required = false) Integer pageSize) {
        return R.ok(inboxUseCase.list(LoginHelper.getUserId(), pageNum, pageSize));
    }

    /** 仅返回当前用户确有收件关系的消息正文。 */
    @GetMapping("/{messageId}")
    public R<NotifyInboxMessageVo> detail(@PathVariable Long messageId) {
        return R.ok(inboxUseCase.detail(messageId, LoginHelper.getUserId()));
    }

    /** 标记已见。 */
    @SaCheckPermission("notify:inbox:seen")
    @Log(title = "通知收件箱已见", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{messageId}/seen")
    public R<Void> seen(@PathVariable Long messageId) { inboxUseCase.seen(messageId, LoginHelper.getUserId()); return R.ok(); }

    /** 标记已读。 */
    @SaCheckPermission("notify:inbox:read")
    @Log(title = "通知收件箱已读", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{messageId}/read")
    public R<Void> read(@PathVariable Long messageId) { inboxUseCase.read(messageId, LoginHelper.getUserId()); return R.ok(); }

    /** 将当前用户全部收件消息标记为已读。 */
    @SaCheckPermission("notify:inbox:read")
    @Log(title = "通知收件箱全部已读", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/read-all")
    public R<Void> readAll() { inboxUseCase.readAll(LoginHelper.getUserId()); return R.ok(); }
}
