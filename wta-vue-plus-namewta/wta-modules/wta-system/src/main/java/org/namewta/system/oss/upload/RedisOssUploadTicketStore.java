package org.namewta.system.oss.upload;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Redis UploadTicket、补偿记录和到期索引。
 */
@Component
public class RedisOssUploadTicketStore implements OssUploadTicketStore {

    private static final String TICKET_PREFIX = "oss:upload:ticket:";
    private static final String CLEANUP_PREFIX = "oss:upload:cleanup:";
    private static final String LOCK_PREFIX = "oss:upload:lock:";
    private static final String EXPIRE_INDEX = "oss:upload:expire-index";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RedissonClient client;

    public RedisOssUploadTicketStore(RedissonClient client) {
        this.client = client;
    }

    @Override
    public void create(OssUploadTicket ticket, OssUploadCleanupRecord cleanup, Duration ticketTtl,
                       Duration cleanupTtl) {
        RBucket<String> ticketBucket = client.getBucket(TICKET_PREFIX + ticket.token());
        if (!ticketBucket.setIfAbsent(JSON.writeValueAsString(ticket), ticketTtl)) {
            throw new OssUploadException(OssUploadError.STATE_STORE_FAILURE, "上传 Token 冲突");
        }
        try {
            client.<String>getBucket(CLEANUP_PREFIX + ticket.token())
                .set(JSON.writeValueAsString(cleanup), cleanupTtl);
            index().add(ticket.expiresAt(), ticket.token());
        } catch (RuntimeException e) {
            ticketBucket.delete();
            client.getBucket(CLEANUP_PREFIX + ticket.token()).delete();
            index().remove(ticket.token());
            throw e;
        }
    }

    @Override
    public OssUploadTicket get(String token) {
        String value = client.<String>getBucket(TICKET_PREFIX + token).get();
        return value == null ? null : JSON.readValue(value, OssUploadTicket.class);
    }

    @Override
    public void save(OssUploadTicket ticket, Duration ttl) {
        client.<String>getBucket(TICKET_PREFIX + ticket.token()).set(JSON.writeValueAsString(ticket), ttl);
    }

    @Override
    public void removeCompletedCleanup(String token) {
        client.getBucket(CLEANUP_PREFIX + token).delete();
        index().remove(token);
    }

    @Override
    public void removeSession(String token) {
        client.getBucket(TICKET_PREFIX + token).delete();
        client.getBucket(CLEANUP_PREFIX + token).delete();
        index().remove(token);
    }

    @Override
    public List<String> findExpired(long nowEpochMilli, int limit) {
        return index().valueRange(0, true, nowEpochMilli, true, 0, Math.max(1, limit)).stream().toList();
    }

    @Override
    public OssUploadCleanupRecord getCleanup(String token) {
        String value = client.<String>getBucket(CLEANUP_PREFIX + token).get();
        return value == null ? null : JSON.readValue(value, OssUploadCleanupRecord.class);
    }

    @Override
    public void scheduleCleanup(String token, long whenEpochMilli) {
        index().add(whenEpochMilli, token);
    }

    @Override
    public <T> T locked(String token, Supplier<T> action) {
        RLock lock = client.getLock(LOCK_PREFIX + token);
        lock.lock();
        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private RScoredSortedSet<String> index() {
        return client.getScoredSortedSet(EXPIRE_INDEX);
    }
}
