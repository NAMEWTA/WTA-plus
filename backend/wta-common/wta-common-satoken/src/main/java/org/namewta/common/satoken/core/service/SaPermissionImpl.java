package org.namewta.common.satoken.core.service;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.model.LoginUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * sa-token 权限管理实现类
 *
 * @author Lion Li
 */
public class SaPermissionImpl implements StpInterface {

    /**
     * 获取指定登录对象的菜单权限列表。
     *
     * @param loginId   登录ID
     * @param loginType 登录类型
     * @return 菜单权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return resolvePermissionList(loginId, LoginUser::getMenuPermission);
    }

    /**
     * 获取指定登录对象的角色权限列表。
     *
     * @param loginId   登录ID
     * @param loginType 登录类型
     * @return 角色权限列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return resolvePermissionList(loginId, LoginUser::getRolePermission);
    }

    /**
     * 仅从当前 Token 登录快照读取权限；无上下文时返回空集合，禁止按 userId 查全局权限。
     *
     * @param loginId                  登录ID
     * @param localPermissionExtractor 当前登录用户权限提取器
     * @return 权限列表
     */
    private List<String> resolvePermissionList(Object loginId,
                                               Function<LoginUser, Collection<String>> localPermissionExtractor) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (ObjectUtil.isNull(loginUser) || !loginUser.getLoginId().equals(loginId)) {
            return new ArrayList<>();
        }
        Collection<String> permissionList = localPermissionExtractor.apply(loginUser);
        if (CollUtil.isNotEmpty(permissionList)) {
            return new ArrayList<>(permissionList);
        }
        return new ArrayList<>();
    }

}
