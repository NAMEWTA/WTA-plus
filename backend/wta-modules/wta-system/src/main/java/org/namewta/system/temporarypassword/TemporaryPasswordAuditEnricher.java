package org.namewta.system.temporarypassword;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.utils.ServletUtils;
import org.namewta.common.log.event.OperLogEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * 为临时密码操作日志补充非敏感的目标用户与 TTL 元数据。
 */
@Component
@RequiredArgsConstructor
public class TemporaryPasswordAuditEnricher {

    public static final String AUDIT_TITLE = "用户临时密码";
    private static final String TARGET_USER_ATTRIBUTE =
        TemporaryPasswordAuditEnricher.class.getName() + ".targetUserId";
    private final JsonMapper jsonMapper;

    /**
     * 在 controller 执行业务校验前记录目标用户，不保存原始请求体。
     */
    public void attachTarget(Long targetUserId) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null) {
            request.setAttribute(TARGET_USER_ATTRIBUTE, targetUserId);
        }
    }

    /**
     * 在异步持久化 listener 取得事件前写入安全审计元数据。
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    public void enrich(OperLogEvent event) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (!AUDIT_TITLE.equals(event.getTitle()) || request == null
            || !(request.getAttribute(TARGET_USER_ATTRIBUTE) instanceof Long targetUserId)) {
            return;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("targetUserId", targetUserId);
        metadata.put("expiresInSeconds", TemporaryPasswordService.EXPIRES_IN_SECONDS);
        event.setOperParam(jsonMapper.writeValueAsString(metadata));
    }
}
