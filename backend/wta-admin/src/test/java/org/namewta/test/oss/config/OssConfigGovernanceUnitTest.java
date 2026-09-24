package org.namewta.test.oss.config;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.annotation.DsTxEventListener;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.tx.DsTxEventListenerFactory;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.oss.constant.OssConstant;
import org.namewta.common.redis.utils.CacheUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.system.domain.SysOssConfig;
import org.namewta.system.domain.bo.SysOssConfigBo;
import org.namewta.system.event.OssConfigChangeEvent;
import org.namewta.system.mapper.SysOssConfigMapper;
import org.namewta.system.service.impl.SysOssConfigServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.aop.framework.ProxyFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
public class OssConfigGovernanceUnitTest {

    @TempDir
    Path temporary;

    @Test
    void rejectsUnknownPolicyAndPublicDefaultBeforeWriting() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);

        SysOssConfigBo unknown = bo(null, "archive", "bucket-a", "1", "N");
        assertThatThrownBy(() -> service.insertByBo(unknown))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("0=PRIVATE或2=PUBLIC_READ");

        SysOssConfigBo publicDefault = bo(null, "portal", "bucket-public", "2", "Y");
        assertThatThrownBy(() -> service.insertByBo(publicDefault))
            .isInstanceOf(ServiceException.class)
            .hasMessage("默认OSS配置必须为PRIVATE");

        verify(mapper, never()).insert(any(SysOssConfig.class));
    }

    @Test
    void referencedConfigCannotChangeOwnershipBoundary() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);
        SysOssConfig old = config(11L, "private-main", "bucket-private", "0", "N");
        when(mapper.selectByIdForUpdate(11L)).thenReturn(old);
        when(mapper.countConfigKeyConflicts("private-main", 11L)).thenReturn(0L);
        when(mapper.countDefaultConfigs()).thenReturn(1L);
        when(mapper.countOssReferences("private-main")).thenReturn(3L);

        SysOssConfigBo edit = bo(11L, "private-main", "bucket-public", "0", "N");
        assertThatThrownBy(() -> service.updateByBo(edit))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不能修改物理存储身份");

        verify(mapper, never()).updateById(any(SysOssConfig.class));
    }

    @Test
    void referencedConfigFreezesEndpointButAllowsCredentialRotation() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);
        SysOssConfig old = config(11L, "private-main", "bucket-private", "0", "N");
        old.setSecretKey("existing-secret");
        old.setEndpoint("old.example.test");
        SysOssConfig persisted = config(11L, "private-main", "bucket-private", "0", "N");
        persisted.setSecretKey("existing-secret");
        persisted.setEndpoint("old.example.test");
        when(mapper.selectByIdForUpdate(11L)).thenReturn(old);
        when(mapper.countConfigKeyConflicts("private-main", 11L)).thenReturn(0L);
        when(mapper.countDefaultConfigs()).thenReturn(1L);
        when(mapper.countOssReferences("private-main")).thenReturn(7L);
        when(mapper.updateById(any(SysOssConfig.class))).thenReturn(1);
        when(mapper.selectById(11L)).thenReturn(persisted);
        SysOssConfigBo edit = bo(11L, "private-main", "bucket-private", "0", "N");
        edit.setSecretKey(null);
        edit.setEndpoint("new.example.test");
        assertThatThrownBy(() -> service.updateByBo(edit))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不能修改物理存储身份");
        verify(mapper, never()).updateById(any(SysOssConfig.class));

        edit.setEndpoint("old.example.test");
        edit.setAccessKey("rotated-access-key");
        List<Object> events = new ArrayList<>();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(JsonMapper.class, () -> JsonMapper.builder().build());
            context.registerBean(SpringUtils.class);
            context.refresh();
            context.addApplicationListener(events::add);
            assertThat(service.updateByBo(edit)).isTrue();
        }

        ArgumentCaptor<SysOssConfig> saved = ArgumentCaptor.forClass(SysOssConfig.class);
        verify(mapper).updateById(saved.capture());
        assertThat(saved.getValue().getSecretKey()).isEqualTo("existing-secret");
        assertThat(saved.getValue().getEndpoint()).isEqualTo("old.example.test");
        assertThat(saved.getValue().getAccessKey()).isEqualTo("rotated-access-key");
        assertThat(saved.getValue().getBucketName()).isEqualTo("bucket-private");
        assertThat(events).anySatisfy(event -> {
            assertThat(event).isInstanceOf(PayloadApplicationEvent.class);
            assertThat(((PayloadApplicationEvent<?>) event).getPayload()).isInstanceOf(OssConfigChangeEvent.class);
        });
    }

    @Test
    void referencedOrDefaultConfigCannotBeDeletedAndPublicCannotBecomeDefault() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);
        SysOssConfig referenced = config(12L, "private-docs", "bucket-docs", "0", "N");
        when(mapper.selectByIdForUpdate(12L)).thenReturn(referenced);
        when(mapper.countDefaultConfigs()).thenReturn(1L);
        when(mapper.countOssReferences("private-docs")).thenReturn(1L);
        assertThatThrownBy(() -> service.deleteWithValidByIds(List.of(12L), true))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已被对象引用");

        SysOssConfig defaultConfig = config(13L, "private-default", "bucket-default", "0", "Y");
        when(mapper.selectByIdForUpdate(13L)).thenReturn(defaultConfig);
        assertThatThrownBy(() -> service.deleteWithValidByIds(List.of(13L), true))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("默认OSS配置不可删除");

        SysOssConfig publicConfig = config(14L, "public-portal", "bucket-public", "2", "N");
        when(mapper.selectByIdForUpdate(14L)).thenReturn(publicConfig);
        SysOssConfigBo switchRequest = new SysOssConfigBo();
        switchRequest.setOssConfigId(14L);
        assertThatThrownBy(() -> service.updateOssConfigStatus(switchRequest))
            .isInstanceOf(ServiceException.class)
            .hasMessage("默认OSS配置必须为PRIVATE");

        verify(mapper, never()).deleteByIds(any());
    }

    @Test
    void initializationUsesAuthoritativeDbAndFailsClosedForMissingOrBrokenDefault() throws Exception {
        // RedisUtils.CLIENT 与 CacheUtils.CacheManagerHolder 缓存静态 Bean；独立 JVM 防止污染 Surefire 其他测试。
        Path result = temporary.resolve("oss-bootstrap.result");
        Path log = temporary.resolve("oss-bootstrap.log");
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        assertThat(classpath).isNotBlank();
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-Xms32m", "-Xmx384m", "-cp", classpath, OssConfigGovernanceUnitTest.class.getName(),
            result.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertThat(child.waitFor(45, TimeUnit.SECONDS)).as("OSS bootstrap isolated JVM completed").isTrue();
            String outcome = Files.isRegularFile(result) ? Files.readString(result).trim() : "NO_RESULT";
            assertThat(outcome).as("OSS bootstrap isolated scenario").isEqualTo("OK");
            assertThat(child.exitValue()).as("OSS bootstrap isolated JVM exit").isZero();
        } finally {
            List<ProcessHandle> descendants = child.toHandle().descendants().toList();
            if (child.isAlive()) {
                child.destroyForcibly();
            }
            descendants.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
            boolean parentStopped = child.waitFor(5, TimeUnit.SECONDS);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (descendants.stream().anyMatch(ProcessHandle::isAlive) && System.nanoTime() < deadline) {
                Thread.sleep(25);
            }
            Files.deleteIfExists(result);
            Files.deleteIfExists(log);
            assertThat(parentStopped && descendants.stream().noneMatch(ProcessHandle::isAlive))
                .as("OSS bootstrap isolated process tree stopped").isTrue();
        }
    }

    /** 子进程只执行固定合成场景，不初始化父 Surefire JVM 的静态缓存客户端。 */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("unlisted OSS bootstrap case");
        }
        String outcome = "OK";
        AtomicReference<String> scenario = new AtomicReference<>("SETUP");
        try {
            bootstrapScenarios(scenario);
        } catch (Throwable failure) {
            outcome = "FAIL:" + scenario.get() + ":" + failure.getClass().getSimpleName();
        }
        Files.writeString(Path.of(args[0]), outcome);
        if (!"OK".equals(outcome)) {
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    private static void bootstrapScenarios(AtomicReference<String> scenario) {
        RedissonClient redis = mock(RedissonClient.class);
        RBucket<String> defaultBucket = mock(RBucket.class);
        AtomicReference<String> defaultPointer = new AtomicReference<>();
        doReturn(defaultBucket).when(redis).getBucket(OssConstant.DEFAULT_CONFIG_KEY);
        doAnswer(call -> {
            defaultPointer.set(call.getArgument(0));
            return null;
        }).when(defaultBucket).set(any());
        when(defaultBucket.get()).thenAnswer(call -> defaultPointer.get());
        when(defaultBucket.delete()).thenAnswer(call -> defaultPointer.getAndSet(null) != null);
        CacheManager cacheManager = new ConcurrentMapCacheManager(CacheNames.SYS_OSS_CONFIG);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(SpringUtils.class);
            context.registerBean(RedissonClient.class, () -> redis);
            context.registerBean(CacheManager.class, () -> cacheManager);
            context.registerBean(JsonMapper.class, () -> JsonMapper.builder().build());
            context.refresh();

            scenario.set("DB_READ_FAILURE");
            SysOssConfigMapper failedRead = mock(SysOssConfigMapper.class);
            IllegalStateException dbFailure = new IllegalStateException("synthetic DB failure");
            when(failedRead.selectList()).thenThrow(dbFailure);
            seedOldCacheAndDefault();
            assertThatThrownBy(() -> new SysOssConfigServiceImpl(failedRead).init()).isSameAs(dbFailure);
            assertThat(CacheUtils.<String>get(CacheNames.SYS_OSS_CONFIG, "old-key"))
                .isEqualTo("old-cache-value");
            assertThat(defaultPointer.get()).isEqualTo("old-key");

            scenario.set("EMPTY_CONFIGS");
            seedOldCacheAndDefault();
            bootstrap(List.of());
            assertThat(CacheUtils.<String>get(CacheNames.SYS_OSS_CONFIG, "old-key")).isNull();
            assertThat(defaultPointer.get()).isNull();

            scenario.set("DUPLICATE_DEFAULTS");
            seedOldCacheAndDefault();
            SysOssConfig optional = config(3L, "public-c", "bucket-c", "2", "N");
            bootstrap(List.of(config(1L, "private-a", "bucket-a", "0", "Y"),
                config(2L, "private-b", "bucket-b", "0", "Y"), optional));
            assertThat(CacheUtils.<String>get(CacheNames.SYS_OSS_CONFIG, "old-key")).isNull();
            assertThat(defaultPointer.get()).isNull();
            assertThat(CacheUtils.<String>get(CacheNames.SYS_OSS_CONFIG, "public-c")).isNotNull();

            scenario.set("BROKEN_DEFAULT");
            seedOldCacheAndDefault();
            SysOssConfig broken = config(4L, "private-bad", "bucket-bad", "0", "Y");
            broken.setEndpoint(null);
            bootstrap(List.of(broken, optional));
            assertThat(CacheUtils.<String>get(CacheNames.SYS_OSS_CONFIG, "old-key")).isNull();
            assertThat(defaultPointer.get()).isNull();
            assertThat(CacheUtils.<String>get(CacheNames.SYS_OSS_CONFIG, "private-bad")).isNull();
            assertThat(CacheUtils.<String>get(CacheNames.SYS_OSS_CONFIG, "public-c")).isNotNull();

            scenario.set("VALID_PRIVATE_DEFAULT");
            seedOldCacheAndDefault();
            bootstrap(List.of(config(5L, "private-good", "bucket-good", "0", "Y"), optional));
            assertThat(CacheUtils.<String>get(CacheNames.SYS_OSS_CONFIG, "old-key")).isNull();
            assertThat(defaultPointer.get()).isEqualTo("private-good");
            assertThat(CacheUtils.<String>get(CacheNames.SYS_OSS_CONFIG, "public-c")).isNotNull();
        }
    }

    private static void seedOldCacheAndDefault() {
        CacheUtils.put(CacheNames.SYS_OSS_CONFIG, "old-key", "old-cache-value");
        RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, "old-key");
    }

    private static void bootstrap(List<SysOssConfig> configs) {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        when(mapper.selectList()).thenReturn(configs);
        new SysOssConfigServiceImpl(mapper).init();
    }

    @Test
    void updateFailureAfterClearingDefaultsMustAbortTheTransaction() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);
        SysOssConfig old = config(21L, "private-secondary", "bucket-secondary", "0", "N");
        when(mapper.selectByIdForUpdate(21L)).thenReturn(old);
        when(mapper.countConfigKeyConflicts("private-secondary", 21L)).thenReturn(0L);
        when(mapper.countOssReferences("private-secondary")).thenReturn(0L);
        when(mapper.clearOtherDefaultStatuses(21L)).thenReturn(1);
        when(mapper.updateById(any(SysOssConfig.class))).thenReturn(0);

        SysOssConfigBo edit = bo(21L, "private-secondary", "bucket-secondary", "0", "Y");
        assertThatThrownBy(() -> service.updateByBo(edit))
            .isInstanceOf(ServiceException.class)
            .hasMessage("OSS配置更新失败");

        verify(mapper).clearOtherDefaultStatuses(21L);
        verify(mapper).updateById(any(SysOssConfig.class));
        verify(mapper, never()).selectById(21L);
    }

    @Test
    void defaultSwitchFailureAfterClearingDefaultsMustAbortTheTransaction() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);
        SysOssConfig target = config(22L, "private-secondary", "bucket-secondary", "0", "N");
        when(mapper.selectByIdForUpdate(22L)).thenReturn(target);
        when(mapper.clearOtherDefaultStatuses(22L)).thenReturn(1);
        when(mapper.updateById(any(SysOssConfig.class))).thenReturn(0);

        SysOssConfigBo switchRequest = new SysOssConfigBo();
        switchRequest.setOssConfigId(22L);
        assertThatThrownBy(() -> service.updateOssConfigStatus(switchRequest))
            .isInstanceOf(ServiceException.class)
            .hasMessage("默认OSS配置切换失败");

        verify(mapper).clearOtherDefaultStatuses(22L);
        verify(mapper).updateById(target);
    }

    @Test
    void everyConfigMutationUsesDynamicDatasourceTransaction() throws Exception {
        assertThat(SysOssConfigServiceImpl.class.getMethod("insertByBo", SysOssConfigBo.class)
            .getAnnotation(DSTransactional.class)).isNotNull();
        assertThat(SysOssConfigServiceImpl.class.getMethod("updateByBo", SysOssConfigBo.class)
            .getAnnotation(DSTransactional.class)).isNotNull();
        assertThat(SysOssConfigServiceImpl.class.getMethod("deleteWithValidByIds", java.util.Collection.class,
            Boolean.class).getAnnotation(DSTransactional.class)).isNotNull();
        assertThat(SysOssConfigServiceImpl.class.getMethod("updateOssConfigStatus", SysOssConfigBo.class)
            .getAnnotation(DSTransactional.class)).isNotNull();
    }

    @Test
    void dynamicTransactionEventsRunOnlyAfterCommitAndNeverAfterRollback() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DsTxEventListenerFactory.class);
            context.registerBean(TxEventProbe.class);
            context.refresh();
            TxEventProbe probe = context.getBean(TxEventProbe.class);
            ProxyFactory proxyFactory = new ProxyFactory(new TxEventPublisher(context, probe));
            proxyFactory.setProxyTargetClass(true);
            proxyFactory.addAdvice(new DynamicLocalTransactionInterceptor(true));
            TxEventPublisher publisher = (TxEventPublisher) proxyFactory.getProxy();

            assertThat(publisher.publish("committed", false)).isZero();
            assertThat(probe.events).containsExactly("committed");
            assertThatThrownBy(() -> publisher.publish("rolled-back", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("forced rollback");
            assertThat(probe.events).containsExactly("committed");
        }
    }

    private static SysOssConfigBo bo(Long id, String key, String bucket, String policy, String status) {
        SysOssConfigBo bo = new SysOssConfigBo();
        bo.setOssConfigId(id);
        bo.setConfigKey(key);
        bo.setAccessKey("access-key");
        bo.setSecretKey("secret-key");
        bo.setBucketName(bucket);
        bo.setEndpoint("endpoint.example.test");
        bo.setAccessPolicy(policy);
        bo.setStatus(status);
        bo.setIsHttps("Y");
        return bo;
    }

    private static SysOssConfig config(Long id, String key, String bucket, String policy, String status) {
        SysOssConfig config = new SysOssConfig();
        config.setOssConfigId(id);
        config.setConfigKey(key);
        config.setAccessKey("access-key");
        config.setSecretKey("secret-key");
        config.setBucketName(bucket);
        config.setEndpoint("endpoint.example.test");
        config.setAccessPolicy(policy);
        config.setStatus(status);
        config.setIsHttps("Y");
        return config;
    }

    static class TxEventPublisher {
        private final AnnotationConfigApplicationContext context;
        private final TxEventProbe probe;

        TxEventPublisher(AnnotationConfigApplicationContext context, TxEventProbe probe) {
            this.context = context;
            this.probe = probe;
        }

        @DSTransactional
        public int publish(String event, boolean fail) {
            context.publishEvent(event);
            int observedInsideTransaction = probe.events.size();
            if (fail) {
                throw new IllegalStateException("forced rollback");
            }
            return observedInsideTransaction;
        }
    }

    static class TxEventProbe {
        private final List<String> events = new ArrayList<>();

        @DsTxEventListener
        public void capture(String event) {
            events.add(event);
        }
    }
}
