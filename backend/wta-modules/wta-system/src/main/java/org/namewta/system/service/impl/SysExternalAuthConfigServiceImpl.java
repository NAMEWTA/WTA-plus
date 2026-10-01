package org.namewta.system.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.MapstructUtils;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.system.auth.ExternalAuthConfigurationCache;
import org.namewta.system.auth.ExternalAuthJson;
import org.namewta.system.domain.*;
import org.namewta.system.domain.bo.*;
import org.namewta.system.domain.vo.*;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.social.oidc.OidcProtocolClient;
import org.namewta.system.domain.vo.SysAuthRegistrationVo;
import org.namewta.system.domain.vo.ExternalAuthLegacyImportResultVo;
import org.namewta.system.event.ExternalAuthConfigChangedEvent;
import org.namewta.system.mapper.*;
import org.namewta.system.service.ISysExternalAuthConfigService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.*;

/** System classic 管理链路；配置写入与提交后的共享缓存失效共同构成一个用例。 */
@Service
@RequiredArgsConstructor
public class SysExternalAuthConfigServiceImpl implements ISysExternalAuthConfigService {
    private static final Set<String> PROTOCOLS = Set.of("OIDC", "DINGTALK", "BAIDU", "GITHUB", "GITEE", "WEIBO",
        "CODING", "OSCHINA", "ALIPAY_WALLET", "QQ", "WECHAT_OPEN", "TAOBAO", "DOUYIN", "LINKEDIN", "MICROSOFT",
        "RENREN", "STACK_OVERFLOW", "HUAWEI", "WECHAT_ENTERPRISE", "GITLAB", "WECHAT_MP", "ALIYUN", "MAXKEY", "TOPIAM", "GITEA");
    private final SysAuthProviderMapper providerMapper;
    private final SysAuthRegistrationMapper registrationMapper;
    private final SysClientMapper clientMapper;
    private final SocialSecretCipher cipher;
    private final ApplicationEventPublisher publisher;
    private final ExternalAuthConfigurationCache cache;
    private final OidcProtocolClient oidc;

    @Override
    public void requireAdminClient() {
        var login = LoginHelper.getLoginUser();
        var client = login == null || login.getClientPk() == null ? null : clientMapper.selectById(login.getClientPk());
        // pc 是初始化基座中的 Admin App key；不能把业务目标 App 切换为当前授权上下文。
        if (client == null || !"pc".equals(client.getClientKey()) || !"0".equals(client.getStatus())) {
            throw new ServiceException("请在 Admin 管理端管理应用登录接入");
        }
    }

    @Override
    public List<ExternalAuthClientOptionVo> clientOptions(String keyword, List<String> clientIds) {
        if (keyword != null && keyword.length() > 200 || clientIds != null && clientIds.size() > 200) {
            throw new ServiceException("查询条件过长，请缩小范围");
        }
        var query = Wrappers.lambdaQuery(SysClient.class)
            .select(SysClient::getClientId, SysClient::getClientKey, SysClient::getStatus,
                SysClient::getGrantType, SysClient::getRegisterEnabled)
            .orderByAsc(SysClient::getClientKey, SysClient::getClientId);
        if (keyword != null && !keyword.isBlank()) {
            query.and(part -> part.like(SysClient::getClientKey, keyword.trim()).or().like(SysClient::getClientId, keyword.trim()));
        }
        if (clientIds != null && !clientIds.isEmpty()) {
            query.in(SysClient::getClientId, clientIds);
        }
        var clients = clientMapper.selectList(query);
        if (clients.isEmpty()) return List.of();
        var registrations = registrationMapper.selectList(Wrappers.lambdaQuery(SysAuthRegistration.class)
            .select(SysAuthRegistration::getBusinessClientId, SysAuthRegistration::getProviderId, SysAuthRegistration::getEnabled)
            .in(SysAuthRegistration::getBusinessClientId, clients.stream().map(SysClient::getClientId).toList()));
        var providerIds = registrations.stream().map(SysAuthRegistration::getProviderId).distinct().toList();
        Set<Long> enabledProviders = providerIds.isEmpty() ? Set.of() : new HashSet<>(providerMapper.selectList(
            Wrappers.lambdaQuery(SysAuthProvider.class).select(SysAuthProvider::getId)
                .in(SysAuthProvider::getId, providerIds).eq(SysAuthProvider::getEnabled, true))
            .stream().map(SysAuthProvider::getId).toList());
        var byClient = registrations.stream().collect(java.util.stream.Collectors.groupingBy(SysAuthRegistration::getBusinessClientId));
        return clients.stream().map(client -> {
            boolean social = hasSocial(client);
            boolean active = "0".equals(client.getStatus()) && social;
            var configured = byClient.getOrDefault(client.getClientId(), List.of());
            long enabled = active ? configured.stream().filter(item -> Boolean.TRUE.equals(item.getEnabled())
                && enabledProviders.contains(item.getProviderId())).count() : 0;
            String reason = !"0".equals(client.getStatus()) ? "客户端已停用" : !social ? "未开放 social 授权" : null;
            return new ExternalAuthClientOptionVo(client.getClientId(), client.getClientKey(), client.getStatus(), social,
                Boolean.TRUE.equals(client.getRegisterEnabled()), configured.size(), enabled, reason);
        }).toList();
    }

