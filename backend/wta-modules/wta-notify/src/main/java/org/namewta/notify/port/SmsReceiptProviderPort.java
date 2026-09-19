package org.namewta.notify.port;

import org.namewta.common.sms.notify.SmsDeliveryQueryClient.Status;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifyDelivery;
import java.time.Instant;

/** 原生只读供应商适配边界，凭据只传给已固定的HTTPS端点。 */
public interface SmsReceiptProviderPort {
    Status query(NotifyChannelAccount account, NotifyDelivery delivery, Instant now);
}
