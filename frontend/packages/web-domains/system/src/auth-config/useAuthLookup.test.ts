import { describe, expect, it, vi } from 'vitest';
import { effectScope, ref } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import { useAuthLookup } from './useAuthLookup';

describe('authentication configuration option requests', () => {
  function fixture(load: (keyword: string, signal: AbortSignal) => Promise<string[]>) {
    const generation = ref(1);
    const runtime = {
      sessionSnapshot: () => ({ generation: generation.value, identityLoaded: true })
    } as SystemWebRuntime;
    const scope = effectScope();
    const state = scope.run(() => useAuthLookup(runtime, load))!;
    return { generation, scope, state };
  }
  it('cancels superseded searches and ignores responses arriving out of order', async () => {
    let finish!: (rows: string[]) => void;
    const load = vi
      .fn<(keyword: string, signal: AbortSignal) => Promise<string[]>>()
      .mockImplementationOnce(
        () =>
          new Promise(resolve => {
            finish = resolve;
          })
      )
      .mockResolvedValueOnce(['new']);
    const f = fixture(load);
    const old = f.state.search('old');
    await f.state.search('new');
    expect(load.mock.calls[0][1].aborted).toBe(true);
    finish(['stale']);
    await old;
    expect(f.state.rows.value).toEqual(['new']);
    expect(f.state.loading.value).toBe(false);
    f.scope.stop();
  });
  it('shows a readable error and permits retry', async () => {
    const load = vi
      .fn()
      .mockRejectedValueOnce(new Error('当前管理员没有读取身份源的权限'))
      .mockResolvedValueOnce(['recovered']);
    const f = fixture(load);
    await f.state.search();
    expect(f.state.error.value).toBe('当前管理员没有读取身份源的权限');
    expect(f.state.loading.value).toBe(false);
    await f.state.search();
    expect(f.state.error.value).toBe('');
    expect(f.state.rows.value).toEqual(['recovered']);
    f.scope.stop();
  });
  for (const invalidate of ['unmount', 'identity'] as const)
    it(`discards pending data after ${invalidate}`, async () => {
      let finish!: (rows: string[]) => void;
      const load = vi.fn(
        (_keyword: string, _signal: AbortSignal) =>
          new Promise<string[]>(resolve => {
            finish = resolve;
          })
      );
      const f = fixture(load);
      const pending = f.state.search();
      if (invalidate === 'unmount') f.scope.stop();
      else f.generation.value++;
      finish(['private-config-name']);
      await pending;
      expect(load.mock.calls[0][1].aborted).toBe(true);
      expect(f.state.rows.value).toEqual([]);
      expect(f.state.loading.value).toBe(false);
      f.scope.stop();
    });
});
