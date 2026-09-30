package org.namewta.profile.person.adapter.api;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.api.material.ProfileTaskMaterialPort;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialReferenceView;
import org.namewta.profile.person.usecase.ProfileMaterialUseCase;
import org.springframework.stereotype.Component;
import java.util.List;

/** 为个人、企业审核提供经过任务授权的材料读取入口。 */
@Component
@RequiredArgsConstructor
public class ProfileTaskMaterialAdapter implements ProfileTaskMaterialPort {
    private final ProfileMaterialUseCase useCase;

    /** 委托用例校验任务并列出材料。 */
    @Override
    public List<MaterialReferenceView> listForTask(MaterialOwnerKey owner, Long taskId) {
        return useCase.listForTask(owner, taskId);
    }

    /** 委托用例校验任务和材料引用归属。 */
    @Override
    public MaterialAccessUrl accessUrlForTask(MaterialOwnerKey owner, Long materialRefId, Long taskId) {
        return useCase.accessUrlForTask(owner, materialRefId, taskId);
    }
}
