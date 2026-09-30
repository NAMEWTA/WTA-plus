<template>
  <div class="profile-review-page" v-loading="loading">
    <el-alert v-if="failure" :title="failure" type="error" :closable="false" show-icon>
      <el-button link @click="load">重新加载</el-button>
    </el-alert>
    <el-card v-if="context" shadow="never">
      <template #header>
        <header>
          <h2>{{ profileType === 'PERSON' ? '实名认证审核' : '企业认证审核' }}</h2>
          <el-tag>{{ statusLabel }}</el-tag>
        </header>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请编号">{{ context.applicationId }}</el-descriptions-item>
        <el-descriptions-item label="提交序号">{{ context.submissionSeq }}</el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ context.submittedTime }}</el-descriptions-item>
      </el-descriptions>
      <section>
        <h3>申请资料</h3>
        <IdentityDetails v-if="identity" :profile-type="profileType" :identity="identity" />
        <el-alert v-else title="申请资料格式异常，请联系管理员。" type="error" :closable="false" />
      </section>
      <section>
        <h3>申请材料</h3>
        <el-table :data="context.materials" row-key="materialRefId" border>
          <el-table-column prop="materialTagName" label="材料标签" />
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button link type="primary" @click="download(row.materialRefId)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
      <template v-if="!readOnly && (canComplete || canOverride)">
        <el-form label-position="top" class="decision-form" :disabled="saving">
          <el-form-item v-if="canComplete" label="流程审核结果">
            <el-segmented v-model="decision" :options="decisionOptions" />
          </el-form-item>
          <el-form-item v-if="canOverride" label="管理员覆盖结果">
            <el-segmented v-model="overrideDecision" :options="overrideOptions" />
          </el-form-item>
          <el-form-item label="审核意见">
            <el-input v-model="reason" type="textarea" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>
        <footer>
          <el-button v-if="canComplete" type="primary" :loading="saving" :disabled="!identity" @click="completeReview">
            提交流程审核
          </el-button>
          <el-button
            v-if="canOverride"
            type="danger"
            plain
            :loading="saving"
            :disabled="!identity"
            @click="overrideReview"
          >
            管理员覆盖决定
          </el-button>
        </footer>
      </template>
      <p v-else class="read-only">当前记录为只读。</p>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import {
  projectEnterpriseIdentity,
  projectPersonIdentity,
  type Identifier,
  type ProfileType,
  type ReviewContext
} from '@namewta/domain-profile';
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import type { ProfileReviewWebRuntime } from './runtime';
import IdentityDetails from '../IdentityDetails.vue';
const props = defineProps<{ runtime: ProfileReviewWebRuntime; profileType: ProfileType }>();
const route = useRoute();
const kind = computed(() => (props.profileType === 'PERSON' ? 'person' : 'enterprise'));
const service = computed(() => props.runtime.service[kind.value]);
const applicationId = computed(() => String(route.query.id ?? ''));
const taskId = computed(() => String(route.query.taskId ?? ''));
const context = ref<ReviewContext>();
const loading = ref(false);
const saving = ref(false);
const failure = ref('');
const decision = ref<'APPROVE' | 'RETURN'>('APPROVE');
const reason = ref('');
const overrideDecision = ref<'APPROVED' | 'REJECTED'>('APPROVED');
const overrideOptions = [
  { label: '通过', value: 'APPROVED' },
  { label: '拒绝（终态）', value: 'REJECTED' }
];
const decisionOptions = [
  { label: '通过', value: 'APPROVE' },
  { label: '退回补充', value: 'RETURN' }
];
let generation = 0;
let active = true;
onBeforeUnmount(() => {
  active = false;
  generation++;
});
const readOnly = computed(() => route.query.type === 'view' || context.value?.status !== 'WAITING');
const canComplete = computed(
  () => Boolean(taskId.value) && props.runtime.hasPermission(`profile:${kind.value}:task-review`)
);
const canOverride = computed(() => props.runtime.hasPermission(`profile:${kind.value}:override`));
const statusLabel = computed(
  () =>
    ({ WAITING: '审核中', FINISH: '已完成', BACK: '已退回', CANCEL: '已撤回', DRAFT: '草稿' })[
      context.value?.status ?? ''
    ] ?? context.value?.status
);
const identity = computed(() => {
  try {
    const source: unknown = JSON.parse(context.value?.fieldSnapshotJson ?? 'null');
    return props.profileType === 'PERSON' ? projectPersonIdentity(source) : projectEnterpriseIdentity(source);
  } catch {
    return null;
  }
});
async function load() {
  const current = ++generation;
  context.value = undefined;
  loading.value = true;
  failure.value = '';
  try {
    if (taskId.value) {
      if (!props.runtime.hasPermission(`profile:${kind.value}:task-review`)) throw new Error('没有此任务的审核权限');
      const result = await service.value.taskReview.context(taskId.value);
      if (current === generation) context.value = result.data;
    } else {
      if (!applicationId.value || !props.runtime.hasPermission(`profile:${kind.value}:review`))
        throw new Error('缺少审核任务或档案查看权限');
      const result = await service.value.archive.review(applicationId.value);
      if (current === generation) context.value = result.data;
    }
  } catch (error) {
    if (current === generation) failure.value = error instanceof Error ? error.message : '审核资料加载失败';
  } finally {
    if (current === generation) loading.value = false;
  }
}
async function download(materialRefId: Identifier) {
  if (!context.value) return;
  const current = generation;
  try {
    const access = taskId.value
      ? await service.value.taskReview.material(taskId.value, materialRefId)
      : await service.value.archive.reviewMaterial(context.value.applicationId, materialRefId);
    if (current === generation && active) await props.runtime.downloadMaterial(access.data);
  } catch (error) {
    if (active) props.runtime.error(error instanceof Error ? error.message : '材料加载失败');
  }
}
async function decide(override: boolean) {
  if (
    saving.value ||
    !context.value ||
    readOnly.value ||
    !identity.value ||
    (override ? !canOverride.value : !canComplete.value)
  )
    return;
  if (!reason.value.trim()) return props.runtime.warning('请填写审核意见');
  saving.value = true;
  const current = generation;
  try {
    try {
      await props.runtime.confirm(
        override
          ? overrideDecision.value === 'REJECTED'
            ? '管理员覆盖将直接拒绝本次申请并终止活动流程，不进入补充资料后重提状态。确认拒绝？'
            : '管理员覆盖将直接通过本次申请并终止活动流程。确认通过？'
          : '确认提交当前审核结果？'
      );
    } catch {
      return;
    }
    if (!active || current !== generation) return;
    if (override)
      await service.value.archive.decide(context.value.applicationId, {
        decision: overrideDecision.value,
        reason: reason.value.trim()
      });
    else
      await service.value.taskReview.decide(taskId.value, {
        decision: decision.value,
        reason: reason.value.trim(),
        snapshotVersion: context.value.submissionSeq
      });
    if (!active || current !== generation) return;
    props.runtime.success('审核结果已提交');
    await props.runtime.closeCurrentPage();
  } catch (error) {
    if (active && current === generation) {
      props.runtime.error(error instanceof Error ? error.message : '审核未完成，请重新加载');
      await load();
    }
  } finally {
    saving.value = false;
  }
}
const completeReview = () => decide(false);
const overrideReview = () => decide(true);
watch(
  () => [taskId.value, applicationId.value, props.profileType],
  () => {
    reason.value = '';
    void load();
  },
  { immediate: true }
);
</script>
<style scoped>
header,
footer {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
header {
  justify-content: space-between;
}
h2 {
  margin: 0;
  font-size: 20px;
}
h3 {
  margin: 0 0 12px;
  font-size: 16px;
}
section {
  margin-top: 20px;
}
.decision-form {
  max-width: 720px;
  margin-top: 20px;
}
.read-only {
  color: var(--app-text-muted);
}
</style>
