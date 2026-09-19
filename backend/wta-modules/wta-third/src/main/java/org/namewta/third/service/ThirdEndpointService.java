package org.namewta.third.service;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.third.dao.ThirdEndpointDao;
import org.namewta.third.dao.ThirdProviderDao;
import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.domain.bo.ThirdEndpointBo;
import org.namewta.third.domain.vo.ThirdEndpointVo;
import org.namewta.third.port.ThirdConfigSnapshotPort;
import org.namewta.third.spi.ThirdProviderAdapterRegistry;
import org.namewta.third.support.ThirdEndpointSecurity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThirdEndpointService {
    private final ThirdEndpointDao endpointDao;
    private final ThirdProviderDao providerDao;
    private final ThirdConfigSnapshotPort configCache;
    private final ThirdProviderAdapterRegistry adapterRegistry;

    public List<ThirdEndpointVo> list(Long providerId, String keyword) {
        return endpointDao.findActive(providerId, keyword).stream().map(ThirdEndpointService::toVo).toList();
    }

    public ThirdEndpointVo get(Long endpointId) { return toVo(required(endpointId)); }

    public void save(ThirdEndpointBo bo) {
        ThirdProvider provider = providerDao.findActiveById(bo.getProviderId());
        if (provider == null) throw new ServiceException("Provider not found");
        ThirdEndpoint entity = bo.getEndpointId() == null ? new ThirdEndpoint() : required(bo.getEndpointId());
        String providerCode = ThirdEndpointSecurity.validateIdentifier(bo.getProviderCode(), "Provider code");
        String endpointCode = ThirdEndpointSecurity.validateIdentifier(bo.getEndpointCode(), "Endpoint code");
        if (entity.getEndpointId() != null && !entity.getEndpointCode().equals(endpointCode)) {
            throw new ServiceException("Endpoint code cannot be changed");
        }
        if (!provider.getProviderCode().equals(providerCode)) throw new ServiceException("Provider code does not match provider id");
        if (entity.getProviderId() != null && !provider.getProviderId().equals(entity.getProviderId())) throw new ServiceException("Endpoint does not belong to provider");
        if (endpointDao.existsCode(providerCode, endpointCode, bo.getEndpointId())) throw new ServiceException("Endpoint code already exists for provider");
        if (bo.getEndpointId() == null) { entity.setEndpointId(IdGeneratorUtil.nextLongId()); entity.setVersion(0); entity.setDelFlag("0"); }
        entity.setProviderId(provider.getProviderId());
        entity.setProviderCode(provider.getProviderCode());
        entity.setEndpointCode(endpointCode);
        entity.setEndpointName(bo.getEndpointName().trim());
        entity.setHttpMethod(ThirdEndpointSecurity.validateMethod(bo.getHttpMethod()));
        entity.setRelativePath(ThirdEndpointSecurity.validateRelativePath(bo.getRelativePath()));
        entity.setRequestMode(ThirdEndpointSecurity.validateRequestMode(bo.getRequestMode()));
        entity.setResponseMode(ThirdEndpointSecurity.validateResponseMode(bo.getResponseMode()));
        ThirdEndpointSecurity.validateMetadataJson(bo.getPathSchemaJson(), "Path schema");
        ThirdEndpointSecurity.validateMetadataJson(bo.getQuerySchemaJson(), "Query schema");
        ThirdEndpointSecurity.validateMetadataJson(bo.getHeaderSchemaJson(), "Header schema");
        ThirdEndpointSecurity.validateMetadataJson(bo.getBodySchemaJson(), "Body schema");
        ThirdEndpointSecurity.validateMetadataJson(bo.getResponseSchemaJson(), "Response schema");
        ThirdEndpointSecurity.validateMetadataJson(bo.getOverrideJson(), "Override");
        ThirdEndpointSecurity.validateOverrideJson(bo.getOverrideJson());
        ThirdEndpointSecurity.validateMetadataJson(bo.getSensitiveFieldsJson(), "Sensitive fields");
        ThirdEndpointSecurity.parseAllowedNames(bo.getPathSchemaJson());
        ThirdEndpointSecurity.parseAllowedNames(bo.getQuerySchemaJson());
        ThirdEndpointSecurity.parseAllowedNames(bo.getHeaderSchemaJson());
        ThirdEndpointSecurity.parseAllowedNames(bo.getBodySchemaJson());
        ThirdEndpointSecurity.parseSensitiveFields(bo.getSensitiveFieldsJson());
        entity.setPathSchemaJson(bo.getPathSchemaJson());
        entity.setQuerySchemaJson(bo.getQuerySchemaJson());
        entity.setHeaderSchemaJson(bo.getHeaderSchemaJson());
        entity.setBodySchemaJson(bo.getBodySchemaJson());
        entity.setResponseSchemaJson(bo.getResponseSchemaJson());
        entity.setOverrideJson(bo.getOverrideJson());
        if (!"0".equals(bo.getStatus()) && !"1".equals(bo.getStatus())) throw new ServiceException("Endpoint status is invalid");
        entity.setStatus(bo.getStatus());
        entity.setIdempotent(Boolean.TRUE.equals(bo.getIdempotent()));
        if (bo.getRetryCount() != null && bo.getRetryCount() > 0 && !Boolean.TRUE.equals(bo.getIdempotent())) throw new ServiceException("Only idempotent endpoints may retry");
        entity.setRateLimit(capped(nonNegative(bo.getRateLimit()), provider.getRateLimit()));
        entity.setConcurrencyLimit(capped(nonNegative(bo.getConcurrencyLimit()), provider.getConcurrencyLimit()));
        entity.setRetryCount(Math.min(nonNegative(bo.getRetryCount()), Boolean.TRUE.equals(bo.getIdempotent()) ? 3 : 0));
        entity.setSensitiveFieldsJson(bo.getSensitiveFieldsJson());
        entity.setAdapterCode(bo.getAdapterCode() == null || bo.getAdapterCode().isBlank()
            ? null : ThirdEndpointSecurity.validateIdentifier(bo.getAdapterCode(), "Adapter code"));
        adapterRegistry.find(entity.getProviderCode(), entity.getEndpointCode(), entity.getAdapterCode());
        int changed = bo.getEndpointId() == null ? endpointDao.insert(entity) : endpointDao.update(entity);
        if (changed != 1) throw new ServiceException("Endpoint save failed");
        configCache.evict(entity.getProviderCode(), entity.getEndpointCode());
    }

    public void changeStatus(Long endpointId, String status) {
        ThirdEndpoint entity = required(endpointId);
        entity.setStatus("1".equals(status) ? "1" : "0");
        endpointDao.update(entity);
        configCache.evict(entity.getProviderCode(), entity.getEndpointCode());
    }

    public void remove(Long endpointId) {
        ThirdEndpoint entity = required(endpointId);
        if (!"1".equals(entity.getStatus())) throw new ServiceException("Disable endpoint before deleting");
        entity.setDelFlag("1");
        endpointDao.update(entity);
        configCache.evict(entity.getProviderCode(), entity.getEndpointCode());
    }

    private ThirdEndpoint required(Long id) {
        ThirdEndpoint value = endpointDao.findActiveById(id);
        if (value == null) throw new ServiceException("Endpoint not found");
        return value;
    }

    private static int nonNegative(Integer value) {
        if (value == null) return 0;
        if (value < 0) throw new ServiceException("Limit cannot be negative");
        return value;
    }

    private static int capped(int child, Integer parent) {
        return parent == null || parent <= 0 ? child : Math.min(child, parent);
    }

    private static ThirdEndpointVo toVo(ThirdEndpoint x) {
        return new ThirdEndpointVo(x.getEndpointId(), x.getProviderId(), x.getProviderCode(), x.getEndpointCode(), x.getEndpointName(),
            x.getHttpMethod(), x.getRelativePath(), x.getRequestMode(), x.getResponseMode(), x.getPathSchemaJson(), x.getQuerySchemaJson(),
            x.getHeaderSchemaJson(), x.getBodySchemaJson(), x.getResponseSchemaJson(), x.getOverrideJson(), x.getStatus(), x.getIdempotent(),
            x.getRateLimit(), x.getConcurrencyLimit(), x.getRetryCount(), x.getSensitiveFieldsJson(), x.getAdapterCode(), x.getVersion(),
            x.getCreateTime(), x.getUpdateTime());
    }
}
