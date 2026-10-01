<template>
  <section class="app-container social-bindings" v-loading="busy">
    <h2>账号绑定</h2>
    <p>绑定后，可使用第三方账号登录当前应用。</p>
    <el-alert v-if="message" :title="message" type="error" :closable="false" />
    <el-table :data="bindings" empty-text="尚未绑定第三方账号">
      <el-table-column label="登录平台" prop="name" />
      <el-table-column label="第三方账号" prop="userName" />
      <el-table-column label="绑定时间" prop="createTime" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="danger" :disabled="busy" @click="unbind(row.id)">解绑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <h3>可绑定的第三方账号</h3>
    <p v-if="!providers.length && !busy">当前应用暂未配置第三方登录。</p>
    <div class="social-bindings__providers">
      <el-button
        v-for="provider in providers"
        :key="provider.providerKey"
        :disabled="busy"
        @click="bind(provider.providerKey)"
      >
        <SvgIcon :icon-class="provider.icon || 'tabler:key'" />
        {{ provider.name }}
      </el-button>
    </div>
  </section>
</template>
<script setup lang="ts">
import type { SocialBinding, SocialProvider } from '@namewta/domain-admin';
import SvgIcon from '@namewta/web-kit-ui-element/icon';
import { onMounted, onBeforeUnmount, ref } from 'vue';
import type { SocialWebRuntime } from '../socialRuntime';
const { runtime, confirm } = defineProps<{ runtime: SocialWebRuntime; confirm(message: string): Promise<void> }>();
const bindings = ref<SocialBinding[]>([]);
const providers = ref<readonly SocialProvider[]>([]);
const busy = ref(false);
const message = ref('');
let active = true;
async function load() {
  busy.value = true;
  message.value = '';
  try {
    const [rows, context] = await Promise.all([
      runtime.service.external.bindings(),
      runtime.service.getClientContext()
    ]);
    if (active) {
      bindings.value = rows;
      providers.value = context.providers ?? [];
    }
  } catch {
    if (active) message.value = '账号绑定信息暂不可用，请刷新重试';
  } finally {
    if (active) busy.value = false;
  }
}
async function bind(key: string) {
  busy.value = true;
  try {
    await runtime.start(key, 'BIND');
  } catch {
    if (active) message.value = '授权入口暂不可用，请重试';
  } finally {
    if (active) busy.value = false;
  }
}
async function unbind(id: string) {
  if (busy.value) return;
  try {
    await confirm('确认解除此第三方账号绑定？');
  } catch {
    return;
  }
  if (!active) return;
  busy.value = true;
  try {
    await runtime.service.external.unbind(id);
    if (active) await load();
  } catch {
    if (active) message.value = '解绑失败，请稍后重试';
  } finally {
    if (active) busy.value = false;
  }
}
onMounted(load);
onBeforeUnmount(() => {
  active = false;
  runtime.dispose();
});
</script>
<style scoped>
.social-bindings__providers {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.social-bindings__providers .el-button {
  margin: 0;
  gap: 8px;
}
</style>
