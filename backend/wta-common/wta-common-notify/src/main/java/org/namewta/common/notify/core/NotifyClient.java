package org.namewta.common.notify.core;

import org.namewta.common.notify.model.NotifyRequest;
import org.namewta.common.notify.model.NotifyResult;

/**
 * 统一通知同步入口。
 */
@FunctionalInterface
public interface NotifyClient {

    NotifyResult send(NotifyRequest request);
}
