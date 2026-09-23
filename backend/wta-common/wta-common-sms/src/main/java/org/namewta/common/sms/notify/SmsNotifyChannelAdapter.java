package org.namewta.common.sms.notify;

import org.namewta.common.notify.model.*;
import org.namewta.common.notify.spi.NotifyChannelAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * SMS4J 短信通知渠道 Adapter。
 */
public final class SmsNotifyChannelAdapter implements NotifyChannelAdapter {

    private final SmsNotificationProviderResolver providerResolver;

    public SmsNotifyChannelAdapter(SmsNotificationProviderResolver providerResolver) {
        this.providerResolver = providerResolver;
    }

    @Override
    public NotifyChannel channel() {
        return NotifyChannel.SMS;
    }

    @Override
    public Set<String> supportedTargetTypes() {
        return Set.of(NotifyTargetType.PHONE);
    }

    @Override
    public NotifyAdapterResult send(NotifyAdapterRequest adapterRequest) {
        NotifyRequest request = adapterRequest.request();
        SmsNotificationProvider provider = providerResolver.resolve(request.providerKey());
        List<NotifyTargetResult> results = new ArrayList<>(request.targets().size());
        for (NotifyTarget target : request.targets()) {
            long startedAt = System.nanoTime();
            try {
                SmsNotificationReceipt receipt = provider.sender().send(target.value(), request.content());
                long costTime = elapsedMillis(startedAt);
                if (receipt != null && receipt.success()) {
                    results.add(NotifyTargetResult.accepted(target, receipt.providerMessageId(), costTime));
                } else {
                    // 供应商错误正文不可信，可能原样回显手机号或验证码。
                    results.add(NotifyTargetResult.failed(target, "PROVIDER_REJECTED",
                        "SMS Provider 未接受请求", costTime));
                }
            } catch (RuntimeException exception) {
                results.add(NotifyTargetResult.failed(target, "PROVIDER_ERROR", "SMS Provider 调用失败",
                    elapsedMillis(startedAt)));
            }
        }
        return new NotifyAdapterResult(provider.providerKey(), results);
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
