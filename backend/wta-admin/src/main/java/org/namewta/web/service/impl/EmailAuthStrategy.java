package org.namewta.web.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.constant.Constants;
import org.namewta.common.core.constant.GlobalConstants;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.enums.LoginType;
import org.namewta.common.core.exception.user.CaptchaExpireException;
import org.namewta.common.core.exception.user.UserException;
import org.namewta.common.core.utils.MessageUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.core.utils.ValidatorUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.model.EmailLoginBody;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.domain.SysUser;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.service.ClientUserTypeAccessService;
import org.namewta.web.domain.vo.LoginVo;
import org.namewta.web.service.IAuthStrategy;
import org.namewta.web.service.SysLoginService;
import org.springframework.stereotype.Service;

/**
 * 邮件认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service("email" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class EmailAuthStrategy implements IAuthStrategy {

    private final SysLoginService loginService;
    private final SysUserMapper userMapper;
    private final ClientUserTypeAccessService clientUserTypeAccessService;

    /**
     * 执行邮箱验证码登录，并按客户端配置生成访问令牌。
     *
     * @param body   登录请求体
     * @param client 当前客户端配置
     * @return 登录结果
     */
    @Override
    public LoginVo login(String body, SysClientVo client) {
        EmailLoginBody loginBody = JsonUtils.parseObject(body, EmailLoginBody.class);
        ValidatorUtils.validate(loginBody);
        String email = loginBody.getEmail();
        String emailCode = loginBody.getEmailCode();
        SysUserVo user = loadUserByEmail(email);
        loginService.checkLogin(LoginType.EMAIL, user.getUserName(), () -> !validateEmailCode(email, emailCode));
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
     * 校验邮箱验证码是否存在且匹配。
     *
     * @param email     邮箱地址
     * @param emailCode 用户输入的邮箱验证码
     * @return 是否校验通过
     */
    private boolean validateEmailCode(String email, String emailCode) {
        String code = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + email);
        if (StringUtils.isBlank(code)) {
            loginService.recordLoginInfo(email, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(emailCode);
    }

    /**
     * 按邮箱加载可登录用户，并校验是否存在或被停用。
     *
     * @param email 邮箱地址
     * @return 用户信息
     */
    private SysUserVo loadUserByEmail(String email) {
        SysUserVo user = userMapper.lambda()
            .eq(SysUser::getEmail, email)
            .voOne();
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", email);
            throw new UserException("user.not.exists", email);
        } else if (SystemConstants.DISABLE.equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", email);
            throw new UserException("user.blocked", email);
        }
        return user;
    }

}
