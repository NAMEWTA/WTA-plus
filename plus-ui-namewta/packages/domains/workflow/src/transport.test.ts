import { describe, expect, it } from 'vitest';
import { projectWorkflowTaskTransport, type WorkflowTaskTransport } from './transport';

describe('workflow OpenAPI transport boundary', () => {
  it('converts every non-isomorphic transport field without leaking generated-only fields', () => {
    const transport: WorkflowTaskTransport = {
      id: 1,
      instanceId: 2,
      definitionId: 3,
      flowCode: 'leave',
      nodeCode: 'approve',
      createTime: '2026-08-27T01:02:03.000Z',
      updateTime: 'invalid-date',
      buttonList: [{ code: 'approve', value: 'generated-only', show: true }],
      copyList: [{ userId: 7, nickName: 'Owner' }],
      varList: { level: '2' },
      delFlag: '0',
      permissionList: ['workflow:task:complete']
    };
    const projected = projectWorkflowTaskTransport(transport);

    expect(projected).toMatchObject({
      id: 1,
      instanceId: '2',
      definitionId: '3',
      flowCode: 'leave',
      buttonList: [{ code: 'approve', show: true }],
      copyList: [{ userId: 7, nickName: 'Owner' }]
    });
    expect(projected.createTime).toEqual(new Date('2026-08-27T01:02:03.000Z'));
    expect(projected.updateTime).toBeUndefined();
    expect(projected.varList).toEqual(new Map([['level', '2']]));
    expect(projected).not.toHaveProperty('delFlag');
    expect(projected).not.toHaveProperty('permissionList');
    expect(projected.buttonList?.[0]).not.toHaveProperty('value');
  });
});
