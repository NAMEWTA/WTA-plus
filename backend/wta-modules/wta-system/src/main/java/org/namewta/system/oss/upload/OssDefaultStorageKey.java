package org.namewta.system.oss.upload;

/**
 * 当前默认 OSS 配置键。直传不在策略里指定桶，初始化时读取这一把。
 */
public interface OssDefaultStorageKey {

    /**
     * @return 默认配置键；尚未设置时返回空白
     */
    String current();
}
