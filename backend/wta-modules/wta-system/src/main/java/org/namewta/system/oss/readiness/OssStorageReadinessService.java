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
import org.namewta.system.domain.vo.OssStorageDiagnosticVo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理员显式触发的 OSS Provider 观察诊断；业务及启动均不以此为门禁。
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

    /**
     * 诊断一个已选择的配置，不返回桶名、端点、凭据、对象 key 或原始 Provider 错误。
     *
     * @param ossConfigId 配置主键
     * @return 可公开的观察状态
     */
    public OssStorageDiagnosticVo diagnoseOne(Long ossConfigId) {
        long revision = registry.configRevision();
        SysOssConfig config = ossConfigId == null ? null : configMapper.selectById(ossConfigId);
        OssStorageReadinessEntry entry = config == null
            ? notServing("missing", null, Set.of(), OssStorageReadinessEntry.Reason.CONFIG_MISSING)
            : StringUtils.isBlank(config.getConfigKey())
                ? notServing("invalid", null, Set.of(), OssStorageReadinessEntry.Reason.DIAGNOSTIC_CONFIG_INVALID)
                : diagnose(config, Set.of());
        if (config != null && StringUtils.isNotBlank(config.getConfigKey())) {
            if (!registry.recordIfUnchanged(entry, revision)) {
                return new OssStorageDiagnosticVo(OssStorageReadinessEntry.Status.NOT_SERVING.name(),
                    OssStorageReadinessEntry.Reason.STALE.name(), Instant.now());
            }
        }
        return new OssStorageDiagnosticVo(entry.status().name(), entry.reason().name(), entry.checkedAt());
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
        if (!properties.diagnosticConfigurationValid()) {
            return notServing(config.getConfigKey(), null, requiredBy,
                OssStorageReadinessEntry.Reason.DIAGNOSTIC_CONFIG_INVALID);
        }
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
                .diagnoseAccess(diagnosticObject, accessPolicy, properties.boundedDiagnosticTimeout());
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
