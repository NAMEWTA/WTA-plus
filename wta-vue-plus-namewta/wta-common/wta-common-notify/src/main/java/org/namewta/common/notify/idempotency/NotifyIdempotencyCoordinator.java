package org.namewta.common.notify.idempotency;

import org.namewta.common.notify.exception.NotifyIdempotencyUnavailableException;
import org.namewta.common.notify.exception.NotifyValidationException;
import org.namewta.common.notify.model.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;

/**
 * 生成稳定作用域/摘要并把 Redis 故障转换为通知层 typed exception。
 */
public final class NotifyIdempotencyCoordinator {

    private static final String KEY_PREFIX = "notify:idempotency:v1:";

    private final NotifyIdempotencyStore store;
    private final NotifyIdempotencyProperties properties;

    public NotifyIdempotencyCoordinator(NotifyIdempotencyStore store, NotifyIdempotencyProperties properties) {
        this.store = store;
        this.properties = properties == null ? new NotifyIdempotencyProperties() : properties;
    }

    public NotifyIdempotencyStore.Claim begin(NotifyRequest request) {
        if (store == null) {
            throw unavailable("ACQUIRE", null);
        }
        try {
            return store.acquire(storageKey(request), digest(request), request.requestId(),
                resolveWindow(request.idempotencyWindow()));
        } catch (NotifyIdempotencyUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unavailable("ACQUIRE", e);
        }
    }

    public void complete(NotifyIdempotencyStore.Acquired acquired, NotifyResult result) {
        try {
            store.complete(acquired, result);
        } catch (RuntimeException e) {
            throw unavailable("COMPLETE", e);
        }
    }

    public void releaseQuietly(NotifyIdempotencyStore.Acquired acquired) {
        try {
            store.release(acquired);
        } catch (RuntimeException ignored) {
            // 原验证异常优先；占位保留至 TTL 过期可阻止立即重复发送。
        }
    }

    public Duration resolveWindow(Duration requested) {
        NotifyIdempotencyProperties.Snapshot settings = properties.currentSnapshot();
        Duration window = requested == null ? settings.defaultWindow() : requested;
        Duration minimum = settings.minWindow();
        Duration maximum = settings.maxWindow();
        if (window == null || minimum == null || maximum == null || minimum.isNegative()
            || minimum.isZero() || maximum.compareTo(minimum) < 0
            || window.compareTo(minimum) < 0 || window.compareTo(maximum) > 0) {
            throw new NotifyValidationException("IDEMPOTENCY_WINDOW_OUT_OF_RANGE", "通知幂等窗口超出允许范围");
        }
        return window;
    }

    public String storageKey(NotifyRequest request) {
        return KEY_PREFIX + sha256(request.channel().value() + "\n" + normalize(request.idempotencyKey()));
    }

    public String digest(NotifyRequest request) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, "bizType", request.bizType());
        append(canonical, "bizId", request.bizId());
        append(canonical, "channel", request.channel().value());
        append(canonical, "providerKey", request.providerKey());
        for (NotifyTarget target : request.targets()) {
            append(canonical, "target.type", target.type());
            append(canonical, "target.value", target.value());
            append(canonical, "target.role", target.role());
        }
        appendContent(canonical, request.content());
        request.attachmentOssIds().forEach(id -> append(canonical, "attachment", String.valueOf(id)));
        request.metadata().entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.nullsFirst(String::compareTo)))
            .forEach(entry -> {
                append(canonical, "metadata.key", entry.getKey());
                append(canonical, "metadata.value", entry.getValue());
            });
        return sha256(canonical.toString());
    }

    private void appendContent(StringBuilder canonical, NotifyContent content) {
        append(canonical, "content.type", content == null ? null : content.getClass().getSimpleName());
        if (content == null) {
            return;
        }
        append(canonical, "content.subject", content.subject());
        append(canonical, "content.snapshot", content.contentSnapshot());
        if (content instanceof NotifyTemplateContent template) {
            append(canonical, "content.templateCode", template.providerTemplateCode());
            template.params().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    append(canonical, "content.param.key", entry.getKey());
                    append(canonical, "content.param.value", entry.getValue());
                });
        } else if (content instanceof NotifyRichContent rich) {
            append(canonical, "content.html", Boolean.toString(rich.html()));
        }
    }

    private void append(StringBuilder canonical, String name, String value) {
        canonical.append(name.length()).append(':').append(name).append('=');
        if (value == null) {
            canonical.append("-1:");
        } else {
            canonical.append(value.length()).append(':').append(value);
        }
        canonical.append(';');
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 不支持 SHA-256", e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }

    private NotifyIdempotencyUnavailableException unavailable(String phase, Throwable cause) {
        return new NotifyIdempotencyUnavailableException(phase, "通知幂等状态不可用", cause);
    }
}
