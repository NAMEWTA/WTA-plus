<template>
  <el-dialog v-model="visible" :title="title" width="50%" :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="消息提醒" prop="messageType">
        <el-checkbox-group v-model="form.messageType">
          <el-checkbox value="1" disabled>站内信</el-checkbox>
          <el-checkbox value="2">邮件</el-checkbox>
          <el-checkbox value="3">短信</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="消息内容" prop="message">
        <el-input v-model="form.message" type="textarea" resize="none" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="submit">确认</el-button>
      <el-button @click="cancel">取消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { FormInstance } from 'element-plus';
import { reactive, ref } from 'vue';

withDefaults(defineProps<{ title?: string }>(), { title: '提示' });
const emit = defineEmits<{ cancelCallback: []; submitCallback: [form: { message: string; messageType: string[] }] }>();
const visible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ message: '', messageType: ['1'] });
const rules = {
  messageType: [{ required: true, message: '请选择消息提醒', trigger: 'change' }],
  message: [{ required: true, message: '请输入消息内容', trigger: 'blur' }]
};
function reset() {
  form.message = '';
  form.messageType = ['1'];
}
function open() {
  reset();
  visible.value = true;
}
function close() {
  reset();
  visible.value = false;
}
async function submit() {
  if (await formRef.value?.validate())
    emit('submitCallback', { message: form.message, messageType: [...form.messageType] });
}
function cancel() {
  visible.value = false;
  emit('cancelCallback');
}
defineExpose({ open, close });
</script>
