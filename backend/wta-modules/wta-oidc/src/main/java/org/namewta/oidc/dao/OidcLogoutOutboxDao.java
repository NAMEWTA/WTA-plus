package org.namewta.oidc.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.domain.PageResult;
import org.namewta.oidc.domain.OidcLogoutOutbox;
import org.namewta.oidc.mapper.OidcLogoutOutboxMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** CAS租约保证多节点只由当前持有者更新结果；网络I/O不在事务内。 */
@Repository
@DS("master")
@RequiredArgsConstructor
public class OidcLogoutOutboxDao {
    private final OidcLogoutOutboxMapper mapper;

    /** 同一中央会话同一RP只预约一次，调用方已持有中央会话锁。 */
    public void enqueue(OidcLogoutOutbox row) {
        if (mapper.selectCount(
                        new LambdaQueryWrapper<OidcLogoutOutbox>()
                                .eq(OidcLogoutOutbox::getApplicationId, row.getApplicationId())
                                .eq(OidcLogoutOutbox::getSessionHash, row.getSessionHash()))
                == 0) mapper.insert(row);
    }

    /** 领取一个到期或租约过期任务，失败由下一轮重试。 */
    public OidcLogoutOutbox claim(String lease) {
        var now = LocalDateTime.now(ZoneOffset.UTC);
        var row =
                mapper.selectOne(
                        new LambdaQueryWrapper<OidcLogoutOutbox>()
                                .and(
                                        q ->
                                                q.eq(OidcLogoutOutbox::getStatus, "READY")
                                                        .le(OidcLogoutOutbox::getNextAttemptAt, now)
                                                        .or(
                                                                r ->
                                                                        r.eq(
                                                                                        OidcLogoutOutbox
                                                                                                ::getStatus,
                                                                                        "DELIVERING")
                                                                                .lt(
                                                                                        OidcLogoutOutbox
                                                                                                ::getLeaseUntil,
                                                                                        now)))
                                .orderByAsc(OidcLogoutOutbox::getLogoutOutboxId)
                                .last("LIMIT 1"));
        if (row == null) return null;
        int n =
                mapper.update(
                        null,
                        new LambdaUpdateWrapper<OidcLogoutOutbox>()
                                .eq(OidcLogoutOutbox::getLogoutOutboxId, row.getLogoutOutboxId())
                                .eq(OidcLogoutOutbox::getVersion, row.getVersion())
                                .set(OidcLogoutOutbox::getStatus, "DELIVERING")
                                .set(OidcLogoutOutbox::getLeaseToken, lease)
                                .set(OidcLogoutOutbox::getLeaseUntil, now.plusSeconds(30))
                                .setSql("attempts=attempts+1,version=version+1"));
        return n == 1 ? mapper.selectById(row.getLogoutOutboxId()) : null;
    }

    /** 当前租约持有者才能结束或重新排队，旧响应不能覆盖新租约。 */
    public boolean result(OidcLogoutOutbox row, String state, String error, LocalDateTime next) {
        return mapper.update(
                        null,
                        new LambdaUpdateWrapper<OidcLogoutOutbox>()
                                .eq(OidcLogoutOutbox::getLogoutOutboxId, row.getLogoutOutboxId())
                                .eq(OidcLogoutOutbox::getStatus, "DELIVERING")
                                .eq(OidcLogoutOutbox::getLeaseToken, row.getLeaseToken())
                                .gt(
                                        OidcLogoutOutbox::getLeaseUntil,
                                        LocalDateTime.now(ZoneOffset.UTC))
                                .set(OidcLogoutOutbox::getStatus, state)
                                .set(OidcLogoutOutbox::getLastError, error)
                                .set(OidcLogoutOutbox::getNextAttemptAt, next)
                                .set(OidcLogoutOutbox::getLeaseToken, null)
                                .set(OidcLogoutOutbox::getLeaseUntil, null)
                                .setSql("version=version+1"))
                == 1;
    }

    /** 维护切换发行方前必须完成旧发行方退出。 */
    public boolean unfinished() {
        return mapper.selectCount(
                        new LambdaQueryWrapper<OidcLogoutOutbox>()
                                .ne(OidcLogoutOutbox::getStatus, "DONE"))
                > 0;
    }

    /** 管理者可显式重排失败任务；进行中的投递不能被抢占。 */
    public int retry(Long id) {
        return mapper.update(
                null,
                new LambdaUpdateWrapper<OidcLogoutOutbox>()
                        .eq(OidcLogoutOutbox::getLogoutOutboxId, id)
                        .eq(OidcLogoutOutbox::getStatus, "FAILED")
                        .set(OidcLogoutOutbox::getStatus, "READY")
                        .set(OidcLogoutOutbox::getAttempts, 0)
                        .set(OidcLogoutOutbox::getNextAttemptAt, LocalDateTime.now(ZoneOffset.UTC))
                        .set(OidcLogoutOutbox::getLastError, null)
                        .setSql("version=version+1"));
    }

    /** 有界分页查看状态。 */
    public PageResult<OidcLogoutOutbox> page(int page, int size) {
        var data =
                mapper.selectPage(
                        new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size))),
                        new LambdaQueryWrapper<OidcLogoutOutbox>()
                                .orderByDesc(OidcLogoutOutbox::getLogoutOutboxId));
        return PageResult.build(data.getRecords(), data.getTotal());
    }
}
