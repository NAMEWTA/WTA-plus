package org.namewta.demo.controller;

import org.namewta.common.core.domain.R;
import org.namewta.common.openapi.annotation.OpenApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenAPI 机器调用演示接口。
 *
 * <p>该控制器只演示注解注册和签名调用链路，不读写业务数据。</p>
 */
@RestController
@RequestMapping("/demo/openapi")
public class OpenApiDemoController {

    /**
     * 回显调用方传入的消息，用于验证 OpenAPI 接口目录和签名调用。
     *
     * @param message 待回显的消息，未传入时使用默认示例文本
     * @return 包含回显消息的统一响应结果
     */
    @OpenApi("OpenAPI 回显演示")
    @GetMapping("/echo")
    public R<String> echo(@RequestParam(defaultValue = "Hello OpenAPI") String message) {
        return R.data(message);
    }
}
