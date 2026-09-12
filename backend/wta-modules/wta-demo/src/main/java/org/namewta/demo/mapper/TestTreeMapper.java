package org.namewta.demo.mapper;

import org.namewta.common.mybatis.annotation.DataColumn;
import org.namewta.common.mybatis.annotation.DataPermission;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.demo.domain.TestTree;
import org.namewta.demo.domain.vo.TestTreeVo;

/**
 * 测试树表Mapper接口
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userName", value = "user_id")
})
public interface TestTreeMapper extends BaseMapperPlus<TestTree, TestTreeVo> {

}
