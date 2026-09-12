package org.namewta.common.openapi.session;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.util.SaTokenConsts;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.model.LoginUser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class SaTokenOpenApiMachineSessionOperationsTest {

    private static final VerifiedOpenApiIdentity IDENTITY = new VerifiedOpenApiIdentity(7L, 9L);
    private static final String MACHINE_TOKEN = "internal-machine-token";

    @Test
    void restoresBrowserStorageAfterNestedAndExceptionalScopes() {
        StpLogic stpLogic = mock(StpLogic.class);
        MapStorage storage = new MapStorage();
        storage.set("JUST_CREATED_", "browser-token");
        storage.set(SaTokenConsts.JUST_CREATED_NOT_PREFIX, "browser-token");
        storage.set(SaTokenConsts.TOKEN_ACTIVE_TIMEOUT_CHECKED_KEY, "browser-active");
        stubMachineSession(stpLogic);
        when(stpLogic.splicingKeyJustCreatedSave()).thenReturn("JUST_CREATED_");
        when(stpLogic.getConfigOrGlobal()).thenReturn(new SaTokenConfig());
        SaTokenOpenApiMachineSessionOperations operations = new SaTokenOpenApiMachineSessionOperations(
            stpLogic, mock(RedissonClient.class), () -> storage);

        assertThatThrownBy(() -> operations.inRequestScope(IDENTITY, () -> {
            assertThat(storage.get("JUST_CREATED_")).isEqualTo(MACHINE_TOKEN);
            storage.set(SaTokenConsts.TOKEN_ACTIVE_TIMEOUT_CHECKED_KEY, "outer-active");
            String nested = operations.inRequestScope(IDENTITY, () -> {
                assertThat(storage.get("JUST_CREATED_")).isEqualTo(MACHINE_TOKEN);
                assertThat(storage.get(SaTokenConsts.TOKEN_ACTIVE_TIMEOUT_CHECKED_KEY)).isNull();
                return "nested";
            });
            assertThat(nested).isEqualTo("nested");
            assertThat(storage.get(SaTokenConsts.TOKEN_ACTIVE_TIMEOUT_CHECKED_KEY)).isEqualTo("outer-active");
            throw new MarkerException();
        })).isInstanceOf(MarkerException.class);

        assertThat(storage.get("JUST_CREATED_")).isEqualTo("browser-token");
        assertThat(storage.get(SaTokenConsts.JUST_CREATED_NOT_PREFIX)).isEqualTo("browser-token");
        assertThat(storage.get(SaTokenConsts.TOKEN_ACTIVE_TIMEOUT_CHECKED_KEY)).isEqualTo("browser-active");
    }

    @Test
    void invalidatesOnlyMachineChannelTokensForTheUser() {
        StpLogic stpLogic = mock(StpLogic.class);
        SaSession accountSession = mock(SaSession.class);
        when(stpLogic.getSessionByLoginId("openapi:9", false)).thenReturn(accountSession);
        when(accountSession.terminalListCopy()).thenReturn(List.of(
            terminal("openapi:credential:7", "machine-7"),
            terminal("openapi:credential:8", "machine-8"),
            terminal("web", "browser")));
        SaTokenOpenApiMachineSessionOperations operations = new SaTokenOpenApiMachineSessionOperations(
            stpLogic, mock(RedissonClient.class), MapStorage::new);

        assertThat(operations.invalidateByUserId(9L)).isEqualTo(2);

        verify(stpLogic).logoutByTokenValue("machine-7");
        verify(stpLogic).logoutByTokenValue("machine-8");
        verify(stpLogic, never()).logoutByTokenValue("browser");
    }

    @Test
    void createsServerOnlySessionWithStandardLoginUserSidecar() {
        StpLogic stpLogic = mock(StpLogic.class);
        SaSession tokenSession = mock(SaSession.class);
        when(stpLogic.createLoginSession(eq("openapi:9"), any(SaLoginParameter.class)))
            .thenReturn(MACHINE_TOKEN);
        when(stpLogic.getTokenSessionByToken(MACHINE_TOKEN, false)).thenReturn(tokenSession);
        SaTokenOpenApiMachineSessionOperations operations = new SaTokenOpenApiMachineSessionOperations(
            stpLogic, mock(RedissonClient.class), MapStorage::new);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        loginUser.setUserType("openapi");

        operations.create(IDENTITY, loginUser, Duration.ofHours(8));

        var parameterCaptor = ArgumentCaptor.forClass(SaLoginParameter.class);
        verify(stpLogic).createLoginSession(eq("openapi:9"), parameterCaptor.capture());
        SaLoginParameter parameter = parameterCaptor.getValue();
        assertThat(parameter.getDeviceType()).isEqualTo("openapi:credential:7");
        assertThat(parameter.getDeviceId()).isEqualTo("openapi:credential:7");
        assertThat(parameter.getIsShare()).isTrue();
        assertThat(parameter.getIsWriteHeader()).isFalse();
        assertThat(parameter.getTimeout()).isEqualTo(Duration.ofHours(8).toSeconds());
        verify(tokenSession).set(LoginHelper.LOGIN_USER_KEY, loginUser);
        verify(tokenSession).set("openapiCredentialId", 7L);
        verify(tokenSession).set("openapiOwnerUserId", 9L);
        verify(stpLogic, never()).setTokenValueToResponseHeader(MACHINE_TOKEN);
    }

    private static void stubMachineSession(StpLogic stpLogic) {
        SaSession accountSession = mock(SaSession.class);
        SaSession tokenSession = mock(SaSession.class);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(9L);
        loginUser.setUserType("openapi");
        when(stpLogic.getSessionByLoginId("openapi:9", false)).thenReturn(accountSession);
        when(accountSession.terminalListCopy()).thenReturn(List.of(
            terminal("openapi:credential:7", MACHINE_TOKEN)));
        when(stpLogic.isValidToken(MACHINE_TOKEN)).thenReturn(true);
        when(stpLogic.getTokenSessionByToken(MACHINE_TOKEN, false)).thenReturn(tokenSession);
        when(tokenSession.get("openapiCredentialId")).thenReturn(7L);
        when(tokenSession.get("openapiOwnerUserId")).thenReturn(9L);
        when(tokenSession.get(LoginHelper.LOGIN_USER_KEY)).thenReturn(loginUser);
    }

    private static SaTerminalInfo terminal(String deviceType, String token) {
        return new SaTerminalInfo().setDeviceType(deviceType).setTokenValue(token);
    }

    private static final class MapStorage implements SaStorage {

        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Object getSource() {
            return values;
        }

        @Override
        public Object get(String key) {
            return values.get(key);
        }

        @Override
        public SaStorage set(String key, Object value) {
            values.put(key, value);
            return this;
        }

        @Override
        public SaStorage delete(String key) {
            values.remove(key);
            return this;
        }

    }

    private static final class MarkerException extends RuntimeException {
    }

}
