package org.namewta.web.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.constant.Constants;
import org.namewta.common.core.constant.GlobalConstants;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.enums.LoginType;
import org.namewta.common.core.exception.user.CaptchaException;
import org.namewta.common.core.exception.user.CaptchaExpireException;
import org.namewta.common.core.exception.user.UserException;
import org.namewta.common.core.utils.MessageUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.core.utils.ValidatorUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.web.config.properties.CaptchaProperties;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.api.model.PasswordLoginBody;
import org.namewta.system.domain.SysUser;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.service.ClientUserTypeAccessService;
import org.namewta.system.temporarypassword.TemporaryPasswordService;
import org.namewta.web.domain.vo.LoginVo;
import org.namewta.web.service.IAuthStrategy;
import org.namewta.web.service.SysLoginService;
import org.springframework.stereotype.Service;

/**
 * 密码认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service("password" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class PasswordAuthStrategy implements IAuthStrategy {

    private final CaptchaProperties captchaProperties;
    private final SysLoginService loginService;
    private final SysUserMapper userMapper;
    private final ClientUserTypeAccessService clientUserTypeAccessService;
    private final TemporaryPasswordService temporaryPasswordService;

    /**
     * 执行账号密码登录，并按客户端配置生成访问令牌。
     *
     * @param body   登录请求体
     * @param client 当前客户端配置
     * @return 登录结果
     */
    @Override
    public LoginVo login(String body, SysClientVo client) {
        PasswordLoginBody loginBody = JsonUtils.parseObject(body, PasswordLoginBody.class);
        ValidatorUtils.validate(loginBody);
        String username = loginBody.getUsername();
        String password = loginBody.getPassword();
        String code = loginBody.getCode();
        String uuid = loginBody.getUuid();

        boolean captchaEnabled = captchaProperties.getEnable();
        // 验证码开关
        if (captchaEnabled) {
            validateCaptcha(username, code, uuid);
        }
        SysUserVo user = loadUserByUsername(username);
        SysUserTypeVo activeUserType = authenticate(user, client, username, password);
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
     * 将永久密码和临时密码合并为一次 grant 判定。临时值只在 Client 准入后消费。
     */
    SysUserTypeVo authenticate(SysUserVo user, SysClientVo client, String username, String password) {
        loginService.checkLoginAllowed(LoginType.PASSWORD, username);
        boolean permanentPassword = BCrypt.checkpw(password, user.getPassword());
        TemporaryPasswordService.VerifiedPassword temporaryPassword = null;
        if (!permanentPassword) {
            temporaryPassword = temporaryPasswordService.verify(user.getUserId(), password)
                .orElseThrow(() -> loginService.loginFailed(LoginType.PASSWORD, username));
        }

        SysUserTypeVo activeUserType = clientUserTypeAccessService.requireLoginAccess(user.getUserId(), client);
        if (!permanentPassword && !temporaryPasswordService.consume(temporaryPassword)) {
            throw loginService.loginFailed(LoginType.PASSWORD, username);
        }
        loginService.loginSucceeded(username);
        return activeUserType;
    }

    /**
     * 校验图形验证码是否有效且匹配。
     *
     * @param username 用户名
     * @param code     用户输入的验证码
     * @param uuid     验证码缓存标识
     */
    private void validateCaptcha(String username, String code, String uuid) {
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + StringUtils.blankToDefault(uuid, "");
        String captcha = RedisUtils.getCacheObject(verifyKey);
        RedisUtils.deleteObject(verifyKey);
        if (captcha == null) {
            loginService.recordLoginInfo(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        if (!StringUtils.equalsIgnoreCase(code, captcha)) {
            loginService.recordLoginInfo(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error"));
            throw new CaptchaException();
        }
    }

    /**
     * 按用户名加载可登录用户，并校验是否存在或被停用。
     *
     * @param username 用户名
     * @return 用户信息
     */
    private SysUserVo loadUserByUsername(String username) {
        SysUserVo user = userMapper.lambda()
            .eq(SysUser::getUserName, username)
            .voOne();
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", username);
            throw new UserException("user.not.exists", username);
        } else if (SystemConstants.DISABLE.equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new UserException("user.blocked", username);
        }
        return user;
    }

}
