package org.namewta.test.nacos.refresh;

import org.namewta.common.nacos.NacosConfigAccessor;
import org.namewta.common.nacos.NacosConfigManager;
import org.namewta.common.nacos.NacosConfigParticipant;
import org.namewta.common.notify.config.NotifyAutoConfiguration;
import org.namewta.common.notify.idempotency.NotifyIdempotencyCoordinator;
import org.namewta.common.notify.idempotency.NotifyIdempotencyProperties;
import org.namewta.common.web.config.properties.CaptchaProperties;
import org.namewta.common.web.config.CaptchaConfig;
import org.namewta.system.oss.config.OssLifecycleProperties;
import org.namewta.web.controller.CaptchaController;
import org.namewta.notify.api.NotificationApplicationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@Tag("dev")
class NacosLiveRefreshContractUnitTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CaptchaConfig.class, NotifyAutoConfiguration.class))
        .withUserConfiguration(OssPropertiesConfiguration.class);

    @Test
    void disabledNacosKeepsLocalBeansAndExposesThreeParticipants() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(NacosConfigManager.class);
            assertThat(context.getBeansOfType(NacosConfigParticipant.class).values())
                .extracting(participant -> ((NacosConfigParticipant<?>) participant).id())
                .containsExactlyInAnyOrder("captcha", "notify-idempotency", "oss-download-ttl");
        });
    }

    @Test
    void captchaBehaviorChangesOnTheNextCallAndDeletionRestoresLocalValues() {
        MutableAccessor accessor = new MutableAccessor();
        CaptchaProperties properties = captchaProperties();
        properties.setNacosConfigAccessor(accessor);
        CaptchaController controller = new CaptchaController(properties,
            mock(NotificationApplicationService.class));

        accessor.replace(Map.of("captcha", new CaptchaProperties.Snapshot(false, "char", 2, 6)));
        assertThat(controller.getCode().getData().captchaEnabled()).isFalse();

        accessor.clear();
        assertThat(properties.currentSnapshot()).isEqualTo(new CaptchaProperties.Snapshot(true, "math", 1, 4));
    }

    @Test
    void notifyWindowChangesOnTheNextCallAndDeletionRestoresLocalValues() {
        MutableAccessor accessor = new MutableAccessor();
        NotifyIdempotencyProperties properties = new NotifyIdempotencyProperties();
        properties.setNacosConfigAccessor(accessor);
        NotifyIdempotencyCoordinator coordinator = new NotifyIdempotencyCoordinator(null, properties);

        accessor.replace(Map.of("notify-idempotency",
            new NotifyIdempotencyProperties.Snapshot(Duration.ofMinutes(8), Duration.ofMinutes(1),
                Duration.ofMinutes(30))));
        assertThat(coordinator.resolveWindow(null)).isEqualTo(Duration.ofMinutes(8));

        accessor.clear();
        assertThat(coordinator.resolveWindow(null)).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void ossDownloadTtlChangesOnTheNextCallAndDeletionRestoresLocalValue() {
        MutableAccessor accessor = new MutableAccessor();
        OssLifecycleProperties properties = new OssLifecycleProperties();
        properties.setNacosConfigAccessor(accessor);

        accessor.replace(Map.of("oss-download-ttl", Duration.ofMinutes(4)));
        assertThat(properties.resolveDownloadTtl(null)).isEqualTo(Duration.ofMinutes(4));

        accessor.clear();
        assertThat(properties.resolveDownloadTtl(null)).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void participantsBindEffectiveValuesAndRejectCrossFieldViolations() {
        CaptchaProperties captcha = captchaProperties();
        CaptchaProperties.Snapshot captchaSnapshot = captcha.prepare(Binder.get(new MockEnvironment()
            .withProperty("captcha.enable", "false")
            .withProperty("captcha.type", "char")
            .withProperty("captcha.number-length", "2")
            .withProperty("captcha.char-length", "6")));
        assertThat(captchaSnapshot).isEqualTo(new CaptchaProperties.Snapshot(false, "char", 2, 6));

        NotifyIdempotencyProperties notify = new NotifyIdempotencyProperties();
        assertThatThrownBy(() -> notify.prepare(Binder.get(new MockEnvironment()
            .withProperty("notify.idempotency.default-window", "2h")
            .withProperty("notify.idempotency.min-window", "1m")
            .withProperty("notify.idempotency.max-window", "1h"))))
            .isInstanceOf(IllegalArgumentException.class);

        OssLifecycleProperties oss = new OssLifecycleProperties();
        assertThatThrownBy(() -> oss.prepare(Binder.get(new MockEnvironment()
            .withProperty("oss.lifecycle.download-ttl", "30m"))))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void concurrentCaptchaReadsObserveOnlyCompleteSnapshots() throws Exception {
        MutableAccessor accessor = new MutableAccessor();
        CaptchaProperties properties = captchaProperties();
        properties.setNacosConfigAccessor(accessor);
        CaptchaProperties.Snapshot first = new CaptchaProperties.Snapshot(true, "math", 1, 4);
        CaptchaProperties.Snapshot second = new CaptchaProperties.Snapshot(false, "char", 2, 6);
        AtomicReference<CaptchaProperties.Snapshot> torn = new AtomicReference<>();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            executor.submit(() -> {
                await(start);
                for (int index = 0; index < 10_000; index++) {
                    accessor.replace(Map.of("captcha", index % 2 == 0 ? first : second));
                }
            });
            executor.submit(() -> {
                await(start);
                for (int index = 0; index < 10_000; index++) {
                    CaptchaProperties.Snapshot observed = properties.currentSnapshot();
                    if (!observed.equals(first) && !observed.equals(second)) {
                        torn.compareAndSet(null, observed);
                    }
                }
            });
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }

        assertThat(torn).hasValue(null);
    }

    @Test
    void realManagerAppliesMixedDocumentAndRejectsInvalidVersionAtomically() throws Exception {
        MockEnvironment environment = localEnvironment();
        NacosConfigManager manager = manager(environment);
        CaptchaProperties captcha = captchaProperties();
        NotifyIdempotencyProperties notify = new NotifyIdempotencyProperties();
        OssLifecycleProperties oss = new OssLifecycleProperties();
        List<NacosConfigParticipant<?>> participants = List.of(captcha, notify, oss);
        ReflectionTestUtils.invokeMethod(manager, "registerParticipants", participants);
        captcha.setNacosConfigAccessor(manager);
        notify.setNacosConfigAccessor(manager);
        oss.setNacosConfigAccessor(manager);

        assertThat(apply(manager, """
            captcha:
              enable: false
            notify:
              idempotency:
                default-window: 8m
                min-window: 1m
                max-window: 30m
            oss:
              lifecycle:
                download-ttl: 4m
            server:
              port: 9090
            """)).isTrue();
        assertThat(captcha.getEnable()).isFalse();
        assertThat(new NotifyIdempotencyCoordinator(null, notify).resolveWindow(null))
            .isEqualTo(Duration.ofMinutes(8));
        assertThat(oss.resolveDownloadTtl(null)).isEqualTo(Duration.ofMinutes(4));
        assertThat(manager.state().immediateKeyCount()).isEqualTo(5);
        assertThat(manager.state().restartKeyCount()).isEqualTo(1);
        String acceptedDigest = manager.state().digest();

        assertThat(apply(manager, """
            captcha:
              enable: true
            notify:
              idempotency:
                default-window: 2h
                min-window: 1m
                max-window: 30m
            oss:
              lifecycle:
                download-ttl: 5m
            """)).isFalse();
        assertThat(captcha.getEnable()).isFalse();
        assertThat(new NotifyIdempotencyCoordinator(null, notify).resolveWindow(null))
            .isEqualTo(Duration.ofMinutes(8));
        assertThat(oss.resolveDownloadTtl(null)).isEqualTo(Duration.ofMinutes(4));
        assertThat(manager.state().digest()).isEqualTo(acceptedDigest);

        assertThat(apply(manager, "")).isTrue();
        assertThat(captcha.getEnable()).isTrue();
        assertThat(new NotifyIdempotencyCoordinator(null, notify).resolveWindow(null))
            .isEqualTo(Duration.ofMinutes(5));
        assertThat(oss.resolveDownloadTtl(null)).isEqualTo(Duration.ofMinutes(2));
    }

    private CaptchaProperties captchaProperties() {
        CaptchaProperties properties = new CaptchaProperties();
        properties.setEnable(true);
        properties.setType("math");
        properties.setNumberLength(1);
        properties.setCharLength(4);
        return properties;
    }

    private MockEnvironment localEnvironment() {
        return new MockEnvironment()
            .withProperty("captcha.enable", "true")
            .withProperty("captcha.type", "math")
            .withProperty("captcha.number-length", "1")
            .withProperty("captcha.char-length", "4")
            .withProperty("notify.idempotency.default-window", "5m")
            .withProperty("notify.idempotency.min-window", "30s")
            .withProperty("notify.idempotency.max-window", "24h")
            .withProperty("oss.lifecycle.download-ttl", "2m");
    }

    private NacosConfigManager manager(MockEnvironment environment) throws Exception {
        Constructor<?> constructor = NacosConfigManager.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Class<?> settingsType = constructor.getParameterTypes()[1];
        Method from = settingsType.getDeclaredMethod("from", org.springframework.core.env.Environment.class);
        from.setAccessible(true);
        Object settings = from.invoke(null, environment);
        return (NacosConfigManager) constructor.newInstance(environment, settings);
    }

    private boolean apply(NacosConfigManager manager, String yaml) {
        try {
            Class<?> originType = Class.forName("org.namewta.common.nacos.NacosUpdateOrigin");
            Object listener = java.util.Arrays.stream(originType.getEnumConstants())
                .filter(value -> "LISTENER".equals(value.toString()))
                .findFirst()
                .orElseThrow();
            Boolean result = ReflectionTestUtils.invokeMethod(manager, "apply", yaml, listener);
            return Boolean.TRUE.equals(result);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class MutableAccessor implements NacosConfigAccessor {

        private final AtomicReference<Map<String, Object>> values = new AtomicReference<>(Map.of());

        @Override
        public <T> Optional<T> configuration(String participantId, Class<T> type) {
            Object value = values.get().get(participantId);
            return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
        }

        void replace(Map<String, Object> next) {
            values.set(Map.copyOf(next));
        }

        void clear() {
            values.set(Map.of());
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(OssLifecycleProperties.class)
    static class OssPropertiesConfiguration {
    }
}
