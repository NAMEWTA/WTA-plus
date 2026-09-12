package org.namewta.system.service;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.domain.SysClient;
import org.namewta.system.domain.vo.SysRoleVo;

/**
 * 客户端默认角色解析服务。
 *
 * @author NAMEWTA
 */
public interface ISysClientDefaultRoleResolverService {

    /**
     * 解析客户端配置的默认角色。
     *
     * @param client 客户端
     * @return 默认角色，未配置时返回 null
     * @throws ServiceException 客户端停用或默认角色配置无效
     */
    SysRoleVo resolveRole(SysClient client);

    /**
     * 根据客户端主键解析默认角色 ID。
     *
     * @param clientId 客户端主键
     * @return 默认角色 ID，客户端主键为空或未配置时返回 null
     * @throws ServiceException 客户端不存在、已停用或默认角色配置无效
     */
    Long resolveRoleId(Long clientId);

}
