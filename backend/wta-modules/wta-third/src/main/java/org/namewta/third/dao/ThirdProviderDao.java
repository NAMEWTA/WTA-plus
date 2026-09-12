package org.namewta.third.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.mapper.ThirdEndpointMapper;
import org.namewta.third.mapper.ThirdProviderMapper;
import org.namewta.third.port.ThirdProviderConfigStore;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ThirdProviderDao implements ThirdProviderConfigStore {
    private final ThirdProviderMapper providerMapper;
    private final ThirdEndpointMapper endpointMapper;

    public List<ThirdProvider> findActive(String keyword) {
        LambdaQueryWrapper<ThirdProvider> query = new LambdaQueryWrapper<ThirdProvider>()
            .eq(ThirdProvider::getDelFlag, "0").orderByAsc(ThirdProvider::getProviderId);
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            query.and(q -> q.like(ThirdProvider::getProviderCode, value).or().like(ThirdProvider::getProviderName, value));
        }
        return providerMapper.selectList(query);
    }

    public ThirdProvider findActiveById(Long providerId) {
        return providerMapper.selectOne(new LambdaQueryWrapper<ThirdProvider>()
            .eq(ThirdProvider::getProviderId, providerId).eq(ThirdProvider::getDelFlag, "0"));
    }

    public ThirdProvider findActiveByCode(String providerCode) {
        return providerMapper.selectOne(new LambdaQueryWrapper<ThirdProvider>()
            .eq(ThirdProvider::getProviderCode, providerCode).eq(ThirdProvider::getDelFlag, "0"));
    }

    public boolean existsCode(String providerCode, Long excludedId) {
        return providerMapper.selectCount(new LambdaQueryWrapper<ThirdProvider>()
            .eq(ThirdProvider::getProviderCode, providerCode).eq(ThirdProvider::getDelFlag, "0")
            .ne(excludedId != null, ThirdProvider::getProviderId, excludedId)) > 0;
    }

    public int insert(ThirdProvider provider) { return providerMapper.insert(provider); }

    public int update(ThirdProvider provider) { return providerMapper.updateById(provider); }

    public long countActiveEndpoints(Long providerId) {
        return endpointMapper.selectCount(new LambdaQueryWrapper<ThirdEndpoint>()
            .eq(ThirdEndpoint::getProviderId, providerId).eq(ThirdEndpoint::getDelFlag, "0"));
    }
}
