<template>
  <section class="self-materials" aria-label="认证材料" :aria-busy="state.busy.value">
    <h2>认证材料</h2>
    <p>支持 JPG、PNG、PDF，单份不超过 10 MiB，最多 10 份。文件登记成功后才能提交。</p>
    <p v-if="ownerId === undefined">请先保存草稿，再上传材料。</p>
    <p v-else-if="!editable">当前申请为只读状态，可查看已登记材料。</p>
    <p v-else-if="!runtime.hasPermission('system:oss:upload')">当前账号没有材料上传权限，请联系管理员。</p>
    <div v-if="state.error.value" role="alert" class="material-error">{{ state.error.value }}</div>
    <el-button v-if="!state.ready.value && !state.loading.value" @click="state.load">重新加载材料</el-button>
    <div v-if="state.phase.value === 'uploading'" class="material-progress">
      <el-progress :percentage="Math.round(state.progress.value)" />
      <el-button @click="state.cancel">取消上传</el-button>
    </div>
    <p v-if="state.phase.value === 'registering'">正在登记材料，请稍候…</p>
    <el-button v-if="state.pending.value && !state.busy.value" @click="state.retryRegistration">重试登记</el-button>
    <article v-for="tag in state.tags.value" :key="tag.materialNodeId" :data-material-tag="tag.materialTagCode" :class="{ 'material-row--missing': state.missingTag.value === tag.materialTagCode }" class="material-row" tabindex="-1">
      <div class="material-row__title">
        <h3>{{ tag.nodeName }}</h3>
        <el-tag v-if="minimum(tag.materialTagCode)" type="danger" size="small">必填 · 至少 {{ minimum(tag.materialTagCode) }} 份</el-tag>
      </div>
      <ul v-if="references(tag).length">
        <li v-for="item in references(tag)" :key="item.materialRefId">
          <span>{{ item.fileName }}</span><el-tag type="success" size="small">已登记</el-tag>
          <el-button link type="primary" :disabled="previewing" @click="preview(item)">预览</el-button>
          <el-button v-if="editable" link type="danger" :disabled="locked || state.busy.value || !!state.pending.value || !state.writable.value" @click="state.remove(item)">移除</el-button>
          <label v-if="editable">替换文件<input type="file" accept=".jpg,.jpeg,.png,.pdf" :aria-label="`替换${item.fileName}`" :disabled="locked || !state.canUpload.value" @change="selectFile(tag, $event, item.materialRefId)" /></label>
        </li>
      </ul>
      <label v-if="editable" class="material-picker">{{ references(tag).length ? '补充文件' : '选择文件' }}
        <input type="file" accept=".jpg,.jpeg,.png,.pdf" :aria-label="`上传${tag.nodeName}`" :disabled="locked || !state.canUpload.value" @change="selectFile(tag, $event)" />
      </label>
    </article>
    <el-dialog v-model="previewVisible" title="材料预览" width="min(90vw, 800px)" destroy-on-close @closed="previewUrl = ''">
      <img v-if="previewImage" :src="previewUrl" alt="认证材料预览" class="material-preview" @error="previewFailed" />
      <p v-else>使用下方链接打开材料。</p>
      <a v-if="previewUrl" :href="previewUrl" target="_blank" rel="noopener noreferrer">打开材料</a>
      <el-button :loading="previewing" @click="retryPreview">重新获取预览</el-button>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import type { Identifier, MaterialReference, ProfileType } from '@namewta/domain-profile';
import type { MaterialNode } from '@namewta/domain-profile/material-tags';
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';
import type { ProfileSelfWebRuntime } from './runtime';
import { useSelfMaterials } from './useSelfMaterials';

const props = defineProps<{ runtime: ProfileSelfWebRuntime; profileType: ProfileType; ownerId?: Identifier; documentTypeCode: string; handlerIsLegalRepresentative: boolean; editable: boolean; locked: boolean }>();
const emit = defineEmits<{ busy: [value: boolean]; pending: [value: boolean] }>();
const state = useSelfMaterials(props.runtime, () => ({ profileType: props.profileType, ownerId: props.ownerId, documentTypeCode: props.documentTypeCode, handlerIsLegalRepresentative: props.handlerIsLegalRepresentative, editable: props.editable }));
const previewVisible = ref(false);
const previewUrl = ref('');
const previewImage = ref(false);
const previewing = ref(false);
let selected: MaterialReference | undefined;
let previewGeneration = 0;
const minimum = (code: string | null) => state.requirements.value.find(item => item.materialTagCode === code)?.minimumCount ?? 0;
const references = (tag: MaterialNode) => state.materials.value.filter(item => String(item.materialNodeId) === String(tag.materialNodeId));
watch(state.busy, value => emit('busy', value), { immediate: true });
watch(state.pending, value => emit('pending', !!value));
watch(() => [props.ownerId, props.profileType], () => { ++previewGeneration; previewUrl.value = ''; previewVisible.value = false; previewing.value = false; selected = undefined; });
watch(previewVisible, value => { if (!value) { ++previewGeneration; previewing.value = false; } });

function selectFile(tag: MaterialNode, event: Event, replaces?: Identifier) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (file) void state.upload(tag, file, replaces);
}
async function preview(item: MaterialReference) {
  const epoch = ++previewGeneration;
  selected = item;
  previewing.value = true;
  const access = await state.access(item);
  if (epoch !== previewGeneration) return;
  previewing.value = false;
  if (!access) return;
  previewUrl.value = access.url;
  previewImage.value = item.mimeType.startsWith('image/');
  previewVisible.value = true;
}
function retryPreview() { if (selected) void preview(selected); }
function previewFailed() { previewUrl.value = ''; props.runtime.warning('预览地址可能已过期，请重新获取预览'); }
async function focusMissing() {
  await nextTick();
  document.querySelector<HTMLElement>(`.self-materials [data-material-tag="${state.missingTag.value}"]`)?.focus();
}
function validate() { const valid = state.validate(); if (!valid) void focusMissing(); return valid; }
function showError(cause: unknown) { const message = state.describeError(cause, '提交失败，请稍后重试'); void focusMissing(); return message; }
onBeforeUnmount(() => { ++previewGeneration; });
defineExpose({ validate, showError });
</script>

<style scoped>
.self-materials { margin-top: 24px; border-top: 1px solid #dbe4ea; padding-top: 12px; }
.self-materials h2 { font-size: 20px; }
.self-materials > p { color: #64748b; margin: 10px 0; }
.material-row { padding: 16px 0; border-bottom: 1px solid #e2e8f0; }
.material-row--missing { outline: 2px solid #dc2626; outline-offset: 4px; }
.material-row__title, .material-row li { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.material-row h3 { margin: 0; font-size: 16px; }
.material-row ul { padding: 0; list-style: none; }
.material-row li { margin: 10px 0; }
.material-picker { display: block; margin-top: 12px; }
.material-picker input { display: block; margin-top: 6px; max-width: 100%; }
.material-error { color: #b91c1c; margin: 12px 0; }
.material-progress { max-width: 480px; }
.material-preview { display: block; max-width: 100%; max-height: 65vh; margin: 0 auto 12px; }
</style>
