<template>
  <el-dialog v-model="visible" title="审批记录" :width="width" :close-on-click-modal="false">
    <el-tabs v-model="tab">
      <el-tab-pane label="流程图" name="chart">
        <FlowChart v-if="instanceId" :ins-id="instanceId" :runtime="runtime" />
      </el-tab-pane>
      <el-tab-pane label="审批信息" name="history">
        <el-table v-loading="loading" :data="history" border>
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="nodeName" label="任务名称" />
          <el-table-column prop="approverName" label="办理人">
            <template #default="scope"><UserNameDisplay :content="scope.row.approverName" /></template>
          </el-table-column>
          <el-table-column prop="flowStatus" label="状态">
            <template #default="scope">
              <el-tag>{{ statusLabel(scope.row.flowStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="审批意见" />
          <el-table-column prop="createTime" label="开始时间" />
          <el-table-column prop="updateTime" label="结束时间" />
          <el-table-column prop="runDuration" label="运行时长" />
          <el-table-column label="附件">
            <template #default="scope">
              <el-popover v-if="scope.row.attachmentList?.length" placement="right" :width="320" trigger="click">
                <template #reference><el-button type="primary">附件</el-button></template>
                <el-table :data="scope.row.attachmentList" border>
                  <el-table-column prop="originalName" label="附件名称" />
                  <el-table-column label="操作" width="80">
                    <template #default="attachment">
                      <el-button link type="primary" @click="runtime.downloadAttachment(attachment.row.ossId)">
                        下载
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-popover>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import type { WorkflowAttachment, WorkflowWebRuntime } from '../runtime';
import FlowChart from './FlowChart.vue';
import UserNameDisplay from './UserNameDisplay.vue';

type HistoryRow = Record<string, unknown> & { attachmentList?: readonly WorkflowAttachment[]; ext?: string | number };
const props = withDefaults(defineProps<{ height?: string; runtime: WorkflowWebRuntime; width?: string }>(), {
  height: '100%',
  width: '80%'
});
const visible = ref(false);
const loading = ref(false);
const tab = ref('chart');
const instanceId = ref<string | number>();
const history = ref<HistoryRow[]>([]);
const status = props.runtime.dicts('wf_task_status').wf_task_status;
function statusLabel(value: unknown) {
  return status.value.find(option => option.value === String(value))?.label ?? String(value ?? '');
}
async function init(businessId: string | number) {
  visible.value = true;
  loading.value = true;
  tab.value = 'chart';
  try {
    const response = await props.runtime.service.flowHistory(businessId);
    instanceId.value = response.data?.instanceId;
    history.value = await Promise.all(
      (response.data?.list ?? []).map(async item => ({
        ...item,
        attachmentList: item.ext ? await props.runtime.resolveAttachments(item.ext as string | number) : []
      }))
    );
  } finally {
    loading.value = false;
  }
}
defineExpose({ init });
</script>
