package org.namewta.common.notify.exception;

import org.namewta.common.notify.model.NotifyResult;

/**
 * Provider 发送未全部接受。
 */
public class NotifyDeliveryException extends RuntimeException {

    private final NotifyResult result;

    public NotifyDeliveryException(NotifyResult result) {
        super("通知未被全部目标接受");
        this.result = result;
    }

    public NotifyResult result() {
        return result;
    }
}
