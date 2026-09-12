package org.namewta.profile.person.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/** ProfileMaterialRef 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_material_ref")
public class ProfileMaterialRef extends BaseEntity {

    @TableId("material_ref_id")
    private Long materialRefId;
    private String ownerType;
    private Long ownerId;
    private String profileType;
    private Long ossId;
    private Long materialNodeId;
    private String materialTagCode;
    private String materialTagName;
    private String fileName;
    private Long fileSize;
    private String fileExtension;
    private String mimeType;
    private String status;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String activeOwnerOssKey;
    private String immutableFlag;
    private LocalDateTime attachedTime;
    private LocalDateTime detachedTime;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
