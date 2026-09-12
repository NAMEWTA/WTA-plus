package org.namewta.common.redis.cache;

import org.redisson.api.RSetCache;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * Redisson pub/sub transport with per-request node acknowledgements.
 */
public class RedissonCacheInvalidationTransport implements CacheInvalidationTransport {

    private static final String CHANNEL = "namewta:cache:invalidation:v1";
    private static final String ACKNOWLEDGEMENT_PREFIX = "namewta:cache:invalidation:ack:";
    private static final Duration ACKNOWLEDGEMENT_TTL = Duration.ofSeconds(30);
    private static final long POLL_INTERVAL_NANOS = Duration.ofMillis(10).toNanos();

    private final RedissonClient redissonClient;
    private final JsonMapper jsonMapper;
    private final String channel;
    private final String acknowledgementPrefix;

    public RedissonCacheInvalidationTransport(RedissonClient redissonClient, JsonMapper jsonMapper) {
        this(redissonClient, jsonMapper, CHANNEL, ACKNOWLEDGEMENT_PREFIX);
    }

    RedissonCacheInvalidationTransport(RedissonClient redissonClient, JsonMapper jsonMapper,
                                       String channel, String acknowledgementPrefix) {
        this.redissonClient = redissonClient;
        this.jsonMapper = jsonMapper;
        this.channel = channel;
        this.acknowledgementPrefix = acknowledgementPrefix;
    }

    @Override
    public Subscription subscribe(Consumer<CacheInvalidationMessage> listener) {
        RTopic topic = redissonClient.getTopic(channel);
        int listenerId = topic.addListener(String.class, (channel, payload) ->
            listener.accept(jsonMapper.readValue(payload, CacheInvalidationMessage.class)));
        return () -> topic.removeListener(listenerId);
    }

    @Override
    public long publish(CacheInvalidationMessage message) {
        return redissonClient.getTopic(channel).publish(jsonMapper.writeValueAsString(message));
    }

    @Override
    public void acknowledge(String requestId, String nodeId) {
        acknowledgements(requestId).add(nodeId, ACKNOWLEDGEMENT_TTL.toSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public boolean awaitAcknowledgements(String requestId, long expected, Duration timeout) {
        RSetCache<String> acknowledgements = acknowledgements(requestId);
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (acknowledgements.size() >= expected) {
                return true;
            }
            LockSupport.parkNanos(POLL_INTERVAL_NANOS);
        }
        return acknowledgements.size() >= expected;
    }

    @Override
    public void clearAcknowledgements(String requestId) {
        acknowledgements(requestId).delete();
    }

    private RSetCache<String> acknowledgements(String requestId) {
        return redissonClient.getSetCache(acknowledgementPrefix + requestId);
    }
}
