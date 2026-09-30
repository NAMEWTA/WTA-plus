package org.namewta.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.namewta.system.domain.SysClient;
import org.namewta.system.domain.SysRole;
import org.namewta.system.domain.SysUser;
import org.namewta.system.domain.SysUserRole;
import org.namewta.system.domain.SysUserType;
import org.namewta.system.domain.SysUserTypeRel;
import org.namewta.system.mapper.SysClientMapper;
import org.namewta.system.mapper.SysRoleMapper;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.mapper.SysUserRoleMapper;
import org.namewta.system.mapper.SysUserTypeMapper;
import org.namewta.system.mapper.SysUserTypeRelMapper;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 校验节点 Client 的真实角色成员语义，以及下推数据库的登录域/启用约束。 */
@Tag("dev")
class SysWorkflowAssigneeDirectoryServiceTest {
    private final SysClientMapper clients = mock(SysClientMapper.class);
    private final SysRoleMapper roles = mock(SysRoleMapper.class);
    private final SysUserMapper users = mock(SysUserMapper.class);
    private final SysUserRoleMapper userRoles = mock(SysUserRoleMapper.class);
    private final SysUserTypeRelMapper userTypes = mock(SysUserTypeRelMapper.class);
    private final SysUserTypeMapper loginDomains = mock(SysUserTypeMapper.class);
    private final SysWorkflowAssigneeDirectoryServiceImpl service = new SysWorkflowAssigneeDirectoryServiceImpl(
        clients, roles, users, userRoles, userTypes, loginDomains);

