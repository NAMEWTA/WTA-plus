package ${packageName}.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.namewta.common.core.utils.MapstructUtils;
import org.namewta.common.core.utils.StringUtils;
<#if table.crud>
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
</#if>
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
<#if enableUnique>
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
</#if>
import org.namewta.common.mybatis.core.query.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ${packageName}.domain.bo.${ClassName}Bo;
import ${packageName}.domain.vo.${ClassName}Vo;
import ${packageName}.domain.${ClassName};
import ${packageName}.mapper.${ClassName}Mapper;
import ${packageName}.service.I${ClassName}Service;
<#if table.tree>
import org.namewta.common.core.exception.ServiceException;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
</#if>

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
 * ${functionName}Service业务层处理
 *
 * @author ${author}
 * @date ${datetime}
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ${ClassName}ServiceImpl implements I${ClassName}Service {

    private final ${ClassName}Mapper ${className}Mapper;

    /**
     * 查询${functionName}
     *
     * @param ${pkColumn.javaField} 主键
     * @return ${functionName}
     */
    @Override
    public ${ClassName}Vo queryById(${pkColumn.javaType} ${pkColumn.javaField}) {
        return ${className}Mapper.selectVoById(${pkColumn.javaField});
    }

<#if table.crud>
    /**
     * 分页查询${functionName}列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return ${functionName}分页列表
     */
    @Override
    public PageResult<${ClassName}Vo> queryPageList(${ClassName}Bo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<${ClassName}> lqw = buildQueryWrapper(bo);
        Page<${ClassName}Vo> result = ${className}Mapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }
</#if>

    /**
     * 查询符合条件的${functionName}列表
     *
     * @param bo 查询条件
     * @return ${functionName}列表
     */
    @Override
    public List<${ClassName}Vo> queryList(${ClassName}Bo bo) {
        LambdaQueryWrapper<${ClassName}> lqw = buildQueryWrapper(bo);
        return ${className}Mapper.selectVoList(lqw);
    }

<#if enableUnique>
    /**
     * 校验${functionName}是否满足组合唯一约束
     *
     * @param bo ${functionName}
     * @return 是否唯一
     */
    @Override
    public boolean checkUnique(${ClassName}Bo bo) {
        boolean hasUniqueValue = true;
<#list uniqueColumns as column>
<#if column.javaType == 'String'>
        hasUniqueValue = hasUniqueValue && StringUtils.isNotBlank(bo.get${column.capJavaField}());
<#else>
        hasUniqueValue = hasUniqueValue && bo.get${column.capJavaField}() != null;
</#if>
</#list>
        if (!hasUniqueValue) {
            return true;
        }
        LambdaQueryWrapper<${ClassName}> lqw = Wrappers.lambdaQuery();
<#list uniqueColumns as column>
        lqw.eq(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}());
</#list>
        lqw.ne(bo.get${pkColumn.capJavaField}() != null, ${ClassName}::get${pkColumn.capJavaField}, bo.get${pkColumn.capJavaField}());
        return !${className}Mapper.exists(lqw);
    }
