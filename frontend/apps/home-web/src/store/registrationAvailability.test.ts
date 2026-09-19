import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const fixture = vi.hoisted(() => ({ getClientContext: vi.fn() }));
vi.mock('@/application/services', () => ({ identityAccessService: fixture }));
import { useRegistrationAvailabilityStore } from './registrationAvailability';

const policy = { minimumLength: 8, maximumLength: 30, requiredCharacterClasses: ['UPPERCASE', 'LOWERCASE', 'DIGIT', 'SPECIAL'], allowedSpecialCharacters: '@$!%*?&' };

describe('public registration availability', () => {
  beforeEach(() => { setActivePinia(createPinia()); vi.clearAllMocks(); });

  it('fails closed until an enabled Client and password policy are loaded', async () => {
    const store = useRegistrationAvailabilityStore();
    expect(store.enabled).toBe(false);
    fixture.getClientContext.mockResolvedValue({ clientEnabled: true, registerEnabled: true, passwordPolicy: policy });
    await store.load(); expect(store.enabled).toBe(true);
    fixture.getClientContext.mockRejectedValue(new Error('owned offline'));
    await store.load(); expect(store.enabled).toBe(false);
    fixture.getClientContext.mockResolvedValue({ clientEnabled: true, registerEnabled: true });
    await store.load(); expect(store.enabled).toBe(false);
  });

  it('discards a public read invalidated by the registration page or a new session', async () => {
    let finish!: (value: unknown) => void;
    fixture.getClientContext.mockReturnValue(new Promise(resolve => { finish = resolve; }));
    const store = useRegistrationAvailabilityStore(); const pending = store.load();
    store.reset();
    finish({ clientEnabled: true, registerEnabled: true, passwordPolicy: policy });
    await pending;
    expect(store.enabled).toBe(false);
  });
});
