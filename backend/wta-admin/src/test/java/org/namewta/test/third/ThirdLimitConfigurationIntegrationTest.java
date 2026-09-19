package org.namewta.test.third;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.mapping.Environment;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.redis.cache.ClusterCacheInvalidationCoordinator;
import org.namewta.common.redis.cache.RedissonCacheInvalidationTransport;
import org.namewta.third.adapter.resilience.ThirdResiliencePolicyAdapter;
import org.namewta.third.adapter.store.ThirdConfigCacheAdapter;
import org.namewta.third.dao.ThirdEndpointDao;
import org.namewta.third.dao.ThirdProviderDao;
import org.namewta.third.domain.bo.ThirdEndpointBo;
import org.namewta.third.domain.bo.ThirdProviderBo;
import org.namewta.third.mapper.ThirdEndpointMapper;
import org.namewta.third.mapper.ThirdProviderMapper;
import org.namewta.third.service.ThirdEndpointService;
import org.namewta.third.service.ThirdProviderService;
import org.namewta.third.spi.ThirdProviderAdapterRegistry;
import org.namewta.third.support.ThirdRejectedException;
import org.namewta.third.usecase.impl.ThirdEndpointUseCaseImpl;
import org.namewta.third.usecase.impl.ThirdProviderUseCaseImpl;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real configuration save -> dynamic transaction commit -> two cache subscribers -> stable Redis quotas. */
@Tag("dev")
@EnabledIfSystemProperty(named = "third.configuration.integration", matches = "true")
class ThirdLimitConfigurationIntegrationTest {
    @Test
    void committedSavesRefreshBothNodesRollbackDoesNotAndFailedRefreshIsRecoverable() throws Exception {
        String url = System.getProperty("third.mysql.integration.url");
        assertThat(url).startsWith("jdbc:mysql://127.0.0.1:").contains("namewta_third_test_");
        String code = "t24-" + UUID.randomUUID().toString().replace("-", "");
        Object previousFactory = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory");
        Object previousContext = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext");
        HikariDataSource pool = new HikariDataSource(); pool.setJdbcUrl(url); pool.setUsername("root");
        pool.setPassword("owned-third-test-only"); pool.setMaximumPoolSize(4);
        DynamicRoutingDataSource routing = new DynamicRoutingDataSource(List.of()); routing.setPrimary("master");
        routing.addDataSource("master", pool);
        RedissonClient redis = client(); RedissonClient peerRedis = client();
        JdbcTemplate db = new JdbcTemplate(routing);
        try (var context = new AnnotationConfigApplicationContext();
             var coordinator = coordinator(redis, "a");
             var peerCoordinator = coordinator(peerRedis, "b")) {
            context.registerBean(RedissonClient.class, () -> redis, bd -> bd.setDestroyMethodName(""));
            context.registerBean(SpringUtils.class); context.refresh();
            var config = new MybatisConfiguration(new Environment("owned-t24", new SpringManagedTransactionFactory(), routing));
            config.setMapUnderscoreToCamelCase(true);
            var interceptor = new MybatisPlusInterceptor(); interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            config.addInterceptor(interceptor); config.addMapper(ThirdProviderMapper.class); config.addMapper(ThirdEndpointMapper.class);
            var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
            var providerDao = new ThirdProviderDao(sessions.getMapper(ThirdProviderMapper.class), sessions.getMapper(ThirdEndpointMapper.class));
            var endpointDao = new ThirdEndpointDao(sessions.getMapper(ThirdEndpointMapper.class));
            var policy = spy(new ThirdResiliencePolicyAdapter(redis));
            var peerPolicy = new ThirdResiliencePolicyAdapter(peerRedis);
            var cache = new ThirdConfigCacheAdapter(providerDao, endpointDao, coordinator, policy);
            var peerCache = new ThirdConfigCacheAdapter(providerDao, endpointDao, peerCoordinator, peerPolicy);
            ReflectionTestUtils.invokeMethod(cache, "registerInvalidationHandler");
            ReflectionTestUtils.invokeMethod(peerCache, "registerInvalidationHandler");
            db.update("insert into third_provider(provider_id,provider_code,provider_name,base_url,concurrency_limit) values(924001,?,'Owned','https://example.test',2)", code);
            db.update("insert into third_endpoint(endpoint_id,provider_id,provider_code,endpoint_code,endpoint_name,http_method,relative_path) values(924002,924001,?,'get','Owned','GET','/get')", code);
            var service = new ThirdProviderService(providerDao, cache);
            var application = transactional(new ThirdProviderUseCaseImpl(service));
            var boundary = transactional(new Boundary(application));
            var endpointApplication = transactional(new ThirdEndpointUseCaseImpl(new ThirdEndpointService(endpointDao,
                providerDao, cache, new ThirdProviderAdapterRegistry(List.of()))));
            var old = cache.get(code, "get"); assertThat(peerCache.get(code, "get").getProvider().getVersion()).isZero();
            var first = policy.acquire(old.getProvider(), old.getEndpoint());
            var second = peerPolicy.acquire(old.getProvider(), old.getEndpoint());
            String key = "third:limit-config:provider:" + code;
            String original = redis.<String>getBucket(key, StringCodec.INSTANCE).get();
            ThirdProviderBo update = provider(code, 1);
            boundary.save(update, () -> assertThat(redis.<String>getBucket(key, StringCodec.INSTANCE).get()).isEqualTo(original), false);
            assertThat(db.queryForObject("select version from third_provider where provider_id=924001", Integer.class)).isEqualTo(1);
            String committed = redis.<String>getBucket(key, StringCodec.INSTANCE).get();
            assertThat(committed).startsWith("1:0:1:").endsWith(":ready");
            var latest = peerCache.get(code, "get"); assertThat(latest.getProvider().getVersion()).isEqualTo(1);
            assertThatThrownBy(() -> peerPolicy.acquire(old.getProvider(), old.getEndpoint())).isInstanceOf(ThirdRejectedException.class);
            first.close();
            assertThatThrownBy(() -> peerPolicy.acquire(latest.getProvider(), latest.getEndpoint())).isInstanceOf(ThirdRejectedException.class);
            second.close(); peerPolicy.acquire(latest.getProvider(), latest.getEndpoint()).close();

            assertThatThrownBy(() -> boundary.save(provider(code, 4), () -> {}, true)).hasMessage("owned rollback");
            assertThat(redis.<String>getBucket(key, StringCodec.INSTANCE).get()).isEqualTo(committed);
            assertThat(providerDao.findActiveByCode(code).getVersion()).isEqualTo(1);

            ThirdEndpointBo endpoint = endpoint(code);
            endpoint.setEndpointCode("renamed");
            assertThatThrownBy(() -> endpointApplication.save(endpoint)).hasMessage("Endpoint code cannot be changed");
            endpoint.setEndpointCode("get"); endpoint.setConcurrencyLimit(1); endpointApplication.save(endpoint);
            assertThat(endpointDao.findActiveById(924002L).getVersion()).isEqualTo(1);
            assertThat(peerRedis.<String>getBucket("third:limit-config:endpoint:" + code + ":get", StringCodec.INSTANCE).get())
                .startsWith("1:0:1:").endsWith(":ready");

            // Redis failure after commit does not undo MySQL; cache eviction allows the next request to repair it.
            doThrow(new IllegalStateException("owned redis refresh failure")).doCallRealMethod().when(policy).refresh(any(), any());
            assertThatThrownBy(() -> application.save(provider(code, 3))).isInstanceOf(RuntimeException.class);
            assertThat(providerDao.findActiveByCode(code).getVersion()).isEqualTo(2);
            var repaired = peerCache.get(code, "get");
            peerPolicy.acquire(repaired.getProvider(), repaired.getEndpoint()).close();
            assertThat(peerRedis.<String>getBucket(key, StringCodec.INSTANCE).get()).startsWith("2:0:3:").endsWith(":ready");

            var one = providerDao.findActiveByCode(code); var stale = providerDao.findActiveByCode(code);
            one.setConcurrencyLimit(4); stale.setConcurrencyLimit(9);
            assertThat(providerDao.update(one)).isEqualTo(1); assertThat(providerDao.update(stale)).isZero();
            assertThat(providerDao.findActiveByCode(code).getConcurrencyLimit()).isEqualTo(4);
            var endpointOne = endpointDao.findActiveById(924002L); var endpointStale = endpointDao.findActiveById(924002L);
            endpointOne.setRateLimit(1); endpointStale.setRateLimit(2);
            assertThat(endpointDao.update(endpointOne)).isEqualTo(1); assertThat(endpointDao.update(endpointStale)).isZero();
            ReflectionTestUtils.invokeMethod(cache, "unregisterInvalidationHandler");
            ReflectionTestUtils.invokeMethod(peerCache, "unregisterInvalidationHandler");
        } finally {
            try { db.update("delete from third_endpoint where endpoint_id=924002"); db.update("delete from third_provider where provider_id=924001"); }
            finally {
                routing.destroy(); redis.shutdown(); peerRedis.shutdown();
                ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory", previousFactory);
                ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext", previousContext);
            }
        }
    }

