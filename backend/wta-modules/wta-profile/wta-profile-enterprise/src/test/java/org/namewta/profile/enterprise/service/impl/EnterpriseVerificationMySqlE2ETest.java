package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.service.impl.EnterpriseVerificationSecurityAuditRecorder;

import org.namewta.profile.enterprise.adapter.codec.EnterpriseVerificationEvidenceCodec;

import org.namewta.profile.enterprise.service.EnterpriseVerificationAttemptService;

import org.namewta.profile.enterprise.adapter.provider.EnterpriseVerificationProviderRegistry;

import org.namewta.profile.enterprise.controller.advice.EnterpriseVerificationCallbackExceptionHandler;
import org.namewta.profile.enterprise.controller.anonymous.EnterpriseVerificationAnonymousController;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseVerificationUseCaseImpl;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationAttempt;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationStartAttemptCommand;
import org.namewta.profile.enterprise.config.EnterpriseVerificationProviderProperties;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.profile.enterprise.mapper.EnterpriseVerificationAttemptMapper;
import org.namewta.profile.enterprise.dao.EnterpriseVerificationAttemptDao;
import org.namewta.profile.enterprise.support.EnterpriseMapperXmlTestSupport;
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
class EnterpriseVerificationMySqlE2ETest {

    private static final long APPLICATION_ID = 940000000001L;
    private static final long SUBMISSION_ID = 940000000002L;
    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    @Test
    void authenticatesHttpCallbackAndPersistsIdempotentEvidenceWithoutPublishing() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            seedApplication(session);
            EnterpriseVerificationAttemptMapper mapper =
                session.getMapper(EnterpriseVerificationAttemptMapper.class);
            EnterpriseVerificationEvidenceCodec codec =
                new EnterpriseVerificationEvidenceCodec(JsonMapper.builder().build());
            EnterpriseDeterministicTestProvider provider =
                new EnterpriseDeterministicTestProvider("enterprise-mysql-e2e-secret");
            EnterpriseVerificationProviderProperties properties = new EnterpriseVerificationProviderProperties();
            properties.setEnabledProviders(Set.of("test-provider"));
            EnterpriseVerificationAttemptService coordinator = new EnterpriseVerificationAttemptService(
                new EnterpriseVerificationProviderRegistry(List.of(provider), properties),
                new EnterpriseVerificationAttemptDao(mapper),
                codec,
                new EnterpriseVerificationSecurityAuditRecorder(new EnterpriseVerificationAttemptDao(mapper)));
            EnterpriseVerificationAttempt attempt = coordinator.startAttempt(
                new EnterpriseVerificationStartAttemptCommand(
                    APPLICATION_ID, SUBMISSION_ID, "enterprise-fingerprint"));
            session.commit();

            MockMvc mvc = MockMvcBuilders.standaloneSetup(
                    new EnterpriseVerificationAnonymousController(new EnterpriseVerificationUseCaseImpl(coordinator), () -> NOW))
                .setControllerAdvice(new EnterpriseVerificationCallbackExceptionHandler())
                .build();
            String accepted = callbackBody(provider, attempt.providerRequestId(), "approved");
            mvc.perform(post("/profile/enterprise/verification/providers/test-provider/callback")
                    .contentType(MediaType.APPLICATION_JSON).content(accepted))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ACCEPTED"));
            session.commit();
            mvc.perform(post("/profile/enterprise/verification/providers/test-provider/callback")
                    .contentType(MediaType.APPLICATION_JSON).content(accepted))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("IDEMPOTENT"));
            session.commit();

            String conflicting = callbackBody(provider, attempt.providerRequestId(), "rejected");
            mvc.perform(post("/profile/enterprise/verification/providers/test-provider/callback")
                    .contentType(MediaType.APPLICATION_JSON).content(conflicting))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.category").value("CONFLICTING_CALLBACK"));
            session.commit();

            assertEquals("SUCCEEDED", scalar(session,
                "select status from profile_verification_attempt where application_id = " + APPLICATION_ID));
            assertEquals("WAITING", scalar(session,
                "select status from profile_enterprise_application where enterprise_application_id = "
                    + APPLICATION_ID));
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
            "profile-enterprise-e2e", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(EnterpriseVerificationAttemptMapper.class);
        EnterpriseMapperXmlTestSupport.parse(configuration, EnterpriseVerificationAttemptMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private void seedApplication(SqlSession session) throws Exception {
        try (PreparedStatement application = session.getConnection().prepareStatement("""
            insert into profile_enterprise_application (
                enterprise_application_id, applicant_user_id, status, enterprise_name,
                unified_credit_code, identity_key, enterprise_type,
                legal_representative_name, legal_document_type_code, legal_document_number,
                established_date, registered_address, business_scope,
                provider_code, submission_seq, version, create_time, update_time, del_flag
            ) values (?, 940000000003, 'WAITING', 'E2E Enterprise Ltd',
                '91310000E2E000001Q', '91310000E2E000001Q', 'LIMITED_COMPANY',
                'E2E Legal', 'CN_ID_CARD', '110101199001011234', '2020-01-01',
                'E2E Address', 'E2E Scope', 'test-provider', 1, 0,
                current_timestamp, current_timestamp, '0')
            """)) {
            application.setLong(1, APPLICATION_ID);
            application.executeUpdate();
        }
        try (PreparedStatement submission = session.getConnection().prepareStatement("""
            insert into profile_enterprise_submission (
                enterprise_submission_id, enterprise_application_id, submission_seq,
                enterprise_name, unified_credit_code, identity_key, enterprise_type,
                legal_representative_name, legal_document_type_code, legal_document_number,
                handler_is_legal_representative, established_date, registered_address,
                business_scope, provider_code, field_snapshot_json, submitted_time,
                version, create_time, update_time, del_flag
            ) values (?, ?, 1, 'E2E Enterprise Ltd', '91310000E2E000001Q',
                '91310000E2E000001Q', 'LIMITED_COMPANY', 'E2E Legal', 'CN_ID_CARD',
                '110101199001011234', 'Y', '2020-01-01', 'E2E Address', 'E2E Scope',
                'test-provider', '{}', current_timestamp, 0,
                current_timestamp, current_timestamp, '0')
            """)) {
            submission.setLong(1, SUBMISSION_ID);
            submission.setLong(2, APPLICATION_ID);
            submission.executeUpdate();
        }
        session.commit();
    }

    private String callbackBody(EnterpriseDeterministicTestProvider provider,
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
