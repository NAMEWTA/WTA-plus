package org.namewta.system.service;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.springframework.stereotype.Service;

/**
 * 按客户端要求的登录域校验用户是否允许登录。
 *
 * @author NAMEWTA
 */
@RequiredArgsConstructor
@Service
public class ClientUserTypeAccessService {

    private final ISysUserTypeService userTypeService;
    private final ISysUserTypeRelService userTypeRelService;

    /**
     * 校验用户是否具备客户端要求的正常登录域。
     *
     * @param userId 用户ID
     * @param client 当前客户端
     * @return 活动登录域
     */
    public SysUserTypeVo requireLoginAccess(Long userId, SysClientVo client) {
        if (ObjectUtil.isNull(client)) {
            throw new ServiceException("客户端不存在");
        }
        if (ObjectUtil.isNull(client.getUserTypeId())) {
            throw new ServiceException("客户端未配置登录域");
        }
        SysUserTypeVo userType = userTypeService.queryById(client.getUserTypeId());
        if (ObjectUtil.isNull(userType)) {
            throw new ServiceException("登录域不存在");
        }
        if (!SystemConstants.NORMAL.equals(userType.getStatus())) {
            throw new ServiceException("登录域已停用");
        }
        if (!userTypeRelService.hasUserType(userId, client.getUserTypeId())) {
            throw new ServiceException("当前账号不具备该应用的登录域");
        }
        return userType;
    }

}
