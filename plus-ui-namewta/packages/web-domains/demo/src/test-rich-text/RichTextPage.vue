<template>
  <section class="rich-text-demo">
    <el-card>
      <template #header>
        <div class="toolbar"><strong>富文本通用组件演示</strong><el-button type="primary" @click="save">{{ editingId ? '保存修改' : '新建并保存' }}</el-button><el-button @click="reset">新建</el-button></div>
      </template>
      <el-input v-model="title" maxlength="120" show-word-limit placeholder="文档标题" />
      <RichTextEditor v-model="html" :assets="runtime.service.richText.assets" :resource-id="editingId" profile="full" @state-change="editorState = $event" />
      <el-alert v-if="!editorState.valid" type="warning" :closable="false" show-icon title="上传未完成或失败时不能保存" />
    </el-card>
    <el-card class="preview-card">
      <template #header><div class="toolbar"><strong>预览</strong><el-switch v-model="showMedia" active-text="显示媒体" inactive-text="隐藏媒体" /></div></template>
      <RichTextViewer :html="html" :assets="runtime.service.richText.assets" :resource-id="editingId" profile="article" :show-media="showMedia" />
    </el-card>
    <el-card>
      <template #header>已保存文档</template>
      <el-table :data="rows" v-loading="loading" row-key="richTextId">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="updateTime" label="更新时间" width="190" />
        <el-table-column label="操作" width="180"><template #default="scope"><el-button link type="primary" @click="edit(scope.row.richTextId)">编辑</el-button><el-button link type="danger" @click="remove(scope.row.richTextId, scope.row.version)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, onBeforeUnmount, ref } from 'vue';
import { RichTextEditor, RichTextViewer, type RichTextEditorState } from '@namewta/web-kit-rich-text';
import type { DemoWebRuntime } from '../runtime';
import type { RichTextSummary } from '@namewta/domain-demo';

const props = defineProps<{ runtime: DemoWebRuntime }>();
const runtime = props.runtime;
const title = ref('');
const html = ref('<h2>富文本接入示例</h2><p>可输入文字、标题、加粗、斜体，并通过 OSS 插入图片、音频、视频和附件。</p>');
const rows = ref<RichTextSummary[]>([]);
const editingId = ref<string>();
const editorState = ref<RichTextEditorState>({ pending: 0, failed: 0, valid: true });
const showMedia = ref(true);
const loading = ref(false);
const controller = new AbortController();

async function load() {
  loading.value = true;
  try { rows.value = (await runtime.service.richText.list({ pageNum: 1, pageSize: 50 })).data?.rows ?? []; }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '富文本列表加载失败'); }
  finally { loading.value = false; }
}
async function edit(id: string) {
  try { const response = await runtime.service.richText.get(id);
    if (response.data) { editingId.value = id; title.value = response.data.title; html.value = response.data.html; }
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '富文本加载失败'); }
}
async function save() {
  if (!editorState.value.valid || !title.value.trim()) { ElMessage.warning('请填写标题并等待上传完成'); return; }
  try {
    const response = editingId.value
      ? await runtime.service.richText.update(editingId.value, { title: title.value.trim(), html: html.value, version: rows.value.find(row => row.richTextId === editingId.value)?.version ?? 0 })
      : await runtime.service.richText.create({ title: title.value.trim(), html: html.value });
    if (response.data) { ElMessage.success('保存成功'); editingId.value = response.data.richTextId; await load(); }
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '富文本保存失败'); }
}
async function remove(id: string, version: number) {
  await ElMessageBox.confirm('确认删除该文档？', '提示');
  try { await runtime.service.richText.remove(id, version); ElMessage.success('删除成功'); if (editingId.value === id) reset(); await load(); }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '富文本删除失败'); }
}
function reset() { editingId.value = undefined; title.value = ''; html.value = '<p></p>'; }
onMounted(() => void load());
onBeforeUnmount(() => controller.abort());
</script>

<style scoped>
.rich-text-demo { display: grid; gap: 16px; padding: 16px; }
.toolbar { display: flex; align-items: center; gap: 12px; justify-content: space-between; }
.preview-card { min-height: 200px; }
</style>
