package org.namewta.notify.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 渠道账号展示对象，不含密钥明文。
 */
@Data
public class NotifyChannelAccountVo {
    private Long accountId;
    private String channel;
    private String configKey;
    private String enabled;
    private String supplier;
    private String host;
    private Integer port;
    private String mailFrom;
    private String mailUser;
    private boolean mailPassSet;
    private String sslEnable;
    private String starttlsEnable;
    private String accessKeyId;
    private boolean accessKeySecretSet;
    private String signature;
    private String sdkAppId;
    private Integer minuteMax;
    private String remark;
    private LocalDateTime updateTime;
}
