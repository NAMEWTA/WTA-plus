package org.namewta.profile.person.domain.model.read;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 个人身份精确匹配查询读模型。 */
@Data
public class PersonActiveIdentityMatchRow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long personProfileId;
}
