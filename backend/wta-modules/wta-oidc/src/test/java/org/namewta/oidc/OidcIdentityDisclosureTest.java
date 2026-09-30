package org.namewta.oidc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.namewta.oidc.domain.OidcPrincipal;
import org.namewta.oidc.port.OidcIdentityPort;
import org.namewta.oidc.service.OidcIdentityService;
import org.namewta.profile.api.domain.*;
import org.namewta.sso.api.SsoSessionSnapshot;
import org.namewta.system.api.domain.AccountIdentity;

import java.time.*;
import java.util.*;

@Tag("dev")
class OidcIdentityDisclosureTest {
    @Test
    void snapshotAndCurrentPolicyAndScopesAllConstrainDisclosure() {
        var port = mock(OidcIdentityPort.class);
        var now = Instant.now();
        var p =
                new OidcPrincipal(
                        "stable",
                        1L,
                        "sid",
                        now.getEpochSecond(),
                        now.plusSeconds(600).getEpochSecond(),
                        Set.of("email", "person_full_name", "person_document_number"));
        when(port.session("sid"))
                .thenReturn(new SsoSessionSnapshot(1L, "alice", now, now.plusSeconds(600)));
        when(port.account(1L))
                .thenReturn(
                        new AccountIdentity(
                                1L,
                                "alice",
                                "Alice",
                                "alice@example.com",
                                "13800138000",
                                null,
                                "0"));
        when(port.profile(eq(1L), anySet()))
                .thenReturn(
                        new ProfileDisclosure(
                                1L,
                                new PersonDisclosure(
                                        null,
                                        null,
                                        null,
                                        "张三",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "full-number-must-not-escape",
                                        null,
                                        null),
                                null));
        var result =
                new OidcIdentityService(port)
                        .userInfo(
                                p,
                                Set.of("email", "person_full_name"),
                                Set.of("openid", "wta_person"));
        assertThat(result)
                .containsEntry("sub", "stable")
                .doesNotContainKeys("email", "person_full_name", "person_document_number");
        assertThat(result.get("wta_person")).isEqualTo(Map.of("full_name", "张三"));
        verify(port).profile(1L, Set.of(ProfileDisclosureField.PERSON_FULL_NAME));
    }

    @Test
    void fullDocumentRequiresBothExplicitGrantAndCurrentFieldAndScope() {
        var port = mock(OidcIdentityPort.class);
        var now = Instant.now();
        var p =
                new OidcPrincipal(
                        "stable",
                        1L,
                        "sid",
                        now.getEpochSecond(),
                        now.plusSeconds(600).getEpochSecond(),
                        Set.of("person_document_number"));
        when(port.session("sid"))
                .thenReturn(new SsoSessionSnapshot(1L, "alice", now, now.plusSeconds(600)));
        when(port.account(1L))
                .thenReturn(new AccountIdentity(1L, "alice", "Alice", null, null, null, "0"));
        when(port.profile(eq(1L), anySet()))
                .thenReturn(
                        new ProfileDisclosure(
                                1L,
                                new PersonDisclosure(
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "explicitly-approved",
                                        null,
                                        null),
                                null));
        var service = new OidcIdentityService(port);
        assertThat(service.userInfo(p, Set.of("person_document_number"), Set.of("openid")))
                .containsOnlyKeys("sub");
        verify(port, never()).profile(anyLong(), anySet());
        assertThat(
                        service.userInfo(
                                p,
                                Set.of("person_document_number"),
                                Set.of("openid", "wta_person")))
                .containsKey("wta_person");
    }

    @Test
    void deletedOrDisabledAccountCannotUseOldSession() {
        var port = mock(OidcIdentityPort.class);
        var now = Instant.now();
        var p =
                new OidcPrincipal(
                        "stable",
                        1L,
                        "sid",
                        now.getEpochSecond(),
                        now.plusSeconds(600).getEpochSecond(),
                        Set.of());
        when(port.session("sid"))
                .thenReturn(new SsoSessionSnapshot(1L, "alice", now, now.plusSeconds(600)));
        assertThatThrownBy(
                        () -> new OidcIdentityService(port).userInfo(p, Set.of(), Set.of("openid")))
                .isInstanceOf(
                        org.springframework.security.oauth2.core.OAuth2AuthenticationException
                                .class);
    }
}
