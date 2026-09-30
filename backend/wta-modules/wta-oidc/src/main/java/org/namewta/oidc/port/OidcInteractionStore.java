package org.namewta.oidc.port;

/** 短期认证加密交互的外部存储端口。 */
public interface OidcInteractionStore {
    /** 以明确有效期写入短期密文上下文。 */
    void put(String key, String value, long seconds);

    /** 读取有界存储中的短期密文上下文。 */
    String get(String key);

    /** 删除短期上下文或撤销指定授权，不影响其他授权。 */
    boolean remove(String key, String expected);
}
