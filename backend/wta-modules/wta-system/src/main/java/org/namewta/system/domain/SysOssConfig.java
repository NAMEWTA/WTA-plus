package org.namewta.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

/**
 * 对象存储配置对象 sys_oss_config
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oss_config")
public class SysOssConfig extends BaseEntity {

    /**
     * 主键
     */
    @TableId(value = "oss_config_id")
    private Long ossConfigId;

    /**
     * 配置key
     */
    private String configKey;

    /**
     * accessKey
     */
    private String accessKey;

    /**
     * 秘钥
     */
    private String secretKey;

    /**
     * 桶名称
     */
    private String bucketName;

    /**
     * 前缀
     */
    private String prefix;

    /**
     * 访问站点
     */
    private String endpoint;

    /**
     * 自定义域名
     */
    private String domainUrl;

    /**
     * 是否https（Y=是，N=否）
     */
    private String isHttps;

    /**
     * 域
     */
    private String region;

    /**
     * 是否默认（Y=是,N=否）
     */
    private String status;

    /**
     * 扩展字段
     */
    private String ext1;

    /**
     * 备注
     */
    private String remark;

    /**
     * 桶权限类型（0=PRIVATE，2=PUBLIC_READ）
     */
    private String accessPolicy;
}
