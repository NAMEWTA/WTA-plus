package org.namewta.profile.person.service;
import org.namewta.profile.person.domain.application.PersonDocumentTypeRule;
import org.namewta.profile.person.domain.application.PersonPublication;
import org.namewta.profile.person.domain.application.PersonSubmission;
import org.namewta.profile.person.domain.bo.PersonApplicationSaveBo;
import org.namewta.profile.person.domain.vo.PersonApplicationVo;
import java.time.Instant;
/**
 * 承载IPersonApplicationService业务规则的领域服务。
 */
public interface IPersonApplicationService extends org.namewta.profile.person.port.PersonApplicationPublicationPort {
    /**
     * 查询当前用户的进行中申请
     */
    PersonApplicationVo current(long userId);
    /**
     * 保存业务申请数据
     */
    PersonApplicationVo save(long userId, PersonApplicationSaveBo command);
    /**
     * 提交申请并启动后续流程
     */
    PersonApplicationVo submit(long userId, int expectedVersion);
}
