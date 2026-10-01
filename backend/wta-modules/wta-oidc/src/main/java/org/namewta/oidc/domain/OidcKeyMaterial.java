package org.namewta.oidc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.namewta.common.mybatis.core.domain.BaseEntity;

/** 版本化私钥材料，仅认证加密密文进入数据库。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oidc_key_material")
public class OidcKeyMaterial extends BaseEntity {
    @TableId private Long keyMaterialId;
    private String kind;
    private String kid;
    private String encryptedMaterial;
    private Boolean active;
    @Version private Integer version;
    @TableLogic private String delFlag;
}
