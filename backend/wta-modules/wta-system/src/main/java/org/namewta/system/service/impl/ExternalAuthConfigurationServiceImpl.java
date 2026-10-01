package org.namewta.system.service.impl;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.system.api.ExternalAuthConfigurationService;
import org.namewta.system.api.model.ExternalAuthEntry;
import org.namewta.system.api.model.ExternalAuthRegistration;
import org.namewta.system.auth.ExternalAuthConfigurationCache;
import org.namewta.system.auth.ExternalAuthJson;
import org.namewta.system.domain.read.ExternalAuthRuntimeStamp;
import org.namewta.system.mapper.SysAuthProviderMapper;
import org.namewta.system.mapper.SysAuthRegistrationMapper;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/** 外部身份配置运行时门面；每次调用均以数据库状态和版本为准。 */
@Service
@RequiredArgsConstructor
public class ExternalAuthConfigurationServiceImpl implements ExternalAuthConfigurationService {
    private final SysAuthProviderMapper providerMapper;
    private final SysAuthRegistrationMapper registrationMapper;
    private final ExternalAuthConfigurationCache cache;
    private final SocialSecretCipher cipher;

    @Override
    public List<ExternalAuthEntry> listEnabled(String businessClientId) {
        if (businessClientId == null || businessClientId.isBlank()) {
            return List.of();
        }
        return List.copyOf(registrationMapper.selectEnabledEntries(businessClientId));
    }

    @Override
    public ExternalAuthRegistration require(String providerKey, String businessClientId) {
        var stamp = registrationMapper.selectEnabledStamp(providerKey, businessClientId);
        if (stamp == null) {
            throw new ServiceException("外部身份接入不存在或已停用");
        }
        String key = ExternalAuthConfigurationCache.key(stamp.getId(), stamp.getVersion(), stamp.getProviderVersion());
        var snapshot = cache.get(key);
        if (!matches(snapshot, stamp)) {
            snapshot = new ExternalAuthConfigurationCache.Snapshot(providerMapper.selectById(stamp.getProviderId()),
                registrationMapper.selectById(stamp.getId()));
            if (!matches(snapshot, stamp)) {
                throw new ServiceException("外部身份接入已变更，请重新发起登录");
            }
            cache.put(key, snapshot);
        }
        if (!isCurrent(stamp.getId(), stamp.getVersion(), stamp.getProviderVersion())) {
            throw new ServiceException("外部身份接入已变更，请重新发起登录");
        }
        return toRuntime(snapshot);
    }

    @Override
    public ExternalAuthRegistration requireForLogout(long registrationId) {
        var registration = registrationMapper.selectForLogout(registrationId);
        if (registration == null) {
            throw new ServiceException("外部身份退出接入不存在");
        }
        var provider = providerMapper.selectForLogout(registration.getProviderId());
        if (provider == null) {
            throw new ServiceException("外部身份退出源不存在");
        }
        return toRuntime(new ExternalAuthConfigurationCache.Snapshot(provider, registration));
    }

    private ExternalAuthRegistration toRuntime(ExternalAuthConfigurationCache.Snapshot snapshot) {
        var provider = snapshot.provider();
        var registration = snapshot.registration();
        var options = new HashMap<>(ExternalAuthJson.options(provider.getOptionsJson()));
        options.putAll(ExternalAuthJson.options(registration.getOptionsJson()));
        String secret = registration.getClientSecretCiphertext();
        if (secret != null && !secret.isBlank()) {
            secret = cipher.decrypt(secretPurpose(registration.getId()), secret);
        }
        return new ExternalAuthRegistration(registration.getId(), registration.getVersion(), provider.getVersion(),
            provider.getProviderKey(), provider.getProtocol(), provider.getIssuer(), provider.getName(), provider.getIcon(),
            registration.getBusinessClientId(), registration.getExternalClientId(), secret,
            registration.getRedirectUri(), registration.getPostLogoutRedirectUri(),
            ExternalAuthJson.scopes(registration.getScopesJson()), registration.getFirstLoginPolicy(), options);
    }

    @Override
    public boolean isCurrent(long id, long version, long providerVersion) {
        var current = registrationMapper.selectCurrentStamp(id);
        return current != null && current.getVersion() == version && current.getProviderVersion() == providerVersion;
    }

    /** 将密文绑定到本条接入，禁止不同记录之间替换密文。 */
    public static String secretPurpose(long id) {
        return "registration:" + id;
    }

    private boolean matches(ExternalAuthConfigurationCache.Snapshot snapshot, ExternalAuthRuntimeStamp stamp) {
        return snapshot != null && snapshot.provider() != null && snapshot.registration() != null
            && Objects.equals(snapshot.provider().getId(), stamp.getProviderId())
            && Objects.equals(snapshot.registration().getId(), stamp.getId())
            && Objects.equals(snapshot.provider().getVersion(), stamp.getProviderVersion())
            && Objects.equals(snapshot.registration().getVersion(), stamp.getVersion());
    }
}
