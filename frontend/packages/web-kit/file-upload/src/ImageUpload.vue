<template>
  <div class="component-upload-image">
    <el-upload
      multiple
      action="#"
      list-type="picture-card"
      :http-request="uploadRequest"
      :on-success="handleUploadSuccess"
      :before-upload="handleBeforeUpload"
      :limit="limit"
      :accept="fileAccept"
      :on-error="handleUploadError"
      :on-exceed="handleExceed"
      :before-remove="handleDelete"
      :show-file-list="true"
      :file-list="fileList"
      :on-preview="handlePictureCardPreview"
      :on-change="trackPreview"
      :on-remove="releasePreview"
      :class="{ hide: fileList.length >= limit }"
    >
      <el-icon class="avatar-uploader-icon"><plus /></el-icon>
    </el-upload>
    <div v-for="file in fileList.filter(item => !item.url)" :key="file.uid" role="status">
      {{ file.name }}（预览暂不可用）<el-button link @click="retryPreview(file.id)">重试预览</el-button>
    </div>
    <div v-if="showTip" class="el-upload__tip">
      请上传
      <template v-if="fileSize">大小不超过 <b style="color: #f56c6c">{{ fileSize }}MB</b></template>
      <template v-if="fileType.length">格式为 <b style="color: #f56c6c">{{ fileType.join('/') }}</b></template>
      的图片文件
    </div>
    <el-dialog v-model="dialogVisible" title="预览" width="800px" append-to-body>
      <img :src="dialogImageUrl" style="display: block; max-width: 100%; margin: 0 auto" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { UploadFile, UploadRequestHandler } from 'element-plus';
import type { UploadResult } from '@namewta/platform-contracts';
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { createUploadRequest } from './upload-request';
import { isUploadIdentifier, normalizeUploadValue, serializeUploadItems, withUploadUid } from './normalize';
import type { ImageUploadProps, UploadItem } from './types';

