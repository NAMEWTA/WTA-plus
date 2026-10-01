import { describe, expect, it, vi } from 'vitest';
import { createRenderer, nextTick, ref, ssrContextKey, type ComponentOptions } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import AuthProviderSelect from './AuthProviderSelect.vue';
import OidcMetadataPanel from './OidcMetadataPanel.vue';
import { useAuthConfig } from './useAuthConfig';

type Host = { parent?: Host; children: Host[] };
const node = (): Host => ({ children: [] });
const renderer = createRenderer<Host, Host>({
  createElement: node,
  createText: node,
  createComment: node,
  setText: () => {},
  setElementText: () => {},
  patchProp: () => {},
  insert: (child, parent) => {
    child.parent = parent;
    parent.children.push(child);
  },
  remove: child => {
    if (child.parent) child.parent.children = child.parent.children.filter(value => value !== child);
  },
  parentNode: child => child.parent ?? null,
  nextSibling: () => null
});
function mount(component: ComponentOptions, props: Record<string, unknown> = {}) {
  const app = renderer.createApp({ ...component, render: () => null }, props);
  app.provide(ssrContextKey, { modules: new Set<string>() });
  const instance = app.mount(node());
  return { state: Reflect.get(instance.$, 'setupState'), unmount: () => app.unmount() };
}
function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>(done => {
    resolve = done;
  });
  return { promise, resolve };
}
const tick = async () => {
  await Promise.resolve();
  await nextTick();
};

describe('registration permission and asynchronous lifecycle', () => {
  it('loads the selected provider with only registration permission and no provider resource access', async () => {
    const selected = {
      id: '42',
      providerKey: 'company',
      name: 'Company',
      protocol: 'OIDC',
      issuer: 'https://id.example',
      enabled: true,
      options: {}
    };
    const providerOptions = vi.fn().mockResolvedValue([selected]);
    const onSelected = vi.fn();
    const runtime = {
      hasPermission: (value: string) => value === 'system:authRegistration:list',
      sessionSnapshot: () => ({ generation: 1 }),
      service: { authConfig: { providerOptions } }
    } as unknown as SystemWebRuntime;
    const f = mount(AuthProviderSelect as unknown as ComponentOptions, { runtime, modelValue: '42', onSelected });
    await tick();
    expect(providerOptions).toHaveBeenCalledWith({ keyword: undefined, selectedId: '42' }, expect.any(AbortSignal));
    await vi.waitFor(() => expect(onSelected).toHaveBeenLastCalledWith(selected));
    f.unmount();
  });
  it('permits fresh metadata inspection with registration list permission', async () => {
    const oidcMetadata = vi.fn().mockResolvedValue({ issuer: 'https://id.example' });
    const runtime = {
      hasPermission: (value: string) => value === 'system:authRegistration:list',
      sessionSnapshot: () => ({ generation: 1 }),
      service: { authConfig: { oidcMetadata } }
    } as unknown as SystemWebRuntime;
    const f = mount(OidcMetadataPanel as unknown as ComponentOptions, { runtime, issuer: 'https://id.example' });
    await f.state.inspect();
    expect(oidcMetadata).toHaveBeenCalledWith('https://id.example', expect.any(AbortSignal));
    f.unmount();
  });
  for (const invalidation of ['generation', 'unmount']) {
    it(`does not return a late saved ID after ${invalidation} during list refresh`, async () => {
      const generation = ref(1);
      const refresh = deferred<{ rows: { id?: string; version: number }[]; total: number }>();
      const list = vi
        .fn()
        .mockResolvedValueOnce({ rows: [], total: 0 })
        .mockImplementationOnce(() => refresh.promise);
      const resource = {
        list,
        get: vi.fn(),
        save: vi.fn().mockResolvedValue('saved-id'),
        remove: vi.fn(),
        refresh: vi.fn()
      };
      const runtime = {
        hasPermission: () => true,
        sessionSnapshot: () => ({ generation: generation.value }),
        success: vi.fn()
      } as unknown as SystemWebRuntime;
      let state!: ReturnType<typeof useAuthConfig<{ id?: string; version: number }>>;
      const f = mount({
        setup() {
          state = useAuthConfig(runtime, resource, 'system:authRegistration', () => ({ version: 0 }));
        }
      });
      await tick();
      const saved = state.save();
      await tick();
      expect(list).toHaveBeenCalledTimes(2);
      if (invalidation === 'generation') generation.value++;
      else f.unmount();
      refresh.resolve({ rows: [{ id: 'other-session-data', version: 0 }], total: 1 });
      expect(await saved).toBeUndefined();
      expect(state.rows.value).toEqual([]);
      if (invalidation === 'generation') f.unmount();
    });
  }
});
