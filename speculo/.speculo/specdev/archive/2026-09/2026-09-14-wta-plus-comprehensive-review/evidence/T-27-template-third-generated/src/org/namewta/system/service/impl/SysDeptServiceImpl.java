package org.namewta.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.namewta.common.core.utils.MapstructUtils;
import org.namewta.common.core.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.namewta.system.domain.bo.SysDeptBo;
import org.namewta.system.domain.vo.SysDeptVo;
import org.namewta.system.domain.SysDept;
import org.namewta.system.mapper.SysDeptMapper;
import org.namewta.system.service.ISysDeptService;
import org.namewta.common.core.exception.ServiceException;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.tx.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * 模板集成验收Service业务层处理
 *
 * @date 2026-09-19
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysDeptServiceImpl implements ISysDeptService {

    private final SysDeptMapper sysDeptMapper;

    /**
     * 查询模板集成验收
     *
     * @param deptId 主键
     * @return 模板集成验收
     */
    @Override
    public SysDeptVo queryById(Long deptId) {
        return sysDeptMapper.selectVoById(deptId);
    }


    /**
     * 查询符合条件的模板集成验收列表
     *
     * @param bo 查询条件
     * @return 模板集成验收列表
     */
    @Override
    public List<SysDeptVo> queryList(SysDeptBo bo) {
        LambdaQueryWrapper<SysDept> lqw = buildQueryWrapper(bo);
        return sysDeptMapper.selectVoList(lqw);
    }


    private LambdaQueryWrapper<SysDept> buildQueryWrapper(SysDeptBo bo) {
        return QueryBuilder.lambda(SysDept.class)
            .orderByAsc(SysDept::getAncestors)
            .orderByAsc(SysDept::getParentId)
            .orderByAsc(SysDept::getDeptId)
            .build();
    }

    /**
     * 新增模板集成验收
     *
     * @param bo 模板集成验收
     * @return 是否新增成功
     */
    @Override
    @DSTransactional
    public Boolean insertByBo(SysDeptBo bo) {
        SysDept add = MapstructUtils.convert(bo, SysDept.class);
        requireParent(add.getParentId());
        if (add.getDeptId() != null) requireId(add.getDeptId());
        if (ObjectUtil.equal(add.getDeptId(), add.getParentId())) {
            throw new ServiceException("父节点不能选择自身");
        }
        List<SysDept> parentPath = lockStructure(List.of(), add.getParentId());
        if (!ObjectUtil.equal(add.getParentId(), 0L)) requireVisible(add.getParentId());
        add.setAncestors(ancestors(parentPath));
        requireChanged(sysDeptMapper.insert(add), 1);
        bo.setDeptId(add.getDeptId());
        return true;
    }

    /**
     * 修改模板集成验收；树变更在同一动态事务内校验结构与数据范围。
     *
     * @param bo 编辑参数
     * @return 是否修改成功
     */
    @Override
    @DSTransactional
    public Boolean updateByBo(SysDeptBo bo) {
        SysDept update = MapstructUtils.convert(bo, SysDept.class);
        requireId(update.getDeptId()); requireParent(update.getParentId());
        if (ObjectUtil.equal(update.getDeptId(), update.getParentId())) {
            throw new ServiceException("父节点不能选择自身");
        }
        List<SysDept> parentPath = lockStructure(List.of(update.getDeptId()), update.getParentId());
        SysDept current = requireVisible(update.getDeptId());
        if (!ObjectUtil.equal(current.getParentId(), update.getParentId())
            && !ObjectUtil.equal(update.getParentId(), 0L)) requireVisible(update.getParentId());
        if (parentPath.stream().anyMatch(node -> ObjectUtil.equal(node.getDeptId(), update.getDeptId()))) {
            throw new ServiceException("不能选择当前节点的后代作为父节点");
        }
        String newAncestors = ancestors(parentPath);
        update.setAncestors(newAncestors);
        if (!ObjectUtil.equal(current.getParentId(), update.getParentId())) updateChildrenAncestors(current, newAncestors);
        requireChanged(sysDeptMapper.updateById(update), 1);
        return true;
    }



    /** 按父边确定树根，统一锁域后current read重验，避免并发形成环或孤儿。 */
    private List<SysDept> lockStructure(List<Long> currentIds, Long parentId) {
        if (TransactionContext.getXID() == null) throw new IllegalStateException("Tree mutations require a transaction");
        Map<Long, List<SysDept>> before = new LinkedHashMap<>();
        for (Long id : currentIds) before.put(id, path(id, false));
        if (!ObjectUtil.equal(parentId, 0L)) before.putIfAbsent(parentId, path(parentId, false));
        Set<Long> roots = new TreeSet<>();
        before.values().forEach(nodes -> roots.add(nodes.getFirst().getDeptId()));
        if (ObjectUtil.equal(parentId, 0L)) roots.addAll(currentIds);
        if (!roots.isEmpty() && sysDeptMapper.lockTreeRoots(new ArrayList<>(roots)).size() != roots.size()) {
            throw new ServiceException("树结构已变化，请刷新后重试");
        }
        List<SysDept> parentPath = List.of();
        for (var entry : before.entrySet()) {
            List<SysDept> locked = path(entry.getKey(), true);
            if (!ObjectUtil.equal(locked.getFirst().getDeptId(), entry.getValue().getFirst().getDeptId())) {
                throw new ServiceException("树结构已变化，请刷新后重试");
            }
            if (ObjectUtil.equal(entry.getKey(), parentId)) parentPath = locked;
        }
        return parentPath;
    }

    /** 校验真实父指针，不依赖可被篡改或过期的ancestors判断是否成环。 */
    private List<SysDept> path(Long id, boolean locking) {
        List<SysDept> nodes = new ArrayList<>(); Set<Long> seen = new HashSet<>();
        while (id != null && !ObjectUtil.equal(id, 0L)) {
            if (!seen.add(id)) throw new ServiceException("树层级存在循环，请先修复");
            SysDept node = sysDeptMapper.selectStructure(id, locking);
            if (node == null) throw new ServiceException("树节点或父节点不存在");
            nodes.add(node); id = node.getParentId();
        }
        if (id == null || nodes.isEmpty()) throw new ServiceException("树层级数据异常，请先修复");
        Collections.reverse(nodes);
        String expected = String.valueOf(0L);
        for (SysDept node : nodes) {
            if (!expected.equals(node.getAncestors())) throw new ServiceException("树路径数据异常，请先修复");
            expected += "," + node.getDeptId();
        }
        return nodes;
    }

    /** 保持Mapper的数据权限；结构检查忽略可见性不代表允许写入隐藏节点。 */
    private SysDept requireVisible(Long id) {
        SysDept node = sysDeptMapper.selectVisibleForUpdate(id);
        if (node == null) throw new ServiceException("树节点不存在或没有访问权限");
        return node;
    }

    /** 根值必须显式提供，null不能暗中转换为根。 */
    private static void requireParent(Long id) {
        if (id == null) throw new ServiceException("父节点不能为空，请显式提供根值");
        if (id < 0) throw new ServiceException("父节点不能为负数");
    }

    /** 主键不能使用根哨兵。 */
    private static void requireId(Long id) {
        if (id == null || ObjectUtil.equal(id, 0L)) throw new ServiceException("树节点ID无效");
        if (id <= 0) throw new ServiceException("树节点ID必须大于0");
    }

    /** 部分写入触发整个事务回滚。 */
    private static void requireChanged(int actual, int expected) {
        if (actual != expected) throw new ServiceException("树节点已变化，请刷新后重试");
    }

    /** 由已锁定父链生成规范路径。 */
    private String ancestors(List<SysDept> parentPath) {
        return parentPath.isEmpty() ? String.valueOf(0L)
            : parentPath.getLast().getAncestors() + "," + parentPath.getLast().getDeptId();
    }

    /** 按实际子边遍历并更新整个子树；任何坏路径或失败均回滚。 */
    private void updateChildrenAncestors(SysDept current, String newAncestors) {
        Map<Long, String> oldPaths = new LinkedHashMap<>(), newPaths = new LinkedHashMap<>();
        oldPaths.put(current.getDeptId(), current.getAncestors());
        newPaths.put(current.getDeptId(), newAncestors);
        List<Long> parents = List.of(current.getDeptId());
        while (!parents.isEmpty()) {
            List<Long> next = new ArrayList<>();
            for (SysDept child : sysDeptMapper.selectTreeChildrenForUpdate(parents)) {
                String expected = oldPaths.get(child.getParentId()) + "," + child.getParentId();
                if (oldPaths.containsKey(child.getDeptId()) || !expected.equals(child.getAncestors())) {
                    throw new ServiceException("树路径数据异常，请先修复");
                }
                String changed = newPaths.get(child.getParentId()) + "," + child.getParentId();
                SysDept update = new SysDept(); update.setDeptId(child.getDeptId());
                update.setAncestors(changed); requireChanged(sysDeptMapper.updateById(update), 1);
                oldPaths.put(child.getDeptId(), expected); newPaths.put(child.getDeptId(), changed);
                next.add(child.getDeptId());
            }
            parents = next;
        }
    }

    /**
     * 批量删除模板集成验收；树节点必须全部可见且没有子节点。
     *
     * @param ids 待删除主键
     * @param isValid 保留的可选业务校验标志，不能跳过树完整性或数据权限
     * @return 是否删除成功
     */
    @Override
    @DSTransactional
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) throw new ServiceException("删除节点不能为空");
        ids.forEach(SysDeptServiceImpl::requireId);
        List<Long> distinctIds = new ArrayList<>(new TreeSet<>(ids));
        lockStructure(distinctIds, 0L);
        distinctIds.forEach(this::requireVisible);
        if (!sysDeptMapper.selectTreeChildrenForUpdate(distinctIds).isEmpty()) throw new ServiceException("存在子节点，不允许删除");
        requireChanged(sysDeptMapper.deleteByIds(distinctIds), distinctIds.size());
        return true;
    }
}
