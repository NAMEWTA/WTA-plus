package org.namewta.notify.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 场景及其渠道绑定展示对象。
 */
@Data
public class NotifySceneBindingVo {
    private String sceneCode;
    private String title;
    private List<NotifySceneVariableVo> variables;
    private String channel;
    private Long bindingId;
    private Long accountId;
    private String accountConfigKey;
    private String accountEnabled;
    private String mailSubject;
    private String mailBody;
    private String smsTemplateCode;
    private Map<String, String> smsParamMapping;
    private Integer templateMinuteMax;
    private String restricted;
    private Integer recipientMinuteMax;
    private Integer recipientDayMax;
}
