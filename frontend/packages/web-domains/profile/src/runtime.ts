import type { Identifier, OssAccessUrl, ProfileService, ProfileType } from '@namewta/domain-profile';
import type { Component } from 'vue';

export interface ProfileUserOption {
  label: string;
  userId: Identifier;
}

export interface ProfileWorkflowCommand {
  comment: string;
  taskId: Identifier;
  variables?: Record<string, unknown>;
}

export interface ProfileWebRuntime {
  closeCurrentPage(): Promise<void> | void;
  completeWorkflowTask(command: ProfileWorkflowCommand): Promise<void>;
  confirm(message: string): Promise<void>;
  downloadMaterial(access: OssAccessUrl): Promise<void> | void;
  error(message: string): void;
  fileUpload: Component;
  findUsers(profileType: ProfileType, keyword: string): Promise<readonly ProfileUserOption[]>;
  hasPermission(permission: string): boolean;
  service: ProfileService;
  success(message: string): void;
  warning(message: string): void;
}

const hostMethods = [
  'closeCurrentPage',
  'completeWorkflowTask',
  'confirm',
  'downloadMaterial',
  'error',
  'findUsers',
  'hasPermission',
  'success',
  'warning'
] as const;

const materialTagMethods = ['archive', 'changeStatus', 'create', 'tree', 'update'] as const;
const archiveMethods = [
  'assign',
  'create',
  'decide',
  'detail',
  'manageBinding',
  'material',
  'page',
  'review',
  'reviewMaterial',
  'revise',
  'revoke'
] as const;

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function hasFunctions(value: unknown, names: readonly string[]): boolean {
  return isRecord(value) && names.every(name => typeof value[name] === 'function');
}

export function requireProfileWebRuntime(runtime: ProfileWebRuntime | undefined): ProfileWebRuntime {
  const service = isRecord(runtime) ? runtime.service : undefined;
  const person = isRecord(service) ? service.person : undefined;
  const enterprise = isRecord(service) ? service.enterprise : undefined;
  if (
    !isRecord(runtime) ||
    !runtime.fileUpload ||
    !hasFunctions(runtime, hostMethods) ||
    !isRecord(service) ||
    !hasFunctions(service.materialTags, materialTagMethods) ||
    !isRecord(person) ||
    !hasFunctions(person.archive, archiveMethods) ||
    !isRecord(enterprise) ||
    !hasFunctions(enterprise.archive, archiveMethods)
  ) {
    throw new Error('ProfileWebRuntime is required');
  }
  return runtime;
}
