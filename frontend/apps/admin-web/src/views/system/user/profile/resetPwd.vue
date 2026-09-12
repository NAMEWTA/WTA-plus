<template>
  <el-form ref="pwdRef" :model="user" :rules="rules" label-width="80px" class="profile-form">
    <el-form-item label="旧密码" prop="oldPassword">
      <el-input v-model="user.oldPassword" placeholder="请输入旧密码" type="password" show-password />
    </el-form-item>
    <el-form-item label="新密码" prop="newPassword">
      <el-input v-model="user.newPassword" placeholder="请输入新密码" type="password" show-password />
    </el-form-item>
    <el-form-item label="确认密码" prop="confirmPassword">
      <el-input v-model="user.confirmPassword" placeholder="请确认新密码" type="password" show-password />
    </el-form-item>
    <el-form-item class="profile-form__actions">
      <el-button type="primary" :loading="submitting" :disabled="policyState !== 'available'" @click="submit">
        保存
      </el-button>
      <el-button @click="close">关闭</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import type { ResetPwdForm } from '@namewta/domain-system';
import {
  requirePasswordPolicy,
  validatePassword,
  type PasswordPolicy,
  type PasswordViolationReason
} from '@namewta/domain-admin';
import { useI18n } from 'vue-i18n';
import modal from '@/application/host/feedback';
import tab from '@/application/host/navigation';
import { identityAccessService, systemService } from '@/application/services';

const pwdRef = ref<ElFormInstance>();
const { t } = useI18n();
const passwordPolicy = ref<PasswordPolicy>();
const policyState = ref<'loading' | 'available' | 'unavailable'>('loading');
const submitting = ref(false);
const user = ref<ResetPwdForm>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const equalToPassword = (rule: any, value: string, callback: any) => {
  if (user.value.newPassword !== value) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const passwordViolationMessage = (reason: PasswordViolationReason) =>
  t(`passwordPolicy.${reason}`, {
    min: passwordPolicy.value?.minimumLength,
    max: passwordPolicy.value?.maximumLength,
    specials: passwordPolicy.value?.allowedSpecialCharacters
  });

const validatePasswordPolicy = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!passwordPolicy.value) {
    callback(new Error(t('passwordPolicy.unavailable')));
    return;
  }
  const violation = validatePassword(passwordPolicy.value, value).at(0);
  callback(violation ? new Error(passwordViolationMessage(violation.reason)) : undefined);
};
const rules = ref({
  oldPassword: [{ required: true, message: '旧密码不能为空', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '新密码不能为空', trigger: 'blur' },
    { validator: validatePasswordPolicy, trigger: ['blur', 'change'] }
  ],
  confirmPassword: [
    { required: true, message: '确认密码不能为空', trigger: 'blur' },
    {
      required: true,
      validator: equalToPassword,
      trigger: 'blur'
    }
  ]
});

/** 提交按钮 */
const submit = () => {
  if (policyState.value !== 'available') return;
  pwdRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      submitting.value = true;
      try {
        await systemService.users.updatePassword(user.value.oldPassword, user.value.newPassword);
        modal.msgSuccess('修改成功');
      } finally {
        submitting.value = false;
      }
    }
  });
};
/** 关闭按钮 */
const close = () => {
  tab.closePage();
};

onMounted(async () => {
  policyState.value = 'loading';
  try {
    passwordPolicy.value = requirePasswordPolicy(await identityAccessService.getClientContext());
    policyState.value = 'available';
  } catch {
    passwordPolicy.value = undefined;
    policyState.value = 'unavailable';
    ElMessage.warning(t('passwordPolicy.unavailable'));
  }
});
</script>

<style lang="scss" scoped>
.profile-form {
  max-width: 520px;
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 12px;
}

.profile-form :deep(.el-button) {
  border-radius: 10px;
}

.profile-form__actions :deep(.el-form-item__content) {
  display: flex;
  gap: 8px;
}
</style>
