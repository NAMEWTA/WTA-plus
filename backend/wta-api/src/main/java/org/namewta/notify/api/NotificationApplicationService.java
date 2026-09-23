package org.namewta.notify.api;

/**
 * 统一通知应用服务，业务模块只能依赖此合同。
 */
public interface NotificationApplicationService {
    /** 提交通知意图。 */
    NotificationReceipt submit(NotificationCommand command);
    /** 查询通知状态。 */
    NotificationSnapshot query(NotificationQuery query);
    /** 精确重排可证明未发送的失败投递；外部未知结果不可重发。 */
    RetryReceipt retry(NotificationRetryCommand command);
    /** 取消尚未完成通知。 */
    CancelReceipt cancel(NotificationCancelCommand command);
}
