<template>
  <div class="p-2 app-container iframe-page">
    <el-alert v-if="failure" :title="failure" type="error" :closable="false" show-icon />
    <div v-else-if="safeUrl" class="iframe-page__inner"><IFrame :src="safeUrl" /></div>
  </div>
</template>
<script setup lang="ts">
import { monitorPermissions, type ExternalMonitorTarget } from '@namewta/domain-system/monitor';
import { ref } from 'vue';
import { createAdminAccessEvaluator } from '@/application/access';
import { monitorService } from '@/application/services';
import IFrame from '@/components/iFrame/index.vue';

const props = defineProps<{ target: ExternalMonitorTarget }>();
const externalUrls: Readonly<Record<ExternalMonitorTarget, string | undefined>> = Object.freeze({
  'monitor-admin': import.meta.env.VITE_APP_MONITOR_ADMIN,
  'snail-job': import.meta.env.VITE_APP_SNAILJOB_ADMIN,
  'snail-ai': import.meta.env.VITE_APP_SNAILAI_ADMIN,
  nacos: import.meta.env.VITE_APP_NACOS_ADMIN
});
const failure = ref('');
const safeUrl = ref('');
try {
  safeUrl.value = monitorService.externalIntent(
    props.target,
    externalUrls[props.target],
    createAdminAccessEvaluator().hasPermission(monitorPermissions[props.target])
  ).url;
} catch (error) {
  failure.value = error instanceof Error ? error.message : '运维入口不可用';
}
</script>
<style scoped>
.iframe-page__inner {
  overflow: hidden;
  border-radius: 8px;
  background: var(--app-surface-bg);
  box-shadow: var(--app-shadow-sm);
}
</style>
