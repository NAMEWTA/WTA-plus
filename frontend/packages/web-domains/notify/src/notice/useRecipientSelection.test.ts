import { afterEach, describe, expect, it, vi } from 'vitest';
import { effectScope, ref } from 'vue';
import type { NotifyUserDirectory } from '../runtime';
import { useRecipientSelection } from './useRecipientSelection';

const user = (id: number) => ({ userId: id, userName: `user${id}`, status: '0' });
const scopes: ReturnType<typeof effectScope>[] = [];
function setup(initial: Array<string | number> = []) {
  const directory: NotifyUserDirectory = {
    searchUsers: vi.fn(async () => ({ data: { rows: [user(1), user(2)], total: 30 } })),
    usersByIds: vi.fn(async () => ({ data: [user(1)] })),
    userTypes: vi.fn(async () => ({ data: [] }))
  };
  const ids = ref(initial);
  const error = vi.fn();
  const scope = effectScope();
  scopes.push(scope);
  const picker = scope.run(() => useRecipientSelection(directory, ids, error))!;
  return { directory, ids, error, picker, scope };
}
afterEach(() => {
  scopes.splice(0).forEach(scope => scope.stop());
  vi.useRealTimers();
});

describe('通知接收者选择', () => {
  it('空白不请求；输入防抖并只查询最后一个关键词', async () => {
    vi.useFakeTimers();
    const { picker, directory } = setup();
    await picker.search();
    picker.keyword.value = 'a';
    await vi.advanceTimersByTimeAsync(150);
    picker.keyword.value = 'admin';
    await vi.advanceTimersByTimeAsync(300);
    expect(directory.searchUsers).toHaveBeenCalledTimes(1);
    expect(directory.searchUsers).toHaveBeenCalledWith('admin', 1, 20);
    expect(picker.rows.value).toHaveLength(2);
  });

  it('跨关键词/分页保留选择，取消当前页全选不影响其他已选', async () => {
    const { picker, directory, ids } = setup();
    picker.keyword.value = 'user';
    await picker.search();
    picker.togglePage(true);
    expect(ids.value).toEqual(['1', '2']);
    vi.mocked(directory.searchUsers).mockResolvedValue({ data: { rows: [user(3)], total: 30 } });
    await picker.search(2);
    picker.togglePage(true);
    expect(ids.value).toEqual(['1', '2', '3']);
    picker.togglePage(false);
    expect(ids.value).toEqual(['1', '2']);
    picker.keyword.value = 'different';
    await picker.search();
    expect(picker.selected.value.map(row => row.userName)).toEqual(['user1', 'user2']);
    picker.toggle(user(1), false);
    expect(ids.value).toEqual(['2']);
  });

  it('新输入使旧响应失效，旧请求不会覆盖候选名单', async () => {
    const { picker, directory } = setup();
    let finish!: (result: Awaited<ReturnType<NotifyUserDirectory['searchUsers']>>) => void;
    vi.mocked(directory.searchUsers).mockImplementationOnce(
      () =>
        new Promise(resolve => {
          finish = resolve;
        })
    );
    picker.keyword.value = 'old';
    const pending = picker.search();
    picker.keyword.value = 'new';
    await picker.search();
    finish({ data: { rows: [user(99)], total: 1 } });
    await pending;
    expect(picker.rows.value.map(row => row.userId)).toEqual([1, 2]);
    expect(picker.loading.value).toBe(false);
  });

  it('恢复草稿名单并去重；失败后能再次查询且loading复位', async () => {
    const { picker, directory, error } = setup(['1', 1]);
    await Promise.resolve();
    expect(picker.selected.value).toEqual([user(1)]);
    vi.mocked(directory.searchUsers).mockRejectedValueOnce(new Error('网络中断'));
    picker.keyword.value = 'user';
    await picker.search();
    expect(error).toHaveBeenCalledWith('网络中断');
    expect(picker.loading.value).toBe(false);
    await picker.search();
    expect(picker.rows.value).toHaveLength(2);
  });

  it('离开弹窗会取消待执行防抖，不产生多余请求', async () => {
    vi.useFakeTimers();
    const { picker, directory, scope } = setup();
    picker.keyword.value = 'user';
    scope.stop();
    await vi.advanceTimersByTimeAsync(500);
    expect(directory.searchUsers).not.toHaveBeenCalled();
  });
});
