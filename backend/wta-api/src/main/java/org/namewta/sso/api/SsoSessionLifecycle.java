package org.namewta.sso.api;

/** 中央会话锁与撤销扩展；仅可信模块内部调用，不提供外部数据库访问。 */
public interface SsoSessionLifecycle {
    /** 在调用方数据库事务内锁定有效会话，与全局退出串行；无效时失败。 */
    void requireActiveLocked(String sessionId);

    /** 管理维护用：是否仍有未撤销且未过期的中央会话。 */
    boolean hasActiveSessions();

    /** 管理维护用：已停止新认证时结束所有中央会话并预约关联App退出。 */
    void revokeAllSessions();
}
