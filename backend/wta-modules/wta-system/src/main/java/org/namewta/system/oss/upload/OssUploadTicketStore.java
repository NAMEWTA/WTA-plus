package org.namewta.system.oss.upload;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * UploadTicket 原子存储接缝。
 */
public interface OssUploadTicketStore {

    void create(OssUploadTicket ticket, OssUploadCleanupRecord cleanup, Duration ticketTtl, Duration cleanupTtl);

    OssUploadTicket get(String token);

    void save(OssUploadTicket ticket, Duration ttl);

    void removeCompletedCleanup(String token);

    void removeSession(String token);

    List<String> findExpired(long nowEpochMilli, int limit);

    OssUploadCleanupRecord getCleanup(String token);

    void scheduleCleanup(String token, long whenEpochMilli);

    <T> T locked(String token, Supplier<T> action);
}
