package org.namewta.notify.controller.anonymous;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.http.CapturedRequestBody;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.notify.usecase.ProviderCallbackUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;


/**
 * 自定义 HMAC 回执接入；不冒充腾讯或阿里的原生回调协议。
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
    @PostMapping(value = "/{channel}", consumes = "application/json")
    @Log(title = "通知状态回执", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public ResponseEntity<R<Void>> callback(@PathVariable String channel, @RequestHeader(value = "X-Notify-Signature", required = false) String signature,
                            @RequestBody String payload, HttpServletRequest request) {
        try {
            // 正文可能已经过 XSS 视图改写；验签与事件解析都必须使用入口捕获的原文。
            CapturedRequestBody raw = CapturedRequestBody.find(request);
            callbackUseCase.apply(channel, signature, raw == null ? payload : raw.utf8(), secret);
            return ResponseEntity.ok(R.ok());
        } catch (ServiceException exception) {
            int code = exception.getCode() == null ? 503 : exception.getCode();
            return ResponseEntity.status(code).body(R.fail(code, exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(R.fail(400, exception.getMessage()));
        } catch (RuntimeException unavailable) {
            // 回执的数据库或提交失败必须可重试，不能落入全局 R.fail 的 HTTP 200 映射。
            return ResponseEntity.status(503).body(R.fail(503, "回执暂时不可用，请稍后重试"));
        }
    }
}
