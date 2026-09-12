package org.namewta.third.controller.admin;

import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import cn.dev33.satoken.interceptor.SaInterceptor;
import org.namewta.common.satoken.handler.SaTokenExceptionHandler;
import org.namewta.third.usecase.ThirdCredentialUseCase;
import org.namewta.third.usecase.ThirdEndpointUseCase;
import org.namewta.third.usecase.ThirdObservabilityUseCase;
import org.namewta.third.usecase.ThirdProviderUseCase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Tag("local")
class ThirdAdminPermissionHttpTest {

    @Test
    void rejectsUnauthenticatedRequestsBeforeAnyUseCaseInvocation() throws Exception {
        ThirdProviderUseCase providerUseCase = mock(ThirdProviderUseCase.class);
        ThirdEndpointUseCase endpointUseCase = mock(ThirdEndpointUseCase.class);
        ThirdCredentialUseCase credentialUseCase = mock(ThirdCredentialUseCase.class);
        ThirdObservabilityUseCase observabilityUseCase = mock(ThirdObservabilityUseCase.class);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ThirdProviderController(providerUseCase),
                new ThirdEndpointController(endpointUseCase),
                new ThirdCredentialController(credentialUseCase),
                new ThirdObservabilityController(observabilityUseCase))
            .addFilters(new SaTokenContextFilterForJakartaServlet())
            .addInterceptors(new SaInterceptor())
            .setControllerAdvice(new SaTokenExceptionHandler())
            .build();

        assertUnauthorized(mvc, get("/third/provider/list"));
        assertUnauthorized(mvc, post("/third/provider")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"));
        assertUnauthorized(mvc, get("/third/endpoint/list"));
        assertUnauthorized(mvc, get("/third/credential/list").param("providerCode", "QCC"));
        assertUnauthorized(mvc, get("/third/invocation/list").param("providerCode", "QCC"));
        assertUnauthorized(mvc, get("/third/statistics/list").param("providerCode", "QCC"));

        verifyNoInteractions(providerUseCase, endpointUseCase, credentialUseCase, observabilityUseCase);
    }

    private static void assertUnauthorized(MockMvc mvc, MockHttpServletRequestBuilder request) throws Exception {
        mvc.perform(request)
            .andExpect(jsonPath("$.code").value(401));
    }
}
