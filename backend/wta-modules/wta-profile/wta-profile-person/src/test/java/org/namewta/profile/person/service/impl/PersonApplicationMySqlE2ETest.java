package org.namewta.profile.person.service.impl;


import org.namewta.profile.person.adapter.provider.PersonManualVerificationProvider;

import org.namewta.profile.person.adapter.codec.PersonVerificationEvidenceCodec;

import org.namewta.profile.person.adapter.api.PersonProfileMaterialOwnerContributor;
import org.namewta.profile.person.adapter.provider.PersonManualVerificationProvider;
import org.namewta.profile.person.usecase.impl.PersonProfileApiUseCaseImpl;
import org.namewta.profile.person.service.PersonProfileApiService;

import org.namewta.profile.person.service.PersonVerificationAttemptService;

import org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry;

import org.namewta.profile.person.controller.self.PersonApplicationExceptionHandler;
import org.namewta.profile.person.controller.self.PersonApplicationController;
import org.namewta.profile.person.domain.exception.PersonApplicationException;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.config.PersonVerificationProviderProperties;
import org.namewta.profile.person.mapper.PersonVerificationAttemptMapper;
import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.person.dao.PersonVerificationAttemptDao;
import org.namewta.profile.person.support.PersonMapperXmlTestSupport;
import org.namewta.profile.person.controller.self.PersonMaterialSelfController;
import org.namewta.profile.person.port.security.ProfileMaterialAccessPolicy;
import org.namewta.profile.person.controller.admin.ProfileMaterialExceptionHandler;
import org.namewta.profile.person.mapper.ProfileMaterialMapper;
import org.namewta.system.api.ConfigService;
import org.namewta.system.api.OssService;
import org.namewta.workflow.api.event.ProcessEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "PROFILE_MYSQL_E2E_URL", matches = ".+")
class PersonApplicationMySqlE2ETest {

    private static final long FRONT_TAG = 2100200000000000101L;
    private static final long BACK_TAG = 2100200000000000102L;
    private static final Instant NOW = Instant.parse("2026-09-01T14:00:00Z");

    @Test
    void completesHttpSnapshotWorkflowPublicationReclaimAndSuccessorLifecycle() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            AtomicLong currentUser = new AtomicLong(950000000001L);
            Fixture fixture = fixture(session, currentUser);
            try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
                login.when(LoginHelper::getUserId).thenAnswer(ignored -> currentUser.get());

                long firstApplication = apply(fixture, currentUser.get(), "110101199001011234", 951000000000L);
                assertThat(scalar(session, "select status from profile_person_application where person_application_id = "
                    + firstApplication)).isEqualTo("WAITING");
                assertThat(scalar(session, "select count(*) from profile_person where identity_key="
                    + "'CN_RESIDENT_ID:110101199001011234'")).isEqualTo("0");
                assertThat(scalar(session, "select count(*) from profile_material_ref where owner_type='SUBMISSION'"
                    + " and owner_id=(select person_submission_id from profile_person_submission where person_application_id="
                    + firstApplication + " and submission_seq=1)")).isEqualTo("2");

                finish(fixture.service(), firstApplication, 1);
                session.commit();
                long originalProfile = Long.parseLong(scalar(session,
                    "select person_profile_id from profile_person where identity_key='CN_RESIDENT_ID:110101199001011234'"
                        + " and status='ACTIVE'"));
                long originalVersion = Long.parseLong(scalar(session,
                    "select current_version_id from profile_person where person_profile_id=" + originalProfile));
                assertThat(scalar(session, "select count(*) from profile_person_binding where person_profile_id="
                    + originalProfile + " and user_id=" + currentUser.get() + " and status='ACTIVE'"))
                    .isEqualTo("1");
                assertThat(scalar(session, "select count(*) from profile_material_ref where owner_type='VERSION'"
                    + " and owner_id=" + originalVersion)).isEqualTo("2");

                finish(fixture.service(), firstApplication, 1);
                session.commit();
                assertThat(scalar(session, "select count(*) from profile_person_version where person_profile_id="
                    + originalProfile)).isEqualTo("1");

