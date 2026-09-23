package org.namewta.test.notify;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.aspect.LogAspect;
import org.namewta.common.log.event.OperLogEvent;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.web.logging.SysLogFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 验证真实 Servlet HTTP 事件和 @Log 切面入口使用路径相关脱敏。 */
@Tag("dev")
class NotifySmsLogEntryTest {

    @Log(title = "统一通知", isSaveResponseData = false)
    void submitForAudit(Map<String, Object> ignored) { }

    @Test
    void captchaGetEmitsNoPhoneToSysLogSink() throws Exception {
        String phone = "13812345678";
        List<Map<String, Object>> events = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(4096, 8192, events::add);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/resource/sms/code");
        request.setQueryString("phoneNumber=" + phone);
        request.addParameter("phoneNumber", phone);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});

        assertEquals("HTTP_REQUEST", events.getFirst().get("event"));
        assertFalse(events.toString().contains(phone));
        assertEquals(List.of("[REDACTED]"),
            ((Map<?, ?>) events.getFirst().get("parameters")).get("phoneNumber"));
    }

    @Test
    void notificationPostEmitsNoPhoneOrOtpToHttpAndOperationSinks() throws Throwable {
        String phone = "13812345678";
        String otp = "otp-canary-7731";
        Map<String, Object> command = Map.of("bizId", phone, "recipientIds", List.of(phone),
            "templateParams", Map.of("code", otp), "sceneCode", "auth-captcha");
        String body = org.namewta.common.json.utils.JsonUtils.toJsonString(command);
        List<Map<String, Object>> httpEvents = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(4096, 8192, httpEvents::add);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/notify/notification");
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/notify/notification");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes((jakarta.servlet.http.HttpServletRequest) req));
            try {
                ApplicationContext context = mock(ApplicationContext.class);
                AtomicReference<OperLogEvent> published = new AtomicReference<>();
                doAnswer(invocation -> {
                    Object event = invocation.getArgument(0);
                    if (event instanceof OperLogEvent operLog) published.set(operLog);
                    return null;
                }).when(context).publishEvent(any(Object.class));
                ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
                Signature signature = mock(Signature.class);
                when(joinPoint.getArgs()).thenReturn(new Object[]{command});
                when(joinPoint.getTarget()).thenReturn(this);
                when(joinPoint.getSignature()).thenReturn(signature);
                when(signature.getName()).thenReturn("submitForAudit");
                when(joinPoint.proceed()).thenReturn(null);
                Log annotation = getClass().getDeclaredMethod("submitForAudit", Map.class).getAnnotation(Log.class);
                try (var spring = mockStatic(SpringUtils.class);
                     var login = mockStatic(LoginHelper.class)) {
                    spring.when(SpringUtils::context).thenReturn(context);
                    login.when(LoginHelper::isLogin).thenReturn(false);
                    new LogAspect().doAround(joinPoint, annotation);
                }
                assertNotNull(published.get(), "@Log must publish an operation event");
                assertFalse(published.get().getOperParam().contains(phone));
                assertFalse(published.get().getOperParam().contains(otp));
            } catch (Throwable failure) {
                throw new RuntimeException(failure);
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        });
        assertFalse(httpEvents.toString().contains(phone));
        assertFalse(httpEvents.toString().contains(otp));
        assertTrue(httpEvents.toString().contains("auth-captcha"));
    }
}
