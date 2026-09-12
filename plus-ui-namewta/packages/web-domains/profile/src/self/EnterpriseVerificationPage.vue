<template>
  <main class="verification-page" v-loading="loading">
    <header class="verification-page__header">
      <div>
        <span class="eyebrow">PROFILE / ENTERPRISE</span>
        <h1>企业认证</h1>
        <p>填写企业主体、法定代表人和联系资料，提交后进入审核。</p>
      </div>
      <el-tag v-if="form.status" :type="statusType">{{ statusText }}</el-tag>
    </header>
    <el-form :model="form" label-position="top" class="verification-form">
      <el-divider content-position="left">企业主体</el-divider>
      <div class="form-grid">
        <el-form-item label="企业名称"><el-input v-model="form.enterpriseName" /></el-form-item>
        <el-form-item label="统一信用代码"><el-input v-model="form.unifiedCreditCode" /></el-form-item>
        <el-form-item label="企业类型"><el-input v-model="form.enterpriseType" /></el-form-item>
        <el-form-item label="成立日期"><el-date-picker v-model="form.establishedDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="营业期限起"><el-date-picker v-model="form.businessTermFrom" type="date" value-format="YYYY-MM-DD" clearable /></el-form-item>
        <el-form-item label="营业期限止"><el-date-picker v-model="form.businessTermUntil" type="date" value-format="YYYY-MM-DD" clearable /></el-form-item>
        <el-form-item label="注册地址"><el-input v-model="form.registeredAddress" /></el-form-item>
        <el-form-item label="经营范围"><el-input v-model="form.businessScope" type="textarea" :rows="2" /></el-form-item>
      </div>
      <el-divider content-position="left">法定代表人</el-divider>
      <div class="form-grid">
        <el-form-item label="法定代表人"><el-input v-model="form.legalRepresentativeName" /></el-form-item>
        <el-form-item label="证件类型"><el-select v-model="form.legalDocumentTypeCode" filterable><el-option v-for="item in documentTypes" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="证件号码"><el-input v-model="form.legalDocumentNumber" /></el-form-item>
        <el-form-item label="经办人是法定代表人"><el-switch v-model="form.handlerIsLegalRepresentative" /></el-form-item>
      </div>
      <el-divider content-position="left">联系信息</el-divider>
      <div class="form-grid">
        <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
        <el-form-item label="企业邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="注册资本"><el-input-number v-model="form.registeredCapital" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="行业编码"><el-input v-model="form.industryCode" /></el-form-item>
        <el-form-item label="企业网站"><el-input v-model="form.website" /></el-form-item>
      </div>
      <div class="form-actions"><el-button :loading="saving" @click="save">保存草稿</el-button><el-button type="primary" :loading="submitting" @click="submit">提交认证</el-button></div>
    </el-form>
  </main>
</template>

<script setup lang="ts">
import type { EnterpriseApplication, EnterpriseIdentity } from '@namewta/domain-profile';
import { computed, onMounted, reactive, ref } from 'vue';
import type { ProfileSelfWebRuntime } from './runtime';

const { runtime } = defineProps<{ runtime: ProfileSelfWebRuntime }>();
const loading = ref(false); const saving = ref(false); const submitting = ref(false);
const emptyIdentity = (): EnterpriseIdentity => ({ businessScope: '', businessTermFrom: '', businessTermUntil: '', contactName: '', contactPhone: '', email: '', enterpriseName: '', enterpriseType: '', establishedDate: '', industryCode: '', legalDocumentNumber: '', legalDocumentTypeCode: 'CN_RESIDENT_ID', legalRepresentativeName: '', registeredAddress: '', registeredCapital: 0, unifiedCreditCode: '', website: '' });
const form = reactive({ ...emptyIdentity(), handlerIsLegalRepresentative: true, expectedVersion: 0, status: '' });
const documentTypes = [['居民身份证', 'CN_RESIDENT_ID'], ['香港居民身份证', 'HK_RESIDENT_ID'], ['澳门居民身份证', 'MO_RESIDENT_ID'], ['台湾居民身份证', 'TW_RESIDENT_ID'], ['中国护照', 'CN_PASSPORT']].map(([label, value]) => ({ label, value }));
const statusText = computed(() => ({ DRAFT: '草稿', BACK: '已退回', WAITING: '审核中', FINISH: '已完成' })[form.status] ?? form.status);
const statusType = computed(() => (form.status === 'FINISH' ? 'success' : form.status === 'WAITING' ? 'warning' : 'info'));
function setApplication(value: EnterpriseApplication | null) { if (value) Object.assign(form, value, { expectedVersion: value.version }); }
async function load() { loading.value = true; try { setApplication((await runtime.service.enterprise.application.current()).data ?? null); } catch (error) { runtime.error(error instanceof Error ? error.message : '认证资料加载失败'); } finally { loading.value = false; } }
function valid() { const required = [form.enterpriseName, form.unifiedCreditCode, form.enterpriseType, form.legalRepresentativeName, form.legalDocumentTypeCode, form.legalDocumentNumber, form.establishedDate, form.registeredAddress, form.businessScope, form.contactName, form.contactPhone]; if (required.some(value => !String(value).trim())) { runtime.warning('请完整填写企业认证资料'); return false; } return true; }
async function save() { if (!valid()) return; saving.value = true; try { const result = await runtime.service.enterprise.application.save({ ...emptyIdentity(), ...form, expectedVersion: form.expectedVersion }); setApplication(result.data ?? null); runtime.success('企业认证草稿已保存'); } catch (error) { runtime.error(error instanceof Error ? error.message : '保存失败，请稍后重试'); } finally { saving.value = false; } }
async function submit() { if (!valid()) return; await runtime.confirm('提交后资料将进入审核，确认继续吗？'); submitting.value = true; try { const saved = await runtime.service.enterprise.application.save({ ...emptyIdentity(), ...form, expectedVersion: form.expectedVersion }); const version = saved.data?.version ?? form.expectedVersion; setApplication(saved.data ?? null); const result = await runtime.service.enterprise.application.submit(version); setApplication(result.data ?? null); runtime.success('企业认证已提交'); } catch (error) { runtime.error(error instanceof Error ? error.message : '提交失败，请稍后重试'); } finally { submitting.value = false; } }
onMounted(load);
</script>

<style scoped>
.verification-page { max-width: 1100px; margin: 0 auto; padding: 40px 32px; }
.verification-page__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 28px; }
.eyebrow { color: #2563eb; font-size: 12px; font-weight: 700; letter-spacing: .12em; }
h1, p { margin: 0; } h1 { margin-top: 8px; color: #172033; font-size: 30px; } .verification-page__header p { margin-top: 8px; color: #64748b; }
.verification-form { padding: 28px; border: 1px solid #dbe4ea; border-radius: 8px; background: #fff; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 4px 20px; } .form-actions { display: flex; gap: 10px; margin-top: 16px; }
@media (max-width: 700px) { .verification-page { padding: 28px 18px; } .verification-form { padding: 18px; } .form-grid { grid-template-columns: 1fr; } }
</style>
