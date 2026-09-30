package org.namewta.profile.person.service;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.profile.person.port.verification.PersonVerificationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.api.material.ProfileTaskMaterialPort;
import org.namewta.profile.person.controller.self.PersonApplicationController;
import org.namewta.profile.person.controller.self.PersonApplicationExceptionHandler;
import org.namewta.profile.person.controller.self.PersonTaskReviewController;
import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.person.dao.PersonTaskReviewDao;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.mapper.PersonTaskReviewMapper;
import org.namewta.profile.person.port.gateway.PersonTaskReviewGateway;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.profile.person.port.provider.PersonVerificationProviderRegistryPort;
import org.namewta.profile.person.support.PersonMapperXmlTestSupport;
import org.namewta.profile.person.usecase.impl.PersonApplicationUseCaseImpl;
import org.namewta.profile.person.usecase.impl.PersonTaskReviewUseCaseImpl;
import org.namewta.system.api.ConfigService;
import org.namewta.workflow.api.domain.WorkflowTaskReviewContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 真实 HTTP 入口、UseCase、Service、DAO、Mapper/XML；外部工作流与材料网关显式使用替身。 */
@Tag("dev")
@EnabledIfEnvironmentVariable(named = "PROFILE_TASK_REVIEW_MYSQL_URL", matches = ".+")
class PersonTaskReviewMySqlIntegrationTest {
    private static final long APPLICATION = 965990001L;
    private static final long SUBMISSION = 965990002L;
    private static final long OLD_SUBMISSION = 965990003L;
    private static final long PROFILE = 965990004L;
    private static final long VERSION = 965990005L;
    private static final long BINDING = 965990006L;
    private static final long SELF_APPLICATION = 965990007L;
    private static final long FOREIGN_APPLICATION = 965990008L;
    private static final long FOREIGN_SUBMISSION = 965990009L;

