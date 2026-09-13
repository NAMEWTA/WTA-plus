package org.namewta.sso.service;

import org.namewta.sso.domain.SsoAuthorizationCode;
import org.namewta.sso.dao.SsoAuthorizationCodeDao;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试用授权码存储，CAS 消费。
 */
final class InMemorySsoAuthorizationCodeStore implements SsoAuthorizationCodeDao {

    private final ConcurrentHashMap<String, SsoAuthorizationCode> codes = new ConcurrentHashMap<>();

    @Override
    public void insert(SsoAuthorizationCode code) {
        codes.put(code.getAuthorizationCode(), copy(code));
    }

    @Override
    public SsoAuthorizationCode findByCode(String authorizationCode) {
        SsoAuthorizationCode current = codes.get(authorizationCode);
        return current == null ? null : copy(current);
    }

    @Override
    public boolean consumeIfUnconsumed(String authorizationCode, Integer version) {
        SsoAuthorizationCode current = codes.get(authorizationCode);
        if (current == null || Boolean.TRUE.equals(current.getConsumed())) {
            return false;
        }
        if (version != null && !version.equals(current.getVersion())) {
            return false;
        }
        synchronized (current) {
            if (Boolean.TRUE.equals(current.getConsumed())) {
                return false;
            }
            current.setConsumed(Boolean.TRUE);
            current.setVersion(current.getVersion() == null ? 1 : current.getVersion() + 1);
            return true;
        }
    }

    private static SsoAuthorizationCode copy(SsoAuthorizationCode source) {
        SsoAuthorizationCode copy = new SsoAuthorizationCode();
        copy.setAuthorizationCodeId(source.getAuthorizationCodeId());
        copy.setAuthorizationCode(source.getAuthorizationCode());
        copy.setClientId(source.getClientId());
        copy.setRedirectUri(source.getRedirectUri());
        copy.setCodeChallenge(source.getCodeChallenge());
        copy.setState(source.getState());
        copy.setUserId(source.getUserId());
        copy.setUsername(source.getUsername());
        copy.setConsumed(source.getConsumed());
        copy.setExpireTime(source.getExpireTime());
        copy.setVersion(source.getVersion());
        return copy;
    }
}
