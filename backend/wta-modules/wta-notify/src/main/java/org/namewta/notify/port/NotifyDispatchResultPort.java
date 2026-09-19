package org.namewta.notify.port;

import org.namewta.notify.domain.entity.NotifyOutbox;

/** Provider 调用结束后的短事务入口；过期租约不写入，部分持久化失败整笔回滚。 */
public interface NotifyDispatchResultPort {
    /** 锁后重新校验数据库时间并续租，返回 false 时禁止开始外部投递。 */
    boolean renew(NotifyOutbox lease);

    /**
     * 提交一次投递结果。
     * @param lease 本次领取的 owner/token
     * @param result 事务外调用的结果快照
     */
    void complete(NotifyOutbox lease, Result result);

    /**
     * 完成一次无需外部调用的编排。
     * @param lease 本次领取的租约
     * @param disposition 无 Provider 调用时的编排结果
     */
    void settle(NotifyOutbox lease, Disposition disposition);

    /** 锁定意图后以当前投递行重算聚合，调用方不可再用旧快照覆盖结果。 */
    void refreshAggregate(Long intentId);

    /** 仅用于本模块写回边界，不携带收件地址、正文或供应商凭据。 */
    record Result(String status, String providerKey, String providerMessageId,
                  String errorCode, String errorMessage, long costTime) { }

    /** 无外部投递时分别延后、按编排取消或关闭已失效任务。 */
    enum Disposition { WAIT, SKIP, CLOSE }
}
