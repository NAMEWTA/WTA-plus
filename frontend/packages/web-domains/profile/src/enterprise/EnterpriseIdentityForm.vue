<template>
  <el-form :model="model" :disabled="disabled" label-position="top">
    <el-row :gutter="12">
      <el-col :span="24"><el-divider content-position="left">企业主体</el-divider></el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="企业名称">
          <el-input :model-value="model.enterpriseName" @update:model-value="update('enterpriseName', $event)" />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="统一信用代码">
          <el-input :model-value="model.unifiedCreditCode" @update:model-value="update('unifiedCreditCode', $event)" />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="企业类型">
          <el-input :model-value="model.enterpriseType" @update:model-value="update('enterpriseType', $event)" />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="成立日期">
          <el-date-picker
            :model-value="model.establishedDate"
            @update:model-value="update('establishedDate', $event)"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="营业期限起">
          <el-date-picker
            :model-value="model.businessTermFrom"
            @update:model-value="update('businessTermFrom', $event)"
            type="date"
            value-format="YYYY-MM-DD"
            clearable
          />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="营业期限止">
          <el-date-picker
            :model-value="model.businessTermUntil"
            @update:model-value="update('businessTermUntil', $event)"
            type="date"
            value-format="YYYY-MM-DD"
            clearable
          />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="注册地址">
          <el-input :model-value="model.registeredAddress" @update:model-value="update('registeredAddress', $event)" />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="经营范围">
          <el-input
            :model-value="model.businessScope"
            @update:model-value="update('businessScope', $event)"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
      </el-col>

      <el-col :span="24"><el-divider content-position="left">法定代表人</el-divider></el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="法定代表人">
          <el-input
            :model-value="model.legalRepresentativeName"
            @update:model-value="update('legalRepresentativeName', $event)"
          />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="证件类型">
          <el-select
            :model-value="model.legalDocumentTypeCode"
            @update:model-value="update('legalDocumentTypeCode', $event)"
            filterable
          >
            <el-option v-for="item in documentTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="证件号码">
          <el-input
            :model-value="model.legalDocumentNumber"
            @update:model-value="update('legalDocumentNumber', $event)"
          />
        </el-form-item>
      </el-col>

      <slot name="legal-extra" />
      <el-col :span="24"><el-divider content-position="left">经营与联系信息</el-divider></el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="联系人">
          <el-input :model-value="model.contactName" @update:model-value="update('contactName', $event)" />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="联系电话">
          <el-input :model-value="model.contactPhone" @update:model-value="update('contactPhone', $event)" />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="企业邮箱">
          <el-input :model-value="model.email" @update:model-value="update('email', $event)" />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="注册资本">
          <el-input-number
            :model-value="model.registeredCapital"
            @update:model-value="update('registeredCapital', $event)"
            :min="0"
          />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="行业编码">
          <el-input :model-value="model.industryCode" @update:model-value="update('industryCode', $event)" />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="企业网站">
          <el-input :model-value="model.website" @update:model-value="update('website', $event)" />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import type { EnterpriseIdentity } from '@namewta/domain-profile';
import { documentTypes } from '../identity-options';

const props = withDefaults(defineProps<{ model: EnterpriseIdentity; disabled?: boolean }>(), { disabled: false });
const emit = defineEmits<{ 'update:model': [value: EnterpriseIdentity] }>();
function update<K extends keyof EnterpriseIdentity>(key: K, value: EnterpriseIdentity[K]) {
  emit('update:model', { ...props.model, [key]: value });
}
</script>
