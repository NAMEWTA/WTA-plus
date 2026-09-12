<template>
  <div class="person-review-page" v-loading="loading">
    <template v-if="context">
      <header>
        <h2>个人认证审核</h2>
        <el-tag>{{ context.status }}</el-tag>
      </header>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请编号">{{ context.applicationId }}</el-descriptions-item>
        <el-descriptions-item label="提交序号">{{ context.submissionSeq }}</el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ context.submittedTime }}</el-descriptions-item>
      </el-descriptions>
      <section>
        <h3>不可变申请快照</h3>
        <pre>{{ snapshot }}</pre>
      </section>
      <section>
        <h3>申请材料</h3>
        <el-table :data="context.materials" row-key="materialRefId" border>
          <el-table-column prop="materialTagName" label="材料标签" />
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column prop="fileSize" label="大小" width="120" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" @click="download(row.materialRefId)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
      <el-form label-width="80px" class="decision-form">
        <el-form-item label="审核结果"><el-segmented v-model="decision" :options="decisionOptions" /></el-form-item>
        <el-form-item label="审核意见"><el-input v-model="reason" type="textarea" maxlength="500" /></el-form-item>
      </el-form>
      <footer>
        <el-button :loading="saving" @click="completeReview">提交流程审核</el-button>
        <el-button v-if="canOverride" type="danger" plain :loading="saving" @click="overrideReview">
          管理员覆盖决定
        </el-button>
      </footer>
    </template>
  </div>
</template>

<script setup lang="ts">
import { profilePermissions, type Identifier, type ReviewContext } from '@namewta/domain-profile';
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import type { ProfileWebRuntime } from '../runtime';
import { safeErrorMessage } from './logic';

const { runtime } = defineProps<{ runtime: ProfileWebRuntime }>();
const route = useRoute();
const applicationId = computed(() => String(route.query.id ?? ''));
const taskId = computed(() => String(route.query.taskId ?? ''));
const context = ref<ReviewContext>();
const loading = ref(false);
const saving = ref(false);
const decision = ref<'APPROVE' | 'REJECT'>('APPROVE');
const reason = ref('');
const decisionOptions = [
  { label: '通过', value: 'APPROVE' },
  { label: '驳回', value: 'REJECT' }
];
const canOverride = computed(() => runtime.hasPermission(profilePermissions.person.override));
const snapshot = computed(() => {
  try {
    return JSON.stringify(JSON.parse(context.value?.fieldSnapshotJson ?? '{}'), null, 2);
  } catch {
    return context.value?.fieldSnapshotJson ?? '';
  }
});

async function load() {
  if (!applicationId.value) return;
  loading.value = true;
  try {
    context.value = (await runtime.service.person.archive.review(applicationId.value)).data;
  } catch (error) {
    runtime.error(safeErrorMessage(error));
  } finally {
    loading.value = false;
  }
}

async function download(materialRefId: Identifier) {
  try {
    const access = (await runtime.service.person.archive.reviewMaterial(applicationId.value, materialRefId)).data;
    await runtime.downloadMaterial(access);
  } catch (error) {
    runtime.error(safeErrorMessage(error));
  }
}

async function completeReview() {
  if (!taskId.value) return runtime.warning('缺少流程任务编号');
  if (!reason.value.trim()) return runtime.warning('请填写审核意见');
  await runtime.confirm('确认提交当前流程审核结果？');
  saving.value = true;
  try {
    await runtime.completeWorkflowTask({
      taskId: taskId.value,
      comment: reason.value.trim(),
      variables: { profileDecision: decision.value }
    });
    runtime.success('审核结果已提交');
    await runtime.closeCurrentPage();
  } catch (error) {
    runtime.error(safeErrorMessage(error));
    await load();
  } finally {
    saving.value = false;
  }
}

async function overrideReview() {
  if (!reason.value.trim()) return runtime.warning('请填写覆盖原因');
  await runtime.confirm('覆盖决定会终止活动流程，确认继续？');
  saving.value = true;
  try {
    await runtime.service.person.archive.decide(applicationId.value, {
      decision: decision.value === 'APPROVE' ? 'APPROVED' : 'REJECTED',
      reason: reason.value.trim()
    });
    runtime.success('管理员决定已生效');
    await runtime.closeCurrentPage();
  } catch (error) {
    runtime.error(safeErrorMessage(error));
    await load();
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.person-review-page {
  padding: 16px;
}
header,
footer {
  display: flex;
  align-items: center;
  gap: 10px;
}
header {
  justify-content: space-between;
  margin-bottom: 12px;
}
h2,
h3 {
  margin: 0;
}
section {
  margin-top: 18px;
}
pre {
  margin: 8px 0 0;
  padding: 12px;
  overflow: auto;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color);
}
.decision-form {
  max-width: 720px;
  margin-top: 18px;
}
</style>
