package org.namewta.notify.port;

/** 已提交站内事实的在线提示入口；事件适配器仅依赖此端口。 */
public interface InAppCommittedPushPort {
    /** 以持久主键重新读取本人消息，再发出可丢失的在线提示。 */
    void push(Long messageId, Long userId);
}
