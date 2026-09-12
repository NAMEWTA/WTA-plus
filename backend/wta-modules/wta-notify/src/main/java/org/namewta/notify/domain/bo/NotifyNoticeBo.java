package org.namewta.notify.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 通知中心公告写入参数。
 */
@Data
public class NotifyNoticeBo {
    private Long noticeId;
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 50, message = "公告标题不能超过{max}个字符")
    private String noticeTitle;
    @NotBlank(message = "公告类型不能为空")
    @jakarta.validation.constraints.Pattern(regexp = "[12]", message = "通知类型仅支持通知和公告")
    private String noticeType;
    @NotBlank(message = "通知内容不能为空")
    private String noticeContent;
    /** ALL 全部正常用户；USER 指定用户；USER_TYPE 匹配任一启用类型。 */
    private String recipientType;
    /** 指定用户 ID 快照，只在 USER 模式非空。 */
    private List<Long> recipientIds;
    /** 用户类型 ID 快照，只在 USER_TYPE 模式非空。 */
    private List<Long> userTypeIds;
    /** 本次发布选择的渠道，缺省为站内信。 */
    private List<String> channels;
    private String status;
    private String remark;
}
