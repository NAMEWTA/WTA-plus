package org.namewta.test.profile.material;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.aopalliance.intercept.MethodInterceptor;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.json.config.JacksonConfig;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.handler.InjectionMetaObjectHandler;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.factory.OssFactory;
import org.namewta.common.oss.properties.OssProperties;
import org.namewta.common.redis.utils.CacheUtils;
import org.namewta.common.satoken.core.service.SaPermissionImpl;
import org.namewta.common.satoken.handler.SaTokenExceptionHandler;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.web.handler.GlobalExceptionHandler;
import org.namewta.profile.enterprise.adapter.api.EnterpriseMaterialOwnerContributor;
import org.namewta.profile.enterprise.controller.admin.EnterpriseMaterialAdminController;
import org.namewta.profile.enterprise.controller.advice.EnterpriseApplicationExceptionHandler;
import org.namewta.profile.enterprise.controller.self.EnterpriseApplicationController;
import org.namewta.profile.enterprise.controller.self.EnterpriseMaterialSelfController;
import org.namewta.profile.enterprise.dao.EnterpriseApplicationDao;
import org.namewta.profile.enterprise.mapper.EnterpriseApplicationMapper;
import org.namewta.profile.enterprise.port.gateway.EnterpriseWorkflowGateway;
import org.namewta.profile.enterprise.port.provider.EnterpriseVerificationProvider;
import org.namewta.profile.enterprise.port.provider.EnterpriseVerificationProviderRegistryPort;
import org.namewta.profile.enterprise.port.verification.EnterpriseVerificationService;
import org.namewta.profile.enterprise.service.EnterpriseApplicationService;
import org.namewta.profile.enterprise.service.EnterpriseMaterialService;
import org.namewta.profile.enterprise.service.EnterpriseProfileApiService;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseApplicationUseCaseImpl;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseProfileApiUseCaseImpl;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseProfileMaterialUseCaseImpl;
import org.namewta.profile.person.adapter.api.PersonProfileMaterialOwnerContributor;
import org.namewta.profile.person.adapter.security.SaTokenProfileMaterialAccessPolicy;
import org.namewta.profile.person.controller.admin.MaterialTagController;
import org.namewta.profile.person.controller.admin.PersonMaterialAdminController;
import org.namewta.profile.person.controller.admin.ProfileMaterialExceptionHandler;
import org.namewta.profile.person.controller.self.PersonApplicationController;
import org.namewta.profile.person.controller.self.PersonApplicationExceptionHandler;
import org.namewta.profile.person.controller.self.PersonMaterialSelfController;
import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.person.dao.ProfileMaterialDao;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.mapper.ProfileMaterialMapper;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.profile.person.port.provider.PersonVerificationProvider;
import org.namewta.profile.person.port.provider.PersonVerificationProviderRegistryPort;
import org.namewta.profile.person.port.verification.PersonVerificationService;
import org.namewta.profile.person.service.PersonApplicationService;
import org.namewta.profile.person.service.PersonProfileApiService;
import org.namewta.profile.person.service.ProfileMaterialService;
import org.namewta.profile.person.usecase.impl.PersonApplicationUseCaseImpl;
import org.namewta.profile.person.usecase.impl.PersonProfileApiUseCaseImpl;
import org.namewta.profile.person.usecase.impl.ProfileMaterialUseCaseImpl;
import org.namewta.system.api.ConfigService;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.controller.system.SysOssUploadController;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.config.OssLifecycleProperties;
import org.namewta.system.oss.mapper.SysOssRefMapper;
import org.namewta.system.oss.provider.DefaultOssObjectStore;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.namewta.system.oss.service.OssLifecycleManager;
import org.namewta.system.oss.upload.DefaultOssUploadIdentityResolver;
import org.namewta.system.oss.upload.DefaultOssUploadMetadataStore;
import org.namewta.system.oss.upload.DefaultOssUploadObjectStore;
import org.namewta.system.oss.upload.OssUploadProperties;
import org.namewta.system.oss.upload.OssUploadService;
import org.namewta.system.oss.upload.RedisOssUploadTicketStore;
import org.namewta.system.oss.upload.OssUploadTicket;
import org.namewta.profile.person.domain.exception.PersonApplicationException;
import org.namewta.system.service.impl.SysOssServiceImpl;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;

