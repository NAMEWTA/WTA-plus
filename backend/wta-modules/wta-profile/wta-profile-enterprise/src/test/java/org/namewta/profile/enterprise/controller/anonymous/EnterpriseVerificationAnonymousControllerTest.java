package org.namewta.profile.enterprise.controller.anonymous;

import cn.dev33.satoken.annotation.SaIgnore;
import org.namewta.profile.enterprise.config.EnterpriseVerificationProviderProperties;
import org.namewta.profile.enterprise.controller.advice.EnterpriseVerificationCallbackExceptionHandler;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVerificationApplicationRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVerificationAttemptRow;
import org.namewta.profile.enterprise.mapper.EnterpriseVerificationAttemptMapper;
import org.namewta.profile.enterprise.dao.EnterpriseVerificationAttemptDao;
import org.namewta.profile.enterprise.service.impl.EnterpriseDeterministicTestProvider;
import org.namewta.profile.enterprise.service.EnterpriseVerificationAttemptService;
import org.namewta.profile.enterprise.adapter.codec.EnterpriseVerificationEvidenceCodec;
import org.namewta.profile.enterprise.adapter.provider.EnterpriseVerificationProviderRegistry;
import org.namewta.profile.enterprise.service.impl.EnterpriseVerificationSecurityAuditRecorder;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseVerificationUseCaseImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class EnterpriseVerificationAnonymousControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    @Test
    void exposesSaIgnoredAuthenticatedCallbackAndStableFailureCategory() throws Exception {
        EnterpriseDeterministicTestProvider provider =
            new EnterpriseDeterministicTestProvider("enterprise-http-secret");
        EnterpriseVerificationAttemptMapper mapper = mapper();
        EnterpriseVerificationProviderProperties properties = new EnterpriseVerificationProviderProperties();
        properties.setEnabledProviders(Set.of("test-provider"));
        EnterpriseVerificationEvidenceCodec codec =
            new EnterpriseVerificationEvidenceCodec(JsonMapper.builder().build());
        EnterpriseVerificationAttemptService coordinator = new EnterpriseVerificationAttemptService(
            new EnterpriseVerificationProviderRegistry(List.of(provider), properties),
            new EnterpriseVerificationAttemptDao(mapper), codec,
            new EnterpriseVerificationSecurityAuditRecorder(new EnterpriseVerificationAttemptDao(mapper)));
        EnterpriseVerificationAnonymousController controller =
            new EnterpriseVerificationAnonymousController(new EnterpriseVerificationUseCaseImpl(coordinator), () -> NOW);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new EnterpriseVerificationCallbackExceptionHandler())
            .build();

        assertNotNull(EnterpriseVerificationAnonymousController.class
            .getMethod("callback", String.class, EnterpriseVerificationAnonymousController.CallbackRequest.class)
            .getAnnotation(SaIgnore.class));

        String signature = provider.sign("enterprise-71-1", NOW.getEpochSecond(), "approved");
        String acceptedBody = """
            {"providerRequestId":"enterprise-71-1","timestampEpochSecond":%d,
             "payload":"approved","signature":"%s"}
            """.formatted(NOW.getEpochSecond(), signature);
        mvc.perform(post("/profile/enterprise/verification/providers/test-provider/callback")
                .contentType(MediaType.APPLICATION_JSON).content(acceptedBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("ACCEPTED"));
        mvc.perform(post("/profile/enterprise/verification/providers/test-provider/callback")
                .contentType(MediaType.APPLICATION_JSON).content(acceptedBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("IDEMPOTENT"));

        String forgedBody = """
            {"providerRequestId":"enterprise-71-1","timestampEpochSecond":%d,
             "payload":"approved","signature":"00"}
            """.formatted(NOW.getEpochSecond());
        mvc.perform(post("/profile/enterprise/verification/providers/test-provider/callback")
                .contentType(MediaType.APPLICATION_JSON).content(forgedBody))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.data.category").value("INVALID_SIGNATURE"));
    }

    private EnterpriseVerificationAttemptMapper mapper() {
        EnterpriseVerificationAttemptMapper mapper = mock(EnterpriseVerificationAttemptMapper.class);
        EnterpriseVerificationApplicationRow application = new EnterpriseVerificationApplicationRow();
        application.setApplicationId(71L);
        application.setSubmissionId(801L);
        application.setProviderCode("test-provider");
        application.setStatus("WAITING");
        AtomicReference<EnterpriseVerificationAttemptRow> attempt = new AtomicReference<>(attempt());
        when(mapper.lockApplication(71L)).thenReturn(application);
        when(mapper.lockByProviderRequest("test-provider", "enterprise-71-1"))
            .thenAnswer(ignored -> attempt.get());
        when(mapper.completeAttempt(anyLong(), any(), any(), any(), any(), any()))
            .thenAnswer(invocation -> {
                EnterpriseVerificationAttemptRow row = attempt.get();
                row.setStatus(invocation.getArgument(1));
                row.setNormalizedResultJson(invocation.getArgument(2));
                row.setProviderEvidenceJson(invocation.getArgument(3));
                row.setErrorCode(invocation.getArgument(4));
                row.setCompletedTime(invocation.getArgument(5));
                return 1;
            });
        when(mapper.insertSecurityAudit(anyLong(), any(), any(), any(), any())).thenReturn(1);
        return mapper;
    }

    private EnterpriseVerificationAttemptRow attempt() {
        EnterpriseVerificationAttemptRow row = new EnterpriseVerificationAttemptRow();
        row.setVerificationAttemptId(191L);
        row.setApplicationId(71L);
        row.setSubmissionId(801L);
        row.setProviderCode("test-provider");
        row.setProviderRequestId("enterprise-71-1");
        row.setRequestFingerprint("fingerprint");
        row.setAttemptNo(1);
        row.setStatus("PENDING");
        return row;
    }
}