    @Test
    void boundSnapshotReturnAndSelfCertificationAreEnforcedByRealMapperQueries() throws Exception {
        try (SqlSession session = session()) {
          try {
            seed(session);
            var workflow = mock(PersonTaskReviewGateway.class);
            var material = mock(ProfileTaskMaterialPort.class);
            var review = new PersonTaskReviewController(new PersonTaskReviewUseCaseImpl(new PersonTaskReviewService(
                new PersonTaskReviewDao(session.getMapper(PersonTaskReviewMapper.class)), workflow, material)));
            var application = new PersonApplicationController(new PersonApplicationUseCaseImpl(new PersonApplicationService(
                new PersonApplicationDao(session.getMapper(PersonApplicationMapper.class)), mock(ProfileMaterialPort.class),
                mock(PersonVerificationProviderRegistryPort.class), mock(PersonVerificationService.class),
                mock(PersonWorkflowGateway.class), mock(ConfigService.class))));
            var mvc = MockMvcBuilders.standaloneSetup(review, application)
                .setControllerAdvice(new PersonApplicationExceptionHandler()).build();
            var user = new AtomicLong(101L);
            try (var login = mockStatic(LoginHelper.class)) {
                login.when(LoginHelper::getUserId).thenAnswer(ignored -> user.get());
                mvc.perform(get("/profile/person/application/summary")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                    .andExpect(jsonPath("$.data.certifiedProfile.identity.fullName").value("本人实名"))
                    .andExpect(jsonPath("$.data.currentApplication").doesNotExist());
                user.set(999L);
                mvc.perform(get("/profile/person/application/summary"))
                    .andExpect(jsonPath("$.data.status").value("UNVERIFIED"))
                    .andExpect(jsonPath("$.data.certifiedProfile").doesNotExist());
                user.set(101L);
                for (String inactive : new String[] {
                    "update profile_person_binding set status='SUSPENDED' where person_binding_id=" + BINDING,
                    "update profile_person set status='REVOKED' where person_profile_id=" + PROFILE,
                    "update profile_person_version set status='SUPERSEDED' where person_version_id=" + VERSION,
                    "update profile_person_binding set del_flag='1' where person_binding_id=" + BINDING}) {
                    execute(session, inactive);
                    session.clearCache();
                    mvc.perform(get("/profile/person/application/summary"))
                        .andExpect(jsonPath("$.data.status").value("UNVERIFIED"));
                    execute(session, "update profile_person_binding set status='ACTIVE',del_flag='0' where person_binding_id=" + BINDING);
                    execute(session, "update profile_person set status='ACTIVE' where person_profile_id=" + PROFILE);
                    execute(session, "update profile_person_version set status='CURRENT' where person_version_id=" + VERSION);
                    session.clearCache();
                }
                insertApplication(session, SELF_APPLICATION, 101L, "DRAFT", 0);
                session.clearCache();
                mvc.perform(get("/profile/person/application/summary"))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"))
                    .andExpect(jsonPath("$.data.certifiedProfile.profileId").value(PROFILE));
                execute(session, "update profile_person_application set status='BACK',decision_reason='请补正证件',update_time=now() where person_application_id=" + SELF_APPLICATION);
                session.clearCache();
                mvc.perform(get("/profile/person/application/summary"))
                    .andExpect(jsonPath("$.data.status").value("BACK"))
                    .andExpect(jsonPath("$.data.returnReason").value("请补正证件"));

                when(workflow.context(701L, false)).thenReturn(context(SUBMISSION, 2));
                when(workflow.context(701L, true)).thenReturn(context(SUBMISSION, 2));
                mvc.perform(get("/profile/person/review/tasks/701"))
                    .andExpect(jsonPath("$.data.submissionId").value(SUBMISSION))
                    .andExpect(jsonPath("$.data.submissionSeq").value(2));
                mvc.perform(post("/profile/person/review/tasks/701/decision").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"decision\":\"RETURN\",\"reason\":\"补齐正反面\",\"snapshotVersion\":2}"))
                    .andExpect(jsonPath("$.code").value(200));
                assertThat(scalar(session, "select decision_reason from profile_person_application where person_application_id=" + APPLICATION))
                    .isEqualTo("补齐正反面");
                assertThat(scalar(session, "select decision_result from profile_person_application where person_application_id=" + APPLICATION)).isNull();
                verify(workflow).returnToApplicant(701L, "补齐正反面");
                verify(workflow, never()).approve(anyLong(), anyString());

                when(workflow.context(701L, false)).thenReturn(context(FOREIGN_SUBMISSION, 2));
                mvc.perform(get("/profile/person/review/tasks/701"))
                    .andExpect(jsonPath("$.msg").value("PERSON_REVIEW_SNAPSHOT_CONFLICT"));
                when(workflow.context(701L, false)).thenReturn(context(SUBMISSION, 9));
                mvc.perform(get("/profile/person/review/tasks/701"))
                    .andExpect(jsonPath("$.msg").value("PERSON_REVIEW_SNAPSHOT_CONFLICT"));

                when(workflow.context(701L, false)).thenReturn(context(OLD_SUBMISSION, 1));
                when(workflow.context(701L, true)).thenReturn(context(OLD_SUBMISSION, 1));
                mvc.perform(get("/profile/person/review/tasks/701"))
                    .andExpect(jsonPath("$.data.submissionId").value(OLD_SUBMISSION));
                mvc.perform(post("/profile/person/review/tasks/701/decision").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"decision\":\"APPROVE\",\"reason\":\"旧快照不能办\",\"snapshotVersion\":1}"))
                    .andExpect(jsonPath("$.msg").value("PERSON_REVIEW_SNAPSHOT_CONFLICT"));
                verify(workflow, never()).approve(anyLong(), anyString());
            }
          } finally {
              // Seed statements use JDBC directly, so force rollback even before MyBatis marks the session dirty.
              session.rollback(true);
          }
        }
    }

    private WorkflowTaskReviewContext context(long submissionId, int sequence) {
        return new WorkflowTaskReviewContext(701L, 401L, String.valueOf(APPLICATION), "profile_person", 20L, sequence, submissionId);
    }

    private SqlSession session() {
        var dataSource = new UnpooledDataSource("com.mysql.cj.jdbc.Driver", System.getenv("PROFILE_TASK_REVIEW_MYSQL_URL"),
            System.getenv("PROFILE_TASK_REVIEW_MYSQL_USERNAME"), System.getenv("PROFILE_TASK_REVIEW_MYSQL_PASSWORD"));
        var configuration = new Configuration(new Environment("profile-task-review", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        PersonMapperXmlTestSupport.parse(configuration, PersonApplicationMapper.class);
        PersonMapperXmlTestSupport.parse(configuration, PersonTaskReviewMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration).openSession(false);
    }

    private void seed(SqlSession session) throws SQLException {
        insertApplication(session, APPLICATION, 102L, "WAITING", 2);
        insertApplication(session, FOREIGN_APPLICATION, 103L, "WAITING", 2);
        insertSubmission(session, OLD_SUBMISSION, APPLICATION, 1);
        insertSubmission(session, SUBMISSION, APPLICATION, 2);
        insertSubmission(session, FOREIGN_SUBMISSION, FOREIGN_APPLICATION, 2);
        String fields = "full_name,document_type_code,document_number,identity_key,gender,birth_date";
        String values = "'本人实名','CN_RESIDENT_ID','110101199001011234','profile-task-review-person','MALE','1990-01-01'";
        execute(session, "insert into profile_person(person_profile_id,current_version_id,status," + fields + ") values("
            + PROFILE + "," + VERSION + ",'ACTIVE'," + values + ")");
        execute(session, "insert into profile_person_version(person_version_id,person_profile_id,version_no,source_type,source_id,status,published_time,"
            + fields + ") values(" + VERSION + "," + PROFILE + ",1,'USER_SUBMISSION'," + SUBMISSION + ",'CURRENT',now()," + values + ")");
        execute(session, "insert into profile_person_binding(person_binding_id,person_profile_id,user_id,status,bound_time) values("
            + BINDING + "," + PROFILE + ",101,'ACTIVE',now())");
    }

    private void insertApplication(SqlSession session, long id, long user, String status, int sequence) throws SQLException {
        try (var statement = session.getConnection().prepareStatement("insert into profile_person_application"
            + "(person_application_id,applicant_user_id,status,provider_code,submission_seq) values(?,?,?,'manual',?)")) {
            statement.setLong(1, id); statement.setLong(2, user); statement.setString(3, status); statement.setInt(4, sequence);
            statement.executeUpdate();
        }
    }

    private void insertSubmission(SqlSession session, long id, long application, int sequence) throws SQLException {
        try (var statement = session.getConnection().prepareStatement("insert into profile_person_submission"
            + "(person_submission_id,person_application_id,submission_seq,full_name,document_type_code,document_number,identity_key,gender,birth_date,provider_code,field_snapshot_json,submitted_time)"
            + " values(?,?,?,'申请人','CN_RESIDENT_ID','110101199001011234','profile-task-review-person','MALE','1990-01-01','manual','{}',now())")) {
            statement.setLong(1, id); statement.setLong(2, application); statement.setInt(3, sequence); statement.executeUpdate();
        }
    }

    private void execute(SqlSession session, String sql) throws SQLException {
        try (var statement = session.getConnection().createStatement()) { statement.executeUpdate(sql); }
    }

    private String scalar(SqlSession session, String sql) throws SQLException {
        try (var statement = session.getConnection().createStatement(); var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }
}
