package org.namewta.notify.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 通知配置测试发送参数。
 */
@Data
public class NotifyTestSendBo {
    private Long accountId;
    private String sceneCode;
    private String channel;
    @NotBlank(message = "测试收件人不能为空")
    private String target;
}
