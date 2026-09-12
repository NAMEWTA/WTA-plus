package org.namewta.third.service;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.third.dao.ThirdProviderDao;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.domain.bo.ThirdProviderBo;
import org.namewta.third.domain.vo.ThirdProviderVo;
import org.namewta.third.port.ThirdConfigSnapshotPort;
import org.namewta.third.support.ThirdEndpointSecurity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThirdProviderService {
    private static final String ENABLED = "0";
    private final ThirdProviderDao providerDao;
    private final ThirdConfigSnapshotPort configCache;

    public List<ThirdProviderVo> list(String keyword) {
        return providerDao.findActive(keyword).stream().map(ThirdProviderService::toVo).toList();
    }

    public ThirdProviderVo get(Long providerId) { return toVo(required(providerId)); }

    public void save(ThirdProviderBo bo) {
        ThirdProvider current = bo.getProviderId() == null ? null : required(bo.getProviderId());
        String providerCode = ThirdEndpointSecurity.validateIdentifier(bo.getProviderCode(), "Provider code");
        if (current != null && !current.getProviderCode().equals(providerCode)) {
            throw new ServiceException("Provider code cannot be changed");
        }
        if (providerDao.existsCode(providerCode, bo.getProviderId())) throw new ServiceException("Provider code already exists");
        ThirdProvider entity = current == null ? new ThirdProvider() : current;
        if (current == null) { entity.setProviderId(IdGeneratorUtil.nextLongId()); entity.setVersion(0); entity.setDelFlag("0"); }
        entity.setProviderCode(providerCode);
        entity.setProviderName(bo.getProviderName().trim());
        entity.setBaseUrl(normalizeBaseUrl(bo.getBaseUrl()));
        entity.setStatus(normalizeStatus(bo.getStatus()));
        entity.setTimeoutConnectMs(positive(bo.getTimeoutConnectMs(), 3000));
        entity.setTimeoutReadMs(positive(bo.getTimeoutReadMs(), 10000));
        entity.setRateLimit(nonNegative(bo.getRateLimit()));
        entity.setConcurrencyLimit(nonNegative(bo.getConcurrencyLimit()));
        ThirdEndpointSecurity.validateSharedHeadersJson(bo.getSharedHeadersJson());
        entity.setSharedHeadersJson(bo.getSharedHeadersJson());
        entity.setRemark(bo.getRemark());
        int changed = current == null ? providerDao.insert(entity) : providerDao.update(entity);
        if (changed != 1) throw new ServiceException("Provider save failed");
        configCache.evict(entity.getProviderCode(), null);
    }

    public void changeStatus(Long providerId, String status) {
        ThirdProvider entity = required(providerId);
        entity.setStatus("1".equals(status) ? "1" : ENABLED);
        providerDao.update(entity);
        configCache.evict(entity.getProviderCode(), null);
    }

    public void remove(Long providerId) {
        ThirdProvider entity = required(providerId);
        if (!"1".equals(entity.getStatus())) throw new ServiceException("Disable provider before deleting");
        if (providerDao.countActiveEndpoints(providerId) > 0) throw new ServiceException("Remove endpoints before deleting provider");
        entity.setDelFlag("1");
        providerDao.update(entity);
        configCache.evict(entity.getProviderCode(), null);
    }

    private ThirdProvider required(Long id) {
        ThirdProvider value = providerDao.findActiveById(id);
        if (value == null) throw new ServiceException("Provider not found");
        return value;
    }

    private static String normalizeBaseUrl(String url) {
        return ThirdEndpointSecurity.validateBaseUrl(url);
    }

    private static String normalizeStatus(String status) {
        if (!"0".equals(status) && !"1".equals(status)) throw new ServiceException("Provider status is invalid");
        return status;
    }

    private static int positive(Integer value, int fallback) {
        if (value == null) return fallback;
        if (value < 100) throw new ServiceException("Timeout must be at least 100 ms");
        return value;
    }

    private static int nonNegative(Integer value) {
        if (value == null) return 0;
        if (value < 0) throw new ServiceException("Limit cannot be negative");
        return value;
    }

    private static ThirdProviderVo toVo(ThirdProvider x) {
        return new ThirdProviderVo(x.getProviderId(), x.getProviderCode(), x.getProviderName(), x.getBaseUrl(), x.getStatus(),
            x.getTimeoutConnectMs(), x.getTimeoutReadMs(), x.getRateLimit(), x.getConcurrencyLimit(), x.getSharedHeadersJson(),
            x.getRemark(), x.getVersion(), x.getCreateTime(), x.getUpdateTime());
    }
}
