package org.namewta.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

/**
 * 逻辑场景在某一渠道上的账号绑定、文案和限额。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_scene_binding")
public class NotifySceneBinding extends BaseEntity {
    @TableId
    private Long bindingId;
    private String sceneCode;
    private String channel;
    private Long accountId;
    private String mailSubject;
    private String mailBody;
    private String smsTemplateCode;
    private String smsParamMappingJson;
    private Integer templateMinuteMax;
    private String restricted;
    private Integer recipientMinuteMax;
    private Integer recipientDayMax;
}
