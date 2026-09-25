package org.namewta.system.oss.upload;

import lombok.Data;
import org.namewta.common.oss.enums.AccessPolicy;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 浏览器直传策略。配置错误必须在启动阶段暴露。
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "oss.direct-upload")
public class OssUploadProperties implements InitializingBean {

    public static final long MIN_PART_SIZE = 5L * 1024 * 1024;
    public static final long MAX_PART_SIZE = 5L * 1024 * 1024 * 1024;
    private static final Pattern POLICY_KEY = Pattern.compile("[a-z][a-z0-9-]{0,63}");
    private static final Pattern OBJECT_PREFIX = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9/_-]{0,127}");
    private Duration ticketTtl = Duration.ofHours(24);
    private Duration presignTtl = Duration.ofMinutes(5);
    private Duration cleanupRecordTtl = Duration.ofDays(7);
    private int maxSignParts = 20;
    private int cleanupBatchSize = 100;
    private boolean cleanupEnabled;
    private boolean cleanupDryRun = true;
    private Map<String, Policy> policies = new LinkedHashMap<>();

    @Override
    public void afterPropertiesSet() {
        validate();
    }

    public void validate() {
        requireDuration(ticketTtl, Duration.ofMinutes(5), Duration.ofDays(7), "ticketTtl");
        requireDuration(presignTtl, Duration.ofMinutes(1), Duration.ofMinutes(30), "presignTtl");
        requireDuration(cleanupRecordTtl, ticketTtl, Duration.ofDays(30), "cleanupRecordTtl");
        if (maxSignParts < 1 || maxSignParts > 100 || cleanupBatchSize < 1 || cleanupBatchSize > 1000) {
            invalid("签名窗口或清理批次超出安全范围");
        }
        if (policies == null || policies.isEmpty()) {
            invalid("至少需要一个命名 uploadPolicy");
        }
        policies.forEach(this::validatePolicy);
    }

    public Policy requirePolicy(String key) {
        Policy policy = policies.get(key);
        if (policy == null || !policy.isEnabled()) {
            throw new OssUploadException(OssUploadError.INVALID_POLICY, "上传策略不存在或未启用: " + key);
        }
        return policy;
    }

    private void validatePolicy(String key, Policy policy) {
        if (key == null || !POLICY_KEY.matcher(key).matches() || policy == null) {
            invalid("uploadPolicy 名称无效: " + key);
        }
        if (policy.maxSize < 1 || policy.allowedContentTypes == null || policy.allowedContentTypes.isEmpty()) {
            invalid("uploadPolicy 必须声明 maxSize 和 allowedContentTypes: " + key);
        }
        if (policy.expectedAccessPolicy == null) {
            invalid("uploadPolicy 必须声明 expectedAccessPolicy: " + key);
        }
        if (policy.objectPrefix == null || !OBJECT_PREFIX.matcher(policy.objectPrefix).matches()
            || policy.objectPrefix.contains("..") || policy.objectPrefix.startsWith("/")
            || policy.objectPrefix.endsWith("/")) {
            invalid("uploadPolicy objectPrefix 不安全: " + key);
        }
        if (policy.mode == null) {
            invalid("uploadPolicy 必须声明 mode: " + key);
        }
        // SINGLE 不使用分片参数；省略时不得因为默认分片值被当成无效配置。
        if (policy.mode != OssUploadMode.SINGLE) {
            if (policy.partSize < MIN_PART_SIZE || policy.partSize > MAX_PART_SIZE
                || policy.multipartThreshold < 1) {
                invalid("uploadPolicy Multipart 参数无效: " + key);
            }
            long partCount = ceilDiv(policy.maxSize, policy.partSize);
            if (partCount > 10_000) {
                invalid("uploadPolicy 最大文件会超过 10000 个 Part: " + key);
            }
        }
        for (String contentType : policy.allowedContentTypes) {
            if (contentType == null || (!contentType.matches("[a-z0-9.+-]+/[a-z0-9.+*-]+"))) {
                invalid("uploadPolicy Content-Type 无效: " + key);
            }
        }
        if (policy.allowedClientPks != null && policy.allowedClientPks.stream().anyMatch(id -> id == null || id <= 0)) {
            invalid("uploadPolicy allowedClientPks 无效: " + key);
        }
    }

    private void requireDuration(Duration value, Duration min, Duration max, String name) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            invalid(name + " 超出安全范围");
        }
    }

    private void invalid(String message) {
        throw new OssUploadException(OssUploadError.INVALID_POLICY, message);
    }

    private long ceilDiv(long value, long divisor) {
        return 1 + (value - 1) / divisor;
    }

    /**
     * 单个命名上传策略。
     */
    @Data
    public static class Policy {

        private boolean enabled = true;
        private AccessPolicy expectedAccessPolicy;
        private long maxSize;
        private Set<String> allowedContentTypes = new LinkedHashSet<>();
        private String objectPrefix;
        private OssUploadMode mode = OssUploadMode.AUTO;
        private long multipartThreshold = 100L * 1024 * 1024;
        private long partSize = 16L * 1024 * 1024;
        private String requiredPermission = "system:oss:upload";
        private Set<Long> allowedClientPks = new LinkedHashSet<>();

        public boolean allowsContentType(String actual) {
            if (actual == null) {
                return false;
            }
            return allowedContentTypes.stream().anyMatch(allowed -> allowed.equals(actual)
                || (allowed.endsWith("/*") && actual.startsWith(allowed.substring(0, allowed.length() - 1))));
        }

        public OssUploadMode resolveMode(long fileSize) {
            return mode == OssUploadMode.AUTO
                ? (fileSize >= multipartThreshold ? OssUploadMode.MULTIPART : OssUploadMode.SINGLE)
                : mode;
        }
    }
}
