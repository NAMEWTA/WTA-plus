import { createRenderer, reactive, type ComponentOptions } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ElMessage } from 'element-plus';
import { refreshMessageInbox } from '@/utils/push';
import Notice from './index.vue';

const harness = vi.hoisted(() => ({
  detail: vi.fn(),
  read: vi.fn(),
  readAll: vi.fn(),
  push: vi.fn(),
  session: undefined as undefined | { sessionGeneration: number; token: string; identityLoaded: boolean },
  notice: undefined as undefined | { state: { notices: unknown[]; loadState: string; identityVersion: number }; unreadCount: number },
  token: 'A',
  permission: true
}));
vi.mock('@/application/services', () => ({ notificationService: { inbox: {
  detail: harness.detail, read: harness.read, readAll: harness.readAll
} } }));
vi.mock('@/application/session', () => ({ getToken: () => harness.token }));
vi.mock('@/application/access', () => ({ createAdminAccessEvaluator: () => ({ hasPermission: () => harness.permission }) }));
vi.mock('@/store/modules/user', () => ({ useUserStore: () => harness.session }));
vi.mock('@/store/modules/notice', () => ({ useNoticeStore: () => harness.notice }));
vi.mock('@/utils/push', () => ({
  initMessageBox: vi.fn(), refreshMessageInbox: vi.fn(), closePush: vi.fn()
}));
vi.mock('@/router', () => ({ default: { push: harness.push } }));
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn() } }));

type HostNode = { parent?: HostNode; children: HostNode[] };
type NoticeState = {
  onNewsClick(item: { messageId: string; title: string; message: string; read: boolean; time: string }): Promise<void>;
  readAll(): Promise<void>;
  state: { loading: boolean };
  canRead: boolean;
  detailVisible: boolean;
  selectedNews?: { title: string; content?: string };
  detailError: string;
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

function fixture() {
  harness.token = 'A';
  harness.permission = true;
  harness.session = reactive({ sessionGeneration: 1, token: 'A', identityLoaded: true });
  harness.notice = reactive({
    state: { notices: [], loadState: 'ready', identityVersion: 1 },
    unreadCount: 1
  });
  harness.detail.mockResolvedValue({ data: { messageId: '1', title: 'A 消息', content: 'A 正文' } });
  const app = renderer.createApp({ ...(Notice as unknown as ComponentOptions), render: () => null });
  const instance = app.mount(node());
  cleanups.push(() => app.unmount());
  return { app, state: Reflect.get(instance.$, 'setupState') as NoticeState };
}

describe('顶部消息盒子详情隔离', () => {
  it('已显示的本人详情在登录代次变化时立即关闭并清空', async () => {
    const f = fixture();
    await f.state.onNewsClick({ messageId: '1', title: 'A 消息', message: '摘要', read: true, time: '' });
    expect(f.state.selectedNews?.content).toBe('A 正文');
    expect(f.state.detailVisible).toBe(true);
    harness.session!.sessionGeneration++;
    harness.session!.token = 'B';
    harness.token = 'B';
    expect(f.state.detailVisible).toBe(false);
    expect(f.state.selectedNews).toBeUndefined();
  });

  it('旧详情晚到的成功不能回填下一身份', async () => {
    const f = fixture();
    let resolve!: (result: { data: { content: string } }) => void;
    harness.detail.mockImplementationOnce(() => new Promise(yes => { resolve = yes; }));
    const pending = f.state.onNewsClick({ messageId: '1', title: 'A 消息', message: '摘要', read: true, time: '' });
    harness.session!.sessionGeneration++;
    harness.session!.token = 'B';
    harness.token = 'B';
    resolve({ data: { content: 'A 正文' } });
    await pending;
    expect(f.state.selectedNews).toBeUndefined();
    expect(f.state.detailError).toBe('');
    expect(harness.read).not.toHaveBeenCalled();
  });

  it('令牌未清但退出代次已变化时，旧全部已读结果和失败均不能刷新、报错或清理新状态', async () => {
    const f = fixture();
    let resolve!: () => void;
    harness.readAll.mockImplementationOnce(() => new Promise<void>(yes => { resolve = yes; }));
    const oldSuccess = f.state.readAll();
    harness.session!.sessionGeneration++;
    f.state.state.loading = true;
    resolve();
    await oldSuccess;
    expect(refreshMessageInbox).not.toHaveBeenCalled();
    expect(f.state.state.loading).toBe(true);

    f.state.state.loading = false;
    let reject!: (error: Error) => void;
    harness.readAll.mockImplementationOnce(() => new Promise<void>((_, no) => { reject = no; }));
    const oldFailure = f.state.readAll();
    harness.session!.sessionGeneration++;
    f.state.state.loading = true;
    reject(new Error('旧会话失败'));
    await oldFailure;
    expect(ElMessage.error).not.toHaveBeenCalled();
    expect(refreshMessageInbox).not.toHaveBeenCalled();
    expect(f.state.state.loading).toBe(true);
  });

  it('只读详情不会自动发送已读', async () => {
    const f = fixture();
    harness.permission = false;
    expect(f.state.canRead).toBe(false);
    await f.state.onNewsClick({ messageId: '1', title: 'A 消息', message: '摘要', read: false, time: '' });
    expect(f.state.selectedNews?.content).toBe('A 正文');
    expect(harness.read).not.toHaveBeenCalled();
  });

  it('令牌未清但退出代次变化时，旧详情已读晚到不能刷新下一身份', async () => {
    const f = fixture();
    let resolve!: () => void;
    harness.read.mockImplementationOnce(() => new Promise<void>(yes => { resolve = yes; }));
    const oldRead = f.state.onNewsClick({ messageId: '1', title: 'A 消息', message: '摘要', read: false, time: '' });
    await vi.waitFor(() => expect(harness.read).toHaveBeenCalledOnce());
    harness.session!.sessionGeneration++;
    resolve();
    await oldRead;
    expect(refreshMessageInbox).not.toHaveBeenCalled();
  });
});
