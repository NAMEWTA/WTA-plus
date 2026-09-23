package org.namewta.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.*;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.mybatis.core.query.LambdaQueryBuilder;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.common.redis.utils.CacheUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.DeptService;
import org.namewta.system.api.domain.DeptDTO;
import org.namewta.system.domain.SysDept;
import org.namewta.system.domain.SysRole;
import org.namewta.system.domain.SysUser;
import org.namewta.system.domain.bo.SysDeptBo;
import org.namewta.system.domain.vo.SysDeptVo;
import org.namewta.system.mapper.SysDeptMapper;
import org.namewta.system.mapper.SysRoleMapper;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.service.ISysDeptService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.*;

/**
 * 部门管理 服务实现
 */
@RequiredArgsConstructor
@Service
public class SysDeptServiceImpl implements ISysDeptService, DeptService {

    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;

    /**
     * 分页查询部门管理数据
     *
     * @param dept      部门信息
     * @param pageQuery 分页对象
     * @return 部门信息集合
     */
    @Override
    public PageResult<SysDeptVo> selectPageDeptList(SysDeptBo dept, PageQuery pageQuery) {
        Page<SysDeptVo> page = deptMapper.selectPageDeptList(pageQuery.build(), buildQueryWrapper(dept));
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 查询部门管理数据
     *
     * @param dept 部门信息
     * @return 部门信息集合
     */
    @Override
    public List<SysDeptVo> selectDeptList(SysDeptBo dept) {
        LambdaQueryWrapper<SysDept> lqw = buildQueryWrapper(dept);
        return deptMapper.selectDeptList(lqw);
    }

    /**
     * 查询部门树结构信息
     *
     * @param bo 部门信息
     * @return 部门树信息集合
     */
    @Override
    public List<Tree<Long>> selectDeptTreeList(SysDeptBo bo) {
        LambdaQueryWrapper<SysDept> lqw = buildQueryWrapper(bo);
        List<SysDeptVo> depts = deptMapper.selectDeptList(lqw);
        return buildDeptTreeSelect(depts);
    }

    /**
     * 构造部门列表查询条件。
     *
     * @param bo 部门筛选条件
     * @return 包含树级过滤、状态、分类和时间区间的查询包装器
     */
    private LambdaQueryWrapper<SysDept> buildQueryWrapper(SysDeptBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryBuilder<SysDept> builder = QueryBuilder.lambda(SysDept.class)
            .eqIfPresent(SysDept::getDeptId, bo.getDeptId())
            .eqIfPresent(SysDept::getParentId, bo.getParentId())
            .likeIfText(SysDept::getDeptName, bo.getDeptName())
            .likeIfText(SysDept::getDeptCategory, bo.getDeptCategory())
            .eqIfText(SysDept::getStatus, bo.getStatus())
            .betweenParams(SysDept::getCreateTime, params, "beginTime", "endTime")
            .orderByAsc(SysDept::getAncestors, SysDept::getParentId, SysDept::getOrderNum, SysDept::getDeptId);
        if (ObjectUtil.isNotNull(bo.getBelongDeptId())) {
            //部门树搜索
            builder.and(x -> {
                List<Long> deptIds = deptMapper.selectDeptAndChildById(bo.getBelongDeptId());
                x.in(SysDept::getDeptId, deptIds);
            });
        }
        return builder.build();
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param depts 部门列表
     * @return 下拉树结构列表
     */
    @Override
    public List<Tree<Long>> buildDeptTreeSelect(List<SysDeptVo> depts) {
        if (CollUtil.isEmpty(depts)) {
            return CollUtil.newArrayList();
        }
        return TreeBuildUtils.buildMultiRoot(
            depts,
            SysDeptVo::getDeptId,
            SysDeptVo::getParentId,
            (node, treeNode) -> treeNode
                .setId(node.getDeptId())
                .setParentId(node.getParentId())
                .setName(node.getDeptName())
                .setWeight(node.getOrderNum())
                .putExtra("disabled", SystemConstants.DISABLE.equals(node.getStatus()))
        );
    }

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    @Override
    public List<Long> selectDeptListByRoleId(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        return deptMapper.selectDeptListByRoleId(roleId, role.getDeptCheckStrictly());
    }

    /**
     * 根据部门ID查询信息
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    @Cacheable(cacheNames = CacheNames.SYS_DEPT, key = "#deptId")
    @Override
    public SysDeptVo selectDeptById(Long deptId) {
        SysDeptVo dept = deptMapper.selectVoById(deptId);
        if (ObjectUtil.isNull(dept)) {
            return null;
        }
        SysDeptVo parentDept = deptMapper.lambda()
            .select(SysDept::getDeptName)
            .eq(SysDept::getDeptId, dept.getParentId())
            .voOne();
        dept.setParentName(ObjectUtils.notNullGetter(parentDept, SysDeptVo::getDeptName));
        return dept;
    }

    /**
     * 按部门主键集合查询部门基础信息。
     *
     * @param deptIds 部门主键集合
     * @return 部门基础信息列表
     */
    @Override
    public List<SysDeptVo> selectDeptByIds(Collection<Long> deptIds) {
        return deptMapper.selectDeptList(deptMapper.lambda()
            .select(SysDept::getDeptId, SysDept::getDeptName, SysDept::getLeader)
            .eq(SysDept::getStatus, SystemConstants.NORMAL)
            .in(CollUtil.isNotEmpty(deptIds), SysDept::getDeptId, deptIds)
            .build());
    }

    /**
     * 通过部门ID查询部门名称
     *
     * @param deptIds 部门ID串逗号分隔
     * @return 部门名称串逗号分隔
     */
    @Override
    public String selectDeptNameByIds(String deptIds) {
        List<String> list = new ArrayList<>();
        for (Long id : StringUtils.splitTo(deptIds, Convert::toLong)) {
            SysDeptVo vo = SpringUtils.getAopProxy(this).selectDeptById(id);
            if (ObjectUtil.isNotNull(vo)) {
                list.add(vo.getDeptName());
            }
        }
        return StringUtils.joinComma(list);
    }

    /**
     * 根据部门ID查询部门负责人
     *
     * @param deptId 部门ID，用于指定需要查询的部门
     * @return 返回该部门的负责人ID
     */
    @Override
    public Long selectDeptLeaderById(Long deptId) {
        SysDeptVo vo = SpringUtils.getAopProxy(this).selectDeptById(deptId);
        return ObjectUtil.isNull(vo) ? null : vo.getLeader();
    }

    /**
     * 查询部门
     *
     * @return 部门列表
     */
    @Override
    public List<DeptDTO> selectDeptsByList() {
        List<SysDeptVo> list = deptMapper.selectDeptList(deptMapper.lambda()
            .select(SysDept::getDeptId, SysDept::getDeptName, SysDept::getParentId)
            .eq(SysDept::getStatus, SystemConstants.NORMAL)
            .build());
        return BeanUtil.copyToList(list, DeptDTO.class);
    }

    /**
     * 根据ID查询所有子部门数（正常状态）
     *
     * @param deptId 部门ID
     * @return 子部门数
     */
    @Override
    public long selectNormalChildrenDeptById(Long deptId) {
        return deptMapper.lambda()
            .eq(SysDept::getStatus, SystemConstants.NORMAL)
            .findInSet(deptId, SysDept::getAncestors)
            .count();
    }

    /**
     * 是否存在子节点
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public boolean hasChildByDeptId(Long deptId) {
        return deptMapper.lambda().eq(SysDept::getParentId, deptId).exists();
    }

    /**
     * 查询部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkDeptExistUser(Long deptId) {
        return userMapper.lambda().eq(SysUser::getDeptId, deptId).exists();
    }

    /**
     * 校验部门名称是否唯一
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public boolean checkDeptNameUnique(SysDeptBo dept) {
        boolean exist = deptMapper.lambda()
            .eq(SysDept::getDeptName, dept.getDeptName())
            .eq(SysDept::getParentId, dept.getParentId())
            .neIfPresent(SysDept::getDeptId, dept.getDeptId())
            .exists();
        return !exist;
    }

    /**
     * 校验部门是否有数据权限
     *
     * @param deptId 部门id
     */
    @Override
    public void checkDeptDataScope(Long deptId) {
        if (ObjectUtil.isNull(deptId)) {
            return;
        }
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        if (deptMapper.countDeptById(deptId) == 0) {
            throw new ServiceException("没有权限访问部门数据！");
        }
    }

    /**
     * 新增保存部门信息
     *
     * @param bo 部门信息
     * @return 结果
     */
    @Override
    @DSTransactional
    public int insertDept(SysDeptBo bo) {
        requireParentId(bo.getParentId());
        if (bo.getDeptId() != null && bo.getDeptId() <= 0) throw new ServiceException("部门ID必须大于0");
        List<SysDept> parentPath = lockStructure(null, bo.getParentId()).parentPath();
        checkParentScope(bo.getParentId(), true);
        if (!parentPath.isEmpty() && !SystemConstants.NORMAL.equals(parentPath.getLast().getStatus())) {
            throw new ServiceException("部门停用，不允许新增");
        }
        SysDept dept = MapstructUtils.convert(bo, SysDept.class);
        dept.setAncestors(ancestors(parentPath));
        int result = deptMapper.insert(dept);
        requireChanged(result);
        evictAfterCommit(Set.of(dept.getDeptId()));
        return result;
    }

    /**
     * 修改保存部门信息
     *
     * @param bo 部门信息
     * @return 结果
     */
    @Override
    @DSTransactional
    public int updateDept(SysDeptBo bo) {
        requireParentId(bo.getParentId());
        if (bo.getDeptId() == null || bo.getDeptId() <= 0) throw new ServiceException("部门不存在，无法修改");
        if (bo.getDeptId().equals(bo.getParentId())) throw new ServiceException("上级部门不能是自己");
        LockedStructure structure = lockStructure(bo.getDeptId(), bo.getParentId());
        SysDept oldDept = structure.current();
        checkDataScopeLocked(bo.getDeptId());
        checkParentScope(bo.getParentId(), !oldDept.getParentId().equals(bo.getParentId()));
        if (structure.parentPath().stream().anyMatch(node -> node.getDeptId().equals(bo.getDeptId()))) {
            throw new ServiceException("上级部门不能是当前部门的子部门");
        }
        List<SysDept> children = descendants(oldDept);
        if (SystemConstants.DISABLE.equals(bo.getStatus())) {
            if (children.stream().anyMatch(node -> SystemConstants.NORMAL.equals(node.getStatus()))) {
                throw new ServiceException("该部门包含未停用的子部门!");
            }
            if (checkDeptExistUser(bo.getDeptId())) throw new ServiceException("该部门下存在已分配用户，不能禁用!");
        }
        SysDept dept = MapstructUtils.convert(bo, SysDept.class);
        dept.setAncestors(ancestors(structure.parentPath()));
        Set<Long> changedIds = new HashSet<>(); changedIds.add(dept.getDeptId());
        if (!oldDept.getParentId().equals(dept.getParentId())) updateDeptChildren(dept, children, changedIds);
        int result = deptMapper.updateById(dept);
        requireChanged(result);
        // 如果部门状态为启用，且部门祖级列表不为空，且部门祖级列表不等于根部门祖级列表（如果部门祖级列表不等于根部门祖级列表，则说明存在上级部门）
        if (SystemConstants.NORMAL.equals(dept.getStatus())
            && StringUtils.isNotEmpty(dept.getAncestors())
            && !StringUtils.equals(SystemConstants.ROOT_DEPT_ANCESTORS, dept.getAncestors())) {
            // 如果该部门是启用状态，则启用该部门的所有上级部门
            updateParentDeptStatusNormal(dept);
            structure.parentPath().forEach(node -> changedIds.add(node.getDeptId()));
        }
        evictAfterCommit(changedIds);
        return result;
    }

    /**
     * 修改该部门的父级部门状态
     *
     * @param dept 当前部门
     */
    private void updateParentDeptStatusNormal(SysDept dept) {
        String ancestors = dept.getAncestors();
        Long[] deptIds = Convert.toLongArray(ancestors);
        deptMapper.lambda()
            .set(SysDept::getStatus, SystemConstants.NORMAL)
            .in(SysDept::getDeptId, Arrays.asList(deptIds))
            .update();
    }

    /**
     * 修改子元素关系
     *
     * @param moved 被移动部门的新路径
     * @param children 已按父先子后的顺序读取且验证的子树
     * @param changedIds 提交后失效的缓存ID
     */
    private void updateDeptChildren(SysDept moved, List<SysDept> children, Set<Long> changedIds) {
        Map<Long, String> paths = new HashMap<>(); paths.put(moved.getDeptId(), moved.getAncestors());
        for (SysDept child : children) {
            SysDept dept = new SysDept();
            dept.setDeptId(child.getDeptId());
            dept.setAncestors(paths.get(child.getParentId()) + "," + child.getParentId());
            requireChanged(deptMapper.updateById(dept));
            paths.put(dept.getDeptId(), dept.getAncestors()); changedIds.add(dept.getDeptId());
        }
    }

    /**
     * 删除部门管理信息
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    @DSTransactional
    public int deleteDeptById(Long deptId) {
        if (deptId == null || deptId <= 0) throw new ServiceException("部门不存在，无法删除");
        lockStructure(deptId, 0L);
        checkDataScopeLocked(deptId);
        if (SystemConstants.DEFAULT_DEPT_ID.equals(deptId)) throw new ServiceException("默认部门,不允许删除");
        if (!deptMapper.selectChildrenForUpdate(List.of(deptId)).isEmpty()) throw new ServiceException("存在下级部门,不允许删除");
        if (checkDeptExistUser(deptId)) throw new ServiceException("部门存在用户,不允许删除");
        int result = deptMapper.deleteById(deptId);
        requireChanged(result); evictAfterCommit(Set.of(deptId)); return result;
    }

    /** Determine lock domains from parent edges, then re-read under the ordered tree-root locks. */
    private LockedStructure lockStructure(Long currentId, Long parentId) {
        if (TransactionContext.getXID() == null) throw new IllegalStateException("Department mutations require a transaction");
        List<SysDept> currentPath = currentId == null ? List.of() : path(currentId, false);
        List<SysDept> parentPath = parentId == 0 ? List.of() : path(parentId, false);
        Set<Long> roots = new TreeSet<>();
        if (!currentPath.isEmpty()) roots.add(currentPath.getFirst().getDeptId());
        if (!parentPath.isEmpty()) roots.add(parentPath.getFirst().getDeptId());
        if (currentId != null && parentId == 0) roots.add(currentId);
        if (!roots.isEmpty() && deptMapper.lockTreeRoots(new ArrayList<>(roots)).size() != roots.size()) {
            throw new ServiceException("部门结构已变化，请刷新后重试");
        }
        List<SysDept> lockedCurrent = currentId == null ? List.of() : path(currentId, true);
        List<SysDept> lockedParent = parentId == 0 ? List.of() : path(parentId, true);
        if (!sameRoot(currentPath, lockedCurrent) || !sameRoot(parentPath, lockedParent)) {
            throw new ServiceException("部门结构已变化，请刷新后重试");
        }
        return new LockedStructure(lockedCurrent.isEmpty() ? null : lockedCurrent.getLast(), lockedParent);
    }

    private List<SysDept> path(Long id, boolean locking) {
        List<SysDept> nodes = new ArrayList<>(); Set<Long> seen = new HashSet<>();
        while (id != null && id > 0) {
            if (!seen.add(id)) throw new ServiceException("部门层级存在循环，请先修复");
            SysDept node = deptMapper.selectStructure(id, locking);
            if (node == null) throw new ServiceException("部门或父部门不存在");
            nodes.add(node); id = node.getParentId();
        }
        if (id == null || id != 0 || nodes.isEmpty()) throw new ServiceException("部门层级数据异常，请先修复");
        Collections.reverse(nodes);
        String expected = SystemConstants.ROOT_DEPT_ANCESTORS;
        for (SysDept node : nodes) {
            if (!expected.equals(node.getAncestors())) throw new ServiceException("部门层级数据异常，请先修复");
            expected += "," + node.getDeptId();
        }
        return nodes;
    }

    private List<SysDept> descendants(SysDept root) {
        List<SysDept> result = new ArrayList<>();
        Map<Long, String> paths = new HashMap<>(); paths.put(root.getDeptId(), root.getAncestors());
        List<Long> parents = List.of(root.getDeptId());
        while (!parents.isEmpty()) {
            List<SysDept> children = deptMapper.selectChildrenForUpdate(parents);
            List<Long> next = new ArrayList<>();
            for (SysDept child : children) {
                String expected = paths.get(child.getParentId()) + "," + child.getParentId();
                if (paths.containsKey(child.getDeptId()) || !expected.equals(child.getAncestors())) {
                    throw new ServiceException("部门层级数据异常，请先修复");
                }
                paths.put(child.getDeptId(), expected); next.add(child.getDeptId()); result.add(child);
            }
            parents = next;
        }
        return result;
    }

    private void checkDataScopeLocked(Long deptId) {
        if (!LoginHelper.isSuperAdmin() && deptMapper.selectVisibleDeptForUpdate(deptId) == null) {
            throw new ServiceException("没有权限访问部门数据！");
        }
    }

    private void checkParentScope(Long parentId, boolean changed) {
        if (!changed) return;
        if (parentId == 0) {
            if (changed && !LoginHelper.isSuperAdmin()) throw new ServiceException("只有超级管理员可以设置根部门");
        } else {
            checkDataScopeLocked(parentId);
        }
    }

    private static String ancestors(List<SysDept> parentPath) {
        return parentPath.isEmpty() ? SystemConstants.ROOT_DEPT_ANCESTORS
            : parentPath.getLast().getAncestors() + "," + parentPath.getLast().getDeptId();
    }

    private static boolean sameRoot(List<SysDept> before, List<SysDept> after) {
        return before.isEmpty() ? after.isEmpty() : !after.isEmpty() && before.getFirst().getDeptId().equals(after.getFirst().getDeptId());
    }

    private static void requireParentId(Long parentId) {
        if (parentId == null || parentId < 0) throw new ServiceException("上级部门不能为空，根部门请使用0");
    }

    private static void requireChanged(int changed) {
        if (changed != 1) throw new ServiceException("部门数据已变化，请刷新后重试");
    }

    private void evictAfterCommit(Set<Long> changedIds) {
        Set<Long> ids = Set.copyOf(changedIds);
        TransactionContext.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CacheUtils.clear(CacheNames.SYS_DEPT_AND_CHILD);
                ids.forEach(id -> CacheUtils.evict(CacheNames.SYS_DEPT, id));
            }
        });
    }

    private record LockedStructure(SysDept current, List<SysDept> parentPath) { }


    /**
     * 根据部门 ID 列表查询部门名称映射关系
     *
     * @param deptIds 部门 ID 列表
     * @return Map，其中 key 为部门 ID，value 为对应的部门名称
     */
    @Override
    public Map<Long, String> selectDeptNamesByIds(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyMap();
        }
        List<SysDept> list = deptMapper.lambda()
            .select(SysDept::getDeptId, SysDept::getDeptName)
            .in(SysDept::getDeptId, deptIds)
            .list();
        return StreamUtils.toMap(list, SysDept::getDeptId, SysDept::getDeptName);
    }

}
