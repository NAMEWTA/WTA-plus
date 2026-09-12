package org.namewta.profile.person.controller.anonymous;

import org.namewta.profile.person.domain.verification.PersonApplicationVerificationState;
import org.namewta.profile.person.domain.verification.PersonProviderAttemptStatus;
import org.namewta.profile.person.domain.verification.PersonVerificationAttempt;
import org.namewta.profile.person.dao.PersonVerificationAttemptDao;
import org.namewta.profile.person.config.PersonVerificationProviderProperties;
import org.namewta.profile.person.service.impl.PersonVerificationSecurityAuditRecorder;
import org.namewta.profile.person.service.impl.PersonDeterministicTestProvider;
import org.namewta.profile.person.service.PersonVerificationAttemptService;
import org.namewta.profile.person.service.impl.PersonVerificationMapperFixture;
import org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry;
import org.namewta.profile.person.usecase.impl.PersonVerificationUseCaseImpl;
import cn.dev33.satoken.annotation.SaIgnore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class PersonVerificationAnonymousControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    @Test
    void exposesSaIgnoredAuthenticatedCallbackAndStableFailureCategory() throws Exception {
        PersonDeterministicTestProvider provider = new PersonDeterministicTestProvider("person-http-secret");
        PersonVerificationMapperFixture fixture = new PersonVerificationMapperFixture(
            new PersonApplicationVerificationState(41L, 501L, "test-provider", "WAITING"));
        fixture.addAttempt(new PersonVerificationAttempt(
            91L, 41L, 501L, "test-provider", "person-41-1", "fingerprint", null, 1,
            PersonProviderAttemptStatus.PENDING, null, null, null, null));
        PersonVerificationProviderProperties properties = new PersonVerificationProviderProperties();
        properties.setEnabledProviders(Set.of("test-provider"));
        PersonVerificationAttemptService coordinator = new PersonVerificationAttemptService(
            new PersonVerificationProviderRegistry(List.of(provider), properties),
            new PersonVerificationAttemptDao(fixture.mapper()), fixture.evidenceCodec(),
            new PersonVerificationSecurityAuditRecorder(new PersonVerificationAttemptDao(fixture.mapper())));
        PersonVerificationAnonymousController controller =
            new PersonVerificationAnonymousController(new PersonVerificationUseCaseImpl(coordinator), () -> NOW);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new PersonVerificationCallbackExceptionHandler())
            .build();

        assertNotNull(PersonVerificationAnonymousController.class
            .getMethod("callback", String.class, PersonVerificationAnonymousController.CallbackRequest.class)
            .getAnnotation(SaIgnore.class));

        String signature = provider.sign("person-41-1", NOW.getEpochSecond(), "approved");
        String acceptedBody = """
            {"providerRequestId":"person-41-1","timestampEpochSecond":%d,
             "payload":"approved","signature":"%s"}
            """.formatted(NOW.getEpochSecond(), signature);
        mvc.perform(post("/profile/person/verification/providers/test-provider/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(acceptedBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("ACCEPTED"));
        mvc.perform(post("/profile/person/verification/providers/test-provider/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(acceptedBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("IDEMPOTENT"));

        String forgedBody = """
            {"providerRequestId":"person-41-1","timestampEpochSecond":%d,
             "payload":"approved","signature":"00"}
            """.formatted(NOW.getEpochSecond());
        mvc.perform(post("/profile/person/verification/providers/test-provider/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(forgedBody))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.data.category").value("INVALID_SIGNATURE"));
    }

}
