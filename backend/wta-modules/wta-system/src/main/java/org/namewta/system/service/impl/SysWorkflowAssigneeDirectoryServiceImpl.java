package org.namewta.system.service.impl;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.system.api.WorkflowAssigneeDirectoryService;
import org.namewta.system.domain.*;
import org.namewta.system.mapper.*;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** 为流程配置与分派提供明确 Client 的目录及登录域校验。 */
@Service
@RequiredArgsConstructor
public class SysWorkflowAssigneeDirectoryServiceImpl implements WorkflowAssigneeDirectoryService {
    private final SysClientMapper clients;
    private final SysRoleMapper roles;
    private final SysUserMapper users;
    private final SysUserRoleMapper userRoles;
    private final SysUserTypeRelMapper userTypes;
    private final SysUserTypeMapper loginDomains;

    @Override
    public List<Client> clients() {
        return clients.selectList(QueryBuilder.lambda(SysClient.class)
            .eq(SysClient::getStatus, "0").ne(SysClient::getClientKey, "sso").build()).stream()
            .map(client -> new Client(client.getId(), client.getClientKey())).toList();
    }

    @Override
    public void requireClient(Long clientPk) {
        activeClient(clientPk);
    }

    @Override
    public void requireRole(Long clientPk, Long roleId) {
        activeClient(clientPk);
        SysRole role = roles.selectById(roleId);
        if (role == null || !"0".equals(role.getStatus()) || !Objects.equals(clientPk, role.getClientId())) {
            throw new ServiceException("审核角色不属于节点办理客户端或已停用");
        }
    }

    @Override
    public List<Long> eligibleUsers(Long clientPk, Collection<Long> userIds) {
        SysClient client = activeClient(clientPk);
        if (userIds == null || userIds.isEmpty()) return List.of();
        List<Long> permitted = userTypes.selectList(QueryBuilder.lambda(SysUserTypeRel.class)
            .eq(SysUserTypeRel::getUserTypeId, client.getUserTypeId())
            .eq(SysUserTypeRel::getStatus, "0").in(SysUserTypeRel::getUserId, userIds).build())
            .stream().map(SysUserTypeRel::getUserId).distinct().toList();
        if (permitted.isEmpty()) return List.of();
        return users.selectList(QueryBuilder.lambda(SysUser.class).in(SysUser::getUserId, permitted)
            .eq(SysUser::getStatus, "0").build()).stream().map(SysUser::getUserId).toList();
    }

    @Override
    public List<Long> roleUsers(Long roleId) {
        SysRole role = roles.selectById(roleId);
        if (role == null || !"0".equals(role.getStatus())) return List.of();
        SysClient client = activeClient(role.getClientId());
        List<Long> candidates;
        if (Objects.equals(roleId, client.getDefaultRoleId())) {
            candidates = userTypes.selectList(QueryBuilder.lambda(SysUserTypeRel.class)
                .eq(SysUserTypeRel::getUserTypeId, client.getUserTypeId())
                .eq(SysUserTypeRel::getStatus, "0").build()).stream().map(SysUserTypeRel::getUserId).toList();
        } else {
            candidates = userRoles.selectList(QueryBuilder.lambda(SysUserRole.class)
                .eq(SysUserRole::getRoleId, roleId).build()).stream().map(SysUserRole::getUserId).toList();
        }
        return eligibleUsers(client.getId(), candidates);
    }

    /** Client 主键与 OAuth 字符串不可互换；不存在或停用均拒绝。 */
    private SysClient activeClient(Long clientPk) {
        SysClient client = clientPk == null ? null : clients.selectById(clientPk);
        if (client == null || !"0".equals(client.getStatus()) || "sso".equals(client.getClientKey())) {
            throw new ServiceException("流程办理客户端不存在或不可用");
        }
        SysUserType loginDomain = client.getUserTypeId() == null ? null : loginDomains.selectById(client.getUserTypeId());
        if (loginDomain == null || !"0".equals(loginDomain.getStatus())) throw new ServiceException("办理客户端登录域不可用");
        return client;
    }
}
