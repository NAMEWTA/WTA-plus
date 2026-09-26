package org.namewta.test.oss.lifecycle;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.system.domain.SysOss;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.config.OssLifecycleProperties;
import org.namewta.system.oss.exception.OssLifecycleException;
import org.namewta.system.oss.mapper.SysOssRefMapper;
import org.namewta.system.oss.provider.OssObjectStore;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.namewta.system.oss.service.OssCleanupAtomicService;
import org.namewta.system.oss.service.OssLifecycleManager;
import org.namewta.test.support.SqlBaselineScripts;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 隔离 MySQL 两连接验证实际代理提交边界；外部删除由可控替身固定回执丢失及并发顺序。 */
@Tag("dev")
class OssCleanupMySqlIntegrationTest {
    @Test
    void committedReservationSurvivesUnknownOutcomesAndFencesConcurrentRestore() throws Exception {
        String url = System.getProperty("oss.lifecycle.mysql.integration.url");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "需要一次性隔离 MySQL JDBC URL");
        PooledDataSource database = new PooledDataSource("com.mysql.cj.jdbc.Driver", url,
            System.getProperty("oss.lifecycle.mysql.integration.username", "root"),
            System.getProperty("oss.lifecycle.mysql.integration.password", ""));
        AtomicReference<String> commitFailure = new AtomicReference<>();
        DynamicRoutingDataSource routing = new DynamicRoutingDataSource(List.of());
        routing.setPrimary("master");
        routing.setStrict(true);
        routing.addDataSource("master", new DelegatingDataSource(database) {
            @Override public Connection getConnection() throws SQLException {
                Connection actual = database.getConnection();
                return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, arguments) -> {
                        if ("commit".equals(method.getName())) {
                            String fault = commitFailure.getAndSet(null);
                            if (fault != null) {
                                if ("after".equals(fault)) actual.commit(); else actual.rollback();
                                throw new SQLRecoverableException("owned cleanup commit failure");
                            }
                        }
                        try { return method.invoke(actual, arguments); }
                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
            }
        });
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            for (String table : List.of("sys_oss_ref", "sys_oss")) execute(database, "drop table if exists " + table);
            for (String table : List.of("sys_oss", "sys_oss_ref")) execute(database, SqlBaselineScripts.createTable(table));
            for (long id = 101; id <= 106; id++) {
                execute(database, "insert into sys_oss(oss_id,file_name,original_name,url,service,is_temp,expire_time,delete_state) "
                    + "values (" + id + ",'cleanup/" + id + "','owned.txt','','owned','Y',date_sub(now(), interval 1 hour),'PENDING')");
            }
            MybatisConfiguration configuration = new MybatisConfiguration(new Environment("owned-cleanup",
                new SpringManagedTransactionFactory(), routing));
            configuration.setMapUnderscoreToCamelCase(true);
            GlobalConfig global = GlobalConfigUtils.defaults();
            global.setBanner(false);
            GlobalConfigUtils.setGlobalConfig(configuration, global);
            for (String resource : List.of("mapper/system/SysOssMapper.xml", "mapper/system/SysOssRefMapper.xml")) {
                try (InputStream input = Resources.getResourceAsStream(resource)) {
                    new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
                }
            }
            SqlSessionTemplate sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
            SysOssMapper objects = sessions.getMapper(SysOssMapper.class);
            SysOssRefMapper references = sessions.getMapper(SysOssRefMapper.class);
            OssObjectStore provider = mock(OssObjectStore.class);
            Set<Long> physical = ConcurrentHashMap.newKeySet();
            physical.addAll(List.of(101L, 102L, 103L, 104L, 105L, 106L));
            when(provider.exists(any(), eq(Duration.ofSeconds(5)))).thenAnswer(call ->
                physical.contains(((SysOss) call.getArgument(0)).getOssId()));
            doAnswer(call -> {
                SysOss object = call.getArgument(0);
                assertThat(TransactionContext.getXID()).isNull();
                // 独立连接已可看到预约，且供应商调用期间没有持有对象行锁。
                assertThat(scalar(database, "select delete_state from sys_oss where oss_id=" + object.getOssId()))
                    .isEqualTo("DELETING");
                execute(database, "select oss_id from sys_oss where oss_id=" + object.getOssId() + " for update nowait");
                if (object.getOssId() == 104L) {
                    entered.countDown();
                    assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                }
                physical.remove(object.getOssId());
                if (object.getOssId() == 101L) throw new IllegalStateException("owned DELETE acknowledgement lost");
                if (object.getOssId() == 103L) commitFailure.set("before");
                return null;
            }).when(provider).delete(any(), eq(Duration.ofSeconds(5)));
            OssLifecycleManager manager = transactional(new OssLifecycleManager(objects, references, provider,
                new OssLifecycleProperties(), mock(OssStorageReadinessRegistry.class),
                transactional(new OssCleanupAtomicService(objects, references))));
            // Fixture timestamps come from MySQL; do not mix the host JVM timezone into this test.
            LocalDateTime now = LocalDateTime.parse(scalar(database,
                "select date_format(now(), '%Y-%m-%dT%H:%i:%s')"));

