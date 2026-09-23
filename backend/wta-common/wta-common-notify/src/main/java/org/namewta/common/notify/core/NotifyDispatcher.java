package org.namewta.common.notify.core;

import org.namewta.common.notify.attachment.NotifyAttachmentResource;
import org.namewta.common.notify.attachment.NotifyAttachmentSnapshot;
import org.namewta.common.notify.attachment.NotifyAttachmentSnapshotService;
import org.namewta.common.notify.attachment.NotifyLogIdGenerator;
import org.namewta.common.notify.event.NotifyDeliveryEvent;
import org.namewta.common.notify.event.NotifyEventPublisher;
import org.namewta.common.notify.exception.NotifyAttachmentSnapshotException;
import org.namewta.common.notify.exception.NotifyDeliveryException;
import org.namewta.common.notify.exception.NotifyIdempotencyConflictException;
import org.namewta.common.notify.exception.NotifyInProgressException;
import org.namewta.common.notify.exception.NotifyValidationException;
import org.namewta.common.notify.idempotency.NotifyIdempotencyCoordinator;
import org.namewta.common.notify.idempotency.NotifyIdempotencyProperties;
import org.namewta.common.notify.idempotency.NotifyIdempotencyStore;
import org.namewta.common.notify.model.*;
import org.namewta.common.notify.registry.NotifyChannelRegistry;
import org.namewta.common.notify.spi.NotifyChannelAdapter;
import org.namewta.common.notify.spi.NotifyContextResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 统一通知同步调度器。
 */
public final class NotifyDispatcher implements NotifyClient {

    private static final Logger log = LoggerFactory.getLogger(NotifyDispatcher.class);
    private static final String REDACTED = "[REDACTED]";

    private final NotifyChannelRegistry registry;
    private final NotifyContextResolver contextResolver;
    private final NotifyEventPublisher eventPublisher;
    private final NotifyIdempotencyCoordinator idempotencyCoordinator;
    private final NotifyAttachmentSnapshotService attachmentSnapshotService;
    private final NotifyLogIdGenerator notifyLogIdGenerator;

    public NotifyDispatcher(NotifyChannelRegistry registry, NotifyContextResolver contextResolver,
                            NotifyEventPublisher eventPublisher) {
        this(registry, contextResolver, eventPublisher,
            new NotifyIdempotencyCoordinator(null, new NotifyIdempotencyProperties()));
    }

    public NotifyDispatcher(NotifyChannelRegistry registry, NotifyContextResolver contextResolver,
                            NotifyEventPublisher eventPublisher,
                            NotifyIdempotencyCoordinator idempotencyCoordinator) {
        this(registry, contextResolver, eventPublisher, idempotencyCoordinator, null, null);
    }

    public NotifyDispatcher(NotifyChannelRegistry registry, NotifyContextResolver contextResolver,
                            NotifyEventPublisher eventPublisher,
                            NotifyIdempotencyCoordinator idempotencyCoordinator,
                            NotifyAttachmentSnapshotService attachmentSnapshotService,
                            NotifyLogIdGenerator notifyLogIdGenerator) {
        this.registry = registry;
        this.contextResolver = contextResolver;
        this.eventPublisher = eventPublisher;
        this.idempotencyCoordinator = idempotencyCoordinator == null
            ? new NotifyIdempotencyCoordinator(null, new NotifyIdempotencyProperties())
            : idempotencyCoordinator;
        this.attachmentSnapshotService = attachmentSnapshotService;
        this.notifyLogIdGenerator = notifyLogIdGenerator;
    }

