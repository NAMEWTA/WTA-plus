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
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import type { NotificationDelivery, NotificationDeliveryQuery } from '@namewta/domain-notify';
import { computed, onMounted, reactive, ref } from 'vue';
import type { NotifyWebRuntime } from './runtime';

const { runtime } = defineProps<{ runtime: NotifyWebRuntime }>();
const dictRefs = runtime.dicts('notify_channel', 'notify_delivery_status');
const channelDict = computed(() => dictRefs.notify_channel?.value ?? []);
const statusDict = computed(() => dictRefs.notify_delivery_status?.value ?? []);
const query = reactive<NotificationDeliveryQuery>({});
const rows = ref<NotificationDelivery[]>([]);
const loading = ref(false);
const statusFallback: Record<string, { label: string; tag: 'success' | 'warning' | 'info' | 'primary' | 'danger' }> = {
  DISPATCH_ERROR: { label: '投递异常', tag: 'danger' }
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
  loading.value = true;
  try {
    rows.value = (await runtime.service.deliveries(query)).data;
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>
