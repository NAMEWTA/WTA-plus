package org.namewta.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.namewta.common.core.utils.StreamUtils;
import org.namewta.common.mybatis.annotation.DataColumn;
import org.namewta.common.mybatis.annotation.DataPermission;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.system.domain.SysDept;
import org.namewta.system.domain.SysRole;
import org.namewta.system.domain.SysRoleDept;
import org.namewta.system.domain.vo.SysDeptVo;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.namewta.common.core.constant.SystemConstants.NORMAL;

/**
 * 部门管理 数据层
 *
 * @author Lion Li
 */
public interface SysDeptMapper extends BaseMapperPlus<SysDept, SysDeptVo>, MPJBaseMapper<SysDept> {

    /** Internal structure read; locking reads must run under the department mutation transaction. */
    SysDept selectStructure(@Param("deptId") Long deptId, @Param("locking") boolean locking);

    /** Lock existing tree roots in primary-key order, shared by insert, move and delete. */
    List<Long> lockTreeRoots(@Param("rootIds") List<Long> rootIds);

    /** Traverse actual parent edges, including descendants whose stored ancestors may be corrupt. */
    List<SysDept> selectChildrenForUpdate(@Param("parentIds") List<Long> parentIds);

    /** Recheck the existing data-scope policy using a current read after structural locks are held. */
    @DataPermission({@DataColumn(key = "deptName", value = "dept_id")})
    Long selectVisibleDeptForUpdate(@Param("deptId") Long deptId);

    /**
     * 查询部门管理数据
     *
     * @param queryWrapper 查询条件
     * @return 部门信息集合
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    default List<SysDeptVo> selectDeptList(Wrapper<SysDept> queryWrapper) {
        return this.selectVoList(queryWrapper);
    }

    /**
     * 分页查询部门管理数据
     *
     * @param page         分页信息
     * @param queryWrapper 查询条件
     * @return 部门信息集合
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
    })
    default Page<SysDeptVo> selectPageDeptList(Page<SysDept> page, Wrapper<SysDept> queryWrapper) {
        return this.selectVoPage(page, queryWrapper);
    }

    /**
     * 统计指定部门ID的部门数量
     *
     * @param deptId 部门ID
     * @return 该部门ID的部门数量
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    default long countDeptById(Long deptId) {
        return this.lambda().eq(SysDept::getDeptId, deptId).count();
    }

    /**
     * 根据父部门ID查询其所有子部门的列表
     *
     * @param parentId 父部门ID
     * @return 包含子部门的列表
     */
    default List<SysDept> selectListByParentId(Long parentId) {
        return this.lambda()
            .select(SysDept::getDeptId)
            .findInSet(parentId, SysDept::getAncestors)
            .list();
    }

    /**
     * 查询某个部门及其所有子部门ID（含自身）
     *
     * @param parentId 父部门ID
     * @return 部门ID集合
     */
    default List<Long> selectDeptAndChildById(Long parentId) {
        List<SysDept> deptList = this.selectListByParentId(parentId);
        List<Long> deptIds = StreamUtils.toList(deptList, SysDept::getDeptId);
        deptIds.add(parentId);
        return deptIds;
    }

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId            角色ID
     * @param deptCheckStrictly 部门树选择项是否关联显示
     * @return 选中部门列表
     */
    default List<Long> selectDeptListByRoleId(Long roleId, boolean deptCheckStrictly) {
        List<SysDept> depts = this.selectJoinList(SysDept.class, QueryBuilder.lambdaJoin("d", SysDept.class)
            .distinct()
            .select(SysDept::getDeptId, SysDept::getParentId, SysDept::getOrderNum)
            .leftJoin(SysRoleDept.class, "srd", SysRoleDept::getDeptId, SysDept::getDeptId)
            .leftJoin(SysRole.class, "sr", SysRole::getRoleId, SysRoleDept::getRoleId)
            .eq("srd", SysRoleDept::getRoleId, roleId)
            .eq("sr", SysRole::getStatus, NORMAL)
            .orderByAsc("d", SysDept::getParentId)
            .orderByAsc("d", SysDept::getOrderNum)
            .build());
        Set<Long> parentIds = deptCheckStrictly ? new HashSet<>(StreamUtils.toList(depts, SysDept::getParentId)) : Collections.emptySet();
        return depts.stream()
            .map(SysDept::getDeptId)
            .filter(deptId -> !parentIds.contains(deptId))
            .toList();
    }

}
