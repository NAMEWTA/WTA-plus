package org.namewta.notify.controller.anonymous;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.usecase.ProviderCallbackUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;


/**
 * 外部供应商回调入口，验签后只更新统一投递状态。
 */
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/notify/callback")
public class ProviderCallbackController {
    private final ProviderCallbackUseCase callbackUseCase;
    @Value("${notify.callback-secret:}")
    private String secret;

    /**
     * 接收供应商状态回调。
     *
     * @param channel 渠道
     * @param signature HMAC-SHA256 签名
     * @param payload 原始 JSON 回调数据，必须包含 providerMessageId 和 status
     * @return 处理结果
     */
    @PostMapping("/{channel}")
    public R<Void> callback(@PathVariable String channel, @RequestHeader("X-Notify-Signature") String signature,
                            @RequestBody String payload) {
        try {
            callbackUseCase.apply(channel, signature, payload, secret);
            return R.ok();
        } catch (ServiceException | IllegalArgumentException exception) {
            return R.fail(exception.getMessage());
        }
    }
}