    @Override
    public NotifyResult send(NotifyRequest request) {
        validateRequest(request);
        request = normalizeAttachments(request);
        NotifyChannelAdapter adapter = registry.require(request.channel());
        validateTargets(request.targets(), adapter.supportedTargetTypes());
        NotifyContext context = resolveContext();
        NotifyIdempotencyStore.Claim claim = beginIdempotency(request);
        if (claim instanceof NotifyIdempotencyStore.InProgress inProgress) {
            throw new NotifyInProgressException(inProgress.originalRequestId());
        }
        if (claim instanceof NotifyIdempotencyStore.Conflict conflict) {
            throw new NotifyIdempotencyConflictException(conflict.originalRequestId());
        }
        if (claim instanceof NotifyIdempotencyStore.Completed completed) {
            publishDuplicate(request, context, completed);
            return requireAccepted(completed.result());
        }

        SnapshotBatch snapshotBatch;
        try {
            snapshotBatch = createSnapshots(request, context);
        } catch (NotifyAttachmentSnapshotException | NotifyValidationException exception) {
            release(claim);
            throw exception;
        }

        NotifyAdapterResult adapterResult;
        try {
            adapterResult = adapter.send(new NotifyAdapterRequest(request, context, snapshotBatch.snapshots()));
            validateAdapterResult(request, adapterResult);
        } catch (NotifyAttachmentSnapshotException | NotifyValidationException exception) {
            cleanupSnapshots(snapshotBatch.snapshots());
            release(claim);
            throw exception;
        } catch (RuntimeException exception) {
            adapterResult = providerFailure(request);
            log.warn("通知渠道调用异常，channel={}, exception={}",
                request.auditPolicy() == NotifyAuditPolicy.REDACT_SENSITIVE ? REDACTED : request.channel(),
                exception.getClass().getSimpleName());
        }

        NotifyResult result = aggregate(request, adapterResult);
        complete(claim, result, request, context, snapshotBatch);
        publish(request, context, result, snapshotBatch);
        return requireAccepted(result);
    }

    private void validateRequest(NotifyRequest request) {
        if (request == null) {
            throw new NotifyValidationException("REQUEST_REQUIRED", "通知请求不能为空");
        }
        if (request.channel() == null) {
            throw new NotifyValidationException("CHANNEL_REQUIRED", "通知渠道不能为空");
        }
        if (request.targets().isEmpty()) {
            throw new NotifyValidationException("TARGET_REQUIRED", "通知目标不能为空");
        }
        if (request.content() == null || request.content().contentSnapshot() == null) {
            throw new NotifyValidationException("CONTENT_REQUIRED", "通知内容不能为空");
        }
        if (request.content() instanceof NotifyTemplateContent template && isBlank(template.contentSnapshot())) {
            throw new NotifyValidationException("CONTENT_SNAPSHOT_REQUIRED", "模板通知必须提供完整内容快照");
        }
        if (request.attachmentOssIds().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new NotifyValidationException("INVALID_ATTACHMENT_OSS_ID", "附件 OSS ID 必须为正整数");
        }
    }

    private NotifyRequest normalizeAttachments(NotifyRequest request) {
        List<Long> normalized = List.copyOf(new LinkedHashSet<>(request.attachmentOssIds()));
        if (normalized.equals(request.attachmentOssIds())) {
            return request;
        }
        return new NotifyRequest(request.requestId(), request.bizType(), request.bizId(), request.channel(),
            request.providerKey(), request.targets(), request.content(), normalized, request.auditPolicy(),
            request.idempotencyKey(), request.idempotencyWindow(), request.metadata());
    }

    private SnapshotBatch createSnapshots(NotifyRequest request, NotifyContext context) {
        if (request.attachmentOssIds().isEmpty()) {
            return SnapshotBatch.empty();
        }
        if (attachmentSnapshotService == null || notifyLogIdGenerator == null) {
            throw new NotifyValidationException("ATTACHMENT_SNAPSHOT_NOT_CONFIGURED", "附件快照能力尚未装配");
        }
        long notifyLogId = notifyLogIdGenerator.nextId();
        if (notifyLogId <= 0) {
            throw new NotifyAttachmentSnapshotException("INVALID_NOTIFY_LOG_ID", "通知日志主键生成失败");
        }
        List<NotifyAttachmentSnapshot> snapshots = attachmentSnapshotService.createSnapshots(notifyLogId,
            request.attachmentOssIds(), context);
        try {
            validateSnapshots(request.attachmentOssIds(), snapshots);
        } catch (RuntimeException exception) {
            cleanupSnapshots(snapshots);
            throw exception;
        }
        return new SnapshotBatch(notifyLogId, snapshots);
    }

    private void validateSnapshots(List<Long> sourceOssIds, List<NotifyAttachmentSnapshot> snapshots) {
        if (snapshots == null || snapshots.size() != sourceOssIds.size()) {
            throw new NotifyAttachmentSnapshotException("INCOMPLETE_ATTACHMENT_SNAPSHOT", "附件快照结果不完整");
        }
        for (int index = 0; index < sourceOssIds.size(); index++) {
            NotifyAttachmentSnapshot snapshot = snapshots.get(index);
            NotifyAttachmentResource resource = snapshot == null ? null : snapshot.resource();
            if (snapshot == null || !sourceOssIds.get(index).equals(snapshot.sourceOssId())
                || resource == null || resource.ossId() == null || resource.ossId() <= 0
                || isBlank(resource.fileName()) || resource.size() < 0 || resource.materializer() == null) {
                throw new NotifyAttachmentSnapshotException("INVALID_ATTACHMENT_SNAPSHOT", "附件快照结果无效");
            }
        }
    }

