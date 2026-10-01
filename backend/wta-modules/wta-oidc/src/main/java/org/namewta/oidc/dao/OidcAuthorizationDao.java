package org.namewta.oidc.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.domain.OidcAuthorization;
import org.namewta.oidc.mapper.OidcAuthorizationMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** 授权原子消费、最终签发与撤销全部在主库决定。 */
@Repository
@RequiredArgsConstructor
@DS("master")
public class OidcAuthorizationDao {
    private final OidcAuthorizationMapper mapper;

    /** 有界物理清理过期授权，状态无关，稳定身份记录不受影响。 */
    public int purgeExpired(LocalDateTime cutoff, int limit) {
        return mapper.purgeExpired(cutoff, Math.min(500, Math.max(1, limit)));
    }

    /** 读取标识对应的当前持久记录，不存在时返回空。 */
    public OidcAuthorization find(String frameworkId) {
        return mapper.selectOne(
                new LambdaQueryWrapper<OidcAuthorization>()
                        .eq(OidcAuthorization::getFrameworkId, frameworkId));
    }

    /** 通过凭据摘要查询授权，实时校验应用和账户状态。 */
    public OidcAuthorization token(String hash) {
        return mapper.selectOne(
                new LambdaQueryWrapper<OidcAuthorization>()
                        .and(
                                q ->
                                        q.eq(OidcAuthorization::getCodeHash, hash)
                                                .or()
                                                .eq(OidcAuthorization::getAccessTokenHash, hash)
                                                .or()
                                                .eq(OidcAuthorization::getIdTokenHash, hash)));
    }

    /** 写入新建领域记录并保留数据库唯一约束。 */
    public void insert(OidcAuthorization row) {
        mapper.insert(row);
    }

    /** 原子消费未过期的一次性凭据，失败时不得恢复可用状态。 */
    public boolean consume(String hash) {
        return mapper.update(
                        null,
                        new LambdaUpdateWrapper<OidcAuthorization>()
                                .eq(OidcAuthorization::getCodeHash, hash)
                                .eq(OidcAuthorization::getCodeConsumed, false)
                                .eq(OidcAuthorization::getStatus, "ACTIVE")
                                .gt(
                                        OidcAuthorization::getCodeExpiresAt,
                                        LocalDateTime.now(ZoneOffset.UTC))
                                .set(OidcAuthorization::getCodeConsumed, true)
                                .set(OidcAuthorization::getStatus, "PENDING")
                                .setSql("version=version+1"))
                == 1;
    }

    /** 完成最终状态校验或退出确认，拒绝复活已撤销授权。 */
    public boolean finish(OidcAuthorization row) {
        return mapper.update(
                        null,
                        new LambdaUpdateWrapper<OidcAuthorization>()
                                .eq(OidcAuthorization::getAuthorizationId, row.getAuthorizationId())
                                .eq(OidcAuthorization::getVersion, row.getVersion())
                                .eq(OidcAuthorization::getStatus, "PENDING")
                                .set(OidcAuthorization::getStatus, "ACTIVE")
                                .set(
                                        OidcAuthorization::getAccessTokenHash,
                                        row.getAccessTokenHash())
                                .set(
                                        OidcAuthorization::getAccessExpiresAt,
                                        row.getAccessExpiresAt())
                                .set(OidcAuthorization::getIdTokenHash, row.getIdTokenHash())
                                .set(
                                        OidcAuthorization::getAuthorizationJson,
                                        row.getAuthorizationJson())
                                .setSql("version=version+1"))
                == 1;
    }

    /** 持久撤销指定授权，状态变化增加乐观版本。 */
    public void revoke(String id) {
        mapper.update(
                null,
                new LambdaUpdateWrapper<OidcAuthorization>()
                        .eq(OidcAuthorization::getFrameworkId, id)
                        .ne(OidcAuthorization::getStatus, "REVOKED")
                        .set(OidcAuthorization::getStatus, "REVOKED")
                        .setSql("version=version+1"));
    }

    /** 持久撤销一个应用下的全部现存授权。 */
    public void revokeApplication(Long app) {
        mapper.update(
                null,
                new LambdaUpdateWrapper<OidcAuthorization>()
                        .eq(OidcAuthorization::getApplicationId, app)
                        .ne(OidcAuthorization::getStatus, "REVOKED")
                        .set(OidcAuthorization::getStatus, "REVOKED")
                        .setSql("version=version+1"));
    }

    /** 已实际签出ID Token的RP需要接收退出，撤销过的授权也保留关联事实。 */
    public java.util.List<OidcAuthorization> sessionGrants(String hash) {
        return mapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                OidcAuthorization>()
                        .eq(OidcAuthorization::getSessionHash, hash)
                        .isNotNull(OidcAuthorization::getIdTokenHash));
    }

    /** 持久撤销当前 SSO 会话关联的授权。 */
    public void revokeSession(String hash) {
        mapper.update(
                null,
                new LambdaUpdateWrapper<OidcAuthorization>()
                        .eq(OidcAuthorization::getSessionHash, hash)
                        .set(OidcAuthorization::getSessionClosed, true)
                        .set(OidcAuthorization::getStatus, "REVOKED")
                        .setSql("version=version+1"));
    }
}
