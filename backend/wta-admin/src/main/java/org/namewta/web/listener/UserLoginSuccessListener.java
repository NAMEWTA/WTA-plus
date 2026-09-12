package org.namewta.web.listener;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.constant.Constants;
import org.namewta.common.core.utils.MessageUtils;
import org.namewta.common.core.utils.ServletUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.core.utils.ip.AddressUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.domain.UserOnlineDTO;
import org.namewta.web.event.UserLoginSuccessEvent;
import org.namewta.web.service.SysLoginService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 用户登录成功监听器。
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class UserLoginSuccessListener {

    private final SysLoginService loginService;

    /**
     * 登录成功后记录在线信息、登录日志与最近登录信息。
     *
     * @param event 用户登录成功事件
     */
    @EventListener
    public void handleLoginSuccess(UserLoginSuccessEvent event) {
        SaLoginParameter loginParameter = event.loginParameter();
        UserAgent userAgent = UserAgentUtil.parse(ServletUtils.getRequest().getHeader("User-Agent"));
        String ip = ServletUtils.getClientIP();
        String username = (String) loginParameter.getExtra(LoginHelper.USER_NAME_KEY);
        String tokenValue = event.tokenValue();

        UserOnlineDTO dto = new UserOnlineDTO();
        dto.setIpaddr(ip);
        dto.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        dto.setBrowser(userAgent.getBrowser().getName());
        dto.setOs(userAgent.getOs().getName());
        dto.setLoginTime(System.currentTimeMillis());
        dto.setTokenId(tokenValue);
        dto.setUserName(username);
        dto.setClientKey((String) loginParameter.getExtra(LoginHelper.CLIENT_KEY));
        dto.setDeviceType(loginParameter.getDeviceType());
        dto.setDeptName((String) loginParameter.getExtra(LoginHelper.DEPT_NAME_KEY));
        if (loginParameter.getTimeout() == -1) {
            RedisUtils.setCacheObject(CacheNames.ONLINE_TOKEN_KEY + tokenValue, dto);
        } else {
            RedisUtils.setCacheObject(CacheNames.ONLINE_TOKEN_KEY + tokenValue, dto, Duration.ofSeconds(loginParameter.getTimeout()));
        }

        loginService.recordLoginInfo(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        loginService.updateLastLoginInfo((Long) loginParameter.getExtra(LoginHelper.USER_KEY), ip);
        log.info("user doLogin, userId:{}, token:***{}", event.loginId(), StringUtils.right(tokenValue, 8));
    }

}
