import { describe, expect, it } from 'vitest';
import { projectOperationLogTransport, type OperationLogTransport } from './transport';

describe('monitor OpenAPI transport boundary', () => {
  it('projects only domain-owned operation-log fields and supplies required defaults', () => {
    const transport = {
      operId: 4,
      title: 'Update',
      status: 0,
      costTime: 12,
      businessTypes: [1, 2],
      serverOnly: 'must-not-cross'
    } satisfies OperationLogTransport & { serverOnly: string };
    const projected = projectOperationLogTransport(transport);
    expect(projected).toMatchObject({
      operId: 4,
      title: 'Update',
      status: 0,
      costTime: 12,
      businessTypes: [1, 2],
      tenantId: '',
      requestMethod: '',
      operName: ''
    });
    expect(projected).not.toHaveProperty('serverOnly');
  });
});