const props = withDefaults(defineProps<ImageUploadProps>(), {
  modelValue: () => [],
  limit: 5,
  fileSize: 5,
  fileType: () => ['png', 'jpg', 'jpeg'],
  isShowTip: true,
  compressSupport: false,
  compressTargetSize: 300,
  policy: 'image',
  separator: ','
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  change: [items: readonly UploadItem[]];
  success: [result: UploadResult];
  error: [error: unknown];
  busy: [value: boolean];
}>();

const fileList = ref<Array<UploadItem & { uid: number }>>([]);
const dialogImageUrl = ref('');
const dialogVisible = ref(false);
const previewUrls = new Set<string>();
function releasePreview(file: UploadFile) {
  if (file.url?.startsWith('blob:')) { URL.revokeObjectURL(file.url); previewUrls.delete(file.url); }
}
function trackPreview(file: UploadFile) {
  if (!file.url?.startsWith('blob:')) return;
  if (file.status === 'ready' || file.status === 'uploading') previewUrls.add(file.url);
  else releasePreview(file);
}
onBeforeUnmount(() => { for (const url of previewUrls) URL.revokeObjectURL(url); previewUrls.clear(); });
const pending = ref(0);
const uploadOwner = new AbortController();
let active = true;
onBeforeUnmount(() => { active = false; generation.value++; uploadOwner.abort(); });
const generation = ref(0);
const fileAccept = computed(() => props.fileType.map(type => `.${type}`).join(','));
const showTip = computed(() => props.isShowTip && (props.fileType.length > 0 || props.fileSize > 0));
const uploadRequest: UploadRequestHandler = createUploadRequest(props.client, props.policy, delta => {
  pending.value = Math.max(0, pending.value + delta);
  emit('busy', pending.value > 0);
  if (delta > 0) props.feedback?.loading('正在上传图片，请稍候...');
  if (delta < 0 && pending.value === 0) props.feedback?.closeLoading();
}, uploadOwner.signal);

watch(
  () => props.modelValue,
  async value => {
    const currentGeneration = ++generation.value;
    const normalized = normalizeUploadValue(value);
    if (!normalized.ids.length) {
      fileList.value = [];
      return;
    }
    try {
      const resolved = normalized.items.length === normalized.ids.length ? normalized.items : await props.client.resolve(normalized.ids);
      if (!active || currentGeneration !== generation.value) return;
      fileList.value = resolved.map(withUploadUid);
    } catch {
      if (!active || currentGeneration !== generation.value) return;
      // 预览查询失败仍保留已完成的引用和文件名，不触发重复上传。
      fileList.value = normalized.ids.map((id, index) => withUploadUid(
        fileList.value.find(item => String(item.id) === String(id)) ?? { id, name: String(id), url: '' }, index));
    }
  },
  { deep: true, immediate: true }
);

async function handleBeforeUpload(file: File): Promise<boolean | File> {
  const extension = file.name.includes('.') ? file.name.slice(file.name.lastIndexOf('.') + 1).toLowerCase() : '';
  const isImage = props.fileType.length
    ? props.fileType.some(type => file.type.toLowerCase().includes(type.toLowerCase()) || extension === type.toLowerCase())
    : file.type.startsWith('image/');
  if (!isImage) {
    props.feedback?.error(`文件格式不正确, 请上传${props.fileType.join('/')}图片格式文件!`);
    return false;
  }
  if (file.name.includes(',')) {
    props.feedback?.error('文件名不正确，不能包含英文逗号!');
    return false;
  }
  if (props.fileSize > 0 && file.size / 1024 / 1024 >= props.fileSize) {
    props.feedback?.error(`上传图片大小不能超过 ${props.fileSize} MB!`);
    return false;
  }
  if (props.compressSupport && props.compress && file.size / 1024 > props.compressTargetSize) {
    return props.compress(file, props.compressTargetSize);
  }
  return true;
}

async function retryPreview(id: UploadItem['id']) {
  if (!isUploadIdentifier(id)) return;
  const currentGeneration = generation.value;
  try {
    const items = await props.client.resolve([id]);
    if (!active || currentGeneration !== generation.value) return;
    const resolved = items.find(item => String(item.id) === String(id));
    if (!resolved?.url) throw new Error('预览暂不可用，请稍后重试');
    fileList.value = fileList.value.map(item => String(item.id) === String(id) ? { ...item, ...resolved, uid: item.uid } : item);
  } catch {
    if (active && currentGeneration === generation.value) props.feedback?.error('预览暂不可用，请稍后重试');
  }
}

function handleExceed() {
  props.feedback?.error(`上传文件数量不能超过 ${props.limit} 个!`);
}

function handleUploadError(error: unknown, file?: UploadFile) {
  if (file) releasePreview(file);
  emit('error', error);
  props.feedback?.error(error instanceof Error ? error.message : '上传图片失败');
}

function handleUploadSuccess(result: UploadResult, file: UploadFile) {
  releasePreview(file);
  if (!result || !isUploadIdentifier(result.id)) {
    handleUploadError(new Error('上传响应缺少文件标识'));
    return;
  }
  const item = withUploadUid({ id: String(result.id), name: result.name || '', url: result.url || '' }, fileList.value.length + 1);
  fileList.value = [...fileList.value, item];
  emit('update:modelValue', serializeUploadItems(fileList.value, props.separator));
  emit('change', fileList.value);
  emit('success', result);
}

async function handleDelete(file: UploadFile): Promise<boolean> {
  const index = fileList.value.findIndex(item => item.uid === file.uid || item.name === file.name);
  if (index < 0) return true;
  const current = fileList.value[index];
  if (isUploadIdentifier(current.id)) await props.client.remove(current.id);
  fileList.value.splice(index, 1);
  emit('update:modelValue', serializeUploadItems(fileList.value, props.separator));
  emit('change', fileList.value);
  return false;
}

function handlePictureCardPreview(file: UploadFile) {
  if (!file.url) { void retryPreview(fileList.value.find(item => item.uid === file.uid)?.id); return; }
  dialogImageUrl.value = file.url || '';
  dialogVisible.value = true;
}
</script>

<style scoped>
:deep(.hide .el-upload--picture-card) {
  display: none;
}
</style>
