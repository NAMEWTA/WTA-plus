package org.namewta.test.profile.access;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.satoken.profile.ProfileAccess;
import org.namewta.common.satoken.profile.ProfileVerificationAdvisor;
import org.namewta.common.satoken.profile.annotation.RequireEnterpriseVerified;
import org.namewta.common.satoken.profile.annotation.RequirePersonVerified;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.CompositeProfileDisclosureService;
import org.namewta.profile.api.CompositeProfileService;
import org.namewta.profile.api.ProfileService;
import org.namewta.profile.api.domain.ProfileSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.enterprise.adapter.api.EnterpriseDisclosureContributor;
import org.namewta.profile.enterprise.adapter.api.EnterpriseProfileProjectionContributor;
import org.namewta.profile.enterprise.dao.EnterpriseApplicationDao;
import org.namewta.profile.enterprise.dao.EnterpriseDisclosureDao;
import org.namewta.profile.enterprise.mapper.EnterpriseApplicationMapper;
import org.namewta.profile.enterprise.mapper.EnterpriseDisclosureMapper;
import org.namewta.profile.enterprise.service.EnterpriseDisclosureService;
import org.namewta.profile.enterprise.service.EnterpriseProfileApiService;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseDisclosureUseCaseImpl;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseProfileApiUseCaseImpl;
import org.namewta.profile.person.adapter.api.PersonDisclosureContributor;
import org.namewta.profile.person.adapter.api.PersonProfileProjectionContributor;
import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.person.dao.PersonDisclosureDao;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.mapper.PersonDisclosureMapper;
import org.namewta.profile.person.service.PersonDisclosureService;
import org.namewta.profile.person.service.PersonProfileApiService;
import org.namewta.profile.person.usecase.impl.PersonDisclosureUseCaseImpl;
import org.namewta.profile.person.usecase.impl.PersonProfileApiUseCaseImpl;
import org.namewta.system.api.model.LoginUser;
import org.namewta.test.support.SqlBaselineScripts;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.namewta.profile.api.domain.ProfileDisclosureField.*;

/**
 * 真实 MySQL 与生产 API Adapter → UseCase → Service → DAO → Mapper → XML 查询链，
 * 在真实 SaToken 登录上下文和 Spring Advisor 代理前验证公共档案访问。
 * <p>只接受空的任务专属 namewta_profile_access_test_* 库，选取唯一 DDL 基座中的八张表；
 * 初始化失败及正常结束均只清理本夹具创建的表。密码通过 PROFILE_ACCESS_MYSQL_PASSWORD 注入。</p>
 * <p>认证、注销及企业转移后的数据状态由夹具直接构造，验证的是公共查询和门禁，
 * 不冒充完整认证 Workflow 或企业转移 UseCase 的端到端测试。</p>
 */
