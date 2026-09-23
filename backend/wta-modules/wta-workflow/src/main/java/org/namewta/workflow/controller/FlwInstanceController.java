package org.namewta.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.utils.StreamUtils;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.redis.annotation.RepeatSubmit;
import org.namewta.common.web.core.BaseController;
import org.dromara.warm.flow.core.service.InsService;
import org.namewta.workflow.common.ConditionalOnEnable;
import org.namewta.workflow.domain.bo.FlowCancelBo;
import org.namewta.workflow.domain.bo.FlowInstanceBo;
import org.namewta.workflow.domain.bo.FlowInvalidBo;
import org.namewta.workflow.domain.bo.FlowVariableBo;
import org.namewta.workflow.domain.vo.FlowInstanceVo;
import org.namewta.workflow.service.IFlwInstanceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程实例管理 控制层
 */
@ConditionalOnEnable
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/instance")
public class FlwInstanceController extends BaseController {

    private final InsService insService;
    private final IFlwInstanceService flwInstanceService;

    /**
     * 分页查询正在运行的流程实例。
     *
     * @param flowInstanceBo 流程实例
     * @param pageQuery      分页
     * @return 正在运行的流程实例分页数据
     */
    @GetMapping("/pageByRunning")
    @SaCheckPermission("workflow:instance:list")
    public R<PageResult<FlowInstanceVo>> selectRunningInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery) {
        return R.ok(flwInstanceService.selectRunningInstanceList(flowInstanceBo, pageQuery));
    }

    /**
     * 分页查询已结束的流程实例。
     *
     * @param flowInstanceBo 流程实例
     * @param pageQuery      分页
     * @return 已结束的流程实例分页数据
     */
    @GetMapping("/pageByFinish")
    @SaCheckPermission("workflow:instance:list")
    public R<PageResult<FlowInstanceVo>> selectFinishInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery) {
        return R.ok(flwInstanceService.selectFinishInstanceList(flowInstanceBo, pageQuery));
    }

    /**
     * 根据业务 id 查询流程实例详细信息。
     *
     * @param businessId 业务id
     * @return 流程实例详情
     */
    @GetMapping("/getInfo/{businessId}")
    @SaCheckPermission("workflow:instance:query")
    public R<FlowInstanceVo> getInfo(@PathVariable Long businessId) {
        return R.ok(flwInstanceService.queryByBusinessId(businessId));
    }

    /**
     * 按业务 id 批量删除流程实例。
     *
     * @param businessIds 业务id
     * @return 操作结果
     */
    @PostMapping("/deleteByBusinessIds/{businessIds}")
    @Log(title = "流程实例管理", businessType = BusinessType.DELETE, isSaveRequestData = false, isSaveResponseData = false)
    @SaCheckPermission("workflow:instance:remove")
    public R<Void> deleteByBusinessIds(@PathVariable List<Long> businessIds) {
        return toAjax(flwInstanceService.deleteByBusinessIds(StreamUtils.toList(businessIds, Convert::toStr)));
    }

    /**
     * 按实例 id 批量删除流程实例。
     *
     * @param instanceIds 实例id
     * @return 操作结果
     */
    @PostMapping("/deleteByInstanceIds/{instanceIds}")
    @Log(title = "流程实例管理", businessType = BusinessType.DELETE, isSaveRequestData = false, isSaveResponseData = false)
    @SaCheckPermission("workflow:instance:remove")
    public R<Void> deleteByInstanceIds(@PathVariable List<Long> instanceIds) {
        return toAjax(flwInstanceService.deleteByInstanceIds(instanceIds));
    }

    /**
     * 按实例 id 批量删除已完成的流程实例。
     *
     * @param instanceIds 实例id
     * @return 操作结果
     */
    @PostMapping("/deleteHisByInstanceIds/{instanceIds}")
    @Log(title = "流程实例管理", businessType = BusinessType.DELETE, isSaveRequestData = false, isSaveResponseData = false)
    @SaCheckPermission("workflow:instance:remove")
    public R<Void> deleteHisByInstanceIds(@PathVariable List<Long> instanceIds) {
        return toAjax(flwInstanceService.deleteHisByInstanceIds(instanceIds));
    }

    /**
     * 撤销当前申请人发起的流程。
     *
     * @param bo 参数
     * @return 操作结果
     */
    @RepeatSubmit()
    @PostMapping("/cancelProcessApply")
    @Log(title = "流程实例管理", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @SaCheckPermission("workflow:instance:cancel")
    public R<Void> cancelProcessApply(@RequestBody FlowCancelBo bo) {
        return toAjax(flwInstanceService.cancelProcessApply(bo));
    }

    /**
     * 激活或挂起流程实例。
     *
     * @param id     流程实例id
     * @param active 激活/挂起
     * @return 处理结果
     */
    @RepeatSubmit()
    @PostMapping("/active/{id}")
    @Log(title = "流程实例管理", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @SaCheckPermission("workflow:instance:active")
    public R<Boolean> active(@PathVariable Long id, @RequestParam boolean active) {
        return R.ok(active ? insService.active(id) : insService.unActive(id));
    }

    /**
     * 获取当前登录人发起的流程实例列表。
     *
     * @param flowInstanceBo 参数
     * @param pageQuery      分页
     * @return 当前用户发起的流程实例分页数据
     */
    @GetMapping("/pageByCurrent")
    @SaCheckPermission("workflow:instance:currentList")
    public R<PageResult<FlowInstanceVo>> selectCurrentInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery) {
        return R.ok(flwInstanceService.selectCurrentInstanceList(flowInstanceBo, pageQuery));
    }

    /**
     * 获取流程图和流程记录，用于展示实例流转轨迹。
     *
     * @param businessId 业务id
     * @return 流程图与历史节点信息
     */
    @GetMapping("/flowHisTaskList/{businessId}")
    @SaCheckPermission("workflow:instance:query")
    public R<Map<String, Object>> flowHisTaskList(@PathVariable String businessId) {
        return R.ok(flwInstanceService.flowHisTaskList(businessId));
    }

    /**
     * 获取流程变量。
     *
     * @param instanceId 流程实例id
     * @return 流程变量
     */
    @GetMapping("/instanceVariable/{instanceId}")
    @SaCheckPermission("workflow:instance:variableQuery")
    public R<Map<String, Object>> instanceVariable(@PathVariable Long instanceId) {
        return R.ok(flwInstanceService.instanceVariable(instanceId));
    }

    /**
     * 修改流程变量。
     *
     * @param bo 参数
     * @return 操作结果
     */
    @RepeatSubmit()
    @PostMapping("/updateVariable")
    @Log(title = "流程实例管理", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @SaCheckPermission("workflow:instance:variable")
    public R<Void> updateVariable(@Validated @RequestBody FlowVariableBo bo) {
        return toAjax(flwInstanceService.updateVariable(bo));
    }

    /**
     * 作废流程实例。
     *
     * @param bo 参数
     * @return 处理结果
     */
    @Log(title = "流程实例管理", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @RepeatSubmit()
    @PostMapping("/invalid")
    @SaCheckPermission("workflow:instance:invalid")
    public R<Boolean> invalid(@Validated @RequestBody FlowInvalidBo bo) {
        return R.ok(flwInstanceService.processInvalid(bo));
    }

}
