package org.namewta.system.oss.readiness;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 持续续期 OSS readiness 快照，避免健康配置因快照自然过期而停止服务。
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class OssStorageReadinessSchedulingConfiguration {
}
