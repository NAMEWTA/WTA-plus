package org.namewta.oidc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.namewta.common.mybatis.core.domain.BaseEntity;

/** 服务配置唯一持久来源；JSON不包含明文密钥。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oidc_service_config")
public class OidcServiceConfig extends BaseEntity {
    @TableId private Long serviceConfigId;
    private String settingsJson;
    @Version private Integer version;
    @TableLogic private String delFlag;
}