    private NotifyIdempotencyStore.Claim beginIdempotency(NotifyRequest request) {
        if (isBlank(request.idempotencyKey())) {
            return null;
        }
        return idempotencyCoordinator.begin(request);
    }

    private void complete(NotifyIdempotencyStore.Claim claim, NotifyResult result,
                          NotifyRequest request, NotifyContext context, SnapshotBatch snapshotBatch) {
        if (!(claim instanceof NotifyIdempotencyStore.Acquired acquired)) {
            return;
        }
        try {
            idempotencyCoordinator.complete(acquired, result);
        } catch (RuntimeException exception) {
            publish(request, context, result, snapshotBatch);
            throw exception;
        }
    }

    private void release(NotifyIdempotencyStore.Claim claim) {
        if (claim instanceof NotifyIdempotencyStore.Acquired acquired) {
            idempotencyCoordinator.releaseQuietly(acquired);
        }
    }

    private void cleanupSnapshots(List<NotifyAttachmentSnapshot> snapshots) {
        if (attachmentSnapshotService == null || snapshots == null || snapshots.isEmpty()) {
            return;
        }
        try {
            attachmentSnapshotService.cleanupSnapshots(snapshots);
        } catch (RuntimeException exception) {
            log.warn("通知附件快照补偿失败，snapshotCount={}, exception={}", snapshots.size(),
                exception.getClass().getSimpleName());
        }
    }

    private NotifyResult requireAccepted(NotifyResult result) {
        if (result.status() != NotifyStatus.ACCEPTED) {
            throw new NotifyDeliveryException(result);
        }
        return result;
    }

    private void validateTargets(List<NotifyTarget> targets, Set<String> supportedTypes) {
        for (NotifyTarget target : targets) {
            if (target == null || isBlank(target.type()) || isBlank(target.value())) {
                throw new NotifyValidationException("INVALID_TARGET", "通知目标类型和值不能为空");
            }
            if (NotifyTargetType.USER.equalsIgnoreCase(target.type())) {
                throw new NotifyValidationException("LOGICAL_TARGET_NOT_SUPPORTED", "common-notify 只接受物理目标");
            }
            if (!supportedTypes.isEmpty() && supportedTypes.stream().noneMatch(target.type()::equalsIgnoreCase)) {
                throw new NotifyValidationException("UNSUPPORTED_TARGET_TYPE", "渠道不支持目标类型: " + target.type());
            }
        }
    }

    private NotifyContext resolveContext() {
        NotifyContext context = contextResolver == null ? null : contextResolver.resolve();
        return context == null ? NotifyContext.empty() : context;
    }

    private void validateAdapterResult(NotifyRequest request, NotifyAdapterResult result) {
        if (result == null || isBlank(result.providerKey())) {
            throw new IllegalStateException("通知渠道未返回 Provider 标识");
        }
        if (result.deliveries().size() != request.targets().size()) {
            throw new IllegalStateException("通知渠道返回的目标结果数量不完整");
        }
        for (int index = 0; index < request.targets().size(); index++) {
            NotifyTargetResult item = result.deliveries().get(index);
            if (item == null || !request.targets().get(index).equals(item.target()) || item.status() == null) {
                throw new IllegalStateException("通知渠道返回的目标结果与请求不匹配");
            }
        }
    }

    private NotifyAdapterResult providerFailure(NotifyRequest request) {
        String provider = isBlank(request.providerKey()) ? "unresolved" : request.providerKey();
        List<NotifyTargetResult> failures = request.targets().stream()
            .map(target -> NotifyTargetResult.failed(target, "PROVIDER_ERROR", "Provider 调用失败", 0L))
            .toList();
        return new NotifyAdapterResult(provider, failures);
    }

