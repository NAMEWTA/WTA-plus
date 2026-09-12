import type { WebPermissionContribution, WebRegistration } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';
import {
  require${BusinessName}WebRuntime,
  type ${BusinessName}WebRuntime
} from './runtime';

export const ${businessName}PermissionContribution: WebPermissionContribution = Object.freeze({
  id: '${moduleName}-${businessName}',
  permissions: Object.freeze([
    '${permissionPrefix}:list',
    '${permissionPrefix}:query',
    '${permissionPrefix}:add',
    '${permissionPrefix}:edit',
    '${permissionPrefix}:remove'<#if enableExport>,
    '${permissionPrefix}:export'</#if>
  ])
});

export function create${BusinessName}WebRegistration(
  runtimeInput: ${BusinessName}WebRuntime | undefined
): WebRegistration<Component> {
  const runtime = require${BusinessName}WebRuntime(runtimeInput);
  return Object.freeze({
    id: '${moduleName}-${businessName}',
    componentKey: '${moduleName}/${businessName}/index',
    componentName: '${BusinessName}',
    load: async (): Promise<Component> => {
      const page = (await import('./${BusinessName}Page.vue')).default;
      return defineComponent({ name: '${BusinessName}', setup: () => () => h(page, { runtime }) });
    }
  });
}
