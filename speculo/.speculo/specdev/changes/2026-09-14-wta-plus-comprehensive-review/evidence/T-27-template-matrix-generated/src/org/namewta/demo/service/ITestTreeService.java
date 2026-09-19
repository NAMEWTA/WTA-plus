package org.namewta.demo.service;

import org.namewta.demo.domain.vo.TestTreeVo;
import org.namewta.demo.domain.bo.TestTreeBo;

import java.util.Collection;
import java.util.List;

/**
 * 模板集成验收Service接口
 *
 * @author NAMEWTA
 * @date 2026-09-19
 */
public interface ITestTreeService {

    /**
     * 查询模板集成验收
     *
     * @param id 主键
     * @return 模板集成验收
     */
    TestTreeVo queryById(Long id);


    /**
     * 查询符合条件的模板集成验收列表
     *
     * @param bo 查询条件
     * @return 模板集成验收列表
     */
    List<TestTreeVo> queryList(TestTreeBo bo);


    /**
     * 新增模板集成验收
     *
     * @param bo 模板集成验收
     * @return 是否新增成功
     */
    Boolean insertByBo(TestTreeBo bo);

    /**
     * 修改模板集成验收
     *
     * @param bo 模板集成验收
     * @return 是否修改成功
     */
    Boolean updateByBo(TestTreeBo bo);



    /**
     * 校验并批量删除模板集成验收信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
