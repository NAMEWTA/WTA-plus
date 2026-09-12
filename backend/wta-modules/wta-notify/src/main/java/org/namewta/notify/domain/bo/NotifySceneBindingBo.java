package org.namewta.notify.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 场景渠道绑定写入参数。
 */
@Data
public class NotifySceneBindingBo {
    @NotBlank(message = "场景编码不能为空")
    private String sceneCode;
    @NotBlank(message = "渠道不能为空")
    private String channel;
    private Long accountId;
    private String mailSubject;
    private String mailBody;
    private String smsTemplateCode;
    private Map<String, String> smsParamMapping;
    private Integer templateMinuteMax;
    private String restricted;
    private Integer recipientMinuteMax;
    private Integer recipientDayMax;
}
