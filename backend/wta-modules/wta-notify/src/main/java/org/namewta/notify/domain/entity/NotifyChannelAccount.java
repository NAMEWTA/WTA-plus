package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

/**
 * 通知渠道账号，邮件 SMTP 或短信厂商凭据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_channel_account")
public class NotifyChannelAccount extends BaseEntity {
    @TableId
    private Long accountId;
    private String channel;
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
    private Integer minuteMax;
    private String remark;
    /** 账号并发编辑版本。 */
    @Version
    private Integer version;
    /** 删除后仍保留渠道和配置标识的唯一命名空间。 */
    @TableLogic
    private String delFlag;
}
