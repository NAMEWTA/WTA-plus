package org.namewta.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.namewta.common.excel.annotation.ExcelDictFormat;
import org.namewta.common.excel.convert.ExcelDictConvert;
import org.namewta.system.domain.SysUserType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录域视图对象 sys_user_type
 *
 * @author NAMEWTA
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysUserType.class)
public class SysUserTypeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录域ID
     */
    @ExcelProperty(value = "登录域ID")
    private Long userTypeId;

    /**
     * 登录域编码
     */
    @ExcelProperty(value = "登录域编码")
    private String userTypeCode;

    /**
     * 登录域名称
     */
    @ExcelProperty(value = "登录域名称")
    private String userTypeName;

    /**
     * 显示顺序
     */
    @ExcelProperty(value = "显示顺序")
    private Integer orderNum;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

}
