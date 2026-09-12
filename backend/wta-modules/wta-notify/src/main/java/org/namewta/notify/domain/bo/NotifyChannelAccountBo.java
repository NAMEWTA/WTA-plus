package org.namewta.notify.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 渠道账号写入参数。
 */
@Data
public class NotifyChannelAccountBo {
    private Long accountId;
    @NotBlank(message = "渠道不能为空")
    private String channel;
    @NotBlank(message = "配置标识不能为空")
    private String configKey;
    private String enabled;
    private String supplier;
    private String host;
    private Integer port;
    private String mailFrom;
    private String mailUser;
    private String mailPass;
    private String sslEnable;
    private String starttlsEnable;
    private String accessKeyId;
    private String accessKeySecret;
    private String signature;
    private String sdkAppId;
    @NotNull(message = "账号每分钟上限不能为空")
    private Integer minuteMax;
    private String remark;
}
