import { describe, expect, it } from 'vitest';
import { projectDemoTransport, type DemoTransport } from './transport';

describe('demo OpenAPI transport boundary', () => {
  it('projects generated transport into the stable domain model', () => {
    const transport: DemoTransport = { id: 7, deptId: 8, userId: 9, orderNum: 1, testKey: 'key', value: 'value' };
    expect(projectDemoTransport(transport)).toEqual(transport);
  });
});
