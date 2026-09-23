package org.namewta.system.service;

import org.namewta.common.core.domain.PageResult;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.system.domain.bo.SysUserTypeBo;
import org.namewta.system.domain.vo.SysUserTypeVo;

import java.util.Collection;
import java.util.List;

/**
 * 登录域Service接口
 */
public interface ISysUserTypeService {

    /**
     * 查询登录域
     *
     * @param userTypeId 登录域ID
     * @return 登录域
     */
    SysUserTypeVo queryById(Long userTypeId);

    /**
     * 按编码查询登录域
     *
     * @param userTypeCode 登录域编码
     * @return 登录域
     */
    SysUserTypeVo queryByCode(String userTypeCode);

    /**
     * 分页查询登录域列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 登录域分页列表
     */
    PageResult<SysUserTypeVo> queryPageList(SysUserTypeBo bo, PageQuery pageQuery);

    /**
     * 查询登录域列表
     *
     * @param bo 查询条件
     * @return 登录域列表
     */
    List<SysUserTypeVo> queryList(SysUserTypeBo bo);

    /**
     * 查询启用中的登录域下拉列表
     *
     * @return 登录域列表
     */
    List<SysUserTypeVo> options();

    /**
     * 新增登录域
     *
     * @param bo 登录域信息
     * @return 是否成功
     */
    Boolean insertByBo(SysUserTypeBo bo);

    /**
     * 修改登录域
     *
     * @param bo 登录域信息
     * @return 是否成功
     */
    Boolean updateByBo(SysUserTypeBo bo);

    /**
     * 修改登录域状态
     *
     * @param userTypeId 登录域ID
     * @param status     状态
     * @return 更新条数
     */
    int updateStatus(Long userTypeId, String status);

    /**
     * 校验并批量删除登录域
     *
     * @param ids 登录域ID集合
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids);

    /**
     * 校验登录域编码是否唯一
     *
     * @param bo 登录域信息
     * @return 是否唯一
     */
    boolean checkUserTypeCodeUnique(SysUserTypeBo bo);

}
