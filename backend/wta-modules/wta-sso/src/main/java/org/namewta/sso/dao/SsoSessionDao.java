package org.namewta.sso.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import lombok.RequiredArgsConstructor;

import org.namewta.sso.domain.SsoBusinessSession;
import org.namewta.sso.domain.SsoSession;
import org.namewta.sso.mapper.SsoBusinessSessionMapper;
import org.namewta.sso.mapper.SsoSessionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 同一主库会话行协调签发、关联与全退，禁止仅靠Redis锁保证原子性。 */
@Repository
@DS("master")
@RequiredArgsConstructor
public class SsoSessionDao {
    private final SsoSessionMapper sessions;
    private final SsoBusinessSessionMapper business;

    /** 读取会话持久状态。 */
    public SsoSession find(String hash) {
        return sessions.selectOne(
                new LambdaQueryWrapper<SsoSession>().eq(SsoSession::getSessionHash, hash));
    }

    /** 在调用方事务内锁定会话，后续授权登记与退出共用该锁。 */
    public SsoSession lock(String hash) {
        return sessions.selectOne(
                new LambdaQueryWrapper<SsoSession>()
                        .eq(SsoSession::getSessionHash, hash)
                        .last("FOR UPDATE"));
    }

    /** 创建固定认证时间与截止的会话。 */
    public void insert(SsoSession row) {
        sessions.insert(row);
    }

    /** 撤销会话和关联业务票，重试任务以PENDING行恢复Redis副作用。 */
    public void revoke(String hash) {
        sessions.update(
                null,
                new LambdaUpdateWrapper<SsoSession>()
                        .eq(SsoSession::getSessionHash, hash)
                        .eq(SsoSession::getStatus, "ACTIVE")
                        .set(SsoSession::getStatus, "REVOKED")
                        .setSql("version=version+1"));
        business.update(
                null,
                new LambdaUpdateWrapper<SsoBusinessSession>()
                        .eq(SsoBusinessSession::getSessionHash, hash)
                        .eq(SsoBusinessSession::getStatus, "ACTIVE")
                        .set(SsoBusinessSession::getStatus, "PENDING")
                        .setSql("version=version+1"));
    }

    /** 按批次读取待维护撤销的会话，所有更新仍锁定同一会话行。 */
    public List<SsoSession> active(boolean expired) {
        return sessions.selectList(
                new LambdaQueryWrapper<SsoSession>()
                        .eq(SsoSession::getStatus, "ACTIVE")
                        .le(expired, SsoSession::getExpiresAt, java.time.Instant.now())
                        .orderByAsc(SsoSession::getSessionId)
                        .last("LIMIT 100"));
    }

    /** 检查结构切换前是否还有有效中央会话。 */
    public boolean hasActive() {
        return sessions.selectCount(
                        new LambdaQueryWrapper<SsoSession>()
                                .eq(SsoSession::getStatus, "ACTIVE")
                                .gt(SsoSession::getExpiresAt, java.time.Instant.now()))
                > 0;
    }

    /** 同一token幂等登记，调用方已经持有中央会话锁。 */
    public void register(SsoBusinessSession row) {
        if (business.selectCount(
                        new LambdaQueryWrapper<SsoBusinessSession>()
                                .eq(SsoBusinessSession::getTokenHash, row.getTokenHash()))
                == 0) business.insert(row);
    }

    /** 有界返回需撤销的业务票；注销按token天然幂等。 */
    public List<SsoBusinessSession> pending() {
        return business.selectList(
                new LambdaQueryWrapper<SsoBusinessSession>()
                        .eq(SsoBusinessSession::getStatus, "PENDING")
                        .orderByAsc(SsoBusinessSession::getBusinessSessionId)
                        .last("LIMIT 50"));
    }

    /** Redis已完成注销后标记交付，旧任务重复执行无副作用。 */
    public void delivered(Long id) {
        business.update(
                null,
                new LambdaUpdateWrapper<SsoBusinessSession>()
                        .eq(SsoBusinessSession::getBusinessSessionId, id)
                        .eq(SsoBusinessSession::getStatus, "PENDING")
                        .set(SsoBusinessSession::getStatus, "REVOKED")
                        .setSql("version=version+1"));
    }
}
