import type { DomainModule } from '@namewta/platform-app-runtime';
import type { ApiErrorInfo, ClientContext, HttpClient, SessionStore } from '@namewta/platform-contracts';
import { requireClientContext } from '@namewta/platform-contracts';
import {
  requirePasswordPolicy,
  validatePassword,
  type ClientAuthContext,
  type PasswordPolicyViolation
} from './password-policy';
import { projectClientAuthContextTransport } from './transport';

export * from './password-policy';
export * from './transport';

export interface ApiResponse<T = unknown> {
  code?: number;
  data?: T;
  msg?: string;
  error?: ApiErrorInfo;
}

export interface LoginVerification {
  captchaEnabled: boolean;
  img?: string;
  uuid?: string;
}

export interface LoginPreparation {
  context: ClientAuthContext;
  verification: LoginVerification;
}

export interface PasswordLoginInput {
  code?: string;
  password: string;
  username: string;
  uuid?: string;
}

export interface IdentitySession {
  accessToken: string;
}

export interface RegistrationInput extends PasswordLoginInput {
  confirmPassword?: string;
}

export interface SocialCallbackInput {
  code?: string;
  socialCode?: string;
  socialState?: string;
  source?: string;
  state?: string;
}

export interface SocialCallbackResult {
  accessToken?: string;
  message?: string;
}

export interface IdentityInfo {
  permissions: readonly string[];
  roles: readonly string[];
  user: unknown;
}

export interface ServerMenuMeta {
  activeMenu?: string;
  icon?: string;
  link?: string;
  noCache?: boolean;
  title?: string;
}

export interface ServerMenuNode {
  alwaysShow?: boolean;
  children?: readonly ServerMenuNode[];
  component?: string;
  ext?: string;
  hidden?: boolean;
  meta?: ServerMenuMeta;
  name?: string;
  path: string;
  permissions?: readonly string[];
  query?: string;
  redirect?: string;
}

export type IdentityAccessErrorCode =
  | 'preparation-superseded'
  | 'session-invalidated'
  | 'client-context-unavailable'
  | 'invalid-credentials'
  | 'invalid-login-response'
  | 'invalid-identity-response'
  | 'invalid-menu-response'
  | 'password-policy-unavailable'
  | 'password-policy-violation'
  | 'registration-disabled'
  | 'invalid-verification-response';

export class IdentityAccessError extends Error {
  readonly code: IdentityAccessErrorCode;
  readonly violations: readonly PasswordPolicyViolation[];

  constructor(code: IdentityAccessErrorCode, message: string, violations: readonly PasswordPolicyViolation[] = []) {
    super(message);
    this.name = 'IdentityAccessError';
    this.code = code;
    this.violations = Object.freeze([...violations]);
  }
}

export interface IdentityAccessService {
  readonly client: ClientContext;
  login(input: PasswordLoginInput): Promise<IdentitySession>;
  prepareLogin(): Promise<LoginPreparation>;
}

export interface IdentityAccessManagementService extends IdentityAccessService {
  getClientContext(): Promise<ClientAuthContext>;
  getInfo(): Promise<IdentityInfo>;
  getMenus(): Promise<readonly ServerMenuNode[]>;
  getVerification(): Promise<LoginVerification>;
  logout(): Promise<void>;
  register(input: RegistrationInput): Promise<void>;
  socialCallback(input: SocialCallbackInput): Promise<SocialCallbackResult>;
  socialLogin(input: SocialCallbackInput): Promise<IdentitySession>;
  readonly social: {
    bindingUrl(source: string): Promise<ApiResponse<string>>;
    unlock(socialId: string | number): Promise<ApiResponse>;
  };
}

export interface IdentityAccessServiceOptions {
  client: ClientContext;
  http: HttpClient;
  identity: {
    loadInfo(): Promise<unknown>;
    loadMenus(): Promise<unknown>;
  };
  session: SessionStore;
}

