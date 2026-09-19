package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.domain.bo.PersonAdminAssignBo;
import org.namewta.profile.person.domain.bo.PersonAdminBindingBo;
import org.namewta.profile.person.domain.bo.PersonAdminCreateBo;
import org.namewta.profile.person.domain.bo.PersonAdminDecisionBo;
import org.namewta.profile.person.domain.bo.PersonAdminIdentityBo;
import org.namewta.profile.person.domain.bo.PersonAdminQueryBo;
import org.namewta.profile.person.domain.bo.PersonAdminReviseBo;
import org.namewta.profile.person.domain.bo.PersonAdminRevokeBo;

import org.namewta.profile.person.controller.admin.PersonAdminController;
import org.namewta.profile.person.controller.admin.PersonAdminExceptionHandler;
import org.namewta.profile.person.domain.exception.PersonAdminException;
import org.namewta.profile.person.mapper.PersonAdminMapper;
import org.namewta.profile.person.support.PersonMapperXmlTestSupport;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialReferenceView;
import org.namewta.profile.person.domain.application.PersonDocumentTypeRule;
import org.namewta.system.api.OssService;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "PROFILE_MYSQL_E2E_URL", matches = ".+")
class PersonAdminMySqlE2ETest {

    private static final long OPERATOR = 978000000001L;
    private static final long USER = 978000000002L;
    private static final long REJECT_APPLICATION = 978000000011L;
    private static final long REJECT_SUBMISSION = 978000000012L;
    private static final long FAIL_APPLICATION = 978000000021L;
    private static final long FAIL_SUBMISSION = 978000000022L;
    private static final String DOCUMENT = "110101199001019909";
    private static final Instant NOW = Instant.parse("2026-09-02T01:00:00Z");

    @Test
    void persistsAdminLifecycleDecisionFenceMaterialAccessAndRollback() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            cleanup(session);
            session.commit(true);

            PersonAdminMapper mapper = session.getMapper(PersonAdminMapper.class);
            PersonApplicationServiceImpl applications = mock(PersonApplicationServiceImpl.class);
            when(applications.findDocumentType("CN_RESIDENT_ID"))
                .thenReturn(Optional.of(new PersonDocumentTypeRule("CN_RESIDENT_ID", "^[0-9]{17}[0-9X]$", true)));
            ProfileMaterialPort materials = materials();
            PersonWorkflowGateway workflow = mock(PersonWorkflowGateway.class);
            org.mockito.Mockito.doNothing().when(workflow).terminate(eq(Long.toString(REJECT_APPLICATION)), anyString());
            org.mockito.Mockito.doThrow(new IllegalStateException("workflow unavailable")).when(workflow).terminate(eq(Long.toString(FAIL_APPLICATION)), anyString());
            UserService users = mock(UserService.class);
            when(users.selectById(anyLong())).thenAnswer(invocation -> normalUser(invocation.getArgument(0)));
            PersonAdminServiceImpl service = new PersonAdminServiceImpl(mapper,
                applications, materials, workflow, users, Clock.fixed(NOW, ZoneOffset.UTC));

            var identity = new PersonAdminIdentityBo("管理直建用户", "CN_RESIDENT_ID", DOCUMENT,
                "UNKNOWN", LocalDate.parse("1990-01-01"), LocalDate.parse("2020-01-01"),
                LocalDate.parse("2035-01-01"));
            var created = service.create(OPERATOR, new PersonAdminCreateBo(identity, USER,
                "e2e admin create", List.of()));
            session.commit(true);

            assertThat(created.status()).isEqualTo("ACTIVE");
            assertThat(scalar(session, "select source_type from profile_person_version where person_version_id="
                + created.versionId())).isEqualTo("ADMIN_CREATE");
            assertThat(scalar(session, "select status from profile_person_binding where person_binding_id="
                + created.bindingId())).isEqualTo("ACTIVE");

            var revisedIdentity = new PersonAdminIdentityBo("管理修订用户", "CN_RESIDENT_ID", DOCUMENT,
                "FEMALE", LocalDate.parse("1990-01-01"), LocalDate.parse("2021-01-01"),
                LocalDate.parse("2036-01-01"));
            var revised = service.revise(OPERATOR, created.profileId(),
                new PersonAdminReviseBo(revisedIdentity, "e2e admin override", 1));
            session.commit(true);
            assertThat(scalar(session, "select count(*) from profile_person_version where person_profile_id="
                + created.profileId())).isEqualTo("2");
            assertThat(scalar(session, "select count(*) from profile_person_version where person_profile_id="
                + created.profileId() + " and status='CURRENT' and source_type='ADMIN_OVERRIDE'"))
                .isEqualTo("1");

