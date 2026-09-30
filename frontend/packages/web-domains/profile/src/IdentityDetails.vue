<template>
  <el-descriptions :column="2" border class="identity-details">
    <el-descriptions-item v-for="field in fields" :key="field.key" :label="field.label" :span="field.wide ? 2 : 1">
      {{ value(field.key) }}
    </el-descriptions-item>
  </el-descriptions>
</template>
<script setup lang="ts">
import type { ProfileType } from '@namewta/domain-profile';
import { computed } from 'vue';
import { documentTypes, genderOptions } from './identity-options';
const props = defineProps<{ profileType: ProfileType; identity: object }>();
type Field = { key: string; label: string; wide?: boolean };
const fields = computed<Field[]>(() =>
  (props.profileType === 'PERSON'
    ? [
        ['fullName', '姓名'],
        ['documentTypeCode', '证件类型'],
        ['documentNumber', '证件号码'],
        ['gender', '性别'],
        ['birthDate', '出生日期'],
        ['validFrom', '有效期起'],
        ['validUntil', '有效期止']
      ]
    : [
        ['enterpriseName', '企业名称'],
        ['unifiedCreditCode', '统一信用代码'],
        ['enterpriseType', '企业类型'],
        ['establishedDate', '成立日期'],
        ['businessTermFrom', '营业期限起'],
        ['businessTermUntil', '营业期限止'],
        ['registeredAddress', '注册地址'],
        ['businessScope', '经营范围'],
        ['legalRepresentativeName', '法定代表人'],
        ['legalDocumentTypeCode', '证件类型'],
        ['legalDocumentNumber', '证件号码'],
        ['contactName', '联系人'],
        ['contactPhone', '联系电话'],
        ['email', '企业邮箱'],
        ['registeredCapital', '注册资本'],
        ['industryCode', '行业编码'],
        ['website', '企业网站']
      ]
  ).map(([key, label]) => ({ key, label, wide: key === 'registeredAddress' || key === 'businessScope' }))
);
function value(key: string): string {
  const raw: unknown = Reflect.get(props.identity, key);
  if (raw === null || raw === undefined || raw === '') return '—';
  if (key === 'documentTypeCode' || key === 'legalDocumentTypeCode')
    return documentTypes.find(item => item.value === raw)?.label ?? String(raw);
  if (key === 'gender')
    return (
      genderOptions.find(item => item.value === raw)?.label ?? (raw === '0' ? '男' : raw === '1' ? '女' : String(raw))
    );
  return typeof raw === 'string' || typeof raw === 'number' ? String(raw) : '—';
}
</script>
<style scoped>
.identity-details {
  overflow-wrap: anywhere;
}
@media (max-width: 640px) {
  .identity-details :deep(.el-descriptions__cell) {
    padding: 8px;
  }
}
</style>
