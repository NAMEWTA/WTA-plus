package org.namewta.demo.controller;

import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.log.annotation.Log;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.redis.utils.RedisUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redis 发布订阅 演示案例
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/redis/pubsub")
public class RedisPubSubController {

    /**
     * 发布消息
     *
     * @param key   通道Key
     * @param value 发送内容
     */
    @PostMapping("/pub")
    @Log(title = "Redis消息发布", businessType = BusinessType.OTHER, isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> pub(String key, String value) {
        RedisUtils.publish(key, value, consumer -> {
            System.out.println("发布通道 => " + key + ", 发送值 => " + value);
        });
        return R.ok("操作成功");
    }

    /**
     * 订阅消息
     *
     * @param key 通道Key
     */
    @PostMapping("/sub")
    @Log(title = "Redis消息订阅", businessType = BusinessType.OTHER, isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> sub(String key) {
        RedisUtils.subscribe(key, String.class, msg -> {
            System.out.println("订阅通道 => " + key + ", 接收值 => " + msg);
        });
        return R.ok("操作成功");
    }

}
