<template>
  <el-form :model="model" :disabled="disabled" label-position="top">
    <el-row :gutter="12">
      <el-col :xs="24" :sm="12">
        <el-form-item label="姓名">
          <el-input :model-value="model.fullName" @update:model-value="update('fullName', $event)" />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="证件类型">
          <el-select
            :model-value="model.documentTypeCode"
            @update:model-value="update('documentTypeCode', $event)"
            filterable
          >
            <el-option v-for="item in documentTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="证件号码">
          <el-input :model-value="model.documentNumber" @update:model-value="update('documentNumber', $event)" />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="性别">
          <el-select :model-value="model.gender" @update:model-value="update('gender', $event)">
            <el-option v-for="item in genderOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="出生日期">
          <el-date-picker
            :model-value="model.birthDate"
            @update:model-value="update('birthDate', $event)"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="有效期起">
          <el-date-picker
            :model-value="model.validFrom"
            @update:model-value="update('validFrom', $event)"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="有效期止">
          <el-date-picker
            :model-value="model.validUntil"
            @update:model-value="update('validUntil', $event)"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import type { PersonIdentity } from '@namewta/domain-profile';
import { documentTypes, genderOptions } from '../identity-options';

const props = withDefaults(defineProps<{ model: PersonIdentity; disabled?: boolean }>(), { disabled: false });
const emit = defineEmits<{ 'update:model': [value: PersonIdentity] }>();
function update<K extends keyof PersonIdentity>(key: K, value: PersonIdentity[K]) {
  emit('update:model', { ...props.model, [key]: value });
}
</script>