                execute(session, "update profile_person_binding set status='UNBOUND', unbound_time=current_timestamp,"
                    + " binding_version=binding_version+1 where person_profile_id=" + originalProfile
                    + " and status='ACTIVE'");
                session.commit();
                currentUser.set(950000000002L);
                long reclaimApplication = apply(fixture, currentUser.get(), "110101199001011234", 952000000000L);
                assertThat(scalar(session, "select current_version_id from profile_person where person_profile_id="
                    + originalProfile)).isEqualTo(String.valueOf(originalVersion));
                finish(fixture.service(), reclaimApplication, 1);
                session.commit();
                assertThat(scalar(session, "select count(*) from profile_person where identity_key="
                    + "'CN_RESIDENT_ID:110101199001011234'")).isEqualTo("1");
                assertThat(scalar(session, "select count(*) from profile_person_binding where person_profile_id="
                    + originalProfile + " and user_id=" + currentUser.get() + " and status='ACTIVE'"))
                    .isEqualTo("1");

                execute(session, "update profile_person_binding set status='UNBOUND', unbound_time=current_timestamp,"
                    + " binding_version=binding_version+1 where person_profile_id=" + originalProfile
                    + " and status='ACTIVE'");
                execute(session, "update profile_person set status='REVOKED', revoked_time=current_timestamp,"
                    + " revoked_reason='e2e', version=version+1 where person_profile_id=" + originalProfile);
                execute(session, "update profile_identity_guard set status='RELEASED', released_time=current_timestamp,"
                    + " version=version+1 where profile_type='PERSON' and owner_id=" + originalProfile
                    + " and status='ACTIVE'");
                session.commit();