/**
 * Home -> MVC/Sa-Token/UseCase/Service/DAO/XML/MySQL，实际 OSS 控制面写入 MinIO。
 * 登录发行、导航载荷、供应商和工作流为显式 fixture；权限来自基座 Home 默认角色。
 */
@Tag("dev")
@EnabledIfSystemProperty(named = "browser.profile.integration", matches = "true")
class ProfileSelfMaterialsBrowserIntegrationTest {
    private static final String HOME = "428a8310cd442757ae699df5d894f051";
    private static final long HOME_PK = 1762000000000000002L;
    @TempDir Path temporary;

    @Test
    void selfApplicationsPersistMaterialsAndSubmissionSnapshots() throws Exception {
        Path root = Path.of(System.getProperty("namewta.repo.root"));
        URI endpoint = URI.create(System.getProperty("oss.minio.integration.endpoint"));
        assertThat(endpoint.getHost()).isEqualTo("127.0.0.1");
        String jdbc = System.getProperty("profile.mysql.integration.url");
        assertThat(jdbc).contains("127.0.0.1:", "/namewta_profile_test_");
        var pooled = new PooledDataSource("com.mysql.cj.jdbc.Driver", jdbc, "root", "owned-profile-test-only");
        var dataSource = new DynamicRoutingDataSource(List.of());
        dataSource.setPrimary("master"); dataSource.setStrict(true); dataSource.addDataSource("master", pooled);
        var db = new JdbcTemplate(dataSource);
        String bucket = "t14-profile-" + UUID.randomUUID().toString().replace("-", "");
        var config = new Config(); config.setThreads(2).setNettyThreads(2);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + System.getProperty("profile.redis.integration.port"))
            .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
        RedissonClient redis = Redisson.create(config);
        var context = new AnnotationConfigWebApplicationContext();
        var server = new Server();
        var connector = new ServerConnector(server); connector.setHost("127.0.0.1"); connector.setPort(0); server.addConnector(connector);
        Process browser = null;
        var previousConfig = SaManager.getConfig(); var previousContext = SaManager.getSaTokenContext();
        var previousPermissions = SaManager.getStpInterface(); var previousLogic = StpUtil.getStpLogic();
        try (var storage = S3Client.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("namewta", "namewta123")))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build()) {
            storage.createBucket(request -> request.bucket(bucket));
            var sessions = sessions(dataSource);
            var ossMapper = sessions.getMapper(SysOssMapper.class);
            var lifecycleProperties = new OssLifecycleProperties();
            var readinessProperties = new OssStorageReadinessProperties(); readinessProperties.setMaxSnapshotAge(Duration.ofHours(1));
            var readiness = new OssStorageReadinessRegistry(readinessProperties);
            readiness.replace(Map.of("minio", new OssStorageReadinessEntry("minio", AccessPolicy.PRIVATE, true,
                Set.of("profile-materials"), OssStorageReadinessEntry.Status.SERVING, OssStorageReadinessEntry.Reason.READY, Instant.now())), Set.of("minio"), true);
            var lifecycle = transactional(new OssLifecycleManager(ossMapper, sessions.getMapper(SysOssRefMapper.class),
                new DefaultOssObjectStore(), lifecycleProperties, readiness));
            var oss = new SysOssServiceImpl(ossMapper, lifecycle);
            var personDao = new PersonApplicationDao(sessions.getMapper(PersonApplicationMapper.class));
            var enterpriseDao = new EnterpriseApplicationDao(sessions.getMapper(EnterpriseApplicationMapper.class));
            var personOwners = new PersonProfileMaterialOwnerContributor(transactional(new PersonProfileApiUseCaseImpl(new PersonProfileApiService(personDao))));
            var enterpriseOwners = new EnterpriseMaterialOwnerContributor(transactional(new EnterpriseProfileApiUseCaseImpl(new EnterpriseProfileApiService(enterpriseDao))));
            var materials = new ProfileMaterialService(new ProfileMaterialDao(sessions.getMapper(ProfileMaterialMapper.class)),
                oss, new SaTokenProfileMaterialAccessPolicy(), List.of(personOwners, enterpriseOwners));
            var personMaterials = transactional(new ProfileMaterialUseCaseImpl(materials));
            var enterpriseMaterials = transactional(new EnterpriseProfileMaterialUseCaseImpl(new EnterpriseMaterialService(materials)));
            ConfigService configurations = mock(ConfigService.class);
            when(configurations.getConfigValue(anyString())).thenAnswer(invocation -> db.queryForObject(
                "select config_value from sys_config where config_key = ? limit 1", String.class, invocation.getArgument(0, String.class)));
            PersonVerificationProviderRegistryPort personProviders = code -> mock(PersonVerificationProvider.class);
            EnterpriseVerificationProviderRegistryPort enterpriseProviders = code -> mock(EnterpriseVerificationProvider.class);
            var workflow = mock(PersonWorkflowGateway.class);
            var workflowFailure = new java.util.concurrent.atomic.AtomicBoolean();
            doAnswer(invocation -> {
                if (LoginHelper.getUserId() == 940010L && workflowFailure.compareAndSet(false, true)) {
                    Long submissionId = invocation.getArgument(1, Long.class);
                    assertThat(db.queryForObject("select count(*) from profile_person_submission where person_submission_id=?", Long.class, submissionId)).isEqualTo(1);
                    assertThat(db.queryForObject("select count(*) from profile_material_ref where owner_type='SUBMISSION' and owner_id=?", Long.class, submissionId)).isEqualTo(2);
                    throw new PersonApplicationException("OWNED_WORKFLOW_FAILURE");
                }
                return null;
            }).when(workflow).start(anyLong(), anyLong(), anyInt());
            var person = transactional(new PersonApplicationUseCaseImpl(new PersonApplicationService(personDao, materials,
                personProviders, mock(PersonVerificationService.class), workflow, configurations)));
            var enterprise = transactional(new EnterpriseApplicationUseCaseImpl(new EnterpriseApplicationService(enterpriseDao, materials,
                enterpriseProviders, mock(EnterpriseVerificationService.class), mock(EnterpriseWorkflowGateway.class), configurations)));
            var uploads = new OssUploadService(uploadProperties(), new DefaultOssUploadIdentityResolver(), new RedisOssUploadTicketStore(redis),
                new DefaultOssUploadObjectStore(), transactional(new DefaultOssUploadMetadataStore(ossMapper, lifecycleProperties)), readiness);
            context.register(MvcConfiguration.class);
            context.addBeanFactoryPostProcessor(factory -> {
                factory.registerSingleton("ownedRedis", redis);
                factory.registerSingleton("ownedCaches", new ConcurrentMapCacheManager());
                factory.registerSingleton("sessionFixture", new SessionFixture(db, new RedisOssUploadTicketStore(redis)));
                factory.registerSingleton("personApplication", new PersonApplicationController(person));
                factory.registerSingleton("enterpriseApplication", new EnterpriseApplicationController(enterprise));
                factory.registerSingleton("personMaterials", new PersonMaterialSelfController(personMaterials));
                factory.registerSingleton("personMaterialReads", new PersonMaterialAdminController(personMaterials));
                factory.registerSingleton("enterpriseMaterials", new EnterpriseMaterialSelfController(enterpriseMaterials));
                factory.registerSingleton("enterpriseMaterialReads", new EnterpriseMaterialAdminController(enterpriseMaterials));
                factory.registerSingleton("materialTags", new MaterialTagController(personMaterials));
                factory.registerSingleton("uploads", new SysOssUploadController(diagnostic(uploads)));
            });
            SaManager.setConfig(new cn.dev33.satoken.config.SaTokenConfig().setTokenName("Authorization")
                .setTokenPrefix("Bearer").setJwtSecretKey("owned-profile-integration-secret-at-least-32-characters"));
            SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
            SaManager.setStpInterface(new SaPermissionImpl()); StpUtil.setStpLogic(new StpLogicJwtForSimple());
            var handler = new ServletContextHandler(); handler.setContextPath("/");
            var mvc = new ServletHolder(new DispatcherServlet(context)); mvc.setInitOrder(1); handler.addServlet(mvc, "/prod-api/*");
            Path dist = root.resolve("frontend/apps/home-web/dist"); assertThat(dist.resolve("index.html")).isRegularFile();
            handler.addServlet(new ServletHolder(new StaticApp(dist)), "/"); server.setHandler(handler); server.start();
            var properties = new OssProperties(); properties.setEndpoint(endpoint.getAuthority()); properties.setIsHttps("N");
            properties.setAccessKey("namewta"); properties.setSecretKey("namewta123"); properties.setBucketName(bucket);
            properties.setRegion("us-east-1"); properties.setAccessPolicy("0");
            CacheUtils.put(CacheNames.SYS_OSS_CONFIG, "minio", JsonUtils.toJsonString(properties));
            var command = new ProcessBuilder("corepack", "pnpm", "exec", "playwright", "test", "--config", "playwright.profile.config.ts")
                .directory(root.resolve("frontend").toFile());
            command.environment().put("PROFILE_TEST_ORIGIN", "http://127.0.0.1:" + connector.getLocalPort());
            Path log = temporary.resolve("chrome.log"); browser = command.redirectErrorStream(true).redirectOutput(log.toFile()).start();
            assertThat(browser.waitFor(300, TimeUnit.SECONDS)).as("owned Chrome terminates").isTrue();
            System.out.println(Files.readString(log).replaceAll("([?&]X-Amz-[^=]+)=([^&\\s\"']+)", "$1=[REDACTED]"));
            assertThat(browser.exitValue()).as("real Profile Chrome scenarios").isZero();
            assertThat(workflowFailure).as("failure was injected after submission and material snapshot inserts").isTrue();
            assertThat(db.queryForObject("select count(*) from profile_person_application where status='WAITING'", Long.class)).isGreaterThanOrEqualTo(1);
            assertThat(db.queryForObject("select count(*) from profile_enterprise_application where status='WAITING'", Long.class)).isGreaterThanOrEqualTo(2);
            assertThat(db.queryForObject("select count(*) from profile_material_ref where owner_type='SUBMISSION' and immutable_flag='Y'", Long.class)).isGreaterThanOrEqualTo(7);
            assertThat(db.queryForObject("select count(*) from sys_oss_ref where del_flag='0'", Long.class)).isGreaterThanOrEqualTo(14);
            var persisted = db.queryForList("select file_name from sys_oss where create_by between 940001 and 940099", String.class);
            assertThat(persisted).isNotEmpty();
            for (String key : persisted) assertThat(storage.getObjectAsBytes(request -> request.bucket(bucket).key(key)).asByteArray()).isNotEmpty();
            System.out.println("T-14: actual Profile/MySQL transactions, immutable snapshots, OSS metadata and private MinIO bytes verified; providers/workflow/login issuance are fixtures");
        } finally {
            if (browser != null && browser.isAlive()) browser.destroyForcibly().waitFor();
            server.stop(); OssFactory.remove("minio"); context.close(); redis.shutdown(); dataSource.destroy(); pooled.forceCloseAll();
            SaManager.setConfig(previousConfig); SaManager.setSaTokenContext(previousContext); SaManager.setStpInterface(previousPermissions); StpUtil.setStpLogic(previousLogic);
            ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", null); ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", null);
            ReflectionTestUtils.setField(JsonUtils.class, "JSON_MAPPER", null);
        }
    }

    private SqlSessionTemplate sessions(DynamicRoutingDataSource dataSource) throws IOException {
        var configuration = new MybatisConfiguration(new Environment("profile-material", new SpringManagedTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        var global = GlobalConfigUtils.defaults(); global.setMetaObjectHandler(new InjectionMetaObjectHandler()); GlobalConfigUtils.setGlobalConfig(configuration, global);
        for (String resource : List.of("mapper/person/PersonApplicationMapper.xml", "mapper/enterprise/EnterpriseApplicationMapper.xml",
            "mapper/person/ProfileMaterialMapper.xml", "mapper/system/SysOssMapper.xml", "mapper/system/SysOssRefMapper.xml")) {
            try (var xml = getClass().getResourceAsStream("/" + resource)) {
                assertThat(xml).as(resource).isNotNull(); new XMLMapperBuilder(xml, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        return new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
    }

    private OssUploadProperties uploadProperties() {
        var yaml = new YamlPropertiesFactoryBean(); yaml.setResources(new ClassPathResource("application.yml"));
        var source = new PropertiesPropertySource("actual-application", yaml.getObject());
        var properties = new Binder(ConfigurationPropertySources.from(source)).bind("oss.direct-upload", Bindable.of(OssUploadProperties.class)).get();
        properties.validate(); assertThat(properties.requirePolicy("general").getExpectedAccessPolicy()).isEqualTo(AccessPolicy.PRIVATE);
        return properties;
    }

    private static <T> T transactional(T target) {
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true); proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        @SuppressWarnings("unchecked") T result = (T) proxy.getProxy(); return result;
    }

    /** 仅记录异常类型和代码位置，不记录上传票、URL、SQL 参数或异常消息。 */
    private static <T> T diagnostic(T target) {
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvice((MethodInterceptor) invocation -> {
            try { return invocation.proceed(); }
            catch (Throwable failure) {
                for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                    System.out.println("T-14 diagnostic: " + cause.getClass().getName());
                    java.util.Arrays.stream(cause.getStackTrace()).limit(8).forEach(frame -> System.out.println("  " + frame));
                }
                throw failure;
            }
        });
        @SuppressWarnings("unchecked") T result = (T) proxy.getProxy(); return result;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class MvcConfiguration implements WebMvcConfigurer {
        @Bean static SpringUtils springUtils() { return new SpringUtils(); }
        @Bean JsonMapper jsonMapper() { return JsonMapper.builder().addModule(new JacksonConfig().registerJavaTimeModule()).build(); }
        @Bean GlobalExceptionHandler globalErrors() { return new GlobalExceptionHandler(); }
        @Bean SaTokenExceptionHandler permissionErrors() { return new SaTokenExceptionHandler(); }
        @Bean ProfileMaterialExceptionHandler materialErrors() { return new ProfileMaterialExceptionHandler(); }
        @Bean PersonApplicationExceptionHandler personErrors() { return new PersonApplicationExceptionHandler(); }
        @Bean EnterpriseApplicationExceptionHandler enterpriseErrors() { return new EnterpriseApplicationExceptionHandler(); }
        @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(new SaInterceptor()); }
        @Override public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
            builder.withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper()));
        }
    }

    /** 测试登录发行；权限读取真实基座 Home Client 默认角色。 */
    @RestController
    static class SessionFixture {
        private final JdbcTemplate db;
        private final RedisOssUploadTicketStore tickets;
        SessionFixture(JdbcTemplate db, RedisOssUploadTicketStore tickets) { this.db = db; this.tickets = tickets; }

        /** 仅测试容器：冻结当前本人票据的截止时间，实际 complete 仍走生产校验。 */
        @PostMapping("/fixture/expire-ticket")
        R<Void> expireTicket(@RequestParam String token) {
            var ticket = tickets.get(token);
            assertThat(ticket).isNotNull();
            assertThat(ticket.userId()).isEqualTo(LoginHelper.getUserId()).isBetween(940001L, 940099L);
            assertThat(ticket.clientPk()).isEqualTo(HOME_PK);
            tickets.save(new OssUploadTicket(ticket.token(), ticket.policyKey(), ticket.mode(), ticket.state(),
                ticket.service(), ticket.bucket(), ticket.objectKey(), ticket.uploadId(), ticket.originalName(),
                ticket.fileSuffix(), ticket.fileSize(), ticket.contentType(), ticket.fingerprint(), ticket.fingerprintDigest(),
                ticket.userId(), ticket.clientPk(), ticket.partSize(), ticket.partCount(), ticket.createdAt(),
                System.currentTimeMillis() - 1, ticket.ossId()), Duration.ofMinutes(1));
            return R.ok();
        }

        /** 浏览器从独立 HTTP 请求检查已提交数据库状态，不能读取上一事务的未提交数据。 */
        @GetMapping("/fixture/transaction-state")
        R<Map<String, Object>> transactionState() {
            Long user = LoginHelper.getUserId(); assertThat(user).isBetween(940001L, 940099L);
            Long application = db.queryForObject("select person_application_id from profile_person_application where applicant_user_id=?", Long.class, user);
            return R.ok(Map.of("status", db.queryForObject("select status from profile_person_application where person_application_id=?", String.class, application),
                "submissions", db.queryForObject("select count(*) from profile_person_submission where person_application_id=?", Integer.class, application),
                "snapshots", db.queryForObject("select count(*) from profile_material_ref m join profile_person_submission s on m.owner_id=s.person_submission_id where m.owner_type='SUBMISSION' and s.person_application_id=?", Integer.class, application),
                "ossReferences", db.queryForObject("select count(*) from sys_oss_ref where create_by=? and del_flag='0'", Integer.class, user)));
        }
        @PostMapping("/fixture/session")
        R<Map<String, String>> login(@RequestParam long userId, @RequestParam(defaultValue = "false") boolean denied) {
            assertThat(userId).isBetween(940001L, 940099L);
            var user = new LoginUser(); user.setUserId(userId); user.setUsername("profile-owned-" + userId); user.setUserType("app");
            user.setClientKey("home-web"); user.setClientPk(HOME_PK); user.setDeptId(1761000000000000103L); user.setIpaddr("127.0.0.1"); user.setLoginLocation("owned fixture");
            Set<String> permissions = new HashSet<>(db.queryForList("select m.perms from sys_menu m join sys_role_menu rm on rm.menu_id=m.menu_id where rm.role_id=1761300000000000010 and m.client_id=? and m.status='0' and m.perms<>''", String.class, HOME_PK));
            assertThat(permissions).contains("profile:person:apply", "profile:enterprise:apply", "profile:person:material", "profile:enterprise:material", "system:oss:upload");
            assertThat(permissions).doesNotContain("system:oss:list", "system:oss:download", "system:oss:remove");
            if (denied) permissions.removeIf(permission -> permission.endsWith(":material") || permission.equals("system:oss:upload"));
            user.setMenuPermission(permissions); user.setRolePermission(Set.of("app_user")); LoginHelper.login(user, new SaLoginParameter().setTimeout(600).setExtra(LoginHelper.CLIENT_KEY, HOME).setExtra(LoginHelper.CLIENT_PK_KEY, HOME_PK));
            return R.ok(Map.of("token", StpUtil.getTokenValue()));
        }
        @GetMapping("/system/user/getInfo")
        R<Map<String, Object>> info() {
            LoginUser user = LoginHelper.getLoginUser();
            return R.ok(Map.of("user", Map.of("userId", user.getUserId(), "userName", user.getUsername(), "nickName", "材料测试", "avatarUrl", ""),
                "roles", user.getRolePermission(), "permissions", user.getMenuPermission()));
        }
        @GetMapping("/system/menu/getRouters")
        R<List<Map<String, Object>>> menus() {
            return R.ok(List.of(Map.of("path", "/profile", "name", "Profile", "component", "profile/center/index", "meta", Map.of("title", "档案中心", "domainId", "profile"),
                "children", List.of(Map.of("path", "person", "name", "PersonVerification", "component", "profile/person/application", "meta", Map.of("title", "个人认证", "domainId", "profile")),
                    Map.of("path", "enterprise", "name", "EnterpriseVerification", "component", "profile/enterprise/application", "meta", Map.of("title", "企业认证", "domainId", "profile"))))));
        }
        @GetMapping("/auth/client/context") R<Map<String, Boolean>> client() { return R.ok(Map.of("clientEnabled", true, "registerEnabled", false)); }
    }

    private static final class StaticApp extends HttpServlet {
        private final Path dist;
        StaticApp(Path dist) { this.dist = dist; }
        @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            Path file = dist.resolve(request.getRequestURI().substring(1)).normalize(); if (!file.startsWith(dist)) { response.setStatus(404); return; }
            if (!Files.isRegularFile(file)) file = dist.resolve("index.html"); String name = file.getFileName().toString();
            response.setContentType(name.endsWith(".js") ? "text/javascript" : name.endsWith(".css") ? "text/css" : "text/html"); Files.copy(file, response.getOutputStream());
        }
    }
}
