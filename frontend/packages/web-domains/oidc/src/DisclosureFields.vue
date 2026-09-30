<template>
  <div class="disclosure-fields">
    <p class="field-hint">用户标识始终提供。其他资料只有在此处勾选且应用请求相应范围时才会提供。</p>
    <fieldset v-for="group in groups" :key="group.name">
      <legend>{{ group.label }}</legend>
      <div class="field-actions">
        <el-button link type="primary" @click="selectGroup(group.fields, true)">选择常用字段</el-button>
        <el-button link @click="selectGroup(group.fields, false)">清空本组</el-button>
      </div>
      <el-checkbox-group :model-value="modelValue" @update:model-value="update">
        <el-checkbox
          v-for="field in group.fields"
          :key="field.key"
          :value="field.key"
          :class="{ 'sensitive-field': field.sensitive }"
        >
          {{ field.label }}
          <span v-if="field.sensitive" class="explicit-label">（单独授权）</span>
        </el-checkbox>
      </el-checkbox-group>
    </fieldset>
  </div>
</template>
<script setup lang="ts">
import { selectOrdinaryFields, type OidcField } from '@namewta/domain-oidc';
import { ElButton, ElCheckbox, ElCheckboxGroup } from 'element-plus';
import { computed } from 'vue';

const props = defineProps<{ modelValue: string[]; fields: OidcField[] }>();
const emit = defineEmits<{ 'update:modelValue': [value: string[]] }>();
const groupLabels: Readonly<Record<string, string>> = {
  account: '账户资料',
  person: '个人认证资料',
  enterprise: '企业认证资料'
};
const groups = computed(() => {
  const groups = new Map<string, OidcField[]>();
  for (const field of props.fields) groups.set(field.group, [...(groups.get(field.group) ?? []), field]);
  return [...groups].map(([name, fields]) => ({ name, label: groupLabels[name] ?? '其他资料', fields }));
});
function selectGroup(fields: OidcField[], checked: boolean) {
  emit('update:modelValue', selectOrdinaryFields(props.modelValue, fields, checked));
}
function update(values: (string | number | boolean)[]) {
  emit(
    'update:modelValue',
    values.filter((value): value is string => typeof value === 'string')
  );
}
</script>
<style scoped>
fieldset {
  border: 1px solid var(--app-border-color, var(--el-border-color));
  border-radius: var(--el-border-radius-base);
  padding: 12px;
  margin: 12px 0;
  min-width: 0;
}
legend {
  font-weight: 600;
  padding: 0 6px;
}
.field-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}
.field-hint {
  color: var(--el-text-color-secondary);
  line-height: 1.6;
  margin: 0;
}
.el-checkbox-group {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
  gap: 4px 12px;
}
.el-checkbox {
  margin-right: 0;
  height: auto;
  min-height: 32px;
  white-space: normal;
}
.el-checkbox :deep(.el-checkbox__label) {
  white-space: normal;
  line-height: 1.5;
}
.explicit-label {
  font-size: 12px;
}
</style>
