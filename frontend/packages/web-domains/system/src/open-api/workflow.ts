import type {
  OpenApiCatalogItem,
  OpenApiCredentialCreateInput,
  OpenApiCredentialIssued,
  OpenApiCredentialSummary,
  OpenApiIdentifier,
  OpenApiService
} from '@namewta/domain-system';

export type OpenApiWorkspaceScope =
  | { readonly kind: 'current-user' }
  | { readonly kind: 'target-user'; readonly userId: OpenApiIdentifier; readonly userLabel: string };

export type OpenApiWorkspaceError = 'conflict' | 'disabled' | 'forbidden' | 'unavailable';

export interface OpenApiWorkspaceRuntime {
  openApi: OpenApiService;
  confirm(message: string): Promise<void>;
  success(message: string): void;
  error(message: string): void;
  hasPermission(permission: string): boolean;
  copyText(value: string): Promise<void>;
}

export interface OpenApiWorkspaceState {
  catalog: readonly OpenApiCatalogItem[];
  copyError: string;
  credential: OpenApiCredentialSummary | null;
  error: OpenApiWorkspaceError | null;
  issued: OpenApiCredentialIssued | null;
  loading: boolean;
  scopeLabel: string;
  submitting: boolean;
}

export interface OpenApiWorkspaceController {
  load(): Promise<void>;
  create(input: OpenApiCredentialCreateInput): Promise<void>;
  reset(): Promise<void>;
  setEnabled(enabled: boolean): Promise<void>;
  deleteCredential(): Promise<void>;
  copyIssuedSecret(): Promise<void>;
  dismissIssuedSecret(): void;
  can(action: OpenApiWorkspaceAction): boolean;
}

export type OpenApiWorkspaceAction = 'create' | 'delete' | 'edit' | 'view';

export function createOpenApiWorkspaceState(): OpenApiWorkspaceState {
  return {
    catalog: [],
    copyError: '',
    credential: null,
    error: null,
    issued: null,
    loading: false,
    scopeLabel: '',
    submitting: false
  };
}