    @BeforeAll
    static void initializeLambdaMapping() {
        for (Class<?> type : List.of(SysClient.class, SysUser.class, SysUserRole.class, SysUserTypeRel.class)) {
            if (TableInfoHelper.getTableInfo(type) == null) {
                TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "workflow-directory-test"), type);
            }
        }
    }

    @BeforeEach
    void activeBusinessClient() {
        when(clients.selectById(20L)).thenReturn(client("0", "review-web", 7L));
        var domain = new SysUserType(); domain.setUserTypeId(7L); domain.setStatus("0");
        when(loginDomains.selectById(7L)).thenReturn(domain);
        when(roles.selectById(201L)).thenReturn(role(201L, 20L, "0"));
        when(roles.selectById(202L)).thenReturn(role(202L, 20L, "0"));
    }

    @Test
    void defaultRoleMembershipTracksCurrentLoginDomainGrantsWithoutExplicitRoleRows() {
        when(userTypes.selectList(any())).thenReturn(List.of(relation(101L)), List.of(relation(101L)),
            List.of(relation(103L)), List.of(relation(103L)));
        when(users.selectList(any())).thenReturn(List.of(user(101L)), List.of(user(103L)));
        assertThat(service.roleUsers(201L)).containsExactly(101L);
        assertThat(service.roleUsers(201L)).containsExactly(103L);
        verifyNoInteractions(userRoles);
        var queries = SysWorkflowAssigneeDirectoryServiceTest.<SysUserTypeRel>captor();
        verify(userTypes, times(4)).selectList(queries.capture());
        for (int index : new int[] {0, 2}) {
            var query = queries.getAllValues().get(index);
            assertThat(query.getSqlSegment()).contains("user_type_id =", "status =").doesNotContain("user_id IN");
            assertThat(parameters(query)).containsValue(7L).containsValue("0");
        }
    }

    @Test
    void explicitRoleCandidatesAreRestrictedAgainByTheirClientsLoginDomain() {
        var first = new SysUserRole(); first.setRoleId(202L); first.setUserId(101L);
        var otherDomain = new SysUserRole(); otherDomain.setRoleId(202L); otherDomain.setUserId(102L);
        when(userRoles.selectList(any())).thenReturn(List.of(first, otherDomain));
        when(userTypes.selectList(any())).thenReturn(List.of(relation(101L)));
        when(users.selectList(any())).thenReturn(List.of(user(101L)));
        assertThat(service.roleUsers(202L)).containsExactly(101L);
        var roleQuery = SysWorkflowAssigneeDirectoryServiceTest.<SysUserRole>captor();
        verify(userRoles).selectList(roleQuery.capture());
        assertThat(roleQuery.getValue().getSqlSegment()).contains("role_id =");
        assertThat(parameters(roleQuery.getValue())).containsValue(202L);
        var domainQuery = SysWorkflowAssigneeDirectoryServiceTest.<SysUserTypeRel>captor();
        verify(userTypes).selectList(domainQuery.capture());
        assertThat(domainQuery.getValue().getSqlSegment()).contains("user_type_id =", "status =", "user_id IN");
        assertThat(parameters(domainQuery.getValue())).containsValue(7L).containsValue("0").containsValue(101L).containsValue(102L);
        var userQuery = SysWorkflowAssigneeDirectoryServiceTest.<SysUser>captor();
        verify(users).selectList(userQuery.capture());
        assertThat(parameters(userQuery.getValue())).containsValue(101L).doesNotContainValue(102L);
    }

    @Test
    void eligibilityRequiresActiveDomainMembershipAndActiveAccountAndNeverReadsUnrequestedUsers() {
        // 103 has no active membership; 102 has membership but its account is disabled.
        when(userTypes.selectList(any())).thenReturn(List.of(relation(101L), relation(102L)));
        when(users.selectList(any())).thenReturn(List.of(user(101L)));
        assertThat(service.eligibleUsers(20L, List.of(101L, 102L, 103L))).containsExactly(101L);
        var domainQuery = SysWorkflowAssigneeDirectoryServiceTest.<SysUserTypeRel>captor();
        verify(userTypes).selectList(domainQuery.capture());
        assertThat(domainQuery.getValue().getSqlSegment()).contains("user_type_id =", "status =", "user_id IN");
        assertThat(parameters(domainQuery.getValue())).containsValue(7L).containsValue("0")
            .containsValue(101L).containsValue(102L).containsValue(103L).doesNotContainValue(999L);
        var userQuery = SysWorkflowAssigneeDirectoryServiceTest.<SysUser>captor();
        verify(users).selectList(userQuery.capture());
        assertThat(userQuery.getValue().getSqlSegment()).contains("user_id IN", "status =");
        assertThat(parameters(userQuery.getValue())).containsValue("0").containsValue(101L).containsValue(102L)
            .doesNotContainValue(103L).doesNotContainValue(999L);
    }

    @Test
    void emptyEligibleMembershipNeverFallsBackToAllUsers() {
        when(userTypes.selectList(any())).thenReturn(List.of());
        assertThat(service.eligibleUsers(20L, List.of(101L))).isEmpty();
        verifyNoInteractions(users);
    }

    @Test
    void emptyRequestedUsersDoesNotIssueAnUnboundedDirectoryQuery() {
        assertThat(service.eligibleUsers(20L, List.of())).isEmpty();
        assertThat(service.eligibleUsers(20L, null)).isEmpty();
        verifyNoInteractions(userTypes, users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "disabled", "sso"})
    void unavailableClientRejectsBeforeAnyMembershipLookup(String state) {
        when(clients.selectById(20L)).thenReturn("missing".equals(state) ? null
            : client("disabled".equals(state) ? "1" : "0", "sso".equals(state) ? "sso" : "review-web", 7L));
        assertThatThrownBy(() -> service.eligibleUsers(20L, List.of(101L)))
            .hasMessage("流程办理客户端不存在或不可用");
        verifyNoInteractions(loginDomains, userTypes, users);
    }

    @Test
    void nullClientCannotBeInterpretedAsAGlobalDirectory() {
        assertThatThrownBy(() -> service.requireClient(null)).hasMessage("流程办理客户端不存在或不可用");
        verifyNoInteractions(clients, loginDomains, userTypes, users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "disabled", "unassigned"})
    void unavailableLoginDomainRejectsEvenWhenTheClientIsEnabled(String state) {
        var domain = new SysUserType(); domain.setUserTypeId(7L); domain.setStatus("1");
        when(loginDomains.selectById(7L)).thenReturn("missing".equals(state) ? null : domain);
        if ("unassigned".equals(state)) when(clients.selectById(20L)).thenReturn(client("0", "review-web", null));
        assertThatThrownBy(() -> service.eligibleUsers(20L, List.of(101L))).hasMessage("办理客户端登录域不可用");
        assertThatThrownBy(() -> service.roleUsers(201L)).hasMessage("办理客户端登录域不可用");
        verifyNoInteractions(userTypes, userRoles, users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"foreign", "disabled", "missing"})
    void roleConfigurationCannotCrossClientsOrSelectAnInactiveRole(String state) {
        when(roles.selectById(202L)).thenReturn("missing".equals(state) ? null
            : role(202L, "foreign".equals(state) ? 30L : 20L, "disabled".equals(state) ? "1" : "0"));
        assertThatThrownBy(() -> service.requireRole(20L, 202L)).hasMessage("审核角色不属于节点办理客户端或已停用");
        verifyNoInteractions(userRoles, userTypes, users);
    }

    @Test
    void sameClientActiveRoleCanBeConfiguredWithoutReadingItsMembers() {
        service.requireRole(20L, 202L);
        verifyNoInteractions(userRoles, userTypes, users);
    }

    @Test
    void inactiveRoleDoesNotFallBackToClientDefaultMembership() {
        when(roles.selectById(201L)).thenReturn(role(201L, 20L, "1"));
        assertThat(service.roleUsers(201L)).isEmpty();
        assertThat(service.roleUsers(999L)).isEmpty();
        verifyNoInteractions(clients, loginDomains, userRoles, userTypes, users);
    }

    private SysClient client(String status, String key, Long domainId) {
        var client = new SysClient(); client.setId(20L); client.setStatus(status); client.setClientKey(key);
        client.setUserTypeId(domainId); client.setDefaultRoleId(201L); return client;
    }

    private SysRole role(long id, long client, String status) {
        var role = new SysRole(); role.setRoleId(id); role.setClientId(client); role.setStatus(status); return role;
    }

    private SysUserTypeRel relation(long userId) {
        var relation = new SysUserTypeRel(); relation.setUserId(userId); relation.setUserTypeId(7L); relation.setStatus("0");
        return relation;
    }

    private SysUser user(long id) { var user = new SysUser(); user.setUserId(id); user.setStatus("0"); return user; }

    private static Map<String, Object> parameters(Wrapper<?> query) {
        query.getSqlSegment();
        return ((AbstractWrapper<?, ?, ?>) query).getParamNameValuePairs();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ArgumentCaptor<Wrapper<T>> captor() { return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class); }
}
