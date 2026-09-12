<template>
  <open-api-workspace :runtime="runtime" :scope="{ kind: 'current-user' }" />
</template>

<script setup lang="ts">
import { OpenApiWorkspace, type OpenApiWorkspaceRuntime } from '@namewta/web-domain-system/open-api';
import { createAdminAccessEvaluator } from '@/application/access';
import modal from '@/application/host/feedback';
import { openApiService } from '@/application/services';

const runtime: OpenApiWorkspaceRuntime = {
  openApi: openApiService,
  confirm: async message => {
    await modal.confirm(message);
  },
  success: message => modal.msgSuccess(message),
  error: message => modal.msgError(message),
  hasPermission: permission => createAdminAccessEvaluator().hasPermission(permission),
  copyText: async value => {
    if (!navigator.clipboard?.writeText) throw new Error('Clipboard API unavailable');
    await navigator.clipboard.writeText(value);
  }
};
</script>
