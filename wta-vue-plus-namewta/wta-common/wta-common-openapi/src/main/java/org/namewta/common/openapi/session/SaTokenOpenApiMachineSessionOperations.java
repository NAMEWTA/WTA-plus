package org.namewta.common.openapi.session;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.util.SaTokenConsts;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.model.LoginUser;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Sa-Token adapter for server-only OpenAPI machine sessions.
 */
public class SaTokenOpenApiMachineSessionOperations implements OpenApiMachineSessionOperations {

    static final String MACHINE_DEVICE_PREFIX = "openapi:credential:";
    static final String SESSION_CREDENTIAL_ID = "openapiCredentialId";
    static final String SESSION_OWNER_USER_ID = "openapiOwnerUserId";
    private static final String USER_LOCK_PREFIX = "openapi:machine-session:user:";

    private final StpLogic stpLogic;
    private final RedissonClient redissonClient;
    private final Supplier<SaStorage> storageSupplier;

    public SaTokenOpenApiMachineSessionOperations(StpLogic stpLogic, RedissonClient redissonClient) {
        this(stpLogic, redissonClient, SaHolder::getStorage);
    }

    SaTokenOpenApiMachineSessionOperations(StpLogic stpLogic, RedissonClient redissonClient,
                                           Supplier<SaStorage> storageSupplier) {
        this.stpLogic = Objects.requireNonNull(stpLogic, "stpLogic");
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient");
        this.storageSupplier = Objects.requireNonNull(storageSupplier, "storageSupplier");
    }

    @Override
    public LoginUser find(VerifiedOpenApiIdentity identity) {
        String token = findToken(identity);
        return token == null ? null : loginUser(token, identity);
    }

    @Override
    public void create(VerifiedOpenApiIdentity identity, LoginUser loginUser, Duration ttl) {
        long ttlSeconds = Math.max(1, ttl.toSeconds());
        SaLoginParameter parameter = new SaLoginParameter()
            .setDeviceType(deviceType(identity.credentialId()))
            .setDeviceId(deviceType(identity.credentialId()))
            .setIsShare(true)
            .setTimeout(ttlSeconds)
            .setRightNowCreateTokenSession(true)
            .setIsWriteHeader(false)
            .setExtra(LoginHelper.USER_KEY, loginUser.getUserId())
            .setExtra(LoginHelper.USER_NAME_KEY, loginUser.getUsername())
            .setExtra(LoginHelper.DEPT_KEY, loginUser.getDeptId())
            .setExtra(LoginHelper.DEPT_NAME_KEY, loginUser.getDeptName())
            .setExtra(LoginHelper.DEPT_CATEGORY_KEY, loginUser.getDeptCategory())
            .setExtra(LoginHelper.USER_TYPE_KEY, loginUser.getUserType());
        String token = stpLogic.createLoginSession(loginId(identity.ownerUserId()), parameter);
        SaSession session = stpLogic.getTokenSessionByToken(token, false);
        if (session == null) {
            throw new OpenApiMachineSessionException();
        }
        session.set(LoginHelper.LOGIN_USER_KEY, loginUser);
        session.set(SESSION_CREDENTIAL_ID, identity.credentialId());
        session.set(SESSION_OWNER_USER_ID, identity.ownerUserId());
    }

    @Override
    public <T> T inRequestScope(VerifiedOpenApiIdentity identity, Supplier<T> callback) {
        String token = findToken(identity);
        if (token == null || loginUser(token, identity) == null) {
            throw new OpenApiMachineSessionException();
        }
        SaStorage storage = storageSupplier.get();
        String tokenKey = stpLogic.splicingKeyJustCreatedSave();
        StoredValue previousToken = capture(storage, tokenKey);
        StoredValue previousRawToken = capture(storage, SaTokenConsts.JUST_CREATED_NOT_PREFIX);
        StoredValue previousActiveCheck = capture(storage, SaTokenConsts.TOKEN_ACTIVE_TIMEOUT_CHECKED_KEY);
        try {
            writeToken(storage, tokenKey, token);
            return callback.get();
        } finally {
            restore(storage, SaTokenConsts.TOKEN_ACTIVE_TIMEOUT_CHECKED_KEY, previousActiveCheck);
            restore(storage, SaTokenConsts.JUST_CREATED_NOT_PREFIX, previousRawToken);
            restore(storage, tokenKey, previousToken);
        }
    }

