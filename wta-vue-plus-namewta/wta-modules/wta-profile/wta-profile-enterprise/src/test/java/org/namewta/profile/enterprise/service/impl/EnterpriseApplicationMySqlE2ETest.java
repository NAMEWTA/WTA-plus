package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.service.impl.EnterpriseVerificationSecurityAuditRecorder;

import org.namewta.profile.enterprise.adapter.codec.EnterpriseVerificationEvidenceCodec;

import org.namewta.profile.enterprise.service.EnterpriseVerificationAttemptService;

import org.namewta.profile.enterprise.adapter.provider.EnterpriseVerificationProviderRegistry;
import org.namewta.profile.enterprise.adapter.provider.EnterpriseManualVerificationProvider;

import org.namewta.profile.enterprise.controller.advice.EnterpriseApplicationExceptionHandler;
import org.namewta.profile.enterprise.controller.self.EnterpriseApplicationController;
import org.namewta.profile.enterprise.domain.exception.EnterpriseApplicationException;
import org.namewta.profile.enterprise.port.gateway.EnterpriseWorkflowGateway;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.enterprise.mapper.EnterpriseApplicationMapper;
import org.namewta.profile.enterprise.config.EnterpriseVerificationProviderProperties;
import org.namewta.profile.enterprise.mapper.EnterpriseVerificationAttemptMapper;
import org.namewta.profile.enterprise.dao.EnterpriseVerificationAttemptDao;
import org.namewta.profile.enterprise.support.EnterpriseMapperXmlTestSupport;
import org.namewta.system.api.ConfigService;
import org.namewta.workflow.api.event.ProcessEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "PROFILE_MYSQL_E2E_URL", matches = ".+")
class EnterpriseApplicationMySqlE2ETest {

    private static final Instant NOW = Instant.parse("2026-09-01T14:00:00Z");

    @Test
    void publishesExactlyOneResponsibleThenAllowsUnboundReauthenticationAndRevokedSuccessor() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            AtomicLong currentUser = new AtomicLong(970000000001L);
            Fixture fixture = fixture(session);
            try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
                login.when(LoginHelper::getUserId).thenAnswer(ignored -> currentUser.get());

                long firstApplication = apply(fixture, credit(1), true);
                finish(fixture.service(), firstApplication, 1);
                session.commit();
                long profileId = Long.parseLong(scalar(session,
                    "select enterprise_profile_id from profile_enterprise"
                        + " where unified_credit_code='" + credit(1) + "' and status='ACTIVE'"));
                assertThat(scalar(session, "select count(*) from profile_enterprise_binding"
                    + " where enterprise_profile_id=" + profileId + " and user_id=" + currentUser.get()
                    + " and status='ACTIVE'")).isEqualTo("1");
                assertThat(scalar(session, "select email from profile_enterprise where enterprise_profile_id="
                    + profileId)).isEqualTo("ops@example.com");
                assertThat(scalar(session, "select registered_capital from profile_enterprise_version"
                    + " where enterprise_profile_id=" + profileId + " and status='CURRENT'"))
                    .isEqualTo("1000000.00");

