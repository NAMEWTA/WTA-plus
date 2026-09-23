import { afterEach, describe, expect, it, vi } from 'vitest';
import { reactive, ref } from 'vue';
import { adminProfileWebRuntime, adminSystemWebRuntime, resolveAdminWebRegistration } from './adminManifestRegistry';

const inboxHost = vi.hoisted(() => ({
  user: { token: '', sessionGeneration: 0, userId: '', identityLoaded: false },
  session: undefined as undefined | { snapshot: () => { epoch: number; active: boolean } },
  routePort: undefined as undefined | {
    snapshot: () => Promise<{ active: boolean; messageId: unknown }>;
    subscribe: (handler: (value: { active: boolean; messageId: unknown }) => void) => () => void;
  },
  route: undefined as unknown
}));
vi.mock('@/router', () => ({ default: { get currentRoute() { return inboxHost.route; } } }));
vi.mock('@/store/modules/user', () => ({
  useUserStore: () => inboxHost.user
}));
vi.mock('@namewta/web-domain-notify', async importOriginal => {
  const actual = await importOriginal<typeof import('@namewta/web-domain-notify')>();
  return {
    ...actual,
    createNotifyWebDomain: (runtime: Parameters<typeof actual.createNotifyWebDomain>[0]) => {
      inboxHost.session = runtime.inboxSession;
      inboxHost.routePort = runtime.inboxRoute;
      return actual.createNotifyWebDomain(runtime);
    }
  };
});
vi.mock('@/application/services', () => {
  const createService = () => {
    const service = new Proxy(vi.fn(), {
      get: (_target, property) => (property === 'then' ? undefined : service)
    });
    return service;
  };
  const getClientContext = vi.fn();
  const personEligibleUsers = vi.fn();
  const enterpriseEligibleUsers = vi.fn();
  const completeTask = vi.fn();
  const methods = (names: readonly string[]) => Object.fromEntries(names.map(name => [name, vi.fn()]));
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
  ];
  return {
    demoService: createService(),
    identityAccessService: new Proxy(createService(), {
      get: (target, property) => (property === 'getClientContext' ? getClientContext : Reflect.get(target, property))
    }),
    monitorService: createService(),
    notificationService: createService(),
    notificationDirectory: { searchUsers: vi.fn(), usersByIds: vi.fn(), userTypes: vi.fn() },
    openApiService: createService(),
    profileService: {
      materialTags: methods(['archive', 'changeStatus', 'create', 'tree', 'update']),
      person: { archive: { ...methods(archiveMethods), eligibleUsers: personEligibleUsers } },
      enterprise: { archive: { ...methods(archiveMethods), eligibleUsers: enterpriseEligibleUsers } }
    },
    systemService: createService(),
    thirdService: createService(),
    workflowService: { completeTask }
  };
});
vi.mock('@/application/access', () => ({
  createAdminAccessEvaluator: () => ({ hasPermission: vi.fn(() => false) })
}));

afterEach(() => vi.unstubAllGlobals());

