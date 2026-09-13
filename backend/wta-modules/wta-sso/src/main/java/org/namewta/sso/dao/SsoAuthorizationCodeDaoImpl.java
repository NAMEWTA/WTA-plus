package org.namewta.sso.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.namewta.sso.domain.SsoAuthorizationCode;
import org.namewta.sso.mapper.SsoAuthorizationCodeMapper;
import org.springframework.stereotype.Repository;

/**
 * 授权码 Mapper 访问。
 */
@Repository
@RequiredArgsConstructor
public class SsoAuthorizationCodeDaoImpl implements SsoAuthorizationCodeDao {

    private final SsoAuthorizationCodeMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(SsoAuthorizationCode code) {
        mapper.insert(code);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SsoAuthorizationCode findByCode(String authorizationCode) {
        return mapper.selectOne(new LambdaQueryWrapper<SsoAuthorizationCode>()
            .eq(SsoAuthorizationCode::getAuthorizationCode, authorizationCode)
            .last("limit 1"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean consumeIfUnconsumed(String authorizationCode, Integer version) {
        return mapper.consumeIfUnconsumed(authorizationCode, version) > 0;
    }
}
