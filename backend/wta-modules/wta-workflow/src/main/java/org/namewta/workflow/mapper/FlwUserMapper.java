package org.namewta.workflow.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.dromara.warm.flow.orm.entity.*;
import org.namewta.workflow.domain.FlowInstanceBizExt;
import org.namewta.workflow.domain.FlowInstanceNodeClient;
import org.namewta.workflow.domain.bo.FlowTaskBo;
import org.namewta.workflow.domain.vo.FlowTaskVo;
import java.util.List;
import java.util.Map;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.warm.flow.orm.entity.FlowUser;

/**
 * 任务信息Mapper接口
 *
 * @date 2024-03-02
 */
public interface FlwUserMapper extends BaseMapperPlus<FlowUser, FlowUser>, MPJBaseMapper<FlowUser> {

    /**
     * 分页查询抄送任务。
     *
     * @param page        分页对象
     * @param bo          查询条件
     * @param categoryIds 流程分类 id 列表
     * @param userId      当前用户 id
     * @return 抄送任务分页结果
     */
    default Page<FlowTaskVo> getTaskCopyByPage(Page<FlowTaskVo> page, FlowTaskBo bo, List<String> categoryIds, String userId) {
        Map<String, Object> params = bo.getParams();
        return selectJoinPage(page, FlowTaskVo.class, QueryBuilder.lambdaJoin("a", FlowUser.class)
            .select(FlowUser::getProcessedBy, FlowUser::getCreateTime)
            .select("b", FlowHisTask::getId, FlowHisTask::getUpdateTime, FlowHisTask::getFormCustom,
                FlowHisTask::getFormPath, FlowHisTask::getNodeName, FlowHisTask::getNodeCode)
            .select("c", FlowInstance::getBusinessId, FlowInstance::getFlowStatus, FlowInstance::getCreateBy)
            .select("d", FlowDefinition::getFlowName, FlowDefinition::getFlowCode, FlowDefinition::getCategory,
                FlowDefinition::getVersion)
            .select("biz", FlowInstanceBizExt::getBusinessCode, FlowInstanceBizExt::getBusinessTitle)
            .leftJoin(FlowHisTask.class, "b", FlowHisTask::getTaskId, FlowUser::getAssociated)
            .leftJoin(FlowInstance.class, "c", FlowInstance::getId, FlowHisTask::getInstanceId)
            .leftJoin(FlowDefinition.class, "d", FlowDefinition::getId, FlowInstance::getDefinitionId)
            .leftJoin(FlowInstanceBizExt.class, "biz", FlowInstanceBizExt::getInstanceId, FlowInstance::getId)
            .leftJoin(FlowInstanceNodeClient.class, "nc", FlowInstanceNodeClient::getInstanceId, FlowHisTask::getInstanceId)
            .apply(true, "nc.node_code = b.node_code")
            .eq("nc", FlowInstanceNodeClient::getClientPk, params.get("workflowClientPk"))
            .eq("a", FlowUser::getType, "4")
            .likeIfText("b", FlowHisTask::getNodeName, bo.getNodeName())
            .likeIfText("d", FlowDefinition::getFlowName, bo.getFlowName())
            .likeIfText("d", FlowDefinition::getFlowCode, bo.getFlowCode())
            .likeIfText("c", FlowInstance::getFlowStatus, bo.getFlowStatus())
            .inIfNotEmpty("c", FlowInstance::getCreateBy, bo.getCreateByIds())
            .inIfNotEmpty("d", FlowDefinition::getCategory, categoryIds)
            .betweenParams("a", FlowUser::getCreateTime, params, "beginTime", "endTime")
            .eqIfText("a", FlowUser::getProcessedBy, userId)
            .orderByDesc("a", FlowUser::getCreateTime)
            .orderByDesc("b", FlowHisTask::getUpdateTime)
            .build());
    }

}
