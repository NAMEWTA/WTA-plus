package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.service.PersonRebindNotificationService;

import org.namewta.profile.person.service.PersonVerificationAttemptService;

import org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry;

import org.namewta.profile.person.controller.self.PersonRebindExceptionHandler;
import org.namewta.profile.person.controller.self.PersonRebindController;
import org.namewta.profile.person.domain.exception.PersonRebindException;
import org.namewta.profile.person.event.PersonReboundEvent;
import org.namewta.profile.person.listener.PersonRebindProcessListener;
import org.namewta.profile.person.mapper.PersonRebindMapper;
import org.namewta.profile.person.support.PersonMapperXmlTestSupport;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.profile.person.mapper.PersonNotificationAuditMapper;
import org.namewta.profile.person.dao.PersonNotificationAuditDao;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.system.api.ConfigService;
import org.namewta.system.api.UserService;
import org.namewta.workflow.api.event.ProcessEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "PROFILE_MYSQL_E2E_URL", matches = ".+")
class PersonRebindMySqlE2ETest {

    private static final Instant NOW = Instant.parse("2026-09-01T14:00:00Z");

    @Test
    void completesPrivateHttpReviewAtomicSwitchNotificationFailureAndSelfUnbind() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            seed(session, 970000000001L, 970000000101L, 970000000201L, 970000000301L,
                "110101199001013001", "DRAFT", false);
            session.commit();
            Fixture fixture = fixture(session);

