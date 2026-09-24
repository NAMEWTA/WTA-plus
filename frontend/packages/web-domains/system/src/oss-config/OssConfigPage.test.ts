import { createRenderer, ref, ssrContextKey, type ComponentOptions } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import page from './OssConfigPage.vue?raw';
import OssConfigPage from './OssConfigPage.vue';
import type { SystemWebRuntime } from '../runtime';

type HostNode = { parent?: HostNode; children: HostNode[] };
const node = (): HostNode => ({ children: [] });
const renderer = createRenderer<HostNode, HostNode>({
  createElement: node, createText: node, createComment: node,
  setText: () => {}, setElementText: () => {}, patchProp: () => {},
  insert: (child, parent) => { child.parent = parent; parent.children.push(child); },
  remove: child => { if (child.parent) child.parent.children = child.parent.children.filter(item => item !== child); },
  parentNode: child => child.parent ?? null, nextSibling: () => null
});

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason: Error) => void;
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

const diagnostic = (subject: string) => ({
  data: { status: 'NOT_SERVING', reason: 'DIAGNOSTIC_UNVERIFIED', checkedAt: '2026-09-23T00:00:00Z',
    facts: [{ subject, observation: 'UNKNOWN', source: 'BUCKET_POLICY', scope: 'BUCKET',
      basis: 'POLICY_UNREADABLE', observedAt: '2026-09-23T00:00:00Z' }] }
});

function fixture() {
  const requests: Array<ReturnType<typeof deferred<ReturnType<typeof diagnostic>>> & { signal?: AbortSignal }> = [];
  const identity = ref<string | undefined>('admin-A');
  const canRead = ref(true);
  const runtime = {
    service: { resources: { ossConfigs: {
      list: async () => ({ data: { rows: [], total: 0 } }),
      diagnose: (_id: string, signal?: AbortSignal) => {
        const gate = Object.assign(deferred<ReturnType<typeof diagnostic>>(), { signal });
        requests.push(gate);
        return gate.promise;
      }
    } } },
    dicts: () => ({ sys_yes_no: [] }),
    currentUserId: () => identity.value,
    hasPermission: () => canRead.value,
    confirm: vi.fn(), success: vi.fn()
  } as unknown as SystemWebRuntime;
  const app = renderer.createApp({ ...(OssConfigPage as unknown as ComponentOptions), render: () => null }, { runtime });
  app.provide(ssrContextKey, { modules: new Set<string>() });
  const instance = app.mount(node());
  let mounted = true;
  const state = Reflect.get(instance.$, 'setupState') as {
    handleDiagnose(row: { ossConfigId: string }): Promise<void>;
    closeDiagnostic(): void;
    diagnosticVisible: boolean;
    diagnosticLoading: boolean;
    diagnosticError: string;
    diagnosticResult?: ReturnType<typeof diagnostic>['data'];
  };
  return { state, requests, identity, canRead, unmount: () => {
    if (mounted) { mounted = false; app.unmount(); }
  } };
}

describe('OSS config access policy page', () => {
  it('offers only the supported semantic access policies', () => {
    expect(page).toContain('<el-radio value="PRIVATE">PRIVATE</el-radio>');
    expect(page).toContain('<el-radio value="PUBLIC_READ">PUBLIC_READ</el-radio>');
    expect(page).not.toMatch(/custom|value="1"/i);
  });

  it('keeps public configs non-default and requires their production domain', () => {
    expect(page).toContain(':disabled="scope.row.accessPolicy === \'PUBLIC_READ\'"');
    expect(page).toContain("if (isPublic) form.value.status = 'N'");
    expect(page).toContain("form.value.accessPolicy === 'PUBLIC_READ' && !value?.trim()");
    expect(page).toContain('PUBLIC_READ 在生产环境必须配置可公开访问的 domainUrl');
  });

  it('uses the default-state wording and labels icon-only row commands', () => {
    expect(page).toContain('label="是否默认"');
    expect(page).toContain('aria-label="修改"');
    expect(page).toContain('aria-label="删除"');
  });

  it('exposes only the list permission for on-demand diagnosis and states its limited scope', () => {
    expect(page).toContain('v-hasPermi="[\'system:ossConfig:list\']"');
    expect(page).toContain('单对象读取不证明全桶安全');
    expect(page).toContain('aria-label="只读诊断"');
  });

  it('keeps the latest diagnosis through older success and failure, and ignores results after close or unmount', async () => {
    const f = fixture();
    try {
      const old = f.state.handleDiagnose({ ossConfigId: '1' });
      const current = f.state.handleDiagnose({ ossConfigId: '2' });
      expect(f.requests[0].signal?.aborted).toBe(true);
      f.requests[1].resolve(diagnostic('OBJECT_GET'));
      await current;
      f.requests[0].reject(new Error('old provider secret'));
      await old;
      expect(f.state.diagnosticResult?.facts[0].subject).toBe('OBJECT_GET');
      expect(f.state.diagnosticError).toBe('');
      expect(f.state.diagnosticLoading).toBe(false);

      const closing = f.state.handleDiagnose({ ossConfigId: '3' });
      f.state.closeDiagnostic();
      expect(f.requests[2].signal?.aborted).toBe(true);
      f.requests[2].resolve(diagnostic('POLICY_READ'));
      await closing;
      expect(f.state.diagnosticVisible).toBe(false);
      expect(f.state.diagnosticResult).toBeUndefined();

      const unmounted = f.state.handleDiagnose({ ossConfigId: '4' });
      f.unmount();
      expect(f.requests[3].signal?.aborted).toBe(true);
      f.requests[3].reject(new Error('late provider secret'));
      await unmounted;
      expect(f.state.diagnosticError).toBe('');
    } finally {
      f.unmount();
    }
  });

  it('shows a fixed failure without exposing a provider exception', async () => {
    const f = fixture();
    try {
      const pending = f.state.handleDiagnose({ ossConfigId: '1' });
      f.requests[0].reject(new Error('provider secret detail'));
      await pending;
      expect(f.state.diagnosticError).toBe('诊断暂不可用，请稍后重试');
      expect(f.state.diagnosticResult).toBeUndefined();
    } finally {
      f.unmount();
    }
  });

  it('closes visible facts on identity change and ignores the old request even if it settles later', async () => {
    const f = fixture();
    try {
      const old = f.state.handleDiagnose({ ossConfigId: '1' });
      f.identity.value = 'admin-B';
      await vi.waitFor(() => expect(f.requests[0].signal?.aborted).toBe(true));
      f.requests[0].resolve(diagnostic('OBJECT_GET'));
      await old;
      await vi.waitFor(() => expect(f.state.diagnosticVisible).toBe(false));
      expect(f.state.diagnosticResult).toBeUndefined();

      const current = f.state.handleDiagnose({ ossConfigId: '2' });
      f.requests[1].resolve(diagnostic('POLICY_READ'));
      await current;
      expect(f.state.diagnosticResult?.facts[0].subject).toBe('POLICY_READ');
      f.identity.value = undefined;
      await vi.waitFor(() => expect(f.state.diagnosticVisible).toBe(false));
      expect(f.state.diagnosticResult).toBeUndefined();
    } finally {
      f.unmount();
    }
  });

  it('does not issue or retain a diagnosis without the list permission', async () => {
    const f = fixture();
    try {
      f.canRead.value = false;
      await f.state.handleDiagnose({ ossConfigId: '1' });
      expect(f.requests).toHaveLength(0);
      expect(f.state.diagnosticVisible).toBe(false);
    } finally {
      f.unmount();
    }
  });
});
