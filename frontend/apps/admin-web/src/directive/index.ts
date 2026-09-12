import type { App } from 'vue';
import { installWebPermissionHost } from '@namewta/web-kit-permission';
import { createAdminAccessEvaluator } from '@/application/access';
import copyText from './common/copyText';

export default (app: App) => {
  app.directive('copyText', copyText);
  installWebPermissionHost(app, createAdminAccessEvaluator);
};
