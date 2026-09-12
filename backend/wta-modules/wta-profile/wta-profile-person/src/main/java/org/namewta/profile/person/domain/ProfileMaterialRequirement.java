package org.namewta.profile.person.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

/** ProfileMaterialRequirement 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_material_requirement")
public class ProfileMaterialRequirement extends BaseEntity {

    @TableId("material_requirement_id")
    private Long materialRequirementId;
    private String profileType;
    private String documentTypeCode;
    private String handlerCondition;
    private String materialTagCode;
    private Integer minimumCount;
    private String status;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
