<template>
  <div class="p-2">
    <el-card>
      <template #header>通知投递监控</template>
      <el-form :inline="true" @submit.prevent="load">
        <el-form-item label="用户编号"><el-input v-model="query.userId" clearable /></el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="query.channel" clearable>
            <el-option v-for="item in channelDict" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable>
            <el-option v-for="item in statusDict" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="load">查询</el-button></el-form-item>
      </el-form>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-3" />
      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="deliveryId" label="投递编号" min-width="160" />
        <el-table-column prop="userId" label="用户" width="120" />
        <el-table-column label="渠道" width="100">
          <template #default="scope">{{ channelLabel(scope.row.channel) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="scope">
            <el-tag :type="statusTag(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="providerMessageId" label="供应商消息" min-width="180" />
        <el-table-column prop="errorCode" label="错误码" width="140" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="runtime.hasPermission('notify:notification:retry') && canRetry(row as NotificationDelivery)"
              link type="primary" :loading="busy !== null" :disabled="busy !== null || !hasIds(row as NotificationDelivery)"
              @click="retry(row as NotificationDelivery)"
            >重试此投递</el-button>
            <el-button
              v-if="runtime.hasPermission('notify:notification:cancel') && canCancel(row as NotificationDelivery)"
              link type="danger" :loading="busy !== null" :disabled="busy !== null || !row.intentId"
              @click="cancel(row as NotificationDelivery)"
            >取消通知</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import type { NotificationDelivery, NotificationDeliveryQuery } from '@namewta/domain-notify';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import type { NotifyWebRuntime } from './runtime';

const { runtime } = defineProps<{ runtime: NotifyWebRuntime }>();
const dictRefs = runtime.dicts('notify_channel', 'notify_delivery_status');
const channelDict = computed(() => dictRefs.notify_channel?.value ?? []);
const statusDict = computed(() => dictRefs.notify_delivery_status?.value ?? []);
const query = reactive<NotificationDeliveryQuery>({});
const rows = ref<NotificationDelivery[]>([]);
const loading = ref(false);
const error = ref('');
const busy = ref<string | null>(null);
let queryGeneration = 0;
let lifetime = 0;
const statusFallback: Record<string, { label: string; tag: 'success' | 'warning' | 'info' | 'primary' | 'danger' }> = {
  DISPATCH_ERROR: { label: '投递异常', tag: 'danger' },
  PENDING: { label: '待投递', tag: 'info' },
  QUEUED: { label: '排队中', tag: 'info' },
  FAILED: { label: '失败', tag: 'danger' },
  CANCELLED: { label: '已取消', tag: 'info' },
  DELIVERED: { label: '已送达', tag: 'success' },
  UNKNOWN: { label: '结果未知', tag: 'warning' }
};
function dictLabel(options: Array<{ label: string; value: string }>, value?: string) {
  return options.find(item => item.value === value)?.label ?? '未知';
}
const channelFallback: Record<string, string> = {
  IN_APP: '\u7ad9\u5185\u4fe1',
  SMS: '\u77ed\u4fe1',
  MAIL: '\u90ae\u4ef6'
};
function channelLabel(value?: string) {
  return channelDict.value.find(item => item.value === value)?.label ?? channelFallback[value ?? ''] ?? '\u5176\u4ed6';
}
function statusLabel(value?: string) {
  return dictLabel(statusDict.value, value) === '未知'
    ? (statusFallback[value ?? '']?.label ?? '未知')
    : dictLabel(statusDict.value, value);
}
function statusTag(value?: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
  return (
    (statusDict.value.find(item => item.value === value)?.listClass as
      | 'success'
      | 'warning'
      | 'info'
      | 'primary'
      | 'danger'
      | undefined) ??
    statusFallback[value ?? '']?.tag ??
    'info'
  );
}
async function load() {
  const request = ++queryGeneration;
  loading.value = true;
  error.value = '';
  try {
    const result = await runtime.service.deliveries(query);
    if (request === queryGeneration) rows.value = result.data;
  } catch (failure) {
    if (request === queryGeneration) {
      rows.value = [];
      error.value = failure instanceof Error ? failure.message : '通知投递加载失败';
    }
  } finally {
    if (request === queryGeneration) loading.value = false;
  }
}
const validId = (value?: string) => value != null && /^[1-9]\d*$/.test(value);
const hasIds = (row: NotificationDelivery) => validId(row.intentId) && validId(row.deliveryId);
const canRetry = (row: NotificationDelivery) => row.status === 'FAILED'
  || (row.channel === 'IN_APP' && row.status === 'UNKNOWN');
const canCancel = (row: NotificationDelivery) =>
  ['PENDING', 'QUEUED', 'PROCESSING', 'ACCEPTED', 'UNKNOWN'].includes(row.status);

async function retry(row: NotificationDelivery) {
  if (!runtime.hasPermission('notify:notification:retry') || !canRetry(row) || !hasIds(row) || busy.value) return;
  const notificationId = row.intentId!;
  const deliveryId = row.deliveryId!;
  const request = lifetime;
  busy.value = `retry:${deliveryId}`;
  try {
    const receipt = (await runtime.service.notification.retry(notificationId, deliveryId)).data;
    if (request !== lifetime) return;
    if (receipt.queuedCount > 0) ElMessage.success(`已重新排队 ${receipt.queuedCount} 项投递`);
    else ElMessage.info(`没有可重试任务，通知当前状态：${statusLabel(receipt.status)}`);
    await load();
  } catch (failure) {
    if (request === lifetime) ElMessage.error(failure instanceof Error ? failure.message : '重试投递失败');
  } finally {
    if (request === lifetime) busy.value = null;
  }
}

async function cancel(row: NotificationDelivery) {
  if (!runtime.hasPermission('notify:notification:cancel') || !canCancel(row)
    || !validId(row.intentId) || busy.value) return;
  const notificationId = row.intentId!;
  const request = lifetime;
  busy.value = `cancel:${notificationId}`;
  try {
    await ElMessageBox.confirm('取消整个通知？这只会停止尚未开始的投递，已受理或结果未知的外部发送无法撤回。',
      '取消通知', { type: 'warning' });
  } catch {
    if (request === lifetime) busy.value = null;
    return;
  }
  if (request !== lifetime) return;
  try {
    await runtime.service.notification.cancel(notificationId);
    if (request !== lifetime) return;
    ElMessage.success('通知已取消');
    await load();
  } catch (failure) {
    if (request === lifetime) ElMessage.error(failure instanceof Error ? failure.message : '取消通知失败');
  } finally {
    if (request === lifetime) busy.value = null;
  }
}
onMounted(load);
onBeforeUnmount(() => { ++queryGeneration; ++lifetime; });
</script>
