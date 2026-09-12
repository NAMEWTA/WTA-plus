package org.namewta.profile.person.domain.model.read;

import lombok.Data;

import java.time.Instant;

/** 用户当前个人档案的查询读模型。 */
@Data
public class PersonActiveProjectionRow {

    private Long userId;
    private Long personProfileId;
    private Instant verifiedAt;
}