                currentUser.set(950000000003L);
                long successorApplication = apply(fixture, currentUser.get(), "110101199001011234", 953000000000L);
                finish(fixture.service(), successorApplication, 1);
                session.commit();
                assertThat(scalar(session, "select count(*) from profile_person where identity_key="
                    + "'CN_RESIDENT_ID:110101199001011234'")).isEqualTo("2");
                assertThat(scalar(session, "select previous_profile_id from profile_person where status='ACTIVE'"
                    + " and identity_key='CN_RESIDENT_ID:110101199001011234'"))
                    .isEqualTo(String.valueOf(originalProfile));
            }
        }
    }

    @Test
    void returnsThenResubmitsAndRollsBackEverySubmitWriteWhenWorkflowFails() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            AtomicLong currentUser = new AtomicLong(950000000011L);
            Fixture fixture = fixture(session, currentUser);
            try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
                login.when(LoginHelper::getUserId).thenAnswer(ignored -> currentUser.get());
                long applicationId = apply(fixture, currentUser.get(), "110101199001011235", 954000000000L);

                process(fixture.service(), applicationId, 1, "back");
                session.commit();
                assertThat(scalar(session, "select status from profile_person_application where person_application_id="
                    + applicationId)).isEqualTo("BACK");
                assertThat(scalar(session, "select count(*) from profile_person where identity_key="
                    + "'CN_RESIDENT_ID:110101199001011235'")).isEqualTo("0");

                fixture.mvc().perform(post("/profile/person/application")
                        .contentType(MediaType.APPLICATION_JSON).content(draft("110101199001011235", 2)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(3));
                fixture.mvc().perform(post("/profile/person/application/submit")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":3}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("WAITING"));
                session.commit();
                finish(fixture.service(), applicationId, 2);
                session.commit();
                assertThat(scalar(session, "select count(*) from profile_person_submission where person_application_id="
                    + applicationId)).isEqualTo("2");
                assertThat(scalar(session, "select count(*) from profile_person where identity_key="
                    + "'CN_RESIDENT_ID:110101199001011235' and status='ACTIVE'")).isEqualTo("1");

                currentUser.set(950000000012L);
                long failingApplication = saveAndAttach(fixture, currentUser.get(),
                    "110101199001011236", 955000000000L);
                session.commit();
                fixture.workflow().fail = true;
                fixture.mvc().perform(post("/profile/person/application/submit")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.msg").value("PERSON_WORKFLOW_UNAVAILABLE"));
                session.rollback();

                assertThat(scalar(session, "select status from profile_person_application where person_application_id="
                    + failingApplication)).isEqualTo("DRAFT");
                assertThat(scalar(session, "select count(*) from profile_person_submission where person_application_id="
                    + failingApplication)).isEqualTo("0");
                assertThat(scalar(session, "select count(*) from profile_verification_attempt where profile_type='PERSON'"
                    + " and application_id=" + failingApplication)).isEqualTo("0");
                assertThat(scalar(session, "select count(*) from profile_material_ref where owner_type='SUBMISSION'"
                    + " and owner_id in (select person_submission_id from profile_person_submission"
                    + " where person_application_id=" + failingApplication + ")")).isEqualTo("0");
            }
        }
    }

    @Test
    void reauthenticatesChangedCoreIdentityWithoutReplacingTheActiveProfileOrBinding() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            AtomicLong currentUser = new AtomicLong(950000000021L);
            Fixture fixture = fixture(session, currentUser);
            try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
                login.when(LoginHelper::getUserId).thenAnswer(ignored -> currentUser.get());
                long firstApplication = apply(fixture, currentUser.get(),
                    "110101199001011241", 956000000000L);
                finish(fixture.service(), firstApplication, 1);
                session.commit();

                long profileId = Long.parseLong(scalar(session,
                    "select person_profile_id from profile_person where identity_key="
                        + "'CN_RESIDENT_ID:110101199001011241' and status='ACTIVE'"));
                long firstVersionId = Long.parseLong(scalar(session,
                    "select current_version_id from profile_person where person_profile_id=" + profileId));

                long reauthentication = apply(fixture, currentUser.get(),
                    "110101199001011242", 957000000000L);
                assertThat(scalar(session, "select identity_key from profile_person where person_profile_id="
                    + profileId)).isEqualTo("CN_RESIDENT_ID:110101199001011241");
                assertThat(scalar(session, "select current_version_id from profile_person where person_profile_id="
                    + profileId)).isEqualTo(String.valueOf(firstVersionId));

                finish(fixture.service(), reauthentication, 1);
                session.commit();
                assertThat(scalar(session, "select identity_key from profile_person where person_profile_id="
                    + profileId)).isEqualTo("CN_RESIDENT_ID:110101199001011242");
                assertThat(scalar(session, "select count(*) from profile_person_version where person_profile_id="
                    + profileId)).isEqualTo("2");
                assertThat(scalar(session, "select count(*) from profile_person_binding where person_profile_id="
                    + profileId + " and user_id=" + currentUser.get() + " and status='ACTIVE'"))
                    .isEqualTo("1");
                assertThat(scalar(session, "select count(*) from profile_identity_guard where profile_type='PERSON'"
                    + " and owner_id=" + profileId + " and identity_key='CN_RESIDENT_ID:110101199001011242'"
                    + " and status='ACTIVE'"))
                    .isEqualTo("1");
            }
        }
    }

    @Test
    void permitsOnlyOneConcurrentOpenSubmissionPerIdentityAndPerAccount() throws Exception {
        UnpooledDataSource dataSource = dataSource();

        List<InsertOutcome> identityRace = raceOpenSubmissions(dataSource,
            new OpenSubmission(960000000001L, 960000000101L, "110101199001012001"),
            new OpenSubmission(960000000002L, 960000000102L, "110101199001012001"));
        assertUniqueConflict(identityRace);

        List<InsertOutcome> accountRace = raceOpenSubmissions(dataSource,
            new OpenSubmission(960000000003L, 960000000103L, "110101199001012002"),
            new OpenSubmission(960000000004L, 960000000103L, "110101199001012003"));
        assertUniqueConflict(accountRace);

        try (SqlSession session = sessionFactory().openSession(true)) {
            assertThat(scalar(session, "select count(*) from profile_person_application where status='WAITING'"
                + " and identity_key='CN_RESIDENT_ID:110101199001012001'"))
                .isEqualTo("1");
            assertThat(scalar(session, "select count(*) from profile_person_application where status='WAITING'"
                + " and applicant_user_id=960000000103"))
                .isEqualTo("1");
            assertThat(scalar(session, "select count(*) from profile_person where identity_key in ("
                + "'CN_RESIDENT_ID:110101199001012001','CN_RESIDENT_ID:110101199001012002',"
                + "'CN_RESIDENT_ID:110101199001012003')")).isEqualTo("0");
            assertThat(scalar(session, "select count(*) from profile_person_binding where user_id in ("
                + "960000000101,960000000102,960000000103)")).isEqualTo("0");
        }
    }

    private long apply(Fixture fixture, long userId, String documentNumber, long ossBase) throws Exception {
        long applicationId = saveAndAttach(fixture, userId, documentNumber, ossBase);
        fixture.mvc().perform(post("/profile/person/application/submit")
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("WAITING"))
            .andExpect(jsonPath("$.data.snapshotVersion").value(1));
        fixture.session().commit();
        assertThat(fixture.workflow().applicationId).isEqualTo(applicationId);
        assertThat(fixture.workflow().snapshotVersion).isEqualTo(1);
        return applicationId;
    }

    private long saveAndAttach(Fixture fixture, long userId, String documentNumber, long ossBase) throws Exception {
        fixture.mvc().perform(post("/profile/person/application")
                .contentType(MediaType.APPLICATION_JSON).content(draft(documentNumber, 0)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.version").value(0));
        long applicationId = Long.parseLong(scalar(fixture.session(),
            "select person_application_id from profile_person_application where applicant_user_id=" + userId
                + " and status in ('DRAFT','BACK','CANCEL','WAITING')"));
        attach(fixture.mvc(), applicationId, ossBase + 1, FRONT_TAG);
        attach(fixture.mvc(), applicationId, ossBase + 2, BACK_TAG);
        return applicationId;
    }

    private void attach(MockMvc mvc, long applicationId, long ossId, long tagId) throws Exception {
        mvc.perform(post("/profile/person/materials/WORKING/{ownerId}", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ossId\":" + ossId + ",\"materialNodeId\":" + tagId + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.ossId").value(ossId));
    }

    private String draft(String documentNumber, int expectedVersion) {
        return """
            {"fullName":"张三","documentTypeCode":"CN_RESIDENT_ID",
             "documentNumber":"%s","gender":"MALE","birthDate":"1990-01-01",
             "validFrom":"2020-01-01","validUntil":"2030-01-01","expectedVersion":%d}
            """.formatted(documentNumber, expectedVersion);
    }

    private void finish(PersonApplicationServiceImpl service, long applicationId, int snapshotVersion) {
        process(service, applicationId, snapshotVersion, "finish");
    }

    private void process(PersonApplicationServiceImpl service, long applicationId, int snapshotVersion, String status) {
        ProcessEvent event = new ProcessEvent();
        event.setFlowCode("profile_person_verification");
        event.setBusinessId(String.valueOf(applicationId));
        event.setStatus(status);
        event.setParams(Map.of("snapshotVersion", snapshotVersion));
        new org.namewta.profile.person.listener.PersonApplicationProcessListener(service).handle(event);
    }

    private Fixture fixture(SqlSession session, AtomicLong currentUser) {
        PersonApplicationMapper applicationMapper = session.getMapper(PersonApplicationMapper.class);
        OssService oss = mock(OssService.class);
        when(oss.objectMetadata(anyLong())).thenAnswer(invocation -> {
            long ossId = invocation.getArgument(0);
            return new OssService.OssObjectMetadata(ossId, "profile/e2e/" + ossId + ".jpg",
                ossId + ".jpg", ".jpg", 1024, "image/jpeg", currentUser.get());
        });
        ProfileMaterialAccessPolicy accessPolicy = mock(ProfileMaterialAccessPolicy.class);
        when(accessPolicy.requireAttach(any())).thenAnswer(ignored -> currentUser.get());
        ProfileMaterialServiceImpl materials = new ProfileMaterialServiceImpl(
            session.getMapper(ProfileMaterialMapper.class),
            oss, accessPolicy,
            List.of(new PersonProfileMaterialOwnerContributor(new PersonProfileApiUseCaseImpl(
                new PersonProfileApiService(new PersonApplicationDao(session.getMapper(PersonApplicationMapper.class)))))));

        PersonVerificationAttemptMapper attemptMapper = session.getMapper(PersonVerificationAttemptMapper.class);
        PersonVerificationEvidenceCodec evidenceCodec = new PersonVerificationEvidenceCodec();
        PersonVerificationProviderProperties properties = new PersonVerificationProviderProperties();
        properties.setEnabledProviders(java.util.Set.of("manual"));
        PersonVerificationAttemptService attempts = new PersonVerificationAttemptService(
            new PersonVerificationProviderRegistry(List.of(new PersonManualVerificationProvider()), properties),
            new PersonVerificationAttemptDao(attemptMapper), evidenceCodec);
        ConfigService config = mock(ConfigService.class);
        when(config.getConfigValue("profile.person.provider.default")).thenReturn("manual");
        when(config.getConfigValue("profile.person.flowCode")).thenReturn("profile_person_verification");
        RecordingWorkflowGateway workflow = new RecordingWorkflowGateway();
        PersonApplicationServiceImpl service = new PersonApplicationServiceImpl(applicationMapper, materials,
            new PersonVerificationProviderRegistry(List.of(new PersonManualVerificationProvider()), properties),
            attempts, workflow, config, Clock.fixed(NOW, ZoneOffset.UTC));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new PersonApplicationController(service), new PersonMaterialSelfController(materials))
            .setControllerAdvice(new PersonApplicationExceptionHandler(), new ProfileMaterialExceptionHandler())
            .build();
        return new Fixture(session, service, workflow, mvc);
    }

    private SqlSessionFactory sessionFactory() {
        Configuration configuration = new Configuration(new Environment(
            "profile-person-application-e2e", new JdbcTransactionFactory(), dataSource()));
        configuration.setMapUnderscoreToCamelCase(true);
        PersonMapperXmlTestSupport.parse(configuration, PersonApplicationMapper.class);
        PersonMapperXmlTestSupport.parse(configuration, ProfileMaterialMapper.class);
        PersonMapperXmlTestSupport.parse(configuration, PersonVerificationAttemptMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private UnpooledDataSource dataSource() {
        return new UnpooledDataSource("com.mysql.cj.jdbc.Driver",
            System.getenv("PROFILE_MYSQL_E2E_URL"), System.getenv("PROFILE_MYSQL_E2E_USERNAME"),
            System.getenv("PROFILE_MYSQL_E2E_PASSWORD"));
    }

    private List<InsertOutcome> raceOpenSubmissions(UnpooledDataSource dataSource,
                                                    OpenSubmission first,
                                                    OpenSubmission second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<InsertOutcome> firstResult = executor.submit(
                () -> insertOpenSubmission(dataSource, first, ready, start));
            Future<InsertOutcome> secondResult = executor.submit(
                () -> insertOpenSubmission(dataSource, second, ready, start));
            ready.await();
            start.countDown();
            return List.of(firstResult.get(), secondResult.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private InsertOutcome insertOpenSubmission(UnpooledDataSource dataSource,
                                               OpenSubmission submission,
                                               CountDownLatch ready,
                                               CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        String sql = """
            insert into profile_person_application (
                person_application_id, applicant_user_id, status, full_name,
                document_type_code, document_number, identity_key, gender, birth_date,
                valid_from, valid_until, provider_code, submission_seq, rebind_intent,
                decision_version, version, create_dept, create_time, create_by,
                update_time, update_by, del_flag
            ) values (?, ?, 'WAITING', '并发测试', 'CN_RESIDENT_ID', ?, ?, 'UNKNOWN', '1990-01-01',
                      '2020-01-01', '2030-01-01', 'manual', 1, 'N', 0, 0, -1,
                      current_timestamp, ?, current_timestamp, ?, '0')
            """;
        try (var connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, submission.applicationId());
            statement.setLong(2, submission.userId());
            statement.setString(3, submission.documentNumber());
            statement.setString(4, "CN_RESIDENT_ID:" + submission.documentNumber());
            statement.setLong(5, submission.userId());
            statement.setLong(6, submission.userId());
            statement.executeUpdate();
            return new InsertOutcome(true, null);
        } catch (SQLException exception) {
            return new InsertOutcome(false, exception.getSQLState());
        }
    }

    private void assertUniqueConflict(List<InsertOutcome> outcomes) {
        assertThat(outcomes).filteredOn(InsertOutcome::success).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.success())
            .singleElement()
            .extracting(InsertOutcome::sqlState)
            .asString()
            .startsWith("23");
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

    private record Fixture(SqlSession session, PersonApplicationServiceImpl service,
                           RecordingWorkflowGateway workflow, MockMvc mvc) {
    }

    private record OpenSubmission(long applicationId, long userId, String documentNumber) {
    }

    private record InsertOutcome(boolean success, String sqlState) {
    }

    private static final class RecordingWorkflowGateway implements PersonWorkflowGateway {
        @Override public void terminate(String businessId, String reason) {
            throw new AssertionError("This fixture only exercises process start and explicit snapshot events");
        }
        @Override public Integer persistedSnapshotVersionByInstanceId(Long instanceId) {
            throw new AssertionError("This fixture supplies snapshotVersion in every process event");
        }

        private long applicationId;
        private int snapshotVersion;
        private boolean fail;

        @Override
        public void start(long applicationId, long submissionId, int snapshotVersion) {
            if (fail) {
                throw new PersonApplicationException("PERSON_WORKFLOW_UNAVAILABLE");
            }
            this.applicationId = applicationId;
            this.snapshotVersion = snapshotVersion;
        }
    }
}
