package org.namewta.oidc.service;

import lombok.RequiredArgsConstructor;

import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.oidc.dao.OidcAuthorizationDao;
import org.namewta.oidc.domain.OidcAuthorization;
import org.springframework.stereotype.Service;

/** 授权持久事实服务，不编排其他同层服务。 */
@Service
@RequiredArgsConstructor
public class OidcAuthorizationPersistenceService {
    private final OidcAuthorizationDao dao;

    /** 授权会话过期后保留一天，再按索引每次删除最多五百条。 */
    public int purgeExpired() {
        return dao.purgeExpired(
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(1), 500);
    }

    /** 读取标识对应的当前持久记录，不存在时返回空。 */
    public OidcAuthorization find(String id) {
        return dao.find(id);
    }

    /** 通过凭据摘要查询授权，实时校验应用和账户状态。 */
    public OidcAuthorization token(String hash) {
        return dao.token(hash);
    }

    /** 写入新建领域记录并保留数据库唯一约束。 */
    public void insert(OidcAuthorization row) {
        row.setAuthorizationId(IdGeneratorUtil.nextLongId());
        dao.insert(row);
    }

    /** 原子消费未过期的一次性凭据，失败时不得恢复可用状态。 */
    public boolean consume(String hash) {
        return dao.consume(hash);
    }

    /** 完成最终状态校验或退出确认，拒绝复活已撤销授权。 */
    public boolean finish(OidcAuthorization row) {
        return dao.finish(row);
    }

    /** 持久撤销指定授权，状态变化增加乐观版本。 */
    public void revoke(String id) {
        dao.revoke(id);
    }

    /** 持久撤销一个应用下的全部现存授权。 */
    public void revokeApplication(Long id) {
        dao.revokeApplication(id);
    }

    /** 返回中央会话已登录的RP授权，供退出事务登记投递。 */
    public java.util.List<OidcAuthorization> sessionGrants(String hash) {
        return dao.sessionGrants(hash);
    }

    /** 持久撤销当前 SSO 会话关联的授权。 */
    public void revokeSession(String hash) {
        dao.revokeSession(hash);
    }
}
