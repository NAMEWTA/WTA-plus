package org.namewta.web.sso;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.enums.LoginType;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.exception.user.UserException;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.api.SsoIdentityService;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.domain.SysUser;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.service.ClientUserTypeAccessService;
import org.namewta.system.service.ISysClientService;
import org.namewta.system.temporarypassword.TemporaryPasswordService;
import org.namewta.web.service.SysLoginService;
import org.springframework.stereotype.Service;

/**
 * 组装层实现 SSO 认人：复用本仓密码与 Client 登录域准入。
 */
@Service
@RequiredArgsConstructor
public class AdminSsoIdentityService implements SsoIdentityService {

    private final SysUserMapper userMapper;
    private final ISysClientService clientService;
    private final ClientUserTypeAccessService clientUserTypeAccessService;
    private final TemporaryPasswordService temporaryPasswordService;
    private final SysLoginService loginService;

    /**
     * {@inheritDoc}
     */
    @Override
    public SsoAuthenticatedUser verifyPassword(String username, String password) {
        SysUserVo user = loadUserByUsername(username);
        loginService.checkLoginAllowed(LoginType.PASSWORD, username);
        boolean permanentPassword = BCrypt.checkpw(password, user.getPassword());
        TemporaryPasswordService.VerifiedPassword temporaryPassword = null;
        if (!permanentPassword) {
            temporaryPassword = temporaryPasswordService.verify(user.getUserId(), password)
                .orElseThrow(() -> loginService.loginFailed(LoginType.PASSWORD, username));
        }
        if (!permanentPassword && !temporaryPasswordService.consume(temporaryPassword)) {
            throw loginService.loginFailed(LoginType.PASSWORD, username);
        }
        loginService.loginSucceeded(username);
        return new SsoAuthenticatedUser(user.getUserId(), user.getUserName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void assertClientAccess(Long userId, String clientId) {
        SysClientVo client = requireClient(clientId);
        clientUserTypeAccessService.requireLoginAccess(userId, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginUser buildLoginUser(Long userId, String clientId) {
        SysClientVo client = requireClient(clientId);
        if ("sso".equalsIgnoreCase(client.getClientKey())) {
            throw new ServiceException("禁止签发 SSO 中心 Client 业务票");
        }
        SysUserVo user = userMapper.selectVoById(userId);
        if (ObjectUtil.isNull(user) || SystemConstants.DISABLE.equals(user.getStatus())) {
            throw new UserException("user.blocked", String.valueOf(userId));
        }
        SysUserTypeVo userType = clientUserTypeAccessService.requireLoginAccess(userId, client);
        return loginService.buildLoginUser(user, client, userType);
    }

    private SysClientVo requireClient(String clientId) {
        SysClientVo client = clientService.queryByClientId(clientId);
        if (ObjectUtil.isNull(client) || !SystemConstants.NORMAL.equals(client.getStatus())) {
            throw new ServiceException("客户端不可用");
        }
        return client;
    }

    private SysUserVo loadUserByUsername(String username) {
        SysUserVo user = userMapper.lambda()
            .eq(SysUser::getUserName, username)
            .voOne();
        if (ObjectUtil.isNull(user)) {
            throw new UserException("user.not.exists", username);
        }
        if (SystemConstants.DISABLE.equals(user.getStatus())) {
            throw new UserException("user.blocked", username);
        }
        return user;
    }
}
