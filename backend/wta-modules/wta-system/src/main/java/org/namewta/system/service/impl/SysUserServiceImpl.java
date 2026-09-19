package org.namewta.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.*;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.common.openapi.session.OpenApiMachineSessionInvalidator;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.UserService;
import org.namewta.system.api.OssService;
import org.namewta.system.api.domain.UserDTO;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.domain.SysUser;
import org.namewta.system.domain.SysUserPost;
import org.namewta.system.domain.SysUserRole;
import org.namewta.system.domain.SysClient;
import org.namewta.system.domain.SysRole;
import org.namewta.system.domain.SysUserType;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.domain.constant.UserTypeGrantSource;
import org.namewta.system.domain.policy.UserPhonePolicy;
import org.namewta.system.domain.vo.SysPostVo;
import org.namewta.system.domain.vo.SysRoleVo;
import org.namewta.system.domain.vo.SysUserExportVo;
import org.namewta.system.domain.vo.SysUserTypeRelVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.mapper.*;
import org.namewta.system.service.ClientSessionService;
import org.namewta.system.service.ISysUserService;
import org.namewta.system.service.ISysUserTypeRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 用户 业务层处理
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysUserServiceImpl implements ISysUserService, UserService {

    private static final String USER_TABLE = "sys_user";

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysPostMapper postMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysClientMapper clientMapper;
    private final SysUserTypeMapper userTypeMapper;
    private final ClientSessionService clientSessionService;
    private final ISysUserTypeRelService userTypeRelService;
    private final OssService ossService;
    private OpenApiMachineSessionInvalidator openApiSessionInvalidator = ignored -> 0;

    @Autowired(required = false)
    void setOpenApiSessionInvalidator(OpenApiMachineSessionInvalidator openApiSessionInvalidator) {
        this.openApiSessionInvalidator = Objects.requireNonNull(openApiSessionInvalidator);
    }

    @Override
    public List<UserDTO> searchActiveUsers(String keyword, int limit) {
        if (StringUtils.isBlank(keyword)) {
            return List.of();
        }
        int boundedLimit = Math.clamp(limit, 1, 50);
        String normalized = keyword.strip();
        List<SysUserVo> list = userMapper.selectUserList(userMapper.lambda()
            .select(SysUser::getUserId, SysUser::getUserName, SysUser::getNickName,
                SysUser::getPhoneNumber, SysUser::getStatus)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .eq(SysUser::getDelFlag, SystemConstants.NORMAL)
            .and(wrapper -> wrapper.like(SysUser::getUserName, normalized)
                .or().like(SysUser::getNickName, normalized)
            .or().like(SysUser::getPhoneNumber, normalized))
            .orderByAsc(SysUser::getUserId)
            .last("limit " + boundedLimit));
        return BeanUtil.copyToList(list, UserDTO.class);
    }

    /**
     * 在调用方的动态数据源事务内锁定一个有效用户。
     *
     * @param userId 用户 ID
     * @return 未删除且状态正常的用户；不存在时返回 {@code null}
     */
    @Override
    public UserDTO lockActiveById(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        SysUserVo vo = userMapper.lambda()
            .select(SysUser::getUserId, SysUser::getDeptId, SysUser::getUserName,
                SysUser::getNickName, SysUser::getEmail,
                SysUser::getPhoneNumber, SysUser::getGender, SysUser::getStatus,
                SysUser::getCreateTime)
            .eq(SysUser::getUserId, userId)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .eq(SysUser::getDelFlag, SystemConstants.NORMAL)
            .last("for update")
            .voOne();
        return BeanUtil.copyProperties(vo, UserDTO.class);
    }

    /**
     * 分页查询用户列表。
     *
     * @param user      用户筛选条件
     * @param pageQuery 分页参数
     * @return 用户分页结果
     */
    @Override
    public PageResult<SysUserVo> selectPageUserList(SysUserBo user, PageQuery pageQuery) {
        Page<SysUserVo> page = userMapper.selectPageUserList(pageQuery.build(), this.buildQueryWrapper(user));
        fillUserTypes(page.getRecords());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 根据条件分页查询用户列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    public List<SysUserExportVo> selectUserExportList(SysUserBo user) {
        List<Long> deptIds = ObjectUtil.isNotNull(user.getDeptId()) ? deptMapper.selectDeptAndChildById(user.getDeptId()) : null;
        return userMapper.selectUserExportList(user, deptIds);
    }

    /**
     * 构造用户列表查询条件。
     *
     * @param user 用户筛选条件
     * @return 叠加部门、状态、时间区间等条件的查询包装器
     */
    private Wrapper<SysUser> buildQueryWrapper(SysUserBo user) {
        Map<String, Object> params = user.getParams();
        LambdaQueryWrapper<SysUser> wrapper = QueryBuilder.lambda(SysUser.class)
            .eqIfPresent(SysUser::getUserId, user.getUserId())
            .in(StringUtils.isNotBlank(user.getUserIds()), SysUser::getUserId, StringUtils.splitTo(user.getUserIds(), Convert::toLong))
            .likeIfText(SysUser::getUserName, user.getUserName())
            .likeIfText(SysUser::getNickName, user.getNickName())
            .eqIfText(SysUser::getStatus, user.getStatus())
            .likeIfText(SysUser::getPhoneNumber, user.getPhoneNumber())
            .betweenParams(SysUser::getCreateTime, params, "beginTime", "endTime")
            .and(ObjectUtil.isNotNull(user.getDeptId()), w -> {
                List<Long> ids = deptMapper.selectDeptAndChildById(user.getDeptId());
                w.in(SysUser::getDeptId, ids);
            })
            .orderByAsc(SysUser::getUserId)
            .build();
        if (StringUtils.isNotBlank(user.getExcludeUserIds())) {
            wrapper.notIn(SysUser::getUserId, StringUtils.splitTo(user.getExcludeUserIds(), Convert::toLong));
        }
        if (StringUtils.isNotBlank(user.getKeyword())) {
            String keyword = user.getKeyword().strip();
            wrapper.and(w -> w.like(SysUser::getUserName, keyword)
                .or().like(SysUser::getNickName, keyword)
                .or().like(SysUser::getPhoneNumber, keyword));
        }
        return wrapper;
    }

    /**
     * 根据条件分页查询已分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    public PageResult<SysUserVo> selectAllocatedList(SysUserBo user, PageQuery pageQuery) {
        Page<SysUserVo> page = userMapper.selectAllocatedList(pageQuery.build(), user);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    public PageResult<SysUserVo> selectUnallocatedList(SysUserBo user, PageQuery pageQuery) {
        List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(user.getRoleId());
        Page<SysUserVo> page = userMapper.selectUnallocatedList(pageQuery.build(), user, userIds);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Override
    public SysUserVo selectUserByUserName(String userName) {
        return userMapper.lambda().eq(SysUser::getUserName, userName).voOne();
    }

    /**
     * 通过手机号查询用户
     *
     * @param phoneNumber 手机号
     * @return 用户对象信息
     */
    @Override
    public SysUserVo selectUserByPhoneNumber(String phoneNumber) {
        return userMapper.lambda().eq(SysUser::getPhoneNumber, phoneNumber).voOne();
    }

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    @Override
    public SysUserVo selectUserById(Long userId) {
        SysUserVo user = userMapper.selectVoById(userId);
        if (ObjectUtil.isNull(user)) {
            return user;
        }
        user.setRoles(roleMapper.selectRolesByUserId(user.getUserId(), resolveLoginClientId()));
        fillUserTypes(user);
        return user;
    }

    /**
     * 通过用户ID串查询用户
     *
     * @param userIds 用户ID串
     * @param deptId  部门id
     * @return 用户列表信息
     */
    @Override
    public List<SysUserVo> selectUserByIds(Collection<Long> userIds, Long deptId) {
        return userMapper.selectUserList(userMapper.lambda()
            .select(SysUser::getUserId, SysUser::getUserName, SysUser::getNickName)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .eqIfPresent(SysUser::getDeptId, deptId)
            .inIfNotEmpty(SysUser::getUserId, userIds)
            .build());
    }

    /**
     * 查询用户所属角色组
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(Long userId) {
        List<SysRoleVo> list = roleMapper.selectRolesByUserId(userId, resolveLoginClientId());
        if (CollUtil.isEmpty(list)) {
            return StringUtils.EMPTY;
        }
        return StreamUtils.join(list, SysRoleVo::getRoleName);
    }

    /**
     * 查询用户所属岗位组
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    public String selectUserPostGroup(Long userId) {
        List<SysPostVo> list = postMapper.selectPostsByUserId(userId);
        if (CollUtil.isEmpty(list)) {
            return StringUtils.EMPTY;
        }
        return StreamUtils.join(list, SysPostVo::getPostName);
    }

    /**
     * 校验用户账号是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkUserNameUnique(SysUserBo user) {
        boolean exist = userMapper.lambda()
            .eq(SysUser::getUserName, user.getUserName())
            .neIfPresent(SysUser::getUserId, user.getUserId())
            .exists();
        return !exist;
    }

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     */
    @Override
    public boolean checkPhoneUnique(SysUserBo user) {
        boolean exist = userMapper.lambda()
            .eq(SysUser::getPhoneNumber, user.getPhoneNumber())
            .neIfPresent(SysUser::getUserId, user.getUserId())
            .exists();
        return !exist;
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     */
    @Override
    public boolean checkEmailUnique(SysUserBo user) {
        boolean exist = userMapper.lambda()
            .eq(SysUser::getEmail, user.getEmail())
            .neIfPresent(SysUser::getUserId, user.getUserId())
            .exists();
        return !exist;
    }

    /**
     * 校验用户是否允许操作
     *
     * @param userId 用户ID
     */
    @Override
    public void checkUserAllowed(Long userId) {
        if (ObjectUtil.isNotNull(userId) && LoginHelper.isSuperAdmin(userId)) {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * 校验用户是否有数据权限
     *
     * @param userId 用户id
     */
    @Override
    public void checkUserDataScope(Long userId) {
        if (ObjectUtil.isNull(userId)) {
            return;
        }
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        if (userMapper.countUserById(userId) == 0) {
            throw new ServiceException("没有权限访问用户数据！");
        }
    }

    /**
     * 新增保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @DSTransactional
    public int insertUser(SysUserBo user) {
        user.setPhoneNumber(UserPhonePolicy.requirePhone(user.getPhoneNumber()));
        SysUser sysUser = MapstructUtils.convert(user, SysUser.class);
        // 新增用户信息
        int rows = userMapper.insert(sysUser);
        user.setUserId(sysUser.getUserId());
        userTypeRelService.coverUserTypes(user.getUserId(), user.getUserTypeIds(), UserTypeGrantSource.ADMIN_CREATE);
        // 新增用户岗位关联
        insertUserPost(user, false);
        // 新增用户与角色管理
        insertUserRole(user, false);
        if (rows > 0) {
            reconcileAvatarReferences(user.getUserId(), null, user.getAvatar());
        }
        return rows;
    }

    /**
     * 注册用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @DSTransactional
    public boolean registerUser(SysUserBo user) {
        user.setPhoneNumber(UserPhonePolicy.requirePhone(user.getPhoneNumber()));
        user.setCreateBy(0L);
        user.setUpdateBy(0L);
        SysUser sysUser = MapstructUtils.convert(user, SysUser.class);
        boolean flag = userMapper.insert(sysUser) > 0;
        if (flag) {
            user.setUserId(sysUser.getUserId());
            reconcileAvatarReferences(user.getUserId(), null, user.getAvatar());
        }
        return flag;
    }

    /**
     * 修改保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @CacheEvict(cacheNames = CacheNames.SYS_NICKNAME, key = "#user.userId")
    @DSTransactional
    public int updateUser(SysUserBo user) {
        SysUser existing = userMapper.selectById(user.getUserId());
        if (existing == null) {
            throw new ServiceException("用户不存在");
        }
        user.setPhoneNumber(UserPhonePolicy.forUpdate(user.getPhoneNumber(), existing.getPhoneNumber()));
        Long previousAvatar = existing.getAvatar();
        if (user.getUserTypeIds() != null) {
            userTypeRelService.coverUserTypes(user.getUserId(), user.getUserTypeIds(), UserTypeGrantSource.ADMIN_GRANT);
        }
        // 新增用户与角色管理
        insertUserRole(user, true);
        // 新增用户与岗位管理
        insertUserPost(user, true);
        SysUser sysUser = MapstructUtils.convert(user, SysUser.class);
        // 防止错误更新后导致的数据误删除
        int flag = userMapper.updateById(sysUser);
        if (flag < 1) {
            throw new ServiceException("修改用户{}信息失败", user.getUserName());
        }
        if (SystemConstants.DISABLE.equals(sysUser.getStatus())) {
            kickoutUserTypes(user.getUserId(), userTypeRelService.selectByUserId(user.getUserId()));
        }
        if (user.getAvatar() != null) {
            reconcileAvatarReferences(user.getUserId(), previousAvatar, user.getAvatar());
        }
        openApiSessionInvalidator.invalidateByUserId(user.getUserId());
        return flag;
    }

    /**
     * 按客户端授权用户角色。只替换该客户端下的显式角色，不影响其他客户端。
     *
     * @param userId   用户ID
     * @param roleIds  角色组，空数组表示撤销该客户端全部显式角色
     * @param clientId 客户端主键
     */
    @Override
    @DSTransactional
    public void insertUserAuth(Long userId, Long[] roleIds, Long clientId) {
        if (ObjectUtil.isNull(clientId)) {
            throw new ServiceException("请选择客户端");
        }
        insertUserRole(userId, roleIds == null ? new Long[0] : roleIds, true, clientId);
    }

    /**
     * 修改用户状态
     *
     * @param userId 用户ID
     * @param status 账号状态
     * @return 结果
     */
    @Override
    @DSTransactional
    public int updateUserStatus(Long userId, String status) {
        List<SysUserTypeRelVo> userTypes = SystemConstants.DISABLE.equals(status)
            ? userTypeRelService.selectByUserId(userId) : List.of();
        int rows = userMapper.lambda()
            .set(SysUser::getStatus, status)
            .eq(SysUser::getUserId, userId)
            .updateCount();
        if (rows > 0 && SystemConstants.DISABLE.equals(status)) {
            kickoutUserTypes(userId, userTypes);
        }
        if (rows > 0) {
            openApiSessionInvalidator.invalidateByUserId(userId);
        }
        return rows;
    }

    /**
     * 修改用户基本信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @CacheEvict(cacheNames = CacheNames.SYS_NICKNAME, key = "#user.userId")
    @DSTransactional
    @Override
    public int updateUserProfile(SysUserBo user) {
        SysUser existing = userMapper.selectById(user.getUserId());
        if (existing == null) {
            throw new ServiceException("用户不存在");
        }
        user.setPhoneNumber(UserPhonePolicy.forUpdate(user.getPhoneNumber(), existing.getPhoneNumber()));
        Long previousAvatar = existing.getAvatar();
        int rows = userMapper.lambda()
            .setIfPresent(SysUser::getNickName, user.getNickName())
            .setIfPresent(SysUser::getAvatar, user.getAvatar())
            .setIfPresent(SysUser::getPhoneNumber, user.getPhoneNumber())
            .setIfPresent(SysUser::getEmail, user.getEmail())
            .setIfPresent(SysUser::getGender, user.getGender())
            .eq(SysUser::getUserId, user.getUserId())
            .updateCount();
        if (rows > 0 && user.getAvatar() != null) {
            reconcileAvatarReferences(user.getUserId(), previousAvatar, user.getAvatar());
        }
        return rows;
    }

    /**
     * 重置用户密码
     *
     * @param userId   用户ID
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(Long userId, String password) {
        return userMapper.lambda()
            .set(SysUser::getPassword, password)
            .eq(SysUser::getUserId, userId)
            .updateCount();
    }

    /**
     * 新增用户角色信息
     *
     * @param user  用户对象
     * @param clear 清除已存在的关联数据
     */
    private void insertUserRole(SysUserBo user, boolean clear) {
        this.insertUserRole(user.getUserId(), user.getRoleIds(), clear, null);
    }

    /**
     * 新增用户岗位信息
     *
     * @param user  用户对象
     * @param clear 清除已存在的关联数据
     */
    private void insertUserPost(SysUserBo user, boolean clear) {
        Long[] postIdArr = user.getPostIds();
        if (ArrayUtil.isEmpty(postIdArr)) {
            return;
        }
        List<Long> postIds = Arrays.asList(postIdArr);

        // 校验是否有权限操作这些岗位（含数据权限控制）
        if (postMapper.selectPostCount(postIds) != postIds.size()) {
            throw new ServiceException("没有权限访问岗位的数据");
        }

        // 是否清除旧的用户岗位绑定
        if (clear) {
            userPostMapper.lambda().eq(SysUserPost::getUserId, user.getUserId()).delete();
        }

        // 构建用户岗位关联列表并批量插入
        List<SysUserPost> list = StreamUtils.toList(postIds,
            postId -> {
                SysUserPost up = new SysUserPost();
                up.setUserId(user.getUserId());
                up.setPostId(postId);
                return up;
            });
        userPostMapper.insertBatch(list);
    }

    /**
     * 新增用户角色信息
     *
     * @param userId   用户ID
     * @param roleIds  角色组；null 表示不改角色（如导入更新），空数组且 clear=true 表示撤销
     * @param clear    清除已存在的关联数据
     * @param clientId 非空时只替换该客户端下的显式角色
     */
    private void insertUserRole(Long userId, Long[] roleIds, boolean clear, Long clientId) {
        if (roleIds == null) {
            return;
        }
        List<Long> roleList = new ArrayList<>();
        for (Long roleId : roleIds) {
            if (roleId != null) {
                roleList.add(roleId);
            }
        }
        List<Long> oldRoleIds = selectUserRoleIds(userId, clientId);

        if (roleList.isEmpty()) {
            if (clear) {
                deleteUserRoles(userId, clientId);
                invalidateUserRoleSessions(userId, clientId, oldRoleIds);
            }
            return;
        }

        // 非超级管理员，禁止包含超级管理员角色
        if (!LoginHelper.isSuperAdmin(userId)) {
            roleList.remove(SystemConstants.SUPER_ADMIN_ROLE_ID);
        }

        // 移除超管角色后若无剩余角色，说明仅选了超管角色且不允许分配，显式报错
        if (roleList.isEmpty()) {
            throw new ServiceException("不允许为普通用户分配超级管理员角色，请至少选择一个其他角色");
        }

        // 校验是否有权限访问这些角色（含数据权限控制）
        if (roleMapper.selectRoleCount(roleList) != roleList.size()) {
            throw new ServiceException("没有权限访问角色的数据");
        }
        if (ObjectUtil.isNotNull(clientId)) {
            List<SysRole> roles = roleMapper.selectByIds(roleList);
            for (SysRole role : roles) {
                if (ObjectUtil.isNull(role) || !clientId.equals(role.getClientId())) {
                    throw new ServiceException("角色必须属于当前客户端");
                }
            }
        }
        validateRolesUserType(userId, roleList);

        if (clear) {
            deleteUserRoles(userId, clientId);
        }

        List<SysUserRole> list = StreamUtils.toList(roleList,
            roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                return ur;
            });
        userRoleMapper.insertBatch(list);
        Set<Long> kickRoleIds = new HashSet<>(oldRoleIds);
        kickRoleIds.addAll(roleList);
        invalidateUserRoleSessions(userId, clientId, kickRoleIds);
    }

    private void invalidateUserRoleSessions(Long userId, Long clientId, Collection<Long> roleIds) {
        if (ObjectUtil.isNotNull(clientId)) {
            clientSessionService.kickoutUserClient(userId, clientId);
            openApiSessionInvalidator.invalidateByUserId(userId);
            return;
        }
        kickUserRoleClients(userId, roleIds);
        openApiSessionInvalidator.invalidateByUserId(userId);
    }

    /**
     * 查询用户已绑定的显式角色 ID。
     *
     * @param userId   用户ID
     * @param clientId 非空时只返回该客户端角色
     * @return 角色 ID 列表
     */
    private List<Long> selectUserRoleIds(Long userId, Long clientId) {
        List<Long> roleIds = userRoleMapper.lambda()
            .eq(SysUserRole::getUserId, userId)
            .list()
            .stream()
            .map(SysUserRole::getRoleId)
            .filter(Objects::nonNull)
            .toList();
        if (CollUtil.isEmpty(roleIds) || ObjectUtil.isNull(clientId)) {
            return roleIds;
        }
        return roleMapper.selectByIds(roleIds).stream()
            .filter(role -> clientId.equals(role.getClientId()))
            .map(SysRole::getRoleId)
            .toList();
    }

    /**
     * 删除用户显式角色。clientId 非空时只删除该客户端下的绑定。
     *
     * @param userId   用户ID
     * @param clientId 客户端主键
     */
    private void deleteUserRoles(Long userId, Long clientId) {
        if (ObjectUtil.isNull(clientId)) {
            userRoleMapper.lambda().eq(SysUserRole::getUserId, userId).delete();
            return;
        }
        List<Long> roleIds = selectUserRoleIds(userId, clientId);
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        userRoleMapper.lambda()
            .eq(SysUserRole::getUserId, userId)
            .in(SysUserRole::getRoleId, roleIds)
            .delete();
    }

    /**
     * 通过用户ID删除用户
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @DSTransactional
    public int deleteUserById(Long userId) {
        SysUser existing = userMapper.selectById(userId);
        List<SysUserTypeRelVo> userTypes = userTypeRelService.selectByUserId(userId);
        // 删除用户与角色关联
        userRoleMapper.lambda().eq(SysUserRole::getUserId, userId).delete();
        // 删除用户与岗位表
        userPostMapper.lambda().eq(SysUserPost::getUserId, userId).delete();
        userTypeRelService.deleteByUserIds(List.of(userId));
        kickoutUserTypes(userId, userTypes);
        // 防止更新失败导致的数据删除
        int flag = userMapper.deleteById(userId);
        if (flag < 1) {
            throw new ServiceException("删除用户失败!");
        }
        openApiSessionInvalidator.invalidateByUserId(userId);
        if (existing != null) {
            reconcileAvatarReferences(userId, existing.getAvatar(), null);
        }
        return flag;
    }

    /**
     * 批量删除用户信息
     *
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    @Override
    @DSTransactional
    public int deleteUserByIds(Long[] userIds) {
        for (Long userId : userIds) {
            checkUserAllowed(userId);
            checkUserDataScope(userId);
        }
        List<Long> ids = List.of(userIds);
        List<SysUser> existingUsers = userMapper.selectBatchIds(ids);
        List<SysUserTypeRelVo> userTypes = userTypeRelService.selectByUserIds(ids);
        // 删除用户与角色关联
        userRoleMapper.lambda().in(SysUserRole::getUserId, ids).delete();
        // 删除用户与岗位表
        userPostMapper.lambda().in(SysUserPost::getUserId, ids).delete();
        userTypeRelService.deleteByUserIds(ids);
        Map<Long, List<SysUserTypeRelVo>> userTypesByUser = StreamUtils.groupByKey(userTypes, SysUserTypeRelVo::getUserId);
        for (Long userId : ids) {
            kickoutUserTypes(userId, userTypesByUser.getOrDefault(userId, List.of()));
        }
        // 防止更新失败导致的数据删除
        int flag = userMapper.deleteByIds(ids);
        if (flag < 1) {
            throw new ServiceException("删除用户失败!");
        }
        ids.forEach(openApiSessionInvalidator::invalidateByUserId);
        for (SysUser existing : existingUsers) {
            reconcileAvatarReferences(existing.getUserId(), existing.getAvatar(), null);
        }
        return flag;
    }

    private void reconcileAvatarReferences(Long userId, Long previousAvatar, Long currentAvatar) {
        ossService.reconcileReferences(USER_TABLE, String.valueOf(userId),
            avatarOssIds(previousAvatar), avatarOssIds(currentAvatar));
    }

    private List<Long> avatarOssIds(Long avatar) {
        return avatar == null ? List.of() : List.of(avatar);
    }

    /**
     * 通过部门id查询当前部门所有用户
     *
     * @param deptId 部门ID
     * @return 用户信息集合信息
     */
    @Override
    public List<SysUserVo> selectUserListByDept(Long deptId) {
        return userMapper.lambda()
            .eq(SysUser::getDeptId, deptId)
            .orderByAsc(SysUser::getUserId)
            .voList();
    }

    /**
     * 通过用户ID查询用户账户
     *
     * @param userId 用户ID
     * @return 用户账户
     */
    @Cacheable(cacheNames = CacheNames.SYS_USER_NAME, key = "#userId")
    @Override
    public String selectUserNameById(Long userId) {
        SysUser sysUser = userMapper.lambda()
            .select(SysUser::getUserName)
            .eq(SysUser::getUserId, userId)
            .one();
        return ObjectUtils.notNullGetter(sysUser, SysUser::getUserName);
    }

    /**
     * 通过用户ID查询用户昵称
     *
     * @param userId 用户ID
     * @return 用户昵称
     */
    @Override
    @Cacheable(cacheNames = CacheNames.SYS_NICKNAME, key = "#userId")
    public String selectNicknameById(Long userId) {
        SysUser sysUser = userMapper.lambda()
            .select(SysUser::getNickName)
            .eq(SysUser::getUserId, userId)
            .one();
        return ObjectUtils.notNullGetter(sysUser, SysUser::getNickName);
    }

    /**
     * 通过用户ID查询用户昵称
     *
     * @param userIds 用户ID 多个用逗号隔开
     * @return 用户昵称
     */
    @Override
    public String selectNicknameByIds(String userIds) {
        List<String> list = new ArrayList<>();
        for (Long id : StringUtils.splitTo(userIds, Convert::toLong)) {
            String nickname = SpringUtils.getAopProxy(this).selectNicknameById(id);
            if (StringUtils.isNotBlank(nickname)) {
                list.add(nickname);
            }
        }
        return StringUtils.joinComma(list);
    }

    /**
     * 通过用户ID查询用户手机号
     *
     * @param userId 用户id
     * @return 用户手机号
     */
    @Override
    public String selectPhonenumberById(Long userId) {
        SysUser sysUser = userMapper.lambda()
            .select(SysUser::getPhoneNumber)
            .eq(SysUser::getUserId, userId)
            .one();
        return ObjectUtils.notNullGetter(sysUser, SysUser::getPhoneNumber);
    }

    /**
     * 通过用户ID查询用户邮箱
     *
     * @param userId 用户id
     * @return 用户邮箱
     */
    @Override
    public String selectEmailById(Long userId) {
        SysUser sysUser = userMapper.lambda()
            .select(SysUser::getEmail)
            .eq(SysUser::getUserId, userId)
            .one();
        return ObjectUtils.notNullGetter(sysUser, SysUser::getEmail);
    }

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户id
     * @return 用户列表
     */
    @Override
    public UserDTO selectById(Long userId) {
        SysUserVo vo = userMapper.lambda()
            .select(SysUser::getUserId, SysUser::getDeptId, SysUser::getUserName,
                SysUser::getNickName, SysUser::getEmail,
                SysUser::getPhoneNumber, SysUser::getGender, SysUser::getStatus,
                SysUser::getCreateTime)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .eq(SysUser::getUserId, userId)
            .voOne();
        return BeanUtil.copyProperties(vo, UserDTO.class);
    }

    /**
     * 通过用户ID查询用户列表
     *
     * @param userIds 用户ids
     * @return 用户列表
     */
    @Override
    public List<UserDTO> selectListByIds(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return List.of();
        }
        List<SysUserVo> list = userMapper.lambda()
            .select(SysUser::getUserId, SysUser::getDeptId, SysUser::getUserName,
                SysUser::getNickName, SysUser::getEmail,
                SysUser::getPhoneNumber, SysUser::getGender, SysUser::getStatus,
                SysUser::getCreateTime)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .eq(SysUser::getDelFlag, SystemConstants.NORMAL)
            .in(SysUser::getUserId, userIds)
            .voList();
        return BeanUtil.copyToList(list, UserDTO.class);
    }

    /**
     * 查询正常用户，用于公告等全量通知的接收人快照。
     *
     * @param limit 最大返回数量
     * @return 正常用户列表
     */
    @Override
    public List<UserDTO> selectAllActiveUsers(int limit) {
        int boundedLimit = Math.clamp(limit, 1, 100_000);
        List<SysUserVo> list = userMapper.lambda()
            .select(SysUser::getUserId, SysUser::getDeptId, SysUser::getUserName,
                SysUser::getNickName, SysUser::getEmail, SysUser::getPhoneNumber,
                SysUser::getGender, SysUser::getStatus, SysUser::getCreateTime)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .eq(SysUser::getDelFlag, SystemConstants.NORMAL)
            .orderByAsc(SysUser::getUserId)
            .last("limit " + boundedLimit)
            .voList();
        return BeanUtil.copyToList(list, UserDTO.class);
    }

    @Override
    public List<UserDTO> selectAllActiveUsers(int offset, int limit) {
        int boundedOffset = Math.max(offset, 0);
        int boundedLimit = Math.clamp(limit, 1, 10_000);
        List<SysUserVo> list = userMapper.lambda()
            .select(SysUser::getUserId, SysUser::getDeptId, SysUser::getUserName,
                SysUser::getNickName, SysUser::getEmail, SysUser::getPhoneNumber,
                SysUser::getGender, SysUser::getStatus, SysUser::getCreateTime)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .eq(SysUser::getDelFlag, SystemConstants.NORMAL)
            .orderByAsc(SysUser::getUserId)
            .last("limit " + boundedOffset + "," + boundedLimit)
            .voList();
        return BeanUtil.copyToList(list, UserDTO.class);
    }

    @Override
    public List<UserDTO> selectUsersByUserTypeIds(Collection<Long> userTypeIds) {
        if (CollUtil.isEmpty(userTypeIds)) return List.of();
        List<Long> requestedTypeIds = userTypeIds.stream().filter(Objects::nonNull).distinct().toList();
        if (requestedTypeIds.isEmpty() || requestedTypeIds.stream().anyMatch(id -> id <= 0)) {
            throw new ServiceException("鐢ㄦ埛绫诲瀷缂栧彿蹇呴』涓烘鏁存暟");
        }
        Set<Long> activeTypeIds = new HashSet<>(userTypeMapper.lambda()
            .select(SysUserType::getUserTypeId)
            .in(SysUserType::getUserTypeId, requestedTypeIds)
            .eq(SysUserType::getStatus, SystemConstants.NORMAL)
            .list().stream().map(SysUserType::getUserTypeId).toList());
        if (activeTypeIds.size() != requestedTypeIds.size()) {
            throw new ServiceException("Only enabled user types are allowed");
        }
        List<Long> ids = userTypeRelService.selectUserIdsByUserTypeIds(requestedTypeIds);
        List<UserDTO> users = selectListByIds(ids);
        users.forEach(user -> checkUserDataScope(user.getUserId()));
        return users;
    }

    @Override
    public List<UserDTO> selectNotificationUsers(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return List.of();
        }
        List<Long> distinctIds = userIds.stream().filter(Objects::nonNull).distinct().toList();
        // Validate every requested identifier before filtering inactive users. An out-of-scope
        // or missing identifier must fail closed instead of silently changing the draft target.
        distinctIds.forEach(this::checkUserDataScope);
        return selectListByIds(distinctIds);
    }

    /**
     * 通过角色ID查询用户ID
     *
     * @param roleIds 角色ids
     * @return 用户ids
     */
    @Override
    public List<Long> selectUserIdsByRoleIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return List.of();
        }
        List<SysUserRole> userRoles = userRoleMapper.lambda().in(SysUserRole::getRoleId, roleIds).list();
        return StreamUtils.toList(userRoles, SysUserRole::getUserId);
    }

    /**
     * 通过角色ID查询用户
     *
     * @param roleIds 角色ids
     * @return 用户
     */
    @Override
    public List<UserDTO> selectUsersByRoleIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return List.of();
        }

        // 通过角色ID获取用户角色信息
        List<SysUserRole> userRoles = userRoleMapper.lambda().in(SysUserRole::getRoleId, roleIds).list();

        // 获取用户ID列表
        Set<Long> userIds = StreamUtils.toSet(userRoles, SysUserRole::getUserId);

        return this.selectListByIds(new ArrayList<>(userIds));
    }

    /**
     * 通过部门ID查询用户
     *
     * @param deptIds 部门ids
     * @return 用户
     */
    @Override
    public List<UserDTO> selectUsersByDeptIds(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return List.of();
        }
        List<SysUserVo> list = userMapper.lambda()
            .select(SysUser::getUserId, SysUser::getUserName, SysUser::getNickName, SysUser::getEmail, SysUser::getPhoneNumber)
            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .in(SysUser::getDeptId, deptIds)
            .voList();
        return BeanUtil.copyToList(list, UserDTO.class);
    }

    /**
     * 通过岗位ID查询用户
     *
     * @param postIds 岗位ids
     * @return 用户
     */
    @Override
    public List<UserDTO> selectUsersByPostIds(Collection<Long> postIds) {
        if (CollUtil.isEmpty(postIds)) {
            return List.of();
        }

        // 通过岗位ID获取用户岗位信息
        List<SysUserPost> userPosts = userPostMapper.lambda().in(SysUserPost::getPostId, postIds).list();

        // 获取用户ID列表
        Set<Long> userIds = StreamUtils.toSet(userPosts, SysUserPost::getUserId);

        return this.selectListByIds(new ArrayList<>(userIds));
    }

    /**
     * 根据用户 ID 列表查询用户昵称映射关系
     *
     * @param userIds 用户 ID 列表
     * @return Map，其中 key 为用户 ID，value 为对应的用户昵称
     */
    @Override
    public Map<Long, String> selectUserNicksByIds(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<SysUser> list = userMapper.lambda()
            .select(SysUser::getUserId, SysUser::getNickName)
            .in(SysUser::getUserId, userIds)
            .list();
        return StreamUtils.toMap(list, SysUser::getUserId, SysUser::getNickName);
    }

    /**
     * 读取当前登录客户端主键，无上下文时不查询跨 Client 角色。
     *
     * @return 客户端主键
     */
    private Long resolveLoginClientId() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        return loginUser == null ? null : loginUser.getClientPk();
    }

    /**
     * 回填用户拥有的登录域。
     *
     * @param user 用户视图
     */
    private void fillUserTypes(SysUserVo user) {
        if (ObjectUtil.isNull(user)) {
            return;
        }
        fillUserTypes(List.of(user));
    }

    /**
     * 批量回填用户拥有的登录域。
     *
     * @param users 用户视图列表
     */
    private void fillUserTypes(List<SysUserVo> users) {
        if (CollUtil.isEmpty(users)) {
            return;
        }
        List<Long> userIds = StreamUtils.toList(users, SysUserVo::getUserId);
        List<SysUserTypeRelVo> rels = userTypeRelService.selectByUserIds(userIds);
        Map<Long, List<SysUserTypeRelVo>> grouped = StreamUtils.groupByKey(rels, SysUserTypeRelVo::getUserId);
        for (SysUserVo user : users) {
            List<SysUserTypeRelVo> userRels = grouped.getOrDefault(user.getUserId(), List.of());
            user.setUserTypeIds(StreamUtils.toList(userRels, SysUserTypeRelVo::getUserTypeId));
            user.setUserTypeCodes(StreamUtils.toList(userRels, SysUserTypeRelVo::getUserTypeCode));
            user.setUserTypeNames(StreamUtils.toList(userRels, SysUserTypeRelVo::getUserTypeName));
        }
    }

    /**
     * 分配角色前校验用户已具备该角色客户端要求的登录域。
     *
     * @param userId   用户ID
     * @param roleIds  角色ID列表
     */
    private void validateRolesUserType(Long userId, Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        List<SysRole> roles = roleMapper.selectByIds(roleIds);
        if (roles.size() != new HashSet<>(roleIds).size()) {
            throw new ServiceException("存在无效角色");
        }
        for (SysRole role : roles) {
            if (ObjectUtil.isNull(role) || !SystemConstants.NORMAL.equals(role.getStatus())
                || ObjectUtil.isNull(role.getClientId())) {
                throw new ServiceException("角色不存在、已停用或未归属客户端");
            }
            SysClient client = clientMapper.selectById(role.getClientId());
            if (ObjectUtil.isNull(client) || !SystemConstants.NORMAL.equals(client.getStatus())) {
                throw new ServiceException("角色所属客户端不存在或已停用");
            }
            if (ObjectUtil.isNull(client.getUserTypeId())) {
                throw new ServiceException("角色所属客户端未配置登录域");
            }
            SysUserType userType = userTypeMapper.selectById(client.getUserTypeId());
            if (ObjectUtil.isNull(userType) || !SystemConstants.NORMAL.equals(userType.getStatus())) {
                throw new ServiceException("角色所属客户端的登录域不存在或已停用");
            }
            if (role.getRoleId().equals(client.getDefaultRoleId())) {
                throw new ServiceException("客户端默认角色由系统自动授予，不能显式分配");
            }
            if (!userTypeRelService.hasUserType(userId, client.getUserTypeId())) {
                throw new ServiceException("用户不具备角色[{}]所属客户端的登录域", role.getRoleName());
            }
        }
    }

    /**
     * 按用户已有登录域清理会话，不扫描或推断其他身份上下文。
     */
    private void kickoutUserTypes(Long userId, Collection<SysUserTypeRelVo> userTypes) {
        if (CollUtil.isEmpty(userTypes)) {
            return;
        }
        Set<String> codes = StreamUtils.toSet(userTypes, SysUserTypeRelVo::getUserTypeCode);
        for (String code : codes) {
            if (StringUtils.isNotBlank(code)) {
                clientSessionService.kickoutUserType(userId, code);
            }
        }
    }

    /**
     * 用户在某客户端的角色变化后，清理该客户端会话。
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     */
    private void kickUserRoleClients(Long userId, Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        List<SysRole> roles = roleMapper.selectByIds(roleIds);
        Set<Long> clientIds = StreamUtils.toSet(roles, SysRole::getClientId);
        for (Long clientId : clientIds) {
            clientSessionService.kickoutUserClient(userId, clientId);
        }
    }

}
