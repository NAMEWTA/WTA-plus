package org.namewta.web.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.lock.annotation.Lock4j;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthUser;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.constant.Constants;
import org.namewta.common.core.enums.LoginType;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.exception.user.UserException;
import org.namewta.common.core.utils.*;
import org.namewta.common.log.event.LoginInfoEvent;
import org.namewta.common.mybatis.helper.DataPermissionHelper;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.domain.PostDTO;
import org.namewta.system.api.domain.RoleDTO;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.domain.SysUser;
import org.namewta.system.domain.bo.SysSocialBo;
import org.namewta.system.domain.vo.*;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * 登录校验方法
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class SysLoginService {

    /**
     * 最大重试次数。
     */
    @Value("${user.password.maxRetryCount}")
    private Integer maxRetryCount;

    /**
     * 锁定时间。
     */
    @Value("${user.password.lockTime}")
    private Integer lockTime;

    private final ISysPermissionService permissionService;
    private final ISysSocialService sysSocialService;
    private final ISysRoleService roleService;
    private final ISysDeptService deptService;
    private final ISysPostService postService;
    private final SysUserMapper userMapper;


    /**
     * 绑定第三方用户
     *
     * @param authUserData 授权响应实体
     */
    @Lock4j
    public void socialRegister(AuthUser authUserData) {
        String authId = authUserData.getSource() + authUserData.getUuid();
        // 第三方用户信息
        SysSocialBo bo = BeanUtil.toBean(authUserData, SysSocialBo.class);
        BeanUtil.copyProperties(authUserData.getToken(), bo);
        Long userId = LoginHelper.getUserId();
        bo.setUserId(userId);
        bo.setAuthId(authId);
        bo.setOpenId(authUserData.getUuid());
        bo.setUserName(authUserData.getUsername());
        bo.setNickName(authUserData.getNickname());
        List<SysSocialVo> checkList = sysSocialService.selectByAuthId(authId);
        if (CollUtil.isNotEmpty(checkList)) {
            throw new ServiceException("此三方账号已经被绑定!");
        }
        // 查询是否已经绑定用户
        SysSocialBo params = new SysSocialBo();
        params.setUserId(userId);
        params.setSource(bo.getSource());
        List<SysSocialVo> list = sysSocialService.queryList(params);
        if (CollUtil.isEmpty(list)) {
            // 没有绑定用户, 新增用户信息
            sysSocialService.insertByBo(bo);
        } else {
            // 更新用户信息
            bo.setId(list.getFirst().getId());
            sysSocialService.updateByBo(bo);
            // 如果要绑定的平台账号已经被绑定过了 是否抛异常自行决断
            // throw new ServiceException("此平台账号已经被绑定!");
        }
    }


    /**
     * 退出登录
     */
    public void logout() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (ObjectUtil.isNull(loginUser)) {
                return;
            }
            recordLoginInfo(loginUser.getUsername(), Constants.LOGOUT, MessageUtils.message("user.logout.success"));
        } catch (NotLoginException ignored) {
        } finally {
            try {
                StpUtil.logout();
            } catch (NotLoginException ignored) {
            }
        }
    }

    /**
     * 记录登录信息
     *
     * @param username 用户名
     * @param status   状态
     * @param message  消息内容
     */
    public void recordLoginInfo(String username, String status, String message) {
        LoginInfoEvent loginInfoEvent = new LoginInfoEvent();
        loginInfoEvent.setUsername(username);
        loginInfoEvent.setStatus(status);
        loginInfoEvent.setMessage(message);
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null) {
            loginInfoEvent.setIp(ServletUtils.getClientIP(request));
            loginInfoEvent.setUserAgent(request.getHeader("User-Agent"));
            loginInfoEvent.setClientId(request.getHeader(LoginHelper.CLIENT_KEY));
        }
        SpringUtils.context().publishEvent(loginInfoEvent);
    }

    /**
     * 根据用户视图对象组装登录态上下文。
     *
     * @param user           用户基础信息
     * @param client         当前客户端
     * @param activeUserType 当前客户端要求且用户已拥有的登录域
     * @return 包含部门、角色、岗位与权限数据的登录用户
     */
    public LoginUser buildLoginUser(SysUserVo user, SysClientVo client, SysUserTypeVo activeUserType) {
        LoginUser loginUser = new LoginUser();
        Long userId = user.getUserId();
        Long clientId = ObjectUtil.isNull(client) ? null : client.getId();
        loginUser.setUserId(userId);
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickname(user.getNickName());
        if (ObjectUtil.isNotNull(activeUserType)) {
            loginUser.setUserType(activeUserType.getUserTypeCode());
            loginUser.setUserTypeId(activeUserType.getUserTypeId());
        }
        if (ObjectUtil.isNotNull(client)) {
            loginUser.setClientPk(client.getId());
            loginUser.setClientKey(client.getClientKey());
            loginUser.setDeviceType(client.getDeviceType());
        }
        if (ObjectUtil.isNotNull(user.getDeptId())) {
            Opt<SysDeptVo> deptOpt = Opt.of(user.getDeptId()).map(deptService::selectDeptById);
            loginUser.setDeptName(deptOpt.map(SysDeptVo::getDeptName).orElse(StringUtils.EMPTY));
            loginUser.setDeptCategory(deptOpt.map(SysDeptVo::getDeptCategory).orElse(StringUtils.EMPTY));
        }
        ThreadUtils.virtualInvokeAll(() -> {
            loginUser.setMenuPermission(permissionService.getMenuPermission(userId, clientId));
        }, () -> {
            loginUser.setRolePermission(permissionService.getRolePermission(userId, clientId));
        }, () -> {
            List<SysRoleVo> roles = roleService.selectRolesByUserId(userId, clientId);
            List<RoleDTO> roleDtos = BeanUtil.copyToList(roles, RoleDTO.class);
            loginUser.setRoles(roleDtos);
            loginUser.setDataScopeRoleMap(permissionService.getDataScopeRoleMap(roleDtos));
        }, () -> {
            List<SysPostVo> posts = postService.selectPostsByUserId(userId);
            loginUser.setPosts(BeanUtil.copyToList(posts, PostDTO.class));
        });
        return loginUser;
    }

    /**
     * 更新用户最近一次登录IP与登录时间。
     *
     * @param userId 用户ID
     * @param ip     登录IP
     */
    public void updateLastLoginInfo(Long userId, String ip) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(ip);
        sysUser.setLoginDate(LocalDateTime.now());
        sysUser.setUpdateBy(userId);
        DataPermissionHelper.ignore(() -> userMapper.updateById(sysUser));
    }

    /**
     * 执行登录失败次数校验，并在成功后清空失败计数。
     *
     * @param loginType 登录类型
     * @param username  登录标识
     * @param supplier  返回 {@code true} 表示本次认证失败
     */
    public void checkLogin(LoginType loginType, String username, Supplier<Boolean> supplier) {
        checkLoginAllowed(loginType, username);
        if (supplier.get()) {
            throw loginFailed(loginType, username);
        }
        loginSucceeded(username);
    }

    /**
     * 在执行一次凭据组合判定前检查账号是否已达到锁定阈值。
     *
     * @param loginType 登录类型
     * @param username  登录标识
     */
    public void checkLoginAllowed(LoginType loginType, String username) {
        String errorKey = CacheNames.PWD_ERR_CNT_KEY + username;
        int errorNumber = ObjectUtil.defaultIfNull(RedisUtils.getCacheObject(errorKey), 0);
        if (errorNumber >= maxRetryCount) {
            recordLoginInfo(username, Constants.LOGIN_FAIL,
                MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
            throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
        }
    }

    /**
     * 为一次失败的组合凭据判定递增一次计数并构造通用登录异常。
     */
    public UserException loginFailed(LoginType loginType, String username) {
        String errorKey = CacheNames.PWD_ERR_CNT_KEY + username;
        int errorNumber = ObjectUtil.defaultIfNull(RedisUtils.getCacheObject(errorKey), 0) + 1;
        RedisUtils.setCacheObject(errorKey, errorNumber, Duration.ofMinutes(lockTime));
        if (errorNumber >= maxRetryCount) {
            recordLoginInfo(username, Constants.LOGIN_FAIL,
                MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
            return new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
        }
        recordLoginInfo(username, Constants.LOGIN_FAIL,
            MessageUtils.message(loginType.getRetryLimitCount(), errorNumber));
        return new UserException(loginType.getRetryLimitCount(), errorNumber);
    }

    /**
     * 一次组合凭据判定成功后清空失败计数。
     */
    public void loginSucceeded(String username) {
        RedisUtils.deleteObject(CacheNames.PWD_ERR_CNT_KEY + username);
    }

}
