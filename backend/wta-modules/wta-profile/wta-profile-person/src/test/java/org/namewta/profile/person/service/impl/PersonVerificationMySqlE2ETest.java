package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.service.impl.PersonVerificationSecurityAuditRecorder;

import org.namewta.profile.person.adapter.codec.PersonVerificationEvidenceCodec;

import org.namewta.profile.person.service.PersonVerificationAttemptService;

import org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry;
import org.namewta.profile.person.service.impl.PersonVerificationSecurityAuditRecorder;

import org.namewta.profile.person.controller.anonymous.PersonVerificationCallbackExceptionHandler;
import org.namewta.profile.person.controller.anonymous.PersonVerificationAnonymousController;
import org.namewta.profile.person.usecase.impl.PersonVerificationUseCaseImpl;
import org.namewta.profile.person.domain.verification.PersonVerificationAttempt;
import org.namewta.profile.person.domain.verification.PersonVerificationStartAttemptCommand;
import org.namewta.profile.person.config.PersonVerificationProviderProperties;
import org.namewta.profile.person.support.PersonVerificationTimeSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.profile.person.mapper.PersonVerificationAttemptMapper;
import org.namewta.profile.person.dao.PersonVerificationAttemptDao;
import org.namewta.profile.person.support.PersonMapperXmlTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "PROFILE_MYSQL_E2E_URL", matches = ".+")
class PersonVerificationMySqlE2ETest {

    private static final long APPLICATION_ID = 930000000001L;
    private static final long SUBMISSION_ID = 930000000002L;
    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    @Test
    void authenticatesHttpCallbackAndPersistsIdempotentEvidenceWithoutPublishing() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            seedApplication(session);
            PersonVerificationAttemptMapper mapper = session.getMapper(PersonVerificationAttemptMapper.class);
            PersonVerificationEvidenceCodec evidenceCodec =
                new PersonVerificationEvidenceCodec(JsonMapper.builder().build());
            PersonDeterministicTestProvider provider =
                new PersonDeterministicTestProvider("person-mysql-e2e-secret");
            PersonVerificationProviderProperties properties = new PersonVerificationProviderProperties();
            properties.setEnabledProviders(Set.of("test-provider"));
            PersonVerificationAttemptService coordinator = new PersonVerificationAttemptService(
                new PersonVerificationProviderRegistry(List.of(provider), properties),
                new PersonVerificationAttemptDao(mapper), evidenceCodec,
                new PersonVerificationSecurityAuditRecorder(new PersonVerificationAttemptDao(mapper)));
            PersonVerificationAttempt attempt = coordinator.startAttempt(
                new PersonVerificationStartAttemptCommand(APPLICATION_ID, SUBMISSION_ID, "person-fingerprint"));
            session.commit();

            MockMvc mvc = MockMvcBuilders.standaloneSetup(
                    new PersonVerificationAnonymousController(new PersonVerificationUseCaseImpl(coordinator), () -> NOW))
                .setControllerAdvice(new PersonVerificationCallbackExceptionHandler())
                .build();
            String accepted = callbackBody(provider, attempt.providerRequestId(), "approved");
            mvc.perform(post("/profile/person/verification/providers/test-provider/callback")
                    .contentType(MediaType.APPLICATION_JSON).content(accepted))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ACCEPTED"));
            session.commit();
            mvc.perform(post("/profile/person/verification/providers/test-provider/callback")
                    .contentType(MediaType.APPLICATION_JSON).content(accepted))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("IDEMPOTENT"));
            session.commit();

            String conflicting = callbackBody(provider, attempt.providerRequestId(), "rejected");
            mvc.perform(post("/profile/person/verification/providers/test-provider/callback")
                    .contentType(MediaType.APPLICATION_JSON).content(conflicting))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.category").value("CONFLICTING_CALLBACK"));
            session.commit();

            assertEquals("SUCCEEDED", scalar(session,
                "select status from profile_verification_attempt where application_id = " + APPLICATION_ID));
            assertEquals("WAITING", scalar(session,
                "select status from profile_person_application where person_application_id = " + APPLICATION_ID));
            assertEquals("1", scalar(session,
                "select count(*) from profile_verification_attempt where application_id = " + APPLICATION_ID));
            assertEquals("1", scalar(session,
                "select count(*) from profile_operation_audit where application_id = " + APPLICATION_ID
                    + " and failure_category = 'CONFLICTING_CALLBACK'"));
            String storedEvidence = scalar(session,
                "select provider_evidence_json from profile_verification_attempt where application_id = "
                    + APPLICATION_ID);
            assertFalse(storedEvidence.contains("signature"));
        }
    }

    private SqlSessionFactory sessionFactory() {
        UnpooledDataSource dataSource = new UnpooledDataSource(
            "com.mysql.cj.jdbc.Driver",
            System.getenv("PROFILE_MYSQL_E2E_URL"),
            System.getenv("PROFILE_MYSQL_E2E_USERNAME"),
            System.getenv("PROFILE_MYSQL_E2E_PASSWORD"));
        Configuration configuration = new Configuration(new Environment(
            "profile-person-e2e", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        PersonMapperXmlTestSupport.parse(configuration, PersonVerificationAttemptMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private void seedApplication(SqlSession session) throws Exception {
        try (PreparedStatement application = session.getConnection().prepareStatement("""
            insert into profile_person_application (
                person_application_id, applicant_user_id, status, full_name,
                document_type_code, document_number, identity_key, gender, birth_date,
                provider_code, submission_seq, version, create_time, update_time, del_flag
            ) values (?, 930000000003, 'WAITING', 'E2E Person', 'CN_ID_CARD',
                '110101199001011234', 'CN_ID_CARD:110101199001011234', 'MALE', '1990-01-01',
                'test-provider', 1, 0, current_timestamp, current_timestamp, '0')
            """)) {
            application.setLong(1, APPLICATION_ID);
            application.executeUpdate();
        }
        try (PreparedStatement submission = session.getConnection().prepareStatement("""
            insert into profile_person_submission (
                person_submission_id, person_application_id, submission_seq, full_name,
                document_type_code, document_number, identity_key, gender, birth_date,
                provider_code, field_snapshot_json, submitted_time, version,
                create_time, update_time, del_flag
            ) values (?, ?, 1, 'E2E Person', 'CN_ID_CARD', '110101199001011234',
                'CN_ID_CARD:110101199001011234', 'MALE', '1990-01-01', 'test-provider',
                '{}', current_timestamp, 0, current_timestamp, current_timestamp, '0')
            """)) {
            submission.setLong(1, SUBMISSION_ID);
            submission.setLong(2, APPLICATION_ID);
            submission.executeUpdate();
        }
        session.commit();
    }

    private String callbackBody(PersonDeterministicTestProvider provider,
                                String requestId,
                                String payload) {
        String signature = provider.sign(requestId, NOW.getEpochSecond(), payload);
        return """
            {"providerRequestId":"%s","timestampEpochSecond":%d,
             "payload":"%s","signature":"%s"}
            """.formatted(requestId, NOW.getEpochSecond(), payload, signature);
    }

    private String scalar(SqlSession session, String sql) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getString(1);
        }
    }
}
