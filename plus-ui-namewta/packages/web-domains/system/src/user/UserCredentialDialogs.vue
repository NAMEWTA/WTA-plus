<template>
  <el-dialog
    v-model="state.resetVisible"
    title="重置永久密码"
    width="480px"
    append-to-body
    :close-on-click-modal="false"
    @closed="clearSensitiveState"
  >
    <div v-loading="resetLoading" class="credential-dialog-body">
      <p class="credential-user">{{ targetUserName }}</p>
      <el-form ref="resetFormRef" :model="state" :rules="resetRules" label-position="top">
        <el-form-item label="新密码" prop="candidate">
          <el-input
            v-model="state.candidate"
            type="password"
            show-password
            autocomplete="new-password"
            :maxlength="passwordPolicy?.maximumLength"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmation">
          <el-input
            v-model="state.confirmation"
            type="password"
            show-password
            autocomplete="new-password"
            :maxlength="passwordPolicy?.maximumLength"
          />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="state.resetVisible = false">取消</el-button>
      <el-button type="primary" :loading="resetSubmitting" :disabled="resetLoading" @click="submitReset">
        确定重置
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="state.temporaryVisible"
    title="一次性临时密码"
    width="480px"
    append-to-body
    :close-on-click-modal="false"
    @closed="clearSensitiveState"
  >
    <div v-loading="temporaryLoading" class="credential-dialog-body">
      <p class="credential-user">{{ targetUserName }}</p>
      <template v-if="state.temporaryPassword">
        <p class="credential-hint">
          此密码仅展示一次，将在 {{ state.expiresInSeconds }} 秒后失效，成功登录后立即作废。
        </p>
        <div class="temporary-password-row">
          <el-input :model-value="state.temporaryPassword" readonly aria-label="一次性临时密码" />
          <el-tooltip content="复制临时密码" placement="top">
            <el-button icon="DocumentCopy" aria-label="复制临时密码" @click="copyTemporaryPassword" />
          </el-tooltip>
        </div>
      </template>
    </div>
    <template #footer>
      <el-button type="primary" :disabled="temporaryLoading" @click="state.temporaryVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { Identifier, UserVO } from '@namewta/domain-system';
import type { FormInstance, FormRules } from 'element-plus';
import { reactive, ref } from 'vue';
import type { SystemPasswordPolicy, SystemWebRuntime } from '../runtime';
import { createCredentialState, describePasswordViolations, resetCredentialState } from './credential-workflow';

const { runtime } = defineProps<{ runtime: SystemWebRuntime }>();
const state = reactive(createCredentialState());
const resetFormRef = ref<FormInstance>();
const passwordPolicy = ref<SystemPasswordPolicy>();
const targetUserId = ref<Identifier>();
const targetUserName = ref('');
const resetLoading = ref(false);
const resetSubmitting = ref(false);
const temporaryLoading = ref(false);
let requestId = 0;

const resetRules: FormRules = {
  candidate: [
    { required: true, message: '新密码不能为空', trigger: 'blur' },
    {
      trigger: ['blur', 'change'],
      validator: (_rule, value: string, callback) => {
        if (!passwordPolicy.value) return callback(new Error('密码策略配置不可用'));
        const violations = runtime.passwordPolicy.validate(passwordPolicy.value, value);
        const messages = describePasswordViolations(passwordPolicy.value, violations);
        callback(messages.length ? new Error(messages.join('；')) : undefined);
      }
    }
  ],
  confirmation: [
    { required: true, message: '确认密码不能为空', trigger: 'blur' },
    {
      trigger: ['blur', 'change'],
      validator: (_rule, value: string, callback) =>
        callback(value === state.candidate ? undefined : new Error('两次输入的密码不一致'))
    }
  ]
};

function identify(row: Partial<UserVO>): Identifier | undefined {
  if (row.userId === undefined || row.userId === null) return undefined;
  targetUserId.value = row.userId;
  targetUserName.value = row.userName ?? String(row.userId);
  return row.userId;
}

function clearSensitiveState(): void {
  requestId += 1;
  resetCredentialState(state);
  passwordPolicy.value = undefined;
  targetUserId.value = undefined;
  targetUserName.value = '';
  resetLoading.value = false;
  resetSubmitting.value = false;
  temporaryLoading.value = false;
  resetFormRef.value?.clearValidate();
}

async function openReset(row: Partial<UserVO>): Promise<void> {
  if (resetLoading.value || resetSubmitting.value || temporaryLoading.value) return;
  clearSensitiveState();
  const userId = identify(row);
  if (userId === undefined) return;
  const activeRequest = ++requestId;
  state.resetVisible = true;
  resetLoading.value = true;
  try {
    const [candidate, policy] = await Promise.all([
      runtime.service.users.passwordResetCandidate(userId),
      runtime.passwordPolicy.load()
    ]);
    if (activeRequest !== requestId) return;
    passwordPolicy.value = policy;
    state.candidate = candidate.data.password;
    state.confirmation = candidate.data.password;
  } catch {
    if (activeRequest === requestId) clearSensitiveState();
  } finally {
    if (activeRequest === requestId) resetLoading.value = false;
  }
}

async function submitReset(): Promise<void> {
  if (resetSubmitting.value || targetUserId.value === undefined) return;
  const valid = await resetFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  resetSubmitting.value = true;
  try {
    await runtime.service.users.resetPassword(targetUserId.value, state.candidate);
    runtime.success('密码重置成功');
    clearSensitiveState();
  } finally {
    resetSubmitting.value = false;
  }
}

async function issueTemporary(row: Partial<UserVO>): Promise<void> {
  if (resetLoading.value || resetSubmitting.value || temporaryLoading.value) return;
  clearSensitiveState();
  const userId = identify(row);
  if (userId === undefined) return;
  const activeRequest = ++requestId;
  state.temporaryVisible = true;
  temporaryLoading.value = true;
  try {
    const response = await runtime.service.users.issueTemporaryPassword(userId);
    if (activeRequest !== requestId) return;
    state.temporaryPassword = response.data.password;
    state.expiresInSeconds = response.data.expiresInSeconds;
  } catch {
    if (activeRequest === requestId) clearSensitiveState();
  } finally {
    if (activeRequest === requestId) temporaryLoading.value = false;
  }
}

async function copyTemporaryPassword(): Promise<void> {
  if (!state.temporaryPassword) return;
  try {
    await runtime.copyText(state.temporaryPassword);
    runtime.success('临时密码已复制');
  } catch {
    runtime.error('复制失败，请手动复制');
  }
}

defineExpose({ issueTemporary, openReset });
</script>

<style scoped>
.credential-dialog-body {
  min-height: 132px;
}

.credential-user {
  color: var(--el-text-color-secondary);
  margin: 0 0 16px;
}

.credential-hint {
  color: var(--el-text-color-regular);
  line-height: 1.6;
  margin: 0 0 14px;
}

.temporary-password-row {
  align-items: center;
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(0, 1fr) 40px;
}

:deep(.el-form-item__error) {
  line-height: 1.5;
  padding-top: 4px;
  position: static;
}
</style>
