package org.namewta.profile.enterprise.service;
import org.namewta.profile.enterprise.domain.application.EnterpriseDocumentTypeRule;
import org.namewta.profile.enterprise.domain.application.EnterprisePublication;
import org.namewta.profile.enterprise.domain.application.EnterpriseSubmission;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationVo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationSaveBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationProbeBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationProbeVo;
/**
 * 承载IEnterpriseApplicationService业务规则的领域服务。
 */
public interface IEnterpriseApplicationService extends org.namewta.profile.enterprise.port.EnterpriseApplicationPublicationPort {
    /**
     * 查询当前用户的进行中申请
     */
    EnterpriseApplicationVo current(long userId);
    /**
     * 校验申请身份并返回探测结果
     */
    EnterpriseApplicationProbeVo probe(EnterpriseApplicationProbeBo command);
    /**
     * 保存业务申请数据
     */
    EnterpriseApplicationVo save(long userId, EnterpriseApplicationSaveBo command);
    /**
     * 提交申请并启动后续流程
     */
    EnterpriseApplicationVo submit(long userId, int expectedVersion);
}
