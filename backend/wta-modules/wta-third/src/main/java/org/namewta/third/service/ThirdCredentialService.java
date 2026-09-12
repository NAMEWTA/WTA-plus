package org.namewta.third.service;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.third.dao.ThirdCredentialDao;
import org.namewta.third.dao.ThirdEndpointDao;
import org.namewta.third.dao.ThirdProviderDao;
import org.namewta.third.domain.ThirdCredential;
import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.domain.bo.ThirdCredentialBo;
import org.namewta.third.domain.vo.ThirdCredentialVo;
import org.namewta.third.port.ThirdConfigSnapshotPort;
import org.namewta.third.port.ThirdCredentialCryptoPort;
import org.namewta.third.support.ThirdEndpointSecurity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ThirdCredentialService {
    private final ThirdCredentialDao credentialDao;
    private final ThirdProviderDao providerDao;
    private final ThirdEndpointDao endpointDao;
    private final ThirdCredentialCryptoPort crypto;
    private final ThirdConfigSnapshotPort configCache;

    public List<ThirdCredentialVo> list(String providerCode, String endpointCode) {
        ThirdProvider provider = provider(providerCode);
        Long endpointId = endpointCode == null || endpointCode.isBlank() ? null : endpoint(provider.getProviderId(), endpointCode).getEndpointId();
        return credentialDao.findByScope(provider.getProviderId(), endpointId).stream()
            .map(c -> new ThirdCredentialVo(c.getCredentialId(), provider.getProviderCode(), endpointCode, c.getScopeType(), c.getCredentialType(),
                c.getKekVersion(), c.getExpiresAt(), c.getVersion(), c.getDelFlag())).toList();
    }

    public void save(ThirdCredentialBo bo) {
        ThirdProvider provider = provider(bo.getProviderCode());
        ThirdEndpoint endpoint = bo.getEndpointCode() == null || bo.getEndpointCode().isBlank() ? null : endpoint(provider.getProviderId(), bo.getEndpointCode());
        String scope = endpoint == null ? "PROVIDER" : "ENDPOINT";
        String credentialType = ThirdEndpointSecurity.validateIdentifier(bo.getCredentialType(), "Credential type");
        if (bo.getSecretJson() == null || bo.getSecretJson().isBlank()) throw new ServiceException("Credential secret is required");
        ThirdCredential credential = bo.getCredentialId() == null ? new ThirdCredential() : credentialDao.findById(bo.getCredentialId());
        if (bo.getCredentialId() != null && credential == null) throw new ServiceException("Credential not found");
        if (bo.getCredentialId() != null && !"0".equals(credential.getDelFlag())) {
            throw new ServiceException("Credential not found");
        }
        if (credential != null && credential.getProviderId() != null && !provider.getProviderId().equals(credential.getProviderId())) throw new ServiceException("Credential does not belong to provider");
        if (bo.getCredentialId() != null && !Objects.equals(credential.getEndpointId(), endpoint == null ? null : endpoint.getEndpointId())) throw new ServiceException("Credential scope cannot be changed");
        if (credential.getCredentialId() == null) credential.setCredentialId(IdGeneratorUtil.nextLongId());
        credential.setProviderId(provider.getProviderId());
        credential.setEndpointId(endpoint == null ? null : endpoint.getEndpointId());
        credential.setScopeType(scope);
        credential.setCredentialType(credentialType);
        ThirdCredentialCryptoPort.EncryptedSecret encrypted = crypto.encrypt(scope, credentialType, bo.getSecretJson());
        credential.setCiphertext(encrypted.ciphertext());
        credential.setNonce(encrypted.nonce());
        credential.setAuthTag(encrypted.authTag());
        credential.setKekVersion("v1");
        credential.setExpiresAt(bo.getExpiresAt());
        credential.setVersion(credential.getVersion() == null ? 1 : credential.getVersion() + 1);
        credential.setDelFlag(Boolean.TRUE.equals(bo.getEnabled()) ? "0" : "1");
        int changed = bo.getCredentialId() == null ? credentialDao.insert(credential) : credentialDao.update(credential);
        if (changed != 1) throw new ServiceException("Credential save failed");
        configCache.evict(provider.getProviderCode(), endpoint == null ? null : endpoint.getEndpointCode());
    }

    public void remove(Long credentialId) {
        ThirdCredential credential = credentialDao.findById(credentialId);
        if (credential == null) throw new ServiceException("Credential not found");
        if (!"0".equals(credential.getDelFlag())) throw new ServiceException("Credential not found");
        credential.setDelFlag("1");
        credentialDao.update(credential);
        ThirdProvider provider = providerDao.findActiveById(credential.getProviderId());
        if (provider == null) throw new ServiceException("Provider not found");
        ThirdEndpoint endpoint = credential.getEndpointId() == null ? null : endpointDao.findActiveById(credential.getEndpointId());
        String endpointCode = endpoint == null ? null : endpoint.getEndpointCode();
        configCache.evict(provider.getProviderCode(), endpointCode);
    }

    private ThirdProvider provider(String code) {
        if (code == null || code.isBlank()) throw new ServiceException("Provider code is required");
        ThirdProvider provider = providerDao.findActiveByCode(code.trim());
        if (provider == null) throw new ServiceException("Provider not found");
        return provider;
    }

    private ThirdEndpoint endpoint(Long providerId, String code) {
        ThirdEndpoint endpoint = endpointDao.findActiveByProviderAndCode(providerId, code.trim());
        if (endpoint == null) throw new ServiceException("Endpoint not found");
        return endpoint;
    }
}