export const adminDomainModule: DomainModule = Object.freeze({
  id: 'admin',
  backendModules: Object.freeze(['wta-admin']),
  capabilities: Object.freeze([
    'client-context',
    'password-login',
    'registration',
    'oauth-callback',
    'identity-info',
    'server-menu',
    'isolated-session'
  ])
});

const clientContextError = () =>
  new IdentityAccessError('client-context-unavailable', '客户端认证配置不可用，无法登录');

function parseClientAuthContext(value: unknown): ClientAuthContext {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw clientContextError();
  const context = value as Record<string, unknown>;
  if (typeof context.clientEnabled !== 'boolean' || typeof context.registerEnabled !== 'boolean') {
    throw clientContextError();
  }
  if (!context.clientEnabled) throw clientContextError();
  const projected = projectClientAuthContextTransport(context);
  const authMode = context.authMode === 'local' || context.authMode === 'sso' || context.authMode === 'both' ? context.authMode : undefined;
  const ssoAuthorizeUrl = typeof context.ssoAuthorizeUrl === 'string' ? context.ssoAuthorizeUrl.trim() : '';
  return Object.freeze({
    ...projected,
    ...(authMode ? { authMode } : {}),
    ...(context.ssoEnabled === true ? { ssoEnabled: true } : {}),
    ...(ssoAuthorizeUrl ? { ssoAuthorizeUrl } : {})
  });
}

function parseVerification(value: unknown): LoginVerification {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new IdentityAccessError('invalid-verification-response', '登录验证码配置不可用');
  }
  const verification = value as Record<string, unknown>;
  if (typeof verification.captchaEnabled !== 'boolean') {
    throw new IdentityAccessError('invalid-verification-response', '登录验证码配置不可用');
  }
  if (!verification.captchaEnabled) return Object.freeze({ captchaEnabled: false });
  const img = typeof verification.img === 'string' ? verification.img.trim() : '';
  const uuid = typeof verification.uuid === 'string' ? verification.uuid.trim() : '';
  if (!img || !uuid) {
    throw new IdentityAccessError('invalid-verification-response', '登录验证码配置不可用');
  }
  return Object.freeze({ captchaEnabled: true, img, uuid });
}

function parseAccessToken(value: unknown): string {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new IdentityAccessError('invalid-login-response', '登录响应缺少访问令牌');
  }
  const token = (value as Record<string, unknown>).access_token;
  if (typeof token !== 'string' || !token.trim()) {
    throw new IdentityAccessError('invalid-login-response', '登录响应缺少访问令牌');
  }
  return token;
}

function requireCredentials(input: PasswordLoginInput): PasswordLoginInput {
  if (!input.username.trim() || !input.password) {
    throw new IdentityAccessError('invalid-credentials', '请输入用户名和密码');
  }
  return input;
}

function parseStringList(value: unknown): readonly string[] {
  if (!Array.isArray(value) || value.some(item => typeof item !== 'string' || !item.trim())) return Object.freeze([]);
  return Object.freeze(value.map(item => item.trim()));
}

function parseIdentityInfo(value: unknown): IdentityInfo {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new IdentityAccessError('invalid-identity-response', '用户身份响应格式无效');
  }
  const info = value as Record<string, unknown>;
  if (!info.user || typeof info.user !== 'object' || Array.isArray(info.user)) {
    throw new IdentityAccessError('invalid-identity-response', '用户身份响应缺少用户信息');
  }
  return Object.freeze({
    user: info.user,
    roles: parseStringList(info.roles),
    permissions: parseStringList(info.permissions)
  });
}

function invalidMenu(path: string): IdentityAccessError {
  return new IdentityAccessError('invalid-menu-response', `服务端菜单响应格式无效: ${path}`);
}

type UnknownObject = { readonly [key: string]: unknown };

function requireMenuRecord(value: unknown, path: string): UnknownObject {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw invalidMenu(path);
  return value as UnknownObject;
}

