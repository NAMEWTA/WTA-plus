import { workflowDomainModule } from '@namewta/domain-workflow';
import { AppRuntimeError, composeAppRuntime } from '@namewta/platform-app-runtime';
import { describe, expect, it, vi } from 'vitest';
import { reactive } from 'vue';
import { createDesignerController } from './designer';
import { createLiveWorkflowDictRefs, createWorkflowWebDomain } from './index';

const runtime = {
  service: {},
  confirm: vi.fn(),
  success: vi.fn(),
  designUrl: vi.fn(id => '/workflow/design/' + id)
} as never;

describe('workflow web-domain manifest', () => {
  it('publishes exact server keys, component names and permission contributions', () => {
    const manifest = createWorkflowWebDomain(runtime);
    expect(manifest.registrations.map(({ componentKey, componentName }) => ({ componentKey, componentName }))).toEqual([
      { componentKey: 'workflow/category/index', componentName: 'Category' },
      { componentKey: 'workflow/processDefinition/index', componentName: 'processDefinition' },
      { componentKey: 'workflow/processDefinition/design', componentName: 'WarmFlow' },
      { componentKey: 'workflow/spel/index', componentName: 'Spel' },
      { componentKey: 'workflow/task/taskWaiting', componentName: 'taskWaiting' },
      { componentKey: 'workflow/task/taskFinish', componentName: 'taskFinish' },
      { componentKey: 'workflow/task/taskCopyList', componentName: 'taskCopyList' },
      { componentKey: 'workflow/task/myDocument', componentName: 'myDocument' },
      { componentKey: 'workflow/task/allTaskWaiting', componentName: 'allTaskWaiting' },
      { componentKey: 'workflow/processInstance/index', componentName: 'processInstance' },
      { componentKey: 'workflow/leave/index', componentName: 'leave' },
      { componentKey: 'workflow/leave/leaveEdit', componentName: 'leaveEdit' }
    ]);
    expect(manifest.permissions.map(item => item.id)).toEqual([
      'workflow-category',
      'workflow-definition',
      'workflow-spel',
      'workflow-task-runtime',
      'workflow-instance-runtime',
      'workflow-leave-runtime'
    ]);
    expect(manifest.permissions.flatMap(item => item.permissions)).toContain('workflow:definition:publish');
    expect(manifest.permissions.flatMap(item => item.permissions)).toContain('workflow:category:query');
    expect(manifest.permissions.flatMap(item => item.permissions)).toContain('workflow:spel:query');
    expect(manifest.permissions.flatMap(item => item.permissions)).toEqual(
      expect.arrayContaining([
        'workflow:task:edit',
        'workflow:instance:cancel',
        'workflow:instance:invalid',
        'workflow:instance:active',
        'workflow:instance:variableQuery',
        'workflow:leave:edit',
        'workflow:leave:remove',
        'workflow:leave:export'
      ])
    );
  });

  it('registers workflow only when selected and rejects duplicate component keys', () => {
    const manifest = createWorkflowWebDomain(runtime);
    const selected = composeAppRuntime({
      appId: 'admin-web',
      domainModules: [workflowDomainModule],
      manifests: [manifest],
      selectedDomainIds: ['workflow'],
      selectedManifestIds: ['web-domain-workflow']
    });
    expect(selected.componentKeys()).toEqual([
      'workflow/category/index',
      'workflow/leave/index',
      'workflow/leave/leaveEdit',
      'workflow/processDefinition/design',
      'workflow/processDefinition/index',
      'workflow/processInstance/index',
      'workflow/spel/index',
      'workflow/task/allTaskWaiting',
      'workflow/task/myDocument',
      'workflow/task/taskCopyList',
      'workflow/task/taskFinish',
      'workflow/task/taskWaiting'
    ]);
    const unselected = composeAppRuntime({
      appId: 'fixture-web',
      domainModules: [],
      manifests: [manifest],
      selectedDomainIds: [],
      selectedManifestIds: []
    });
    expect(unselected.componentKeys()).toEqual([]);
    const duplicate = {
      ...manifest,
      id: 'workflow-duplicate',
      registrations: [{ ...manifest.registrations[0], id: 'duplicate' }]
    };
    expect(() =>
      composeAppRuntime({
        appId: 'admin-web',
        domainModules: [workflowDomainModule],
        manifests: [manifest, duplicate],
        selectedDomainIds: ['workflow'],
        selectedManifestIds: ['web-domain-workflow', 'workflow-duplicate']
      })
    ).toThrowError(AppRuntimeError);
  });
});

describe('workflow designer controller', () => {
  it('preserves definitionId, disabled and close-return semantics', async () => {
    const designUrl = vi.fn(async () => '/designer');
    const closeDesigner = vi.fn();
    const controller = createDesignerController(
      {
        service: {},
        confirm: vi.fn(),
        success: vi.fn(),
        error: vi.fn(),
        dicts: vi.fn(),
        download: vi.fn(),
        designUrl,
        closeDesigner
      } as never,
      { definitionId: 'definition/a', disabled: 'true', activeName: '1' }
    );
    await expect(controller.url()).resolves.toBe('/designer');
    expect(designUrl).toHaveBeenCalledWith('definition/a', true);
    await controller.onMessage({ method: 'close' });
    expect(closeDesigner).toHaveBeenCalledWith('1');
  });
});

describe('workflow host adapters', () => {
  it('keeps asynchronously loaded dictionary properties live after reassignment', async () => {
    const source = reactive({ status: [{ label: '初始', value: '0' }] });
    let load!: (value: typeof source) => void;
    const dicts = createLiveWorkflowDictRefs(['status'], () => new Promise(resolve => (load = resolve)));

    expect(dicts.status.value).toEqual([]);
    load(source);
    await vi.waitFor(() => expect(dicts.status.value).toEqual([{ label: '初始', value: '0' }]));
    source.status = [{ label: '已加载', value: '1' }];
    await vi.waitFor(() => expect(dicts.status.value).toEqual([{ label: '已加载', value: '1' }]));
  });
});