    @Override
    public <T> T withUserLock(Long userId, Supplier<T> callback) {
        RLock lock = redissonClient.getLock(USER_LOCK_PREFIX + userId);
        lock.lock();
        try {
            return callback.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public int invalidateByUserId(Long userId) {
        SaSession accountSession = stpLogic.getSessionByLoginId(loginId(userId), false);
        if (accountSession == null) {
            return 0;
        }
        List<String> tokens = accountSession.terminalListCopy().stream()
            .filter(terminal -> terminal.getDeviceType() != null
                && terminal.getDeviceType().startsWith(MACHINE_DEVICE_PREFIX))
            .map(SaTerminalInfo::getTokenValue)
            .toList();
        tokens.forEach(stpLogic::logoutByTokenValue);
        return tokens.size();
    }

    private String findToken(VerifiedOpenApiIdentity identity) {
        SaSession accountSession = stpLogic.getSessionByLoginId(loginId(identity.ownerUserId()), false);
        if (accountSession == null) {
            return null;
        }
        List<String> matches = new ArrayList<>();
        for (SaTerminalInfo terminal : accountSession.terminalListCopy()) {
            if (deviceType(identity.credentialId()).equals(terminal.getDeviceType())
                && stpLogic.isValidToken(terminal.getTokenValue())
                && loginUser(terminal.getTokenValue(), identity) != null) {
                matches.add(terminal.getTokenValue());
            }
        }
        if (matches.size() > 1) {
            throw new OpenApiMachineSessionException();
        }
        return matches.isEmpty() ? null : matches.getFirst();
    }

    private LoginUser loginUser(String token, VerifiedOpenApiIdentity identity) {
        SaSession session = stpLogic.getTokenSessionByToken(token, false);
        if (session == null
            || !identity.credentialId().equals(session.get(SESSION_CREDENTIAL_ID))
            || !identity.ownerUserId().equals(session.get(SESSION_OWNER_USER_ID))) {
            return null;
        }
        Object value = session.get(LoginHelper.LOGIN_USER_KEY);
        if (!(value instanceof LoginUser loginUser)
            || !identity.ownerUserId().equals(loginUser.getUserId())
            || !"openapi".equals(loginUser.getUserType())
            || loginUser.getClientPk() != null
            || loginUser.getClientKey() != null) {
            return null;
        }
        return loginUser;
    }

    private void writeToken(SaStorage storage, String tokenKey, String token) {
        SaTokenConfig config = stpLogic.getConfigOrGlobal();
        String prefix = config.getTokenPrefix();
        String presentedToken = prefix == null || prefix.isEmpty()
            ? token
            : prefix + SaTokenConsts.TOKEN_CONNECTOR_CHAT + token;
        storage.set(tokenKey, presentedToken);
        storage.set(SaTokenConsts.JUST_CREATED_NOT_PREFIX, token);
        storage.delete(SaTokenConsts.TOKEN_ACTIVE_TIMEOUT_CHECKED_KEY);
    }

    private static StoredValue capture(SaStorage storage, String key) {
        Object value = storage.get(key);
        return new StoredValue(value != null, value);
    }

    private static void restore(SaStorage storage, String key, StoredValue value) {
        if (value.present()) {
            storage.set(key, value.value());
        } else {
            storage.delete(key);
        }
    }

    private static String loginId(Long userId) {
        return "openapi:" + userId;
    }

    private static String deviceType(Long credentialId) {
        return MACHINE_DEVICE_PREFIX + credentialId;
    }

    private record StoredValue(boolean present, Object value) {
    }

}
