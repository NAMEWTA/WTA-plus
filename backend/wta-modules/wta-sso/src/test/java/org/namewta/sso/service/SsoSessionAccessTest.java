package org.namewta.sso.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.sso.adapter.api.SsoSessionAccessAdapter;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.port.SsoIdentityPort;
import org.namewta.sso.port.SsoSessionPort;
import org.namewta.sso.support.SsoSessionRecords;
import org.namewta.sso.usecase.impl.SsoSessionUseCaseImpl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("local")
@Tag("dev")
class SsoSessionAccessTest {
    private static final Instant AUTH = Instant.parse("2026-09-30T10:00:00Z");

    @Test
    void persistenceRecordsOriginalTimesAndIgnoresInputMetadataWithoutMutatingIt() {
        var identity = new SsoAuthenticatedUser(19L, "identity");
        identity.setAuthenticatedAt(AUTH.minusSeconds(10000));
        var stored = SsoSessionRecords.create(identity, AUTH, Duration.ofHours(8));
        assertThat(stored).isNotSameAs(identity);
        assertThat(stored.getAuthenticatedAt()).isEqualTo(AUTH);
        assertThat(stored.getExpiresAt()).isEqualTo(AUTH.plus(Duration.ofHours(8)));
        assertThat(identity.getAuthenticatedAt()).isEqualTo(AUTH.minusSeconds(10000));
        assertThatThrownBy(() -> SsoSessionRecords.create(identity, AUTH, Duration.ZERO))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publicAdapterPreservesAuthTimeAndExpiryAcrossReadsAndDelegatesLogout() {
        SsoSessionPort store = mock(SsoSessionPort.class);
        when(store.find("session")).thenReturn(SsoSessionRecords.create(
            new SsoAuthenticatedUser(19L, "identity"), AUTH, Duration.ofHours(8)));
        var adapter = adapter(store, AUTH.plusSeconds(300));
        assertThat(adapter.current("session").authenticatedAt()).isEqualTo(AUTH);
        assertThat(adapter.current("session").expiresAt()).isEqualTo(AUTH.plus(Duration.ofHours(8)));
        assertThat(adapter(store, AUTH.plusSeconds(600)).current("session").authenticatedAt()).isEqualTo(AUTH);
        adapter.logout("session");
        verify(store).delete("session");
    }

    @Test
    void legacySessionRemainsReadableForFirstPartyButCannotSupplyOidcAuthTime() {
        SsoSessionPort store = mock(SsoSessionPort.class);
        var legacy = new SsoAuthenticatedUser(19L, "legacy");
        when(store.find("legacy")).thenReturn(legacy);
        var service = new SsoSessionService(mock(SsoIdentityPort.class), store, Clock.fixed(AUTH, ZoneOffset.UTC));
        assertThat(service.current("legacy")).isSameAs(legacy);
        assertThat(service.currentSnapshot("legacy")).isNull();
    }

    @Test
    void rejectsFutureAuthTimeExpiredAndMissingSession() {
        SsoSessionPort store = mock(SsoSessionPort.class);
        when(store.find("expired")).thenReturn(SsoSessionRecords.create(
            new SsoAuthenticatedUser(19L, "user"), AUTH.minusSeconds(60), Duration.ofSeconds(60)));
        when(store.find("future")).thenReturn(SsoSessionRecords.create(
            new SsoAuthenticatedUser(19L, "user"), AUTH.plusSeconds(60), Duration.ofHours(1)));
        var access = adapter(store, AUTH);
        assertThat(access.current("expired")).isNull();
        assertThat(access.current("future")).isNull();
        assertThat(access.current("missing")).isNull();
    }

    private SsoSessionAccessAdapter adapter(SsoSessionPort store, Instant now) {
        return new SsoSessionAccessAdapter(new SsoSessionUseCaseImpl(
            new SsoSessionService(mock(SsoIdentityPort.class), store, Clock.fixed(now, ZoneOffset.UTC))));
    }
}

