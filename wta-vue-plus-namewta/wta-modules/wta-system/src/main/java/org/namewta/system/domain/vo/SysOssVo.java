package org.namewta.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.namewta.common.translation.annotation.Translation;
import org.namewta.common.translation.constant.TransConstant;
import org.namewta.system.domain.SysOss;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OSS对象存储视图对象 sys_oss
 *
 * @author Lion Li
 */
@Data
@AutoMapper(target = SysOss.class)
public class SysOssVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 对象存储主键
     */
    private Long ossId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 原名
     */
    private String originalName;

    /**
     * 文件后缀名
     */
    private String fileSuffix;

    /**
     * URL地址
     */
    private String url;

    /**
     * 扩展字段
     */
    private String ext1;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 上传人
     */
    private Long createBy;

    /**
     * 上传人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "createBy")
    private String createByName;

    /**
     * 服务商
     */
    private String service;

    /**
     * 是否为临时对象。
     */
    private String isTemp;

    /**
     * 临时对象到期时间。
     */
    private LocalDateTime expireTime;

    /**
     * 对象删除状态（ACTIVE 正常，PENDING 等待供应商删除）。
     */
    private String deleteState;

    /**
     * 当前有效业务引用数。
     */
    private Long referenceCount;

    /**
     * 用于管理面反向定位的引用摘要。
     */
    private java.util.List<org.namewta.system.api.OssService.OssReference> references;


}
