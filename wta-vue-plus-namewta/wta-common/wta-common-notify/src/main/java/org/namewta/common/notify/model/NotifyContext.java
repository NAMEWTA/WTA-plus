package org.namewta.common.notify.model;

/**
 * 发送线程的审计上下文快照。
 *
 * @param userId   当前用户
 * @param clientPk 请求来源 sys_client.id，仅审计记录
 * @param traceId  链路标识
 */
public record NotifyContext(Long userId, Long clientPk, String traceId) {

    public static NotifyContext empty() {
        return new NotifyContext(null, null, null);
    }
}
