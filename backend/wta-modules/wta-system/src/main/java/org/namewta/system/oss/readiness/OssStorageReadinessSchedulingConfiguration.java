package org.namewta.system.oss.readiness;

import org.springframework.context.annotation.Configuration;

/**
 * OSS 诊断不自行调度；全应用调度由启动类保持，用于 Notify 等业务任务。
 */
@Configuration(proxyBeanMethods = false)
public class OssStorageReadinessSchedulingConfiguration {
}
