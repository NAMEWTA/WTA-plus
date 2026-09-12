<template>
  <el-form :model="model" label-width="112px">
    <el-row :gutter="12">
      <el-col :span="24"><el-divider content-position="left">企业主体</el-divider></el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="企业名称"><el-input v-model="model.enterpriseName" /></el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="统一信用代码"><el-input v-model="model.unifiedCreditCode" /></el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="企业类型"><el-input v-model="model.enterpriseType" /></el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="成立日期">
          <el-date-picker v-model="model.establishedDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="营业期限起">
          <el-date-picker v-model="model.businessTermFrom" type="date" value-format="YYYY-MM-DD" clearable />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="营业期限止">
          <el-date-picker v-model="model.businessTermUntil" type="date" value-format="YYYY-MM-DD" clearable />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="注册地址"><el-input v-model="model.registeredAddress" /></el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="经营范围">
          <el-input v-model="model.businessScope" type="textarea" :rows="3" />
        </el-form-item>
      </el-col>

      <el-col :span="24"><el-divider content-position="left">法定代表人</el-divider></el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="法定代表人"><el-input v-model="model.legalRepresentativeName" /></el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="证件类型">
          <el-select v-model="model.legalDocumentTypeCode" filterable>
            <el-option v-for="item in documentTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="证件号码"><el-input v-model="model.legalDocumentNumber" /></el-form-item>
      </el-col>

      <el-col :span="24"><el-divider content-position="left">经营与联系信息</el-divider></el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="联系人"><el-input v-model="model.contactName" /></el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="联系电话"><el-input v-model="model.contactPhone" /></el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="企业邮箱"><el-input v-model="model.email" /></el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="注册资本"><el-input-number v-model="model.registeredCapital" :min="0" /></el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="行业编码"><el-input v-model="model.industryCode" /></el-form-item>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-form-item label="企业网站"><el-input v-model="model.website" /></el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import type { EnterpriseIdentity } from '@namewta/domain-profile';

defineProps<{ model: EnterpriseIdentity }>();
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