describe('admin selected manifest registry', () => {
  it('owns lazy inbox route snapshots, same-page updates and cancellation before import resolves', async () => {
    const port = inboxHost.routePort;
    expect(port).toBeDefined();
    const route = ref({ path: '/notify/inbox', fullPath: '/notify/inbox?messageId=41',
      query: { messageId: '41' as unknown } });
    inboxHost.route = route;
    const cancelled = vi.fn();
    const stopBeforeImport = port!.subscribe(cancelled);
    stopBeforeImport();
    const snapshots: Array<{ active: boolean; messageId: unknown }> = [];
    const stop = port!.subscribe(value => snapshots.push(value));
    await vi.waitFor(() => expect(snapshots).toEqual([{ active: true, messageId: '41' }]));
    expect(cancelled).not.toHaveBeenCalled();
    expect(await port!.snapshot()).toEqual({ active: true, messageId: '41' });
    route.value = { path: '/notify/inbox', fullPath: '/notify/inbox?messageId=42', query: { messageId: '42' } };
    await vi.waitFor(() => expect(snapshots.at(-1)).toEqual({ active: true, messageId: '42' }));
    route.value = { path: '/notify/notice', fullPath: '/notify/notice?noticeId=9',
      query: { messageId: undefined } };
    await vi.waitFor(() => expect(snapshots.at(-1)).toEqual({ active: false, messageId: undefined }));
    const count = snapshots.length;
    stop();
    route.value = { path: '/notify/inbox', fullPath: '/notify/inbox?messageId=43', query: { messageId: '43' } };
    expect(snapshots).toHaveLength(count);
  });

  it('uses the real Admin inbox host session epoch across pending logout and the next identity', () => {
    const session = inboxHost.session;
    expect(session).toBeDefined();
    inboxHost.user = reactive({
      token: 't41-A-token-canary', sessionGeneration: 10, userId: '101', identityLoaded: true
    });
    const a = session!.snapshot();
    expect(a).toEqual({ epoch: expect.any(Number), active: true });

    // 退出先改变代次；远端请求悬挂时旧 token、userId 和 loaded 仍未清理。
    inboxHost.user.sessionGeneration++;
    const suspended = session!.snapshot();
    expect(suspended.epoch).toBeGreaterThan(a.epoch);
    expect(suspended.active).toBe(false);
    expect(session!.snapshot()).toEqual(suspended);

    inboxHost.user.token = 't41-B-token-canary';
    inboxHost.user.userId = '202';
    inboxHost.user.identityLoaded = false;
    const loading = session!.snapshot();
    expect(loading.epoch).toBeGreaterThan(suspended.epoch);
    expect(loading.active).toBe(false);
    inboxHost.user.identityLoaded = true;
    const b = session!.snapshot();
    expect(b.epoch).toBeGreaterThan(loading.epoch);
    expect(b).toEqual({ epoch: expect.any(Number), active: true });
    expect(JSON.stringify([a, suspended, loading, b])).not.toContain('token-canary');
  });

  it('does not register retired AI pages while keeping unrelated monitor registrations', () => {
    expect(resolveAdminWebRegistration('ai/chat/index', 'ai')).toBeUndefined();
    expect(resolveAdminWebRegistration('monitor/snailai/index', 'system')).toBeUndefined();
    expect(resolveAdminWebRegistration('monitor/snailjob/index', 'system')).toMatchObject({
      componentName: 'SnailJob'
    });
    expect(resolveAdminWebRegistration('monitor/nacos/index', 'system')).toMatchObject({ componentName: 'Nacos' });
  });
  it('adapts the admin identity policy and browser clipboard through explicit system runtime ports', async () => {
    const services = await import('@/application/services');
    vi.mocked(services.identityAccessService.getClientContext).mockResolvedValue({
      clientEnabled: true,
      registerEnabled: true,
      passwordPolicy: {
        minimumLength: 8,
        maximumLength: 20,
        requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'],
        allowedSpecialCharacters: '!@#'
      }
    });
    const writeText = vi.fn(async () => undefined);
    vi.stubGlobal('navigator', { clipboard: { writeText } });

    const policy = await adminSystemWebRuntime.passwordPolicy.load();
    expect(adminSystemWebRuntime.passwordPolicy.validate(policy, 'weak')).not.toEqual([]);
    await adminSystemWebRuntime.copyText('redacted');
    expect(writeText).toHaveBeenCalledWith('redacted');
  });

  it('selects the active admin manifests and excludes unregistered system slices', () => {
    expect(resolveAdminWebRegistration('identity-access/login/index', 'admin')).toMatchObject({
      componentName: 'IdentityLogin'
    });
    expect(resolveAdminWebRegistration('demo/demo/index', 'demo')).toMatchObject({ componentName: 'Demo' });
    expect(resolveAdminWebRegistration('tool/gen/index', 'gen')).toBeUndefined();
    expect(resolveAdminWebRegistration('tool/gen-edit/index', 'gen')).toBeUndefined();
    expect(resolveAdminWebRegistration('tool/openapi/index', 'gen')).toBeUndefined();
    expect(resolveAdminWebRegistration('workflow/category/index', 'workflow')).toMatchObject({
      componentName: 'Category'
    });
    expect(resolveAdminWebRegistration('workflow/processDefinition/index', 'workflow')).toMatchObject({
      componentName: 'processDefinition'
    });
    expect(resolveAdminWebRegistration('workflow/task/index', 'workflow')).toBeUndefined();
    expect(resolveAdminWebRegistration('system/user/index', 'system')).toMatchObject({
      componentName: 'User'
    });
    expect(resolveAdminWebRegistration('system/role/authUser', 'system')).toMatchObject({
      componentName: 'AuthUser'
    });
    expect(resolveAdminWebRegistration('system/oss/index', 'system')).toMatchObject({ componentName: 'Oss' });
    expect(resolveAdminWebRegistration('system/dict/index', 'system')).toMatchObject({ componentName: 'Dict' });
    expect(resolveAdminWebRegistration('system/openApi/index', 'system')).toMatchObject({ componentName: 'OpenApi' });
    expect(resolveAdminWebRegistration('third/provider/index', 'third')).toMatchObject({
      componentName: 'ThirdProvider'
    });
    expect(resolveAdminWebRegistration('system/devtools/index', 'system')).toBeUndefined();
    expect(resolveAdminWebRegistration('ai/model/index', 'ai')).toBeUndefined();
    expect(resolveAdminWebRegistration('monitor/online/index', 'system')).toMatchObject({
      componentName: 'Online'
    });
    expect(resolveAdminWebRegistration('monitor/notify/index', 'system')).toBeUndefined();
    expect(resolveAdminWebRegistration('notify/monitor/index', 'notify')).toMatchObject({
      componentName: 'NotificationMonitor'
    });
    expect(resolveAdminWebRegistration('monitor/admin/index', 'system')).toMatchObject({
      componentName: 'MonitorAdmin'
    });
    expect(resolveAdminWebRegistration('monitor/snailjob/index', 'system')).toMatchObject({
      componentName: 'SnailJob'
    });
    expect(resolveAdminWebRegistration('monitor/nacos/index', 'system')).toMatchObject({ componentName: 'Nacos' });
    expect(resolveAdminWebRegistration('monitor/report/index', 'system')).toBeUndefined();
    expect(resolveAdminWebRegistration('profile/materialTag/index', 'profile')).toMatchObject({
      componentName: 'ProfileMaterialTag'
    });
    expect(resolveAdminWebRegistration('profile/person/index', 'profile')).toMatchObject({
      componentName: 'PersonProfile'
    });
    expect(resolveAdminWebRegistration('profile/person/detail', 'profile')).toMatchObject({
      componentName: 'PersonProfileDetail'
    });
    expect(resolveAdminWebRegistration('profile/person/review', 'profile')).toMatchObject({
      componentName: 'PersonProfileReview'
    });
    expect(resolveAdminWebRegistration('profile/enterprise/index', 'profile')).toMatchObject({
      componentName: 'EnterpriseProfile'
    });
    expect(resolveAdminWebRegistration('profile/enterprise/detail', 'profile')).toMatchObject({
      componentName: 'EnterpriseProfileDetail'
    });
    expect(resolveAdminWebRegistration('profile/enterprise/review', 'profile')).toMatchObject({
      componentName: 'EnterpriseProfileReview'
    });
  });

  it('adapts profile candidate search and workflow completion through closed host ports', async () => {
    const services = await import('@/application/services');
    vi.mocked(services.profileService.person.archive.eligibleUsers).mockResolvedValue({
      data: [{ userId: 7, userName: 'alice', nickName: 'Alice' }]
    });
    vi.mocked(services.profileService.enterprise.archive.eligibleUsers).mockResolvedValue({
      data: [{ userId: 8, userName: 'owner', nickName: '' }]
    });

    await expect(adminProfileWebRuntime.findUsers('PERSON', 'ali')).resolves.toEqual([
      { userId: 7, label: 'Alice (alice)' }
    ]);
    await expect(adminProfileWebRuntime.findUsers('ENTERPRISE', 'own')).resolves.toEqual([
      { userId: 8, label: 'owner (owner)' }
    ]);
    await adminProfileWebRuntime.completeWorkflowTask({
      taskId: 'task-1',
      comment: 'checked',
      variables: { profileDecision: 'APPROVE' }
    });

    expect(services.profileService.person.archive.eligibleUsers).toHaveBeenCalledWith('ali');
    expect(services.profileService.enterprise.archive.eligibleUsers).toHaveBeenCalledWith('own');
    expect(services.workflowService.completeTask).toHaveBeenCalledWith({
      taskId: 'task-1',
      message: 'checked',
      variables: { profileDecision: 'APPROVE' }
    });
  });
});
