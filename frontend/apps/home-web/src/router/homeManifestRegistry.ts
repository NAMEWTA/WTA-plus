import { adminDomainModule } from '@namewta/domain-admin';
import { profileDomainModule } from '@namewta/domain-profile';
import { composeAppRuntime, AppRuntimeError, type WebComponentRegistration } from '@namewta/platform-app-runtime';
import { createAdminWebDomain } from '@namewta/web-domain-admin';
import { createProfileSelfWebDomain } from '@namewta/web-domain-profile';
import { identityAccessService, profileService, uploadProfileMaterial } from '@/application/services';
import { homeSso, homeSsoRedirectUri } from '@/application/sso';
import { hasPermission } from '@/application/access';
import { getToken } from '@/application/session';
import { ElMessage, ElMessageBox } from 'element-plus';

const identityManifest = createAdminWebDomain({
  service: identityAccessService,
  title: '用户登录',
  description: '登录用户中心，完成个人或企业认证。',
  startSsoLogin: ({ authorizeUrl }) =>
    homeSso.startSsoLogin({
      authorizeUrl,
      clientId: import.meta.env.VITE_APP_CLIENT_ID,
      redirectUri: homeSsoRedirectUri(),
      returnTo: new URLSearchParams(window.location.search).get('redirect') || '/profile'
    }),
  onAuthenticated: () => {
    window.location.href = `${import.meta.env.VITE_APP_CONTEXT_PATH}profile`;
  }
});
const selfManifest = createProfileSelfWebDomain({
  service: profileService,
  uploadMaterial: uploadProfileMaterial,
  hasPermission,
  confirm: message => ElMessageBox.confirm(message, '确认').then(() => undefined),
  error: message => ElMessage.error(message),
  success: message => ElMessage.success(message),
  warning: message => ElMessage.warning(message)
});
const runtime = composeAppRuntime({
  appId: 'home-web',
  domainModules: [adminDomainModule, profileDomainModule],
  manifests: [identityManifest, selfManifest],
  selectedDomainIds: ['admin', 'profile'],
  selectedManifestIds: ['web-domain-admin', 'web-domain-profile-self']
});
export function resolveHomeWebRegistration(componentKey: string, domainId: string): WebComponentRegistration | undefined {
  try { return runtime.resolve({ componentKey, domainId }); } catch (error) { if (error instanceof AppRuntimeError && (error.code === 'missing-component-key' || error.code === 'unselected-domain')) return undefined; throw error; }
}
export { getToken };
