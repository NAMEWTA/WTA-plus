<template>
  <div class="p-2">
    <el-card>
      <template #header>
        <div class="flex items-center justify-between gap-2">
          <span>通知收件箱 · 未读 {{ unreadTotal }}</span>
          <div class="flex gap-2">
            <el-button v-if="canRead" :disabled="!unreadTotal || mutationLoading" :loading="mutationLoading" @click="readAll">
              全部已读（包括历史消息）
            </el-button>
            <el-button :loading="loading" @click="load">刷新</el-button>
          </div>
        </div>
      </template>
      <div v-if="error" role="alert" class="mb-3">
        {{ error }}
        <el-button link type="primary" @click="load">重试</el-button>
      </div>
      <div v-if="loading && !rows.length" role="status">正在加载消息…</div>
      <el-empty v-else-if="!loading && !rows.length && !error" description="暂无消息" />
      <el-table v-if="rows.length" v-loading="loading" :data="rows" border @row-click="openDetail">
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
            <el-button v-if="canRead && !row.readTime" link type="primary" @click.stop="markRead(row.messageId)">
              标记已读
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mt-4 flex justify-end">
        <el-pagination
          v-if="total > 0"
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="changePage"
        />
      </div>
    </el-card>
    <el-dialog v-model="detailVisible" title="通知详情" width="680px">
      <div v-if="detailLoading" role="status">正在加载详情…</div>
      <div v-else-if="detailError" role="alert">
        {{ detailError }}
        <el-button link type="primary" @click="retryDetail">重试</el-button>
      </div>
      <el-descriptions v-else-if="selected" :column="1" border>
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
import { computed, onActivated, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import type { NotifyWebRuntime } from './runtime';

const { runtime } = defineProps<{ runtime: NotifyWebRuntime }>();
const dicts = runtime.dicts('sys_notice_type', 'notify_message_category', 'notify_channel');
const rows = ref<NotifyInboxMessage[]>([]);
const pageNum = ref(1);
const pageSize = 20;
const total = ref(0);
const unreadTotal = ref(0);
const loading = ref(false);
const error = ref('');
const mutationLoading = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailError = ref('');
const selected = ref<NotifyInboxMessage>();
const reading = reactive(new Set<string>());
const sessionSnapshot = () => runtime.inboxSession?.snapshot() ?? { epoch: 0, active: false };
const session = ref(sessionSnapshot());
const canRead = computed(() => runtime.hasPermission('notify:inbox:read'));
let mounted = false;
let initialized = false;
let generation = 0;
let detailGeneration = 0;
let unsubscribe: (() => void) | undefined;

function owns(epoch: number) {
  return mounted && session.value.epoch === epoch && sessionSnapshot().epoch === epoch;
}

function reset() {
  ++generation;
  ++detailGeneration;
  rows.value = [];
  pageNum.value = 1;
  total.value = 0;
  unreadTotal.value = 0;
  loading.value = false;
  error.value = '';
  mutationLoading.value = false;
  reading.clear();
  detailVisible.value = false;
  selected.value = undefined;
  detailLoading.value = false;
  detailError.value = '';
}

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
  const epoch = session.value.epoch;
  if (!mounted || !session.value.active) return;
  const request = ++generation;
  const requestedPage = pageNum.value;
  loading.value = true;
  error.value = '';
  try {
    const result = await runtime.service.inbox.list(requestedPage, pageSize);
    if (owns(epoch) && request === generation) {
      rows.value = result.data.rows;
      total.value = result.data.total;
      unreadTotal.value = result.data.unreadTotal;
    }
  } catch (cause) {
    if (owns(epoch) && request === generation) error.value = cause instanceof Error ? cause.message : '收件箱加载失败';
  } finally {
    if (owns(epoch) && request === generation) {
      loading.value = false;
      initialized = true;
    }
  }
}

function changePage(next: number) {
  ++detailGeneration;
  detailVisible.value = false;
  selected.value = undefined;
  pageNum.value = next;
  rows.value = [];
  void load();
}

async function markRead(id: string | number) {
  const epoch = session.value.epoch;
  const key = epoch + ':' + id;
  if (!session.value.active || !canRead.value || reading.has(key)) return;
  reading.add(key);
  try {
    await runtime.service.inbox.read(id);
    if (!owns(epoch)) return;
    runtime.inboxChanged?.();
    await load();
  } catch (cause) {
    if (owns(epoch)) ElMessage.error(cause instanceof Error ? cause.message : '标记已读失败');
  } finally {
    reading.delete(key);
  }
}

async function readAll() {
  const epoch = session.value.epoch;
  if (!session.value.active || !canRead.value || mutationLoading.value || !unreadTotal.value) return;
  mutationLoading.value = true;
  try {
    await runtime.service.inbox.readAll();
    if (!owns(epoch)) return;
    runtime.inboxChanged?.();
    await load();
  } catch (cause) {
    if (owns(epoch)) ElMessage.error(cause instanceof Error ? cause.message : '全部已读失败');
  } finally {
    if (owns(epoch)) mutationLoading.value = false;
  }
}

async function openDetail(row: NotifyInboxMessage) {
  const epoch = session.value.epoch;
  if (!session.value.active) return;
  const request = ++detailGeneration;
  selected.value = { ...row, content: undefined };
  detailVisible.value = true;
  detailLoading.value = true;
  detailError.value = '';
  try {
    const response = await runtime.service.inbox.detail(row.messageId);
    if (!owns(epoch) || request !== detailGeneration || !detailVisible.value) return;
    selected.value = response.data;
  } catch (cause) {
    if (owns(epoch) && request === detailGeneration) {
      detailError.value = cause instanceof Error ? cause.message : '消息详情加载失败';
    }
    return;
  } finally {
    if (owns(epoch) && request === detailGeneration) detailLoading.value = false;
  }
  if (canRead.value && !row.readTime) await markRead(row.messageId);
}

function retryDetail() {
  const current = selected.value;
  if (current) void openDetail(current);
}

async function openBusiness() {
  const path = selected.value?.path;
  if (!path || !session.value.active) return;
  detailVisible.value = false;
  await runtime.navigate(path);
}

watch(sessionSnapshot, next => {
  if (next.epoch === session.value.epoch && next.active === session.value.active) return;
  session.value = next;
  reset();
  if (mounted && next.active) void load();
}, { flush: 'sync' });
watch(detailVisible, visible => {
  if (!visible) {
    ++detailGeneration;
    selected.value = undefined;
    detailLoading.value = false;
    detailError.value = '';
  }
});
onMounted(() => {
  mounted = true;
  void load();
  unsubscribe = runtime.subscribeInbox?.(() => void load());
});
onActivated(() => {
  if (initialized) void load();
});
onBeforeUnmount(() => {
  mounted = false;
  reset();
  unsubscribe?.();
});
</script>
