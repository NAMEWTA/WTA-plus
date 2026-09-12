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
    Map<String, String> metadata
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

        public NotifyRequest build() {
            return new NotifyRequest(requestId, bizType, bizId, channel, providerKey, targets, content,
                attachmentOssIds, auditPolicy, idempotencyKey, idempotencyWindow, metadata);
        }
    }
}
