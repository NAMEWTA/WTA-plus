package org.namewta.system.service;

import org.namewta.common.core.domain.PageResult;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.system.domain.bo.*;
import org.namewta.system.domain.vo.SysAuthProviderVo;
import org.namewta.system.domain.vo.SysAuthRegistrationVo;

/** 外部身份源及业务客户端接入管理。 */
public interface ISysExternalAuthConfigService {
    /** 身份源分页。 */
    PageResult<SysAuthProviderVo> providers(SysAuthProviderBo query, PageQuery page);
    /** 身份源详情。 */
    SysAuthProviderVo provider(long id);
    /** 新增或编辑身份源；编辑检测版本。 */
    Long saveProvider(SysAuthProviderBo bo, boolean create);
    /** 有接入引用的身份源禁止删除。 */
    void removeProvider(ExternalAuthRemoveBo bo);
    /** 接入分页；不包含密钥或密文。 */
    PageResult<SysAuthRegistrationVo> registrations(SysAuthRegistrationBo query, PageQuery page);
    /** 接入详情；不包含密钥或密文。 */
    SysAuthRegistrationVo registration(long id);
    /** 新增或编辑接入；编辑时空密钥保留。 */
    Long saveRegistration(SysAuthRegistrationBo bo, boolean create);
    /** 逻辑删除接入并保留退出校验所需的墓碑。 */
    void removeRegistration(ExternalAuthRemoveBo bo);
    /** 显式预览或导入旧 justauth.type 内容，空示例与已有接入不覆盖。 */
    org.namewta.system.domain.vo.ExternalAuthLegacyImportResultVo importLegacy(ExternalAuthLegacyImportBo bo);
    /** 显式清除当前全部共享快照；后续请求按数据库回源。 */
    void refresh();
}
