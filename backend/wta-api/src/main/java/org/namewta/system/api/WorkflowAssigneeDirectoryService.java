package org.namewta.system.api;

import java.util.Collection;
import java.util.List;

/** 工作流跨客户端配置目录；只投影客户端标识及用户准入，不暴露凭据。 */
public interface WorkflowAssigneeDirectoryService {
    /** 返回启用的业务客户端，供拥有流程设计权限的入口使用。 */
    List<Client> clients();

    /** 校验客户端仍正常；无效时抛出业务异常。 */
    void requireClient(Long clientPk);

    /** 校验角色正常且归属节点客户端。 */
    void requireRole(Long clientPk, Long roleId);

    /** 保留正常且持有该客户端登录域的用户。 */
    List<Long> eligibleUsers(Long clientPk, Collection<Long> userIds);

    /** 查询指定客户端当前有效角色成员，包含动态默认角色成员。 */
    List<Long> roleUsers(Long roleId);

    /** 不含秘密的客户端目录项。 */
    record Client(Long clientPk, String clientKey) { }
}
