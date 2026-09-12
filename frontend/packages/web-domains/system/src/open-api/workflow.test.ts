import type {
  OpenApiCatalogItem,
  OpenApiCredentialIssued,
  OpenApiCredentialSummary,
  OpenApiService
} from '@namewta/domain-system';
import { describe, expect, it, vi } from 'vitest';
import {
  createOpenApiWorkspaceController,
  createOpenApiWorkspaceState,
  type OpenApiWorkspaceRuntime
} from './workflow';

const summary: OpenApiCredentialSummary = {
  credentialId: '9',
  ownerUserId: '41',
  appKey: 'app-key',
  appName: 'Operations',
  status: '0',
  expiresAt: null,
  remark: null,
  createTime: '2026-08-31T10:00:00',
  updateTime: '2026-08-31T10:00:00'
};
const issued: OpenApiCredentialIssued = { ...summary, appSecret: 'one-time-secret' };
const catalog: OpenApiCatalogItem[] = [
  {
    interfaceId: 'GET:/orders',
    summary: '订单列表',
    method: 'GET',
    path: '/orders',
    accessRule: { permissions: [], roles: [] },
    parameters: [],
    requestSchema: null,
    responseSchema: 'Order[]',
    curlExample: 'curl example',
    javaExample: 'java example'
  }
];

describe('OpenAPI workspace workflow', () => {
  it('binds current-user scope without ever calling target-user APIs', async () => {
    const { runtime, service } = fixture();
    const state = createOpenApiWorkspaceState();
    const controller = createOpenApiWorkspaceController(runtime, state, { kind: 'current-user' });
    await controller.load();
    await controller.create({ appName: ' Operations ' });
    expect(service.currentUser.getCredential).toHaveBeenCalledOnce();
    expect(service.currentUser.listInterfaces).toHaveBeenCalledOnce();
    expect(service.currentUser.createCredential).toHaveBeenCalledWith({ appName: ' Operations ' });
    expect(service.targetUser.getCredential).not.toHaveBeenCalled();
    expect(state.credential).toEqual(summary);
    expect(state.catalog).toEqual(catalog);
    expect(state.issued).toEqual(issued);
  });

  it('binds every target-user operation to the explicit selected user', async () => {
    const { runtime, service } = fixture();
    const state = createOpenApiWorkspaceState();
    const controller = createOpenApiWorkspaceController(runtime, state, {
      kind: 'target-user',
      userId: '41',
      userLabel: 'demo / Demo'
    });
    await controller.load();
    await controller.reset();
    await controller.setEnabled(false);
    await controller.deleteCredential();
    expect(service.targetUser.getCredential).toHaveBeenCalledWith('41');
    expect(service.targetUser.listInterfaces).toHaveBeenCalledWith('41');
    expect(service.targetUser.resetCredential).toHaveBeenCalledWith('41');
    expect(service.targetUser.disableCredential).toHaveBeenCalledWith('41');
    expect(service.targetUser.deleteCredential).toHaveBeenCalledWith('41');
    expect(state.scopeLabel).toBe('demo / Demo');
  });

  it('treats a missing credential as empty only when the catalog endpoint is available', async () => {
    const { runtime, service } = fixture();
    vi.mocked(service.currentUser.getCredential).mockRejectedValueOnce({ status: 404 });
    const state = createOpenApiWorkspaceState();
    const controller = createOpenApiWorkspaceController(runtime, state, { kind: 'current-user' });
    await controller.load();
    expect(state.credential).toBeNull();
    expect(state.error).toBeNull();
    vi.mocked(service.currentUser.getCredential).mockRejectedValueOnce({ status: 404 });
    vi.mocked(service.currentUser.listInterfaces).mockRejectedValueOnce({ response: { status: 404 } });
    await controller.load();
    expect(state.error).toBe('disabled');
  });

  it('discards a one-time secret on close and reports clipboard failures visibly', async () => {
    const { runtime } = fixture();
    runtime.copyText = vi.fn(async () => Promise.reject(new Error('denied')));
    const state = createOpenApiWorkspaceState();
    const controller = createOpenApiWorkspaceController(runtime, state, { kind: 'current-user' });
    await controller.create({ appName: 'Operations' });
    await controller.copyIssuedSecret();
    expect(state.copyError).toBe('复制失败，请手动复制');
    expect(runtime.error).toHaveBeenCalledWith('复制失败，请手动复制');
    controller.dismissIssuedSecret();
    expect(state.issued).toBeNull();
    expect(state.copyError).toBe('');
  });

  it('fails closed before target commands when the viewer lacks the required permission', async () => {
    const { runtime, service } = fixture();
    runtime.hasPermission = vi.fn(permission => permission === 'system:openApi:list');
    const state = createOpenApiWorkspaceState();
    const controller = createOpenApiWorkspaceController(runtime, state, {
      kind: 'target-user',
      userId: '41',
      userLabel: 'demo'
    });
    await controller.create({ appName: 'Operations' });
    expect(service.targetUser.createCredential).not.toHaveBeenCalled();
    expect(state.error).toBe('forbidden');
  });

  it('keeps conflict and forbidden failures distinguishable for recovery UI', async () => {
    const { runtime, service } = fixture();
    const state = createOpenApiWorkspaceState();
    const controller = createOpenApiWorkspaceController(runtime, state, { kind: 'current-user' });
    vi.mocked(service.currentUser.resetCredential).mockRejectedValueOnce({ code: 409 });
    await controller.reset();
    expect(state.error).toBe('conflict');
    vi.mocked(service.currentUser.listInterfaces).mockRejectedValueOnce({ response: { status: 403 } });
    await controller.load();
    expect(state.error).toBe('forbidden');
  });
});

function fixture(): { runtime: OpenApiWorkspaceRuntime; service: ReturnType<typeof mockService> } {
  const service = mockService();
  return {
    service,
    runtime: {
      openApi: service as unknown as OpenApiService,
      confirm: vi.fn(async () => undefined),
      success: vi.fn(),
      error: vi.fn(),
      hasPermission: vi.fn(() => true),
      copyText: vi.fn(async () => undefined)
    }
  };
}

function response<T>(data: T) {
  return Promise.resolve({ code: 200, data });
}

function mockService() {
  return {
    currentUser: {
      getCredential: vi.fn(() => response(summary)),
      createCredential: vi.fn(() => response(issued)),
      resetCredential: vi.fn(() => response(issued)),
      enableCredential: vi.fn(() => response(summary)),
      disableCredential: vi.fn(() => response({ ...summary, status: '1' as const })),
      deleteCredential: vi.fn(() => response(null)),
      listInterfaces: vi.fn(() => response(catalog)),
      getInterface: vi.fn(() => response(catalog[0]!))
    },
    targetUser: {
      listUsers: vi.fn(() => response([])),
      getCredential: vi.fn(() => response(summary)),
      createCredential: vi.fn(() => response(issued)),
      resetCredential: vi.fn(() => response(issued)),
      enableCredential: vi.fn(() => response(summary)),
      disableCredential: vi.fn(() => response({ ...summary, status: '1' as const })),
      deleteCredential: vi.fn(() => response(null)),
      listInterfaces: vi.fn(() => response(catalog)),
      getInterface: vi.fn(() => response(catalog[0]!))
    }
  };
}
