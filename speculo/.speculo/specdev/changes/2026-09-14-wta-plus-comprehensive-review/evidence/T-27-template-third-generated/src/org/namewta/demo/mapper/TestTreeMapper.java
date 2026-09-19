package org.namewta.demo.mapper;

import org.namewta.demo.domain.TestTree;
import org.namewta.demo.domain.vo.TestTreeVo;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 模板集成验收Mapper接口
 *
 * @author NAMEWTA
 * @date 2026-09-19
 */
public interface TestTreeMapper extends BaseMapperPlus<TestTree, TestTreeVo> {

    /** 仅用于内部结构校验，不返回给HTTP调用者。 */
    @InterceptorIgnore(dataPermission = "true")
    TestTree selectStructure(@Param("id") Long id, @Param("locking") boolean locking);

    /** 按主键顺序锁定结构根。 */
    @InterceptorIgnore(dataPermission = "true")
    List<TestTree> lockTreeRoots(@Param("ids") List<Long> ids);

    /** 通过资源数据权限校验当前节点和变化的新父节点。 */
    TestTree selectVisibleForUpdate(@Param("id") Long id);

    /** 包含隐藏子节点，防止删除留下孤儿；必须配套父列索引。 */
    @InterceptorIgnore(dataPermission = "true")
    List<TestTree> selectTreeChildrenForUpdate(@Param("ids") List<Long> ids);
}
