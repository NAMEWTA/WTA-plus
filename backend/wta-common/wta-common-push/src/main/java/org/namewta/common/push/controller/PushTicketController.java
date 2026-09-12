package org.namewta.common.push.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.push.security.PushTicketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 长连接票据接口。 */
@RestController
@RequiredArgsConstructor
public class PushTicketController {
    private final PushTicketService ticketService;

    @SaCheckLogin
    @GetMapping("${message.path:/resource/message}/ticket")
    public R<String> issue() {
        return R.ok("操作成功", ticketService.issue());
    }
}
