package org.namewta.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.domain.SysClient;
import org.namewta.system.domain.vo.SysRoleVo;
import org.namewta.system.mapper.SysClientMapper;
import org.namewta.system.mapper.SysRoleMapper;
import org.namewta.system.service.ISysClientDefaultRoleResolverService;
import org.springframework.stereotype.Service;

/**
 * 客户端默认角色解析服务实现。
 *
 * @author NAMEWTA
 */
@RequiredArgsConstructor
@Service
public class SysClientDefaultRoleResolverServiceImpl implements ISysClientDefaultRoleResolverService {

    private final SysClientMapper clientMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public SysRoleVo resolveRole(SysClient client) {
        requireActiveClient(client);
        if (ObjectUtil.isNull(client.getDefaultRoleId())) {
            return null;
        }
        SysRoleVo role = roleMapper.selectVoById(client.getDefaultRoleId());
        if (ObjectUtil.isNull(role)
            || !SystemConstants.NORMAL.equals(role.getStatus())
            || ObjectUtil.isNull(client.getId())
            || ObjectUtil.isNull(role.getClientId())
            || !client.getId().equals(role.getClientId())) {
            throw new ServiceException("客户端默认角色配置无效");
        }
        return role;
    }

    @Override
    public Long resolveRoleId(Long clientId) {
        if (ObjectUtil.isNull(clientId)) {
            return null;
        }
        SysRoleVo role = resolveRole(clientMapper.selectById(clientId));
        return ObjectUtil.isNull(role) ? null : role.getRoleId();
    }

    /**
     * 默认角色只能从正常客户端解析，异常配置必须失败关闭。
     */
    private void requireActiveClient(SysClient client) {
        if (ObjectUtil.isNull(client) || !SystemConstants.NORMAL.equals(client.getStatus())) {
            throw new ServiceException("客户端不存在或已停用");
        }
    }

}
