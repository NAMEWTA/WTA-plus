import { describe, expect, it } from 'vitest';
import { extractLegacyAuthProviders, readLegacyAuthImportItems } from './legacy';
describe('explicit legacy identity configuration import', () => {
  it('extracts only JustAuth providers and normalizes legacy YAML keys', () => {
    expect(
      extractLegacyAuthProviders([
        {
          spring: { datasource: { password: 'do-not-send' } },
          justauth: {
            address: 'https://old.example',
            type: {
              github: {
                'client-id': 'external',
                'client-secret': 'secret',
                'redirect-uri': 'https://home.example/social-callback'
              }
            }
          }
        }
      ])
    ).toEqual({
      github: { clientId: 'external', clientSecret: 'secret', redirectUri: 'https://home.example/social-callback' }
    });
  });
  it('rejects ambiguous or nested input before sending it', () => {
    expect(() => extractLegacyAuthProviders([{ type: { github: { options: { secret: 'x' } } } }])).toThrow();
    expect(() => extractLegacyAuthProviders([{ spring: {} }])).toThrow();
    expect(() => extractLegacyAuthProviders([{ type: { github: {} } }, { type: { github: {} } }])).toThrow();
  });
  it('projects a secret-free preview even if an upstream response includes secret material', () => {
    const rows = readLegacyAuthImportItems({
      items: [{ source: 'github', status: 'READY', secretConfigured: true, clientSecret: 'not-for-display' }]
    });
    expect(rows[0]).not.toHaveProperty('clientSecret');
    expect(rows[0].secretConfigured).toBe(true);
  });
});
