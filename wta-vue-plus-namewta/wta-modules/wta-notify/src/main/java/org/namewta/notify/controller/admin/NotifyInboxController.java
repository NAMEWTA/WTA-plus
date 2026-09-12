package org.namewta.notify.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.notify.domain.vo.NotifyInboxMessageVo;
import org.namewta.notify.usecase.NotifyInboxUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知中心用户收件箱接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notify/inbox")
@SaCheckLogin
public class NotifyInboxController {
    private final NotifyInboxUseCase inboxUseCase;

    /** 查询当前用户收件箱。 */
    @GetMapping
    public R<List<NotifyInboxMessageVo>> list() { return R.ok(inboxUseCase.list(LoginHelper.getUserId())); }

    /** 标记已见。 */
    @SaCheckPermission("notify:inbox:seen")
    @PostMapping("/{messageId}/seen")
    public R<Void> seen(@PathVariable Long messageId) { inboxUseCase.seen(messageId, LoginHelper.getUserId()); return R.ok(); }

    /** 标记已读。 */
    @SaCheckPermission("notify:inbox:read")
    @PostMapping("/{messageId}/read")
    public R<Void> read(@PathVariable Long messageId) { inboxUseCase.read(messageId, LoginHelper.getUserId()); return R.ok(); }

    /** 将当前用户全部收件消息标记为已读。 */
    @SaCheckPermission("notify:inbox:read")
    @PostMapping("/read-all")
    public R<Void> readAll() { inboxUseCase.readAll(LoginHelper.getUserId()); return R.ok(); }
}
