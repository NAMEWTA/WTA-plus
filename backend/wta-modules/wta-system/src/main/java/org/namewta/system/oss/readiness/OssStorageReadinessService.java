package org.namewta.system.oss.readiness;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.model.OssAccessDiagnostic;
import org.namewta.system.domain.SysOss;
import org.namewta.system.domain.SysOssConfig;
import org.namewta.system.mapper.SysOssConfigMapper;
import org.namewta.system.mapper.SysOssMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 发现必检配置并刷新 Provider readiness 快照。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssStorageReadinessService {

    private final SysOssConfigMapper configMapper;
    private final SysOssMapper ossMapper;
    private final OssStorageReadinessProperties properties;
    private final OssStorageReadinessRegistry registry;
    private final OssReadinessClientProvider clientProvider;
    private final List<OssRequiredConfigContributor> contributors;

    @Scheduled(initialDelayString = "${oss.readiness.refresh-interval:PT1M}",
        fixedDelayString = "${oss.readiness.refresh-interval:PT1M}")
    public synchronized void refresh() {
        try {
            List<SysOssConfig> configs = configMapper.selectList();
            Map<String, Set<String>> required = requiredConfigs(configs);
            Map<String, OssStorageReadinessEntry> entries = new LinkedHashMap<>();
            Map<String, SysOssConfig> byKey = new LinkedHashMap<>();
            configs.forEach(config -> byKey.put(config.getConfigKey(), config));
            configs.forEach(config -> entries.put(config.getConfigKey(), diagnose(config,
                required.getOrDefault(config.getConfigKey(), Set.of()))));
            required.forEach((configKey, sources) -> {
                if (!byKey.containsKey(configKey)) {
                    entries.put(configKey, notServing(configKey, null, sources,
                        OssStorageReadinessEntry.Reason.CONFIG_MISSING));
                }
            });
            registry.replace(entries, required.keySet(), true);
        } catch (RuntimeException ex) {
            registry.replace(Map.of(), Set.of(), false);
            log.error("OSS readiness 必检集合发现失败: DISCOVERY_FAILED ({})", ex.getClass().getSimpleName());
        }
    }

    private Map<String, Set<String>> requiredConfigs(List<SysOssConfig> configs) {
        Map<String, Set<String>> required = new LinkedHashMap<>();
        configs.stream().filter(config -> SystemConstants.YES.equals(config.getStatus()))
            .forEach(config -> add(required, config.getConfigKey(), "DEFAULT"));
        List<Object> services = ossMapper.selectObjs(new QueryWrapper<SysOss>()
            .select("service").groupBy("service"));
        services.stream().map(String::valueOf).filter(StringUtils::isNotBlank)
            .forEach(configKey -> add(required, configKey, "STORED_OBJECT"));
        contributors.forEach(contributor -> contributor.requiredConfigs().forEach((key, sources) ->
            sources.forEach(source -> add(required, key, source))));
        return required;
    }

    private OssStorageReadinessEntry diagnose(SysOssConfig config, Set<String> requiredBy) {
        AccessPolicy accessPolicy;
        try {
            accessPolicy = AccessPolicy.formType(config.getAccessPolicy());
        } catch (RuntimeException ex) {
            return notServing(config.getConfigKey(), null, requiredBy,
                OssStorageReadinessEntry.Reason.INVALID_ACCESS_POLICY);
        }
        if (accessPolicy == AccessPolicy.PUBLIC_READ && StringUtils.isBlank(config.getDomainUrl())
            && !properties.isAllowEndpointDomainFallback()) {
            return notServing(config.getConfigKey(), accessPolicy, requiredBy,
                OssStorageReadinessEntry.Reason.DOMAIN_REQUIRED);
        }
        String diagnosticObject = properties.getDiagnosticObjects().get(config.getConfigKey());
        if (StringUtils.isBlank(diagnosticObject)) {
            return notServing(config.getConfigKey(), accessPolicy, requiredBy,
                OssStorageReadinessEntry.Reason.DIAGNOSTIC_OBJECT_MISSING);
        }
        try {
            OssAccessDiagnostic diagnostic = clientProvider.client(config.getConfigKey())
                .diagnoseAccess(diagnosticObject, accessPolicy, properties.getDiagnosticTimeout());
            if (diagnostic.verified()) {
                return new OssStorageReadinessEntry(config.getConfigKey(), accessPolicy, !requiredBy.isEmpty(),
                    requiredBy, OssStorageReadinessEntry.Status.SERVING,
                    OssStorageReadinessEntry.Reason.READY, diagnostic.checkedAt());
            }
            OssStorageReadinessEntry.Reason reason = diagnostic.verification()
                == OssAccessDiagnostic.Verification.MISMATCH
                ? OssStorageReadinessEntry.Reason.PROVIDER_MISMATCH
                : OssStorageReadinessEntry.Reason.DIAGNOSTIC_UNVERIFIED;
            return notServing(config.getConfigKey(), accessPolicy, requiredBy, reason);
        } catch (RuntimeException ex) {
            return notServing(config.getConfigKey(), accessPolicy, requiredBy,
                OssStorageReadinessEntry.Reason.DIAGNOSTIC_UNVERIFIED);
        }
    }

    private OssStorageReadinessEntry notServing(String configKey, AccessPolicy accessPolicy,
                                                Set<String> requiredBy,
                                                OssStorageReadinessEntry.Reason reason) {
        return new OssStorageReadinessEntry(configKey, accessPolicy, !requiredBy.isEmpty(), requiredBy,
            OssStorageReadinessEntry.Status.NOT_SERVING, reason, Instant.now());
    }

    private void add(Map<String, Set<String>> required, String configKey, String source) {
        if (StringUtils.isBlank(configKey) || StringUtils.isBlank(source)) {
            return;
        }
        required.computeIfAbsent(configKey, ignored -> new LinkedHashSet<>()).add(source);
    }
}
