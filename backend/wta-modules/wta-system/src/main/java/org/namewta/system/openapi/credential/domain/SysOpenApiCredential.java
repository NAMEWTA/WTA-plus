package org.namewta.system.openapi.credential.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/**
 * A user's single active OpenAPI credential.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_open_api_credential")
public class SysOpenApiCredential extends BaseEntity {

    @TableId("open_api_credential_id")
    private Long openApiCredentialId;
    private Long ownerUserId;

    @TableField(insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER,
        updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private Long activeOwnerUserId;

    private String appKey;
    private String appName;
    private byte[] secretCiphertext;
    private byte[] secretNonce;
    private byte[] secretTag;
    private String kekVersion;
    private String status;
    private LocalDateTime expiresAt;
    private String remark;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
