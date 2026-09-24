package org.namewta.common.notify.model;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 单渠道通知请求。
 */
public record NotifyRequest(
    String requestId,
    String bizType,
    String bizId,
    NotifyChannel channel,
    String providerKey,
    List<NotifyTarget> targets,
    NotifyContent content,
    List<Long> attachmentOssIds,
    NotifyAuditPolicy auditPolicy,
    String idempotencyKey,
    Duration idempotencyWindow,
    Map<String, String> metadata,
    Long attachmentOwnerIntentId,
    @com.fasterxml.jackson.annotation.JsonIgnore PreSendGate preSendGate
) {

    public NotifyRequest {
        requestId = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
        targets = targets == null ? List.of() : List.copyOf(targets);
        attachmentOssIds = attachmentOssIds == null ? List.of() : List.copyOf(attachmentOssIds);
        auditPolicy = auditPolicy == null ? NotifyAuditPolicy.FULL : auditPolicy;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 请求构建器。
     */
    public static final class Builder {

        private String requestId;
        private String bizType;
        private String bizId;
        private NotifyChannel channel;
        private String providerKey;
        private List<NotifyTarget> targets = List.of();
        private NotifyContent content;
        private List<Long> attachmentOssIds = List.of();
        private NotifyAuditPolicy auditPolicy = NotifyAuditPolicy.FULL;
        private String idempotencyKey;
        private Duration idempotencyWindow;
        private Map<String, String> metadata = Map.of();
        private Long attachmentOwnerIntentId;
        private PreSendGate preSendGate;

        private Builder() {
        }

        public Builder requestId(String value) {
            requestId = value;
            return this;
        }

        public Builder bizType(String value) {
            bizType = value;
            return this;
        }

        public Builder bizId(String value) {
            bizId = value;
            return this;
        }

        public Builder channel(NotifyChannel value) {
            channel = value;
            return this;
        }

        public Builder providerKey(String value) {
            providerKey = value;
            return this;
        }

        public Builder targets(List<NotifyTarget> value) {
            targets = value;
            return this;
        }

        public Builder content(NotifyContent value) {
            content = value;
            return this;
        }

        public Builder attachmentOssIds(List<Long> value) {
            attachmentOssIds = value;
            return this;
        }

        public Builder auditPolicy(NotifyAuditPolicy value) {
            auditPolicy = value;
            return this;
        }

        public Builder idempotencyKey(String value) {
            idempotencyKey = value;
            return this;
        }

        public Builder idempotencyWindow(Duration value) {
            idempotencyWindow = value;
            return this;
        }

        public Builder metadata(Map<String, String> value) {
            metadata = value;
            return this;
        }

        /** 持久 Intent 主键，只由通知 Worker 设置，不从外部提交者模板或上下文推断。 */
        public Builder attachmentOwnerIntentId(Long value) {
            attachmentOwnerIntentId = value;
            return this;
        }

        /** 仅运行时 Worker 设置；HTTP、持久化和监控事件不得携带此回调。 */
        public Builder preSendGate(PreSendGate value) {
            preSendGate = value;
            return this;
        }

        public NotifyRequest build() {
            return new NotifyRequest(requestId, bizType, bizId, channel, providerKey, targets, content,
                attachmentOssIds, auditPolicy, idempotencyKey, idempotencyWindow, metadata,
                attachmentOwnerIntentId, preSendGate);
        }
    }

    /** 在物化完成与真正进入 Provider 之间执行一次短事务栅栏；保留原失败实例。 */
    public static final class PreSendGate implements Runnable {
        private final Runnable delegate;
        private volatile boolean failed;

        public PreSendGate(Runnable delegate) { this.delegate = java.util.Objects.requireNonNull(delegate); }

        @Override public void run() {
            failed = false;
            try { delegate.run(); }
            catch (RuntimeException | Error failure) {
                failed = true;
                throw failure;
            }
        }

        public boolean failed() { return failed; }
    }

    /** 栅栏已在数据库内关闭任务；调用链须释放本次幂等占位并直接停止。 */
    public static final class PreSendClosed extends RuntimeException {
        public PreSendClosed() { super("通知在供应商调用前已关闭"); }
    }
}
