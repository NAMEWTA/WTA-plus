import { adminDomainModule } from '@namewta/domain-admin';
import { profileDomainModule } from '@namewta/domain-profile';
import { workflowDomainModule } from '@namewta/domain-workflow';
import { composeAppRuntime, AppRuntimeError, type WebComponentRegistration } from '@namewta/platform-app-runtime';
import { safeSsoReturnTo } from '@namewta/platform-auth';
import { createAdminWebDomain } from '@namewta/web-domain-admin';
import { createProfileSelfWebDomain, createProfileReviewWebDomain } from '@namewta/web-domain-profile';
import { createWorkflowTaskWebDomain } from '@namewta/web-domain-workflow/task';
import { ElMessage, ElMessageBox } from 'element-plus';
import { hasPermission } from '@/application/access';
import { identityAccessService, profileService, workflowService, uploadProfileMaterial } from '@/application/services';
import { getToken } from '@/application/session';
import { createAppSocialRuntime } from '@/application/social';

const identityManifest = createAdminWebDomain({
  service: identityAccessService,
  title: '用户登录',
  description: '登录用户中心，完成个人或企业认证。',
  startSocialLogin: providerKey =>
    createAppSocialRuntime().start(
      providerKey,
      'LOGIN',
      new URLSearchParams(window.location.search).get('redirect') || '/profile'
    ),
  onAuthenticated: async () => {
    const target = new URLSearchParams(window.location.search).get('redirect') || '/profile';
    const { default: router } = await import('./index');
    await router.replace(safeSsoReturnTo(target));
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
const feedback = {
  confirm: (message: string) => ElMessageBox.confirm(message, '确认').then(() => undefined),
  error: (message: string) => {
    ElMessage.error(message);
  },
  success: (message: string) => {
    ElMessage.success(message);
  },
  warning: (message: string) => {
    ElMessage.warning(message);
  }
};
const reviewManifest = createProfileReviewWebDomain({
  ...feedback,
  service: profileService,
  hasPermission,
  closeCurrentPage: async () => {
    const { default: router } = await import('./index');
    await router.push('/taskWaiting');
  },
  downloadMaterial: access => {
    const link = document.createElement('a');
    link.href = access.url;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.click();
  }
});
const taskManifest = createWorkflowTaskWebDomain({
  ...feedback,
  service: workflowService,
  openWorkflowForm: async (task, actionable) => {
    const [{ default: router }, { useNavigationStore }] = await Promise.all([
      import('./index'),
      import('@/store/navigation')
    ]);
    const componentKey = task.formPath.replace(/^\/+/, '');
    const path = useNavigationStore().componentPaths[componentKey];
    if (!path || !router.resolve(path).matched.some(record => record.name))
      return feedback.error('当前客户端未配置此任务的办理页面');
    await router.push({
      path,
      query: { id: task.businessId, taskId: String(task.taskId), type: actionable ? 'approval' : 'view' }
    });
  }
});
const runtime = composeAppRuntime({
  appId: 'home-web',
  domainModules: [adminDomainModule, profileDomainModule, workflowDomainModule],
  manifests: [identityManifest, selfManifest, reviewManifest, taskManifest],
  selectedDomainIds: ['admin', 'profile', 'workflow'],
  selectedManifestIds: [
    'web-domain-admin',
    'web-domain-profile-self',
    'web-domain-profile-review',
    'web-domain-workflow-tasks'
  ]
});
export function resolveHomeWebRegistration(
  componentKey: string,
  domainId: string
): WebComponentRegistration | undefined {
  try {
    return runtime.resolve({ componentKey, domainId });
  } catch (error) {
    if (
      error instanceof AppRuntimeError &&
      (error.code === 'missing-component-key' || error.code === 'unselected-domain')
    )
      return undefined;
    throw error;
  }
}
export { getToken };
