package org.namewta.notify.adapter.provider;

import org.namewta.common.sms.notify.SmsDeliveryQueryClient;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.port.SmsReceiptProviderPort;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.ZoneOffset;

/** 将业务账号与投递快照交给common-sms只读客户端；不注册第二套发送器。 */
@Component
public class NativeSmsReceiptAdapter implements SmsReceiptProviderPort {
    private final SmsDeliveryQueryClient client = new SmsDeliveryQueryClient();

    @Override
    public SmsDeliveryQueryClient.Status query(NotifyChannelAccount account, NotifyDelivery delivery, Instant now) {
        return client.query(new SmsDeliveryQueryClient.Account(account.getSupplier(), account.getAccessKeyId(),
                account.getAccessKeySecret(), account.getSdkAppId()),
            new SmsDeliveryQueryClient.Query(delivery.getTargetValue(), delivery.getProviderMessageId(),
                delivery.getAcceptedAt().toInstant(ZoneOffset.UTC)), now);
    }
}
