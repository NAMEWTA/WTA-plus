package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminAssignBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminBindingBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminCreateBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminDecisionBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminIdentityBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminQueryBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminReviseBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminRevokeBo;

import org.namewta.profile.enterprise.controller.admin.EnterpriseAdminController;
import org.namewta.profile.enterprise.controller.advice.EnterpriseAdminExceptionHandler;
import org.namewta.profile.enterprise.domain.exception.EnterpriseAdminException;
import org.namewta.profile.enterprise.mapper.EnterpriseAdminMapper;
import org.namewta.profile.enterprise.service.IEnterpriseApplicationService;
import org.namewta.profile.enterprise.support.EnterpriseMapperXmlTestSupport;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.profile.api.ProfileService;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialReferenceView;
import org.namewta.profile.enterprise.domain.application.EnterpriseDocumentTypeRule;
import org.namewta.system.api.OssService;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.namewta.workflow.api.WorkflowService;
import org.namewta.workflow.api.domain.WorkflowTerminationResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
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
class EnterpriseAdminMySqlE2ETest {

    private static final long OPERATOR = 979000000001L;
    private static final long USER = 979000000002L;
    private static final long REJECT_APPLICATION = 979000000011L;
    private static final long REJECT_SUBMISSION = 979000000012L;
    private static final long FAIL_APPLICATION = 979000000021L;
    private static final long FAIL_SUBMISSION = 979000000022L;
    private static final String CREDIT = "91310000E2EADM9901";
    private static final Instant NOW = Instant.parse("2026-09-02T02:00:00Z");

    @Test
    void persistsAdminLifecycleQualifiedOwnerDecisionFenceAndRollback() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            cleanup(session);
            session.commit(true);

            EnterpriseAdminMapper mapper = session.getMapper(EnterpriseAdminMapper.class);
            IEnterpriseApplicationService applications = mock(IEnterpriseApplicationService.class);
            when(applications.findDocumentType("CN_RESIDENT_ID"))
                .thenReturn(Optional.of(new EnterpriseDocumentTypeRule("CN_RESIDENT_ID", "^[0-9]{17}[0-9X]$", true)));
            ProfileMaterialPort materials = materials();
            WorkflowService workflow = mock(WorkflowService.class);
            when(workflow.terminateInstance(eq(Long.toString(REJECT_APPLICATION)), anyString()))
                .thenReturn(new WorkflowTerminationResult(WorkflowTerminationResult.Status.TERMINATED, 702L));
            when(workflow.terminateInstance(eq(Long.toString(FAIL_APPLICATION)), anyString()))
                .thenThrow(new IllegalStateException("workflow unavailable"));
            UserService users = mock(UserService.class);
            when(users.selectById(anyLong())).thenAnswer(invocation -> normalUser(invocation.getArgument(0)));
            ProfileService profiles = mock(ProfileService.class);
            when(profiles.findByUserId(USER)).thenReturn(new ProfileSummary(USER,
                new ProfileBindingSummary(799000000001L, ProfileType.PERSON, NOW.minusSeconds(60)), null));
            EnterpriseAdminServiceImpl service = new EnterpriseAdminServiceImpl(mapper,
                JsonMapper.builder().build(), applications, materials, workflow, users, profiles,
                Clock.fixed(NOW, ZoneOffset.UTC));

            var identity = identity("管理直建企业", "软件开发");
            var created = service.create(OPERATOR, new EnterpriseAdminCreateBo(identity, USER,
                "e2e admin create", List.of()));
            session.commit(true);
            assertThat(created.status()).isEqualTo("ACTIVE");
            assertThat(scalar(session, "select source_type from profile_enterprise_version where enterprise_version_id="
                + created.versionId())).isEqualTo("ADMIN_CREATE");
            assertThat(scalar(session, "select status from profile_enterprise_binding where enterprise_binding_id="
                + created.bindingId())).isEqualTo("ACTIVE");

            var revised = service.revise(OPERATOR, created.profileId(),
                new EnterpriseAdminReviseBo(identity("管理修订企业", "软件开发与技术服务"),
                    "e2e admin override", 1));
            session.commit(true);
            assertThat(scalar(session, "select count(*) from profile_enterprise_version where enterprise_profile_id="
                + created.profileId())).isEqualTo("2");
            assertThat(scalar(session, "select count(*) from profile_enterprise_version where enterprise_profile_id="
                + created.profileId() + " and status='CURRENT' and source_type='ADMIN_OVERRIDE'"))
                .isEqualTo("1");

            var suspended = service.manageBinding(OPERATOR, created.profileId(),
                new EnterpriseAdminBindingBo("SUSPEND", "e2e suspend", 1));
            var resumed = service.manageBinding(OPERATOR, created.profileId(),
                new EnterpriseAdminBindingBo("RESUME", "e2e resume", suspended.version()));
            service.manageBinding(OPERATOR, created.profileId(),
                new EnterpriseAdminBindingBo("UNBIND", "e2e unbind", resumed.version()));
            var assigned = service.assign(OPERATOR, created.profileId(),
                new EnterpriseAdminAssignBo(USER, "e2e assign"));
            session.commit(true);
            assertThat(scalar(session, "select count(*) from profile_enterprise_binding_event where enterprise_profile_id="
                + created.profileId())).isEqualTo("5");

