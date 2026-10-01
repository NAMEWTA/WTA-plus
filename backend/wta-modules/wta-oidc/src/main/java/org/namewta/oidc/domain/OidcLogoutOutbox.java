package org.namewta.oidc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/** 标准退出投递快照；授权清理不删除该持久重试事实。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oidc_logout_outbox")
public class OidcLogoutOutbox extends BaseEntity {
    @TableId private Long logoutOutboxId;
    private Long applicationId;
    private String clientId;
    private String issuer;
    private String sessionHash;
    private String subject;
    private String targetUri;
    private String status;
    private Integer attempts;
    private LocalDateTime nextAttemptAt;
    private String leaseToken;
    private LocalDateTime leaseUntil;
    private String lastError;
    @Version private Integer version;
    @TableLogic private String delFlag;
}
