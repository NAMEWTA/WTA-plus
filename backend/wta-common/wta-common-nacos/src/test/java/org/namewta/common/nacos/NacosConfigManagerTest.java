package org.namewta.common.nacos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class NacosConfigManagerTest {

    @Test
    void appliesSparseYamlWithoutReplacingLocalValues() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("feature.local-only", "local")
            .withProperty("feature.mode", "local");
        NacosConfigManager manager = manager(environment);

        assertThat(manager.apply("feature:\n  mode: remote\n", NacosUpdateOrigin.LISTENER)).isTrue();

        assertThat(environment.getProperty("feature.mode")).isEqualTo("remote");
        assertThat(environment.getProperty("feature.local-only")).isEqualTo("local");
    }

    @Test
    void rejectsProtectedKeysAndKeepsThePreviousVersion() {
        MockEnvironment environment = new MockEnvironment().withProperty("feature.mode", "local");
        NacosConfigManager manager = manager(environment);
        manager.apply("feature.mode: accepted", NacosUpdateOrigin.LISTENER);
        String digest = manager.state().digest();

        assertThat(manager.apply("feature.mode: rejected\nnacos.config.enabled: false", NacosUpdateOrigin.LISTENER))
            .isFalse();

        assertThat(environment.getProperty("feature.mode")).isEqualTo("accepted");
        assertThat(manager.state().digest()).isEqualTo(digest);
        assertThat(manager.state().errorCode()).isEqualTo("PROTECTED_KEY");
    }

    @Test
    void participantFailureRejectsTheWholeCandidate() {
        MockEnvironment environment = new MockEnvironment().withProperty("sample.count", "3");
        NacosConfigManager manager = manager(environment);
        manager.registerParticipants(Set.of(new IntegerParticipant()));

        assertThat(manager.apply("sample.count: invalid", NacosUpdateOrigin.LISTENER)).isFalse();
        assertThat(environment.getProperty("sample.count")).isEqualTo("3");
        assertThat(manager.state().errorCode()).isEqualTo("PARTICIPANT_REJECTED");
    }

    @Test
    void lateParticipantRejectionRestoresTheLocalBaseline() {
        MockEnvironment environment = new MockEnvironment().withProperty("sample.count", "3");
        NacosConfigManager manager = manager(environment);
        assertThat(manager.apply("sample.count: broken", NacosUpdateOrigin.STARTUP)).isTrue();

        manager.registerParticipants(Set.of(new IntegerParticipant()));

        assertThat(environment.getProperty("sample.count")).isEqualTo("3");
        assertThat(manager.configuration("sample", Integer.class)).contains(3);
        assertThat(manager.state().digest()).isNull();
        assertThat(manager.state().result()).isEqualTo("REJECTED");
        assertThat(manager.state().errorCode()).isEqualTo("PARTICIPANT_REJECTED");
    }

    @Test
    void rejectsKnownInvalidScalarBeforeParticipantsExist() {
        MockEnvironment environment = new MockEnvironment().withProperty("captcha.numberLength", "1");
        NacosConfigManager manager = manager(environment);

        assertThat(manager.apply("captcha.numberLength: broken", NacosUpdateOrigin.STARTUP)).isFalse();

        assertThat(environment.getProperty("captcha.numberLength")).isEqualTo("1");
        assertThat(manager.state().errorCode()).isEqualTo("KNOWN_TYPE_INVALID");
    }

    @Test
    void rejectsKnownCrossFieldViolationsBeforeParticipantsExist() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("notify.idempotency.default-window", "5m")
            .withProperty("notify.idempotency.min-window", "30s")
            .withProperty("notify.idempotency.max-window", "24h")
            .withProperty("oss.lifecycle.download-ttl", "2m")
            .withProperty("oss.lifecycle.download-ttl-min", "1m")
            .withProperty("oss.lifecycle.download-ttl-max", "10m");
        NacosConfigManager manager = manager(environment);

        assertThat(manager.apply("""
            notify:
              idempotency:
                default-window: 7m
                min-window: 10m
                max-window: 5m
            """, NacosUpdateOrigin.STARTUP)).isFalse();
        assertThat(manager.state().errorCode()).isEqualTo("PARTICIPANT_REJECTED");
        assertThat(environment.getProperty("notify.idempotency.default-window")).isEqualTo("5m");

        assertThat(manager.apply("oss.lifecycle.download-ttl: 30m", NacosUpdateOrigin.STARTUP)).isFalse();
        assertThat(manager.state().errorCode()).isEqualTo("PARTICIPANT_REJECTED");
        assertThat(environment.getProperty("oss.lifecycle.download-ttl")).isEqualTo("2m");

        assertThat(manager.apply("captcha.enable: false", NacosUpdateOrigin.LISTENER)).isTrue();
        assertThat(environment.getProperty("captcha.enable")).isEqualTo("false");
    }

    @Test
    void emptyRemoteDocumentRemovesTheOverlay() {
        MockEnvironment environment = new MockEnvironment().withProperty("feature.mode", "local");
        NacosConfigManager manager = manager(environment);
        manager.apply("feature.mode: remote", NacosUpdateOrigin.LISTENER);

        assertThat(manager.apply("", NacosUpdateOrigin.LISTENER)).isTrue();

        assertThat(environment.getProperty("feature.mode")).isEqualTo("local");
        assertThat(manager.state().digest()).isNull();
    }

    @Test
    void classifiesExactKeysWithoutIncludingAdjacentProperties() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("oss.lifecycle.download-ttl", "2m")
            .withProperty("oss.lifecycle.download-ttl-max", "10m");
        NacosConfigManager manager = manager(environment);
        manager.registerParticipants(Set.of(new ExactDurationParticipant()));

        assertThat(manager.apply("""
            oss:
              lifecycle:
                download-ttl: 3m
                download-ttl-max: 20m
            """, NacosUpdateOrigin.LISTENER)).isTrue();

        assertThat(manager.state().immediateKeyCount()).isEqualTo(1);
        assertThat(manager.state().restartKeyCount()).isEqualTo(1);
    }

    @Test
    void participantFailureKeepsEveryPreviouslyPreparedSnapshot() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("first.count", "1")
            .withProperty("second.count", "2");
        NacosConfigManager manager = manager(environment);
        manager.registerParticipants(Set.of(
            new NamedIntegerParticipant("first"),
            new NamedIntegerParticipant("second")));
        assertThat(manager.apply("first.count: 10\nsecond.count: 20", NacosUpdateOrigin.LISTENER)).isTrue();

        assertThat(manager.apply("first.count: 30\nsecond.count: broken", NacosUpdateOrigin.LISTENER)).isFalse();

        assertThat(manager.configuration("first", Integer.class)).contains(10);
        assertThat(manager.configuration("second", Integer.class)).contains(20);
    }

    private static NacosConfigManager manager(MockEnvironment environment) {
        NacosConfigManager manager = new NacosConfigManager(environment, NacosConfigSettings.from(environment));
        NacosPropertySourceRegistrar.register(environment, manager.propertySource());
        return manager;
    }

    private static final class IntegerParticipant implements NacosConfigParticipant<Integer> {

        @Override
        public String id() {
            return "sample";
        }

        @Override
        public Set<String> prefixes() {
            return Set.of("sample.");
        }

        @Override
        public Integer prepare(Binder binder) {
            return binder.bindOrCreate("sample.count", Integer.class);
        }
    }

    private record NamedIntegerParticipant(String id) implements NacosConfigParticipant<Integer> {

        @Override
        public Set<String> prefixes() {
            return Set.of(id + ".");
        }

        @Override
        public Integer prepare(Binder binder) {
            return binder.bindOrCreate(id + ".count", Integer.class);
        }
    }

    private static final class ExactDurationParticipant implements NacosConfigParticipant<String> {

        @Override
        public String id() {
            return "oss-download-ttl";
        }

        @Override
        public Set<String> prefixes() {
            return Set.of();
        }

        @Override
        public Set<String> exactKeys() {
            return Set.of("oss.lifecycle.download-ttl");
        }

        @Override
        public String prepare(Binder binder) {
            return binder.bindOrCreate("oss.lifecycle.download-ttl", String.class);
        }
    }
}
