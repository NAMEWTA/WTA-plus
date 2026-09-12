package org.namewta.system.oss.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.namewta.common.nacos.NacosConfigAccessor;
import org.namewta.common.nacos.NacosConfigParticipant;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * OSS 临时对象生命周期配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oss.lifecycle")
public class OssLifecycleProperties implements InitializingBean, NacosConfigParticipant<Duration> {

    private static final Pattern POLICY_KEY = Pattern.compile("[a-z][a-z0-9-]{0,63}");

    /** 最后一个引用解除后保留临时对象的时间。 */
    private Duration tempRetention = Duration.ofHours(24);

    /** 下载签名有效期。 */
    private Duration downloadTtl = Duration.ofMinutes(2);

    /** 下载签名允许的最短有效期。 */
    private Duration downloadTtlMin = Duration.ofMinutes(1);

    /** 下载签名允许的最长有效期。 */
    private Duration downloadTtlMax = Duration.ofMinutes(10);

    /** 仅由服务端选择的命名下载策略。 */
    private Map<String, DownloadPolicy> downloadPolicies = new LinkedHashMap<>();

    /** 是否启用定时清理。默认关闭，需审核 dry-run 后显式开启。 */
    private boolean cleanupEnabled = false;

    /** 是否只报告待清理对象而不实际删除。 */
    private boolean cleanupDryRun = true;

    /** 每批扫描上限。 */
    private int cleanupBatchSize = 100;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private transient volatile NacosConfigAccessor nacosConfigAccessor;

    @Override
    public String id() {
        return "oss-download-ttl";
    }

    @Override
    public Set<String> prefixes() {
        return Set.of();
    }

    @Override
    public Set<String> exactKeys() {
        return Set.of("oss.lifecycle.download-ttl");
    }

    @Override
    public Duration prepare(Binder binder) {
        Duration candidate = binder.bind("oss.lifecycle.download-ttl", Duration.class).orElse(downloadTtl);
        requireDownloadTtl(candidate, "默认下载签名 TTL");
        return candidate;
    }

    @Autowired(required = false)
    public void setNacosConfigAccessor(NacosConfigAccessor nacosConfigAccessor) {
        this.nacosConfigAccessor = nacosConfigAccessor;
    }

    public Duration getDownloadTtl() {
        NacosConfigAccessor accessor = nacosConfigAccessor;
        return accessor == null
            ? downloadTtl
            : accessor.configuration(id(), Duration.class).orElse(downloadTtl);
    }

    @Override
    public void afterPropertiesSet() {
        validate();
    }

    /**
     * 校验生命周期与私有下载配置。
     */
    public void validate() {
        if (!isPositive(downloadTtlMin) || !isPositive(downloadTtlMax)
            || downloadTtlMin.compareTo(downloadTtlMax) > 0) {
            invalid("下载签名 TTL 安全边界无效");
        }
        requireDownloadTtl(downloadTtl, "默认下载签名 TTL");
        if (downloadPolicies == null) {
            invalid("命名下载策略不能为空");
        }
        downloadPolicies.forEach((name, policy) -> {
            if (name == null || !POLICY_KEY.matcher(name).matches() || policy == null) {
                invalid("命名下载策略名称无效: " + name);
            }
            requireDownloadTtl(policy.ttl, "命名下载策略 TTL: " + name);
        });
        if (!isPositive(tempRetention) || cleanupBatchSize < 1 || cleanupBatchSize > 1000) {
            invalid("生命周期清理配置无效");
        }
    }

    /**
     * 解析服务端命名下载策略；空策略名保持原有 2 分钟默认行为。
     *
     * @param policyName 命名策略，空值表示默认策略
     * @return 经过安全边界校验的签名有效期
     */
    public Duration resolveDownloadTtl(String policyName) {
        if (policyName == null || policyName.isBlank()) {
            return getDownloadTtl();
        }
        DownloadPolicy policy = downloadPolicies.get(policyName);
        if (policy == null || !policy.enabled) {
            throw new IllegalStateException("下载策略不存在或未启用: " + policyName);
        }
        return policy.ttl;
    }

    private void requireDownloadTtl(Duration value, String name) {
        if (!isPositive(value) || value.compareTo(downloadTtlMin) < 0 || value.compareTo(downloadTtlMax) > 0) {
            invalid(name + " 超出安全范围");
        }
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }

    private void invalid(String message) {
        throw new IllegalStateException(message);
    }

    /**
     * 单个服务端命名私有下载策略。
     */
    @Data
    public static class DownloadPolicy {

        private boolean enabled = true;
        private Duration ttl;
    }
}
