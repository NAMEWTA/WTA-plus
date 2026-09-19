package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.common.sms.notify.SmsDeliveryQueryClient.Status;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.port.SmsReceiptProviderPort;
import org.namewta.notify.port.SmsReceiptResultPort;
import org.springframework.stereotype.Service;
import java.time.ZoneOffset;
import java.util.Objects;

/** 只读状态核对规则；领取事务与外部查询/结果事务明确分离。 */
@Service
@RequiredArgsConstructor
public class SmsReceiptQueryService {
    private final NotifyNotificationDao dao;
    private final NotifyConfigDao configDao;
    private final SmsReceiptProviderPort provider;
    private final SmsReceiptResultPort results;

    /** 预约15分钟后重试，覆盖单次最多20页的有界I/O和进程退出。 */
    public NotifyDelivery claim() {
        var now = dao.databaseNow().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        NotifyDelivery candidate = dao.smsReceiptCandidate(now);
        if (candidate == null || dao.reserveSmsReceiptQuery(candidate, now.plusMinutes(15)) != 1) return null;
        candidate.setReceiptQueryAt(now.plusMinutes(15));
        return candidate;
    }

    /**
     * 重新读取投递及账号后查询；停用/删除/状态变化均不调用供应商。
     * 凭据修改期间完成的结果不应用，预约到期后使用新配置重新核对。
     */
    public void query(NotifyDelivery claimed) {
        if (claimed == null) return;
        NotifyDelivery delivery = dao.delivery(claimed.getDeliveryId());
        if (delivery == null || !"SMS".equals(delivery.getChannel()) || !"ACCEPTED".equals(delivery.getStatus())
            || !Objects.equals(claimed.getReceiptQueryAt(), delivery.getReceiptQueryAt())
            || !Objects.equals(claimed.getProviderKey(), delivery.getProviderKey())
            || !Objects.equals(claimed.getProviderMessageId(), delivery.getProviderMessageId())) return;
        var account = configDao.findAccount("SMS", delivery.getProviderKey());
        if (account == null || !"Y".equals(account.getEnabled())
            || !("tencent".equals(account.getSupplier()) || "alibaba".equals(account.getSupplier()))) return;
        var now = dao.databaseNow();
        if (delivery.getAcceptedAt() == null || !delivery.getAcceptedAt().isAfter(now.minusHours(71))) return;
        Status status = provider.query(account, delivery, now.toInstant(ZoneOffset.UTC));
        if (status == Status.WAITING) return;
        var current = configDao.findAccount(account.getAccountId());
        if (current == null || !"Y".equals(current.getEnabled()) || !Objects.equals(account.getVersion(), current.getVersion())) return;
        results.confirm(account.getSupplier(), delivery.getProviderKey(), delivery.getProviderMessageId(),
            delivery.getTargetValue(), status.name());
    }
}
