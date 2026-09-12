<template>
  <div class="rich-text-editor" :class="{ 'is-disabled': disabled }">
    <EditorToolbar v-if="!disabled" :editor="editor" :default-config="toolbarConfig" mode="default" />
    <div v-if="!disabled" class="rich-text-editor__assets">
      <button v-for="option in assetInputs" :key="option.kind" type="button" @click="openAssetInput(option.kind)">{{ option.label }}</button>
      <input ref="assetInput" class="rich-text-editor__file" type="file" @change="handleAssetInput" />
    </div>
    <WangEditor v-model="liveHtml" :default-config="editorConfig" mode="default" @onCreated="handleCreated" />
    <div v-if="state.pending || state.failed" class="rich-text-editor__status" role="status">
      <span v-if="state.pending">正在上传 {{ state.pending }} 个文件</span>
      <span v-if="state.failed">，{{ state.failed }} 个文件上传失败</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import '@wangeditor-next/editor/dist/css/style.css';
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor-next/editor';
import { Editor as WangEditor, Toolbar as EditorToolbar } from '@wangeditor-next/editor-for-vue';
import { computed, onBeforeUnmount, reactive, ref, shallowRef, watch } from 'vue';
import { canonicalizeRichText, editorHtmlFromCanonical } from './content';
import type { RichTextAssetKind, RichTextAssetsPort, RichTextEditorState } from './types';

const props = withDefaults(defineProps<{ modelValue?: string; assets: RichTextAssetsPort; resourceId?: string; disabled?: boolean; placeholder?: string; profile?: 'full' | 'basic' }>(), {
  modelValue: '', disabled: false, placeholder: '请输入内容', profile: 'full'
});
const emit = defineEmits<{ 'update:modelValue': [value: string]; 'state-change': [state: RichTextEditorState]; error: [error: unknown] }>();

const editor = shallowRef<IDomEditor>();
const liveHtml = ref(editorHtmlFromCanonical(props.modelValue ?? ''));
const generation = ref(0);
const state = reactive<RichTextEditorState>({ pending: 0, failed: 0, valid: true });
const uploads = new Map<string, { controller: AbortController; file: File; kind: RichTextAssetKind }>();
const assetInput = ref<HTMLInputElement>();
const selectedKind = ref<RichTextAssetKind>('attachment');
const assetInputs: ReadonlyArray<{ kind: RichTextAssetKind; label: string }> = [
  { kind: 'image', label: '图片' }, { kind: 'audio', label: '音频' }, { kind: 'video', label: '视频' }, { kind: 'attachment', label: '附件' }
];
const editorConfig = computed<Partial<IEditorConfig>>(() => ({
  placeholder: props.placeholder,
  readOnly: props.disabled,
  autoFocus: false,
  customPaste: handlePaste,
  MENU_CONF: { uploadImage: { customUpload: (file: File) => void queueUpload(file, 'image') } }
}));
const toolbarConfig = computed<Partial<IToolbarConfig>>(() => ({
  modalAppendToBody: false,
  excludeKeys: props.profile === 'basic' ? ['fullScreen', 'uploadVideo', 'insertVideo'] : ['fullScreen']
}));

watch(() => props.modelValue, value => {
  const next = editorHtmlFromCanonical(value ?? '');
  if (next !== liveHtml.value && state.pending === 0) liveHtml.value = next;
});
watch(liveHtml, value => {
  const canonical = canonicalizeRichText(value);
  if (state.pending === 0 && state.failed === 0 && canonical !== props.modelValue) emit('update:modelValue', canonical);
});
watch(state, value => emit('state-change', { pending: value.pending, failed: value.failed, valid: value.valid }), { deep: true, immediate: true });

function kindFor(file: File): RichTextAssetKind {
  if (file.type.startsWith('image/')) return 'image';
  if (file.type.startsWith('audio/')) return 'audio';
  if (file.type.startsWith('video/')) return 'video';
  return 'attachment';
}

function openAssetInput(kind: RichTextAssetKind) {
  selectedKind.value = kind;
  if (assetInput.value) {
    assetInput.value.accept = kind === 'image' ? 'image/*' : kind === 'audio' ? 'audio/*' : kind === 'video' ? 'video/*' : '';
    assetInput.value.click();
  }
}

function handleAssetInput(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (file) void queueUpload(file, selectedKind.value);
}

function makeFileFromDataUrl(value: string): File | null {
  const match = value.match(/^data:([^;,]+);base64,(.+)$/);
  if (!match) return null;
  try {
    const bytes = Uint8Array.from(atob(match[2]), character => character.charCodeAt(0));
    return new File([bytes], `pasted-image-${Date.now()}.png`, { type: match[1] });
  } catch { return null; }
}

