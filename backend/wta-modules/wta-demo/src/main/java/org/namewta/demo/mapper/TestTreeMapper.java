package org.namewta.demo.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import org.namewta.common.mybatis.annotation.DataColumn;
import org.namewta.common.mybatis.annotation.DataPermission;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.demo.domain.TestTree;
import org.namewta.demo.domain.vo.TestTreeVo;

import java.util.List;

/**
 * 测试树表Mapper接口
 *
 * @date 2021-07-26
 */
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userName", value = "user_id")
})
public interface TestTreeMapper extends BaseMapperPlus<TestTree, TestTreeVo> {

    /** 仅供服务端树不变量检查读取父边；不向调用者返回隐藏节点。 */
    @InterceptorIgnore(dataPermission = "true")
    TestTree selectStructure(@Param("id") Long id, @Param("locking") boolean locking);

    /** 按主键升序锁定树根，使新增、移动和删除使用同一结构锁域。 */
    @InterceptorIgnore(dataPermission = "true")
    List<TestTree> lockTreeRoots(@Param("ids") List<Long> ids);

    /** 锁后重验当前节点或新父节点的数据访问范围。 */
    TestTree selectVisibleForUpdate(@Param("id") Long id);

    /** 检查所有直接子节点，包含调用者不可见的子节点，防止删除留下孤儿。 */
    @InterceptorIgnore(dataPermission = "true")
    List<Long> selectChildIdsForUpdate(@Param("ids") List<Long> ids);
}
