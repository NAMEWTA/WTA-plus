import { describe, expect, it } from 'vitest';
import { createAccessEvaluator } from './index';

describe('access evaluator', () => {
  it.each([
    { roles: ['superadmin'], expected: true },
    { roles: ['admin'], expected: true },
    { roles: ['operator'], expected: false },
    { roles: [], expected: false },
    { roles: [''], expected: false },
    { roles: undefined, expected: false }
  ])('applies the canonical superadmin and explicit legacy alias: $roles', ({ roles, expected }) => {
    const evaluator = createAccessEvaluator({ permissions: [], roles });
    expect(evaluator.hasRole('system-admin')).toBe(expected);
  });

  it.each([
    { permissions: ['*:*:*'], expected: true },
    { permissions: ['system:user:list'], expected: true },
    { permissions: ['system:user:edit'], expected: false },
    { permissions: [''], expected: false },
    { permissions: 'system:user:list', expected: false },
    { permissions: undefined, expected: false }
  ])('evaluates permission wildcard and fails closed on malformed snapshots', ({ permissions, expected }) => {
    const evaluator = createAccessEvaluator({ permissions, roles: [] });
    expect(evaluator.hasPermission('system:user:list')).toBe(expected);
  });

  it('requires non-empty requested permission and role sets', () => {
    const evaluator = createAccessEvaluator({ permissions: ['system:user:list'], roles: ['operator'] });
    expect(evaluator.hasAnyPermission([])).toBe(false);
    expect(evaluator.hasAllPermissions([])).toBe(false);
    expect(evaluator.hasAnyRole([])).toBe(false);
    expect(evaluator.hasAllRoles([])).toBe(false);
    expect(evaluator.hasAllPermissions(['system:user:list', 'system:user:edit'])).toBe(false);
    expect(evaluator.hasAllRoles(['operator', 'auditor'])).toBe(false);
  });
});
