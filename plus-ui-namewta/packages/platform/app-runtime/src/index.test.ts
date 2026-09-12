import { describe, expect, it, vi } from 'vitest';
import {
  AppRuntimeError,
  composeAppRuntime,
  type DomainModule,
  type WebDomainManifest,
  type WebRegistration
} from './index';

const domain = (id: string): DomainModule => ({
  id,
  backendModules: [`wta-${id}`],
  capabilities: [`${id}:read`]
});

const registration = (
  id: string,
  componentKey: string,
  componentName: string,
  load: () => Promise<string>
): WebRegistration<string> => ({ id, componentKey, componentName, load });

const manifest = (
  id: string,
  domainId: string,
  options: Partial<Omit<WebDomainManifest<string>, 'id' | 'domainId'>> = {}
): WebDomainManifest<string> => ({
  id,
  domainId,
  messages: options.messages ?? [],
  permissions: options.permissions ?? [],
  registrations: options.registrations ?? []
});

describe('app runtime manifest registry', () => {
  it('composes only selected messages, permissions, and web registrations', () => {
    const demoLoader = vi.fn(async () => 'demo-view');
    const hiddenLoader = vi.fn(async () => 'hidden-view');
    const runtime = composeAppRuntime({
      appId: 'admin-web',
      domainModules: [domain('demo'), domain('hidden')],
      manifests: [
        manifest('web-domain-demo', 'demo', {
          messages: [{ namespace: 'demo', messages: { title: 'Demo' } }],
          permissions: [{ id: 'demo-table', permissions: ['demo:demo:list'] }],
          registrations: [registration('demo-table', 'demo/demo/index', 'Demo', demoLoader)]
        }),
        manifest('web-domain-hidden', 'hidden', {
          messages: [{ namespace: 'hidden', messages: { title: 'Hidden' } }],
          permissions: [{ id: 'hidden-page', permissions: ['hidden:list'] }],
          registrations: [registration('hidden-page', 'hidden/index', 'Hidden', hiddenLoader)]
        })
      ],
      selectedDomainIds: ['demo'],
      selectedManifestIds: ['web-domain-demo']
    });

    expect(runtime.componentKeys()).toEqual(['demo/demo/index']);
    expect(runtime.messages()).toEqual([{ namespace: 'demo', messages: { title: 'Demo' } }]);
    expect(runtime.permissionContributions()).toEqual([{ id: 'demo-table', permissions: ['demo:demo:list'] }]);
    expect(runtime.webRegistrations()).toMatchObject([
      { id: 'demo-table', componentKey: 'demo/demo/index', componentName: 'Demo' }
    ]);
    expect(runtime.resolve({ componentKey: 'demo/demo/index', domainId: 'demo' })).toMatchObject({
      componentName: 'Demo',
      load: demoLoader
    });
    expect(demoLoader).not.toHaveBeenCalled();
    expect(hiddenLoader).not.toHaveBeenCalled();
  });

  it('ignores duplicate modules and manifests outside the selected App composition', () => {
    const runtime = composeAppRuntime({
      appId: 'admin-web',
      domainModules: [domain('demo'), domain('hidden'), domain('hidden')],
      manifests: [
        manifest('web-domain-demo', 'demo'),
        manifest('web-domain-hidden', 'hidden'),
        manifest('web-domain-hidden', 'hidden')
      ],
      selectedDomainIds: ['demo'],
      selectedManifestIds: ['web-domain-demo']
    });

    expect(runtime.componentKeys()).toEqual([]);
  });

  it('returns component keys in stable order without requiring new Array APIs', () => {
    const runtime = composeAppRuntime({
      appId: 'admin-web',
      domainModules: [domain('demo')],
      manifests: [
        manifest('web-domain-demo', 'demo', {
          registrations: [
            registration('z-page', 'demo/z/index', 'ZPage', async () => 'z'),
            registration('a-page', 'demo/a/index', 'APage', async () => 'a')
          ]
        })
      ],
      selectedDomainIds: ['demo'],
      selectedManifestIds: ['web-domain-demo']
    });

    expect(runtime.componentKeys()).toEqual(['demo/a/index', 'demo/z/index']);
  });

  it('rejects duplicate modules inside the selected App composition', () => {
    expect(() =>
      composeAppRuntime({
        appId: 'admin-web',
        domainModules: [domain('demo'), domain('demo')],
        manifests: [manifest('web-domain-demo', 'demo')],
        selectedDomainIds: ['demo'],
        selectedManifestIds: ['web-domain-demo']
      })
    ).toThrowError(expect.objectContaining({ code: 'duplicate-domain-module', domainId: 'demo' }));
  });

  it('rejects duplicate manifests inside the selected App composition', () => {
    expect(() =>
      composeAppRuntime({
        appId: 'admin-web',
        domainModules: [domain('demo')],
        manifests: [manifest('web-domain-demo', 'demo'), manifest('web-domain-demo', 'demo')],
        selectedDomainIds: ['demo'],
        selectedManifestIds: ['web-domain-demo']
      })
    ).toThrowError(expect.objectContaining({ code: 'duplicate-web-domain-manifest', manifestId: 'web-domain-demo' }));
  });

  it('fails before a later manifest can overwrite a duplicate component key', () => {
    expect(() =>
      composeAppRuntime({
        appId: 'admin-web',
        domainModules: [domain('demo'), domain('alternate')],
        manifests: [
          manifest('web-domain-demo', 'demo', {
            registrations: [registration('demo-table', 'demo/demo/index', 'Demo', async () => 'first')]
          }),
          manifest('web-domain-alternate', 'alternate', {
            registrations: [registration('alternate-table', 'demo/demo/index', 'Alternate', async () => 'second')]
          })
        ],
        selectedDomainIds: ['demo', 'alternate'],
        selectedManifestIds: ['web-domain-demo', 'web-domain-alternate']
      })
    ).toThrowError(
      expect.objectContaining({
        code: 'duplicate-component-key',
        appId: 'admin-web',
        domainId: 'alternate',
        componentKey: 'demo/demo/index',
        manifestId: 'web-domain-alternate',
        conflictingDomainId: 'demo',
        conflictingManifestId: 'web-domain-demo'
      })
    );
  });

  it.each([
    {
      code: 'duplicate-message-namespace',
      first: manifest('web-domain-demo', 'demo', {
        messages: [{ namespace: 'shared', messages: { first: 'First' } }]
      }),
      second: manifest('web-domain-alternate', 'alternate', {
        messages: [{ namespace: 'shared', messages: { second: 'Second' } }]
      })
    },
    {
      code: 'duplicate-web-registration-id',
      first: manifest('web-domain-demo', 'demo', {
        registrations: [registration('shared', 'demo/demo/index', 'Demo', async () => 'first')]
      }),
      second: manifest('web-domain-alternate', 'alternate', {
        registrations: [registration('shared', 'alternate/index', 'Alternate', async () => 'second')]
      })
    },
    {
      code: 'duplicate-permission-contribution-id',
      first: manifest('web-domain-demo', 'demo', {
        permissions: [{ id: 'shared', permissions: ['demo:list'] }]
      }),
      second: manifest('web-domain-alternate', 'alternate', {
        permissions: [{ id: 'shared', permissions: ['alternate:list'] }]
      })
    }
  ])('rejects selected contribution conflicts: $code', ({ code, first, second }) => {
    expect(() =>
      composeAppRuntime({
        appId: 'admin-web',
        domainModules: [domain('demo'), domain('alternate')],
        manifests: [first, second],
        selectedDomainIds: ['demo', 'alternate'],
        selectedManifestIds: ['web-domain-demo', 'web-domain-alternate']
      })
    ).toThrowError(expect.objectContaining({ code }));
  });

  it('returns cloned frozen registrations that source manifest mutation cannot change', () => {
    const sourceRegistration = registration('demo-table', 'demo/demo/index', 'Demo', async () => 'view');
    const sourceMessages = { title: 'Demo' };
    const sourcePermissions = ['demo:list'];
    const runtime = composeAppRuntime({
      appId: 'admin-web',
      domainModules: [domain('demo')],
      manifests: [
        manifest('web-domain-demo', 'demo', {
          messages: [{ namespace: 'demo', messages: sourceMessages }],
          permissions: [{ id: 'demo-table', permissions: sourcePermissions }],
          registrations: [sourceRegistration]
        })
      ],
      selectedDomainIds: ['demo'],
      selectedManifestIds: ['web-domain-demo']
    });
    const resolved = runtime.resolve({ componentKey: 'demo/demo/index', domainId: 'demo' });

    sourceRegistration.componentName = 'Mutated';
    sourceMessages.title = 'Mutated';
    sourcePermissions[0] = 'mutated:list';
    expect(resolved.componentName).toBe('Demo');
    expect(runtime.messages()).toEqual([{ namespace: 'demo', messages: { title: 'Demo' } }]);
    expect(runtime.permissionContributions()).toEqual([{ id: 'demo-table', permissions: ['demo:list'] }]);
    expect(Object.isFrozen(resolved)).toBe(true);
    expect(Object.isFrozen(runtime.webRegistrations())).toBe(true);
    expect(Object.isFrozen(runtime.webRegistrations()[0])).toBe(true);
    expect(Object.isFrozen(runtime.messages())).toBe(true);
    expect(Object.isFrozen(runtime.messages()[0])).toBe(true);
    expect(Object.isFrozen(runtime.messages()[0].messages)).toBe(true);
    expect(Object.isFrozen(runtime.permissionContributions())).toBe(true);
    expect(Object.isFrozen(runtime.permissionContributions()[0])).toBe(true);
    expect(Object.isFrozen(runtime.permissionContributions()[0].permissions)).toBe(true);
    expect(() => {
      (resolved as { componentName: string }).componentName = 'External mutation';
    }).toThrow(TypeError);
  });

  it('reports a stable missing component diagnostic with app, domain, and key', () => {
    const runtime = composeAppRuntime({
      appId: 'fixture-web',
      domainModules: [domain('demo')],
      manifests: [manifest('web-domain-demo', 'demo')],
      selectedDomainIds: ['demo'],
      selectedManifestIds: ['web-domain-demo']
    });

    expect(() => runtime.resolve({ componentKey: 'demo/missing/index', domainId: 'demo' })).toThrowError(
      expect.objectContaining({
        code: 'missing-component-key',
        appId: 'fixture-web',
        domainId: 'demo',
        componentKey: 'demo/missing/index'
      })
    );
  });

  it('rejects selected web domains whose headless domain is unselected', () => {
    expect(() =>
      composeAppRuntime({
        appId: 'fixture-web',
        domainModules: [domain('demo')],
        manifests: [manifest('web-domain-demo', 'demo')],
        selectedDomainIds: [],
        selectedManifestIds: ['web-domain-demo']
      })
    ).toThrowError(
      expect.objectContaining({
        code: 'unselected-domain',
        appId: 'fixture-web',
        domainId: 'demo',
        componentKey: '*'
      })
    );
  });

  it('rejects a selected manifest that is not available to the App', () => {
    expect(() =>
      composeAppRuntime({
        appId: 'admin-web',
        domainModules: [domain('demo')],
        manifests: [],
        selectedDomainIds: ['demo'],
        selectedManifestIds: ['web-domain-demo']
      })
    ).toThrowError(AppRuntimeError);
  });
});
