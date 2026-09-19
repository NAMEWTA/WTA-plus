package org.namewta.test.phone;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.github.yulichang.injector.MPJSqlInjector;
import com.zaxxer.hikari.HikariDataSource;
import io.github.linpeilie.Converter;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.mapping.Environment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.web.config.properties.CaptchaProperties;
import org.namewta.system.api.OssService;
import org.namewta.system.api.model.RegisterBody;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserImportVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.listener.SysUserImportListener;
import org.namewta.system.mapper.*;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.*;
import org.namewta.system.service.impl.SysUserServiceImpl;
import org.namewta.system.service.impl.SysUserTypeRelServiceImpl;
import org.namewta.web.service.SysRegisterService;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.namewta.common.core.constant.GlobalConstants;

import java.util.List;
import java.net.URI;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 任务自有 MySQL 中验证真实写入、动态事务和导入行隔离，不连接运行库。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "phone.mysql.integration", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhoneRegistrationMySqlIntegrationTest {
    private DynamicRoutingDataSource routing;
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate db;
    private SysUserServiceImpl users;
    private SysRegisterService registration;
    private PasswordPolicyService passwords;
    private ValidatorFactory validator;
    private Object previousFactory, previousContext;
    private boolean capturedSpringState;
    private String ownerToken;
    private String schemaName;
    private RedissonClient redis;
    private CaptchaProperties captcha;

    static void requireOwnedDatabaseUrl(String url) {
        if (url == null || !url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("A task-owned phone test database is required");
        }
        URI target = URI.create(url.substring(5));
        if (!"127.0.0.1".equals(target.getHost()) || target.getPort() < 1 || target.getPort() > 65535
            || target.getUserInfo() != null || target.getFragment() != null
            || !target.getRawPath().matches("/namewta_phone_test_[A-Za-z0-9_]+")
            || (target.getRawQuery() != null && !target.getRawQuery().matches(
                "(?:useSSL=false|allowPublicKeyRetrieval=true|serverTimezone=[A-Za-z_/]+)(?:&(?:useSSL=false|allowPublicKeyRetrieval=true|serverTimezone=[A-Za-z_/]+))*"))) {
            throw new IllegalArgumentException("A task-owned phone test database is required");
        }
    }

    @BeforeAll
    void open() {
        String url = System.getProperty("phone.mysql.integration.url");
        requireOwnedDatabaseUrl(url);
        ownerToken = System.getProperty("phone.mysql.integration.owner");
        if (ownerToken == null || !ownerToken.matches("[a-f0-9]{32}")) {
            throw new IllegalArgumentException("The fixture's random ownership token is required");
        }
        schemaName = URI.create(url.substring(5)).getPath().substring(1);
        previousFactory = ReflectionTestUtils.getField(SpringUtil.class, "beanFactory");
        previousContext = ReflectionTestUtils.getField(SpringUtil.class, "applicationContext");
        capturedSpringState = true;
        var pool = new HikariDataSource(); pool.setJdbcUrl(url);
        pool.setUsername(System.getProperty("phone.mysql.integration.username", "root"));
        pool.setPassword(System.getProperty("phone.mysql.integration.password"));
        pool.setMaximumPoolSize(4);
        routing = new DynamicRoutingDataSource(List.of()); routing.setPrimary("master"); routing.addDataSource("master", pool);
        // 独立连接读取已提交状态，避免在断言中复用动态事务的连接。
        var candidate = new JdbcTemplate(pool);
        requireOwnership(candidate);
        db = candidate;
        context = new AnnotationConfigApplicationContext();
        int redisPort = Integer.parseInt(System.getProperty("phone.redis.integration.port"));
        if (redisPort < 1 || redisPort > 65535) throw new IllegalArgumentException("Invalid owned Redis port");
        var redisConfig = new Config();
        redisConfig.useSingleServer().setAddress("redis://127.0.0.1:" + redisPort);
        redis = Redisson.create(redisConfig);
        context.registerBean(RedissonClient.class, () -> redis, bean -> bean.setDestroyMethodName(""));
        context.registerBean(SpringUtils.class); context.registerBean(Converter.class, () -> new Converter());
        context.registerBean("messageSource", StaticMessageSource.class, () -> {
            var messages = new StaticMessageSource(); messages.setUseCodeAsDefaultMessage(true); return messages;
        });
        context.refresh();
        var config = new MybatisConfiguration(new Environment("owned-phone", new SpringManagedTransactionFactory(), routing));
        config.setMapUnderscoreToCamelCase(true);
        GlobalConfigUtils.setGlobalConfig(config, GlobalConfigUtils.defaults().setSqlInjector(new MPJSqlInjector()));
        config.addMapper(SysUserMapper.class); config.addMapper(SysUserTypeRelMapper.class); config.addMapper(SysUserTypeMapper.class);
        var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        var grants = transactional(new SysUserTypeRelServiceImpl(sessions.getMapper(SysUserTypeRelMapper.class),
            sessions.getMapper(SysUserTypeMapper.class), mock(ClientSessionService.class)));
        users = transactional(new SysUserServiceImpl(sessions.getMapper(SysUserMapper.class), mock(SysDeptMapper.class),
            mock(SysRoleMapper.class), mock(SysPostMapper.class), mock(SysUserRoleMapper.class), mock(SysUserPostMapper.class),
            mock(SysClientMapper.class), sessions.getMapper(SysUserTypeMapper.class), mock(ClientSessionService.class), grants,
            mock(OssService.class)));
        var clients = mock(ISysClientService.class); var types = mock(ISysUserTypeService.class);
        var client = new SysClientVo(); client.setStatus("0"); client.setRegisterEnabled(true); client.setUserTypeId(9L);
        when(clients.queryByClientId("owned-phone")).thenReturn(client);
        var type = new SysUserTypeVo(); type.setStatus("0"); when(types.queryById(9L)).thenReturn(type);
        captcha = new CaptchaProperties(); captcha.setEnable(false);
        passwords = mock(PasswordPolicyService.class); when(passwords.generateDefaultPassword()).thenReturn("OwnedPass!9");
        registration = transactional(new SysRegisterService(users, captcha, clients, types, grants, passwords));
        validator = Validation.buildDefaultValidatorFactory();
    }

    @BeforeEach
    @AfterEach
    void clean() {
        if (db == null) return;
        requireOwnership(db);
        if (captcha != null) captcha.setEnable(false);
        db.execute("drop trigger if exists owned_phone_grant_failure");
        db.update("delete from sys_user_type_rel");
        db.update("delete from sys_user");
    }

    private void requireOwnership(JdbcTemplate connection) {
        assertThat(connection.queryForObject("select database()", String.class)).isEqualTo(schemaName);
        assertThat(connection.queryForObject("select owner_token from owned_phone_fixture where id = 1", String.class))
            .isEqualTo(ownerToken);
    }

    @AfterAll
    void close() throws Exception {
        try {
            if (validator != null) validator.close();
            if (context != null) context.close();
            if (routing != null) routing.destroy();
            if (redis != null) redis.shutdown();
        } finally {
            if (capturedSpringState) {
                ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", previousFactory);
                ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", previousContext);
            }
        }
    }

    @Test
    void invalidPhonePreservesTheRealCaptchaUntilAValidRegistrationConsumesIt() {
        captcha.setEnable(true);
        String key = GlobalConstants.CAPTCHA_CODE_KEY + ownerToken;
        var challenge = redis.<String>getBucket(key);
        challenge.set("1234");
        try {
            var body = registrationBody("captcha-user", " ");
            body.setUuid(ownerToken); body.setCode("1234");
            assertThatThrownBy(() -> registration.register(body)).isInstanceOf(ServiceException.class)
                .hasMessageContaining("手机号码");
            assertThat(challenge.get()).isEqualTo("1234");
            body.setPhoneNumber("13800138008");
            registration.register(body);
            assertThat(challenge.get()).isNull();
            assertThat(phone("captcha-user")).isEqualTo("13800138008");
        } finally {
            challenge.delete();
        }
    }

    @Test
    void grantFailureRollsBackTheAlreadyInsertedAccountAndSuccessfulRetryCommitsBoth() {
        db.execute("""
            create trigger owned_phone_grant_failure before insert on sys_user_type_rel for each row begin
              if not exists (select 1 from sys_user where user_id = new.user_id) then
                signal sqlstate '45000' set message_text='fixture user missing before grant';
              end if;
              signal sqlstate '45000' set message_text='owned phone grant failure';
            end
            """);
        var body = registrationBody("registered", " 13800138000 ");
        assertThatThrownBy(() -> registration.register(body)).hasStackTraceContaining("owned phone grant failure");
        assertThat(count("sys_user")).isZero();
        assertThat(count("sys_user_type_rel")).isZero();
        db.execute("drop trigger owned_phone_grant_failure");
        registration.register(body);
        assertThat(count("sys_user")).isEqualTo(1);
        assertThat(count("sys_user_type_rel")).isEqualTo(1);
        assertThat(phone("registered")).isEqualTo("13800138000");
    }

    @Test
    void invalidNewAndPartialWritesLeaveEveryStoredFieldUnchanged() {
        seed("legacy", null); seed("current", "13800138000");
        for (String name : List.of("legacy", "current")) {
            for (String requested : new String[] {"", " ", "12345"}) {
                var before = db.queryForList("select * from sys_user order by user_id");
                var user = update(name, requested);
                assertThatThrownBy(() -> users.updateUser(user)).isInstanceOf(ServiceException.class);
                assertThatThrownBy(() -> users.updateUserProfile(user)).isInstanceOf(ServiceException.class);
                assertThat(db.queryForList("select * from sys_user order by user_id")).isEqualTo(before);
            }
        }
        var legacy = update("legacy", null);
        assertThatThrownBy(() -> users.updateUser(legacy)).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> users.updateUserProfile(legacy)).isInstanceOf(ServiceException.class);
        for (String requested : new String[] {null, "", " ", "invalid"}) {
            assertThatThrownBy(() -> users.insertUser(newUser("new-user", requested))).isInstanceOf(ServiceException.class);
            assertThatThrownBy(() -> registration.register(registrationBody("new-user", requested))).isInstanceOf(ServiceException.class);
        }
        assertThat(count("sys_user")).isEqualTo(2);
    }

    @Test
    void omittedPhonePreservesExistingValueAndValidWritesRepairLegacyAccounts() {
        seed("current", "13800138000"); seed("legacy", null);
        assertThat(users.updateUser(update("current", null))).isEqualTo(1);
        assertThat(phone("current")).isEqualTo("13800138000");
        assertThat(users.updateUserProfile(update("current", null))).isEqualTo(1);
        assertThat(phone("current")).isEqualTo("13800138000");
        assertThat(users.updateUser(update("legacy", " 13800138001 "))).isEqualTo(1);
        assertThat(phone("legacy")).isEqualTo("13800138001");
        assertThat(users.updateUserProfile(update("legacy", " 13800138002 "))).isEqualTo(1);
        assertThat(phone("legacy")).isEqualTo("13800138002");
        assertThat(users.insertUser(newUser("created", " 13800138003 "))).isEqualTo(1);
        assertThat(phone("created")).isEqualTo("13800138003");
    }

    @Test
    void actualValidatorAndImportListenerRejectInvalidRowsAndContinueWithValidRows() {
        seed("legacy", null); seed("current", "13800138000");
        var listener = new SysUserImportListener(users, passwords, true, 99L, user -> {
            var violations = validator.getValidator().validate(user);
            if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        });
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            listener.invoke(row("missing", null), null);
            listener.invoke(row("malformed", "12345"), null);
            listener.invoke(row("legacy", null), null);
            listener.invoke(row("current", " "), null);
            assertThat(phone("legacy")).isNull();
            assertThat(phone("current")).isEqualTo("13800138000");
            listener.invoke(row("current", null), null);
            listener.invoke(row("legacy", " 13800138001 "), null);
            listener.invoke(row("created", " 13800138002 "), null);
        }
        assertThatThrownBy(() -> listener.getExcelResult().getAnalysis()).hasMessageContaining("共 4 条")
            .hasMessageNotContaining("13800138000").hasMessageNotContaining("13800138001").hasMessageNotContaining("OwnedPass!9");
        assertThat(count("sys_user")).isEqualTo(3);
        assertThat(phone("legacy")).isEqualTo("13800138001");
        assertThat(phone("current")).isEqualTo("13800138000");
        assertThat(phone("created")).isEqualTo("13800138002");
    }

    private void seed(String name, String phone) {
        db.update("insert into sys_user(user_id,user_name,nick_name,phone_number) values(?,?,?,?)",
            name.equals("legacy") ? 41L : 42L, name, name, phone);
    }

    private SysUserBo update(String name, String phone) {
        var user = newUser(name, phone);
        user.setUserId(db.queryForObject("select user_id from sys_user where user_name=?", Long.class, name));
        user.setNickName("changed"); return user;
    }

    private static SysUserBo newUser(String name, String phone) {
        var user = new SysUserBo(); user.setUserName(name); user.setNickName(name); user.setPhoneNumber(phone); return user;
    }

    private static SysUserImportVo row(String name, String phone) {
        var row = new SysUserImportVo(); row.setUserName(name); row.setNickName(name); row.setPhoneNumber(phone); return row;
    }

    private static RegisterBody registrationBody(String name, String phone) {
        var body = new RegisterBody(); body.setUsername(name); body.setPassword("OwnedPass!9");
        body.setClientId("owned-phone"); body.setPhoneNumber(phone); return body;
    }

    private int count(String table) { return db.queryForObject("select count(*) from " + table, Integer.class); }
    private String phone(String name) { return db.queryForObject("select phone_number from sys_user where user_name=?", String.class, name); }

    @SuppressWarnings("unchecked")
    private static <T> T transactional(T target) {
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return (T) proxy.getProxy();
    }
}
