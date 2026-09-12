import type { MaterialNode } from '@namewta/domain-profile';
import { describe, expect, it } from 'vitest';
import {
  allowedChildTypes,
  canChangeMaterialLifecycle,
  canEditMaterialCode,
  countMaterialNodes,
  executeMaterialCommand
} from './logic';

const node = (overrides: Partial<MaterialNode> = {}): MaterialNode => ({
  materialNodeId: 1,
  parentId: 0,
  nodeType: 'CATEGORY',
  nodeDepth: 1,
  scope: 'PERSON',
  materialTagCode: null,
  nodeName: '个人材料',
  systemRequired: false,
  enabled: true,
  orderNum: 1,
  version: 1,
  children: [],
  ...overrides
});

describe('material tag tree constraints', () => {
  it('allows categories only below the fixed first level and tags at either category level', () => {
    expect(allowedChildTypes(node())).toEqual(['CATEGORY', 'TAG']);
    expect(allowedChildTypes(node({ nodeDepth: 2 }))).toEqual(['TAG']);
    expect(allowedChildTypes(node({ nodeType: 'TAG', nodeDepth: 3 }))).toEqual([]);
  });

  it('locks system-required tag codes and all lifecycle actions', () => {
    const required = node({ nodeType: 'TAG', nodeDepth: 3, materialTagCode: 'PERSON_ID', systemRequired: true });
    expect(canEditMaterialCode(required)).toBe(false);
    expect(canChangeMaterialLifecycle(required)).toBe(false);
    expect(canEditMaterialCode(node({ nodeType: 'TAG', materialTagCode: 'OPTIONAL' }))).toBe(true);
  });

  it('counts nested server trees without rebuilding their ownership', () => {
    expect(countMaterialNodes([node({ children: [node({ materialNodeId: 2, nodeDepth: 2 })] })])).toBe(2);
  });

  it('keeps the current tree on a backend reference conflict', async () => {
    const events: string[] = [];
    const conflict = new Error('节点已被材料引用');
    const succeeded = await executeMaterialCommand(
      async () => Promise.reject(conflict),
      () => events.push('reload'),
      error => events.push((error as Error).message)
    );

    expect(succeeded).toBe(false);
    expect(events).toEqual(['节点已被材料引用']);
  });
});
