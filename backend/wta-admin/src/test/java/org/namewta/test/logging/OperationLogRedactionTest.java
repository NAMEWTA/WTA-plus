package org.namewta.test.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.model.LoginUser;
import org.springframework.context.ApplicationContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.aspect.LogAspect;
import org.namewta.common.log.event.OperLogEvent;
import org.namewta.sso.controller.anonymous.SsoOAuthController;
import org.namewta.sso.domain.bo.SsoTokenBo;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.namewta.system.controller.system.SysOssUploadController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class OperationLogRedactionTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void uploadAuditNeverCopiesPathTokenEvenWithoutMvcRouteMetadata(boolean mapped) throws Throwable {
        String canary = "upload-token-audit-canary";
        var request = new MockHttpServletRequest("POST", "/resource/oss/uploads/" + canary);
        String route = "/resource/oss/uploads/{uploadToken}";
        if (mapped) request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, route);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var publisher = mock(ApplicationContext.class);
        var point = mock(ProceedingJoinPoint.class);
        var signature = mock(Signature.class);
        when(point.getTarget()).thenReturn(this);
        when(signature.getName()).thenReturn("abort");
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(new Object[]{canary});
        when(point.proceed()).thenReturn(Map.of("data", canary));
        try (var login = mockStatic(LoginHelper.class); var spring = mockStatic(SpringUtils.class)) {
            spring.when(SpringUtils::context).thenReturn(publisher);
            var annotation = SysOssUploadController.class.getMethod("abort", String.class).getAnnotation(Log.class);
            assertThat(new LogAspect().doAround(point, annotation)).isEqualTo(Map.of("data", canary));
            var event = ArgumentCaptor.forClass(OperLogEvent.class);
            verify(publisher).publishEvent(event.capture());
            assertThat(event.getValue().getOperUrl()).isEqualTo(mapped ? route : "[unmapped]");
            assertThat(event.getValue().getTitle()).isEqualTo("OSS上传取消");
            assertThat(event.getValue().getRequestMethod()).isEqualTo("POST");
            assertThat(event.getValue().toString()).doesNotContain(canary);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void tokenIssuancePreservesAuditMetadataWithoutResponseBody() throws Exception {
        Log annotation = SsoOAuthController.class.getMethod("token", SsoTokenBo.class).getAnnotation(Log.class);
        assertThat(annotation.isSaveResponseData()).isFalse();
        OperLogEvent event = new OperLogEvent();
        event.setRequestMethod("POST");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sso/oauth2/token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            new LogAspect().getControllerMethodDescription(mock(JoinPoint.class), annotation, event,
                Map.of("access_token", "credential-canary-token"));
            assertThat(event.getJsonResult()).isNull();
            assertThat(event.getTitle()).isEqualTo("SSO换票");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
    @Log(title = "审计失败元数据", isSaveRequestData = false)
    void auditedFailure() {
    }

    @Test
    void preservesOperatorDurationAndFailureStatusWithoutExceptionPayload() throws Throwable {
        String canary = "credential-canary-error";
        RuntimeException failure = new IllegalArgumentException(canary);
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.proceed()).thenThrow(failure);
        when(point.getTarget()).thenReturn(this);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("auditedFailure");
        when(point.getSignature()).thenReturn(signature);
        LoginUser user = new LoginUser();
        user.setUserId(123L);
        user.setUsername("audit-operator");
        user.setDeptId(456L);
        ApplicationContext publisher = mock(ApplicationContext.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/failure");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class);
             MockedStatic<SpringUtils> spring = mockStatic(SpringUtils.class)) {
            login.when(LoginHelper::isLogin).thenReturn(true);
            login.when(LoginHelper::getLoginUser).thenReturn(user);
            spring.when(SpringUtils::context).thenReturn(publisher);
            Log annotation = getClass().getDeclaredMethod("auditedFailure").getAnnotation(Log.class);
            assertThatThrownBy(() -> new LogAspect().doAround(point, annotation)).isSameAs(failure);
            ArgumentCaptor<OperLogEvent> event = ArgumentCaptor.forClass(OperLogEvent.class);
            verify(publisher).publishEvent(event.capture());
            assertThat(event.getValue().getStatus()).isEqualTo(1);
            assertThat(event.getValue().getOperName()).isEqualTo("audit-operator");
            assertThat(event.getValue().getUserId()).isEqualTo(123L);
            assertThat(event.getValue().getDeptId()).isEqualTo(456L);
            assertThat(event.getValue().getCostTime()).isNotNegative();
            assertThat(event.getValue().getErrorMsg()).isEqualTo(IllegalArgumentException.class.getName());
            assertThat(event.getValue().toString()).doesNotContain(canary);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

}
