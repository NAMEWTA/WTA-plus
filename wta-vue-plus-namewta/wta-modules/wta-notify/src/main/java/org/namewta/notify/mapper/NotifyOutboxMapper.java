package org.namewta.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.namewta.notify.domain.entity.NotifyOutbox;
import java.time.LocalDateTime;

import java.util.List;

/** Outbox Mapper。 */
@Mapper
public interface NotifyOutboxMapper extends BaseMapper<NotifyOutbox> {
    /** 领取到期且可执行的 Outbox 任务。 */
    List<NotifyOutbox> selectClaimable(@Param("now") LocalDateTime now, @Param("limit") int limit);

    int claim(@Param("outboxId") Long outboxId, @Param("owner") String owner,
              @Param("token") String token, @Param("leaseUntil") LocalDateTime leaseUntil,
              @Param("now") LocalDateTime now);

    int renew(@Param("outboxId") Long outboxId, @Param("owner") String owner,
              @Param("token") String token, @Param("leaseUntil") LocalDateTime leaseUntil,
              @Param("now") LocalDateTime now);

    int finish(@Param("outboxId") Long outboxId, @Param("owner") String owner,
               @Param("token") String token, @Param("status") String status,
               @Param("attemptCount") Integer attemptCount, @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
               @Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage);

    /** Reuse the terminal outbox row for a manual retry instead of creating a duplicate delivery row. */
    int requeue(@Param("deliveryId") Long deliveryId, @Param("now") LocalDateTime now);
}
