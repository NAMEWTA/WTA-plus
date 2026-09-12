package org.namewta.notify.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.notify.domain.vo.NotifyRecipientUserVo;
import org.namewta.notify.usecase.NotifyRecipientUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 通知发送对象目录，只暴露通知编辑所需的最小用户字段。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notify/recipients")
public class NotifyRecipientController {
    private final NotifyRecipientUseCase recipientUseCase;

    @GetMapping("/search")
    @SaCheckPermission(value = {"notify:notice:add", "notify:notice:edit"}, mode = SaMode.OR)
    public R<PageResult<NotifyRecipientUserVo>> search(@RequestParam String keyword,
                                         @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(recipientUseCase.search(keyword, pageSize));
    }

    @GetMapping("/by-ids")
    @SaCheckPermission(value = {"notify:notice:add", "notify:notice:edit"}, mode = SaMode.OR)
    public R<List<NotifyRecipientUserVo>> byIds(@RequestParam String userIds) {
        try {
            return R.ok(recipientUseCase.byIds(userIds));
        } catch (IllegalArgumentException exception) {
            return R.fail("用户编号格式错误");
        }
    }
}
