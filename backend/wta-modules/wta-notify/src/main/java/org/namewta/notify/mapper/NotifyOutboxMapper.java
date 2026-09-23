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
              @Param("now") LocalDateTime now, @Param("inApp") boolean inApp);

    /** 为同一有效租约原子预留一次站内投递预算；与结果事务分开才能保留回滚次数。 */
    int reserveInApp(@Param("outboxId") Long outboxId, @Param("owner") String owner,
                     @Param("token") String token);

    /** 数据库 UTC 时钟，用于锁后租约判断。 */
    LocalDateTime databaseNow();

    /** 仅续期仍有效的 owner/token，过期 owner 返回零行。 */
    int renew(@Param("outboxId") Long outboxId, @Param("owner") String owner, @Param("token") String token);

    int finish(@Param("outboxId") Long outboxId, @Param("owner") String owner,
               @Param("token") String token, @Param("status") String status,
               @Param("attemptCount") Integer attemptCount, @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
               @Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage);

    /** Reuse the terminal outbox row for a manual retry instead of creating a duplicate delivery row. */
    int requeue(@Param("deliveryId") Long deliveryId, @Param("now") LocalDateTime now);
}
