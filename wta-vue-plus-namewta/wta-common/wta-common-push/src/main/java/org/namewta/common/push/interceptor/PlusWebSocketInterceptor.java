package org.namewta.common.push.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import org.namewta.common.push.constant.MessageConstants;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.push.security.PushTicketService;
import org.namewta.system.api.model.LoginUser;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import org.springframework.http.server.ServletServerHttpRequest;

/**
 * WebSocket 握手拦截器。
 *
 * @author Lion Li
 */
public class PlusWebSocketInterceptor implements HandshakeInterceptor {
    private final PushTicketService ticketService;

    public PlusWebSocketInterceptor(PushTicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 握手前提取统一鉴权后的用户信息。
     *
     * @param attributes 用于传递到 WebSocketSession 的属性集合
     * @return 是否允许握手（true=允许，false=拒绝）
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String ticket = request instanceof ServletServerHttpRequest servletRequest
            ? servletRequest.getServletRequest().getParameter("ticket") : null;
        LoginUser loginUser = ticketService.consume(ticket);
        String tokenValue = ticket;
        if (loginUser == null) {
            loginUser = LoginHelper.getLoginUser();
            tokenValue = StpUtil.getTokenValue();
        }
        attributes.put(MessageConstants.LOGIN_USER_KEY, loginUser);
        attributes.put(MessageConstants.LOGIN_TOKEN_KEY, tokenValue);
        return true;
    }

    /**
     * 握手完成后触发
     * 此处无需处理，留空即可
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}