export function createOpenApiWorkspaceController(
  runtime: OpenApiWorkspaceRuntime,
  state: OpenApiWorkspaceState,
  scope: OpenApiWorkspaceScope
): OpenApiWorkspaceController {
  const target = scope.kind === 'target-user' ? scope.userId : undefined;
  state.scopeLabel = scope.kind === 'target-user' ? scope.userLabel : '当前用户';
  let generation = 0;

  const permission = (action: OpenApiWorkspaceAction): string => {
    if (scope.kind === 'current-user') return 'system:openApi:self';
    return {
      view: 'system:openApi:query',
      create: 'system:openApi:add',
      edit: 'system:openApi:edit',
      delete: 'system:openApi:remove'
    }[action];
  };
  const can = (action: OpenApiWorkspaceAction) => runtime.hasPermission(permission(action));
  const requireAction = (action: OpenApiWorkspaceAction): boolean => {
    if (can(action)) return true;
    state.error = 'forbidden';
    return false;
  };

  const getCredential = () =>
    scope.kind === 'current-user'
      ? runtime.openApi.currentUser.getCredential()
      : runtime.openApi.targetUser.getCredential(target!);
  const listInterfaces = () =>
    scope.kind === 'current-user'
      ? runtime.openApi.currentUser.listInterfaces()
      : runtime.openApi.targetUser.listInterfaces(target!);
  const createCredential = (input: OpenApiCredentialCreateInput) =>
    scope.kind === 'current-user'
      ? runtime.openApi.currentUser.createCredential(input)
      : runtime.openApi.targetUser.createCredential(target!, input);
  const resetCredential = () =>
    scope.kind === 'current-user'
      ? runtime.openApi.currentUser.resetCredential()
      : runtime.openApi.targetUser.resetCredential(target!);
  const enableCredential = () =>
    scope.kind === 'current-user'
      ? runtime.openApi.currentUser.enableCredential()
      : runtime.openApi.targetUser.enableCredential(target!);
  const disableCredential = () =>
    scope.kind === 'current-user'
      ? runtime.openApi.currentUser.disableCredential()
      : runtime.openApi.targetUser.disableCredential(target!);
  const deleteCredential = () =>
    scope.kind === 'current-user'
      ? runtime.openApi.currentUser.deleteCredential()
      : runtime.openApi.targetUser.deleteCredential(target!);

  const applyIssued = (value: OpenApiCredentialIssued): void => {
    state.issued = value;
    state.copyError = '';
    state.credential = safeSummary(value);
  };
  const command = async (
    action: OpenApiWorkspaceAction,
    execute: () => Promise<{ data: OpenApiCredentialSummary }>,
    success: string
  ): Promise<void> => {
    if (!requireAction(action) || state.submitting) return;
    state.submitting = true;
    state.error = null;
    try {
      state.credential = (await execute()).data;
      runtime.success(success);
    } catch (error) {
      state.error = classifyOpenApiError(error);
    } finally {
      state.submitting = false;
    }
  };

  return {
    can,
    async load() {
      if (!requireAction('view')) return;
      const active = ++generation;
      state.loading = true;
      state.error = null;
      const [credentialResult, catalogResult] = await Promise.allSettled([getCredential(), listInterfaces()]);
      if (active !== generation) return;
      if (catalogResult.status === 'rejected') {
        state.catalog = [];
        state.error = classifyOpenApiError(catalogResult.reason);
      } else {
        state.catalog = catalogResult.value.data;
      }
      if (credentialResult.status === 'fulfilled') {
        state.credential = credentialResult.value.data;
      } else if (errorStatus(credentialResult.reason) === 404 && catalogResult.status === 'fulfilled') {
        state.credential = null;
      } else if (state.error === null) {
        state.error = classifyOpenApiError(credentialResult.reason);
      }
      state.loading = false;
    },
    async create(input) {
      if (!requireAction('create') || state.submitting) return;
      state.submitting = true;
      state.error = null;
      try {
        applyIssued((await createCredential(input)).data);
        runtime.success('OpenAPI 凭据已创建');
      } catch (error) {
        state.error = classifyOpenApiError(error);
      } finally {
        state.submitting = false;
      }
    },
    async reset() {
      if (!requireAction('edit') || state.submitting) return;
      state.submitting = true;
      state.error = null;
      try {
        applyIssued((await resetCredential()).data);
        runtime.success('OpenAPI 密钥已重置');
      } catch (error) {
        state.error = classifyOpenApiError(error);
      } finally {
        state.submitting = false;
      }
    },
    setEnabled(enabled) {
      return command('edit', enabled ? enableCredential : disableCredential, enabled ? '凭据已启用' : '凭据已停用');
    },
    async deleteCredential() {
      if (!requireAction('delete') || state.submitting) return;
      state.submitting = true;
      state.error = null;
      try {
        await deleteCredential();
        state.credential = null;
        runtime.success('OpenAPI 凭据已删除');
      } catch (error) {
        state.error = classifyOpenApiError(error);
      } finally {
        state.submitting = false;
      }
    },
    async copyIssuedSecret() {
      if (!state.issued) return;
      state.copyError = '';
      try {
        await runtime.copyText(state.issued.appSecret);
        runtime.success('密钥已复制');
      } catch {
        state.copyError = '复制失败，请手动复制';
        runtime.error(state.copyError);
      }
    },
    dismissIssuedSecret() {
      state.issued = null;
      state.copyError = '';
    }
  };
}

function safeSummary(issued: OpenApiCredentialIssued): OpenApiCredentialSummary {
  const { appSecret: _discarded, ...summary } = issued;
  return summary;
}

function classifyOpenApiError(error: unknown): OpenApiWorkspaceError {
  const status = errorStatus(error);
  if (status === 403) return 'forbidden';
  if (status === 404) return 'disabled';
  if (status === 409) return 'conflict';
  return 'unavailable';
}

function errorStatus(error: unknown): number | undefined {
  if (!error || typeof error !== 'object') return undefined;
  const source = error as {
    code?: unknown;
    status?: unknown;
    response?: { data?: { code?: unknown }; status?: unknown };
  };
  for (const value of [source.status, source.code, source.response?.status, source.response?.data?.code]) {
    const status = Number(value);
    if (Number.isInteger(status) && status >= 400 && status < 600) return status;
  }
  return undefined;
}
