package org.namewta.demo.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.namewta.common.core.utils.MapstructUtils;
import org.namewta.common.core.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.namewta.demo.domain.bo.TestTreeBo;
import org.namewta.demo.domain.vo.TestTreeVo;
import org.namewta.demo.domain.TestTree;
import org.namewta.demo.mapper.TestTreeMapper;
import org.namewta.demo.service.ITestTreeService;
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
public class TestTreeServiceImpl implements ITestTreeService {

    private final TestTreeMapper testTreeMapper;

    /**
     * 查询模板集成验收
     *
     * @param id 主键
     * @return 模板集成验收
     */
    @Override
    public TestTreeVo queryById(Long id) {
        return testTreeMapper.selectVoById(id);
    }


    /**
     * 查询符合条件的模板集成验收列表
     *
     * @param bo 查询条件
     * @return 模板集成验收列表
     */
    @Override
    public List<TestTreeVo> queryList(TestTreeBo bo) {
        LambdaQueryWrapper<TestTree> lqw = buildQueryWrapper(bo);
        return testTreeMapper.selectVoList(lqw);
    }


    private LambdaQueryWrapper<TestTree> buildQueryWrapper(TestTreeBo bo) {
        return QueryBuilder.lambda(TestTree.class)
            .orderByAsc(TestTree::getParentId)
            .orderByAsc(TestTree::getId)
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
    public Boolean insertByBo(TestTreeBo bo) {
        TestTree add = MapstructUtils.convert(bo, TestTree.class);
        requireParent(add.getParentId());
        if (add.getId() != null) requireId(add.getId());
        if (ObjectUtil.equal(add.getId(), add.getParentId())) {
            throw new ServiceException("父节点不能选择自身");
        }
        List<TestTree> parentPath = lockStructure(List.of(), add.getParentId());
        if (!ObjectUtil.equal(add.getParentId(), 0L)) requireVisible(add.getParentId());
        requireChanged(testTreeMapper.insert(add), 1);
        bo.setId(add.getId());
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
    public Boolean updateByBo(TestTreeBo bo) {
        TestTree update = MapstructUtils.convert(bo, TestTree.class);
        requireId(update.getId()); requireParent(update.getParentId());
        if (ObjectUtil.equal(update.getId(), update.getParentId())) {
            throw new ServiceException("父节点不能选择自身");
        }
        List<TestTree> parentPath = lockStructure(List.of(update.getId()), update.getParentId());
        TestTree current = requireVisible(update.getId());
        if (!ObjectUtil.equal(current.getParentId(), update.getParentId())
            && !ObjectUtil.equal(update.getParentId(), 0L)) requireVisible(update.getParentId());
        if (parentPath.stream().anyMatch(node -> ObjectUtil.equal(node.getId(), update.getId()))) {
            throw new ServiceException("不能选择当前节点的后代作为父节点");
        }
        requireChanged(testTreeMapper.updateById(update), 1);
        return true;
    }



    /** 按父边确定树根，统一锁域后current read重验，避免并发形成环或孤儿。 */
    private List<TestTree> lockStructure(List<Long> currentIds, Long parentId) {
        if (TransactionContext.getXID() == null) throw new IllegalStateException("Tree mutations require a transaction");
        Map<Long, List<TestTree>> before = new LinkedHashMap<>();
        for (Long id : currentIds) before.put(id, path(id, false));
        if (!ObjectUtil.equal(parentId, 0L)) before.putIfAbsent(parentId, path(parentId, false));
        Set<Long> roots = new TreeSet<>();
        before.values().forEach(nodes -> roots.add(nodes.getFirst().getId()));
        if (ObjectUtil.equal(parentId, 0L)) roots.addAll(currentIds);
        if (!roots.isEmpty() && testTreeMapper.lockTreeRoots(new ArrayList<>(roots)).size() != roots.size()) {
            throw new ServiceException("树结构已变化，请刷新后重试");
        }
        List<TestTree> parentPath = List.of();
        for (var entry : before.entrySet()) {
            List<TestTree> locked = path(entry.getKey(), true);
            if (!ObjectUtil.equal(locked.getFirst().getId(), entry.getValue().getFirst().getId())) {
                throw new ServiceException("树结构已变化，请刷新后重试");
            }
            if (ObjectUtil.equal(entry.getKey(), parentId)) parentPath = locked;
        }
        return parentPath;
    }

    /** 校验真实父指针，不依赖可被篡改或过期的ancestors判断是否成环。 */
    private List<TestTree> path(Long id, boolean locking) {
        List<TestTree> nodes = new ArrayList<>(); Set<Long> seen = new HashSet<>();
        while (id != null && !ObjectUtil.equal(id, 0L)) {
            if (!seen.add(id)) throw new ServiceException("树层级存在循环，请先修复");
            TestTree node = testTreeMapper.selectStructure(id, locking);
            if (node == null) throw new ServiceException("树节点或父节点不存在");
            nodes.add(node); id = node.getParentId();
        }
        if (id == null || nodes.isEmpty()) throw new ServiceException("树层级数据异常，请先修复");
        Collections.reverse(nodes);
        return nodes;
    }

    /** 保持Mapper的数据权限；结构检查忽略可见性不代表允许写入隐藏节点。 */
    private TestTree requireVisible(Long id) {
        TestTree node = testTreeMapper.selectVisibleForUpdate(id);
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
        ids.forEach(TestTreeServiceImpl::requireId);
        List<Long> distinctIds = new ArrayList<>(new TreeSet<>(ids));
        lockStructure(distinctIds, 0L);
        distinctIds.forEach(this::requireVisible);
        if (!testTreeMapper.selectTreeChildrenForUpdate(distinctIds).isEmpty()) throw new ServiceException("存在子节点，不允许删除");
        requireChanged(testTreeMapper.deleteByIds(distinctIds), distinctIds.size());
        return true;
    }
}
