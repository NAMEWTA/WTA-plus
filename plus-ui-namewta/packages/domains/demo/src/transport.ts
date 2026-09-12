import type { OpenApiSchema } from '@namewta/api-contracts';
import type { DemoVO } from './index';

export type DemoTransport = OpenApiSchema<'TestDemoVo'>;

export function projectDemoTransport(value: DemoTransport): DemoVO {
  return {
    id: value.id ?? '',
    deptId: value.deptId ?? '',
    userId: value.userId ?? '',
    orderNum: value.orderNum ?? 0,
    testKey: value.testKey ?? '',
    value: value.value ?? ''
  };
}
