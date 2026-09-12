<template>
  <div class="upload-file">
    <el-upload
      v-if="!disabled"
      multiple
      action="#"
      :before-upload="handleBeforeUpload"
      :file-list="fileList"
      :http-request="uploadRequest"
      :limit="limit"
      :accept="fileAccept"
      :on-error="handleUploadError"
      :on-exceed="handleExceed"
      :on-success="handleUploadSuccess"
      :show-file-list="false"
      class="upload-file-uploader"
    >
      <el-button type="primary">选取文件</el-button>
    </el-upload>
    <div v-if="showTip && !disabled" class="el-upload__tip">
      请上传
      <template v-if="fileSize">大小不超过 <b style="color: #f56c6c">{{ fileSize }}MB</b></template>
      <template v-if="fileType.length">格式为 <b style="color: #f56c6c">{{ fileType.join('/') }}</b></template>
      的文件
    </div>
    <transition-group class="upload-file-list el-upload-list el-upload-list--text" name="el-fade-in-linear" tag="ul">
      <li v-for="(file, index) in fileList" :key="file.uid" class="el-upload-list__item ele-upload-list__item-content">
        <el-link :href="file.url" underline="never" target="_blank">
          <span class="el-icon-document">{{ getFileName(file.name) }}</span>
        </el-link>
        <div v-if="!disabled" class="ele-upload-list__item-content-action">
          <el-button type="danger" link @click="handleDelete(index)">删除</el-button>
        </div>
      </li>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import type { UploadResult } from '@namewta/platform-contracts';
import type { UploadRequestHandler } from 'element-plus';
import { computed, ref, watch } from 'vue';
import { createUploadRequest } from './upload-request';
import { isUploadIdentifier, normalizeUploadValue, serializeUploadItems, withUploadUid } from './normalize';
import type { FileUploadProps, UploadItem } from './types';

const props = withDefaults(defineProps<FileUploadProps>(), {
  modelValue: () => [],
  limit: 5,
  fileSize: 5,
  fileType: () => ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'pdf'],
  isShowTip: true,
  disabled: false,
  policy: 'document',
  separator: ','
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  change: [items: readonly UploadItem[]];
  success: [result: UploadResult];
  error: [error: unknown];
}>();

const fileList = ref<Array<UploadItem & { uid: number }>>([]);
const pending = ref(0);
const generation = ref(0);
const fileAccept = computed(() => props.fileType.map(type => `.${type}`).join(','));
const showTip = computed(() => props.isShowTip && (props.fileType.length > 0 || props.fileSize > 0));
const uploadRequest: UploadRequestHandler = createUploadRequest(props.client, props.policy, delta => {
  pending.value = Math.max(0, pending.value + delta);
  if (delta > 0) props.feedback?.loading('正在上传文件，请稍候...');
  if (delta < 0 && pending.value === 0) props.feedback?.closeLoading();
});

watch(
  () => props.modelValue,
  async value => {
    const currentGeneration = ++generation.value;
    const normalized = normalizeUploadValue(value);
    if (!normalized.ids.length) {
      fileList.value = [];
      return;
    }
    const resolved = normalized.items.length === normalized.ids.length ? normalized.items : await props.client.resolve(normalized.ids);
    if (currentGeneration !== generation.value) return;
    fileList.value = resolved.map(withUploadUid);
  },
  { deep: true, immediate: true }
);

function handleBeforeUpload(file: File): boolean {
  const extension = file.name.includes('.') ? file.name.slice(file.name.lastIndexOf('.') + 1).toLowerCase() : '';
  if (props.fileType.length && !props.fileType.some(type => type.toLowerCase() === extension)) {
    props.feedback?.error(`文件格式不正确, 请上传${props.fileType.join('/')}格式文件!`);
    return false;
  }
  if (file.name.includes(',')) {
    props.feedback?.error('文件名不正确，不能包含英文逗号!');
    return false;
  }
  if (props.fileSize > 0 && file.size / 1024 / 1024 >= props.fileSize) {
    props.feedback?.error(`上传文件大小不能超过 ${props.fileSize} MB!`);
    return false;
  }
  return true;
}

function handleExceed() {
  props.feedback?.error(`上传文件数量不能超过 ${props.limit} 个!`);
}

function handleUploadError(error: unknown) {
  emit('error', error);
  props.feedback?.error(error instanceof Error ? error.message : '上传文件失败');
}

function handleUploadSuccess(result: UploadResult) {
  if (!result || !isUploadIdentifier(result.id)) {
    handleUploadError(new Error('上传响应缺少文件标识'));
    return;
  }
  const item = withUploadUid({ id: String(result.id), name: result.name || '', url: result.url || '' }, fileList.value.length + 1);
  fileList.value = [...fileList.value, item];
  const value = serializeUploadItems(fileList.value, props.separator);
  emit('update:modelValue', value);
  emit('change', fileList.value);
  emit('success', result);
}

async function handleDelete(index: number) {
  const file = fileList.value[index];
  if (!file) return;
  if (isUploadIdentifier(file.id)) await props.client.remove(file.id);
  fileList.value.splice(index, 1);
  emit('update:modelValue', serializeUploadItems(fileList.value, props.separator));
  emit('change', fileList.value);
}

function getFileName(name: string) {
  return name.lastIndexOf('/') > -1 ? name.slice(name.lastIndexOf('/') + 1) : name;
}
</script>

<style scoped>
.upload-file-uploader {
  margin-bottom: 5px;
}

.upload-file-list .el-upload-list__item {
  border: 1px solid #e4e7ed;
  line-height: 2;
  margin-bottom: 10px;
  position: relative;
}

.upload-file-list .ele-upload-list__item-content {
  align-items: center;
  color: inherit;
  display: flex;
  justify-content: space-between;
}

.ele-upload-list__item-content-action .el-link {
  margin-right: 10px;
}
</style>