function menuString(
  menu: UnknownObject,
  key: string,
  path: string,
  options: { nonBlank?: boolean; required?: boolean } = {}
): string | undefined {
  const value = menu[key];
  if ((value === undefined || value === null) && !options.required) return undefined;
  if (typeof value !== 'string' || (options.nonBlank && !value.trim())) throw invalidMenu(`${path}.${key}`);
  return value;
}

function menuBoolean(menu: UnknownObject, key: string, path: string): boolean | undefined {
  const value = menu[key];
  if (value === undefined || value === null) return undefined;
  if (typeof value !== 'boolean') throw invalidMenu(`${path}.${key}`);
  return value;
}

function menuStringList(
  menu: UnknownObject,
  key: string,
  path: string
): readonly string[] | undefined {
  const value = menu[key];
  if (value === undefined || value === null) return undefined;
  if (!Array.isArray(value) || value.some(item => typeof item !== 'string' || !item.trim())) {
    throw invalidMenu(`${path}.${key}`);
  }
  return Object.freeze(value.map(item => item.trim()));
}

function parseMenuMeta(value: unknown, path: string): ServerMenuMeta | undefined {
  if (value === undefined || value === null) return undefined;
  const meta = requireMenuRecord(value, path);
  return Object.freeze({
    ...(menuString(meta, 'activeMenu', path) !== undefined ? { activeMenu: menuString(meta, 'activeMenu', path) } : {}),
    ...(menuString(meta, 'icon', path) !== undefined ? { icon: menuString(meta, 'icon', path) } : {}),
    ...(menuString(meta, 'link', path) !== undefined ? { link: menuString(meta, 'link', path) } : {}),
    ...(menuBoolean(meta, 'noCache', path) !== undefined ? { noCache: menuBoolean(meta, 'noCache', path) } : {}),
    ...(menuString(meta, 'title', path) !== undefined ? { title: menuString(meta, 'title', path) } : {})
  });
}

function parseMenuNode(value: unknown, path: string): ServerMenuNode {
  const menu = requireMenuRecord(value, path);
  const childrenValue = menu.children;
  let children: readonly ServerMenuNode[] | undefined;
  if (childrenValue !== undefined && childrenValue !== null) {
    if (!Array.isArray(childrenValue)) throw invalidMenu(`${path}.children`);
    children = Object.freeze(childrenValue.map((child, index) => parseMenuNode(child, `${path}.children[${index}]`)));
  }
  const meta = parseMenuMeta(menu.meta, `${path}.meta`);
  const alwaysShow = menuBoolean(menu, 'alwaysShow', path);
  const component = menuString(menu, 'component', path, { nonBlank: true });
  const ext = menuString(menu, 'ext', path);
  const hidden = menuBoolean(menu, 'hidden', path);
  const name = menuString(menu, 'name', path);
  const permissions = menuStringList(menu, 'permissions', path);
  const query = menuString(menu, 'query', path);
  const redirect = menuString(menu, 'redirect', path);
  return Object.freeze({
    path: menuString(menu, 'path', path, { nonBlank: true, required: true })!,
    ...(alwaysShow !== undefined ? { alwaysShow } : {}),
    ...(children ? { children } : {}),
    ...(component !== undefined ? { component } : {}),
    ...(ext !== undefined ? { ext } : {}),
    ...(hidden !== undefined ? { hidden } : {}),
    ...(meta ? { meta } : {}),
    ...(name !== undefined ? { name } : {}),
    ...(permissions !== undefined ? { permissions } : {}),
    ...(query !== undefined ? { query } : {}),
    ...(redirect !== undefined ? { redirect } : {})
  });
}

function parseMenus(value: unknown): readonly ServerMenuNode[] {
  if (!Array.isArray(value)) throw invalidMenu('menus');
  return Object.freeze(value.map((menu, index) => parseMenuNode(menu, `menus[${index}]`)));
}

