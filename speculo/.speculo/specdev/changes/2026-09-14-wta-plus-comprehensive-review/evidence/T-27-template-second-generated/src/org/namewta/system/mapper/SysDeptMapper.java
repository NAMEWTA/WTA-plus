package org.namewta.system.mapper;

import org.namewta.system.domain.SysDept;
import org.namewta.system.domain.vo.SysDeptVo;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 模板集成验收Mapper接口
 *
 * @date 2026-09-19
 */
public interface SysDeptMapper extends BaseMapperPlus<SysDept, SysDeptVo> {

    /** 仅用于内部结构校验，不返回给HTTP调用者。 */
    @InterceptorIgnore(dataPermission = "true")
    SysDept selectStructure(@Param("id") Long id, @Param("locking") boolean locking);

    /** 按主键顺序锁定结构根。 */
    @InterceptorIgnore(dataPermission = "true")
    List<SysDept> lockTreeRoots(@Param("ids") List<Long> ids);

    /** 通过资源数据权限校验当前节点和变化的新父节点。 */
    SysDept selectVisibleForUpdate(@Param("id") Long id);

    /** 包含隐藏子节点，防止删除留下孤儿；必须配套父列索引。 */
    @InterceptorIgnore(dataPermission = "true")
    List<SysDept> selectTreeChildrenForUpdate(@Param("ids") List<Long> ids);
}