function insertPending(key: string, file: File, kind: RichTextAssetKind) {
  const label = file.name.replace(/[<>]/g, '');
  const placeholder = `<p><span data-richtext-pending="${key}" data-richtext-kind="${kind}">${label}（上传中）</span></p>`;
  editor.value?.dangerouslyInsertHtml(placeholder);
}

function replacePending(key: string, html: string) {
  if (!editor.value) return;
  const doc = new DOMParser().parseFromString(editor.value.getHtml(), 'text/html');
  const marker = doc.querySelector(`[data-richtext-pending="${CSS.escape(key)}"]`);
  if (!marker) return;
  marker.outerHTML = html;
  editor.value.setHtml(doc.body.innerHTML);
}

async function queueUpload(file: File, kind: RichTextAssetKind) {
  const instance = editor.value;
  if (!instance || !file.size) return;
  const uploadGeneration = generation.value;
  const key = crypto.randomUUID();
  const controller = new AbortController();
  uploads.set(key, { controller, file, kind });
  state.pending += 1;
  state.valid = false;
  insertPending(key, file, kind);
  try {
    const result = await props.assets.upload(file, kind, { signal: controller.signal });
    if (generation.value !== uploadGeneration || !editor.value || editor.value.isDestroyed) return;
    const id = result.ossId;
    if (!/^[1-9]\d*$/.test(id)) throw new Error('OSS 返回了无效文件标识');
    let liveUrl = `oss://${id}`;
    try {
      const access = await props.assets.resolve([id], { signal: controller.signal, richTextId: props.resourceId });
      liveUrl = access[0]?.url ?? liveUrl;
    } catch { /* canonical marker remains until viewer resolves it */ }
    const marker = kind === 'attachment' ? `<a data-oss-id="${id}" href="${liveUrl}">${result.fileName}</a>` : kind === 'audio' ? `<span data-richtext-asset-kind="audio" data-oss-id="${id}">${result.fileName}</span>` : `<${kind === 'image' ? 'img' : kind} data-oss-id="${id}" src="${liveUrl}"${kind === 'image' ? ` alt="${result.fileName}"` : ' controls'}>`;
    replacePending(key, marker);
  } catch (error) {
    if (!controller.signal.aborted) { state.failed += 1; emit('error', error); }
  } finally {
    uploads.delete(key);
    state.pending = Math.max(0, state.pending - 1);
    state.valid = state.pending === 0 && state.failed === 0;
  }
}

function handlePaste(target: IDomEditor, event: ClipboardEvent): boolean {
  const data = event.clipboardData;
  if (!data) return true;
  const files = [...data.files];
  const pastedImage = data.getData('text/plain').startsWith('data:image/') ? makeFileFromDataUrl(data.getData('text/plain')) : null;
  if (pastedImage) files.push(pastedImage);
  if (!files.length) return true;
  event.preventDefault();
  files.forEach(file => void queueUpload(file, kindFor(file)));
  const text = data.getData('text/plain');
  if (text && !pastedImage) target.dangerouslyInsertHtml(`<p>${text.replace(/[&<>]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[character] ?? character))}</p>`);
  return false;
}

function handleDrop(event: DragEvent) {
  if (props.disabled || !event.dataTransfer?.files.length) return;
  event.preventDefault();
  event.stopPropagation();
  [...event.dataTransfer.files].forEach(file => void queueUpload(file, kindFor(file)));
}

function handleCreated(value: IDomEditor) {
  editor.value = value;
  const container = value.getEditableContainer() as HTMLElement;
  container.addEventListener('drop', handleDrop, true);
  value.on('destroy', () => container.removeEventListener('drop', handleDrop, true));
}

defineExpose({ validate: () => state.pending === 0 && state.failed === 0, getHtml: () => canonicalizeRichText(editor.value?.getHtml() ?? liveHtml.value) });

onBeforeUnmount(() => {
  generation.value += 1;
  uploads.forEach(upload => upload.controller.abort());
  uploads.clear();
  editor.value?.destroy();
});
</script>

<style scoped>
.rich-text-editor { border: 1px solid var(--el-border-color, #dcdfe6); border-radius: 4px; overflow: hidden; }
.rich-text-editor.is-disabled { background: var(--el-fill-color-light, #f5f7fa); }
.rich-text-editor__assets { display: flex; gap: 6px; padding: 5px 8px; border-top: 1px solid var(--el-border-color, #dcdfe6); }
.rich-text-editor__assets button { border: 1px solid var(--el-border-color, #dcdfe6); border-radius: 3px; background: transparent; padding: 2px 8px; cursor: pointer; }
.rich-text-editor__file { display: none; }
.rich-text-editor__status { padding: 6px 10px; color: var(--el-color-warning, #e6a23c); font-size: 12px; }
</style>
