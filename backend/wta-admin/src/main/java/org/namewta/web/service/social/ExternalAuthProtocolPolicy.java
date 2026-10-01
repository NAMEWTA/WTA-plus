package org.namewta.web.service.social;

import org.namewta.common.core.service.HttpProtocolPolicy;
import org.springframework.stereotype.Component;

/** 标准退出通知跳过业务响应增强和敏感请求日志，保持表单协议原文。 */
@Component
public class ExternalAuthProtocolPolicy implements HttpProtocolPolicy {
    @Override
    public boolean isProtocolPath(String path) {
        return path != null && path.matches("/auth/social/backchannel/[0-9]+");
    }

    @Override
    public boolean isSensitivePath(String path) {
        return path != null && (path.startsWith("/auth/social/") || path.equals("/auth/login"));
    }
}
