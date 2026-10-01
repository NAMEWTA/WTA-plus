package org.namewta.sso.api;

/** 同一主库事务中的持久撤销参与者；不得在此回调执行网络请求。 */
public interface SsoSessionRevocationParticipant {
    /** 撤销该中央会话关联的协议授权并持久预约投递；重复调用须幂等。 */
    void revoke(String sessionHash);
}
