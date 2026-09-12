package org.namewta.common.notify.attachment;

/**
 * 在 Provider 调用前预生成通知日志主键。
 */
@FunctionalInterface
public interface NotifyLogIdGenerator {

    long nextId();
}