@Tag("dev")
@EnabledIfSystemProperty(named = "profile.access.mysql.integration", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("SaManager")
class ProfileAccessMySqlIntegrationTest {
    private static final long USER = 101L;
    private static final long UNBOUND_USER = 102L;
    private static final List<String> OWNED_TABLES = List.of(
        "profile_person", "profile_person_version", "profile_person_binding", "profile_person_application",
        "profile_enterprise", "profile_enterprise_version", "profile_enterprise_binding", "profile_enterprise_application");

    private final List<String> createdTables = new ArrayList<>();
    private final SelectCounter selects = new SelectCounter();
    private final AtomicInteger invoked = new AtomicInteger();
    private HikariDataSource pool;
    private DynamicRoutingDataSource routing;
    private JdbcTemplate db;
    private ProfileService profiles;
    private ProfileAccess access;
    private ProtectedActions actions;
    private Runnable restoreAuthentication;

    @BeforeAll
    void openOwnedFixture() throws Exception {
        try {
            String url = System.getProperty("profile.access.mysql.url");
            assertThat(url).as("An explicitly owned local MySQL URL is required")
                .startsWith("jdbc:mysql://127.0.0.1:");
            pool = new HikariDataSource();
            pool.setJdbcUrl(url);
            pool.setUsername(System.getProperty("profile.access.mysql.username", "root"));
            String password = System.getenv("PROFILE_ACCESS_MYSQL_PASSWORD");
            pool.setPassword(password != null ? password : System.getProperty("profile.access.mysql.password", ""));
            pool.setMaximumPoolSize(4);
            routing = new DynamicRoutingDataSource(List.of());
            routing.setPrimary("master");
            routing.addDataSource("master", pool);
            db = new JdbcTemplate(routing);
            assertThat(db.queryForObject("select database()", String.class))
                .matches("namewta_profile_access_test_[a-zA-Z0-9_]+");
            assertThat(db.queryForObject("select count(*) from information_schema.tables where table_schema = database()", Long.class))
                .as("Refuse to initialize or clean any nonempty database").isZero();
            for (String table : OWNED_TABLES) {
                String ddl = SqlBaselineScripts.createTable(table);
                // Include a possibly committed CREATE even if the connection fails while returning its result.
                createdTables.add(table);
                SqlBaselineScripts.execute(routing, ddl);
            }
            assembleProductionQueries();
            installOwnedAuthentication();
            var factory = new DefaultListableBeanFactory();
            factory.registerSingleton("profileAccess", access);
            var proxy = new ProxyFactory(new ProtectedActions(invoked));
            proxy.setProxyTargetClass(true);
            proxy.addAdvisor(new ProfileVerificationAdvisor(factory.getBeanProvider(ProfileAccess.class)));
            actions = (ProtectedActions) proxy.getProxy();
        } catch (Exception | AssertionError failure) {
            try {
                closeOwnedFixture();
            } catch (Exception cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private void assembleProductionQueries() throws Exception {
        var configuration = new MybatisConfiguration(
            new Environment("profile-access-owned", new SpringManagedTransactionFactory(), routing));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addInterceptor(selects);
        for (Class<?> mapper : List.of(PersonApplicationMapper.class, PersonDisclosureMapper.class,
            EnterpriseApplicationMapper.class, EnterpriseDisclosureMapper.class)) {
            configuration.addMapper(mapper);
        }
        for (String xml : List.of("/mapper/person/PersonApplicationMapper.xml", "/mapper/person/PersonDisclosureMapper.xml",
            "/mapper/enterprise/EnterpriseApplicationMapper.xml", "/mapper/enterprise/EnterpriseDisclosureMapper.xml")) {
            try (var input = getClass().getResourceAsStream(xml)) {
                assertThat(input).as("Production Mapper XML: %s", xml).isNotNull();
                new XMLMapperBuilder(input, configuration, xml, configuration.getSqlFragments()).parse();
            }
        }
        var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
        profiles = new CompositeProfileService(List.of(
            new PersonProfileProjectionContributor(transactional(new PersonProfileApiUseCaseImpl(
                new PersonProfileApiService(new PersonApplicationDao(sessions.getMapper(PersonApplicationMapper.class)))))),
            new EnterpriseProfileProjectionContributor(transactional(new EnterpriseProfileApiUseCaseImpl(
                new EnterpriseProfileApiService(new EnterpriseApplicationDao(sessions.getMapper(EnterpriseApplicationMapper.class))))))));
        var disclosures = new CompositeProfileDisclosureService(List.of(
            new PersonDisclosureContributor(new PersonDisclosureUseCaseImpl(new PersonDisclosureService(
                new PersonDisclosureDao(sessions.getMapper(PersonDisclosureMapper.class))))),
            new EnterpriseDisclosureContributor(new EnterpriseDisclosureUseCaseImpl(new EnterpriseDisclosureService(
                new EnterpriseDisclosureDao(sessions.getMapper(EnterpriseDisclosureMapper.class)))))));
        access = new ProfileAccess(profiles, disclosures);
    }

    private void installOwnedAuthentication() {
        var previousConfig = SaManager.getConfig();
        var previousContext = SaManager.getSaTokenContext();
        var previousLogic = StpUtil.getStpLogic();
        var previousDao = SaManager.getSaTokenDao();
        var previousRequest = RequestContextHolder.getRequestAttributes();
        var ownedDao = new SaTokenDaoDefaultImpl();
        restoreAuthentication = () -> {
            ownedDao.destroy();
            SaManager.setSaTokenDao(previousDao);
            SaManager.setConfig(previousConfig);
            SaManager.setSaTokenContext(previousContext);
            StpUtil.setStpLogic(previousLogic);
            RequestContextHolder.setRequestAttributes(previousRequest);
        };
        SaManager.setConfig(new SaTokenConfig().setTokenName("Authorization").setTokenPrefix("Bearer")
            .setJwtSecretKey("owned-profile-access-fixture-only-at-least-32-characters"));
        SaManager.setSaTokenDao(ownedDao);
        SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
        StpUtil.setStpLogic(new StpLogicJwtForSimple());
    }

    @BeforeEach
    void seedPublishedBindings() {
        assertThat(createdTables).containsExactlyElementsOf(OWNED_TABLES);
        for (String table : OWNED_TABLES.reversed()) {
            db.update("delete from " + table);
        }
        seedCertifiedUser(USER);
        login(USER, 11L, "home-web");
        invoked.set(0);
        selects.reset();
    }

    @AfterAll
    void closeOwnedFixture() throws Exception {
        Exception failure = null;
        for (String table : List.copyOf(createdTables).reversed()) {
            try {
                db.execute("drop table if exists " + table);
                createdTables.remove(table);
            } catch (Exception cleanupFailure) {
                failure = accumulate(failure, cleanupFailure);
            }
        }
        if (restoreAuthentication != null) {
            try {
                restoreAuthentication.run();
            } catch (Exception cleanupFailure) {
                failure = accumulate(failure, cleanupFailure);
            } finally {
                restoreAuthentication = null;
            }
        }
        try {
            if (routing != null) routing.destroy();
        } catch (Exception cleanupFailure) {
            failure = accumulate(failure, cleanupFailure);
        } finally {
            routing = null;
            try {
                if (pool != null) pool.close();
            } catch (Exception cleanupFailure) {
                failure = accumulate(failure, cleanupFailure);
            }
            pool = null;
        }
        if (failure != null) throw failure;
    }

    @Test
    void verifiedAndUnboundSubjectsUseTrustedLoginAndRemainConsistentAcrossClients() {
        ProfileSummary home = access.requireVerified(ProfileType.ENTERPRISE, ProfileType.PERSON, ProfileType.PERSON);
        assertVerified(home, USER);
        assertThat(access.isPersonVerified()).isTrue();
        assertThat(access.isEnterpriseVerified()).isTrue();
        actions.both();
        assertThat(invoked).hasValue(1);

        login(USER, 22L, "admin-web");
        assertThat(access.currentSummary()).isEqualTo(home);
        assertThat(StpUtil.getExtra(LoginHelper.CLIENT_KEY)).isEqualTo("owned-oauth-client-22");
        assertThat(LoginHelper.getLoginUser().getClientKey()).isEqualTo("admin-web");

        login(UNBOUND_USER, 11L, "home-web");
        assertThat(access.currentSummary()).isEqualTo(ProfileSummary.unverified(UNBOUND_USER));
        assertThat(access.isPersonVerified()).isFalse();
        assertThat(access.isEnterpriseVerified()).isFalse();
        var missing = access.currentDisclosure(Set.of(PERSON_VERIFIED, PERSON_FULL_NAME, ENTERPRISE_VERIFIED, ENTERPRISE_NAME));
        assertThat(missing.person().verified()).isFalse();
        assertThat(missing.person().fullName()).isNull();
        assertThat(missing.enterprise().verified()).isFalse();
        assertThat(missing.enterprise().name()).isNull();
        assertDenied(actions::both, "PROFILE_VERIFICATION_REQUIRED", List.of("PERSON", "ENTERPRISE"),
            List.of("PERSON", "ENTERPRISE"));
        assertThat(invoked).hasValue(1);
    }

    @Test
    void pendingDraftsCannotReplaceTheCertifiedVersionOrDisableExistingCertification() {
        db.update("insert into profile_person_application(person_application_id, applicant_user_id, status, provider_code, full_name) "
            + "values (?, ?, 'DRAFT', 'owned-fixture', '草稿姓名')", 8001L, USER);
        db.update("insert into profile_enterprise_application(enterprise_application_id, applicant_user_id, status, provider_code, enterprise_name) "
            + "values (?, ?, 'DRAFT', 'owned-fixture', '草稿企业')", 8002L, USER);
        assertVerified(access.currentSummary(), USER);
        var disclosure = access.currentDisclosure(Set.of(PERSON_FULL_NAME, ENTERPRISE_NAME));
        assertThat(disclosure.person().fullName()).isEqualTo("已认证姓名-" + USER);
        assertThat(disclosure.enterprise().name()).isEqualTo("已认证企业-" + USER);
        actions.both();
        assertThat(invoked).hasValue(1);
    }

    @ParameterizedTest(name = "{0} is not a current verified binding")
    @ValueSource(strings = {"SUSPENDED", "UNBOUND", "REVOKED"})
    void inactiveBindingsAndRevokedProfilesFailBothAccessAndAnnotations(String state) {
        if ("REVOKED".equals(state)) {
            db.update("update profile_person set status = 'REVOKED' where person_profile_id = ?", personId(USER));
            db.update("update profile_enterprise set status = 'REVOKED' where enterprise_profile_id = ?", enterpriseId(USER));
        } else {
            db.update("update profile_person_binding set status = ? where user_id = ?", state, USER);
            db.update("update profile_enterprise_binding set status = ? where user_id = ?", state, USER);
        }
        assertThat(access.currentSummary()).isEqualTo(ProfileSummary.unverified(USER));
        assertDenied(actions::person, "PERSON_VERIFICATION_REQUIRED", List.of("PERSON"), List.of("PERSON"));
        assertDenied(actions::enterprise, "ENTERPRISE_VERIFICATION_REQUIRED", List.of("ENTERPRISE"), List.of("ENTERPRISE"));
        assertDenied(actions::both, "PROFILE_VERIFICATION_REQUIRED", List.of("PERSON", "ENTERPRISE"),
            List.of("PERSON", "ENTERPRISE"));
        assertThat(invoked).hasValue(0);
        var disclosure = access.currentDisclosure(Set.of(PERSON_VERIFIED, PERSON_FULL_NAME, ENTERPRISE_VERIFIED, ENTERPRISE_NAME));
        assertThat(disclosure.person().verified()).isFalse();
        assertThat(disclosure.person().fullName()).isNull();
        assertThat(disclosure.enterprise().verified()).isFalse();
        assertThat(disclosure.enterprise().name()).isNull();
    }

    @Test
    void transferResultIsVisibleOnlyToTheNewEnterpriseOwner() {
        long newOwner = 103L;
        // Construct the committed transfer result; this is deliberately not a transfer Workflow E2E.
        db.update("update profile_enterprise_binding set status = 'UNBOUND', unbound_time = now() where user_id = ?", USER);
        db.update("insert into profile_enterprise_binding(enterprise_binding_id, enterprise_profile_id, user_id, status, bound_time) "
            + "values (?, ?, ?, 'ACTIVE', '2026-01-02 00:00:00')", newOwner * 10 + 6, enterpriseId(USER), newOwner);
        var oldOwner = access.currentSummary();
        assertThat(oldOwner.personVerified()).isTrue();
        assertThat(oldOwner.enterpriseVerified()).isFalse();
        assertDenied(actions::enterprise, "ENTERPRISE_VERIFICATION_REQUIRED", List.of("ENTERPRISE"), List.of("ENTERPRISE"));
        assertThat(access.currentDisclosure(Set.of(ENTERPRISE_NAME)).enterprise().name()).isNull();

        login(newOwner, 11L, "home-web");
        var newSummary = access.requireVerified(ProfileType.ENTERPRISE);
        assertThat(newSummary.personVerified()).isFalse();
        assertThat(newSummary.enterprise().profileId()).isEqualTo(enterpriseId(USER));
        assertThat(access.currentDisclosure(Set.of(ENTERPRISE_NAME)).enterprise().name()).isEqualTo("已认证企业-" + USER);
        actions.enterprise();
        assertThat(invoked).hasValue(1);
        assertDenied(actions::both, "PERSON_VERIFICATION_REQUIRED", List.of("PERSON", "ENTERPRISE"), List.of("PERSON"));
    }

    @Test
    void disclosureWhitelistControlsBothReturnedFieldsAndActualSelectedColumns() {
        var masked = access.currentDisclosure(Set.of(PERSON_FULL_NAME, PERSON_DOCUMENT_NUMBER_MASKED,
            ENTERPRISE_NAME, ENTERPRISE_LEGAL_DOCUMENT_NUMBER_MASKED));
        assertThat(masked.userId()).isEqualTo(USER);
        assertThat(masked.person().fullName()).isEqualTo("已认证姓名-" + USER);
        assertThat(masked.person().documentNumberMasked()).isEqualTo(mask(document(USER)));
        assertThat(masked.person().documentNumber()).isNull();
        assertThat(masked.person().profileId()).isNull();
        assertThat(masked.person().verified()).isNull();
        assertThat(masked.person()).hasAllNullFieldsOrPropertiesExcept("fullName", "documentNumberMasked");
        assertThat(masked.enterprise().legalDocumentNumberMasked()).isEqualTo(mask(document(USER + 5000)));
        assertThat(masked.enterprise().legalDocumentNumber()).isNull();
        assertThat(masked.enterprise().contactPhone()).isNull();
        assertThat(masked.enterprise().email()).isNull();
        assertThat(masked.enterprise()).hasAllNullFieldsOrPropertiesExcept("name", "legalDocumentNumberMasked");
        assertThat(selects.statements).hasSize(2);
        assertThat(selects.statements.get(0)).contains("null as document_number ,");
        assertThat(selects.statements.get(1)).contains("null as legal_document_number ,", "null as contact_phone ,");

        selects.reset();
        var personOnly = access.currentDisclosure(Set.of(PERSON_VERIFIED));
        assertThat(personOnly.person().verified()).isTrue();
        assertThat(personOnly.person().fullName()).isNull();
        assertThat(personOnly.enterprise()).isNull();
        assertThat(selects.statements).hasSize(1);

        selects.reset();
        var none = access.currentDisclosure(Set.of());
        assertThat(none.person()).isNull();
        assertThat(none.enterprise()).isNull();
        assertThat(selects.statements).isEmpty();

        var full = access.currentDisclosure(Set.of(PERSON_DOCUMENT_NUMBER, ENTERPRISE_LEGAL_DOCUMENT_NUMBER));
        assertThat(full.person().documentNumber()).isEqualTo(document(USER));
        assertThat(full.person().documentNumberMasked()).isNull();
        assertThat(full.person().fullName()).isNull();
        assertThat(full.enterprise().legalDocumentNumber()).isEqualTo(document(USER + 5000));
        assertThat(full.enterprise().legalDocumentNumberMasked()).isNull();
        assertThat(full.enterprise().name()).isNull();
    }

    @Test
    void oneHundredUsersUseTwoRealSelectsRatherThanOneQueryPerUser() {
        List<Long> users = LongStream.range(1000, 1100).boxed().toList();
        users.forEach(this::seedCertifiedUser);
        List<Long> requested = new ArrayList<>(users);
        requested.add(users.getFirst());
        requested.add(UNBOUND_USER);
        selects.reset();
        var summaries = profiles.findByUserIds(requested);
        assertThat(summaries).hasSize(101);
        users.forEach(userId -> assertVerified(summaries.get(userId), userId));
        assertThat(summaries.get(UNBOUND_USER)).isEqualTo(ProfileSummary.unverified(UNBOUND_USER));
        assertThat(selects.statements).as("Actual JDBC SELECT prepares across both production contributors").hasSize(2);
        assertThat(selects.statements).allSatisfy(sql -> assertThat(sql).contains("b.user_id in"));
        selects.reset();
        assertThat(profiles.findByUserIds(List.of())).isEmpty();
        assertThat(selects.statements).isEmpty();
    }

    @Test
    void revocationTakesEffectOnTheNextQueryWithoutRefreshingTheLoginSession() {
        actions.both();
        String sameToken = StpUtil.getTokenValue();
        // Simulate a committed revocation by another operation, keeping this request and token intact.
        db.update("update profile_person set status = 'REVOKED' where person_profile_id = ?", personId(USER));
        selects.reset();
        var updated = access.currentSummary();
        assertThat(updated.personVerified()).isFalse();
        assertThat(updated.enterpriseVerified()).isTrue();
        assertThat(selects.statements).hasSize(2);
        assertDenied(actions::both, "PERSON_VERIFICATION_REQUIRED", List.of("PERSON", "ENTERPRISE"), List.of("PERSON"));
        assertThat(invoked).hasValue(1);
        assertThat(StpUtil.getTokenValue()).isEqualTo(sameToken);
        assertThat(access.currentDisclosure(Set.of(PERSON_VERIFIED)).person().verified()).isFalse();
    }

    @Test
    void inconsistentSessionIdentityIsRejectedBeforeAnyProfileQuery() {
        LoginUser user = LoginHelper.getLoginUser();
        user.setClientPk(99L);
        StpUtil.getTokenSession().set(LoginHelper.LOGIN_USER_KEY, user);
        selects.reset();
        ServiceException failure = catchThrowableOfType(ServiceException.class, access::currentSummary);
        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo(401);
        assertThat(failure.getData()).isEqualTo(Map.of("reason", "PROFILE_LOGIN_CONTEXT_INVALID"));
        assertThat(selects.statements).isEmpty();
    }

    private void login(long userId, long clientPk, String appKey) {
        var request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "OwnedProfileFixture/1.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
        var user = new LoginUser();
        user.setUserId(userId);
        user.setUsername("owned-profile-" + userId);
        user.setUserType("sys_user");
        user.setClientPk(clientPk);
        user.setClientKey(appKey);
        user.setDeptId(1L);
        user.setIpaddr("127.0.0.1");
        user.setLoginLocation("owned fixture");
        user.setMenuPermission(Set.of());
        user.setRolePermission(Set.of());
        LoginHelper.login(user, new SaLoginParameter().setTimeout(300)
            .setExtra(LoginHelper.CLIENT_PK_KEY, clientPk)
            .setExtra(LoginHelper.CLIENT_KEY, "owned-oauth-client-" + clientPk));
        assertThat(StpUtil.getLoginIdAsString()).isEqualTo(user.getLoginId());
        assertThat(StpUtil.getExtra(LoginHelper.USER_KEY).toString()).isEqualTo(Long.toString(userId));
        assertThat(StpUtil.getExtra(LoginHelper.CLIENT_PK_KEY).toString()).isEqualTo(Long.toString(clientPk));
        assertThat(StpUtil.getExtra(LoginHelper.USER_TYPE_KEY)).isEqualTo(user.getUserType());
    }

    private void seedCertifiedUser(long userId) {
        db.update("""
            insert into profile_person(person_profile_id, current_version_id, full_name, document_type_code,
                document_number, identity_key, gender, birth_date, status)
            values (?, ?, '主档影子姓名', 'ID_CARD', ?, ?, '1', '1990-01-01', 'ACTIVE')
            """, personId(userId), userId * 10 + 2, document(userId), "ID_CARD:" + document(userId));
        db.update("""
            insert into profile_person_version(person_version_id, person_profile_id, version_no, source_type, source_id,
                full_name, document_type_code, document_number, identity_key, gender, birth_date, status, published_time)
            values (?, ?, 1, 'USER_SUBMISSION', ?, ?, 'ID_CARD', ?, ?, '1', '1990-01-01', 'CURRENT', '2026-01-01 00:00:00')
            """, userId * 10 + 2, personId(userId), userId, "已认证姓名-" + userId, document(userId), "ID_CARD:" + document(userId));
        db.update("""
            insert into profile_person_binding(person_binding_id, person_profile_id, user_id, status, bound_time)
            values (?, ?, ?, 'ACTIVE', '2026-01-01 00:00:00')
            """, userId * 10 + 3, personId(userId), userId);
        db.update("""
            insert into profile_enterprise(enterprise_profile_id, current_version_id, enterprise_name, unified_credit_code,
                enterprise_type, legal_representative_name, legal_document_type_code, legal_document_number,
                established_date, registered_address, business_scope, status)
            values (?, ?, '主档影子企业', ?, 'COMPANY', '认证法人', 'ID_CARD', ?, '2020-01-01', '测试地址', '测试范围', 'ACTIVE')
            """, enterpriseId(userId), userId * 10 + 5, "OWNED-" + userId, document(userId + 5000));
        db.update("""
            insert into profile_enterprise_version(enterprise_version_id, enterprise_profile_id, version_no, source_type,
                source_id, enterprise_name, unified_credit_code, enterprise_type, legal_representative_name,
                legal_document_type_code, legal_document_number, established_date, registered_address, business_scope,
                contact_phone, email, status, published_time)
            values (?, ?, 1, 'USER_SUBMISSION', ?, ?, ?, 'COMPANY', '认证法人', 'ID_CARD', ?, '2020-01-01',
                '测试地址', '测试范围', '15800001001', 'owned@example.test', 'CURRENT', '2026-01-01 00:00:00')
            """, userId * 10 + 5, enterpriseId(userId), userId, "已认证企业-" + userId,
            "OWNED-" + userId, document(userId + 5000));
        db.update("""
            insert into profile_enterprise_binding(enterprise_binding_id, enterprise_profile_id, user_id, status, bound_time)
            values (?, ?, ?, 'ACTIVE', '2026-01-01 00:00:00')
            """, userId * 10 + 6, enterpriseId(userId), userId);
    }

    private static void assertVerified(ProfileSummary summary, long userId) {
        assertThat(summary.userId()).isEqualTo(userId);
        assertThat(summary.personVerified()).isTrue();
        assertThat(summary.enterpriseVerified()).isTrue();
        assertThat(summary.person().profileId()).isEqualTo(personId(userId));
        assertThat(summary.enterprise().profileId()).isEqualTo(enterpriseId(userId));
        assertThat(summary.person().verifiedAt()).isNotNull();
        assertThat(summary.enterprise().verifiedAt()).isNotNull();
    }

    private static void assertDenied(Runnable invocation, String reason, List<String> required, List<String> missing) {
        ServiceException failure = catchThrowableOfType(ServiceException.class, invocation::run);
        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo(403);
        assertThat(failure.getData()).isEqualTo(Map.of("reason", reason, "requiredTypes", required, "missingTypes", missing));
    }

    private static long personId(long userId) { return userId * 10 + 1; }
    private static long enterpriseId(long userId) { return userId * 10 + 4; }
    private static String document(long userId) { return Long.toString(110101199000000000L + userId); }
    private static String mask(String value) { return "*".repeat(value.length() - 4) + value.substring(value.length() - 4); }

    private static Exception accumulate(Exception previous, Exception next) {
        if (previous == null) return next;
        previous.addSuppressed(next);
        return previous;
    }

    @SuppressWarnings("unchecked")
    private static <T> T transactional(T target) {
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return (T) proxy.getProxy();
    }

    public static class ProtectedActions {
        private final AtomicInteger invocations;

        public ProtectedActions(AtomicInteger invocations) { this.invocations = invocations; }

        @RequirePersonVerified
        public void person() { invocations.incrementAndGet(); }

        @RequireEnterpriseVerified
        public void enterprise() { invocations.incrementAndGet(); }

        @RequirePersonVerified
        @RequireEnterpriseVerified
        public void both() { invocations.incrementAndGet(); }
    }

    /** Observe genuine JDBC prepares without replacing Mapper SQL or query results. */
    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
    public static class SelectCounter implements Interceptor {
        private final List<String> statements = new ArrayList<>();

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            var statement = (StatementHandler) invocation.getTarget();
            String sql = statement.getBoundSql().getSql().replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
            if (sql.startsWith("select ")) statements.add(sql);
            return invocation.proceed();
        }

        void reset() { statements.clear(); }
    }
}
