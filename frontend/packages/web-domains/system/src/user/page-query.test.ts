import { createSystemService } from '@namewta/domain-system';
import { createRenderer, nextTick, ref, ssrContextKey, type Component, type ComponentOptions } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import UserPage from './UserPage.vue';
import RolePage from '../role/RolePage.vue';
import MenuPage from '../menu/MenuPage.vue';
import type { SystemWebRuntime } from '../runtime';

vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }));
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }));

type HttpRequest = Parameters<Parameters<typeof createSystemService>[0]['request']>[0];
type QueryRow = { userId?: string; roleId?: string; menuId?: string; parentId?: string; label: string };
type PageState = {
  getList(): unknown;
  queryParams: Record<string, unknown>;
  loading: boolean;
  queryError?: string;
  userList?: QueryRow[];
  roleList?: QueryRow[];
  menuList?: QueryRow[];
  total?: number;
  form: Record<string, unknown>;
  selectedButtonPermissionIds?: string[];
  withLoading?<T>(task: () => Promise<T>): Promise<T>;
  editing?: boolean;
};
type HostNode = { parent?: HostNode; children: HostNode[] };
const node = (): HostNode => ({ children: [] });
const renderer = createRenderer<HostNode, HostNode>({
  createElement: node, createText: node, createComment: node,
  setText: () => {}, setElementText: () => {}, patchProp: () => {},
  insert: (child, parent) => { child.parent = parent; parent.children.push(child); },
  remove: child => { if (child.parent) child.parent.children = child.parent.children.filter(item => item !== child); },
  parentNode: child => child.parent ?? null, nextSibling: () => null
});
const cleanups: (() => void)[] = [];
afterEach(() => cleanups.splice(0).forEach(cleanup => cleanup()));
function deferred<T>() {
  let resolve!: (value: T) => void; let reject!: (reason: Error) => void;
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}
const pages = [
  { name: 'User', component: UserPage, endpoint: '/system/user/list', list: 'userList' },
  { name: 'Role', component: RolePage, endpoint: '/system/role/list', list: 'roleList' },
  { name: 'Menu', component: MenuPage, endpoint: '/system/menu/list', list: 'menuList' }
] as const;
async function fixture(page: typeof pages[number]) {
  const response = (label: string) => ({ data: page.name === 'Menu'
    ? [{ menuId: label, parentId: '0', label }] : { rows: [{ userId: label, roleId: label, label }], total: 1 } });
  const pending = new Map<string, ReturnType<typeof deferred<ReturnType<typeof response>>>>();
  const requests: unknown[] = [];
  const service = createSystemService({ request: async <T>(request: HttpRequest): Promise<T> => {
    if (request.url === page.endpoint) {
      const query = request.params as Record<string, unknown>; requests.push(query);
      const key = query.testFilter as string;
      if (pending.has(key)) return await pending.get(key)!.promise as T;
      return response('initial') as T;
    }
    if (request.url === '/system/client/optionSelect') return { data: [] } as T;
    return { data: [] } as T;
  } });
  const runtime = {
    service, dicts: (...types: string[]) => Object.fromEntries(types.map(type => [type, ref([])])),
    treePanel: {} as Component, currentUserId: () => 'owner', hasPermission: () => true,
    confirm: vi.fn(), success: vi.fn(), error: vi.fn(), warning: vi.fn(), download: vi.fn(),
    passwordPolicy: { load: vi.fn(), validate: () => [] }, importUsers: vi.fn()
  } as unknown as SystemWebRuntime;
  // Mount the real SFC setup/lifecycle with a no-DOM renderer; UI remains covered by App E2E.
  const app = renderer.createApp({ ...(page.component as unknown as ComponentOptions), render: () => null }, { runtime });
  app.provide(ssrContextKey, { modules: new Set<string>() });
  const instance = app.mount(node()); let mounted = true;
  const unmount = () => { if (mounted) { mounted = false; app.unmount(); } };
  cleanups.push(unmount);
  const state = Reflect.get(instance.$, 'setupState') as PageState;
  await nextTick(); await Promise.resolve(); await Promise.resolve();
  const start = (key: string, clientId = 'client-1') => {
    const gate = deferred<ReturnType<typeof response>>(); pending.set(key, gate);
    Object.assign(state.queryParams, { clientId, testFilter: key });
    state.getList();
    return { ...gate, finish: () => gate.resolve(response(key)), empty: () => gate.resolve({ data: page.name === 'Menu' ? [] : { rows: [], total: 0 } }) };
  };
  const rows = () => state[page.list];
  return { state, start, rows, requests, unmount };
}

