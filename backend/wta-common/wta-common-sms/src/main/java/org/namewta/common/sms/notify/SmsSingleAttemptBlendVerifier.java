package org.namewta.common.sms.notify;

import org.dromara.sms4j.api.SmsBlend;

/**
 * 核对已选 SMS4J 实例确由受控零内部重试配置创建；只看 configId 或 supplier 不足以证明。
 */
@FunctionalInterface
public interface SmsSingleAttemptBlendVerifier {
    boolean isSingleAttempt(SmsBlend selectedBlend);
}
