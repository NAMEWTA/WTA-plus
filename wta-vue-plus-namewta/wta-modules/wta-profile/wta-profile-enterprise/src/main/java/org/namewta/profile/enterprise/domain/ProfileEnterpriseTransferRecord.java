package org.namewta.profile.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 企业负责人转移挑战与结果审计实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_enterprise_transfer_record")
public class ProfileEnterpriseTransferRecord extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("enterprise_transfer_record_id")
    private Long enterpriseTransferRecordId;
    private Long enterpriseProfileId;
    private Long sourceBindingId;
    private Long sourceUserId;
    private Long targetUserId;
    private String challengeId;
    private Integer expectedBindingVersion;
    private String status;
    private Integer failedAttempts;
    private LocalDateTime expiresTime;
    private LocalDateTime confirmedTime;
    private String failureCategory;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
