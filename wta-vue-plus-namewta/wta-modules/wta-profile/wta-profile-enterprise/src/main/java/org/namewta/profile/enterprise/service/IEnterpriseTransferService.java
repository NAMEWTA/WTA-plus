package org.namewta.profile.enterprise.service;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferConfirmBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferSendBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseTransferVo;
/** 企业档案转移服务接口，定义挑战发送、确认和解绑能力。 */
public interface IEnterpriseTransferService {
    /** 发起企业档案转移。 */
    EnterpriseTransferVo send(long sourceUserId, EnterpriseTransferSendBo command);
    /** 确认换绑验证码。 */
    EnterpriseTransferVo confirm(long sourceUserId, EnterpriseTransferConfirmBo command);
    /** 解除档案绑定关系。 */
    EnterpriseTransferVo unbind(long userId);
}
