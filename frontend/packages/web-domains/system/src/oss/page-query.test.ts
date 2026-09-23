import { createSystemService } from '@namewta/domain-system';
import { createRenderer, ssrContextKey, toRaw, type ComponentOptions } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import OssPage from './OssPage.vue';
import type { SystemWebRuntime } from '../runtime';

vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }));

type HttpRequest = Parameters<Parameters<typeof createSystemService>[0]['request']>[0];
type OssRow = { ossId: string; fileName: string; fileSuffix: string; deleteState: 'ACTIVE' | 'PENDING'; url: string };
type PageState = {
  getList(): Promise<unknown>;
  handleQuery(): void;
  handleOrderChange(prop: string, order: string): void;
  handleDelete(row: OssRow): Promise<void>;
  handlePreviewListResource(enabled: boolean): Promise<void>;
  queryParams: Record<string, unknown>;
  dateRangeCreateTime: [string, string];
  ossList: OssRow[];
  total: number;
  previewListResource: boolean;
  previewUrls: Record<string, string>;
  deletedPreviewIds: Record<string, true>;
  loading: boolean;
  queryError: string;
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
  let resolve!: (value: T) => void;
  let reject!: (reason: Error) => void;
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}
const settle = () => new Promise<void>(resolve => setImmediate(resolve));

const row = (id: string, fileSuffix = '.png', deleteState: OssRow['deleteState'] = 'ACTIVE'): OssRow =>
  ({ ossId: id, fileName: id, fileSuffix, deleteState, url: 'untrusted-list-url' });
const list = (rows: OssRow[], total = rows.length) => ({ data: { rows, total } });
const access = (id: string, url: string) => ({ data: { url, fileName: id, accessType: 'PUBLIC' as const, expiresAt: null } });

async function fixture() {
  const configs: ReturnType<typeof deferred<{ data: string }>>[] = [];
  const lists: Array<{ query: Record<string, unknown>; gate: ReturnType<typeof deferred<ReturnType<typeof list>>> }> = [];
  const previews: Array<{ id: string; gate: ReturnType<typeof deferred<ReturnType<typeof access>>> }> = [];
  const deletes: ReturnType<typeof deferred<{ data: null }>>[] = [];
  let controlled = false;
  const request = async <T>(input: HttpRequest): Promise<T> => {
    if (input.url === '/system/config/configKey/sys.oss.previewListResource') {
      if (!controlled) return { data: 'false' } as T;
      const gate = deferred<{ data: string }>(); configs.push(gate); return await gate.promise as T;
    }
    if (input.url === '/resource/oss/list') {
      if (!controlled) return list([]) as T;
      const gate = deferred<ReturnType<typeof list>>();
      lists.push({ query: structuredClone(toRaw(input.params as Record<string, unknown>)), gate });
      return await gate.promise as T;
    }
    if (input.url.endsWith('/download-url')) {
      const gate = deferred<ReturnType<typeof access>>();
      previews.push({ id: input.url.split('/').at(-2) ?? '', gate });
      return await gate.promise as T;
    }
    if (input.url.startsWith('/resource/oss/') && input.method === 'post') {
      const gate = deferred<{ data: null }>(); deletes.push(gate); return await gate.promise as T;
    }
    return { data: null } as T;
  };
  const runtime = {
    service: createSystemService({ request }),
    imagePreview: { render: () => null },
    downloadOss: vi.fn(), confirm: vi.fn().mockResolvedValue(undefined),
    success: vi.fn(), error: vi.fn()
  } as unknown as SystemWebRuntime;
  const app = renderer.createApp({ ...(OssPage as unknown as ComponentOptions), render: () => null }, { runtime });
  app.provide(ssrContextKey, { modules: new Set<string>() });
  const instance = app.mount(node());
  let mounted = true;
  const unmount = () => { if (mounted) { mounted = false; app.unmount(); } };
  cleanups.push(unmount);
  const state = Reflect.get(instance.$, 'setupState') as PageState;
  await vi.waitFor(() => expect(state.loading).toBe(false));
  controlled = true;
  const start = (filter: string) => {
    state.queryParams.fileName = filter;
    return state.getList();
  };
  const config = (index: number, enabled: boolean) => configs[index].resolve({ data: String(enabled) });
  const finishList = (index: number, rows: OssRow[], total = rows.length) => lists[index].gate.resolve(list(rows, total));
  return { state, start, config, finishList, configs, lists, previews, deletes, unmount, runtime };
}

