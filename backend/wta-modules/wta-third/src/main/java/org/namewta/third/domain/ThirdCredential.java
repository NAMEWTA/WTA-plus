package org.namewta.third.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("third_credential")
public class ThirdCredential extends BaseEntity {
    @TableId private Long credentialId;
    private Long providerId;
    private Long endpointId;
    private String scopeType;
    private String credentialType;
    private byte[] ciphertext;
    private byte[] nonce;
    private byte[] authTag;
    private String kekVersion;
    private LocalDateTime expiresAt;
    private Integer version;
    private String delFlag;
}
