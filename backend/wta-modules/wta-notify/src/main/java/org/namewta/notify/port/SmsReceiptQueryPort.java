package org.namewta.notify.port;

import org.namewta.notify.domain.entity.NotifyDelivery;

/** Worker只通过用例领取并核对，外部I/O不在领取事务中。 */
public interface SmsReceiptQueryPort {
    /** 领取一条到期短信；无候选或CAS竞争失败返回null。 */
    NotifyDelivery claim();
    /** 读取供应商状态并通过独立结果事务确认；不发送短信。 */
    void query(NotifyDelivery delivery);
}
