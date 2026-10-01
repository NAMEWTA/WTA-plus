package org.namewta.sso.api;

/** 提供中央会话的可信认证时间；不签发业务会话，也不授予业务 Client 权限。 */
public interface SsoSessionAccess {
    /**
     * 读取具有原始认证时间的有效中央会话。
     *
     * @param sessionId 不可信 Cookie 会话标识
     * @return 有效快照；过期、不存在或旧会话没有原始认证时间时返回 null，要求重新认证
     */
    SsoSessionSnapshot current(String sessionId);

    /**
     * 注销指定中央会话并持久预约全部关联应用退出，幂等；远端退出可能异步重试。
     *
     * @param sessionId 中央会话标识
     */
    void logout(String sessionId);
}
