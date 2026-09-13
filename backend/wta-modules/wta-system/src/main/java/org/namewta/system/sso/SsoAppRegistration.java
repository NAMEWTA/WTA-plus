package org.namewta.system.sso;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.system.domain.bo.SysClientBo;

/**
 * SSO 管理登记：创建应用与拿配置必须走本入口，强制写入扩展后的 sys_client SSO 字段。
 */
public final class SsoAppRegistration {

    private SsoAppRegistration() {
    }

    /**
     * 规范化 SSO 管理提交：始终启用 SSO，并校验精确回调。
     *
     * @param bo 管理面输入
     * @return 同一对象，便于链式调用
     */
    public static SysClientBo prepare(SysClientBo bo) {
        if (bo == null) {
            throw new ServiceException("SSO 应用不能为空");
        }
        bo.setSsoEnabled(Boolean.TRUE);
        if (StringUtils.isBlank(bo.getSsoAuthMode())) {
            bo.setSsoAuthMode("both");
        }
        if (StringUtils.isBlank(bo.getSsoClientKind())) {
            bo.setSsoClientKind("public");
        }
        if (bo.getSsoPkceRequired() == null) {
            bo.setSsoPkceRequired(Boolean.TRUE);
        }
        if (bo.getSsoAutoConsent() == null) {
            bo.setSsoAutoConsent(Boolean.TRUE);
        }
        SsoClientFieldsSupport.validate(bo);
        return bo;
    }
}
