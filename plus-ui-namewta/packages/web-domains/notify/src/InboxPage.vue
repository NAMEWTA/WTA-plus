<template>
  <div class="p-2">
    <el-card>
      <template #header>
        <div class="flex justify-between">
          <span>通知收件箱</span>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="rows" border @row-click="openDetail">
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column label="通知类型" width="110">
          <template #default="{ row }">{{ typeLabel(row as NotifyInboxMessage) }}</template>
        </el-table-column>
        <el-table-column label="发送渠道" min-width="160">
          <template #default="{ row }">{{ channelsLabel(row as NotifyInboxMessage) }}</template>
        </el-table-column>
        <el-table-column prop="message" label="摘要" min-width="300" />
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.readTime ? 'info' : 'warning'">{{ scope.row.readTime ? '已读' : '未读' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row as NotifyInboxMessage)">查看详情</el-button>
            <el-button v-if="!row.readTime" link type="primary" @click.stop="markRead(row.messageId)">
              标记已读
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="detailVisible" title="通知详情" width="680px">
      <el-descriptions v-if="selected" :column="1" border>
        <el-descriptions-item label="标题">{{ selected.title || '通知' }}</el-descriptions-item>
        <el-descriptions-item label="通知类型">{{ typeLabel(selected) }}</el-descriptions-item>
        <el-descriptions-item label="发送渠道">{{ channelsLabel(selected) }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ selected.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容">
          <div class="whitespace-pre-wrap">{{ selected.content || selected.message || '-' }}</div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button v-if="selected?.path" type="primary" @click="openBusiness">查看业务</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { NotifyInboxMessage } from '@namewta/domain-notify';
import { ElMessage } from 'element-plus';
import { onActivated, onBeforeUnmount, onMounted, ref } from 'vue';
import type { NotifyWebRuntime } from './runtime';

const { runtime } = defineProps<{ runtime: NotifyWebRuntime }>();
const dicts = runtime.dicts('sys_notice_type', 'notify_message_category', 'notify_channel');
const rows = ref<NotifyInboxMessage[]>([]);
const loading = ref(false);
const detailVisible = ref(false);
const selected = ref<NotifyInboxMessage>();
let generation = 0;
let initialized = false;
const reading = new Set<string>();
let unsubscribe: (() => void) | undefined;

function typeLabel(row: NotifyInboxMessage) {
  return row.noticeType
    ? (dicts.sys_notice_type?.value.find(item => item.value === row.noticeType)?.label ?? '其他')
    : (dicts.notify_message_category?.value.find(item => item.value === row.category)?.label ?? '其他');
}
function channelsLabel(row: NotifyInboxMessage) {
  return (row.channels?.length ? row.channels : ['IN_APP'])
    .map(channel => dicts.notify_channel?.value.find(item => item.value === channel)?.label ?? '其他')
    .join('、');
}

async function load() {
  const request = ++generation;
  loading.value = true;
  try {
    const result = await runtime.service.inbox.list();
    if (request === generation) rows.value = result.data;
  } catch (error) {
    if (request === generation) ElMessage.error(error instanceof Error ? error.message : '收件箱加载失败');
  } finally {
    if (request === generation) loading.value = false;
    initialized = true;
  }
}

async function markRead(id: string | number) {
  if (reading.has(String(id))) return;
  reading.add(String(id));
  try {
    await runtime.service.inbox.read(id);
    runtime.inboxChanged?.();
    await load();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '标记已读失败');
  } finally {
    reading.delete(String(id));
  }
}

async function openDetail(row: NotifyInboxMessage) {
  selected.value = row;
  detailVisible.value = true;
  if (!row.readTime) await markRead(row.messageId);
}

async function openBusiness() {
  if (!selected.value?.path) return;
  detailVisible.value = false;
  await runtime.navigate(selected.value.path);
}

onMounted(() => {
  void load();
  unsubscribe = runtime.subscribeInbox?.(() => void load());
});
onActivated(() => {
  if (initialized) void load();
});
onBeforeUnmount(() => {
  ++generation;
  unsubscribe?.();
});
</script>
