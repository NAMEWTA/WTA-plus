package org.namewta.profile.enterprise.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.Alias;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 企业供应商验证尝试实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Alias("EnterpriseProfileVerificationAttempt")
@TableName("profile_verification_attempt")
public class ProfileVerificationAttempt extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("verification_attempt_id")
    private Long verificationAttemptId;
    private String profileType;
    private Long applicationId;
    private Long submissionId;
    private String providerCode;
    private String providerRequestId;
    private String requestFingerprint;
    private Integer attemptNo;
    private String status;
    private String normalizedResultJson;
    private String providerEvidenceJson;
    private String errorCode;
    private LocalDateTime completedTime;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
