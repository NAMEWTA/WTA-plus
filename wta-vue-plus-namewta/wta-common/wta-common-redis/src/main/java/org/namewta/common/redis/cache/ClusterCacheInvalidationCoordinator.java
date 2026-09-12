package org.namewta.common.redis.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinates process-local invalidation through an acknowledged cluster message.
 */
public class ClusterCacheInvalidationCoordinator implements AutoCloseable {

    private static final Duration DEFAULT_ACKNOWLEDGEMENT_TIMEOUT = Duration.ofSeconds(2);
    private static final String NAMESPACE_PATTERN = "[a-z0-9][a-z0-9:._-]{0,127}";

    private final CacheInvalidationTransport transport;
    private final Duration acknowledgementTimeout;
    private final String nodeId;
    private final Map<String, CopyOnWriteArrayList<CacheInvalidationHandler>> handlers = new ConcurrentHashMap<>();
    private final CacheInvalidationTransport.Subscription subscription;

    public ClusterCacheInvalidationCoordinator(CacheInvalidationTransport transport) {
        this(transport, DEFAULT_ACKNOWLEDGEMENT_TIMEOUT, UUID.randomUUID().toString());
    }

    public ClusterCacheInvalidationCoordinator(CacheInvalidationTransport transport,
                                                Duration acknowledgementTimeout,
                                                String nodeId) {
        this.transport = transport;
        this.acknowledgementTimeout = acknowledgementTimeout;
        this.nodeId = nodeId;
        this.subscription = transport.subscribe(this::receive);
    }

    /**
     * Registers a process-local cache namespace.
     *
     * @param namespace stable namespace
     * @param handler   local eviction handler
     * @return registration used to detach the handler
     */
    public AutoCloseable register(String namespace, CacheInvalidationHandler handler) {
        validateNamespace(namespace);
        CopyOnWriteArrayList<CacheInvalidationHandler> namespaceHandlers =
            handlers.computeIfAbsent(namespace, ignored -> new CopyOnWriteArrayList<>());
        namespaceHandlers.add(handler);
        return () -> namespaceHandlers.remove(handler);
    }

    /**
     * Invalidates one key on every subscribed node.
     *
     * @param namespace cache namespace
     * @param key       raw key, retained only in this process
     */
    public void invalidate(String namespace, Object key) {
        publish(new CacheInvalidationMessage(
            UUID.randomUUID().toString(), namespace, CacheInvalidationAction.KEY, fingerprint(key)));
    }

    /**
     * Clears one namespace on every subscribed node.
     *
     * @param namespace cache namespace
     */
    public void clear(String namespace) {
        publish(new CacheInvalidationMessage(
            UUID.randomUUID().toString(), namespace, CacheInvalidationAction.ALL, null));
    }

    /**
     * Produces the stable fingerprint used in cluster messages.
     *
     * @param key raw local key
     * @return lowercase SHA-256 fingerprint
     */
    public static String fingerprint(Object key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(String.valueOf(key).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private void publish(CacheInvalidationMessage message) {
        validateNamespace(message.namespace());
        apply(message);
        try {
            long expectedAcknowledgements = transport.publish(message);
            if (expectedAcknowledgements <= 0) {
                throw new ClusterCacheInvalidationException(
                    "Cache invalidation was published without an active subscriber");
            }
            if (!transport.awaitAcknowledgements(
                message.requestId(), expectedAcknowledgements, acknowledgementTimeout)) {
                throw new ClusterCacheInvalidationException(
                    "Cache invalidation acknowledgements timed out for namespace " + message.namespace());
            }
        } catch (ClusterCacheInvalidationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ClusterCacheInvalidationException(
                "Cache invalidation failed for namespace " + message.namespace(), e);
        } finally {
            transport.clearAcknowledgements(message.requestId());
        }
    }

    private void receive(CacheInvalidationMessage message) {
        apply(message);
        transport.acknowledge(message.requestId(), nodeId);
    }

    private void apply(CacheInvalidationMessage message) {
        CopyOnWriteArrayList<CacheInvalidationHandler> namespaceHandlers = handlers.get(message.namespace());
        if (namespaceHandlers == null) {
            return;
        }
        try {
            for (CacheInvalidationHandler handler : namespaceHandlers) {
                if (message.action() == CacheInvalidationAction.ALL) {
                    handler.clear();
                } else {
                    handler.invalidate(message.keyFingerprint());
                }
            }
        } catch (RuntimeException e) {
            throw new ClusterCacheInvalidationException(
                "Local cache invalidation failed for namespace " + message.namespace(), e);
        }
    }

    private static void validateNamespace(String namespace) {
        if (namespace == null || !namespace.matches(NAMESPACE_PATTERN)) {
            throw new IllegalArgumentException("Invalid cache invalidation namespace");
        }
    }

    @Override
    public void close() {
        subscription.close();
        handlers.clear();
    }
}
