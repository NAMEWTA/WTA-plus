package org.namewta.common.push.security;

import cn.dev33.satoken.stp.StpUtil;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.model.LoginUser;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/** 为浏览器长连接签发一次性短时票据，避免把长期登录令牌放入 URL。 */
@Service
public class PushTicketService {
    private static final String PREFIX = "push:ticket:";
    private static final Duration TTL = Duration.ofSeconds(30);

    public String issue() {
        String token = StpUtil.getTokenValue();
        if (StringUtils.isBlank(token) || !StpUtil.isLogin()) {
            throw new IllegalStateException("登录状态已失效");
        }
        String ticket = UUID.randomUUID().toString().replace("-", "");
        RedisUtils.setCacheObject(PREFIX + ticket, token, TTL);
        return ticket;
    }

    public LoginUser consume(String ticket) {
        if (StringUtils.isBlank(ticket)) return null;
        String token = RedisUtils.getCacheObject(PREFIX + ticket);
        if (StringUtils.isBlank(token)) return null;
        RedisUtils.deleteObject(PREFIX + ticket);
        return LoginHelper.getLoginUser(token);
    }
}
