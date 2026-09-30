package org.namewta.oidc.domain;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.namewta.common.mybatis.core.domain.BaseEntity;

/** OIDC 持久化事实，凭据完整内容仅允许保存认证加密密文。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oidc_subject")
public class OidcSubject extends BaseEntity {
    @TableId private Long subjectId;
    private Long userId;
    private String subject;
    @Version private Integer version;
    @TableLogic private String delFlag;
}
