package org.namewta.notify.port;

import org.namewta.notify.api.InAppNotificationPort;
import org.namewta.notify.domain.entity.NotifyOutbox;

/** Provider 调用结束后的短事务入口；过期租约不写入，部分持久化失败整笔回滚。 */
public interface NotifyDispatchResultPort {
    /** 锁后重新校验数据库时间并续租，返回 false 时禁止开始外部投递。 */
    boolean renew(NotifyOutbox lease);

    /**
     * 在短事务中核对计划/截止与本次领取来源；到期或外部重领不确定时原子收敛结果。
     *
     * @param lease 带原领取状态及 owner/token 的任务
     * @return 仍可开始下一步本地编排或外部发送时为 true
     */
    boolean deadlineGate(NotifyOutbox lease);

    /**
     * 提交一次投递结果。
     * @param lease 本次领取的 owner/token
     * @param result 事务外调用的结果快照
     */
    void complete(NotifyOutbox lease, Result result);

    /**
     * 在独立短事务中占用一次站内投递预算；同一租约只允许占用一次。
     * @param lease 当前 owner/token 的租约
     * @return 已占用预算时为 true；失效、重复或预算耗尽时为 false
     */
    boolean beginInAppAttempt(NotifyOutbox lease);

    /**
     * 在同一事务写入站内消息、收件关系和投递结果；提交失败须由调用方保守处理。
     * @param lease 已预留预算的租约
     * @param port 本地收件箱持久化端口，不得在此执行外部供应商 I/O
     * @param snapshot 意图的站内内容快照
     * @param userId 当前投递所属用户
     * @param costTime 毫秒耗时
     */
    void completeInApp(NotifyOutbox lease, InAppNotificationPort port,
                       InAppNotificationPort.InAppSnapshot snapshot, Long userId, long costTime);

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
