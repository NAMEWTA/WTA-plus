package org.namewta.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.MapstructUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.demo.domain.TestTree;
import org.namewta.demo.domain.bo.TestTreeBo;
import org.namewta.demo.domain.vo.TestTreeVo;
import org.namewta.demo.mapper.TestTreeMapper;
import org.namewta.demo.service.ITestTreeService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 测试树表Service业务层处理
 *
 * @date 2021-07-26
 */
// @DS("slave") // 切换从库查询
@RequiredArgsConstructor
@Service
public class TestTreeServiceImpl implements ITestTreeService {

    private final TestTreeMapper treeMapper;

    /**
     * 根据主键查询测试树表详情。
     *
     * @param id 主键
     * @return 测试树表视图对象
     */
    @Override
    public TestTreeVo queryById(Long id) {
        return treeMapper.selectVoById(id);
    }

    /**
     * 查询符合条件的测试树表列表。
     *
     * @param bo 查询条件
     * @return 结果列表
     */
    // @DS("slave") // 切换从库查询
    @Override
    public List<TestTreeVo> queryList(TestTreeBo bo) {
        LambdaQueryWrapper<TestTree> lqw = buildQueryWrapper(bo);
        return treeMapper.selectVoList(lqw);
    }

    /**
     * 构建测试树表动态查询条件。
     *
     * @param bo 查询条件
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<TestTree> buildQueryWrapper(TestTreeBo bo) {
        LambdaQueryWrapper<TestTree> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getDeptId() != null, TestTree::getDeptId, bo.getDeptId());
        lqw.eq(bo.getUserId() != null, TestTree::getUserId, bo.getUserId());
        lqw.like(StringUtils.isNotBlank(bo.getTreeName()), TestTree::getTreeName, bo.getTreeName());
        lqw.orderByAsc(TestTree::getId);
        return lqw;
    }

    /**
     * 新增测试树表数据。
     *
     * @param bo 新增业务对象
     * @return 是否新增成功
     */
    @Override
    @DSTransactional
    public Boolean insertByBo(TestTreeBo bo) {
        requireParent(bo.getParentId());
        if (bo.getId() != null && bo.getId() <= 0) throw new ServiceException("树节点ID必须大于0");
        if (bo.getId() != null && bo.getId().equals(bo.getParentId())) throw new ServiceException("父节点不能选择自身");
        lockStructure(List.of(), bo.getParentId());
        if (bo.getParentId() != 0) requireVisible(bo.getParentId());
        TestTree add = MapstructUtils.convert(bo, TestTree.class);
        requireChanged(treeMapper.insert(add), 1);
        bo.setId(add.getId());
        return true;
    }

    /**
     * 更新测试树表数据。
     *
     * @param bo 编辑业务对象
     * @return 是否更新成功
     */
    @Override
    @DSTransactional
    public Boolean updateByBo(TestTreeBo bo) {
        requireId(bo.getId()); requireParent(bo.getParentId());
        if (bo.getId().equals(bo.getParentId())) throw new ServiceException("父节点不能选择自身");
        List<TestTree> parentPath = lockStructure(List.of(bo.getId()), bo.getParentId());
        TestTree current = requireVisible(bo.getId());
        if (!current.getParentId().equals(bo.getParentId()) && bo.getParentId() != 0) requireVisible(bo.getParentId());
        if (parentPath.stream().anyMatch(node -> node.getId().equals(bo.getId()))) {
            throw new ServiceException("不能选择当前节点的后代作为父节点");
        }
        TestTree update = MapstructUtils.convert(bo, TestTree.class);
        requireChanged(treeMapper.updateById(update), 1);
        return true;
    }

    /**
     * 根据父边确定锁域，按根ID排序加锁后使用current read重验，避免并发移动或删除绕过检查。
     * 隐藏祖先仅用于内部结构校验；实际写入节点和变化的新父节点另行校验数据权限。
     */
    private List<TestTree> lockStructure(List<Long> currentIds, Long parentId) {
        if (TransactionContext.getXID() == null) throw new IllegalStateException("Tree mutations require a transaction");
        Map<Long, List<TestTree>> before = new LinkedHashMap<>();
        for (Long id : currentIds) before.put(id, path(id, false));
        if (parentId != 0) before.putIfAbsent(parentId, path(parentId, false));
        Set<Long> roots = new TreeSet<>();
        before.values().forEach(nodes -> roots.add(nodes.getFirst().getId()));
        if (parentId == 0) roots.addAll(currentIds);
        if (!roots.isEmpty() && treeMapper.lockTreeRoots(new ArrayList<>(roots)).size() != roots.size()) {
            throw new ServiceException("树结构已变化，请刷新后重试");
        }
        List<TestTree> parentPath = List.of();
        for (var entry : before.entrySet()) {
            List<TestTree> locked = path(entry.getKey(), true);
            if (!locked.getFirst().getId().equals(entry.getValue().getFirst().getId())) {
                throw new ServiceException("树结构已变化，请刷新后重试");
            }
            if (entry.getKey().equals(parentId)) parentPath = locked;
        }
        return parentPath;
    }

    /** 按真实父指针读取到根，坏链或已有环拒绝变更，不设置任意深度上限。 */
    private List<TestTree> path(Long id, boolean locking) {
        List<TestTree> nodes = new ArrayList<>(); Set<Long> seen = new HashSet<>();
        while (id != null && id > 0) {
            if (!seen.add(id)) throw new ServiceException("树层级存在循环，请先修复");
            TestTree node = treeMapper.selectStructure(id, locking);
            if (node == null) throw new ServiceException("树节点或父节点不存在");
            nodes.add(node); id = node.getParentId();
        }
        if (id == null || id != 0 || nodes.isEmpty()) throw new ServiceException("树层级数据异常，请先修复");
        Collections.reverse(nodes); return nodes;
    }

    /** 锁后查询使用Mapper既有数据权限，不能只依赖前端树选项。 */
    private TestTree requireVisible(Long id) {
        TestTree node = treeMapper.selectVisibleForUpdate(id);
        if (node == null) throw new ServiceException("树节点不存在或没有访问权限");
        return node;
    }

    /** 根必须显式传0；不存在的节点和空值不能自动归为根。 */
    private static void requireParent(Long parent) {
        if (parent == null || parent < 0) throw new ServiceException("父节点不能为空，根节点请使用0");
    }

    /** 变更节点主键必须有效。 */
    private static void requireId(Long id) {
        if (id == null || id <= 0) throw new ServiceException("树节点ID必须大于0");
    }

    /** 部分写入视为冲突，由外层动态事务整批回滚。 */
    private static void requireChanged(int actual, int expected) {
        if (actual != expected) throw new ServiceException("树节点已变化，请刷新后重试");
    }

    /**
     * 按主键集合删除测试树表数据，并按需执行删除前校验。
     *
     * @param ids     主键集合
     * @param isValid 保留的可选业务校验标志；树完整性与数据权限始终校验
     * @return 是否删除成功
     */
    @Override
    @DSTransactional
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) throw new ServiceException("删除节点不能为空");
        ids.forEach(TestTreeServiceImpl::requireId);
        List<Long> distinctIds = new ArrayList<>(new TreeSet<>(ids));
        lockStructure(distinctIds, 0L);
        distinctIds.forEach(this::requireVisible);
        // 树完整性是必选约束，legacy isValid参数不能授权删除仍被引用的父节点。
        if (!treeMapper.selectChildIdsForUpdate(distinctIds).isEmpty()) throw new ServiceException("存在子节点，不允许删除");
        requireChanged(treeMapper.deleteByIds(distinctIds), distinctIds.size());
        return true;
    }
}
