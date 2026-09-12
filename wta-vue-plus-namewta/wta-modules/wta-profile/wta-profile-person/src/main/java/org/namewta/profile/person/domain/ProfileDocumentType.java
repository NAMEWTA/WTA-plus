package org.namewta.profile.person.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

/** ProfileDocumentType 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_document_type")
public class ProfileDocumentType extends BaseEntity {

    @TableId("document_type_id")
    private Long documentTypeId;
    private String documentTypeCode;
    private String issuingRegion;
    private String documentTypeName;
    private String numberPattern;
    private String validityRequired;
    private String status;
    private Integer orderNum;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
