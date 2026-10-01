<template>
  <el-dialog
    :model-value="visible"
    title="导入旧第三方登录配置"
    width="min(900px, 96vw)"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="
      value => {
        if (!value && !busy) emit('close');
      }
    "
  >
    <p>选择旧 YAML 或 JSON 文件，核对预览后导入。仅导入其中的 justauth.type；示例空值和已有配置会跳过。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-form label-position="top">
      <el-form-item label="目标业务客户端 ID"><el-input v-model="businessClientId" :disabled="busy" /></el-form-item>
      <el-form-item label="首次登录策略">
        <el-radio-group v-model="firstLoginPolicy" :disabled="busy">
          <el-radio value="BIND_ONLY">仅已绑定账号</el-radio>
          <el-radio value="AUTO_REGISTER">自动创建账号（Home）</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="旧配置文件">
        <input type="file" accept=".yaml,.yml,.json" :disabled="busy" aria-label="旧配置文件" @change="readFile" />
      </el-form-item>
    </el-form>
    <el-table v-if="items.length" :data="items">
      <el-table-column label="来源" prop="source" />
      <el-table-column label="Client ID" prop="externalClientId" show-overflow-tooltip />
      <el-table-column label="回调地址" prop="redirectUri" show-overflow-tooltip />
      <el-table-column label="密钥" width="90">
        <template #default="{ row }">{{ row.secretConfigured ? '已提供' : '未提供' }}</template>
      </el-table-column>
      <el-table-column label="结果" width="95">
        <template #default="{ row }">{{ statusLabel(row.status) }}</template>
      </el-table-column>
      <el-table-column label="说明" prop="message" show-overflow-tooltip />
    </el-table>
    <template #footer>
      <el-button :disabled="busy" @click="emit('close')">关闭</el-button>
      <el-button :disabled="busy || !configuration || !businessClientId.trim()" @click="preview">预览导入</el-button>
      <el-button type="primary" :loading="busy" :disabled="!previewReady" @click="execute">确认导入</el-button>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import {
  extractLegacyAuthProviders,
  type LegacyAuthImportInput,
  type LegacyAuthImportItem
} from '@namewta/domain-system';
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue';
import { parseAllDocuments } from 'yaml';
import type { SystemWebRuntime } from '../runtime';
const { visible, runtime } = defineProps<{ visible: boolean; runtime: SystemWebRuntime }>();
const emit = defineEmits<{ close: []; imported: [] }>();
const businessClientId = ref('');
const firstLoginPolicy = ref<LegacyAuthImportInput['firstLoginPolicy']>('BIND_ONLY');
const configuration = shallowRef<LegacyAuthImportInput['type']>();
const items = ref<LegacyAuthImportItem[]>([]);
const busy = ref(false);
const error = ref('');
const previewed = ref(false);
const previewReady = computed(() => previewed.value && items.value.some(item => item.status === 'READY'));
let generation = 0;
let controller: AbortController | undefined;
let active = true;
const allowed = () =>
  runtime.hasPermission('system:authProvider:add') && runtime.hasPermission('system:authRegistration:add');
function statusLabel(value: unknown) {
  return (
    (
      { READY: '可导入', SKIPPED: '已跳过', EXISTS: '已存在', IMPORTED: '已导入', INVALID: '无效' } as Record<
        string,
        string
      >
    )[String(value)] ?? '未知'
  );
}
function invalidate() {
  generation++;
  controller?.abort();
  previewed.value = false;
  items.value = [];
  busy.value = false;
  error.value = '';
}
watch([businessClientId, firstLoginPolicy], invalidate);
watch(
  () => visible,
  value => {
    if (!value) {
      invalidate();
      configuration.value = undefined;
    }
  }
);
watch(
  () => runtime.sessionSnapshot().generation,
  () => {
    invalidate();
    configuration.value = undefined;
    emit('close');
  }
);
async function readFile(event: Event) {
  invalidate();
  configuration.value = undefined;
  const target = event.target;
  if (!(target instanceof HTMLInputElement)) return;
  const file = target.files?.[0];
  if (!file) return;
  const current = generation;
  if (file.size > 1024 * 1024) {
    error.value = '配置文件不能超过 1 MB';
    return;
  }
  try {
    const text = await file.text();
    if (!active || current !== generation) return;
    const docs = parseAllDocuments(text);
    if (docs.some(doc => doc.errors.length)) throw new Error();
    configuration.value = extractLegacyAuthProviders(docs.map(doc => doc.toJS({ maxAliasCount: 100 })));
  } catch {
    if (active && current === generation)
      error.value = '无法读取配置，请选择包含 justauth.type 的有效 YAML 或 JSON 文件';
  }
}
async function run(dryRun: boolean) {
  if (busy.value || !allowed() || !configuration.value || !businessClientId.value.trim()) return;
  const current = ++generation;
  controller?.abort();
  controller = new AbortController();
  busy.value = true;
  error.value = '';
  try {
    const result = await runtime.service.authConfig.importLegacy(
      {
        businessClientId: businessClientId.value.trim(),
        firstLoginPolicy: firstLoginPolicy.value,
        dryRun,
        type: configuration.value
      },
      controller.signal
    );
    if (!active || current !== generation) return;
    items.value = result;
    previewed.value = dryRun;
    if (!dryRun) {
      configuration.value = undefined;
      emit('imported');
      runtime.success('导入完成，请检查结果');
    }
  } catch {
    if (active && current === generation) error.value = '导入未完成，请检查配置、权限和客户端设置后重新预览';
  } finally {
    if (active && current === generation) busy.value = false;
  }
}
const preview = () => run(true);
async function execute() {
  if (!previewReady.value) return;
  try {
    await runtime.confirm('确认将预览中可导入的配置写入数据库？已有配置不会覆盖。');
  } catch {
    return;
  }
  if (active && visible && previewReady.value) await run(false);
}
onBeforeUnmount(() => {
  active = false;
  invalidate();
  configuration.value = undefined;
});
</script>
