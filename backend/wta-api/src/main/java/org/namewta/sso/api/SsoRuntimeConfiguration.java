package org.namewta.sso.api;

/** 供管理聚合页使用的中央会话配置边界，调用方负责管理权限与外层事务。 */
public interface SsoRuntimeConfiguration {
    /** 返回本进程实际生效的结构和当前动态参数。 */
    SsoRuntimeSettings current();

    /** 返回持久化目标配置；结构变更可能等待维护重启。 */
    SsoRuntimeSettings saved();

    /** 校验并保存目标配置；结构变化只允许在服务停用期间。 */
    void save(SsoRuntimeSettings settings);
}
