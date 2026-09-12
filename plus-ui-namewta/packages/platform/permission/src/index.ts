export const SUPERADMIN_ROLE = 'superadmin';
export const LEGACY_ADMIN_ROLE = 'admin';
export const ALL_PERMISSIONS = '*:*:*';

export interface AccessSnapshot {
  permissions?: unknown;
  roles?: unknown;
}

export interface AccessEvaluator {
  hasAllPermissions(required: readonly string[]): boolean;
  hasAllRoles(required: readonly string[]): boolean;
  hasAnyPermission(required: readonly string[]): boolean;
  hasAnyRole(required: readonly string[]): boolean;
  hasPermission(permission: string): boolean;
  hasRole(role: string): boolean;
}

function normalizeSnapshotList(value: unknown): readonly string[] {
  if (!Array.isArray(value) || value.some(item => typeof item !== 'string' || !item.trim())) return [];
  return Object.freeze(value.map(item => item.trim()));
}

function normalizeRequired(values: readonly string[]): readonly string[] {
  if (!Array.isArray(values) || values.length === 0) return [];
  const normalized = values.map(value => (typeof value === 'string' ? value.trim() : ''));
  return normalized.some(value => !value) ? [] : normalized;
}

export function createAccessEvaluator(snapshot: AccessSnapshot): AccessEvaluator {
  const permissions = normalizeSnapshotList(snapshot.permissions);
  const roles = normalizeSnapshotList(snapshot.roles);
  const isSuperAdmin = roles.includes(SUPERADMIN_ROLE) || roles.includes(LEGACY_ADMIN_ROLE);
  const hasPermission = (permission: string) => {
    const required = typeof permission === 'string' ? permission.trim() : '';
    return Boolean(required) && (permissions.includes(ALL_PERMISSIONS) || permissions.includes(required));
  };
  const hasRole = (role: string) => {
    const required = typeof role === 'string' ? role.trim() : '';
    return Boolean(required) && (isSuperAdmin || roles.includes(required));
  };

  return Object.freeze({
    hasPermission,
    hasRole,
    hasAnyPermission(required) {
      const normalized = normalizeRequired(required);
      return normalized.some(hasPermission);
    },
    hasAllPermissions(required) {
      const normalized = normalizeRequired(required);
      return normalized.length > 0 && normalized.every(hasPermission);
    },
    hasAnyRole(required) {
      const normalized = normalizeRequired(required);
      return normalized.some(hasRole);
    },
    hasAllRoles(required) {
      const normalized = normalizeRequired(required);
      return normalized.length > 0 && normalized.every(hasRole);
    }
  });
}
