<template>
  <nav aria-label="任务测试控制">
    <button @click="dialog?.open('A')">打开 A</button>
    <button @click="dialog?.open('B')">打开 B</button>
    <button @click="dialog?.close()">关闭任务</button>
    <button @click="mounted = false">卸载任务</button>
  </nav>
  <output aria-label="完成次数">{{ completed }}</output>
  <ProcessActionDialog v-if="mounted" ref="dialog" :runtime="runtime" @completed="completed++" />
</template>
<script setup lang="ts">
import { h, ref } from 'vue';
import { ElMessageBox } from 'element-plus';
import { createWorkflowDefinitionService } from '@namewta/domain-workflow';
import type { WorkflowWebRuntime } from '../../../runtime';
import ProcessActionDialog from '../../ProcessActionDialog.vue';

const mounted = ref(true);
const completed = ref(0);
const dialog = ref<InstanceType<typeof ProcessActionDialog>>();
type WorkflowHttpClient = Parameters<typeof createWorkflowDefinitionService>[0];
type HttpRequest = Parameters<WorkflowHttpClient['request']>[0];
const service = createWorkflowDefinitionService({
  async request<T>(request: HttpRequest): Promise<T> {
    const url = new URL(request.url, location.origin);
    for (const [key, value] of Object.entries(typeof request.params === 'object' && request.params !== null ? request.params : {})) if (value !== undefined) url.searchParams.set(key, String(value));
    const response = await fetch(url, { method: request.method ?? 'GET', headers: { 'content-type': 'application/json' }, ...(request.data === undefined ? {} : { body: JSON.stringify(request.data) }) });
    const data = await response.json();
    if (data.code !== 200) throw new Error(data.msg ?? '请求失败');
    return data as T;
  }
});
const runtime: WorkflowWebRuntime = {
  service, confirm: async message => { await ElMessageBox.confirm(message, '提示', { confirmButtonText: '确定', cancelButtonText: '取消' }); }, success: () => undefined,
  error: () => undefined, chartUrl: () => '', closeCurrentPage: () => undefined, closeDesigner: () => undefined,
  designUrl: () => '', dicts: () => ({}), download: () => undefined, downloadAttachment: () => undefined,
  resolveAttachments: async () => [], fileUpload: () => h('span', '附件测试接缝'), treePanel: () => h('div')
};
</script>
<style scoped>
nav { position: fixed; top: 0; z-index: 100000; padding: 8px; background: white; }
output { position: fixed; bottom: 0; }
</style>
