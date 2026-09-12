<template>
  <main class="verification-page" v-loading="loading">
    <header class="verification-page__header">
      <div>
        <span class="eyebrow">PROFILE / PERSON</span>
        <h1>个人认证</h1>
        <p>请填写真实身份信息，保存后可继续补充，提交后进入审核。</p>
      </div>
      <el-tag v-if="form.status" :type="statusType">{{ statusText }}</el-tag>
    </header>
    <el-form :model="form" label-position="top" class="verification-form">
      <div class="form-grid">
        <el-form-item label="姓名"><el-input v-model="form.fullName" maxlength="100" /></el-form-item>
        <el-form-item label="证件类型">
          <el-select v-model="form.documentTypeCode" filterable>
            <el-option v-for="item in documentTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="证件号码"><el-input v-model="form.documentNumber" maxlength="128" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender"><el-option label="男" value="0" /><el-option label="女" value="1" /></el-select>
        </el-form-item>
        <el-form-item label="出生日期"><el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="有效期起"><el-date-picker v-model="form.validFrom" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="有效期止"><el-date-picker v-model="form.validUntil" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      </div>
      <div class="form-actions">
        <el-button :loading="saving" @click="save">保存草稿</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">提交认证</el-button>
      </div>
    </el-form>
  </main>
</template>

<script setup lang="ts">
import type { PersonApplication, PersonIdentity } from '@namewta/domain-profile';
import { computed, onMounted, reactive, ref } from 'vue';
import type { ProfileSelfWebRuntime } from './runtime';

const { runtime } = defineProps<{ runtime: ProfileSelfWebRuntime }>();
const loading = ref(false);
const saving = ref(false);
const submitting = ref(false);
const application = ref<PersonApplication | null>(null);
const emptyIdentity = (): PersonIdentity => ({
  birthDate: '',
  documentNumber: '',
  documentTypeCode: 'CN_RESIDENT_ID',
  fullName: '',
  gender: '',
  validFrom: '',
  validUntil: ''
});
const form = reactive({ ...emptyIdentity(), expectedVersion: 0, status: '' });
const documentTypes = [
  ['居民身份证', 'CN_RESIDENT_ID'],
  ['香港居民身份证', 'HK_RESIDENT_ID'],
  ['澳门居民身份证', 'MO_RESIDENT_ID'],
  ['台湾居民身份证', 'TW_RESIDENT_ID'],
  ['港澳居民居住证', 'HK_MACAO_RESIDENCE_PERMIT'],
  ['台湾居民居住证', 'TW_RESIDENCE_PERMIT'],
  ['中国护照', 'CN_PASSPORT']
].map(([label, value]) => ({ label, value }));
const statusText = computed(() => ({ DRAFT: '草稿', BACK: '已退回', WAITING: '审核中', FINISH: '已完成' })[form.status] ?? form.status);
const statusType = computed(() => (form.status === 'FINISH' ? 'success' : form.status === 'WAITING' ? 'warning' : 'info'));

function setApplication(value: PersonApplication | null) {
  application.value = value;
  if (value) Object.assign(form, value, { expectedVersion: value.version });
}

async function load() {
  loading.value = true;
  try {
    setApplication((await runtime.service.person.application.current()).data ?? null);
  } catch (error) {
    runtime.error(error instanceof Error ? error.message : '认证资料加载失败');
  } finally {
    loading.value = false;
  }
}

function valid() {
  const values = [form.fullName, form.documentTypeCode, form.documentNumber, form.gender, form.birthDate, form.validFrom, form.validUntil];
  if (values.some(value => !String(value).trim())) {
    runtime.warning('请完整填写个人身份信息');
    return false;
  }
  return true;
}

async function save() {
  if (!valid()) return;
  saving.value = true;
  try {
    const result = await runtime.service.person.application.save({ ...emptyIdentity(), ...form, expectedVersion: form.expectedVersion });
    setApplication(result.data ?? null);
    runtime.success('个人认证草稿已保存');
  } catch (error) {
    runtime.error(error instanceof Error ? error.message : '保存失败，请稍后重试');
  } finally {
    saving.value = false;
  }
}

async function submit() {
  if (!valid()) return;
  await runtime.confirm('提交后资料将进入审核，确认继续吗？');
  submitting.value = true;
  try {
    const saved = await runtime.service.person.application.save({ ...emptyIdentity(), ...form, expectedVersion: form.expectedVersion });
    const version = saved.data?.version ?? form.expectedVersion;
    setApplication(saved.data ?? null);
    const result = await runtime.service.person.application.submit(version);
    setApplication(result.data ?? null);
    runtime.success('个人认证已提交');
  } catch (error) {
    runtime.error(error instanceof Error ? error.message : '提交失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.verification-page { max-width: 1040px; margin: 0 auto; padding: 40px 32px; }
.verification-page__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 28px; }
.eyebrow { color: #0f766e; font-size: 12px; font-weight: 700; letter-spacing: .12em; }
h1, p { margin: 0; }
h1 { margin-top: 8px; color: #172033; font-size: 30px; }
.verification-page__header p { margin-top: 8px; color: #64748b; }
.verification-form { padding: 28px; border: 1px solid #dbe4ea; border-radius: 8px; background: #fff; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 4px 20px; }
.form-actions { display: flex; gap: 10px; margin-top: 16px; }
@media (max-width: 700px) { .verification-page { padding: 28px 18px; } .verification-form { padding: 18px; } .form-grid { grid-template-columns: 1fr; } }
</style>