            assertThatThrownBy(() -> manager.cleanupExpired(101L, now, false)).isInstanceOf(RuntimeException.class);
            assertThat(physical).doesNotContain(101L);
            assertFenced(manager, 101L);
            assertThat(manager.cleanupExpired(101L, now, false)).isTrue();
            assertThat(objects.selectById(101L)).isNull();

            // 数据库预约实际已提交但回执丢失：调用者不能开始 DELETE，也不能恢复。
            commitFailure.set("after");
            assertThatThrownBy(() -> manager.cleanupExpired(102L, now, false)).isInstanceOf(RuntimeException.class);
            assertThat(objects.selectById(102L).getDeleteState()).isEqualTo("DELETING");
            assertFenced(manager, 102L);
            assertThatThrownBy(() -> manager.cleanupExpired(102L, now, false)).isInstanceOf(OssLifecycleException.class);
            verify(provider, never()).delete(argThat(object -> object.getOssId() == 102L), any(Duration.class));
            physical.remove(102L); // 人工核实并处理后，HEAD 才能确认缺失。
            assertThat(manager.cleanupExpired(102L, now, false)).isTrue();

            // 供应商已删除，完成事务失败必须回滚元数据删除并保留预约。
            assertThatThrownBy(() -> manager.cleanupExpired(103L, now, false)).isInstanceOf(RuntimeException.class);
            assertThat(objects.selectById(103L).getDeleteState()).isEqualTo("DELETING");
            assertThat(manager.cleanupExpired(103L, now, false)).isTrue();

            try (var executor = Executors.newSingleThreadExecutor()) {
                var deleting = executor.submit(() -> manager.cleanupExpired(104L, now, false));
                try {
                    assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                    assertFenced(manager, 104L);
                    assertThat(manager.cleanupExpired(105L, now, false)).isTrue();
                } finally { release.countDown(); }
                assertThat(deleting.get(5, TimeUnit.SECONDS)).isTrue();
            }
            assertThat(manager.restoreObjects(List.of(106L))).isTrue();
            assertThat(manager.cleanupExpired(106L, now, false)).isFalse();
            for (long id : List.of(101L, 103L, 104L, 105L)) {
                verify(provider, times(1)).delete(argThat(object -> object.getOssId() == id), any(Duration.class));
            }
            verify(provider, never()).delete(argThat(object -> object.getOssId() == 106L), any(Duration.class));
        } finally {
            release.countDown();
            try {
                for (String table : List.of("sys_oss_ref", "sys_oss")) execute(database, "drop table if exists " + table);
            } finally { database.forceCloseAll(); }
        }
    }

    private void assertFenced(OssLifecycleManager manager, long id) {
        assertThatThrownBy(() -> manager.bind(id, "owned_record", "1")).isInstanceOf(OssLifecycleException.class);
        assertThatThrownBy(() -> manager.unbind(id, "owned_record", "1")).isInstanceOf(OssLifecycleException.class);
        assertThatThrownBy(() -> manager.restoreObjects(List.of(id))).isInstanceOf(OssLifecycleException.class);
        assertThatThrownBy(() -> manager.deleteObjects(List.of(id))).isInstanceOf(OssLifecycleException.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T transactional(T target) {
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(
            new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return (T) proxy.getProxy();
    }

    private void execute(PooledDataSource database, String sql) throws SQLException {
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String scalar(PooledDataSource database, String sql) throws SQLException {
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement();
             var rows = statement.executeQuery(sql)) {
            assertThat(rows.next()).isTrue();
            return rows.getString(1);
        }
    }
}
