import { createRenderer, ssrContextKey, type ComponentOptions } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { NotificationDelivery } from '@namewta/domain-notify';
import NotificationPage from './NotificationPage.vue';
import type { NotifyWebRuntime } from './runtime';

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), info: vi.fn(), error: vi.fn() },
  ElMessageBox: { confirm: vi.fn().mockResolvedValue(undefined) }
}));

type HostNode = { parent?: HostNode; children: HostNode[] };
type PageState = {
  retry(row: NotificationDelivery): Promise<void>;
  cancel(row: NotificationDelivery): Promise<void>;
  load(): Promise<void>;
  rows: NotificationDelivery[];
  busy: string | null;
  loading: boolean;
  error: string;
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
const failed = (deliveryId = '303'): NotificationDelivery => ({
  intentId: '101', deliveryId, userId: '7', channel: 'SMS', status: 'FAILED', providerMessageId: null
});

async function fixture(permissions = ['notify:notification:retry', 'notify:notification:cancel']) {
  const deliveries = vi.fn().mockResolvedValue({ data: [failed()] });
  const retry = vi.fn().mockResolvedValue({ data: { notificationId: '101', status: 'QUEUED', queuedCount: 1 } });
  const cancel = vi.fn().mockResolvedValue({ data: { notificationId: '101', status: 'CANCELLED' } });
  const runtime = {
    service: { deliveries, notification: { retry, cancel } },
    hasPermission: (permission: string) => permissions.includes(permission),
    dicts: () => ({})
  } as unknown as NotifyWebRuntime;
  const app = renderer.createApp({ ...(NotificationPage as unknown as ComponentOptions), render: () => null }, { runtime });
  app.provide(ssrContextKey, { modules: new Set<string>() });
  const instance = app.mount(node());
  cleanups.push(() => app.unmount());
  const state = Reflect.get(instance.$, 'setupState') as PageState;
  await vi.waitFor(() => expect(state.loading).toBe(false));
  return { state, app, deliveries, retry, cancel };
}

describe('通知监控真实 SFC 人工操作', () => {
  it('按行精确重试且挂起期间只发一次，成功刷新当前列表', async () => {
    const f = await fixture();
    const gate = deferred<{ data: { notificationId: string; status: 'QUEUED'; queuedCount: number } }>();
    f.retry.mockReturnValueOnce(gate.promise);
    const first = f.state.retry(failed());
    await f.state.retry(failed());
    expect(f.retry).toHaveBeenCalledExactlyOnceWith('101', '303');
    expect(f.state.busy).toBe('retry:303');
    gate.resolve({ data: { notificationId: '101', status: 'QUEUED', queuedCount: 1 } });
    await first;
    expect(ElMessage.success).toHaveBeenCalledWith('已重新排队 1 项投递');
    expect(f.deliveries).toHaveBeenCalledTimes(2);
    expect(f.state.busy).toBeNull();
  });

  it('零任务显示真实状态；失败能恢复按钮，缺ID和权限时无请求', async () => {
    const f = await fixture();
    f.retry.mockResolvedValueOnce({ data: { notificationId: '101', status: 'FAILED', queuedCount: 0 } });
    await f.state.retry(failed());
    expect(ElMessage.info).toHaveBeenCalledWith('没有可重试任务，通知当前状态：失败');
    f.retry.mockRejectedValueOnce(new Error('不允许重试'));
    await f.state.retry(failed());
    expect(ElMessage.error).toHaveBeenCalledWith('不允许重试');
    expect(f.state.busy).toBeNull();
    await f.state.retry({ ...failed(), deliveryId: undefined });
    expect(f.retry).toHaveBeenCalledTimes(2);
    const forbidden = await fixture([]);
    await forbidden.state.retry(failed());
    expect(forbidden.retry).not.toHaveBeenCalled();
  });

  it('取消明确作用整个通知，确认等待期间重复点击不再发请求', async () => {
    const f = await fixture();
    const gate = deferred<unknown>();
    vi.mocked(ElMessageBox.confirm).mockReturnValueOnce(gate.promise as never);
    const row = { ...failed(), status: 'PENDING' as const };
    const first = f.state.cancel(row);
    await f.state.cancel(row);
    expect(ElMessageBox.confirm).toHaveBeenCalledTimes(1);
    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      expect.stringContaining('取消整个通知'), '取消通知', { type: 'warning' }
    );
    expect(f.cancel).not.toHaveBeenCalled();
    gate.resolve(undefined);
    await first;
    expect(f.cancel).toHaveBeenCalledExactlyOnceWith('101');
    expect(f.state.busy).toBeNull();
  });

  it('卸载后晚到的重试结果不提示也不刷新', async () => {
    const f = await fixture();
    const gate = deferred<{ data: { notificationId: string; status: 'QUEUED'; queuedCount: number } }>();
    f.retry.mockReturnValueOnce(gate.promise);
    const pending = f.state.retry(failed());
    f.app.unmount();
    cleanups.pop();
    gate.resolve({ data: { notificationId: '101', status: 'QUEUED', queuedCount: 1 } });
    await pending;
    expect(ElMessage.success).not.toHaveBeenCalled();
    expect(f.deliveries).toHaveBeenCalledTimes(1);
  });
});
