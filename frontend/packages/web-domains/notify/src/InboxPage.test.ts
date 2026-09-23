import type { NotifyInboxMessage } from '@namewta/domain-notify';
import { ElMessage } from 'element-plus';
import { createRenderer, ref, ssrContextKey, type ComponentOptions } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import InboxPage from './InboxPage.vue';
import type { NotifyWebRuntime } from './runtime';

vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn() } }));

type HostNode = { parent?: HostNode; children: HostNode[] };
type PageState = {
  rows: NotifyInboxMessage[];
  total: number;
  unreadTotal: number;
  loading: boolean;
  error: string;
  selected?: NotifyInboxMessage;
  detailVisible: boolean;
  detailError: string;
  detailLoading: boolean;
  pageNum: number;
  load(): Promise<void>;
  changePage(page: number): void;
  openDetail(row: NotifyInboxMessage): Promise<void>;
  readAll(): Promise<void>;
};
const node = (): HostNode => ({ children: [] });
const renderer = createRenderer<HostNode, HostNode>({
  createElement: node, createText: node, createComment: node,
  setText: () => {}, setElementText: () => {}, patchProp: () => {},
  insert: (child, parent) => { child.parent = parent; parent.children.push(child); },
  remove: child => { if (child.parent) child.parent.children = child.parent.children.filter(item => item !== child); },
  parentNode: child => child.parent ?? null, nextSibling: () => null
});
const cleanups: Array<() => void> = [];
afterEach(() => { cleanups.splice(0).forEach(cleanup => cleanup()); vi.clearAllMocks(); });

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason: Error) => void;
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}
const row = (messageId: string, readTime: string | null = null): NotifyInboxMessage =>
  ({ messageId, category: 'notice', title: '合成消息 ' + messageId, message: '摘要', readTime });
const page = (rows: NotifyInboxMessage[], total = rows.length, unreadTotal = rows.length) =>
  ({ data: { rows, total, unreadTotal } });

function fixture(canRead = true) {
  const epoch = ref(1);
  const active = ref(true);
  const list = vi.fn().mockResolvedValue(page([row('501')], 501, 481));
  const detail = vi.fn().mockResolvedValue({ data: { ...row('501'), content: '完整正文' } });
  const read = vi.fn().mockResolvedValue({ data: null });
  const readAll = vi.fn().mockResolvedValue({ data: null });
  const inboxChanged = vi.fn();
  const unsubscribe = vi.fn();
  let event: (() => void) | undefined;
  const runtime = {
    service: { inbox: { list, detail, read, readAll } },
    inboxSession: { snapshot: () => ({ epoch: epoch.value, active: active.value }) },
    dicts: () => ({}),
    hasPermission: () => canRead,
    inboxChanged,
    subscribeInbox: (handler: () => void) => { event = handler; return unsubscribe; },
    navigate: vi.fn()
  } as unknown as NotifyWebRuntime;
  const app = renderer.createApp({ ...(InboxPage as unknown as ComponentOptions), render: () => null }, { runtime });
  app.provide(ssrContextKey, { modules: new Set<string>() });
  const instance = app.mount(node());
  cleanups.push(() => app.unmount());
  const state = Reflect.get(instance.$, 'setupState') as PageState;
  return { state, app, epoch, active, list, detail, read, readAll, inboxChanged, unsubscribe, event: () => event };
}

