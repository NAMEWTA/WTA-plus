package org.namewta.profile.api.material;

import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialReferenceView;
import java.time.Instant;
import java.util.List;

/** 基于工作流任务授权读取提交材料，不授予档案管理权限。 */
public interface ProfileTaskMaterialPort {
    /** 校验当前或已办任务与提交快照后列出材料。 */
    List<MaterialReferenceView> listForTask(MaterialOwnerKey owner, Long taskId);
    /** 校验任务、提交快照和材料归属后签发访问地址。 */
    MaterialAccessUrl accessUrlForTask(MaterialOwnerKey owner, Long materialRefId, Long taskId);
    /** 已授权的材料地址，不暴露存储模块类型。 */
    record MaterialAccessUrl(String accessType, String url, Instant expiresAt, String fileName) {
    }
}