describe.each(pages)('$name page query ownership', page => {
  it('keeps the newer filter result when the older response arrives last', async () => {
    const { start, rows } = await fixture(page);
    const a = start('A'); const b = start('B');
    b.finish(); await vi.waitFor(() => expect(rows()?.[0]?.label).toBe('B'));
    a.finish(); await a.promise; await new Promise<void>(resolve => setImmediate(resolve));
    expect(rows()?.[0]?.label).toBe('B');
  });
  it('keeps loading while the current filter is pending after an old request finishes', async () => {
    const { state, start } = await fixture(page);
    const a = start('A'); const b = start('B');
    a.finish(); await a.promise; await new Promise<void>(resolve => setImmediate(resolve));
    expect(state.loading).toBe(true);
    b.finish(); await vi.waitFor(() => expect(state.loading).toBe(false));
  });
  it('ignores an old failure without clearing the current loading state or showing its error', async () => {
    const { state, start, rows } = await fixture(page);
    const a = start('A'); const b = start('B');
    a.reject(new Error('obsolete failure')); await new Promise<void>(resolve => setImmediate(resolve));
    expect(state.loading).toBe(true); expect(state.queryError).toBe('');
    b.finish(); await vi.waitFor(() => expect(rows()?.[0]?.label).toBe('B'));
  });
  it('settles a current failure and can retry to an empty result without unhandled rejection', async () => {
    const { state, start, rows } = await fixture(page);
    const failed = start('failed'); failed.reject(new Error('current failure'));
    await vi.waitFor(() => expect(state.queryError).toBe('current failure'));
    expect(state.loading).toBe(false);
    const retry = start('retry'); expect(state.queryError).toBe(''); retry.empty();
    await vi.waitFor(() => expect(state.loading).toBe(false));
    expect(rows()).toEqual([]);
    if (page.name !== 'Menu') expect(state.total).toBe(0);
  });
  it('snapshots nested filters, Client, sorting and paging before later edits', async () => {
    const { state, start, requests } = await fixture(page);
    Object.assign(state.queryParams, { pageNum: 3, pageSize: 20, orderByColumn: 'createTime', isAsc: 'desc', params: { extra: { value: 'before' } } });
    const a = start('A');
    Object.assign(state.queryParams, { clientId: 'client-2', pageNum: 1 });
    (state.queryParams.params as { extra: { value: string } }).extra.value = 'after';
    expect(requests.at(-1)).toMatchObject({ testFilter: 'A', clientId: 'client-1', pageNum: 3, pageSize: 20, orderByColumn: 'createTime', isAsc: 'desc', params: { extra: { value: 'before' } } });
    a.finish(); await a.promise;
  });
  it('does not apply a response after component unmount', async () => {
    const { state, start, rows, unmount } = await fixture(page);
    const request = start('late'); const prior = rows(); unmount(); request.finish();
    await request.promise; await new Promise<void>(resolve => setImmediate(resolve));
    expect(rows()).toBe(prior); expect(state.loading).toBe(false); expect(state.queryError).toBe('');
  });
  it('does not reset the independent edit draft or permission selection when a query completes', async () => {
    const { state, start, rows } = await fixture(page);
    state.form.remark = 'unsaved edit';
    if (page.name === 'Role') state.selectedButtonPermissionIds = ['permission-1'];
    const request = start('new'); request.finish();
    await vi.waitFor(() => expect(rows()?.[0]?.label).toBe('new'));
    expect(state.form.remark).toBe('unsaved edit');
    if (page.name === 'Role') expect(state.selectedButtonPermissionIds).toEqual(['permission-1']);
  });
});

describe.each(pages.filter(page => page.name !== 'User'))('$name Client reset', page => {
  it('clears the previous Client rows before a new Client query, including a failed query', async () => {
    const { state, start, rows } = await fixture(page);
    const first = start('first'); first.finish();
    await vi.waitFor(() => expect(rows()?.[0]?.label).toBe('first'));
    const second = start('second', 'client-2'); expect(rows()).toEqual([]);
    second.reject(new Error('client-2 unavailable'));
    await vi.waitFor(() => expect(state.queryError).toBe('client-2 unavailable'));
    expect(rows()).toEqual([]);
  });
  it('invalidates the previous Client request immediately when the filter is cleared', async () => {
    const { state, start, rows } = await fixture(page);
    const request = start('old-client'); state.queryParams.clientId = undefined; state.getList();
    expect(state.loading).toBe(false); expect(rows()).toEqual([]);
    request.finish(); await request.promise; await new Promise<void>(resolve => setImmediate(resolve));
    expect(rows()).toEqual([]);
  });
});

it('User edit completion cannot settle the list query loading state', async () => {
  const { state, start } = await fixture(pages[0]);
  const editing = deferred<void>(); const edit = state.withLoading!(() => editing.promise);
  const query = start('pending-list'); expect(state.editing).toBe(true);
  editing.resolve(); await edit;
  expect(state.editing).toBe(false); expect(state.loading).toBe(true);
  query.finish(); await vi.waitFor(() => expect(state.loading).toBe(false));
});
