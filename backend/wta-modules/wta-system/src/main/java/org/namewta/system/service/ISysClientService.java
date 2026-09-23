package org.namewta.system.service;

import org.namewta.common.core.domain.PageResult;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.system.domain.bo.SysClientBo;
import org.namewta.system.domain.vo.SysClientVo;

import java.util.Collection;
import java.util.List;

/**
 * 客户端管理Service接口
 *
 * @date 2023-06-18
 */
public interface ISysClientService {

    /**
     * 查询客户端管理
     */
    SysClientVo queryById(Long id);

    /**
     * 查询客户端信息基于客户端id
     */
    SysClientVo queryByClientId(String clientId);

    /**
     * 查询客户端管理列表
     */
    PageResult<SysClientVo> queryPageList(SysClientBo bo, PageQuery pageQuery);

    /**
     * 查询客户端管理列表
     */
    List<SysClientVo> queryList(SysClientBo bo);

    /**
     * 新增客户端管理
     */
    Boolean insertByBo(SysClientBo bo);

    /**
     * 修改客户端管理
     */
    Boolean updateByBo(SysClientBo bo);

    /**
     * 修改状态
     */
    int updateClientStatus(String clientId, String status);

    /**
     * 校验并批量删除客户端管理信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验客户端key是否唯一
     *
     * @param client 客户端信息
     * @return 结果
     */
    boolean checkClickKeyUnique(SysClientBo client);

    /**
     * 轮换 SSO 密钥，明文只返回一次。
     *
     * @param id 客户端主键
     * @return 含一次性明文的视图
     */
    SysClientVo rotateSsoSecret(Long id);

    /**
     * 客户端管理完成自有 App SSO 接入（须已在 SSO 管理登记）。
     *
     * @param id       主键
     * @param authMode sso 或 both
     * @return 接入后的视图
     */
    SysClientVo bindSsoAccess(Long id, String authMode);
}
