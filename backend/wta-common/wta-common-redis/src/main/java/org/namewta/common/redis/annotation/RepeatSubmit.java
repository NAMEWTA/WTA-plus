package org.namewta.common.redis.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 自定义注解防止表单重复提交
 * <p>仅保护同步调用的短时提交窗口；成功保留原TTL，失败按随机owner原子释放。
 * 不提供跨嵌套调用或异步完成协议，不替代数据库唯一约束、业务幂等或持久exactly-once。</p>
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /**
     * 间隔时间(ms)，小于此时间视为重复提交
     */
    int interval() default 5000;

    /**
     * 重复提交间隔时间单位。
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 提示消息 支持国际化 格式为 {code}
     */
    String message() default "{repeat.submit.message}";

}