            when(profiles.findByUserId(USER)).thenReturn(ProfileSummary.unverified(USER));
            service.manageBinding(OPERATOR, created.profileId(),
                new EnterpriseAdminBindingBo("UNBIND", "e2e owner eligibility", assigned.version()));
            assertThatThrownBy(() -> service.assign(OPERATOR, created.profileId(),
                new EnterpriseAdminAssignBo(USER, "must have person profile")))
                .isInstanceOf(EnterpriseAdminException.class)
                .hasMessage("ENTERPRISE_BINDING_TARGET_INELIGIBLE");
            session.rollback(true);

            MockMvc mvc = MockMvcBuilders.standaloneSetup(new EnterpriseAdminController(service))
                .setControllerAdvice(new EnterpriseAdminExceptionHandler()).build();
            mvc.perform(get("/profile/enterprise/archive/{profileId}", created.profileId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.enterpriseName").value("管理修订企业"))
                .andExpect(jsonPath("$.data.currentMaterials[0].fileName").value("e2e-license.jpg"));
            mvc.perform(get("/profile/enterprise/archive/{profileId}/material/{materialRefId}/access-url",
                    created.profileId(), 8901L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessType").value("PRIVATE"));

            var revoked = service.revoke(OPERATOR, created.profileId(),
                new EnterpriseAdminRevokeBo("e2e revoke", revised.version()));
            session.commit(true);
            assertThat(revoked.status()).isEqualTo("REVOKED");
            assertThat(service.page(new EnterpriseAdminQueryBo(null, CREDIT, null, 1, 20)).getTotal())
                .isZero();
            assertThat(service.page(new EnterpriseAdminQueryBo(null, CREDIT, "REVOKED", 1, 20)).getTotal())
                .isEqualTo(1);
            assertThat(service.detail(created.profileId()).currentMaterials()).hasSize(1);
            assertThatThrownBy(() -> service.revise(OPERATOR, created.profileId(),
                new EnterpriseAdminReviseBo(identity, "must remain read only", 3)))
                .isInstanceOf(EnterpriseAdminException.class).hasMessage("ENTERPRISE_PROFILE_REVOKED_READ_ONLY");

            seedWaiting(session, REJECT_APPLICATION, REJECT_SUBMISSION, "91310000E2EREJ9901");
            session.commit(true);
            var rejection = service.decide(OPERATOR, REJECT_APPLICATION,
                new EnterpriseAdminDecisionBo("REJECTED", "e2e reject"));
            session.commit(true);
            assertThat(rejection.status()).isEqualTo("REJECTED");
            assertThat(scalar(session, "select concat(status,':',decision_version,':',decision_source)"
                + " from profile_enterprise_application where enterprise_application_id=" + REJECT_APPLICATION))
                .isEqualTo("INVALID:1:ADMIN_OVERRIDE");
            assertThat(scalar(session, "select decision_status from profile_decision_record where profile_type='ENTERPRISE'"
                + " and application_id=" + REJECT_APPLICATION)).isEqualTo("FINAL");

            seedWaiting(session, FAIL_APPLICATION, FAIL_SUBMISSION, "91310000E2EFAI9901");
            session.commit(true);
            assertThatThrownBy(() -> service.decide(OPERATOR, FAIL_APPLICATION,
                new EnterpriseAdminDecisionBo("REJECTED", "e2e fail")))
                .isInstanceOf(EnterpriseAdminException.class)
                .hasMessage("ENTERPRISE_ADMIN_WORKFLOW_TERMINATION_FAILED");
            session.rollback(true);
            assertThat(scalar(session, "select concat(status,':',decision_version) from profile_enterprise_application"
                + " where enterprise_application_id=" + FAIL_APPLICATION)).isEqualTo("WAITING:0");
            assertThat(scalar(session, "select count(*) from profile_decision_record where profile_type='ENTERPRISE'"
                + " and application_id=" + FAIL_APPLICATION)).isEqualTo("0");

            cleanup(session);
            session.commit(true);
            verify(workflow).terminateInstance(Long.toString(REJECT_APPLICATION), "e2e reject");
        }
    }

    private EnterpriseAdminIdentityBo identity(String name, String scope) {
        return new EnterpriseAdminIdentityBo(name, CREDIT, "COMPANY", "法定代表人",
            "CN_RESIDENT_ID", "110101199001019909", LocalDate.parse("2012-01-01"),
            LocalDate.parse("2012-01-01"), LocalDate.parse("2042-01-01"), "上海市测试路1号", scope,
            "联系人", "13800000000", "admin-e2e@example.invalid", new BigDecimal("1000.00"),
            "SOFTWARE", "https://example.invalid");
    }

    private ProfileMaterialPort materials() {
        ProfileMaterialPort materials = mock(ProfileMaterialPort.class);
        MaterialOwnerKey owner = new MaterialOwnerKey(ProfileType.ENTERPRISE, MaterialOwnerType.VERSION, 1L);
        MaterialReferenceView reference = new MaterialReferenceView(8901L, owner, 9902L, 7702L,
            "BUSINESS_LICENSE", "营业执照", "e2e-license.jpg", 2048, "jpg", "image/jpeg",
            true, true, NOW, null, 0);
        when(materials.list(any())).thenReturn(List.of(reference));
        when(materials.accessUrl(any(), eq(8901L)))
            .thenReturn(new OssService.OssAccessUrl("PRIVATE", "https://example.invalid/e2e", NOW.plusSeconds(60),
                "e2e-license.jpg"));
        when(materials.snapshotImmutable(any(), any())).thenReturn(List.of(reference));
        return materials;
    }

    private UserDTO normalUser(long userId) {
        UserDTO user = new UserDTO();
        user.setUserId(userId);
        user.setStatus("0");
        return user;
    }

    private void seedWaiting(SqlSession session, long applicationId, long submissionId, String credit) throws Exception {
        execute(session, "insert into profile_enterprise_application(enterprise_application_id,applicant_user_id,status,"
            + "enterprise_name,unified_credit_code,identity_key,enterprise_type,legal_representative_name,"
            + "legal_document_type_code,legal_document_number,handler_is_legal_representative,established_date,"
            + "registered_address,business_scope,provider_code,submission_seq,decision_version,version,create_dept,"
            + "create_time,create_by,update_time,update_by,del_flag) values(" + applicationId + ","
            + (applicationId + 1000) + ",'WAITING','审核企业','" + credit + "','" + credit
            + "','COMPANY','法定代表人','CN_RESIDENT_ID','110101199001019909','Y','2012-01-01',"
            + "'上海市测试路1号','软件','manual',1,0,0,-1,current_timestamp," + OPERATOR
            + ",current_timestamp," + OPERATOR + ",'0')");
        execute(session, "insert into profile_enterprise_submission(enterprise_submission_id,enterprise_application_id,"
            + "submission_seq,enterprise_name,unified_credit_code,identity_key,enterprise_type,legal_representative_name,"
            + "legal_document_type_code,legal_document_number,handler_is_legal_representative,established_date,"
            + "registered_address,business_scope,provider_code,field_snapshot_json,submitted_time,version,create_dept,"
            + "create_time,create_by,update_time,update_by,del_flag) values(" + submissionId + "," + applicationId
            + ",1,'审核企业','" + credit + "','" + credit + "','COMPANY','法定代表人','CN_RESIDENT_ID',"
            + "'110101199001019909','Y','2012-01-01','上海市测试路1号','软件','manual','{}',current_timestamp,"
            + "0,-1,current_timestamp," + OPERATOR + ",current_timestamp," + OPERATOR + ",'0')");
    }

    private SqlSessionFactory sessionFactory() {
        Configuration configuration = new Configuration(new Environment("enterprise-admin-e2e",
            new JdbcTransactionFactory(), new UnpooledDataSource("com.mysql.cj.jdbc.Driver",
            System.getenv("PROFILE_MYSQL_E2E_URL"), System.getenv("PROFILE_MYSQL_E2E_USERNAME"),
            System.getenv("PROFILE_MYSQL_E2E_PASSWORD"))));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(EnterpriseAdminMapper.class);
        EnterpriseMapperXmlTestSupport.parse(configuration, EnterpriseAdminMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private void cleanup(SqlSession session) throws Exception {
        execute(session, "delete from profile_operation_audit where operator_user_id=" + OPERATOR);
        execute(session, "delete from profile_decision_record where profile_type='ENTERPRISE' and application_id in ("
            + REJECT_APPLICATION + "," + FAIL_APPLICATION + ")");
        execute(session, "delete from profile_enterprise_submission where enterprise_application_id in ("
            + REJECT_APPLICATION + "," + FAIL_APPLICATION + ")");
        execute(session, "delete from profile_enterprise_application where enterprise_application_id in ("
            + REJECT_APPLICATION + "," + FAIL_APPLICATION + ")");
        execute(session, "delete from profile_enterprise_binding_event where enterprise_profile_id in ("
            + "select enterprise_profile_id from profile_enterprise where unified_credit_code='" + CREDIT + "')");
        execute(session, "delete from profile_enterprise_binding where enterprise_profile_id in ("
            + "select enterprise_profile_id from profile_enterprise where unified_credit_code='" + CREDIT + "')");
        execute(session, "delete from profile_enterprise_version where enterprise_profile_id in ("
            + "select enterprise_profile_id from profile_enterprise where unified_credit_code='" + CREDIT + "')");
        execute(session, "delete from profile_enterprise_source where enterprise_profile_id in ("
            + "select enterprise_profile_id from profile_enterprise where unified_credit_code='" + CREDIT + "')");
        execute(session, "delete from profile_enterprise where unified_credit_code='" + CREDIT + "'");
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
