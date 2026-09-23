package org.namewta.web.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.exception.user.UserException;
import org.namewta.common.core.utils.ValidatorUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.social.config.properties.SocialProperties;
import org.namewta.common.social.utils.SocialUtils;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.api.model.SocialLoginBody;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysSocialVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.service.ClientUserTypeAccessService;
import org.namewta.system.service.ISysSocialService;
import org.namewta.web.domain.vo.LoginVo;
import org.namewta.web.service.IAuthStrategy;
import org.namewta.web.service.SysLoginService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 第三方授权策略
 */
@Slf4j
@Service("social" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class SocialAuthStrategy implements IAuthStrategy {

    private final SocialProperties socialProperties;
    private final ISysSocialService sysSocialService;
    private final SysUserMapper userMapper;
    private final SysLoginService loginService;
    private final ClientUserTypeAccessService clientUserTypeAccessService;

    /**
     * 执行第三方授权登录，并校验授权账号与系统账号的绑定关系。
     *
     * @param body   登录信息
     * @param client 客户端信息
     * @return 登录结果
     */
    @Override
    public LoginVo login(String body, SysClientVo client) {
        SocialLoginBody loginBody = JsonUtils.parseObject(body, SocialLoginBody.class);
        ValidatorUtils.validate(loginBody);
        AuthResponse<AuthUser> response = SocialUtils.loginAuth(
            loginBody.getSource(), loginBody.getSocialCode(),
            loginBody.getSocialState(), socialProperties);
        if (!response.ok()) {
            throw new ServiceException(response.getMsg());
        }
        AuthUser authUserData = response.getData();

        List<SysSocialVo> list = sysSocialService.selectByAuthId(authUserData.getSource() + authUserData.getUuid());
        if (CollUtil.isEmpty(list)) {
            throw new ServiceException("你还没有绑定第三方账号，绑定后才可以登录！");
        }
        SysUserVo user = loadUser(list.getFirst().getUserId());
        SysUserTypeVo activeUserType = clientUserTypeAccessService.requireLoginAccess(user.getUserId(), client);
        LoginUser loginUser = loginService.buildLoginUser(user, client, activeUserType);
        SaLoginParameter model = IAuthStrategy.buildLoginParameter(client);
        // 生成token
        LoginHelper.login(loginUser, model);

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(client.getClientId());
        return loginVo;
    }

    /**
     * 根据用户ID加载用户，并校验账号状态是否允许登录。
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    private SysUserVo loadUser(Long userId) {
        SysUserVo user = userMapper.selectVoById(userId);
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", "");
            throw new UserException("user.not.exists", "");
        } else if (SystemConstants.DISABLE.equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", "");
            throw new UserException("user.blocked", "");
        }
        return user;
    }

}
