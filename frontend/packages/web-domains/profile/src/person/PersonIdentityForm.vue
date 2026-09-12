<template>
  <el-form :model="model" label-width="96px">
    <el-row :gutter="12">
      <el-col :xs="24" :sm="12">
        <el-form-item label="姓名"><el-input v-model="model.fullName" /></el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="证件类型">
          <el-select v-model="model.documentTypeCode" filterable>
            <el-option v-for="item in documentTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="证件号码"><el-input v-model="model.documentNumber" /></el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="性别">
          <el-select v-model="model.gender">
            <el-option label="男" value="0" />
            <el-option label="女" value="1" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="出生日期">
          <el-date-picker v-model="model.birthDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="有效期起">
          <el-date-picker v-model="model.validFrom" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="有效期止">
          <el-date-picker v-model="model.validUntil" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import type { PersonIdentity } from '@namewta/domain-profile';

defineProps<{ model: PersonIdentity }>();
const documentTypes = [
  ['居民身份证', 'CN_RESIDENT_ID'],
  ['香港居民身份证', 'HK_RESIDENT_ID'],
  ['澳门居民身份证', 'MO_RESIDENT_ID'],
  ['台湾居民身份证', 'TW_RESIDENT_ID'],
  ['港澳居民居住证', 'HK_MACAO_RESIDENCE_PERMIT'],
  ['台湾居民居住证', 'TW_RESIDENCE_PERMIT'],
  ['港澳居民来往内地通行证', 'MAINLAND_TRAVEL_PERMIT_HK_MACAO'],
  ['台湾居民来往大陆通行证', 'MAINLAND_TRAVEL_PERMIT_TW'],
  ['中国护照', 'CN_PASSPORT'],
  ['香港护照', 'HK_PASSPORT'],
  ['澳门护照', 'MO_PASSPORT'],
  ['台湾旅行证件', 'TW_TRAVEL_DOCUMENT']
].map(([label, value]) => ({ label, value }));
</script>
