package org.namewta.common.liteflow.core;

/**
 * LiteFlow 失败提示提供者。
 */
public interface FailMessageProvider {

    /**
     * 获取公共失败节点抛出的业务提示。
     *
     * @return 失败提示
     */
    String getFailMessage();

}
