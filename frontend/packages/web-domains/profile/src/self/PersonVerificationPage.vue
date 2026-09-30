<template>
  <main class="verification-page" v-loading="loading">
    <header class="verification-page__header">
      <div>
        <span class="eyebrow">PROFILE / PERSON</span>
        <h1>实名认证</h1>
        <p>请填写真实身份信息，保存后可继续补充，提交后进入审核。</p>
      </div>
      <el-tag v-if="form.status" :type="statusType">{{ statusText }}</el-tag>
    </header>
    <el-alert v-if="returnReason" type="warning" :title="`退回原因：${returnReason}`" :closable="false" show-icon />
    <el-card v-if="certified && !application" shadow="never" class="verification-form">
      <p class="certified-time">认证完成时间：{{ certified.verifiedAt || '—' }}</p>
      <IdentityDetails profile-type="PERSON" :identity="certified.identity" />
    </el-card>
    <el-card v-else shadow="never" class="verification-form">
      <PersonIdentityForm
        :model="form"
        :disabled="locked || materialBusy || materialPending || !editable"
        @update:model="Object.assign(form, $event)"
      ></PersonIdentityForm>
    </el-card>
    <SelfMaterials
      v-if="loaded && (!certified || application)"
      ref="materialSection"
      :runtime="runtime"
      profile-type="PERSON"
      :owner-id="application?.personApplicationId"
      :document-type-code="form.documentTypeCode"
      :handler-is-legal-representative="true"
      :editable="editable"
      :locked="locked"
      @busy="materialBusy = $event"
      @pending="materialPending = $event"
    />
    <div v-if="editable" class="form-actions">
      <el-button :loading="saving" :disabled="locked || materialBusy || materialPending || !loaded" @click="save">
        保存草稿
      </el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="locked || materialBusy || materialPending || !loaded"
        @click="submit"
      >
        提交认证
      </el-button>
    </div>
    <el-button v-if="!loaded && !loading" @click="load">重新加载申请</el-button>
  </main>
</template>

<script setup lang="ts">
import type { PersonApplication, PersonIdentity } from '@namewta/domain-profile';
import type { PersonCertificationSummary, CertificationStatus } from '@namewta/domain-profile';
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import type { ProfileSelfWebRuntime } from './runtime';
import IdentityDetails from '../IdentityDetails.vue';
import PersonIdentityForm from '../person/PersonIdentityForm.vue';
import { certificationLabel } from './certification-status';
import SelfMaterials from './SelfMaterials.vue';

const { runtime } = defineProps<{ runtime: ProfileSelfWebRuntime }>();
const certified = ref<PersonCertificationSummary['certifiedProfile']>(null);
const returnReason = ref<string | null>(null);
let active = true;
onBeforeUnmount(() => {
  active = false;
});
const loading = ref(false);
const saving = ref(false);
const submitting = ref(false);
const application = ref<PersonApplication | null>(null);
const loaded = ref(false);
const materialBusy = ref(false);
const materialPending = ref(false);
const materialSection = ref<InstanceType<typeof SelfMaterials>>();
const locked = computed(() => loading.value || saving.value || submitting.value);
const editable = computed(
  () =>
    (!certified.value || Boolean(application.value)) &&
    (!form.status || ['UNVERIFIED', 'DRAFT', 'BACK', 'CANCEL'].includes(form.status))
);
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
const statusText = computed(() => certificationLabel(form.status as CertificationStatus));
const statusType = computed(() =>
  form.status === 'VERIFIED' ? 'success' : form.status === 'WAITING' ? 'warning' : 'info'
);

function setApplication(value: PersonApplication | null) {
  application.value = value;
  if (value) Object.assign(form, value, { expectedVersion: value.version });
}

async function load() {
  loading.value = true;
  try {
    const summary = (await runtime.service.person.application.summary()).data;
    if (!active) return;
    certified.value = summary.certifiedProfile;
    returnReason.value = summary.returnReason;
    Object.assign(form, emptyIdentity(), { expectedVersion: 0, status: summary.status });
    setApplication(summary.currentApplication);
    loaded.value = true;
  } catch (error) {
    if (active) runtime.error(error instanceof Error ? error.message : '认证资料加载失败');
  } finally {
    loading.value = false;
  }
}

function valid() {
  const values = [
    form.fullName,
    form.documentTypeCode,
    form.documentNumber,
    form.gender,
    form.birthDate,
    form.validFrom,
    form.validUntil
  ];
  if (values.some(value => !String(value).trim())) {
    runtime.warning('请完整填写个人身份信息');
    return false;
  }
  return true;
}

async function save() {
  if (!loaded.value || !editable.value || locked.value || materialBusy.value || materialPending.value) return;
  saving.value = true;
  try {
    const result = await runtime.service.person.application.save({
      ...emptyIdentity(),
      ...form,
      expectedVersion: form.expectedVersion
    });
    if (!active) return;
    setApplication(result.data ?? null);
    runtime.success('实名认证草稿已保存');
  } catch (error) {
    if (active) runtime.error(error instanceof Error ? error.message : '保存失败，请稍后重试');
  } finally {
    saving.value = false;
  }
}

async function submit() {
  if (
    !loaded.value ||
    !editable.value ||
    locked.value ||
    materialBusy.value ||
    materialPending.value ||
    !valid() ||
    !materialSection.value?.validate()
  )
    return;
  submitting.value = true;
  try {
    try {
      await runtime.confirm('提交后资料将进入审核，确认继续吗？');
    } catch {
      return;
    }
    if (!active) return;
    const saved = await runtime.service.person.application.save({
      ...emptyIdentity(),
      ...form,
      expectedVersion: form.expectedVersion
    });
    const version = saved.data?.version ?? form.expectedVersion;
    if (!active) return;
    setApplication(saved.data ?? null);
    const result = await runtime.service.person.application.submit(version);
    if (!active) return;
    setApplication(result.data ?? null);
    returnReason.value = null;
    runtime.success('实名认证已提交');
  } catch (error) {
    if (active) runtime.error(materialSection.value?.showError(error) ?? '提交失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.verification-page {
  min-width: 0;
}
.verification-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
h1 {
  margin: 0;
  font-size: 20px;
  color: var(--app-text-title);
}
.verification-page__header p,
.certified-time {
  margin: 8px 0;
  color: var(--app-text-muted);
}
.eyebrow {
  display: none;
}
.verification-form {
  margin-top: 12px;
}
.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
</style>
