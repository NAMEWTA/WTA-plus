package org.namewta.third.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.namewta.third.domain.ThirdCredential;
import org.namewta.third.mapper.ThirdCredentialMapper;
import org.namewta.third.port.ThirdCredentialStore;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ThirdCredentialDao implements ThirdCredentialStore {
    private final ThirdCredentialMapper credentialMapper;

    public List<ThirdCredential> findByScope(Long providerId, Long endpointId) {
        LambdaQueryWrapper<ThirdCredential> query = new LambdaQueryWrapper<ThirdCredential>()
            .eq(ThirdCredential::getDelFlag, "0").eq(ThirdCredential::getProviderId, providerId);
        if (endpointId == null) query.isNull(ThirdCredential::getEndpointId);
        else query.eq(ThirdCredential::getEndpointId, endpointId);
        return credentialMapper.selectList(query);
    }

    @Override
    public List<ThirdCredential> findByScopes(Long providerId, Long endpointId) {
        return credentialMapper.selectList(new LambdaQueryWrapper<ThirdCredential>()
            .eq(ThirdCredential::getProviderId, providerId).eq(ThirdCredential::getDelFlag, "0")
            .and(query -> query.isNull(ThirdCredential::getEndpointId).or().eq(ThirdCredential::getEndpointId, endpointId)));
    }

    public ThirdCredential findById(Long credentialId) { return credentialMapper.selectById(credentialId); }

    public int insert(ThirdCredential credential) { return credentialMapper.insert(credential); }

    public int update(ThirdCredential credential) { return credentialMapper.updateById(credential); }
}
