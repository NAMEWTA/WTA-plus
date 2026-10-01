package org.namewta.test.auth.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.tx.DsTxEventListenerFactory;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import io.github.linpeilie.Converter;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.handler.InjectionMetaObjectHandler;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.system.auth.ExternalAuthConfigurationCache;
import org.namewta.system.controller.admin.SysAuthProviderController;
import org.namewta.system.controller.admin.SysAuthRegistrationController;
import org.namewta.system.domain.bo.*;
import org.namewta.system.listener.ExternalAuthConfigChangeListener;
import org.namewta.system.mapper.*;
import org.namewta.system.service.impl.ExternalAuthConfigurationServiceImpl;
import org.namewta.system.service.impl.SysExternalAuthConfigServiceImpl;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Base64;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/** Only a task-owned MySQL/Redis pair is accepted. Run separately from other static RedisUtils fixtures. */
@Tag("dev")
@EnabledIfSystemProperty(named = "external.auth.mysql.integration", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExternalAuthConfigurationMySqlIntegrationTest {
    private DynamicRoutingDataSource routing;
    private AnnotationConfigApplicationContext context;
    private RedissonClient redis;
    private JdbcTemplate db;
    private SysExternalAuthConfigServiceImpl management;
    private ExternalAuthConfigurationServiceImpl runtime;
    private SysAuthProviderController providerController;
    private SysAuthRegistrationController registrationController;
    private Object previousFactory, previousContext;
    private org.mockito.MockedStatic<org.namewta.common.satoken.utils.LoginHelper> loginContext;

    @BeforeAll
    void open() throws Exception {
        String url = System.getProperty("external.auth.mysql.url");
        assertThat(url).startsWith("jdbc:mysql://127.0.0.1:45406/namewta_external_auth_test");
        previousFactory = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory");
        previousContext = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext");
        var pool = new HikariDataSource(); pool.setJdbcUrl(url); pool.setUsername("root");
        pool.setPassword("owned-external-auth-test-only"); pool.setMaximumPoolSize(4);
        routing = new DynamicRoutingDataSource(List.of()); routing.setPrimary("master"); routing.addDataSource("master", pool);
        db = new JdbcTemplate(routing);
        var redisConfig = new Config(); redisConfig.setThreads(2).setNettyThreads(2);
        redisConfig.useSingleServer().setAddress("redis://127.0.0.1:45479").setDatabase(9)
            .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
        redis = Redisson.create(redisConfig);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(SpringUtils.class); context.registerBean(Converter.class, () -> new Converter());
        context.registerBean(RedissonClient.class, () -> redis, bean -> bean.setDestroyMethodName(""));
        context.registerBean(DsTxEventListenerFactory.class);
        context.registerBean(ExternalAuthConfigurationCache.class);
        context.registerBean(ExternalAuthConfigChangeListener.class);
        context.getEnvironment().setActiveProfiles("dev");
        context.registerBean(org.namewta.common.social.oidc.OidcProtocolClient.class);
        context.refresh();
        var config = new MybatisConfiguration(new Environment("external-auth-owned", new SpringManagedTransactionFactory(), routing));
        config.setMapUnderscoreToCamelCase(true);
        GlobalConfigUtils.setGlobalConfig(config, GlobalConfigUtils.defaults().setMetaObjectHandler(new InjectionMetaObjectHandler()));
        var interceptor = new MybatisPlusInterceptor(); interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor()); config.addInterceptor(interceptor);
        for (var mapper : List.of(SysAuthProviderMapper.class, SysAuthRegistrationMapper.class, SysClientMapper.class)) {
            config.addMapper(mapper);
        }
        for (String xml : List.of("/mapper/system/SysAuthProviderMapper.xml", "/mapper/system/SysAuthRegistrationMapper.xml")) {
            try (var input = getClass().getResourceAsStream(xml)) {
                assertThat(input).isNotNull(); new XMLMapperBuilder(input, config, xml, config.getSqlFragments()).parse();
            }
        }
        var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        var cipher = new SocialSecretCipher(Base64.getEncoder().encodeToString(new byte[32]));
        var cache = context.getBean(ExternalAuthConfigurationCache.class);
        management = transactional(new SysExternalAuthConfigServiceImpl(sessions.getMapper(SysAuthProviderMapper.class),
            sessions.getMapper(SysAuthRegistrationMapper.class), sessions.getMapper(SysClientMapper.class), cipher, context, cache, context.getBean(org.namewta.common.social.oidc.OidcProtocolClient.class)));
        runtime = new ExternalAuthConfigurationServiceImpl(sessions.getMapper(SysAuthProviderMapper.class),
            sessions.getMapper(SysAuthRegistrationMapper.class), cache, cipher);
        // Controller 仍验证管理端；测试为隔离数据库创建真实 Admin Client 会话上下文。
        providerController = new SysAuthProviderController(management);
        registrationController = new SysAuthRegistrationController(management);
    }

    @BeforeEach
    void seed() {
        db.update("delete from sys_auth_registration"); db.update("delete from sys_auth_provider");
        db.update("delete from sys_client where client_id='external-auth-owned'");
        db.update("insert into sys_client(id,client_id,client_key,grant_type,status) values (991001,'external-auth-owned','external-auth-owned','password,social','0')");
        db.update("insert into sys_client(id,client_id,client_key,grant_type,status) select 991002,'external-auth-admin','pc','password,social','0' where not exists (select 1 from sys_client where client_key='pc')");
        var login = new org.namewta.system.api.model.LoginUser();
        login.setClientPk(db.queryForObject("select id from sys_client where client_key='pc'", Long.class));
        loginContext = org.mockito.Mockito.mockStatic(org.namewta.common.satoken.utils.LoginHelper.class);
        loginContext.when(org.namewta.common.satoken.utils.LoginHelper::getLoginUser).thenReturn(login);
        redis.getKeys().deleteByPattern("system:external-auth:*");
    }

    @AfterEach
    void clearLogin() { if (loginContext != null) loginContext.close(); }

    @AfterAll
    void close() throws Exception {
        if (context != null) context.close(); if (redis != null) redis.shutdown(); if (routing != null) routing.destroy();
        ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory", previousFactory);
        ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "applicationContext", previousContext);
    }

    @Test
    void controllerServiceMapperXmlRoundTripKeepsSecretsEncryptedAndCacheLossRefills() {
        long providerId = providerController.add(provider()).getData();
        var registration = registration(providerId);
        long id = registrationController.add(registration).getData();
        String encrypted = db.queryForObject("select client_secret_ciphertext from sys_auth_registration where auth_registration_id=?", String.class, id);
        assertThat(encrypted).startsWith("v1.").doesNotContain("private-client-secret");
        String response = JsonUtils.toJsonString(registrationController.detail(id).getData());
        assertThat(response).contains("secretConfigured").doesNotContain("private-client-secret", "clientSecretCiphertext", "clientSecret\"");
        assertThat(runtime.listEnabled("external-auth-owned")).hasSize(1);
        assertThat(runtime.listEnabled("another-client")).isEmpty();
        var result = runtime.require("corporate", "external-auth-owned");
        String key = ExternalAuthConfigurationCache.key(id, 0, 0);
        assertThat(result.clientSecret()).isEqualTo("private-client-secret");
        assertThat(redis.getBucket(key).isExists()).isTrue();
        var anotherNode = new ExternalAuthConfigurationCache();
        assertThat(anotherNode.get(key).registration().getClientSecretCiphertext()).isEqualTo(encrypted);
        redis.getBucket(key).delete();
        assertThat(runtime.require("corporate", "external-auth-owned").clientSecret()).isEqualTo("private-client-secret");
        assertThat(redis.getBucket(key).isExists()).isTrue();
        assertThat(db.queryForObject("select create_time is not null and update_time is not null from sys_auth_registration where auth_registration_id=?", Boolean.class, id)).isTrue();
    }

    @Test
    void blankSecretEditRetainsCiphertextAndCommitInvalidatesEveryNode() {
        long providerId = management.saveProvider(provider(), true);
        long id = management.saveRegistration(registration(providerId), true);
        var before = runtime.require("corporate", "external-auth-owned");
        String oldKey = ExternalAuthConfigurationCache.key(id, 0, 0);
        String cipherBefore = db.queryForObject("select client_secret_ciphertext from sys_auth_registration where auth_registration_id=?", String.class, id);
        var edit = registration(providerId); edit.setId(id); edit.setVersion(0L); edit.setClientSecret(" ");
        edit.setScopes(List.of("openid", "email")); management.saveRegistration(edit, false);
        assertThat(new ExternalAuthConfigurationCache().get(oldKey)).isNull();
        assertThat(db.queryForObject("select client_secret_ciphertext from sys_auth_registration where auth_registration_id=?", String.class, id)).isEqualTo(cipherBefore);
        assertThat(runtime.isCurrent(before.id(), before.version(), before.providerVersion())).isFalse();
        assertThat(runtime.require("corporate", "external-auth-owned").scopes()).containsExactly("openid", "email");
        assertThatThrownBy(() -> management.saveRegistration(edit, false)).isInstanceOf(ServiceException.class);
        assertThat(management.registration(id).getVersion()).isEqualTo(1L);
    }

    @Test
    void databaseDisablingAnyParticipantOverridesWarmRedisCache() {
        long providerId = management.saveProvider(provider(), true);
        long id = management.saveRegistration(registration(providerId), true);
        runtime.require("corporate", "external-auth-owned");
        for (String condition : List.of("status='1'", "grant_type='password'", "del_flag='1'")) {
            db.update("update sys_client set " + condition + " where client_id='external-auth-owned'");
            assertThatThrownBy(() -> runtime.require("corporate", "external-auth-owned")).isInstanceOf(ServiceException.class);
            assertThat(runtime.listEnabled("external-auth-owned")).isEmpty();
            db.update("update sys_client set status='0',grant_type='password,social',del_flag='0' where client_id='external-auth-owned'");
        }
        db.update("update sys_auth_provider set enabled=0 where auth_provider_id=?", providerId);
        assertThat(runtime.isCurrent(id, 0, 0)).isFalse();
        assertThatThrownBy(() -> runtime.require("corporate", "external-auth-owned")).isInstanceOf(ServiceException.class);
        assertThat(runtime.requireForLogout(id).externalClientId()).isEqualTo("external-rp");
    }

    @Test
    void logicalRemovalPreservesLogoutAndFreesUniqueSlotForNewRegistration() {
        long providerId = management.saveProvider(provider(), true);
        long id = management.saveRegistration(registration(providerId), true);
        assertThatThrownBy(() -> management.saveRegistration(registration(providerId), true)).isInstanceOf(ServiceException.class);
        management.removeRegistration(new ExternalAuthRemoveBo(id, 0L));
        assertThat(runtime.listEnabled("external-auth-owned")).isEmpty();
        assertThat(runtime.requireForLogout(id).clientSecret()).isEqualTo("private-client-secret");
        assertThatThrownBy(() -> management.registration(id)).isInstanceOf(ServiceException.class);
        long replacement = management.saveRegistration(registration(providerId), true);
        assertThat(replacement).isNotEqualTo(id);
        assertThatThrownBy(() -> management.removeProvider(new ExternalAuthRemoveBo(providerId, 0L))).isInstanceOf(ServiceException.class);
        assertThat(db.queryForObject("select count(*) from sys_auth_registration", Integer.class)).isEqualTo(2);
    }

    @Test
    void rollbackRestoresDatabaseAndDoesNotInvalidateWarmCache() {
        long providerId = management.saveProvider(provider(), true);
        long id = management.saveRegistration(registration(providerId), true);
        runtime.require("corporate", "external-auth-owned");
        String key = ExternalAuthConfigurationCache.key(id, 0, 0);
        var edit = registration(providerId); edit.setId(id); edit.setVersion(0L); edit.setClientSecret("replacement-secret");
        var failing = transactional(new RollbackProbe(management));
        assertThatThrownBy(() -> failing.editThenFail(edit)).isInstanceOf(IllegalStateException.class).hasMessage("rollback probe");
        assertThat(management.registration(id).getVersion()).isEqualTo(0L);
        assertThat(redis.getBucket(key).isExists()).isTrue();
        assertThat(runtime.require("corporate", "external-auth-owned").clientSecret()).isEqualTo("private-client-secret");
    }

    @Test
    void providerVersionAlsoInvalidatesCachedRegistrationAndRefreshCanRebuild() {
        long providerId = management.saveProvider(provider(), true);
        long id = management.saveRegistration(registration(providerId), true);
        runtime.require("corporate", "external-auth-owned");
        var edit = provider(); edit.setId(providerId); edit.setVersion(0L); edit.setName("新名称");
        management.saveProvider(edit, false);
        assertThat(redis.getBucket(ExternalAuthConfigurationCache.key(id, 0, 0)).isExists()).isFalse();
        assertThat(runtime.isCurrent(id, 0, 0)).isFalse();
        assertThat(runtime.require("corporate", "external-auth-owned").name()).isEqualTo("新名称");
        management.refresh();
        assertThat(redis.getBucket(ExternalAuthConfigurationCache.key(id, 0, 1)).isExists()).isFalse();
        assertThat(runtime.require("corporate", "external-auth-owned").providerVersion()).isEqualTo(1L);
    }

    @Test
    void explicitLegacyPreviewSkipsExamplesAndImportIsIdempotentWithoutSecretEcho() {
        var legacy = new ExternalAuthLegacyProviderBo(); legacy.setExternalClientId("github-client");
        legacy.setClientSecret("legacy-private-secret"); legacy.setRedirectUri("http://127.0.0.1:3001/auth/callback");
        legacy.setScopes(List.of("read:user"));
        var request = new ExternalAuthLegacyImportBo(); request.setBusinessClientId("external-auth-owned");
        request.setType(java.util.Map.of("github", legacy, "gitee", new ExternalAuthLegacyProviderBo()));
        var preview = management.importLegacy(request);
        assertThat(preview.items()).extracting(item -> item.status()).containsExactlyInAnyOrder("READY", "SKIPPED");
        assertThat(JsonUtils.toJsonString(preview)).doesNotContain("legacy-private-secret");
        assertThat(db.queryForObject("select count(*) from sys_auth_provider", Integer.class)).isZero();
        request.setDryRun(false);
        var imported = management.importLegacy(request);
        assertThat(imported.items()).extracting(item -> item.status()).containsExactlyInAnyOrder("IMPORTED", "SKIPPED");
        assertThat(runtime.require("github", "external-auth-owned").clientSecret()).isEqualTo("legacy-private-secret");
        assertThat(management.importLegacy(request).items()).extracting(item -> item.status())
            .containsExactlyInAnyOrder("EXISTS", "SKIPPED");
        assertThat(db.queryForObject("select count(*) from sys_auth_registration", Integer.class)).isEqualTo(1);
    }

    @Test
    void invalidLegacyEntryRollsBackEarlierEntriesInTheSameImport() {
        var valid = new ExternalAuthLegacyProviderBo(); valid.setExternalClientId("github-client");
        valid.setClientSecret("legacy-private-secret"); valid.setRedirectUri("http://127.0.0.1:3001/auth/callback");
        var invalid = new ExternalAuthLegacyProviderBo(); invalid.setExternalClientId("gitee-client");
        invalid.setRedirectUri("https://*.example/callback");
        var values = new java.util.LinkedHashMap<String, ExternalAuthLegacyProviderBo>();
        values.put("github", valid); values.put("gitee", invalid);
        var request = new ExternalAuthLegacyImportBo(); request.setBusinessClientId("external-auth-owned");
        request.setDryRun(false); request.setType(values);
        assertThatThrownBy(() -> management.importLegacy(request)).isInstanceOf(ServiceException.class)
            .hasMessageContaining("整批未保存");
        assertThat(db.queryForObject("select count(*) from sys_auth_provider", Integer.class)).isZero();
        assertThat(db.queryForObject("select count(*) from sys_auth_registration", Integer.class)).isZero();
    }

    private SysAuthProviderBo provider() {
        var bo = new SysAuthProviderBo(); bo.setProviderKey("corporate"); bo.setName("企业身份");
        bo.setProtocol("OIDC"); bo.setIssuer("http://127.0.0.1:45406/identity"); bo.setEnabled(true); return bo;
    }

    private SysAuthRegistrationBo registration(long providerId) {
        var bo = new SysAuthRegistrationBo(); bo.setProviderId(providerId); bo.setBusinessClientId("external-auth-owned");
        bo.setExternalClientId("external-rp"); bo.setClientSecret("private-client-secret");
        bo.setRedirectUri("http://127.0.0.1:3001/auth/callback"); bo.setScopes(List.of("openid", "profile"));
        bo.setFirstLoginPolicy("BIND_ONLY"); bo.setEnabled(true); return bo;
    }

    @SuppressWarnings("unchecked")
    private static <T> T transactional(T target) {
        var factory = new ProxyFactory(target); factory.setProxyTargetClass(true);
        factory.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return (T) factory.getProxy();
    }

    public static class RollbackProbe {
        private final SysExternalAuthConfigServiceImpl service;
        public RollbackProbe(SysExternalAuthConfigServiceImpl service) { this.service = service; }
        @DSTransactional
        public void editThenFail(SysAuthRegistrationBo bo) { service.saveRegistration(bo, false); throw new IllegalStateException("rollback probe"); }
    }
}
