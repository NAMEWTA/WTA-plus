package org.namewta.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.constant.Constants;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.MapstructUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.core.utils.TreeBuildUtils;
import org.namewta.common.openapi.session.OpenApiMachineSessionInvalidator;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.domain.SysClient;
import org.namewta.system.domain.SysMenu;
import org.namewta.system.domain.SysRole;
import org.namewta.system.domain.SysRoleMenu;
import org.namewta.system.domain.SysUserRole;
import org.namewta.system.domain.SysUserTypeRel;
import org.namewta.system.domain.bo.SysMenuBo;
import org.namewta.system.domain.vo.MetaVo;
import org.namewta.system.domain.vo.RouterVo;
import org.namewta.system.domain.vo.SysMenuVo;
import org.namewta.system.mapper.SysClientMapper;
import org.namewta.system.mapper.SysMenuMapper;
import org.namewta.system.mapper.SysRoleMapper;
import org.namewta.system.mapper.SysRoleMenuMapper;
import org.namewta.system.mapper.SysUserRoleMapper;
import org.namewta.system.mapper.SysUserTypeRelMapper;
import org.namewta.system.service.ClientSessionService;
import org.namewta.system.service.ISysClientDefaultRoleResolverService;
import org.namewta.system.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 菜单 业务层处理
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysMenuServiceImpl implements ISysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final ClientSessionService clientSessionService;
    private final ISysClientDefaultRoleResolverService defaultRoleResolverService;
    private final SysUserRoleMapper userRoleMapper;
    private final SysClientMapper clientMapper;
    private final SysUserTypeRelMapper userTypeRelMapper;
    private OpenApiMachineSessionInvalidator openApiSessionInvalidator = ignored -> 0;

    @Autowired(required = false)
    void setOpenApiSessionInvalidator(OpenApiMachineSessionInvalidator openApiSessionInvalidator) {
        this.openApiSessionInvalidator = Objects.requireNonNull(openApiSessionInvalidator);
    }

    /**
     * 根据用户查询系统菜单列表
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Override
    public List<SysMenuVo> selectMenuList(Long userId) {
        return selectMenuList(new SysMenuBo(), userId);
    }

    /**
     * 查询系统菜单列表
     *
     * @param menu   菜单筛选条件
     * @param userId 当前查询的用户主键
     * @return 菜单列表
     */
    @Override
    public List<SysMenuVo> selectMenuList(SysMenuBo menu, Long userId) {
        Long clientId = resolveClientId(menu.getClientId());
        if (ObjectUtil.isNull(clientId)) {
            return CollUtil.newArrayList();
        }
        menu.setClientId(clientId);
        // 管理员显示当前客户端全部菜单，不返回跨 Client 并集
        if (LoginHelper.isSuperAdmin(userId)) {
            return menuMapper.lambda()
                .eq(SysMenu::getClientId, clientId)
                .likeIfText(SysMenu::getMenuName, menu.getMenuName())
                .eqIfText(SysMenu::getVisible, menu.getVisible())
                .eqIfText(SysMenu::getStatus, menu.getStatus())
                .eqIfText(SysMenu::getMenuType, menu.getMenuType())
                .eqIfPresent(SysMenu::getParentId, menu.getParentId())
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum)
                .voList();
        }
        return mergeMenuVos(menuMapper.selectMenuListByUserId(menu, userId, clientId),
            menuMapper.selectMenuListByRoleId(menu, defaultRoleResolverService.resolveRoleId(clientId), clientId));
    }

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByUserId(Long userId, Long clientId) {
        Set<String> perms = new HashSet<>(menuMapper.selectMenuPermsByUserId(userId, clientId));
        Long defaultRoleId = defaultRoleResolverService.resolveRoleId(clientId);
        if (defaultRoleId != null) {
            perms.addAll(menuMapper.selectMenuPermsByRoleId(defaultRoleId));
        }
        return perms;
    }

    /**
     * 根据角色ID查询权限
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByRoleId(Long roleId) {
        return menuMapper.selectMenuPermsByRoleId(roleId);
    }

    /**
     * 根据角色ID列表批量查询权限
     *
     * @param roleIds 角色ID列表
     * @return 角色权限映射
     */
    @Override
    public Map<Long, Set<String>> selectMenuPermsByRoleIds(Collection<Long> roleIds) {
        return menuMapper.selectMenuPermsByRoleIds(roleIds);
    }

    /**
     * 根据用户ID查询菜单树信息
     *
     * @param userId 用户ID
     * @return 按树结构组织的菜单列表
     */
    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId, Long clientId) {
        List<SysMenu> menus;
        if (ObjectUtil.isNull(clientId)) {
            return CollUtil.newArrayList();
        }
        if (LoginHelper.isSuperAdmin(userId)) {
            menus = menuMapper.selectMenuTreeAll(clientId);
        } else {
            menus = mergeMenus(
                menuMapper.selectMenuTreeByUserId(userId, clientId),
                menuMapper.selectMenuTreeByRoleId(defaultRoleResolverService.resolveRoleId(clientId), clientId));
        }
        if (CollUtil.isEmpty(menus)) {
            return CollUtil.newArrayList();
        }

        List<SysMenu> menuTree = TreeBuildUtils.build(menus, Constants.TOP_PARENT_ID, SysMenu::getParentId, (menu, nodeTreeMaps) -> {
            // 将当前节点的菜单ID用作父节点ID
            Long menuParentId = menu.getMenuId();
            // 从动态规划表中取出子节点列表
            // 如果不存在子节点，则返回一个空的列表，确保数据在进行JSON序列化时该字段的类型和结构是正确的
            List<SysMenu> childMenus = nodeTreeMaps.getOrDefault(menuParentId, Collections.emptyList());
            // 设置子节点
            // 如果存在根节点指向尾节点的情况，则会出现环形依赖。但在菜单表中基本不会出现这种情况...
            menu.setChildren(childMenus);
        });
        return CollUtil.isEmpty(menuTree) ? CollUtil.newArrayList() : menuTree;
    }

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    @Override
    public List<Long> selectMenuListByRoleId(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (ObjectUtil.isNull(role) || ObjectUtil.isNull(role.getClientId())) {
            return CollUtil.newArrayList();
        }
        return menuMapper.selectMenuListByRoleId(roleId, role.getClientId(), role.getMenuCheckStrictly());
    }

    /**
     * 构建前端路由所需要的菜单
     * 路由name命名规则 path首字母转大写 + id
     *
     * @param menus 菜单列表
     * @return 路由列表
     */
    @Override
    public List<RouterVo> buildMenus(List<SysMenu> menus) {
        if (CollUtil.isEmpty(menus)) {
            return CollUtil.newArrayList();
        }
        List<RouterVo> routers = new LinkedList<>();
        for (SysMenu menu : menus) {
            String name = menu.getRouteName() + menu.getMenuId();
            RouterVo router = new RouterVo();
            router.setHidden("1".equals(menu.getVisible()));
            router.setName(name);
            router.setPath(menu.getRouterPath());
            router.setComponent(menu.getComponentInfo());
            router.setQuery(menu.getQueryParam());
            router.setExt(menu.getExt());
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals(SystemConstants.NO, menu.getIsCache()), menu.getPath(), menu.getActiveMenu()));
            List<SysMenu> cMenus = menu.getChildren();
            if (CollUtil.isNotEmpty(cMenus) && SystemConstants.TYPE_DIR.equals(menu.getMenuType())) {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildMenus(cMenus));
            } else if (menu.isMenuFrame()) {
                String frameName = StringUtils.capitalize(menu.getPath()) + menu.getMenuId();
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo children = new RouterVo();
                children.setPath(menu.getPath());
                children.setComponent(menu.getComponent());
                children.setName(frameName);
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals(SystemConstants.NO, menu.getIsCache()), menu.getPath(), menu.getActiveMenu()));
                children.setQuery(menu.getQueryParam());
                children.setExt(menu.getExt());
                childrenList.add(children);
                router.setChildren(childrenList);
            } else if (menu.getParentId().equals(Constants.TOP_PARENT_ID) && menu.isInnerLink()) {
                router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
                router.setPath("/");
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo children = new RouterVo();
                String routerPath = SysMenu.innerLinkReplaceEach(menu.getPath());
                String innerLinkName = StringUtils.capitalize(routerPath) + menu.getMenuId();
                children.setPath(routerPath);
                children.setComponent(SystemConstants.INNER_LINK);
                children.setName(innerLinkName);
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath()));
                children.setExt(menu.getExt());
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    @Override
    public List<Tree<Long>> buildMenuTreeSelect(List<SysMenuVo> menus) {
        if (CollUtil.isEmpty(menus)) {
            return CollUtil.newArrayList();
        }
        return TreeBuildUtils.build(menus, (menu, tree) -> {
            Tree<Long> menuTree = tree.setId(menu.getMenuId())
                .setParentId(menu.getParentId())
                .setName(menu.getMenuName())
                .setWeight(menu.getOrderNum());
            menuTree.put("menuType", menu.getMenuType());
            menuTree.put("icon", menu.getIcon());
            menuTree.put("visible", menu.getVisible());
            menuTree.put("status", menu.getStatus());
        });
    }

    /**
     * 根据菜单ID查询信息
     *
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    @Override
    public SysMenuVo selectMenuById(Long menuId) {
        return menuMapper.selectVoById(menuId);
    }

    /**
     * 是否存在菜单子节点
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean hasChildByMenuId(Long menuId) {
        return menuMapper.lambda().eq(SysMenu::getParentId, menuId).exists();
    }

    /**
     * 是否存在菜单子节点
     *
     * @param menuIds 菜单ID列表
     * @return 结果
     */
    @Override
    public boolean hasChildByMenuId(Collection<Long> menuIds) {
        return menuMapper.lambda()
            .in(SysMenu::getParentId, menuIds)
            .notIn(SysMenu::getMenuId, menuIds)
            .exists();
    }

    /**
     * 查询菜单使用数量
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean checkMenuExistRole(Long menuId) {
        return roleMenuMapper.lambda().eq(SysRoleMenu::getMenuId, menuId).exists();
    }

    /**
     * 新增保存菜单信息
     *
     * @param bo 菜单信息
     * @return 结果
     */
    @Override
    public int insertMenu(SysMenuBo bo) {
        SysMenu menu = MapstructUtils.convert(bo, SysMenu.class);
        validateMenuClient(menu, true);
        return menuMapper.insert(menu);
    }

    /**
     * 修改保存菜单信息
     *
     * @param bo 菜单信息
     * @return 结果
     */
    @Override
    @DSTransactional
    public int updateMenu(SysMenuBo bo) {
        SysMenu menu = MapstructUtils.convert(bo, SysMenu.class);
        SysMenu dbMenu = menuMapper.selectById(menu.getMenuId());
        if (ObjectUtil.isNotNull(dbMenu)) {
            menu.setClientId(dbMenu.getClientId());
        }
        validateMenuClient(menu, false);
        Set<Long> affectedUserIds = hasAuthorizationChange(dbMenu, menu)
            ? findAffectedUserIdsByMenuIds(Set.of(menu.getMenuId())) : Set.of();
        int rows = menuMapper.updateById(menu);
        if (rows > 0) {
            clientSessionService.kickoutClient(menu.getClientId());
            invalidateUsers(affectedUserIds);
        }
        return rows;
    }

    /**
     * 删除菜单管理信息
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    @DSTransactional
    public int deleteMenuById(Long menuId) {
        SysMenu menu = menuMapper.selectById(menuId);
        Set<Long> affectedUserIds = findAffectedUserIdsByMenuIds(Set.of(menuId));
        int rows = menuMapper.deleteById(menuId);
        if (rows > 0 && ObjectUtil.isNotNull(menu)) {
            clientSessionService.kickoutClient(menu.getClientId());
            invalidateUsers(affectedUserIds);
        }
        return rows;
    }

    /**
     * 批量删除菜单管理信息
     *
     * @param menuIds 菜单ID串
     */
    @Override
    @DSTransactional
    public void deleteMenuById(Collection<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        Set<Long> affectedUserIds = findAffectedUserIdsByMenuIds(menuIds);
        List<SysMenu> menus = menuMapper.selectByIds(menuIds);
        Set<Long> clientIds = new HashSet<>();
        for (SysMenu menu : menus) {
            if (ObjectUtil.isNotNull(menu) && ObjectUtil.isNotNull(menu.getClientId())) {
                clientIds.add(menu.getClientId());
            }
        }
        menuMapper.deleteByIds(menuIds);
        roleMenuMapper.deleteByMenuIds(menuIds);
        for (Long clientId : clientIds) {
            clientSessionService.kickoutClient(clientId);
        }
        invalidateUsers(affectedUserIds);
    }

    private boolean hasAuthorizationChange(SysMenu current, SysMenu update) {
        if (ObjectUtil.isNull(current)) {
            return false;
        }
        return changed(current.getStatus(), update.getStatus())
            || changed(current.getPerms(), update.getPerms())
            || changed(current.getClientId(), update.getClientId());
    }

    private boolean changed(Object current, Object update) {
        return ObjectUtil.isNotNull(update) && !ObjectUtil.equal(current, update);
    }

    private Set<Long> findAffectedUserIdsByMenuIds(Collection<Long> menuIds) {
        Set<Long> ids = new LinkedHashSet<>();
        menuIds.stream().filter(Objects::nonNull).forEach(ids::add);
        if (ids.isEmpty()) {
            return Set.of();
        }
        Set<Long> roleIds = new LinkedHashSet<>();
        roleMenuMapper.lambda().in(SysRoleMenu::getMenuId, ids).list().stream()
            .map(SysRoleMenu::getRoleId)
            .filter(Objects::nonNull)
            .forEach(roleIds::add);
        return findAffectedUserIdsByRoleIds(roleIds);
    }

    private Set<Long> findAffectedUserIdsByRoleIds(Collection<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        userRoleMapper.lambda().in(SysUserRole::getRoleId, roleIds).list().stream()
            .map(SysUserRole::getUserId)
            .filter(Objects::nonNull)
            .forEach(userIds::add);
        Set<Long> userTypeIds = new LinkedHashSet<>();
        clientMapper.lambda()
            .in(SysClient::getDefaultRoleId, roleIds)
            .eq(SysClient::getStatus, SystemConstants.NORMAL)
            .list()
            .stream()
            .map(SysClient::getUserTypeId)
            .filter(Objects::nonNull)
            .forEach(userTypeIds::add);
        if (!userTypeIds.isEmpty()) {
            userTypeRelMapper.lambda()
                .in(SysUserTypeRel::getUserTypeId, userTypeIds)
                .eq(SysUserTypeRel::getStatus, SystemConstants.NORMAL)
                .list()
                .stream()
                .map(SysUserTypeRel::getUserId)
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        }
        return userIds;
    }

    private void invalidateUsers(Collection<Long> userIds) {
        userIds.stream().filter(Objects::nonNull).distinct()
            .forEach(openApiSessionInvalidator::invalidateByUserId);
    }

    /**
     * 校验菜单名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkMenuNameUnique(SysMenuBo menu) {
        boolean exist = menuMapper.lambda()
            .eq(SysMenu::getMenuName, menu.getMenuName())
            .eq(SysMenu::getParentId, menu.getParentId())
            .eq(SysMenu::getClientId, menu.getClientId())
            .neIfPresent(SysMenu::getMenuId, menu.getMenuId())
            .exists();
        return !exist;
    }

    /**
     * 校验路由组合是否唯一
     *
     * @param menuBo 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkRouteConfigUnique(SysMenuBo menuBo) {
        SysMenu menu = MapstructUtils.convert(menuBo, SysMenu.class);
        if (SystemConstants.TYPE_BUTTON.equals(menu.getMenuType())) {
            return true;
        }
        long menuId = ObjectUtil.isNull(menu.getMenuId()) ? -1L : menu.getMenuId();
        Long parentId = menu.getParentId();
        String path = menu.getPath();
        String routeName = StringUtils.isEmpty(menu.getRouteName()) ? path : menu.getRouteName();
        List<SysMenu> sysMenuList = menuMapper.lambda()
            .eq(SysMenu::getClientId, menu.getClientId())
            .in(SysMenu::getMenuType, SystemConstants.TYPE_DIR, SystemConstants.TYPE_MENU)
            .and(w ->
                w.eq(SysMenu::getPath, path).or().eq(SysMenu::getPath, routeName)
            ).list();
        for (SysMenu sysMenu : sysMenuList) {
            if (!sysMenu.getMenuId().equals(menuId)) {
                Long dbParentId = sysMenu.getParentId();
                String dbPath = sysMenu.getPath();
                String dbRouteName = StringUtils.isEmpty(sysMenu.getRouteName()) ? dbPath : sysMenu.getRouteName();
                if (StringUtils.equalsAnyIgnoreCase(path, dbPath) && parentId.equals(dbParentId)) {
                    log.warn("[同级路由冲突] 同级下已存在相同路由路径 '{}'，冲突菜单：{}", dbPath, sysMenu.getMenuName());
                    return false;
                } else if (StringUtils.equalsAnyIgnoreCase(path, dbPath)
                    && Constants.TOP_PARENT_ID.equals(parentId)
                    && Constants.TOP_PARENT_ID.equals(dbParentId)) {
                    log.warn("[根目录路由冲突] 根目录下路由 '{}' 必须唯一，已被菜单 '{}' 占用", path, sysMenu.getMenuName());
                    return false;
                } else if (StringUtils.equalsAnyIgnoreCase(routeName, dbRouteName)
                    && sysMenu.getMenuType().equals(menu.getMenuType())) {
                    log.warn("[路由名称冲突] 路由名称 '{}' 需在当前客户端内唯一，已被菜单 '{}' 使用", routeName, sysMenu.getMenuName());
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 解析客户端主键：优先使用入参，否则读取当前登录快照。缺上下文时拒绝查全局菜单。
     *
     * @param clientId 入参客户端主键
     * @return 客户端主键
     */
    private Long resolveClientId(Long clientId) {
        if (ObjectUtil.isNotNull(clientId)) {
            return clientId;
        }
        var loginUser = LoginHelper.getLoginUser();
        return loginUser == null ? null : loginUser.getClientPk();
    }

    /**
     * 校验菜单归属客户端，以及父子菜单必须同 Client。
     *
     * @param menu  菜单
     * @param isAdd 是否新增
     */
    private void validateMenuClient(SysMenu menu, boolean isAdd) {
        if (ObjectUtil.isNull(menu.getClientId())) {
            throw new ServiceException("客户端不能为空");
        }
        if (ObjectUtil.isNull(menu.getParentId()) || Constants.TOP_PARENT_ID.equals(menu.getParentId())) {
            return;
        }
        SysMenu parent = menuMapper.selectById(menu.getParentId());
        if (ObjectUtil.isNull(parent)) {
            throw new ServiceException("父菜单不存在");
        }
        if (!menu.getClientId().equals(parent.getClientId())) {
            throw new ServiceException(isAdd ? "父菜单必须属于同一客户端" : "不能将菜单移动到其他客户端");
        }
    }

    /**
     * 合并用户显式菜单与默认角色菜单，按菜单ID去重。
     *
     * @param owned   显式角色菜单
     * @param extra   默认角色菜单
     * @return 合并后的菜单
     */
    private List<SysMenu> mergeMenus(List<SysMenu> owned, List<SysMenu> extra) {
        LinkedHashMap<Long, SysMenu> merged = new LinkedHashMap<>();
        if (CollUtil.isNotEmpty(owned)) {
            owned.forEach(menu -> merged.put(menu.getMenuId(), menu));
        }
        if (CollUtil.isNotEmpty(extra)) {
            extra.forEach(menu -> merged.putIfAbsent(menu.getMenuId(), menu));
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * 合并用户显式菜单与默认角色菜单视图。
     *
     * @param owned 显式角色菜单
     * @param extra 默认角色菜单
     * @return 合并后的菜单
     */
    private List<SysMenuVo> mergeMenuVos(List<SysMenuVo> owned, List<SysMenuVo> extra) {
        LinkedHashMap<Long, SysMenuVo> merged = new LinkedHashMap<>();
        if (CollUtil.isNotEmpty(owned)) {
            owned.forEach(menu -> merged.put(menu.getMenuId(), menu));
        }
        if (CollUtil.isNotEmpty(extra)) {
            extra.forEach(menu -> merged.putIfAbsent(menu.getMenuId(), menu));
        }
        return new ArrayList<>(merged.values());
    }

}