    @Override
    public List<ExternalAuthProviderOptionVo> providerOptions(String keyword, Long selectedId) {
        if (keyword != null && keyword.length() > 200) throw new ServiceException("查询条件过长，请缩小范围");
        var query = QueryBuilder.lambda(SysAuthProvider.class).likeIfText(SysAuthProvider::getName, keyword)
            .build().orderByAsc(SysAuthProvider::getName, SysAuthProvider::getId);
        var rows = new ArrayList<>(providerMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 50, false), query).getRecords());
        if (selectedId != null && rows.stream().noneMatch(row -> Objects.equals(row.getId(), selectedId))) {
            rows.addFirst(requireProvider(requiredId(selectedId)));
        }
        return rows.stream().map(provider -> {
            String method = ExternalAuthJson.options(provider.getOptionsJson()).get("authenticationMethod");
            return new ExternalAuthProviderOptionVo(provider.getId(), provider.getProviderKey(), provider.getName(),
                provider.getProtocol(), provider.getIssuer(), Boolean.TRUE.equals(provider.getEnabled()),
                method == null ? Map.of() : Map.of("authenticationMethod", method));
        }).toList();
    }

    @Override
    public ExternalAuthConnectionInfoVo connectionInfo(long id) {
        var registration = requireRegistration(id);
        var provider = requireProvider(registration.getProviderId());
        var options = mergedOptions(provider, ExternalAuthJson.options(registration.getOptionsJson()));
        boolean isOidc = "OIDC".equals(provider.getProtocol());
        String base = options.get("apiPublicBase");
        String backchannel = "";
        if (isOidc && base != null && !base.isBlank()) {
            absoluteUrl(base, "业务 API 外部地址", true);
            backchannel = stripTrailingSlash(base) + "/auth/social/backchannel/" + id;
        }
        return new ExternalAuthConnectionInfoVo(id, provider.getName(), provider.getProviderKey(), provider.getProtocol(),
            provider.getIssuer(), isOidc ? stripTrailingSlash(provider.getIssuer()) + "/.well-known/openid-configuration" : null,
            registration.getBusinessClientId(), registration.getExternalClientId(), options.getOrDefault("authenticationMethod", "client_secret_basic"),
            options.getOrDefault("appPublicUrl", ""), registration.getRedirectUri(), registration.getPostLogoutRedirectUri(), ExternalAuthJson.scopes(registration.getScopesJson()),
            backchannel, registrationMapper.selectCurrentStamp(id) != null,
            registration.getClientSecretCiphertext() != null && !registration.getClientSecretCiphertext().isBlank());
    }

    @Override
    public OidcMetadataDiagnosticVo oidcMetadata(String issuer) {
        absoluteUrl(issuer, "OIDC issuer", true);
        var result = oidc.diagnose(issuer);
        var metadata = result.metadata();
        return new OidcMetadataDiagnosticVo(issuer, stripTrailingSlash(issuer) + "/.well-known/openid-configuration",
            metadata.authorizationEndpoint(), metadata.tokenEndpoint(), metadata.jwksUri(), metadata.userInfoEndpoint(),
            metadata.endSessionEndpoint(), result.tokenEndpointAuthMethodsSupported(), result.scopesSupported(),
            result.codeChallengeMethodsSupported(), result.responseTypesSupported(), result.idTokenSigningAlgValuesSupported(),
            result.backchannelLogoutSupported(), result.backchannelLogoutSessionSupported(), result.checkedAt());
    }

    /** 以登记的绝对路径生成交付地址，保留部署子路径。 */
    private static String stripTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }

    /** 身份源选项作为默认值，接入选项显式覆盖。 */
    private static Map<String, String> mergedOptions(SysAuthProvider provider, Map<String, String> registrationOptions) {
        var result = new HashMap<>(ExternalAuthJson.options(provider.getOptionsJson()));
        result.putAll(registrationOptions);
        return result;
    }

    /** 与运行时入口查询使用同一 social 授权判定。 */
    private static boolean hasSocial(SysClient client) {
        return client.getGrantType() != null
            && Arrays.stream(client.getGrantType().split(",")).map(String::trim).anyMatch("social"::equals);
    }

    @Override
    public PageResult<SysAuthProviderVo> providers(SysAuthProviderBo query, PageQuery page) {
        var result = providerMapper.selectPage(page.build(), QueryBuilder.lambda(SysAuthProvider.class)
            .eqIfText(SysAuthProvider::getProviderKey, query.getProviderKey())
            .likeIfText(SysAuthProvider::getName, query.getName())
            .eqIfPresent(SysAuthProvider::getEnabled, query.getEnabled()).build().orderByAsc(SysAuthProvider::getId));
        return PageResult.build(result.getRecords().stream().map(this::providerVo).toList(), result.getTotal());
    }

    @Override
    public SysAuthProviderVo provider(long id) {
        return providerVo(requireProvider(id));
    }

    @Override
    @DSTransactional
    public Long saveProvider(SysAuthProviderBo bo, boolean create) {
        validateProvider(bo);
        SysAuthProvider previous = create ? null : providerMapper.selectForUpdate(requiredId(bo.getId()));
        if (!create) {
            checkVersion(previous == null ? null : previous.getVersion(), bo.getVersion());
            if (registrationMapper.countAllForProvider(bo.getId()) > 0
                && (!Objects.equals(previous.getProviderKey(), bo.getProviderKey())
                || !Objects.equals(previous.getProtocol(), bo.getProtocol())
                || !Objects.equals(previous.getIssuer(), bo.getIssuer()))) {
                throw new ServiceException("已有接入引用的身份源不可修改标识、协议或 issuer，请新建身份源");
            }
        }
        var entity = MapstructUtils.convert(bo, SysAuthProvider.class);
        entity.setId(create ? IdGeneratorUtil.nextLongId() : previous.getId());
        entity.setVersion(create ? 0L : previous.getVersion());
        entity.setDelFlag("0");
        entity.setOptionsJson(JsonUtils.toJsonString(options(bo.getOptions())));
        List<String> oldKeys = create ? List.of() : keysForProvider(previous);
        try {
            int changed = create ? providerMapper.insert(entity) : providerMapper.updateById(entity);
            requireChanged(changed);
        } catch (DuplicateKeyException error) {
            throw new ServiceException("身份源标识已使用，请使用其他标识");
        }
        publisher.publishEvent(new ExternalAuthConfigChangedEvent(oldKeys));
        return entity.getId();
    }

    @Override
    @DSTransactional
    public void removeProvider(ExternalAuthRemoveBo bo) {
        var previous = providerMapper.selectForUpdate(bo.id());
        checkVersion(previous == null ? null : previous.getVersion(), bo.version());
        if (registrationMapper.countAllForProvider(bo.id()) > 0) {
            throw new ServiceException("身份源仍有接入或退出记录引用，可停用但不可删除");
        }
        previous.setEnabled(false);
        requireChanged(providerMapper.updateById(previous));
        requireChanged(providerMapper.deleteById(previous.getId()));
        publisher.publishEvent(new ExternalAuthConfigChangedEvent(List.of()));
    }

    @Override
    public PageResult<SysAuthRegistrationVo> registrations(SysAuthRegistrationBo query, PageQuery page) {
        var result = registrationMapper.selectPage(page.build(), QueryBuilder.lambda(SysAuthRegistration.class)
            .eqIfPresent(SysAuthRegistration::getProviderId, query.getProviderId())
            .eqIfText(SysAuthRegistration::getBusinessClientId, query.getBusinessClientId())
            .eqIfPresent(SysAuthRegistration::getEnabled, query.getEnabled()).build().orderByAsc(SysAuthRegistration::getId));
        var rows = result.getRecords().stream().map(this::registrationVo).toList();
        if (!rows.isEmpty()) {
            var providerIds = rows.stream().map(SysAuthRegistrationVo::getProviderId).distinct().toList();
            var names = providerMapper.selectList(Wrappers.lambdaQuery(SysAuthProvider.class)
                .select(SysAuthProvider::getId, SysAuthProvider::getName).in(SysAuthProvider::getId, providerIds))
                .stream().collect(java.util.stream.Collectors.toMap(SysAuthProvider::getId, SysAuthProvider::getName));
            rows.forEach(row -> row.setProviderName(names.get(row.getProviderId())));
        }
        return PageResult.build(rows, result.getTotal());
    }

    @Override
    public SysAuthRegistrationVo registration(long id) {
        var row = registrationVo(requireRegistration(id));
        row.setProviderName(requireProvider(row.getProviderId()).getName());
        return row;
    }

    @Override
    @DSTransactional
    public Long saveRegistration(SysAuthRegistrationBo bo, boolean create) {
        validateRegistration(bo);
        // 与身份源删除共用行锁，避免校验与插入之间出现悬空引用。
        var provider = providerMapper.selectForUpdate(requiredId(bo.getProviderId()));
        if (provider == null) {
            throw new ServiceException("身份源不存在");
        }
        requireSocialClient(bo.getBusinessClientId());
        var previous = create ? null : requireRegistration(requiredId(bo.getId()));
        if (!create) {
            checkVersion(previous.getVersion(), bo.getVersion());
            if (!Objects.equals(previous.getProviderId(), bo.getProviderId())
                || !Objects.equals(previous.getBusinessClientId(), bo.getBusinessClientId())
                || !Objects.equals(previous.getExternalClientId(), bo.getExternalClientId())) {
                throw new ServiceException("接入所属身份源、业务客户端与外部客户端不可修改，请新建接入");
            }
        }
        validateOidcRegistration(provider, bo, previous);
        var entity = MapstructUtils.convert(bo, SysAuthRegistration.class);
        entity.setId(create ? IdGeneratorUtil.nextLongId() : previous.getId());
        entity.setVersion(create ? 0L : previous.getVersion());
        entity.setDelFlag("0");
        entity.setOptionsJson(JsonUtils.toJsonString(options(bo.getOptions())));
        entity.setScopesJson(JsonUtils.toJsonString(scopes(bo.getScopes())));
        String secret = bo.getClientSecret();
        entity.setClientSecretCiphertext(secret == null || secret.isBlank()
            ? (previous == null ? null : previous.getClientSecretCiphertext())
            : cipher.encrypt(ExternalAuthConfigurationServiceImpl.secretPurpose(entity.getId()), secret));
        try {
            int changed = create ? registrationMapper.insert(entity) : registrationMapper.updateById(entity);
            requireChanged(changed);
        } catch (DuplicateKeyException error) {
            throw new ServiceException("该业务客户端已存在同身份源接入");
        }
        publisher.publishEvent(new ExternalAuthConfigChangedEvent(create ? List.of() : List.of(
            ExternalAuthConfigurationCache.key(previous.getId(), previous.getVersion(), provider.getVersion()))));
        return entity.getId();
    }

    @Override
    @DSTransactional
    public void removeRegistration(ExternalAuthRemoveBo bo) {
        var previous = requireRegistration(bo.id());
        checkVersion(previous.getVersion(), bo.version());
        var provider = requireProvider(previous.getProviderId());
        String key = ExternalAuthConfigurationCache.key(previous.getId(), previous.getVersion(), provider.getVersion());
        previous.setEnabled(false);
        requireChanged(registrationMapper.updateById(previous));
        requireChanged(registrationMapper.deleteById(previous.getId()));
        publisher.publishEvent(new ExternalAuthConfigChangedEvent(List.of(key)));
    }

    @Override
    @DSTransactional
    public ExternalAuthLegacyImportResultVo importLegacy(ExternalAuthLegacyImportBo bo) {
        requiredText(bo.getBusinessClientId(), "业务客户端", 200);
        requireSocialClient(bo.getBusinessClientId());
        if (bo.getType() == null || bo.getType().isEmpty() || bo.getType().size() > 32) {
            throw new ServiceException("导入应包含 1 至 32 项旧配置");
        }
        boolean dryRun = !Boolean.FALSE.equals(bo.getDryRun());
        var results = new ArrayList<ExternalAuthLegacyImportResultVo.Item>();
        for (var entry : bo.getType().entrySet()) {
            String source = entry.getKey();
            var legacy = entry.getValue();
            String key = source == null ? "" : source.toLowerCase(Locale.ROOT);
            String client = legacy == null ? null : legacy.getExternalClientId();
            String redirect = legacy == null ? null : legacy.getRedirectUri();
            boolean hasSecret = legacy != null && legacy.getClientSecret() != null && !legacy.getClientSecret().isBlank();
            if (legacy == null || client == null || client.isBlank() || redirect == null || redirect.isBlank()) {
                results.add(new ExternalAuthLegacyImportResultVo.Item(source, key, client, redirect, hasSecret,
                    "SKIPPED", "空白或不完整示例未导入", null));
                continue;
            }
            try {
                var providerBo = new SysAuthProviderBo(); providerBo.setProviderKey(key); providerBo.setName(source);
                providerBo.setProtocol(source.toUpperCase(Locale.ROOT)); providerBo.setEnabled(true);
                validateProvider(providerBo);
                var registrationBo = new SysAuthRegistrationBo(); registrationBo.setBusinessClientId(bo.getBusinessClientId());
                registrationBo.setExternalClientId(client); registrationBo.setClientSecret(legacy.getClientSecret());
                registrationBo.setRedirectUri(redirect); registrationBo.setScopes(legacy.getScopes());
                registrationBo.setFirstLoginPolicy(bo.getFirstLoginPolicy()); registrationBo.setEnabled(true);
                registrationBo.setOptions(legacyOptions(legacy));
                validateRegistration(registrationBo);
                var existingProvider = providerMapper.selectOne(Wrappers.lambdaQuery(SysAuthProvider.class)
                    .eq(SysAuthProvider::getProviderKey, key));
                if (existingProvider != null && !Objects.equals(existingProvider.getProtocol(), providerBo.getProtocol())) {
                    throw new ServiceException("同名身份源协议不同，请先人工核对");
                }
                if (existingProvider != null) {
                    var existing = registrationMapper.selectOne(Wrappers.lambdaQuery(SysAuthRegistration.class)
                        .eq(SysAuthRegistration::getProviderId, existingProvider.getId())
                        .eq(SysAuthRegistration::getBusinessClientId, bo.getBusinessClientId()));
                    if (existing != null) {
                        results.add(new ExternalAuthLegacyImportResultVo.Item(source, key, client, redirect, hasSecret,
                            "EXISTS", "已存在接入，保留管理端当前配置", existing.getId()));
                        continue;
                    }
                }
                Long id = null;
                if (!dryRun) {
                    // 外层 importLegacy 已通过代理开启事务，以下复用同一业务不变量与事件流程。
                    long providerId = existingProvider == null ? saveProvider(providerBo, true) : existingProvider.getId();
                    registrationBo.setProviderId(providerId);
                    id = saveRegistration(registrationBo, true);
                }
                results.add(new ExternalAuthLegacyImportResultVo.Item(source, key, client, redirect, hasSecret,
                    dryRun ? "READY" : "IMPORTED", dryRun ? "可导入" : "已导入", id));
            } catch (ServiceException exception) {
                if (!dryRun) {
                    throw new ServiceException("导入失败，整批未保存：" + exception.getMessage());
                }
                results.add(new ExternalAuthLegacyImportResultVo.Item(source, key, client, redirect, hasSecret,
                    "INVALID", exception.getMessage(), null));
            }
        }
        return new ExternalAuthLegacyImportResultVo(results);
    }

    private Map<String, String> legacyOptions(ExternalAuthLegacyProviderBo legacy) {
        if (legacy.getStackOverflowKey() != null && !legacy.getStackOverflowKey().isBlank()) {
            throw new ServiceException("旧 StackOverflow 独立 API 密钥不能迁入公开选项，请人工核对凭据配置");
        }
        Map<String, String> values = new HashMap<>();
        values.put("tenantId", legacy.getTenantId()); values.put("codingGroupName", legacy.getCodingGroupName());
        values.put("alipayPublicKey", legacy.getAlipayPublicKey()); values.put("agentId", legacy.getAgentId());
        values.put("deviceId", legacy.getDeviceId()); values.put("clientOsType", legacy.getClientOsType());
        values.put("serverUrl", legacy.getServerUrl());
        if (legacy.getUnionId() != null) {
            values.put("unionId", legacy.getUnionId().toString());
        }
        values.values().removeIf(value -> value == null || value.isBlank());
        return values;
    }

    @Override
    public void refresh() {
        var providers = providerMapper.selectList(Wrappers.lambdaQuery(SysAuthProvider.class));
        var keys = providers.stream().flatMap(provider -> keysForProvider(provider).stream()).toList();
        cache.invalidate(keys);
    }

    private SysAuthProvider requireProvider(long id) {
        var entity = providerMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("身份源不存在");
        }
        return entity;
    }

    private SysAuthRegistration requireRegistration(long id) {
        var entity = registrationMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("接入不存在");
        }
        return entity;
    }

    private void requireSocialClient(String businessClientId) {
        var client = clientMapper.selectOne(Wrappers.lambdaQuery(SysClient.class).eq(SysClient::getClientId, businessClientId));
        if (client == null || !"0".equals(client.getStatus()) || !hasSocial(client)) {
            throw new ServiceException("业务客户端不存在、已停用或未开放 social 授权");
        }
    }

    private List<String> keysForProvider(SysAuthProvider provider) {
        return registrationMapper.selectList(Wrappers.lambdaQuery(SysAuthRegistration.class)
            .eq(SysAuthRegistration::getProviderId, provider.getId())).stream()
            .map(registration -> ExternalAuthConfigurationCache.key(registration.getId(), registration.getVersion(), provider.getVersion()))
            .toList();
    }

    private SysAuthProviderVo providerVo(SysAuthProvider entity) {
        var vo = MapstructUtils.convert(entity, SysAuthProviderVo.class);
        vo.setOptions(ExternalAuthJson.options(entity.getOptionsJson()));
        return vo;
    }

    private SysAuthRegistrationVo registrationVo(SysAuthRegistration entity) {
        var vo = MapstructUtils.convert(entity, SysAuthRegistrationVo.class);
        vo.setOptions(ExternalAuthJson.options(entity.getOptionsJson()));
        vo.setScopes(ExternalAuthJson.scopes(entity.getScopesJson()));
        vo.setSecretConfigured(entity.getClientSecretCiphertext() != null && !entity.getClientSecretCiphertext().isBlank());
        return vo;
    }

    private void validateProvider(SysAuthProviderBo bo) {
        if (bo.getProviderKey() == null || !bo.getProviderKey().matches("[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}")) {
            throw new ServiceException("身份源标识应为 1 至 64 位字母、数字、下划线或短横线");
        }
        requiredText(bo.getName(), "名称", 200);
        if (!PROTOCOLS.contains(bo.getProtocol() == null ? "" : bo.getProtocol())) {
            throw new ServiceException("不支持的外部身份协议");
        }
        if ("OIDC".equals(bo.getProtocol())) {
            absoluteUrl(bo.getIssuer(), "OIDC issuer", true);
            oidc.validateUrl(bo.getIssuer());
            validateAuthenticationMethod(options(bo.getOptions()));
        } else if (bo.getIssuer() != null && !bo.getIssuer().isBlank()) {
            absoluteUrl(bo.getIssuer(), "issuer", true);
        }
        if (bo.getEnabled() == null) {
            throw new ServiceException("启用状态不能为空");
        }
        options(bo.getOptions());
    }

    private void validateRegistration(SysAuthRegistrationBo bo) {
        requiredText(bo.getBusinessClientId(), "业务客户端", 200);
        requiredText(bo.getExternalClientId(), "外部客户端", 200);
        absoluteUrl(bo.getRedirectUri(), "登录回调地址", false);
        if (bo.getPostLogoutRedirectUri() != null && !bo.getPostLogoutRedirectUri().isBlank()) {
            absoluteUrl(bo.getPostLogoutRedirectUri(), "退出回调地址", false);
        }
        if (!Set.of("BIND_ONLY", "AUTO_REGISTER").contains(bo.getFirstLoginPolicy() == null ? "" : bo.getFirstLoginPolicy())) {
            throw new ServiceException("首次登录策略只能为 BIND_ONLY 或 AUTO_REGISTER");
        }
        if (bo.getEnabled() == null) {
            throw new ServiceException("启用状态不能为空");
        }
        if (bo.getClientSecret() != null && bo.getClientSecret().length() > 4096) {
            throw new ServiceException("密钥过长");
        }
        options(bo.getOptions());
        scopes(bo.getScopes());
    }

    /** 保存只做静态校验，不在数据库事务内调用身份提供方。 */
    private void validateOidcRegistration(SysAuthProvider provider, SysAuthRegistrationBo bo, SysAuthRegistration previous) {
        if (!"OIDC".equals(provider.getProtocol())) return;
        var effectiveOptions = mergedOptions(provider, options(bo.getOptions()));
        validateAuthenticationMethod(effectiveOptions);
        if (!scopes(bo.getScopes()).contains("openid")) {
            throw new ServiceException("OIDC scope 必须包含 openid");
        }
        if ((bo.getClientSecret() == null || bo.getClientSecret().isBlank())
            && (previous == null || previous.getClientSecretCiphertext() == null || previous.getClientSecretCiphertext().isBlank())) {
            throw new ServiceException("OIDC 机密客户端必须配置 Client Secret；编辑时留空仅保留已有密钥");
        }
        oidc.validateUrl(provider.getIssuer());
        oidc.validateUrl(bo.getRedirectUri());
        if (bo.getPostLogoutRedirectUri() != null && !bo.getPostLogoutRedirectUri().isBlank()) {
            oidc.validateUrl(bo.getPostLogoutRedirectUri());
        }
        for (String key : List.of("appPublicUrl", "apiPublicBase")) {
            String value = effectiveOptions.get(key);
            if (value != null && !value.isBlank()) {
                absoluteUrl(value, key, true);
                oidc.validateUrl(value);
            }
        }
    }

    /** 支持的认证方式与现有机密客户端协议实现保持一致。 */
    private static void validateAuthenticationMethod(Map<String, String> value) {
        if (!Set.of("client_secret_basic", "client_secret_post").contains(value.getOrDefault("authenticationMethod", "client_secret_basic"))) {
            throw new ServiceException("OIDC 客户端认证方式只支持 client_secret_basic 或 client_secret_post");
        }
    }

    private static Map<String, String> options(Map<String, String> value) {
        if (value == null) {
            return Map.of();
        }
        if (value.size() > 32) {
            throw new ServiceException("扩展参数不能超过 32 项");
        }
        value.forEach((key, item) -> {
            if (key == null || !key.matches("[a-zA-Z][a-zA-Z0-9_.-]{0,63}") || item == null || item.length() > 2048
                || key.toLowerCase(Locale.ROOT).matches(".*(secret|password|private.?key|access.?token|refresh.?token).*")) {
                throw new ServiceException("扩展参数格式无效；密钥只能通过专用密钥输入项保存");
            }
        });
        return Map.copyOf(value);
    }

    private static List<String> scopes(List<String> value) {
        if (value == null) {
            return List.of();
        }
        if (value.size() > 64 || value.stream().anyMatch(item -> item == null || item.isBlank()
            || item.length() > 200 || item.chars().anyMatch(Character::isWhitespace))) {
            throw new ServiceException("scope 应为最多 64 个非空且不含空格的范围标识");
        }
        return value.stream().distinct().toList();
    }

    private static void absoluteUrl(String value, String label, boolean issuer) {
        requiredText(value, label, 2048);
        try {
            var uri = URI.create(value);
            if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getFragment() != null || (issuer && uri.getQuery() != null)
                || value.contains("*")) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException error) {
            throw new ServiceException(label + "必须为不含通配符或片段的 HTTP(S) 绝对地址");
        }
    }

    private static void requiredText(String value, String label, int limit) {
        if (value == null || value.isBlank() || value.length() > limit || !value.equals(value.trim())) {
            throw new ServiceException(label + "不能为空、超长或包含首尾空格");
        }
    }

    private static long requiredId(Long value) {
        if (value == null || value <= 0) {
            throw new ServiceException("主键不能为空且必须为正数");
        }
        return value;
    }

    private static void checkVersion(Long current, Long expected) {
        if (current == null || expected == null || !current.equals(expected)) {
            throw new ServiceException("配置已被修改或删除，请刷新后重试");
        }
    }

    private static void requireChanged(int changed) {
        if (changed != 1) {
            throw new ServiceException("配置已被修改或删除，请刷新后重试");
        }
    }
}
