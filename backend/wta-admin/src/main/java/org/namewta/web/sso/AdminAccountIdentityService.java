package org.namewta.web.sso;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.system.api.AccountIdentityService;
import org.namewta.system.api.domain.AccountIdentity;
import org.namewta.system.domain.SysUser;
import org.namewta.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

/** 组装层将正常账户投影到身份协议；不授予本仓 Client、登录域或 RBAC 会话。 */
@Service
@RequiredArgsConstructor
public class AdminAccountIdentityService implements AccountIdentityService {
    private final SysUserMapper users;

    @Override
    public AccountIdentity findActiveById(Long userId) {
        if (userId == null || userId <= 0) return null;
        SysUser user = users.selectById(userId);
        if (user == null || !SystemConstants.NORMAL.equals(user.getStatus())
            || !"0".equals(user.getDelFlag())) return null;
        return new AccountIdentity(user.getUserId(), user.getUserName(), user.getNickName(), user.getEmail(),
            user.getPhoneNumber(), user.getAvatar(), user.getStatus());
    }
}