            var suspended = service.manageBinding(OPERATOR, created.profileId(),
                new PersonAdminBindingBo("SUSPEND", "e2e suspend", 1));
            var resumed = service.manageBinding(OPERATOR, created.profileId(),
                new PersonAdminBindingBo("RESUME", "e2e resume", suspended.version()));
            var unbound = service.manageBinding(OPERATOR, created.profileId(),
                new PersonAdminBindingBo("UNBIND", "e2e unbind", resumed.version()));
            var assigned = service.assign(OPERATOR, created.profileId(),
                new PersonAdminAssignBo(USER, "e2e assign"));
            session.commit(true);
            assertThat(unbound.status()).isEqualTo("UNBOUND");
            assertThat(scalar(session, "select count(*) from profile_person_binding_event where person_profile_id="
                + created.profileId())).isEqualTo("5");

            MockMvc mvc = MockMvcBuilders.standaloneSetup(new PersonAdminController(service))
                .setControllerAdvice(new PersonAdminExceptionHandler()).build();
            mvc.perform(get("/profile/person/archive/{profileId}", created.profileId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.fullName").value("管理修订用户"))
                .andExpect(jsonPath("$.data.currentMaterials[0].fileName").value("e2e-proof.jpg"));
            mvc.perform(get("/profile/person/archive/{profileId}/material/{materialRefId}/access-url",
                    created.profileId(), 8801L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessType").value("PRIVATE"));

            var revoked = service.revoke(OPERATOR, created.profileId(),
                new PersonAdminRevokeBo("e2e revoke", revised.version()));
            session.commit(true);
            assertThat(revoked.status()).isEqualTo("REVOKED");
            assertThat(service.page(new PersonAdminQueryBo(null, DOCUMENT, null, 1, 20)).getTotal())
                .isZero();
            assertThat(service.page(new PersonAdminQueryBo(null, DOCUMENT, "REVOKED", 1, 20)).getTotal())
                .isEqualTo(1);
            assertThat(service.detail(created.profileId()).currentMaterials()).hasSize(1);
            assertThatThrownBy(() -> service.assign(OPERATOR, created.profileId(),
                new PersonAdminAssignBo(USER, "must remain read only")))
                .isInstanceOf(PersonAdminException.class).hasMessage("PERSON_PROFILE_REVOKED_READ_ONLY");

            seedWaiting(session, REJECT_APPLICATION, REJECT_SUBMISSION, "110101199001019910");
            session.commit(true);
            var rejection = service.decide(OPERATOR, REJECT_APPLICATION,
                new PersonAdminDecisionBo("REJECTED", "e2e reject"));
            session.commit(true);
            assertThat(rejection.status()).isEqualTo("REJECTED");
            assertThat(scalar(session, "select concat(status,':',decision_version,':',decision_source)"
                + " from profile_person_application where person_application_id=" + REJECT_APPLICATION))
                .isEqualTo("INVALID:1:ADMIN_OVERRIDE");
            assertThat(scalar(session, "select decision_status from profile_decision_record where profile_type='PERSON'"
                + " and application_id=" + REJECT_APPLICATION)).isEqualTo("FINAL");

            seedWaiting(session, FAIL_APPLICATION, FAIL_SUBMISSION, "110101199001019911");
            session.commit(true);
            assertThatThrownBy(() -> service.decide(OPERATOR, FAIL_APPLICATION,
                new PersonAdminDecisionBo("REJECTED", "e2e fail")))
                .isInstanceOf(PersonAdminException.class)
                .hasMessage("PERSON_ADMIN_WORKFLOW_TERMINATION_FAILED");
            session.rollback(true);
            assertThat(scalar(session, "select concat(status,':',decision_version) from profile_person_application"
                + " where person_application_id=" + FAIL_APPLICATION)).isEqualTo("WAITING:0");
            assertThat(scalar(session, "select count(*) from profile_decision_record where profile_type='PERSON'"
                + " and application_id=" + FAIL_APPLICATION)).isEqualTo("0");

            cleanup(session);
            session.commit(true);
            verify(workflow).terminate(Long.toString(REJECT_APPLICATION), "e2e reject");
            assertThat(assigned.status()).isEqualTo("ACTIVE");
        }
    }

    private ProfileMaterialPort materials() {
        ProfileMaterialPort materials = mock(ProfileMaterialPort.class);
        MaterialOwnerKey owner = new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.VERSION, 1L);
        MaterialReferenceView reference = new MaterialReferenceView(8801L, owner, 9901L, 7701L,
            "IDENTITY_FRONT", "身份证人像面", "e2e-proof.jpg", 1024, "jpg", "image/jpeg",
            true, true, NOW, null, 0);
        when(materials.list(any())).thenReturn(List.of(reference));
        when(materials.accessUrl(any(), eq(8801L)))
            .thenReturn(new OssService.OssAccessUrl("PRIVATE", "https://example.invalid/e2e", NOW.plusSeconds(60),
                "e2e-proof.jpg"));
        when(materials.snapshotImmutable(any(), any())).thenReturn(List.of(reference));
        return materials;
    }

    private UserDTO normalUser(long userId) {
        UserDTO user = new UserDTO();
        user.setUserId(userId);
        user.setStatus("0");
        return user;
    }

    private void seedWaiting(SqlSession session, long applicationId, long submissionId, String document) throws Exception {
        execute(session, "insert into profile_person_application(person_application_id,applicant_user_id,status,"
            + "full_name,document_type_code,document_number,identity_key,gender,birth_date,valid_from,valid_until,"
            + "provider_code,submission_seq,rebind_intent,decision_version,version,create_dept,create_time,create_by,"
            + "update_time,update_by,del_flag) values(" + applicationId + "," + (applicationId + 1000)
            + ",'WAITING','审核用户','CN_RESIDENT_ID','" + document + "','CN_RESIDENT_ID:" + document
            + "','UNKNOWN','1990-01-01','2020-01-01','2035-01-01','manual',1,'N',0,0,-1,current_timestamp,"
            + OPERATOR + ",current_timestamp," + OPERATOR + ",'0')");
        execute(session, "insert into profile_person_submission(person_submission_id,person_application_id,submission_seq,"
            + "full_name,document_type_code,document_number,identity_key,gender,birth_date,valid_from,valid_until,"
            + "provider_code,rebind_intent,field_snapshot_json,submitted_time,version,create_dept,create_time,create_by,"
            + "update_time,update_by,del_flag) values(" + submissionId + "," + applicationId
            + ",1,'审核用户','CN_RESIDENT_ID','" + document + "','CN_RESIDENT_ID:" + document
            + "','UNKNOWN','1990-01-01','2020-01-01','2035-01-01','manual','N','{}',current_timestamp,0,-1,"
            + "current_timestamp," + OPERATOR + ",current_timestamp," + OPERATOR + ",'0')");
    }

    private SqlSessionFactory sessionFactory() {
        Configuration configuration = new Configuration(new Environment("person-admin-e2e",
            new JdbcTransactionFactory(), new UnpooledDataSource("com.mysql.cj.jdbc.Driver",
            System.getenv("PROFILE_MYSQL_E2E_URL"), System.getenv("PROFILE_MYSQL_E2E_USERNAME"),
            System.getenv("PROFILE_MYSQL_E2E_PASSWORD"))));
        configuration.setMapUnderscoreToCamelCase(true);
        PersonMapperXmlTestSupport.parse(configuration, PersonAdminMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private void cleanup(SqlSession session) throws Exception {
        execute(session, "delete from profile_operation_audit where operator_user_id=" + OPERATOR);
        execute(session, "delete from profile_decision_record where profile_type='PERSON' and application_id in ("
            + REJECT_APPLICATION + "," + FAIL_APPLICATION + ")");
        execute(session, "delete from profile_person_submission where person_application_id in ("
            + REJECT_APPLICATION + "," + FAIL_APPLICATION + ")");
        execute(session, "delete from profile_person_application where person_application_id in ("
            + REJECT_APPLICATION + "," + FAIL_APPLICATION + ")");
        execute(session, "delete from profile_person_binding_event where person_profile_id in (select person_profile_id"
            + " from profile_person where identity_key='CN_RESIDENT_ID:" + DOCUMENT + "')");
        execute(session, "delete from profile_person_binding where person_profile_id in (select person_profile_id"
            + " from profile_person where identity_key='CN_RESIDENT_ID:" + DOCUMENT + "')");
        execute(session, "delete from profile_person_version where person_profile_id in (select person_profile_id"
            + " from profile_person where identity_key='CN_RESIDENT_ID:" + DOCUMENT + "')");
        execute(session, "delete from profile_person_source where person_profile_id in (select person_profile_id"
            + " from profile_person where identity_key='CN_RESIDENT_ID:" + DOCUMENT + "')");
        execute(session, "delete from profile_person where identity_key='CN_RESIDENT_ID:" + DOCUMENT + "'");
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
}
