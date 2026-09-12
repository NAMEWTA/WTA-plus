import type { OpenApiSchema } from '@namewta/api-contracts';
import type { WorkflowTask } from './index';

export type WorkflowTaskTransport = OpenApiSchema<'FlowTaskVo'>;

const projectDate = (value: string | undefined): Date | undefined => {
  if (!value) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? undefined : date;
};

export function projectWorkflowTaskTransport(value: WorkflowTaskTransport): WorkflowTask {
  return {
    applyNode: value.applyNode,
    businessCode: value.businessCode ?? '',
    businessId: value.businessId ?? '',
    businessTitle: value.businessTitle ?? '',
    buttonList: value.buttonList?.map(button => ({ code: button.code ?? '', show: button.show === true })),
    copyList: value.copyList?.map(copy => ({ nickName: copy.nickName ?? '', userId: copy.userId ?? '' })),
    createTime: projectDate(value.createTime),
    definitionId: value.definitionId === undefined ? undefined : String(value.definitionId),
    flowCode: value.flowCode ?? '',
    flowName: value.flowName ?? '',
    flowStatus: value.flowStatus ?? '',
    formCustom: value.formCustom ?? '',
    formPath: value.formPath ?? '',
    id: value.id ?? '',
    instanceId: value.instanceId === undefined ? '' : String(value.instanceId),
    nodeCode: value.nodeCode ?? '',
    nodeName: value.nodeName ?? '',
    nodeRatio: value.nodeRatio ?? '',
    nodeType: value.nodeType ?? 0,
    updateTime: projectDate(value.updateTime),
    varList: value.varList ? new Map(Object.entries(value.varList)) : undefined,
    version: value.version
  };
}
