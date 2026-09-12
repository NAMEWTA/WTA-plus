import type { OpenApiSchema } from '@namewta/api-contracts';
import type { OperLogVO } from './types';

export type OperationLogTransport = OpenApiSchema<'SysOperLogVo'>;

export function projectOperationLogTransport(value: OperationLogTransport): OperLogVO {
  return {
    browser: value.browser ?? '',
    businessType: value.businessType ?? 0,
    businessTypes: value.businessTypes,
    clientKey: value.clientKey ?? '',
    costTime: value.costTime ?? 0,
    deptId: value.deptId ?? '',
    deptName: value.deptName ?? '',
    deviceType: value.deviceType ?? '',
    errorMsg: value.errorMsg ?? '',
    jsonResult: value.jsonResult ?? '',
    method: value.method ?? '',
    operId: value.operId ?? '',
    operIp: value.operIp ?? '',
    operLocation: value.operLocation ?? '',
    operatorType: value.operatorType ?? 0,
    operName: value.operName ?? '',
    operParam: value.operParam ?? '',
    operTime: value.operTime ?? '',
    operUrl: value.operUrl ?? '',
    os: value.os ?? '',
    requestMethod: value.requestMethod ?? '',
    status: value.status ?? 0,
    tenantId: '',
    title: value.title ?? '',
    userId: value.userId ?? ''
  };
}
