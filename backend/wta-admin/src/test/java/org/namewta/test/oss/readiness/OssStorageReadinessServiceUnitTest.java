package org.namewta.test.oss.readiness;

import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.model.OssAccessDiagnostic;
import org.namewta.system.domain.SysOssConfig;
import org.namewta.system.mapper.SysOssConfigMapper;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.readiness.OssReadinessClientProvider;
import org.namewta.system.oss.readiness.OssRequiredConfigContributor;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.namewta.system.oss.readiness.OssStorageReadinessService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class OssStorageReadinessServiceUnitTest {

    @Test
    void discoversDefaultUploadStoredObjectAndKeepsUnreferencedFailureObservable() {
        Fixture fixture = fixture();
        SysOssConfig defaultPrivate = config("private", "0", "Y", null);
        SysOssConfig uploadPublic = config("public", "2", "N", "cdn.example.test");
        SysOssConfig storedPrivate = config("archive", "0", "N", null);
        SysOssConfig placeholder = config("placeholder", "0", "N", null);
        when(fixture.configMapper.selectList()).thenReturn(List.of(
            defaultPrivate, uploadPublic, storedPrivate, placeholder));
        when(fixture.ossMapper.selectObjs(any())).thenReturn(List.of("archive"));
        fixture.properties.setDiagnosticObjects(Map.of(
            "private", "diagnostic/private.txt",
            "public", "diagnostic/public.txt",
            "archive", "diagnostic/archive.txt"
        ));
        serving(fixture, "private", AccessPolicy.PRIVATE);
        serving(fixture, "public", AccessPolicy.PUBLIC_READ);
        serving(fixture, "archive", AccessPolicy.PRIVATE);

        fixture.service.refresh();

        Map<String, OssStorageReadinessEntry> snapshot = fixture.registry.snapshot();
        assertThat(fixture.registry.overallServing()).isTrue();
        assertThat(snapshot.get("private").requiredBy()).containsExactly("DEFAULT");
        assertThat(snapshot.get("public").required()).isFalse();
        assertThat(snapshot.get("public").requiredBy()).isEmpty();
        assertThat(snapshot.get("archive").requiredBy()).containsExactly("STORED_OBJECT");
        assertThat(snapshot.get("placeholder").required()).isFalse();
        assertThat(snapshot.get("placeholder").reason())
            .isEqualTo(OssStorageReadinessEntry.Reason.DIAGNOSTIC_OBJECT_MISSING);
    }

    @Test
    void publicDomainMissingAndRequiredConfigMissingFailClosed() {
        Fixture fixture = fixture();
        SysOssConfig publicConfig = config("public", "2", "Y", null);
        when(fixture.configMapper.selectList()).thenReturn(List.of(publicConfig));
        when(fixture.ossMapper.selectObjs(any())).thenReturn(List.of("missing-config"));
        fixture.properties.setDiagnosticObjects(Map.of("public", "diagnostic/public.txt"));

        fixture.service.refresh();

        assertThat(fixture.registry.overallServing()).isFalse();
        assertThat(fixture.registry.snapshot().get("public").reason())
            .isEqualTo(OssStorageReadinessEntry.Reason.DOMAIN_REQUIRED);
        assertThat(fixture.registry.snapshot().get("missing-config").reason())
            .isEqualTo(OssStorageReadinessEntry.Reason.CONFIG_MISSING);
        verify(fixture.clientProvider, never()).client(any());
    }

    @Test
    void providerMismatchAndDiscoveryFailureNeverServe() {
        Fixture fixture = fixture();
        when(fixture.configMapper.selectList()).thenReturn(List.of(config("private", "0", "Y", null)));
        when(fixture.ossMapper.selectObjs(any())).thenReturn(List.of());
        fixture.properties.setDiagnosticObjects(Map.of("private", "diagnostic/private.txt"));
        OssClient client = mock(OssClient.class);
        when(fixture.clientProvider.client("private")).thenReturn(client);
        when(client.diagnoseAccess(eq("diagnostic/private.txt"), eq(AccessPolicy.PRIVATE), any()))
            .thenReturn(new OssAccessDiagnostic(OssAccessDiagnostic.Verification.MISMATCH,
                OssAccessDiagnostic.Reason.ANONYMOUS_READ_MISMATCH, AccessPolicy.PRIVATE, List.of(), Instant.now()));

        fixture.service.refresh();
        assertThat(fixture.registry.overallServing()).isFalse();
        assertThat(fixture.registry.snapshot().get("private").reason())
            .isEqualTo(OssStorageReadinessEntry.Reason.PROVIDER_MISMATCH);

        when(fixture.configMapper.selectList()).thenThrow(new IllegalStateException("database secret detail"));
        fixture.service.refresh();
        assertThat(fixture.registry.discoverySucceeded()).isFalse();
        assertThat(fixture.registry.snapshot()).isEmpty();
    }

    @Test
    void contributorAddsMigrationConfigToRequiredSet() {
        OssRequiredConfigContributor contributor = () -> Map.of("migration-target", Set.of("MIGRATION_TARGET"));
        Fixture fixture = fixture(List.of(contributor));
        SysOssConfig target = config("migration-target", "0", "N", null);
        when(fixture.configMapper.selectList()).thenReturn(List.of(target));
        when(fixture.ossMapper.selectObjs(any())).thenReturn(List.of());
        fixture.properties.setDiagnosticObjects(Map.of("migration-target", "diagnostic/private.txt"));
        serving(fixture, "migration-target", AccessPolicy.PRIVATE);

        fixture.service.refresh();

        assertThat(fixture.registry.overallServing()).isTrue();
        assertThat(fixture.registry.snapshot().get("migration-target").requiredBy())
            .containsExactly("MIGRATION_TARGET");
    }

    @Test
    void oneConfigDiagnosisUsesOnlySelectedRowAndBoundsEachProviderStep() {
        Fixture fixture = fixture();
        SysOssConfig selected = config("private", "0", "N", null);
        when(fixture.configMapper.selectById(42L)).thenReturn(selected);
        fixture.properties.setDiagnosticObjects(Map.of("private", "diagnostic/private.txt"));
        fixture.properties.setDiagnosticTimeout("3s");
        OssClient client = mock(OssClient.class);
        when(fixture.clientProvider.client("private")).thenReturn(client);
        Instant observedAt = Instant.parse("2026-09-23T00:00:00Z");
        var observedGet = new OssAccessDiagnostic.Fact(OssAccessDiagnostic.Subject.OBJECT_GET,
            OssAccessDiagnostic.Observation.DENIED, OssAccessDiagnostic.Source.ANONYMOUS_GET,
            OssAccessDiagnostic.Scope.OBJECT, OssAccessDiagnostic.Basis.HTTP_DENIED, observedAt);
        when(client.diagnoseAccess(any(), eq(AccessPolicy.PRIVATE), any()))
            .thenReturn(new OssAccessDiagnostic(OssAccessDiagnostic.Verification.VERIFIED,
                OssAccessDiagnostic.Reason.READY, AccessPolicy.PRIVATE, List.of(observedGet), Instant.now()));

        var result = fixture.service.diagnoseOne(42L);

        assertThat(result.status()).isEqualTo("SERVING");
        assertThat(result.reason()).isEqualTo("READY");
        assertThat(result.checkedAt()).isNotNull();
        assertThat(result.facts()).singleElement().satisfies(fact -> {
            assertThat(fact.subject()).isEqualTo("OBJECT_GET");
            assertThat(fact.observation()).isEqualTo("DENIED");
            assertThat(fact.source()).isEqualTo("ANONYMOUS_GET");
            assertThat(fact.scope()).isEqualTo("OBJECT");
            assertThat(fact.basis()).isEqualTo("HTTP_DENIED");
            assertThat(fact.observedAt()).isEqualTo(observedAt);
        });
        verify(client).diagnoseAccess("diagnostic/private.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(3));
        verify(fixture.configMapper, never()).selectList();
        verify(fixture.ossMapper, never()).selectObjs(any());
    }

    @Test
    void badOptionalDurationReturnsFixedReasonWithoutProviderOrGlobalDiscovery() {
        Fixture fixture = fixture();
        when(fixture.configMapper.selectById(43L)).thenReturn(config("private", "0", "N", null));
        fixture.properties.setDiagnosticTimeout("invalid-secret-looking-value");

        var result = fixture.service.diagnoseOne(43L);

        assertThat(result.status()).isEqualTo("NOT_SERVING");
        assertThat(result.reason()).isEqualTo("DIAGNOSTIC_CONFIG_INVALID");
        verify(fixture.clientProvider, never()).client(any());
        verify(fixture.configMapper, never()).selectList();
    }

    @Test
    void configurationCommitDuringProviderProbeRejectsLateSuccess() {
        Fixture fixture = fixture();
        when(fixture.configMapper.selectById(44L)).thenReturn(config("private", "0", "N", null));
        fixture.properties.setDiagnosticObjects(Map.of("private", "diagnostic/private.txt"));
        OssClient client = mock(OssClient.class);
        when(fixture.clientProvider.client("private")).thenReturn(client);
        when(client.diagnoseAccess(any(), eq(AccessPolicy.PRIVATE), any())).thenAnswer(invocation -> {
            fixture.registry.invalidate("private");
            return new OssAccessDiagnostic(OssAccessDiagnostic.Verification.VERIFIED,
                OssAccessDiagnostic.Reason.READY, AccessPolicy.PRIVATE, List.of(), Instant.now());
        });

        var result = fixture.service.diagnoseOne(44L);

        assertThat(result.status()).isEqualTo("NOT_SERVING");
        assertThat(result.reason()).isEqualTo("STALE");
        assertThat(fixture.registry.snapshot()).isEmpty();
    }

    private void serving(Fixture fixture, String key, AccessPolicy policy) {
        OssClient client = mock(OssClient.class);
        when(fixture.clientProvider.client(key)).thenReturn(client);
        when(client.diagnoseAccess(any(), eq(policy), any())).thenReturn(new OssAccessDiagnostic(
            OssAccessDiagnostic.Verification.VERIFIED, OssAccessDiagnostic.Reason.READY, policy,
            List.of(), Instant.now()));
    }

    private Fixture fixture() {
        return fixture(List.of());
    }

    private Fixture fixture(List<OssRequiredConfigContributor> contributors) {
        SysOssConfigMapper configMapper = mock(SysOssConfigMapper.class);
        SysOssMapper ossMapper = mock(SysOssMapper.class);
        OssStorageReadinessProperties properties = new OssStorageReadinessProperties();
        OssStorageReadinessRegistry registry = new OssStorageReadinessRegistry(properties);
        OssReadinessClientProvider clientProvider = mock(OssReadinessClientProvider.class);
        OssStorageReadinessService service = new OssStorageReadinessService(configMapper, ossMapper,
            properties, registry, clientProvider, contributors);
        return new Fixture(configMapper, ossMapper, properties, registry, clientProvider, service);
    }

    private SysOssConfig config(String key, String policy, String status, String domain) {
        SysOssConfig config = new SysOssConfig();
        config.setConfigKey(key);
        config.setAccessPolicy(policy);
        config.setStatus(status);
        config.setDomainUrl(domain);
        return config;
    }

    private record Fixture(SysOssConfigMapper configMapper, SysOssMapper ossMapper,
                           OssStorageReadinessProperties properties,
                           OssStorageReadinessRegistry registry, OssReadinessClientProvider clientProvider,
                           OssStorageReadinessService service) {
    }
}