                currentUser.set(970000000002L);
                save(fixture, credit(1), false);
                fixture.mvc().perform(post("/profile/enterprise/application/submit")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.msg").value("ENTERPRISE_RESPONSIBLE_ALREADY_BOUND"));
                session.rollback();

                execute(session, "update profile_enterprise_binding set status='UNBOUND',"
                    + " unbound_time=current_timestamp, binding_version=binding_version+1"
                    + " where enterprise_profile_id=" + profileId + " and status='ACTIVE'");
                session.commit();
                long reauthentication = apply(fixture, credit(1), false);
                finish(fixture.service(), reauthentication, 1);
                session.commit();
                assertThat(scalar(session, "select count(*) from profile_enterprise"
                    + " where unified_credit_code='" + credit(1) + "'")).isEqualTo("1");
                assertThat(scalar(session, "select count(*) from profile_enterprise_version"
                    + " where enterprise_profile_id=" + profileId)).isEqualTo("2");
                assertThat(scalar(session, "select count(*) from profile_enterprise_binding"
                    + " where enterprise_profile_id=" + profileId + " and user_id=" + currentUser.get()
                    + " and status='ACTIVE'")).isEqualTo("1");

                execute(session, "update profile_enterprise_binding set status='UNBOUND',"
                    + " unbound_time=current_timestamp, binding_version=binding_version+1"
                    + " where enterprise_profile_id=" + profileId + " and status='ACTIVE'");
                execute(session, "update profile_enterprise set status='REVOKED', revoked_time=current_timestamp,"
                    + " revoked_reason='e2e', version=version+1 where enterprise_profile_id=" + profileId);
                session.commit();

                currentUser.set(970000000003L);
                long successor = apply(fixture, credit(1), true);
                finish(fixture.service(), successor, 1);
                session.commit();
                assertThat(scalar(session, "select count(*) from profile_enterprise"
                    + " where unified_credit_code='" + credit(1) + "'")).isEqualTo("2");
                assertThat(scalar(session, "select previous_profile_id from profile_enterprise"
                    + " where unified_credit_code='" + credit(1) + "' and status='ACTIVE'"))
                    .isEqualTo(String.valueOf(profileId));
            }
        }
    }

    @Test
    void rollsBackTheSubmissionSnapshotWhenWorkflowCannotStart() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            AtomicLong currentUser = new AtomicLong(970000000011L);
            Fixture fixture = fixture(session);
            try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
                login.when(LoginHelper::getUserId).thenAnswer(ignored -> currentUser.get());
                long applicationId = save(fixture, credit(11), true);
                session.commit();
                fixture.workflow().fail = true;

                fixture.mvc().perform(post("/profile/enterprise/application/submit")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.msg").value("ENTERPRISE_WORKFLOW_UNAVAILABLE"));
                session.rollback();

                assertThat(scalar(session, "select status from profile_enterprise_application"
                    + " where enterprise_application_id=" + applicationId)).isEqualTo("DRAFT");
                assertThat(scalar(session, "select count(*) from profile_enterprise_submission"
                    + " where enterprise_application_id=" + applicationId)).isEqualTo("0");
                assertThat(scalar(session, "select count(*) from profile_verification_attempt"
                    + " where profile_type='ENTERPRISE' and application_id=" + applicationId)).isEqualTo("0");
            }
        }
    }

    @Test
    void databaseAllowsOnlyOneOpenEnterpriseApplicationPerIdentityAndAccount() throws Exception {
        UnpooledDataSource dataSource = dataSource();
        List<InsertOutcome> identityRace = race(dataSource,
            new OpenApplication(980000000001L, 980000000101L, credit(21)),
            new OpenApplication(980000000002L, 980000000102L, credit(21)));
        assertUniqueConflict(identityRace);
        List<InsertOutcome> accountRace = race(dataSource,
            new OpenApplication(980000000003L, 980000000103L, credit(22)),
            new OpenApplication(980000000004L, 980000000103L, credit(23)));
        assertUniqueConflict(accountRace);
    }

    private long apply(Fixture fixture, String creditCode, boolean legalHandler) throws Exception {
        long applicationId = save(fixture, creditCode, legalHandler);
        fixture.mvc().perform(post("/profile/enterprise/application/submit")
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("WAITING"));
        fixture.session().commit();
        assertThat(fixture.workflow().applicationId).isEqualTo(applicationId);
        return applicationId;
    }

    private long save(Fixture fixture, String creditCode, boolean legalHandler) throws Exception {
        fixture.mvc().perform(post("/profile/enterprise/application")
                .contentType(MediaType.APPLICATION_JSON).content(draft(creditCode, legalHandler)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DRAFT"));
        return Long.parseLong(scalar(fixture.session(),
            "select enterprise_application_id from profile_enterprise_application"
                + " where applicant_user_id=" + LoginHelper.getUserId()
                + " and status in ('DRAFT','BACK','CANCEL','WAITING')"));
    }

    private String draft(String creditCode, boolean legalHandler) {
        return """
            {"enterpriseName":"示例企业","unifiedCreditCode":"%s","enterpriseType":"COMPANY",
             "legalRepresentativeName":"张法","legalDocumentTypeCode":"CN_RESIDENT_ID",
             "legalDocumentNumber":"110101199001011234","handlerIsLegalRepresentative":%s,
             "establishedDate":"2010-01-01","businessTermFrom":"2010-01-01",
             "businessTermUntil":"2035-01-01","registeredAddress":"上海市示例路1号",
             "businessScope":"软件服务","contactName":"李联","contactPhone":"13800138000",
             "email":"OPS@EXAMPLE.COM","registeredCapital":1000000.00,
             "industryCode":"SOFTWARE","website":"https://example.com","expectedVersion":0}
            """.formatted(creditCode, legalHandler);
    }

    private void finish(EnterpriseApplicationServiceImpl service, long applicationId, int snapshotVersion) {
        ProcessEvent event = new ProcessEvent();
        event.setFlowCode("profile_enterprise_verification");
        event.setBusinessId(String.valueOf(applicationId));
        event.setStatus("finish");
        event.setParams(Map.of("snapshotVersion", snapshotVersion));
        service.handleProcessEvent(event);
    }

    private Fixture fixture(SqlSession session) {
        JsonMapper json = JsonMapper.builder().build();
        EnterpriseApplicationMapper applicationMapper = session.getMapper(EnterpriseApplicationMapper.class);
        EnterpriseVerificationAttemptMapper attemptMapper =
            session.getMapper(EnterpriseVerificationAttemptMapper.class);
        EnterpriseVerificationEvidenceCodec evidenceCodec = new EnterpriseVerificationEvidenceCodec(json);
        EnterpriseVerificationProviderProperties properties = new EnterpriseVerificationProviderProperties();
        properties.setEnabledProviders(java.util.Set.of("manual"));
        EnterpriseVerificationProviderRegistry providers = new EnterpriseVerificationProviderRegistry(
            List.of(new EnterpriseManualVerificationProvider()), properties);
        EnterpriseVerificationAttemptService attempts = new EnterpriseVerificationAttemptService(
            providers, new EnterpriseVerificationAttemptDao(attemptMapper), evidenceCodec,
            new EnterpriseVerificationSecurityAuditRecorder(new EnterpriseVerificationAttemptDao(attemptMapper)));
        ConfigService config = mock(ConfigService.class);
        when(config.getConfigValue("profile.enterprise.provider.default")).thenReturn("manual");
        when(config.getConfigValue("profile.enterprise.flowCode")).thenReturn("profile_enterprise_verification");
        ProfileMaterialPort materials = mock(ProfileMaterialPort.class);
        when(materials.snapshotImmutable(any(), any())).thenReturn(List.of());
        RecordingWorkflowGateway workflow = new RecordingWorkflowGateway();
        EnterpriseApplicationServiceImpl service = new EnterpriseApplicationServiceImpl(
            applicationMapper, json, materials, providers, attempts, workflow, config,
            Clock.fixed(NOW, ZoneOffset.UTC));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new EnterpriseApplicationController(service))
            .setControllerAdvice(new EnterpriseApplicationExceptionHandler()).build();
        return new Fixture(session, service, workflow, mvc);
    }

    private SqlSessionFactory sessionFactory() {
        Configuration configuration = new Configuration(new Environment(
            "profile-enterprise-application-e2e", new JdbcTransactionFactory(), dataSource()));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(EnterpriseApplicationMapper.class);
        configuration.addMapper(EnterpriseVerificationAttemptMapper.class);
        EnterpriseMapperXmlTestSupport.parse(configuration, EnterpriseApplicationMapper.class);
        EnterpriseMapperXmlTestSupport.parse(configuration, EnterpriseVerificationAttemptMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private UnpooledDataSource dataSource() {
        return new UnpooledDataSource("com.mysql.cj.jdbc.Driver", System.getenv("PROFILE_MYSQL_E2E_URL"),
            System.getenv("PROFILE_MYSQL_E2E_USERNAME"), System.getenv("PROFILE_MYSQL_E2E_PASSWORD"));
    }

    private List<InsertOutcome> race(UnpooledDataSource dataSource,
                                     OpenApplication first, OpenApplication second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<InsertOutcome> a = executor.submit(() -> insert(dataSource, first, ready, start));
            Future<InsertOutcome> b = executor.submit(() -> insert(dataSource, second, ready, start));
            ready.await();
            start.countDown();
            return List.of(a.get(), b.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private InsertOutcome insert(UnpooledDataSource dataSource, OpenApplication value,
                                 CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        String sql = """
            insert into profile_enterprise_application (
                enterprise_application_id, applicant_user_id, status, enterprise_name,
                unified_credit_code, identity_key, enterprise_type, legal_representative_name,
                legal_document_type_code, legal_document_number, handler_is_legal_representative,
                established_date, registered_address, business_scope, provider_code, submission_seq,
                decision_version, version, create_dept, create_time, create_by,
                update_time, update_by, del_flag
            ) values (?, ?, 'WAITING', '并发测试', ?, ?, 'COMPANY', '张法',
                      'CN_RESIDENT_ID', '110101199001011234', 'Y', '2010-01-01',
                      '上海', '软件', 'manual', 1, 0, 0, -1,
                      current_timestamp, ?, current_timestamp, ?, '0')
            """;
        try (var connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, value.applicationId());
            statement.setLong(2, value.userId());
            statement.setString(3, value.creditCode());
            statement.setString(4, value.creditCode());
            statement.setLong(5, value.userId());
            statement.setLong(6, value.userId());
            statement.executeUpdate();
            return new InsertOutcome(true, null);
        } catch (SQLException exception) {
            return new InsertOutcome(false, exception.getSQLState());
        }
    }

    private void assertUniqueConflict(List<InsertOutcome> outcomes) {
        assertThat(outcomes).filteredOn(InsertOutcome::success).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.success()).singleElement()
            .extracting(InsertOutcome::sqlState).asString().startsWith("23");
    }

    private String credit(int suffix) {
        return "91310000ABCDEF" + String.format("%04d", suffix);
    }

    private void execute(SqlSession session, String sql) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private String scalar(SqlSession session, String sql) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getString(1);
        }
    }

    private record Fixture(SqlSession session, EnterpriseApplicationServiceImpl service,
                           RecordingWorkflowGateway workflow, MockMvc mvc) {
    }

    private record OpenApplication(long applicationId, long userId, String creditCode) {
    }

    private record InsertOutcome(boolean success, String sqlState) {
    }

    private static final class RecordingWorkflowGateway implements EnterpriseWorkflowGateway {
        private long applicationId;
        private boolean fail;

        @Override
        public void start(long applicationId, long submissionId, int snapshotVersion) {
            if (fail) {
                throw new EnterpriseApplicationException("ENTERPRISE_WORKFLOW_UNAVAILABLE");
            }
            this.applicationId = applicationId;
        }
    }
}
