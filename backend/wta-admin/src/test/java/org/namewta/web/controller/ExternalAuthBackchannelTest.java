package org.namewta.web.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.web.service.social.ExternalAuthService;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 后台退出以 HTTP 状态决定重试，不能由业务 R 包装把失败变成成功确认。 */
@Tag("dev")
class ExternalAuthBackchannelTest {
    @Test
    void invalidAndMissingTokensReturn400WhileUnavailableDependenciesReturn503() throws Exception {
        var service = mock(ExternalAuthService.class);
        var mvc =
                MockMvcBuilders.standaloneSetup(new ExternalAuthController(service, null, null))
                        .build();
        String path = "/auth/social/backchannel/17";
        mvc.perform(post(path).contentType("application/x-www-form-urlencoded"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
        verifyNoInteractions(service);
        doThrow(new ServiceException("invalid signature")).when(service).backchannel(17, "invalid");
        mvc.perform(
                        post(path)
                                .contentType("application/x-www-form-urlencoded")
                                .param("logout_token", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
        doThrow(new ServiceException("JWKS unavailable", 503))
                .when(service)
                .backchannel(17, "jwks-failure");
        mvc.perform(
                        post(path)
                                .contentType("application/x-www-form-urlencoded")
                                .param("logout_token", "jwks-failure"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(""));
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(service)
                .backchannel(17, "redis-failure");
        mvc.perform(
                        post(path)
                                .contentType("application/x-www-form-urlencoded")
                                .param("logout_token", "redis-failure"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(""));
        mvc.perform(
                        post(path)
                                .contentType("application/x-www-form-urlencoded")
                                .param("logout_token", "valid"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(""));
    }
}