describe('完整本人收件箱真实 SFC', () => {
  it('第 501 条按页读取，详情独立 GET，单条已读和全部已读刷新全局数', async () => {
    const f = fixture();
    await vi.waitFor(() => expect(f.state.loading).toBe(false));
    expect(f.list).toHaveBeenCalledWith(1, 20);
    f.list.mockResolvedValueOnce(page([row('1')], 501, 481));
    f.state.changePage(26);
    await vi.waitFor(() => expect(f.state.rows[0]?.messageId).toBe('1'));
    expect(f.list).toHaveBeenLastCalledWith(26, 20);
    expect(f.state.total).toBe(501);
    expect(f.state.unreadTotal).toBe(481);
    f.list.mockResolvedValueOnce(page([row('1', '2026-09-23 10:00:00')], 501, 480));
    f.detail.mockResolvedValueOnce({ data: { ...row('1'), content: '仅详情的完整正文' } });
    await f.state.openDetail(row('1'));
    expect(f.detail).toHaveBeenCalledWith('1');
    expect(f.state.selected?.content).toBe('仅详情的完整正文');
    expect(f.read).toHaveBeenCalledWith('1');
    expect(f.state.unreadTotal).toBe(480);
    expect(f.inboxChanged).toHaveBeenCalledOnce();
    f.list.mockResolvedValueOnce(page([row('1', '2026-09-23 10:00:00')], 501, 0));
    await f.state.readAll();
    expect(f.readAll).toHaveBeenCalledOnce();
    expect(f.state.unreadTotal).toBe(0);
    expect(f.inboxChanged).toHaveBeenCalledTimes(2);
  });

  it('旧查询、旧详情及其失败不覆盖同页切换后的身份', async () => {
    const f = fixture();
    await vi.waitFor(() => expect(f.state.loading).toBe(false));
    const pendingList = deferred<ReturnType<typeof page>>();
    f.list.mockReturnValueOnce(pendingList.promise);
    const oldLoad = f.state.load();
    const pendingDetail = deferred<{ data: NotifyInboxMessage }>();
    f.detail.mockReturnValueOnce(pendingDetail.promise);
    const oldDetail = f.state.openDetail(row('501'));
    f.list.mockResolvedValueOnce(page([row('B')], 2, 2));
    f.epoch.value = 2;
    await vi.waitFor(() => expect(f.state.rows[0]?.messageId).toBe('B'));
    expect(f.state.detailVisible).toBe(false);
    pendingList.resolve(page([row('A')], 501, 481));
    pendingDetail.reject(new Error('旧会话详情失败'));
    await Promise.all([oldLoad, oldDetail]);
    expect(f.state.rows.map(item => item.messageId)).toEqual(['B']);
    expect(f.state.unreadTotal).toBe(2);
    expect(f.state.detailError).toBe('');
    expect(ElMessage.error).not.toHaveBeenCalled();
  });

  it('翻页关闭未完成的旧详情，只有新页能显示详情', async () => {
    const f = fixture();
    await vi.waitFor(() => expect(f.state.loading).toBe(false));
    const pending = deferred<{ data: NotifyInboxMessage }>();
    f.detail.mockReturnValueOnce(pending.promise);
    const oldDetail = f.state.openDetail(row('501'));
    f.list.mockResolvedValueOnce(page([row('1')], 501, 481));
    f.state.changePage(26);
    await vi.waitFor(() => expect(f.state.rows[0]?.messageId).toBe('1'));
    pending.resolve({ data: { ...row('501'), content: '旧页正文' } });
    await oldDetail;
    expect(f.state.detailVisible).toBe(false);
    expect(f.state.selected).toBeUndefined();
    expect(f.read).not.toHaveBeenCalled();
  });

  it('旧身份已读失败的 finally 不会清理新身份页面或提示错误', async () => {
    const f = fixture();
    await vi.waitFor(() => expect(f.state.loading).toBe(false));
    const pending = deferred<{ data: null }>();
    f.read.mockReturnValueOnce(pending.promise);
    const oldRead = f.state.openDetail(row('501'));
    await vi.waitFor(() => expect(f.read).toHaveBeenCalledWith('501'));
    f.list.mockResolvedValueOnce(page([row('B')], 2, 2));
    f.epoch.value = 2;
    await vi.waitFor(() => expect(f.state.rows[0]?.messageId).toBe('B'));
    pending.reject(new Error('旧身份已读失败'));
    await oldRead;
    expect(f.state.rows.map(item => item.messageId)).toEqual(['B']);
    expect(f.state.unreadTotal).toBe(2);
    expect(f.state.detailVisible).toBe(false);
    expect(ElMessage.error).not.toHaveBeenCalled();
  });

  it('退出代次先变化而令牌尚未清除时立即清空且不重载旧身份', async () => {
    const f = fixture();
    await vi.waitFor(() => expect(f.state.loading).toBe(false));
    const previousCalls = f.list.mock.calls.length;
    f.active.value = false;
    f.epoch.value = 2;
    expect(f.state.rows).toEqual([]);
    expect(f.state.unreadTotal).toBe(0);
    expect(f.state.detailVisible).toBe(false);
    expect(f.list).toHaveBeenCalledTimes(previousCalls);
  });

  it('只读权限可查看完整详情而不会自动标记已读', async () => {
    const f = fixture(false);
    await vi.waitFor(() => expect(f.state.loading).toBe(false));
    await f.state.openDetail(row('501'));
    expect(f.state.selected?.content).toBe('完整正文');
    expect(f.read).not.toHaveBeenCalled();
    await f.state.readAll();
    expect(f.readAll).not.toHaveBeenCalled();
  });

  it('空结果、当前错误恢复及卸载晚到响应都有明确终态', async () => {
    const f = fixture();
    await vi.waitFor(() => expect(f.state.loading).toBe(false));
    f.list.mockRejectedValueOnce(new Error('网络失败'));
    await f.state.load();
    expect(f.state.error).toBe('网络失败');
    expect(f.state.loading).toBe(false);
    f.list.mockResolvedValueOnce(page([], 0, 0));
    await f.state.load();
    expect(f.state.rows).toEqual([]);
    expect(f.state.total).toBe(0);
    expect(f.state.error).toBe('');
    const pending = deferred<ReturnType<typeof page>>();
    f.list.mockReturnValueOnce(pending.promise);
    const oldLoad = f.state.load();
    f.app.unmount();
    cleanups.pop();
    pending.resolve(page([row('late')], 1, 1));
    await oldLoad;
    expect(f.state.rows).toEqual([]);
    expect(f.unsubscribe).toHaveBeenCalledOnce();
  });
});
