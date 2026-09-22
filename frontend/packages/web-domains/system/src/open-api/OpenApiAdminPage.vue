<template>
  <div class="p-2 app-container openapi-admin-page">
    <el-card shadow="hover" class="table-panel">
      <template #header>
        <div class="toolbar-shell">
          <div class="table-heading">
            <h3>OpenAPI 管理</h3>
            <p>选择目标用户后管理其调用凭据与实时接口目录。</p>
          </div>
          <div class="toolbar-actions">
            <el-select
              v-model="selectedUserId"
              filterable
              remote
              clearable
              :remote-method="searchUsers"
              :loading="loadingUsers"
              placeholder="搜索用户名或昵称"
              aria-label="目标用户"
              class="user-select"
            >
              <el-option
                v-for="user in users"
                :key="String(user.userId)"
                :value="String(user.userId)"
                :label="userLabel(user)"
              />
            </el-select>
          </div>
        </div>
      </template>
      <el-alert v-if="userError" :title="userError" type="error" show-icon :closable="false" />
      <el-empty v-if="!selectedUser" description="请选择要管理的目标用户" />
      <open-api-workspace
        v-else
        :key="String(selectedUser.userId)"
        :runtime="runtime"
        :scope="{ kind: 'target-user', userId: selectedUser.userId, userLabel: userLabel(selectedUser) }"
      />
    </el-card>
  </div>
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
.user-select {
  width: 280px;
}
</style>
