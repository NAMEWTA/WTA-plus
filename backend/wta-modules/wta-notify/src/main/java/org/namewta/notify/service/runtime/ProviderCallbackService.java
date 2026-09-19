package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.dao.NotifyProviderReceiptDao;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyProviderReceipt;
import org.namewta.notify.domain.policy.NotificationDeliveryPolicy;
import org.namewta.notify.port.NotifyDispatchResultPort;
import org.namewta.notify.port.ProviderCallbackPort;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 已认证回执的持久幂等规则；由外层 UseCase 保证 receipt、状态与聚合同事务。 */
@Service
@RequiredArgsConstructor
public class ProviderCallbackService implements ProviderCallbackPort {
    private static final Set<String> CALLBACK_STATUSES = Set.of("ACCEPTED", "DELIVERED", "UNDELIVERABLE", "FAILED", "UNKNOWN");

    private final NotifyNotificationDao dao;
    private final NotifyDispatchResultPort resultPort;
    private final NotifyProviderReceiptDao receipts;

    /**
     * 账号内事件编号永久去重；同事件不同事实失败关闭，回滚不会消费事件。
     * 传输时间戳由接入层验证，不进入业务摘要，允许重试时重新签名。
     */
    @Override
    public void apply(String channel, String providerKey, String providerMessageId, String status, String eventId, String target) {
        requireText(channel, 32); requireText(providerKey, 64); requireText(providerMessageId, 255);
        requireText(status, 32); requireText(eventId, 255);
        String normalizedChannel = channel.toUpperCase(Locale.ROOT);
        String normalizedStatus = status.toUpperCase(Locale.ROOT);
        String normalizedTarget = target == null ? "" : target;
        if (!Set.of("MAIL", "SMS").contains(normalizedChannel)) throw new ServiceException("回调渠道不支持", 422);
        if (!CALLBACK_STATUSES.contains(normalizedStatus)) throw new ServiceException("回调状态不支持", 422);
        if (normalizedTarget.length() > 1000 || (!normalizedTarget.isEmpty() && normalizedTarget.isBlank())) {
            throw new ServiceException("回调收件人无效", 422);
        }
        String eventKey = digest(List.of(normalizedChannel, providerKey, eventId));
        String factsHash = digest(List.of(normalizedChannel, providerKey, providerMessageId, normalizedStatus, normalizedTarget));
        NotifyProviderReceipt committed = receipts.find(eventKey);
        if (committed != null) {
            requireSameFacts(committed, factsHash);
            return;
        }
        var candidates = dao.deliveriesByProvider(normalizedChannel, providerKey, providerMessageId, normalizedTarget);
        if (candidates.isEmpty()) throw new ServiceException("未找到对应投递记录，请稍后重试");
        if (candidates.size() != 1) throw new ServiceException("回执投递不唯一，请指定准确收件人", 422);
        NotifyDelivery delivery = candidates.getFirst();
        // 与 dispatch 一致：Intent 在前，随后锁定并重新读取 Delivery。
        if (dao.lockIntent(delivery.getIntentId()) == null) throw new ServiceException("通知不存在");
        delivery = dao.lockDelivery(delivery.getDeliveryId());
        if (delivery == null) throw new ServiceException("投递记录不存在");
        if (!normalizedChannel.equals(delivery.getChannel())
            || !providerKey.equals(delivery.getProviderKey())
            || !providerMessageId.equals(delivery.getProviderMessageId())
            || (!normalizedTarget.isEmpty() && !normalizedTarget.equals(delivery.getTargetValue()))) {
            throw new ServiceException("回执对应投递已变化");
        }
        NotifyProviderReceipt receipt = new NotifyProviderReceipt();
        receipt.setProviderReceiptId(IdGeneratorUtil.nextLongId());
        receipt.setEventKey(eventKey); receipt.setFactsHash(factsHash); receipt.setDeliveryId(delivery.getDeliveryId());
        receipt.setVersion(0); receipt.setDelFlag("0");
        try {
            if (receipts.insert(receipt) != 1) throw new ServiceException("回执持久化失败，请重试");
        } catch (DuplicateKeyException duplicate) {
            // MySQL 重复键只回滚当前语句，随后以当前读核对已提交的赢家。
            requireSameFacts(receipts.lock(eventKey), factsHash);
            return;
        }
        if (!NotificationDeliveryPolicy.canAdvance(delivery.getStatus(), normalizedStatus)) return;
        String previousStatus = delivery.getStatus();
        delivery.setStatus(normalizedStatus);
        if ("ACCEPTED".equals(normalizedStatus)) delivery.setAcceptedAt(dao.databaseNow());
        if ("DELIVERED".equals(normalizedStatus)) {
            delivery.setAcceptedAt(delivery.getAcceptedAt() == null ? dao.databaseNow() : delivery.getAcceptedAt());
            delivery.setDeliveredAt(dao.databaseNow());
        }
        if (dao.updateDeliveryStatus(delivery.getDeliveryId(), previousStatus, delivery) != 1) {
            throw new ServiceException("回执状态写入冲突，请重试");
        }
        resultPort.refreshAggregate(delivery.getIntentId());
    }

    private static void requireText(String value, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum) throw new ServiceException("回调参数为空或过长", 422);
    }

    private static void requireSameFacts(NotifyProviderReceipt receipt, String factsHash) {
        if (receipt == null || !factsHash.equals(receipt.getFactsHash())) {
            throw new ServiceException("回执事件编号已存在且业务事实不一致", 409);
        }
    }

    /** JSON 数组保留字段边界，避免简单字符串拼接产生身份碰撞。 */
    private static String digest(List<String> values) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(JsonUtils.toJsonString(values).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 unavailable", unavailable);
        }
    }
}
