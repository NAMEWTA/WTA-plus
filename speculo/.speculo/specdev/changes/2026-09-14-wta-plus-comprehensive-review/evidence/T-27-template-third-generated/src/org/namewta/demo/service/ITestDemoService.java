package org.namewta.demo.service;

import org.namewta.demo.domain.vo.TestDemoVo;
import org.namewta.demo.domain.bo.TestDemoBo;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 模板集成验收Service接口
 *
 * @author NAMEWTA
 * @date 2026-09-19
 */
public interface ITestDemoService {

    /**
     * 查询模板集成验收
     *
     * @param id 主键
     * @return 模板集成验收
     */
    TestDemoVo queryById(Long id);

    /**
     * 分页查询模板集成验收列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 模板集成验收分页列表
     */
    PageResult<TestDemoVo> queryPageList(TestDemoBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的模板集成验收列表
     *
     * @param bo 查询条件
     * @return 模板集成验收列表
     */
    List<TestDemoVo> queryList(TestDemoBo bo);


    /**
     * 新增模板集成验收
     *
     * @param bo 模板集成验收
     * @return 是否新增成功
     */
    Boolean insertByBo(TestDemoBo bo);

    /**
     * 修改模板集成验收
     *
     * @param bo 模板集成验收
     * @return 是否修改成功
     */
    Boolean updateByBo(TestDemoBo bo);



    /**
     * 校验并批量删除模板集成验收信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