</#if>

    private LambdaQueryWrapper<${ClassName}> buildQueryWrapper(${ClassName}Bo bo) {
<#if hasBetween>
        Map<String, Object> params = bo.getParams();
</#if>
        return QueryBuilder.lambda(${ClassName}.class)
<#list columns as column>
<#if column.query>
<#assign queryType = column.queryType>
<#assign javaType = column.javaType>
<#assign AttrName = column.capJavaField>
<#assign mpMethod = column.queryType?lower_case>
<#if queryType != 'BETWEEN'>
<#if javaType == 'String'>
<#assign condition = 'StringUtils.isNotBlank(bo.get'+AttrName+'())'>
<#if queryType == 'LIKE'>
            .likeIfText(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#elseif queryType == 'EQ'>
            .eqIfText(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#elseif queryType == 'NE'>
            .neIfText(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#else>
            .${mpMethod}(${condition}, ${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
</#if>
<#else>
<#assign condition = 'bo.get'+AttrName+'() != null'>
<#if queryType == 'EQ'>
            .eqIfPresent(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#elseif queryType == 'NE'>
            .neIfPresent(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#elseif queryType == 'GT'>
            .gtIfPresent(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#elseif queryType == 'LT'>
            .ltIfPresent(${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
<#else>
            .${mpMethod}(${condition}, ${ClassName}::get${column.capJavaField}, bo.get${column.capJavaField}())
</#if>
</#if>
<#else>
            .betweenParams(${ClassName}::get${column.capJavaField}, params, "begin${column.capJavaField}", "end${column.capJavaField}")
</#if>
</#if>
</#list>
<#if table.tree && "" != treeAncestorsField>
            .orderByAsc(${ClassName}::get${treeAncestorsCap})
</#if>
<#if table.tree && "" != treeParentCode>
            .orderByAsc(${ClassName}::get${treeParentCap})
</#if>
<#if table.tree && "" != treeOrderField>
            .orderByAsc(${ClassName}::get${treeOrderCap})
<#elseif enableSort>
            .orderByAsc(${ClassName}::get${sortColumn.capJavaField})
</#if>
            .orderByAsc(${ClassName}::get${pkColumn.capJavaField})
            .build();
    }

    /**
     * 新增${functionName}
     *
     * @param bo ${functionName}
     * @return 是否新增成功
     */
    @Override
<#if table.tree>
    @DSTransactional
</#if>
    public Boolean insertByBo(${ClassName}Bo bo) {
        ${ClassName} add = MapstructUtils.convert(bo, ${ClassName}.class);
<#if table.tree>
        requireParent(add.get${treeParentCap}());
        if (add.get${pkColumn.capJavaField}() != null) requireId(add.get${pkColumn.capJavaField}());
        if (ObjectUtil.equal(add.get${pkColumn.capJavaField}(), add.get${treeParentCap}())) {
            throw new ServiceException("父节点不能选择自身");
        }
        List<${ClassName}> parentPath = lockStructure(List.of(), add.get${treeParentCap}());
        if (!ObjectUtil.equal(add.get${treeParentCap}(), ${treeRootValueJavaLiteral})) requireVisible(add.get${treeParentCap}());
<#if "" != treeAncestorsField>
        add.set${treeAncestorsCap}(ancestors(parentPath));
</#if>
        requireChanged(${className}Mapper.insert(add), 1);
        bo.set${pkColumn.capJavaField}(add.get${pkColumn.capJavaField}());
        return true;
<#else>
        boolean flag = ${className}Mapper.insert(add) > 0;
        if (flag) bo.set${pkColumn.capJavaField}(add.get${pkColumn.capJavaField}());
        return flag;
</#if>
    }

    /**
     * 修改${functionName}；树变更在同一动态事务内校验结构与数据范围。
     *
     * @param bo 编辑参数
     * @return 是否修改成功
     */
    @Override
<#if table.tree>
    @DSTransactional
</#if>
    public Boolean updateByBo(${ClassName}Bo bo) {
        ${ClassName} update = MapstructUtils.convert(bo, ${ClassName}.class);
<#if table.tree>
        requireId(update.get${pkColumn.capJavaField}()); requireParent(update.get${treeParentCap}());
        if (ObjectUtil.equal(update.get${pkColumn.capJavaField}(), update.get${treeParentCap}())) {
            throw new ServiceException("父节点不能选择自身");
        }
        List<${ClassName}> parentPath = lockStructure(List.of(update.get${pkColumn.capJavaField}()), update.get${treeParentCap}());
        ${ClassName} current = requireVisible(update.get${pkColumn.capJavaField}());
        if (!ObjectUtil.equal(current.get${treeParentCap}(), update.get${treeParentCap}())
            && !ObjectUtil.equal(update.get${treeParentCap}(), ${treeRootValueJavaLiteral})) requireVisible(update.get${treeParentCap}());
        if (parentPath.stream().anyMatch(node -> ObjectUtil.equal(node.get${pkColumn.capJavaField}(), update.get${pkColumn.capJavaField}()))) {
            throw new ServiceException("不能选择当前节点的后代作为父节点");
        }
<#if "" != treeAncestorsField>
        String newAncestors = ancestors(parentPath);
        update.set${treeAncestorsCap}(newAncestors);
        if (!ObjectUtil.equal(current.get${treeParentCap}(), update.get${treeParentCap}())) updateChildrenAncestors(current, newAncestors);
</#if>
        requireChanged(${className}Mapper.updateById(update), 1);
        return true;
<#else>
        return ${className}Mapper.updateById(update) > 0;
</#if>
    }

<#if enableStatus>
    /**
     * 修改${functionName}状态
     *
     * @param ${pkColumn.javaField} 主键
     * @param status 状态值
     * @return 是否修改成功
     */
    @Override
    public Boolean updateStatus(${pkColumn.javaType} ${pkColumn.javaField}, ${statusColumn.javaType} status) {
        return ${className}Mapper.lambda()
            .set(${ClassName}::get${statusColumn.capJavaField}, status)
            .eq(${ClassName}::get${pkColumn.capJavaField}, ${pkColumn.javaField})
            .update();
    }
</#if>

<#if enableSort>
    /**
     * 调整${functionName}排序
     *
     * @param ${pkColumn.javaField} 主键
     * @param sortValue 排序值
     * @return 是否修改成功
     */
    @Override
    public Boolean updateSort(${pkColumn.javaType} ${pkColumn.javaField}, ${sortColumn.javaType} sortValue) {
        return ${className}Mapper.lambda()
            .set(${ClassName}::get${sortColumn.capJavaField}, sortValue)
            .eq(${ClassName}::get${pkColumn.capJavaField}, ${pkColumn.javaField})
            .update();
    }
</#if>

<#if table.tree>
    /** 按父边确定树根，统一锁域后current read重验，避免并发形成环或孤儿。 */
    private List<${ClassName}> lockStructure(List<${pkColumn.javaType}> currentIds, ${pkColumn.javaType} parentId) {
        if (TransactionContext.getXID() == null) throw new IllegalStateException("Tree mutations require a transaction");
        Map<${pkColumn.javaType}, List<${ClassName}>> before = new LinkedHashMap<>();
        for (${pkColumn.javaType} id : currentIds) before.put(id, path(id, false));
        if (!ObjectUtil.equal(parentId, ${treeRootValueJavaLiteral})) before.putIfAbsent(parentId, path(parentId, false));
        Set<${pkColumn.javaType}> roots = new TreeSet<>();
        before.values().forEach(nodes -> roots.add(nodes.getFirst().get${pkColumn.capJavaField}()));
        if (ObjectUtil.equal(parentId, ${treeRootValueJavaLiteral})) roots.addAll(currentIds);
        if (!roots.isEmpty() && ${className}Mapper.lockTreeRoots(new ArrayList<>(roots)).size() != roots.size()) {
            throw new ServiceException("树结构已变化，请刷新后重试");
        }
        List<${ClassName}> parentPath = List.of();
        for (var entry : before.entrySet()) {
            List<${ClassName}> locked = path(entry.getKey(), true);
            if (!ObjectUtil.equal(locked.getFirst().get${pkColumn.capJavaField}(), entry.getValue().getFirst().get${pkColumn.capJavaField}())) {
                throw new ServiceException("树结构已变化，请刷新后重试");
            }
            if (ObjectUtil.equal(entry.getKey(), parentId)) parentPath = locked;
        }
        return parentPath;
    }

    /** 校验真实父指针，不依赖可被篡改或过期的ancestors判断是否成环。 */
    private List<${ClassName}> path(${pkColumn.javaType} id, boolean locking) {
        List<${ClassName}> nodes = new ArrayList<>(); Set<${pkColumn.javaType}> seen = new HashSet<>();
        while (id != null && !ObjectUtil.equal(id, ${treeRootValueJavaLiteral})) {
            if (!seen.add(id)) throw new ServiceException("树层级存在循环，请先修复");
            ${ClassName} node = ${className}Mapper.selectStructure(id, locking);
            if (node == null) throw new ServiceException("树节点或父节点不存在");
            nodes.add(node); id = node.get${treeParentCap}();
        }
        if (id == null || nodes.isEmpty()) throw new ServiceException("树层级数据异常，请先修复");
        Collections.reverse(nodes);
<#if "" != treeAncestorsField>
        String expected = String.valueOf(${treeRootValueJavaLiteral});
        for (${ClassName} node : nodes) {
            if (!expected.equals(node.get${treeAncestorsCap}())) throw new ServiceException("树路径数据异常，请先修复");
            expected += "," + node.get${pkColumn.capJavaField}();
        }
</#if>
        return nodes;
    }

    /** 保持Mapper的数据权限；结构检查忽略可见性不代表允许写入隐藏节点。 */
    private ${ClassName} requireVisible(${pkColumn.javaType} id) {
        ${ClassName} node = ${className}Mapper.selectVisibleForUpdate(id);
        if (node == null) throw new ServiceException("树节点不存在或没有访问权限");
        return node;
    }

    /** 根值必须显式提供，null不能暗中转换为根。 */
    private static void requireParent(${pkColumn.javaType} id) {
        if (id == null) throw new ServiceException("父节点不能为空，请显式提供根值");
<#if pkColumn.javaType == "Long" || pkColumn.javaType == "Integer">
        if (id < 0) throw new ServiceException("父节点不能为负数");
</#if>
    }

    /** 主键不能使用根哨兵。 */
    private static void requireId(${pkColumn.javaType} id) {
        if (id == null || ObjectUtil.equal(id, ${treeRootValueJavaLiteral})) throw new ServiceException("树节点ID无效");
<#if pkColumn.javaType == "Long" || pkColumn.javaType == "Integer">
        if (id <= 0) throw new ServiceException("树节点ID必须大于0");
</#if>
    }

    /** 部分写入触发整个事务回滚。 */
    private static void requireChanged(int actual, int expected) {
        if (actual != expected) throw new ServiceException("树节点已变化，请刷新后重试");
    }
<#if "" != treeAncestorsField>

    /** 由已锁定父链生成规范路径。 */
    private String ancestors(List<${ClassName}> parentPath) {
        return parentPath.isEmpty() ? String.valueOf(${treeRootValueJavaLiteral})
            : parentPath.getLast().get${treeAncestorsCap}() + "," + parentPath.getLast().get${pkColumn.capJavaField}();
    }

    /** 按实际子边遍历并更新整个子树；任何坏路径或失败均回滚。 */
    private void updateChildrenAncestors(${ClassName} current, String newAncestors) {
        Map<${pkColumn.javaType}, String> oldPaths = new LinkedHashMap<>(), newPaths = new LinkedHashMap<>();
        oldPaths.put(current.get${pkColumn.capJavaField}(), current.get${treeAncestorsCap}());
        newPaths.put(current.get${pkColumn.capJavaField}(), newAncestors);
        List<${pkColumn.javaType}> parents = List.of(current.get${pkColumn.capJavaField}());
        while (!parents.isEmpty()) {
            List<${pkColumn.javaType}> next = new ArrayList<>();
            for (${ClassName} child : ${className}Mapper.selectTreeChildrenForUpdate(parents)) {
                String expected = oldPaths.get(child.get${treeParentCap}()) + "," + child.get${treeParentCap}();
                if (oldPaths.containsKey(child.get${pkColumn.capJavaField}()) || !expected.equals(child.get${treeAncestorsCap}())) {
                    throw new ServiceException("树路径数据异常，请先修复");
                }
                String changed = newPaths.get(child.get${treeParentCap}()) + "," + child.get${treeParentCap}();
                ${ClassName} update = new ${ClassName}(); update.set${pkColumn.capJavaField}(child.get${pkColumn.capJavaField}());
                update.set${treeAncestorsCap}(changed); requireChanged(${className}Mapper.updateById(update), 1);
                oldPaths.put(child.get${pkColumn.capJavaField}(), expected); newPaths.put(child.get${pkColumn.capJavaField}(), changed);
                next.add(child.get${pkColumn.capJavaField}());
            }
            parents = next;
        }
    }
</#if>
</#if>

    /**
     * 批量删除${functionName}；树节点必须全部可见且没有子节点。
     *
     * @param ids 待删除主键
     * @param isValid 保留的可选业务校验标志，不能跳过树完整性或数据权限
     * @return 是否删除成功
     */
    @Override
<#if table.tree>
    @DSTransactional
</#if>
    public Boolean deleteWithValidByIds(Collection<${pkColumn.javaType}> ids, Boolean isValid) {
<#if table.tree>
        if (ids == null || ids.isEmpty()) throw new ServiceException("删除节点不能为空");
        ids.forEach(${ClassName}ServiceImpl::requireId);
        List<${pkColumn.javaType}> distinctIds = new ArrayList<>(new TreeSet<>(ids));
        lockStructure(distinctIds, ${treeRootValueJavaLiteral});
        distinctIds.forEach(this::requireVisible);
        if (!${className}Mapper.selectTreeChildrenForUpdate(distinctIds).isEmpty()) throw new ServiceException("存在子节点，不允许删除");
        requireChanged(${className}Mapper.deleteByIds(distinctIds), distinctIds.size());
        return true;
<#else>
        return ${className}Mapper.deleteByIds(ids) > 0;
</#if>
    }
}