export function createClientSessionKey(appId: string, clientId: string): string {
  if (!appId.trim()) throw new Error('App id is required');
  const client = requireClientContext({ clientId });
  return `namewta:${encodeURIComponent(appId.trim())}:${encodeURIComponent(client.clientId)}:access-token`;
}

export function createIdentityAccessService({
  client: clientInput,
  http,
  identity,
  session
}: IdentityAccessServiceOptions): IdentityAccessManagementService {
  const client = Object.freeze(requireClientContext(clientInput));
  let prepared = false;
  let sessionGeneration = 0;
  let preparationGeneration = 0;
  let verificationGeneration = 0;
  let currentVerification: LoginVerification | undefined;
  let contextAttempt: { generation: number; promise: Promise<ClientAuthContext> } | undefined;
  const assertSessionCurrent = (generation: number) => {
    if (generation !== sessionGeneration) throw new IdentityAccessError('session-invalidated', '会话已结束，请重新登录');
  };
  let context: ClientAuthContext | undefined;

  const loadClientContext = () => {
    // 页头与表单可以同时读取公开配置，共用本次请求，避免互相清除准备状态。
    if (contextAttempt?.generation === sessionGeneration) return contextAttempt.promise;
    const generation = sessionGeneration;
    const preparation = ++preparationGeneration;
    verificationGeneration++;
    prepared = false;
    currentVerification = undefined;
    context = undefined;
    const promise = (async () => {
      const contextResponse = await http.request<ApiResponse<unknown>>({
        url: '/auth/client/context',
        method: 'get',
        headers: { isToken: false }
      });
      assertSessionCurrent(generation);
      if (preparation !== preparationGeneration) throw new IdentityAccessError('preparation-superseded', '认证准备已更新，请重试');
      context = parseClientAuthContext(contextResponse.data);
      return context;
    })();
    contextAttempt = { generation, promise };
    void promise.finally(() => { if (contextAttempt?.promise === promise) contextAttempt = undefined; }).catch(() => undefined);
    return promise;
  };

  const ensureClientContext = async () => context ?? loadClientContext();

  const loadVerification = async () => {
    const generation = sessionGeneration;
    const preparation = preparationGeneration;
    const verificationVersion = ++verificationGeneration;
    prepared = false;
    currentVerification = undefined;
    if (!context) throw clientContextError();
    const verificationResponse = await http.request<ApiResponse<unknown>>({
      url: '/auth/code',
      method: 'get',
      headers: { isToken: false },
      timeout: 20000
    });
    assertSessionCurrent(generation);
    if (preparation !== preparationGeneration || verificationVersion !== verificationGeneration) {
      throw new IdentityAccessError('preparation-superseded', '验证码已更新，请重新输入');
    }
    const verification = parseVerification(verificationResponse.data);
    currentVerification = verification;
    prepared = true;
    return verification;
  };

  return Object.freeze<IdentityAccessManagementService>({
    client,
    social: Object.freeze({
      bindingUrl: (source: string) =>
        http.request<ApiResponse<string>>({
          url: '/auth/binding/' + encodeURIComponent(source),
          method: 'get'
        }),
      unlock: (socialId: string | number) =>
        http.request<ApiResponse>({
          url: '/auth/unlock/' + encodeURIComponent(String(socialId)),
          method: 'post'
        })
    }),
    getClientContext: loadClientContext,
    getVerification: loadVerification,
    async prepareLogin() {
      prepared = false;
      const loadedContext = await loadClientContext();
      const verification = await loadVerification();
      return Object.freeze({ context: loadedContext, verification });
    },
    async login(input) {
      const generation = ++sessionGeneration;
      if (!prepared) throw clientContextError();
      const credentials = requireCredentials(input);
      const response = await http.request<ApiResponse<unknown>>({
        url: '/auth/login',
        method: 'post',
        headers: { isToken: false, repeatSubmit: false },
        data: {
          username: credentials.username,
          password: credentials.password,
          ...(credentials.code ? { code: credentials.code } : {}),
          ...(credentials.uuid ? { uuid: credentials.uuid } : {}),
          clientId: client.clientId,
          grantType: 'password'
        }
      });
      assertSessionCurrent(generation);
      const accessToken = parseAccessToken(response.data);
      session.setToken(accessToken);
      return Object.freeze({ accessToken });
    },
    async register(input) {
      if (!prepared || !context) throw clientContextError();
      if (!context.registerEnabled) {
        throw new IdentityAccessError('registration-disabled', '当前客户端未开放注册');
      }
      const credentials = requireCredentials(input);
      let policy;
      try {
        policy = requirePasswordPolicy(context);
      } catch {
        throw new IdentityAccessError('password-policy-unavailable', '密码策略配置不可用');
      }
      const violations = validatePassword(policy, credentials.password);
      if (violations.length) {
        throw new IdentityAccessError('password-policy-violation', '密码不符合安全策略', violations);
      }
      if (input.confirmPassword !== undefined && input.confirmPassword !== input.password) {
        throw new IdentityAccessError('invalid-credentials', '两次输入的密码不一致');
      }
      // 后端先消费一次性验证码再校验其他注册条件；失败或网络结果不明也必须换一张。
      if (currentVerification?.captchaEnabled) {
        prepared = false;
        currentVerification = undefined;
      }
      await http.request<ApiResponse<unknown>>({
        url: '/auth/register',
        method: 'post',
        headers: { isToken: false, repeatSubmit: false },
        data: {
          username: credentials.username,
          password: credentials.password,
          ...(credentials.code ? { code: credentials.code } : {}),
          ...(credentials.uuid ? { uuid: credentials.uuid } : {}),
          clientId: client.clientId
        }
      });
    },
    async getInfo() {
      return parseIdentityInfo(await identity.loadInfo());
    },
    async getMenus() {
      return parseMenus(await identity.loadMenus());
    },
    async socialCallback(input) {
      const generation = ++sessionGeneration;
      await ensureClientContext();
      const response = await http.request<ApiResponse<unknown>>({
        url: '/auth/social/callback',
        method: 'post',
        data: { ...input, clientId: client.clientId, grantType: 'social' }
      });
      assertSessionCurrent(generation);
      let accessToken: string | undefined;
      if (response.data && typeof response.data === 'object' && !Array.isArray(response.data)) {
        const candidate = (response.data as Record<string, unknown>).access_token;
        if (typeof candidate === 'string' && candidate.trim()) {
          accessToken = candidate;
          session.setToken(accessToken);
        }
      }
      return Object.freeze({
        ...(accessToken ? { accessToken } : {}),
        ...(response.msg ? { message: response.msg } : {})
      });
    },
    async socialLogin(input) {
      const generation = ++sessionGeneration;
      await ensureClientContext();
      const response = await http.request<ApiResponse<unknown>>({
        url: '/auth/login',
        method: 'post',
        headers: { isToken: false, repeatSubmit: false },
        data: { ...input, clientId: client.clientId, grantType: 'social' }
      });
      assertSessionCurrent(generation);
      const accessToken = parseAccessToken(response.data);
      session.setToken(accessToken);
      return Object.freeze({ accessToken });
    },
    async logout() {
      const generation = ++sessionGeneration;
      const previousToken = session.getToken();
      prepared = false;
      preparationGeneration++;
      verificationGeneration++;
      currentVerification = undefined;
      context = undefined;
      try {
        await http.request<ApiResponse<unknown>>({ url: '/auth/logout', method: 'post', timeout: 10000,
          headers: { repeatSubmit: false } });
      } finally {
        // A late logout must not erase a session established by a later login.
        if (generation === sessionGeneration && session.getToken() === previousToken) session.clear();
      }
    }
  });
}
