package org.namewta.system.service;

import org.namewta.system.domain.vo.SysDeptVo;
import org.namewta.system.domain.bo.SysDeptBo;

import java.util.Collection;
import java.util.List;

/**
 * 模板集成验收Service接口
 *
 * @date 2026-09-19
 */
public interface ISysDeptService {

    /**
     * 查询模板集成验收
     *
     * @param deptId 主键
     * @return 模板集成验收
     */
    SysDeptVo queryById(Long deptId);


    /**
     * 查询符合条件的模板集成验收列表
     *
     * @param bo 查询条件
     * @return 模板集成验收列表
     */
    List<SysDeptVo> queryList(SysDeptBo bo);


    /**
     * 新增模板集成验收
     *
     * @param bo 模板集成验收
     * @return 是否新增成功
     */
    Boolean insertByBo(SysDeptBo bo);

    /**
     * 修改模板集成验收
     *
     * @param bo 模板集成验收
     * @return 是否修改成功
     */
    Boolean updateByBo(SysDeptBo bo);



    /**
     * 校验并批量删除模板集成验收信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
