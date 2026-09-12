package org.namewta.profile.enterprise.domain.model.read;

import lombok.Data;

/** 企业转移当前绑定所有者查询读模型。 */
@Data
public class EnterpriseTransferOwnerRow {

    private Long bindingId;
    private Long profileId;
    private Long userId;
    private Integer bindingVersion;
}
