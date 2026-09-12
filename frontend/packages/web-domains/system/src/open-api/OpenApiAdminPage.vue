<template>
  <main class="app-container openapi-admin-page">
    <header class="page-heading">
      <div>
        <h2>OpenAPI 管理</h2>
        <p>选择目标用户后管理其调用凭据与实时接口目录。</p>
      </div>
    </header>
    <section class="target-toolbar" aria-labelledby="target-user-heading">
      <div>
        <h3 id="target-user-heading">目标用户</h3>
        <p>所有操作只作用于当前明确选择的用户。</p>
      </div>
      <el-select
        v-model="selectedUserId"
        filterable
        remote
        clearable
        :remote-method="searchUsers"
        :loading="loadingUsers"
        placeholder="搜索用户名或昵称"
        class="user-select"
      >
        <el-option
          v-for="user in users"
          :key="String(user.userId)"
          :value="String(user.userId)"
          :label="userLabel(user)"
        />
      </el-select>
    </section>
    <el-alert v-if="userError" :title="userError" type="error" show-icon :closable="false" />
    <el-empty v-if="!selectedUser" description="请选择要管理的目标用户" :image-size="88" />
    <open-api-workspace
      v-else
      :key="String(selectedUser.userId)"
      :runtime="runtime"
      :scope="{ kind: 'target-user', userId: selectedUser.userId, userLabel: userLabel(selectedUser) }"
    />
  </main>
</template>

<script setup lang="ts">
import type { OpenApiCredentialUserSummary } from '@namewta/domain-system';
import { computed, onMounted, ref } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import OpenApiWorkspace from './OpenApiWorkspace.vue';

const { runtime } = defineProps<{ runtime: SystemWebRuntime }>();
const users = ref<readonly OpenApiCredentialUserSummary[]>([]);
const selectedUserId = ref<string>();
const loadingUsers = ref(false);
const userError = ref('');
const selectedUser = computed(() => users.value.find(user => String(user.userId) === selectedUserId.value));

async function searchUsers(keyword = ''): Promise<void> {
  if (!runtime.hasPermission('system:openApi:list')) {
    userError.value = '当前账号没有目标用户查询权限';
    return;
  }
  loadingUsers.value = true;
  userError.value = '';
  try {
    users.value = (await runtime.openApi.targetUser.listUsers({ keyword, limit: 50 })).data;
  } catch {
    users.value = [];
    userError.value = '目标用户列表暂不可用';
  } finally {
    loadingUsers.value = false;
  }
}

function userLabel(user: OpenApiCredentialUserSummary): string {
  return `${user.userName} / ${user.nickName || user.userId}`;
}

onMounted(() => void searchUsers());
</script>

<style scoped>
.openapi-admin-page {
  display: grid;
  gap: 20px;
}
.page-heading h2,
.target-toolbar h3 {
  margin: 0;
}
.page-heading p,
.target-toolbar p {
  color: var(--el-text-color-secondary);
  margin: 6px 0 0;
}
.target-toolbar {
  align-items: end;
  border-bottom: 1px solid var(--el-border-color-lighter);
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 420px);
  padding-bottom: 18px;
}
.user-select {
  width: 100%;
}
@media (max-width: 720px) {
  .target-toolbar {
    align-items: stretch;
    grid-template-columns: 1fr;
  }
}
</style>