describe('OSS real SFC query ownership', () => {
  it('keeps B rows, total and preview mode when A list resolves last', async () => {
    const f = await fixture();
    const a = f.start('A'); f.config(0, true); await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    const b = f.start('B'); f.config(1, false); await vi.waitFor(() => expect(f.lists).toHaveLength(2));
    f.finishList(1, [row('B', '.pdf')], 7); await b;
    f.finishList(0, [row('A', '.pdf')], 99); await a;
    expect(f.state.ossList.map(item => item.ossId)).toEqual(['B']);
    expect(f.state.total).toBe(7);
    expect(f.state.previewListResource).toBe(false);
    expect(f.state.loading).toBe(false);
  });

  it('snapshots a nested query and date range before a delayed config resolves', async () => {
    const f = await fixture();
    Object.assign(f.state.queryParams, { pageNum: 3, pageSize: 20, orderByColumn: 'createTime',
      isAsc: 'descending', params: { nested: { value: 'before' } } });
    f.state.dateRangeCreateTime = ['2026-09-01', '2026-09-23'];
    const pending = f.start('before');
    f.state.queryParams.fileName = 'after';
    f.state.queryParams.pageNum = 1;
    (f.state.queryParams.params as { nested: { value: string } }).nested.value = 'after';
    f.state.dateRangeCreateTime = ['2026-10-01', '2026-10-23'];
    f.config(0, false);
    await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    expect(f.lists[0].query).toMatchObject({ fileName: 'before', pageNum: 3, pageSize: 20,
      orderByColumn: 'createTime', isAsc: 'descending',
      params: { nested: { value: 'before' }, beginCreateTime: '2026-09-01', endCreateTime: '2026-09-23' } });
    f.finishList(0, []); await pending;
  });

  it('does not query or apply an obsolete config after B has completed', async () => {
    const f = await fixture();
    const a = f.start('A');
    const b = f.start('B'); f.config(1, false);
    await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    f.finishList(0, [row('B', '.pdf')], 2); await b;
    f.config(0, true); await a;
    expect(f.lists).toHaveLength(1);
    expect(f.state.ossList.map(item => item.ossId)).toEqual(['B']);
    expect(f.state.previewListResource).toBe(false);
    expect(f.state.total).toBe(2);
  });

  it('commits rows before preview and ignores A preview after B commits', async () => {
    const f = await fixture();
    const a = f.start('A'); f.config(0, true);
    await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    f.finishList(0, [row('A')]);
    await vi.waitFor(() => expect(f.previews).toHaveLength(1));
    await a;
    expect(f.state.ossList.map(item => item.ossId)).toEqual(['A']);
    expect(f.state.loading).toBe(false);
    const b = f.start('B'); f.config(1, true);
    await vi.waitFor(() => expect(f.lists).toHaveLength(2));
    f.finishList(1, [row('B'), row('pending', '.png', 'PENDING')], 6);
    await vi.waitFor(() => expect(f.previews).toHaveLength(2));
    expect(f.previews.map(item => item.id)).toEqual(['A', 'B']);
    await b;
    expect(f.state.ossList.map(item => item.ossId)).toEqual(['B', 'pending']);
    expect(f.state.total).toBe(6);
    expect(f.state.previewUrls).toEqual({});
    f.previews[1].gate.reject(new Error('该文件已删除'));
    await vi.waitFor(() => expect(f.state.deletedPreviewIds).toEqual({ B: true }));
    f.previews[0].gate.resolve(access('A', 'https://example.test/authorized-A')); await settle();
    expect(f.state.previewUrls).toEqual({});
    expect(f.state.deletedPreviewIds).toEqual({ B: true });
    expect(f.state.loading).toBe(false);
  });

  it('ignores old failures without clearing B loading or error after B success', async () => {
    const f = await fixture();
    const a = f.start('A'); f.config(0, true);
    await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    const b = f.start('B'); f.config(1, false);
    await vi.waitFor(() => expect(f.lists).toHaveLength(2));
    f.lists[0].gate.reject(new Error('obsolete list failure')); await a;
    expect(f.state.loading).toBe(true);
    expect(f.state.queryError).toBe('');
    f.finishList(1, [row('B', '.pdf')]); await b;
    const oldConfig = f.start('older config');
    const latest = f.start('latest'); f.config(3, false);
    await vi.waitFor(() => expect(f.lists).toHaveLength(3));
    f.finishList(2, [row('latest', '.pdf')]); await latest;
    f.configs[2].reject(new Error('obsolete config failure')); await oldConfig;
    expect(f.state.ossList.map(item => item.ossId)).toEqual(['latest']);
    expect(f.state.queryError).toBe('');
    expect(f.state.loading).toBe(false);
  });

  it('shows current failure, then clears it on an empty successful retry', async () => {
    const f = await fixture();
    const failed = f.start('failed'); f.configs[0].reject(new Error('配置读取失败')); await failed;
    expect(f.state.queryError).toBe('配置读取失败');
    expect(f.state.loading).toBe(false);
    const retry = f.start('retry');
    expect(f.state.queryError).toBe('');
    f.config(1, true); await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    f.finishList(0, [], 0); await retry;
    expect(f.state.ossList).toEqual([]);
    expect(f.state.total).toBe(0);
    expect(f.state.queryError).toBe('');
    expect(f.state.loading).toBe(false);
  });

  it('does not report a preview-setting change as fully refreshed when its current list fails', async () => {
    const f = await fixture();
    const changing = f.state.handlePreviewListResource(true);
    await vi.waitFor(() => expect(f.configs).toHaveLength(1));
    f.config(0, true);
    await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    f.lists[0].gate.reject(new Error('刷新失败')); await changing;
    expect(f.state.queryError).toBe('刷新失败');
    expect(f.runtime.success).not.toHaveBeenCalled();
    expect(f.state.loading).toBe(false);
  });

  it.each(['config', 'list', 'preview'] as const)('does not backfill after unmount during %s', async stage => {
    const f = await fixture();
    const pending = f.start(stage);
    if (stage !== 'config') {
      f.config(0, true); await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    }
    if (stage === 'preview') {
      f.finishList(0, [row('late')]);
      await vi.waitFor(() => expect(f.previews).toHaveLength(1));
      await pending;
    }
    const priorRows = f.state.ossList;
    const priorTotal = f.state.total;
    f.unmount();
    if (stage === 'config') f.config(0, true);
    if (stage === 'list') f.finishList(0, [row('late', '.pdf')]);
    if (stage === 'preview') f.previews[0].gate.resolve(access('late', 'https://example.test/authorized-late'));
    await pending;
    await settle();
    expect(f.state.ossList).toBe(priorRows);
    expect(f.state.total).toBe(priorTotal);
    expect(f.state.previewUrls).toEqual({});
    expect(f.state.queryError).toBe('');
    expect(f.state.loading).toBe(false);
  });

  it('keeps mutation loading while a concurrent list settles, then refreshes', async () => {
    const f = await fixture();
    const deleting = f.state.handleDelete(row('delete', '.pdf'));
    await vi.waitFor(() => expect(f.deletes).toHaveLength(1));
    const query = f.start('B'); f.config(0, false);
    await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    f.finishList(0, [row('B', '.pdf')]); await query;
    expect(f.state.loading).toBe(true);
    f.deletes[0].resolve({ data: null });
    await vi.waitFor(() => expect(f.configs).toHaveLength(2));
    expect(f.state.loading).toBe(true);
    f.config(1, false); await vi.waitFor(() => expect(f.lists).toHaveLength(2));
    f.finishList(1, [row('refreshed', '.pdf')]); await deleting;
    expect(f.state.loading).toBe(false);
    expect(f.state.ossList.map(item => item.ossId)).toEqual(['refreshed']);
  });

  it('uses current search, pagination and sort values for each refresh', async () => {
    const f = await fixture();
    f.state.queryParams.pageNum = 4;
    f.state.handleQuery(); f.config(0, false);
    await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    expect(f.lists[0].query.pageNum).toBe(1);
    f.finishList(0, []); await vi.waitFor(() => expect(f.state.loading).toBe(false));
    f.state.queryParams.pageNum = 2;
    const page = f.state.getList(); f.config(1, false);
    await vi.waitFor(() => expect(f.lists).toHaveLength(2));
    expect(f.lists[1].query.pageNum).toBe(2);
    f.finishList(1, []); await page;
    f.state.handleOrderChange('service', 'descending'); f.config(2, false);
    await vi.waitFor(() => expect(f.lists).toHaveLength(3));
    expect(String(f.lists[2].query.orderByColumn)).toContain('service');
    expect(String(f.lists[2].query.isAsc)).toContain('descending');
    f.finishList(2, []); await vi.waitFor(() => expect(f.state.loading).toBe(false));
  });

  it('keeps ordinary preview errors blank and never trusts list URLs', async () => {
    const f = await fixture();
    const pending = f.start('image'); f.config(0, true);
    await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    f.finishList(0, [row('image')]);
    await vi.waitFor(() => expect(f.previews).toHaveLength(1));
    await pending;
    f.previews[0].gate.reject(new Error('temporary URL failure')); await settle();
    expect(f.state.ossList[0].url).toBe('');
    expect(f.state.previewUrls).toEqual({});
    expect(f.state.deletedPreviewIds).toEqual({});
    expect(f.state.queryError).toBe('');
  });

  it('uses an authorized URL for a current image without exposing the list URL', async () => {
    const f = await fixture();
    const pending = f.start('image'); f.config(0, true);
    await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    f.finishList(0, [row('image')]);
    await vi.waitFor(() => expect(f.previews).toHaveLength(1));
    await pending;
    expect(f.state.previewUrls).toEqual({});
    f.previews[0].gate.resolve(access('image', 'https://example.test/authorized-current-image'));
    await vi.waitFor(() => expect(f.state.previewUrls).toEqual({ image: 'https://example.test/authorized-current-image' }));
    expect(f.state.ossList[0].url).toBe('');
    expect(f.state.previewUrls).toEqual({ image: 'https://example.test/authorized-current-image' });
    expect(f.state.deletedPreviewIds).toEqual({});
  });

  it('shows each authorized preview as it resolves without waiting for another image', async () => {
    const f = await fixture();
    const pending = f.start('images'); f.config(0, true);
    await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    f.finishList(0, [row('slow'), row('fast')]);
    await vi.waitFor(() => expect(f.previews).toHaveLength(2));
    await pending;
    f.previews[1].gate.resolve(access('fast', 'https://example.test/fast'));
    await vi.waitFor(() => expect(f.state.previewUrls).toEqual({ fast: 'https://example.test/fast' }));
    expect(f.state.loading).toBe(false);
    f.previews[0].gate.resolve(access('slow', 'https://example.test/slow'));
    await vi.waitFor(() => expect(f.state.previewUrls).toEqual({
      fast: 'https://example.test/fast', slow: 'https://example.test/slow'
    }));
  });

  it('shows a successful list and completes getList while its image preview never settles', async () => {
    const f = await fixture();
    const pending = f.start('image'); f.config(0, true);
    await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    f.finishList(0, [row('image')], 8);
    await vi.waitFor(() => expect(f.previews).toHaveLength(1));
    let completed = false;
    void pending.then(() => { completed = true; });
    await vi.waitFor(() => expect(completed).toBe(true), { timeout: 500 });
    expect(f.state.ossList.map(item => item.ossId)).toEqual(['image']);
    expect(f.state.total).toBe(8);
    expect(f.state.previewListResource).toBe(true);
    expect(f.state.previewUrls).toEqual({});
    expect(f.state.loading).toBe(false);
  });

  it('finishes an operation refresh while its optional preview remains pending', async () => {
    const f = await fixture();
    const changing = f.state.handlePreviewListResource(true);
    await vi.waitFor(() => expect(f.configs).toHaveLength(1));
    f.config(0, true); await vi.waitFor(() => expect(f.lists).toHaveLength(1));
    f.finishList(0, [row('image')]);
    await vi.waitFor(() => expect(f.previews).toHaveLength(1));
    let completed = false;
    void changing.then(() => { completed = true; });
    await vi.waitFor(() => expect(completed).toBe(true), { timeout: 500 });
    expect(f.runtime.success).toHaveBeenCalledWith('启用成功');
    expect(f.state.loading).toBe(false);
  });
});
