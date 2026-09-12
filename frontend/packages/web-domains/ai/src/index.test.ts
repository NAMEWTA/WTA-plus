import { aiDomainModule } from '@namewta/domain-ai';
import { composeAppRuntime } from '@namewta/platform-app-runtime';
import { describe, expect, it, vi } from 'vitest';
import type { AiWebRuntime } from './runtime';
import { createAiWebDomain } from './index';

const runtime = (): AiWebRuntime => ({
  baseUrl: () => '/prod-api',
  probeFrame: vi.fn<AiWebRuntime['probeFrame']>(async () => undefined),
  service: { registerCurrentSnailUser: vi.fn() },
  trustedCredential: vi.fn(() => 'credential')
});

describe('AI web-domain manifest', () => {
  it('publishes the exact key, stable component name, and intentionally empty permission contribution', () => {
    const manifest = createAiWebDomain(runtime());

    expect(manifest).toMatchObject({ id: 'web-domain-ai', domainId: 'ai' });
    expect(manifest.registrations).toMatchObject([
      { id: 'embedded-chat', componentKey: 'ai/chat/index', componentName: 'AiChatPage' }
    ]);
    expect(manifest.permissions).toEqual([]);
    expect(Object.isFrozen(manifest)).toBe(true);
    expect(Object.isFrozen(manifest.registrations)).toBe(true);
  });

  it('registers only when the App explicitly selects AI', () => {
    const manifest = createAiWebDomain(runtime());
    const selected = composeAppRuntime({
      appId: 'admin-web',
      domainModules: [aiDomainModule],
      manifests: [manifest],
      selectedDomainIds: ['ai'],
      selectedManifestIds: ['web-domain-ai']
    });
    const unselected = composeAppRuntime({
      appId: 'fixture-web',
      domainModules: [],
      manifests: [manifest],
      selectedDomainIds: [],
      selectedManifestIds: []
    });

    expect(selected.componentKeys()).toEqual(['ai/chat/index']);
    expect(unselected.componentKeys()).toEqual([]);
  });

  it('fails closed when the host omits or corrupts a runtime port', () => {
    expect(() => createAiWebDomain(undefined as unknown as AiWebRuntime)).toThrow('AiWebRuntime is required');
    expect(() => createAiWebDomain({ ...runtime(), baseUrl: undefined as never })).toThrow(
      'AiWebRuntime.baseUrl is required'
    );
    expect(() => createAiWebDomain({ ...runtime(), probeFrame: undefined as never })).toThrow(
      'AiWebRuntime.probeFrame is required'
    );
  });
});
