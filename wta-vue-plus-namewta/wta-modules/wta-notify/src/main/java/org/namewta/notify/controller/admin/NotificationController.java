package org.namewta.notify.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.web.core.BaseController;
import org.namewta.notify.api.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制面管理接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/notify/notification")
public class NotificationController extends BaseController {
    private final NotificationApplicationService notificationService;

    /** 提交通知。 */
    @Log(title = "统一通知", businessType = BusinessType.INSERT)
    @SaCheckPermission("notify:notification:submit")
    @PostMapping
    public R<NotificationReceipt> submit(@Valid @RequestBody NotificationCommand command) {
        return R.ok(notificationService.submit(command));
    }

    /** 查询通知。 */
    @SaCheckPermission("notify:notification:query")
    @GetMapping("/{notificationId}")
    public R<NotificationSnapshot> query(@PathVariable String notificationId,
                                         @RequestParam(defaultValue = "false") boolean includeContent) {
        return R.ok(notificationService.query(new NotificationQuery(notificationId, includeContent)));
    }

    /** 重试通知。 */
    @Log(title = "统一通知", businessType = BusinessType.UPDATE)
    @SaCheckPermission("notify:notification:retry")
    @PostMapping("/{notificationId}/retry")
    public R<RetryReceipt> retry(@PathVariable String notificationId,
                                 @RequestBody(required = false) NotificationRetryCommand command) {
        NotificationRetryCommand actual = command == null
            ? new NotificationRetryCommand(notificationId, null, "manual", null) : command;
        return R.ok(notificationService.retry(actual));
    }

    /** 取消通知。 */
    @Log(title = "统一通知", businessType = BusinessType.UPDATE)
    @SaCheckPermission("notify:notification:cancel")
    @PostMapping("/{notificationId}/cancel")
    public R<CancelReceipt> cancel(@PathVariable String notificationId,
                                   @RequestBody(required = false) NotificationCancelCommand command) {
        NotificationCancelCommand actual = command == null
            ? new NotificationCancelCommand(notificationId, "manual") : command;
        return R.ok(notificationService.cancel(actual));
    }
}
