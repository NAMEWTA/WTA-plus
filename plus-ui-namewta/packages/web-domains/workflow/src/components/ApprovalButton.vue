<template>
  <div class="approval-actions">
    <div>
      <el-button v-if="showSubmit" :loading="buttonLoading" type="info" @click="emit('submitForm', 'draft', mode)">
        暂存
      </el-button>
      <el-button v-if="showSubmit" :loading="buttonLoading" type="primary" @click="emit('submitForm', 'submit', mode)">
        提 交
      </el-button>
      <el-button v-if="showApproval" :loading="buttonLoading" type="primary" @click="emit('approvalVerifyOpen')">
        审批
      </el-button>
      <el-button v-if="id && status !== 'draft'" type="primary" @click="emit('handleApprovalRecord')">
        流程进度
      </el-button>
      <slot />
    </div>
    <el-button @click="runtime.closeCurrentPage()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { WorkflowWebRuntime } from '../runtime';

const props = withDefaults(
  defineProps<{
    buttonLoading?: boolean;
    id?: string | number;
    mode?: boolean;
    pageType?: string;
    runtime: WorkflowWebRuntime;
    status?: string;
  }>(),
  { buttonLoading: false, id: '', mode: false, pageType: '', status: '' }
);
const emit = defineEmits<{
  approvalVerifyOpen: [];
  handleApprovalRecord: [];
  submitForm: [type: string, mode: boolean];
}>();
const showSubmit = computed(
  () => props.pageType === 'add' || (props.pageType === 'update' && ['draft', 'cancel', 'back'].includes(props.status))
);
const showApproval = computed(() => props.pageType === 'approval' && props.status === 'waiting');
</script>

<style scoped>
.approval-actions {
  display: flex;
  justify-content: space-between;
}
</style>