    private NotifyResult aggregate(NotifyRequest request, NotifyAdapterResult adapterResult) {
        long accepted = adapterResult.deliveries().stream()
            .filter(item -> item.status() == NotifyDeliveryStatus.ACCEPTED)
            .count();
        NotifyStatus status = accepted == adapterResult.deliveries().size()
            ? NotifyStatus.ACCEPTED
            : accepted == 0 ? NotifyStatus.FAILED : NotifyStatus.PARTIAL_FAILURE;
        return new NotifyResult(request.requestId(), request.channel(), adapterResult.providerKey(), status,
            adapterResult.deliveries());
    }

    private void publish(NotifyRequest request, NotifyContext context, NotifyResult result,
                         SnapshotBatch snapshotBatch) {
        try {
            boolean redact = request.auditPolicy() == NotifyAuditPolicy.REDACT_SENSITIVE;
            eventPublisher.publish(new NotifyDeliveryEvent(auditRequest(request), auditContext(context, redact),
                auditResult(result, redact), null, redact ? null : snapshotBatch.notifyLogId(),
                redact ? List.of() : snapshotBatch.snapshotOssIds(), Instant.now()));
        } catch (RuntimeException exception) {
            log.warn("通知监控事件发布失败，requestId={}, exception={}", auditIdentifier(request),
                exception.getClass().getSimpleName());
        }
    }

    private void publishDuplicate(NotifyRequest request, NotifyContext context,
                                  NotifyIdempotencyStore.Completed completed) {
        NotifyResult skipped = new NotifyResult(request.requestId(), request.channel(),
            completed.result().providerKey(), NotifyStatus.SKIPPED_DUPLICATE, List.of());
        try {
            boolean redact = request.auditPolicy() == NotifyAuditPolicy.REDACT_SENSITIVE;
            eventPublisher.publish(new NotifyDeliveryEvent(auditRequest(request), auditContext(context, redact),
                auditResult(skipped, redact), redact ? REDACTED : completed.originalRequestId(),
                null, List.of(), Instant.now()));
        } catch (RuntimeException exception) {
            log.warn("通知重复监控事件发布失败，requestId={}, exception={}", auditIdentifier(request),
                exception.getClass().getSimpleName());
        }
    }

    /** 仅事件得到脱敏副本；Provider 和幂等摘要始终使用原请求。 */
    private NotifyRequest auditRequest(NotifyRequest request) {
        if (request.auditPolicy() != NotifyAuditPolicy.REDACT_SENSITIVE) return request;
        List<NotifyTarget> targets = request.targets().stream()
            .map(target -> new NotifyTarget(REDACTED, REDACTED, REDACTED)).toList();
        NotifyContent content = switch (request.content()) {
            case NotifyTemplateContent ignored -> new NotifyTemplateContent(REDACTED, REDACTED, java.util.Map.of(), REDACTED);
            case NotifyRichContent rich -> new NotifyRichContent(REDACTED, REDACTED, rich.html());
            case NotifyTextContent ignored -> new NotifyTextContent(REDACTED, REDACTED);
        };
        return new NotifyRequest(REDACTED, REDACTED, REDACTED, request.channel(), REDACTED,
            targets, content, List.of(), request.auditPolicy(), REDACTED,
            request.idempotencyWindow(), java.util.Map.of());
    }

    private NotifyContext auditContext(NotifyContext context, boolean redact) {
        return redact ? new NotifyContext(null, null, null) : context;
    }

    private NotifyResult auditResult(NotifyResult result, boolean redact) {
        if (!redact) return result;
        List<NotifyTargetResult> deliveries = result.deliveries().stream()
            .map(item -> new NotifyTargetResult(
                new NotifyTarget(REDACTED, REDACTED, REDACTED),
                item.status(), null, item.errorCode() == null ? null : REDACTED,
                item.errorMessage() == null ? null : REDACTED, item.costTime())).toList();
        return new NotifyResult(REDACTED, result.channel(), REDACTED, result.status(), deliveries);
    }

    private String auditIdentifier(NotifyRequest request) {
        return request.auditPolicy() == NotifyAuditPolicy.REDACT_SENSITIVE ? REDACTED : request.requestId();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SnapshotBatch(Long notifyLogId, List<NotifyAttachmentSnapshot> snapshots) {

        private SnapshotBatch {
            snapshots = snapshots == null ? List.of() : List.copyOf(snapshots);
        }

        private static SnapshotBatch empty() {
            return new SnapshotBatch(null, List.of());
        }

        private List<Long> snapshotOssIds() {
            return snapshots.stream().map(snapshot -> snapshot.resource().ossId()).toList();
        }
    }
}
