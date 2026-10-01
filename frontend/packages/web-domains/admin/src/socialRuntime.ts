import type { IdentityAccessManagementService, SocialLoginResult, SocialPurpose } from '@namewta/domain-admin';
import {
  createSocialTransactionStore,
  safeSsoReturnTo,
  sha256Bytes,
  type SocialTransactionStorage
} from '@namewta/platform-auth';

export interface SocialWebPorts {
  service: IdentityAccessManagementService;
  storage: SocialTransactionStorage;
  namespace: string;
  defaultReturnPath: string;
  bindingReturnPath: string;
  owner(): string;
  snapshot(): string;
  navigateExternal(url: string): void;
  navigate(path: string): Promise<void>;
  acceptToken(token: string): void;
  clearSession(): void;
}

/** App 提供会话与导航，Admin/Home 共用第三方回调、补资料与退出行为。 */
export function createSocialWebRuntime(ports: SocialWebPorts) {
  const ownerFingerprint = async () => {
    const owner = ports.owner();
    if (!owner) return '';
    const bytes = sha256Bytes(new TextEncoder().encode(owner));
    return Array.from(new Uint8Array(bytes), byte => byte.toString(16).padStart(2, '0')).join('');
  };
  let bindingRequired = false;
  const transactions = createSocialTransactionStore(ports.storage, ports.namespace);
  let active = true;
  let completing:
    | { registrationTicket: string; transactionKey: string; returnPath: string; snapshot: string }
    | undefined;
  const current = (snapshot: string) => active && ports.snapshot() === snapshot;
  const complete = async (result: SocialLoginResult, returnPath: string, snapshot: string) => {
    if (!current(snapshot)) throw new Error('当前会话已改变，请重新开始。');
    bindingRequired = result.nextAction === 'BIND_REQUIRED';
    if (result.nextAction === 'LOGIN_COMPLETE') {
      completing = undefined;
      ports.acceptToken(result.accessToken);
      await ports.navigate(returnPath);
    }
    return result;
  };
  return {
    service: ports.service,
    async start(providerKey: string, purpose: SocialPurpose = 'LOGIN', returnPath = ports.defaultReturnPath) {
      const snapshot = ports.snapshot();
      const owner = await ownerFingerprint();
      if (purpose === 'BIND' && !owner) throw new Error('请先登录需要绑定的账号');
      const target = safeSsoReturnTo(purpose === 'BIND' ? ports.bindingReturnPath : returnPath);
      const result = await ports.service.external.authorize({ providerKey, purpose, returnPath: target });
      if (!current(snapshot)) return;
      transactions.save({
        ...result,
        providerKey,
        purpose,
        returnPath: target,
        owner,
        clientId: ports.service.client.clientId,
        expiresAt: Date.now() + result.expiresIn * 1000
      });
      ports.navigateExternal(result.authorizationUrl);
    },
    async callback(search: string): Promise<SocialLoginResult | { nextAction: 'BOUND' }> {
      const snapshot = ports.snapshot();
      const { transaction, code } = transactions.consume(search, ports.service.client.clientId);
      if ((transaction.purpose === 'BIND' && !transaction.owner) || transaction.owner !== (await ownerFingerprint()))
        throw new Error('绑定账号已改变，请登录原账号后重新绑定。');
      const input = {
        source: transaction.providerKey,
        socialCode: code,
        socialState: transaction.state,
        transactionKey: transaction.transactionKey
      };
      if (transaction.purpose === 'BIND') {
        await ports.service.external.bind(input);
        if (!current(snapshot)) throw new Error('当前会话已改变');
        await ports.navigate(transaction.returnPath);
        return { nextAction: 'BOUND' };
      }
      const result = await ports.service.external.login(input);
      if (!current(snapshot)) throw new Error('当前会话已改变，请重新开始。');
      if (result.nextAction === 'COMPLETE_PROFILE')
        completing = {
          registrationTicket: result.registrationTicket,
          transactionKey: transaction.transactionKey,
          returnPath: transaction.returnPath,
          snapshot
        };
      return complete(result, transaction.returnPath, snapshot);
    },
    async register(phoneNumber: string) {
      if (!completing || !current(completing.snapshot)) throw new Error('补充资料流程已失效，请重新登录。');
      const pending = completing;
      const result = await ports.service.external.register({
        phoneNumber,
        registrationTicket: pending.registrationTicket,
        transactionKey: pending.transactionKey
      });
      return complete(result, pending.returnPath, pending.snapshot);
    },
    async globalLogout() {
      const snapshot = ports.snapshot();
      const result = await ports.service.external.logout();
      if (!current(snapshot)) return;
      transactions.saveLogout(result.state);
      ports.clearSession();
      ports.navigateExternal(result.endSessionUrl);
    },
    logoutCallback: (search: string) => transactions.consumeLogout(search),
    returnToLogin: () =>
      ports.navigate(bindingRequired ? `/login?redirect=${encodeURIComponent(ports.bindingReturnPath)}` : '/login'),
    dispose() {
      active = false;
      completing = undefined;
    }
  };
}
export type SocialWebRuntime = ReturnType<typeof createSocialWebRuntime>;
