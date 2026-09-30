package org.namewta.common.core.service;

/** 标准协议适配器声明原文端点与敏感路径；基础设施不得依赖业务模块。 */
public interface HttpProtocolPolicy {
    /** 是否必须保留标准请求与响应原文。 */
    boolean isProtocolPath(String path);

    /** 是否禁止记录请求、响应正文和参数。 */
    boolean isSensitivePath(String path);
}
