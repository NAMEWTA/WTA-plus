package org.namewta.oidc.service;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.oidc.dao.OidcLogoutOutboxDao;
import org.namewta.oidc.domain.OidcApplication;
import org.namewta.oidc.domain.OidcAuthorization;
import org.namewta.oidc.domain.OidcLogoutOutbox;
import org.namewta.oidc.domain.vo.OidcLogoutDeliveryVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** 标准退出预约、租约和退避规则。 */
@Service
@RequiredArgsConstructor
public class OidcLogoutService {
    private final OidcLogoutOutboxDao dao;

    /** 在中央会话撤销事务内保存完整RP目标快照。 */
    public void enqueue(
            String issuer, OidcAuthorization authorization, OidcApplication application) {
        if (application == null
                || application.getBackchannelLogoutUri() == null
                || application.getBackchannelLogoutUri().isBlank()) return;
        var row = new OidcLogoutOutbox();
        row.setLogoutOutboxId(IdGeneratorUtil.nextLongId());
        row.setApplicationId(application.getApplicationId());
        row.setClientId(application.getClientId());
        row.setIssuer(issuer);
        row.setSessionHash(authorization.getSessionHash());
        row.setSubject(authorization.getSubject());
        row.setTargetUri(application.getBackchannelLogoutUri());
        row.setStatus("READY");
        row.setAttempts(0);
        row.setNextAttemptAt(LocalDateTime.now(ZoneOffset.UTC));
        row.setVersion(0);
        row.setDelFlag("0");
        dao.enqueue(row);
    }

    /** 领取一个任务。 */
    public OidcLogoutOutbox claim() {
        return dao.claim(java.util.UUID.randomUUID().toString());
    }

    /** 保存稳定错误码，禁止将网络异常正文/凭据写入管理响应。 */
    public void result(OidcLogoutOutbox row, int status) {
        boolean success = status == 200 || status == 204;
        boolean retry = status == 0 || status == 429 || status >= 500;
        String state = success ? "DONE" : retry && row.getAttempts() < 12 ? "READY" : "FAILED";
        long delay = Math.min(3600, 5L << Math.min(row.getAttempts(), 10));
        dao.result(
                row,
                state,
                success ? null : status == 0 ? "NETWORK_ERROR" : "HTTP_" + status,
                LocalDateTime.now(ZoneOffset.UTC).plusSeconds(delay));
    }

    /** 只有失败任务允许显式重试，不重发已确认完成的退出。 */
    public void retry(Long id) {
        if (dao.retry(id) != 1) throw new ServiceException("仅失败投递可以重试");
    }

    /** 返回是否存在尚未送达的退出；维护切换时不能丢弃旧任务。 */
    public boolean unfinished() {
        return dao.unfinished();
    }

    /** 管理响应只包含投递状态。 */
    public PageResult<OidcLogoutDeliveryVo> page(int page, int size) {
        var data = dao.page(page, size);
        return PageResult.build(
                data.getRows().stream()
                        .map(
                                r ->
                                        new OidcLogoutDeliveryVo(
                                                r.getLogoutOutboxId().toString(),
                                                r.getClientId(),
                                                r.getStatus(),
                                                r.getAttempts(),
                                                r.getNextAttemptAt(),
                                                r.getLastError(),
                                                r.getCreateTime()))
                        .toList(),
                data.getTotal());
    }
}