            try (MockedStatic<LoginHelper> login = org.mockito.Mockito.mockStatic(LoginHelper.class)) {
                login.when(LoginHelper::getUserId).thenReturn(970000000101L);
                fixture.mvc().perform(post("/profile/person/rebind/probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentTypeCode\":\"CN_RESIDENT_ID\","
                            + "\"documentNumber\":\"110101199001013001\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("BOUND"))
                    .andExpect(jsonPath("$.data.personProfileId").doesNotExist());
                fixture.mvc().perform(post("/profile/person/rebind/match")
                        .contentType(MediaType.APPLICATION_JSON).content(matchBody("错误姓名")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("NOT_AVAILABLE"))
                    .andExpect(jsonPath("$.data.maskedPhone").doesNotExist());
                fixture.mvc().perform(post("/profile/person/rebind/match")
                        .contentType(MediaType.APPLICATION_JSON).content(matchBody("张三")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REBIND_AVAILABLE"))
                    .andExpect(jsonPath("$.data.maskedPhone").value("138****8000"))
                    .andExpect(jsonPath("$.data.personProfileId").doesNotExist());
                fixture.mvc().perform(post("/profile/person/rebind/confirm")
                        .contentType(MediaType.APPLICATION_JSON).content(confirmBody(0)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.data.version").value(1));
                session.commit();

                assertThat(scalar(session, "select status from profile_person_binding where person_binding_id="
                    + "970000000201")).isEqualTo("ACTIVE");
                fixture.mvc().perform(post("/profile/person/rebind/submit")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":1}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("WAITING"))
                    .andExpect(jsonPath("$.data.snapshotVersion").value(1));
                session.commit();

                assertThat(scalar(session, "select status from profile_person_binding where person_binding_id="
                    + "970000000201")).isEqualTo("ACTIVE");
                assertThat(scalar(session, "select status from profile_person_application where "
                    + "person_application_id=970000000301")).isEqualTo("WAITING");

                fixture.listener().handle(finish(970000000301L, 1));
                session.commit();
                assertThat(scalar(session, "select status from profile_person_binding where person_binding_id="
                    + "970000000201")).isEqualTo("UNBOUND");
                assertThat(scalar(session, "select count(*) from profile_person_binding where person_profile_id="
                    + "970000000001 and user_id=970000000101 and status='ACTIVE'")).isEqualTo("1");
                assertThat(scalar(session, "select count(*) from profile_person_binding_event where "
                    + "person_profile_id=970000000001 and event_type='UNBOUND' and binding_version=2"))
                    .isEqualTo("1");
                assertThat(scalar(session, "select count(*) from profile_person_binding_event where "
                    + "person_profile_id=970000000001 and user_id=970000000101 and event_type='ACTIVE'"))
                    .isEqualTo("1");
                assertThat(scalar(session, "select count(*) from profile_person_version where "
                    + "person_profile_id=970000000001")).isEqualTo("2");
                assertThat(scalar(session, "select status from profile_person_application where "
                    + "person_application_id=970000000301")).isEqualTo("FINISH");

                ArgumentCaptor<PersonReboundEvent> event = ArgumentCaptor.forClass(PersonReboundEvent.class);
                verify(fixture.events()).publishEvent(event.capture());
                fixture.notifications().notifyOldAccount(event.getValue());
                session.commit();
                assertThat(scalar(session, "select count(*) from profile_notification_audit where "
                    + "application_id=970000000301 and status='FAILED'")).isEqualTo("2");

                fixture.service().unbind(970000000101L);
                session.commit();
                assertThat(scalar(session, "select count(*) from profile_person_binding where person_profile_id="
                    + "970000000001 and status='ACTIVE'")).isEqualTo("0");
                assertThat(scalar(session, "select status from profile_person where person_profile_id="
                    + "970000000001")).isEqualTo("ACTIVE");
                assertThat(scalar(session, "select count(*) from profile_person_version where "
                    + "person_profile_id=970000000001")).isEqualTo("2");
            }
        }
    }

    @Test
    void bindingVersionRaceRollsBackWithNoSwitchOrNotification() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            seed(session, 971000000001L, 971000000101L, 971000000201L, 971000000301L,
                "110101199001013002", "WAITING", true);
            session.commit();
            execute(session, "update profile_person_binding set binding_version=2, version=version+1 "
                + "where person_binding_id=971000000201");
            session.commit();
            PersonRebindServiceImpl service = new PersonRebindServiceImpl(
                session.getMapper(PersonRebindMapper.class), session.getMapper(PersonApplicationMapper.class),
                JsonMapper.builder().build(), mock(ProfileMaterialPort.class),
                mock(PersonVerificationProviderRegistry.class), mock(PersonVerificationAttemptService.class),
                mock(PersonWorkflowGateway.class), mock(UserService.class), Clock.fixed(NOW, ZoneOffset.UTC));

            assertThatThrownBy(() -> service.publishApprovedRebind(971000000301L, 1, NOW))
                .isInstanceOf(PersonRebindException.class)
                .hasMessage("PERSON_REBIND_BINDING_CHANGED");
            session.rollback();

            assertThat(scalar(session, "select status from profile_person_application where "
                + "person_application_id=971000000301")).isEqualTo("WAITING");
            assertThat(scalar(session, "select status from profile_person_binding where "
                + "person_binding_id=971000000201")).isEqualTo("ACTIVE");
            assertThat(scalar(session, "select count(*) from profile_person_binding where "
                + "person_profile_id=971000000001 and user_id=971000000101")).isEqualTo("0");
            assertThat(scalar(session, "select count(*) from profile_person_version where "
                + "person_profile_id=971000000001")).isEqualTo("1");
            assertThat(scalar(session, "select count(*) from profile_notification_audit where "
                + "application_id=971000000301")).isEqualTo("0");
        }
    }

    private Fixture fixture(SqlSession session) {
        PersonApplicationMapper applications = session.getMapper(PersonApplicationMapper.class);
        PersonRebindMapper rebinds = session.getMapper(PersonRebindMapper.class);
        ProfileMaterialPort materials = mock(ProfileMaterialPort.class);
        PersonVerificationProviderRegistry providers = mock(PersonVerificationProviderRegistry.class);
        PersonVerificationAttemptService attempts = mock(PersonVerificationAttemptService.class);
        RecordingWorkflow workflow = new RecordingWorkflow();
        UserService users = mock(UserService.class);
        when(users.selectPhonenumberById(970000000201L)).thenReturn("13800138000");
        ConfigService config = mock(ConfigService.class);
        when(config.getConfigValue("profile.person.flowCode")).thenReturn("profile_person_verification");
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        NotificationApplicationService notificationService = mock(NotificationApplicationService.class);
        doThrow(new IllegalStateException("offline")).when(notificationService).submit(any());
        PersonRebindNotificationService notifications = new PersonRebindNotificationService(
            new PersonNotificationAuditDao(session.getMapper(PersonNotificationAuditMapper.class)),
            notificationService, users);
        PersonRebindServiceImpl service = new PersonRebindServiceImpl(rebinds, applications,
            JsonMapper.builder().build(), materials, providers, attempts, workflow, users,
            Clock.fixed(NOW, ZoneOffset.UTC), config, notifications, events);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PersonRebindController(service))
            .setControllerAdvice(new PersonRebindExceptionHandler()).build();
        PersonRebindProcessListener listener = new PersonRebindProcessListener(service);
        return new Fixture(service, listener, notifications, events, mvc);
    }

    private void seed(SqlSession session, long profileId, long newUserId, long oldBindingId,
                      long applicationId, String documentNumber, String applicationStatus,
                      boolean frozenSubmission) throws Exception {
        long oldUserId = oldBindingId;
        long versionId = profileId + 1;
        String identityKey = "CN_RESIDENT_ID:" + documentNumber;
        execute(session, "insert into profile_person (person_profile_id,current_version_id,full_name,"
            + "document_type_code,document_number,identity_key,gender,birth_date,valid_from,valid_until,status,"
            + "version,create_dept,create_time,create_by,update_time,update_by,del_flag) values (" + profileId
            + "," + versionId + ",'张三','CN_RESIDENT_ID','" + documentNumber + "','" + identityKey
            + "','MALE','1990-01-01','2020-01-01','2030-01-01','ACTIVE',0,-1,current_timestamp,-1,"
            + "current_timestamp,-1,'0')");
        execute(session, "insert into profile_person_version (person_version_id,person_profile_id,version_no,"
            + "source_type,source_id,full_name,document_type_code,document_number,identity_key,gender,birth_date,"
            + "valid_from,valid_until,status,published_time,version,create_dept,create_time,create_by,update_time,"
            + "update_by,del_flag) values (" + versionId + "," + profileId + ",1,'USER_SUBMISSION',"
            + (profileId + 2) + ",'张三','CN_RESIDENT_ID','" + documentNumber + "','" + identityKey
            + "','MALE','1990-01-01','2020-01-01','2030-01-01','CURRENT',current_timestamp,0,-1,"
            + "current_timestamp,-1,current_timestamp,-1,'0')");
        execute(session, "insert into profile_person_binding (person_binding_id,person_profile_id,user_id,status,"
            + "binding_version,source_type,source_id,bound_time,version,create_dept,create_time,create_by,update_time,"
            + "update_by,del_flag) values (" + oldBindingId + "," + profileId + "," + oldUserId
            + ",'ACTIVE',1,'USER_SUBMISSION'," + (profileId + 2)
            + ",current_timestamp,0,-1,current_timestamp,-1,current_timestamp,-1,'0')");
        execute(session, "insert into profile_person_binding_event (person_binding_event_id,person_binding_id,"
            + "person_profile_id,user_id,event_type,binding_version,source_type,source_id,reason,occurred_time,version,"
            + "create_dept,create_time,create_by,update_time,update_by,del_flag) values (" + (oldBindingId + 1)
            + "," + oldBindingId + "," + profileId + "," + oldUserId
            + ",'ACTIVE',1,'USER_SUBMISSION'," + (profileId + 2)
            + ",'INITIAL',current_timestamp,0,-1,current_timestamp,-1,current_timestamp,-1,'0')");
        int submissionSeq = frozenSubmission ? 1 : 0;
        int version = frozenSubmission ? 1 : 0;
        execute(session, "insert into profile_person_application (person_application_id,applicant_user_id,"
            + "target_profile_id,status,full_name,document_type_code,document_number,identity_key,gender,birth_date,"
            + "valid_from,valid_until,provider_code,submission_seq,rebind_intent,expected_binding_id,"
            + "expected_binding_version,decision_version,version,create_dept,create_time,create_by,update_time,"
            + "update_by,del_flag) values (" + applicationId + "," + newUserId + ","
            + (frozenSubmission ? profileId : "null") + ",'" + applicationStatus
            + "','张三','CN_RESIDENT_ID','" + documentNumber + "','" + identityKey
            + "','MALE','1990-01-01','2020-01-01','2030-01-01','manual'," + submissionSeq + ",'"
            + (frozenSubmission ? "Y" : "N") + "'," + (frozenSubmission ? oldBindingId : "null") + ","
            + (frozenSubmission ? "1" : "null") + ",0," + version
            + ",-1,current_timestamp," + newUserId + ",current_timestamp," + newUserId + ",'0')");
        if (frozenSubmission) {
            execute(session, "insert into profile_person_submission (person_submission_id,person_application_id,"
                + "submission_seq,full_name,document_type_code,document_number,identity_key,gender,birth_date,"
                + "valid_from,valid_until,provider_code,rebind_intent,target_profile_id,expected_binding_id,"
                + "expected_binding_version,field_snapshot_json,submitted_time,version,create_dept,create_time,"
                + "create_by,update_time,update_by,del_flag) values (" + (applicationId + 1) + "," + applicationId
                + ",1,'张三','CN_RESIDENT_ID','" + documentNumber + "','" + identityKey
                + "','MALE','1990-01-01','2020-01-01','2030-01-01','manual','Y'," + profileId + ","
                + oldBindingId + ",1,'{}',current_timestamp,0,-1,current_timestamp," + newUserId
                + ",current_timestamp," + newUserId + ",'0')");
        }
    }

    private String matchBody(String fullName) {
        return """
            {"identity":{"fullName":"%s","documentTypeCode":"CN_RESIDENT_ID",
             "documentNumber":"110101199001013001","gender":"MALE","birthDate":"1990-01-01",
             "validFrom":"2020-01-01","validUntil":"2030-01-01"}}
            """.formatted(fullName);
    }

    private String confirmBody(int expectedVersion) {
        String match = matchBody("张三");
        return match.substring(0, match.lastIndexOf('}')) + ",\"expectedVersion\":" + expectedVersion + "}";
    }

    private ProcessEvent finish(long applicationId, int snapshotVersion) {
        ProcessEvent event = new ProcessEvent();
        event.setFlowCode("profile_person_verification");
        event.setBusinessId(Long.toString(applicationId));
        event.setStatus("FINISH");
        event.setParams(Map.of("snapshotVersion", snapshotVersion));
        return event;
    }

    private SqlSessionFactory sessionFactory() {
        Configuration configuration = new Configuration(new Environment("profile-person-rebind-e2e",
            new JdbcTransactionFactory(), new UnpooledDataSource("com.mysql.cj.jdbc.Driver",
            System.getenv("PROFILE_MYSQL_E2E_URL"), System.getenv("PROFILE_MYSQL_E2E_USERNAME"),
            System.getenv("PROFILE_MYSQL_E2E_PASSWORD"))));
        configuration.setMapUnderscoreToCamelCase(true);
        PersonMapperXmlTestSupport.parse(configuration, PersonApplicationMapper.class);
        PersonMapperXmlTestSupport.parse(configuration, PersonRebindMapper.class);
        PersonMapperXmlTestSupport.parse(configuration, PersonNotificationAuditMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
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

    private record Fixture(PersonRebindServiceImpl service, PersonRebindProcessListener listener,
                           PersonRebindNotificationService notifications, ApplicationEventPublisher events,
                           MockMvc mvc) {
    }

    private static final class RecordingWorkflow implements PersonWorkflowGateway {
        @Override
        public void start(long applicationId, long submissionId, int snapshotVersion) {
        }
    }
}