    private static ThirdProviderBo provider(String code, int concurrency) {
        var bo = new ThirdProviderBo(); bo.setProviderId(924001L); bo.setProviderCode(code); bo.setProviderName("Owned");
        bo.setBaseUrl("https://example.test"); bo.setConcurrencyLimit(concurrency); return bo;
    }

    private static ThirdEndpointBo endpoint(String code) {
        var bo = new ThirdEndpointBo(); bo.setEndpointId(924002L); bo.setProviderId(924001L); bo.setProviderCode(code);
        bo.setEndpointCode("get"); bo.setEndpointName("Owned"); bo.setHttpMethod("GET"); bo.setRelativePath("/get"); return bo;
    }

    private static RedissonClient client() {
        var config = new Config(); config.setCodec(StringCodec.INSTANCE); config.setThreads(2); config.setNettyThreads(2);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + System.getProperty("third.redis.integration.port"))
            .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
        return Redisson.create(config);
    }

    private static ClusterCacheInvalidationCoordinator coordinator(RedissonClient client, String node) {
        return new ClusterCacheInvalidationCoordinator(new RedissonCacheInvalidationTransport(client,
            JsonMapper.builder().build()), Duration.ofSeconds(1), node);
    }

    @SuppressWarnings("unchecked")
    private static <T> T transactional(T target) {
        ProxyFactory proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return (T) proxy.getProxy();
    }

    public static class Boundary {
        private final ThirdProviderUseCaseImpl application;
        public Boundary(ThirdProviderUseCaseImpl application) { this.application = application; }
        @DSTransactional
        public void save(ThirdProviderBo bo, Runnable during, boolean fail) {
            application.save(bo); during.run();
            if (fail) throw new IllegalStateException("owned rollback");
        }
    }
}
