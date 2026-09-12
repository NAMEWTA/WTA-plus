<template>
  <div class="p-2 app-container auth-role-page">
    <el-card shadow="hover" class="search-panel auth-role-info">
      <template #header>
        <div class="panel-heading">
          <div><h3>基本信息</h3></div>
        </div>
      </template>
      <el-form :model="form" :inline="true" class="query-form auth-role-form">
        <el-form-item label="用户昵称" prop="nickName">
          <el-input v-model="form.nickName" disabled />
        </el-form-item>
        <el-form-item label="登录账号" prop="userName">
          <el-input v-model="form.userName" disabled />
        </el-form-item>
        <el-form-item label="客户端" prop="clientId">
          <el-select
            v-model="clientId"
            placeholder="请选择客户端后再分配角色"
            filterable
            style="width: 260px"
            @change="handleClientChange"
          >
            <el-option
              v-for="item in clientOptions"
              :key="item.id"
              :label="item.clientKey ? `${item.clientKey}（${item.clientId}）` : item.clientId"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="table-panel">
      <template #header>
        <div class="toolbar-shell">
          <div class="table-heading">
            <h3>角色信息</h3>
          </div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="submitForm()">提交</el-button>
            <el-button @click="close()">返回</el-button>
          </div>
        </div>
      </template>
      <el-table
        ref="tableRef"
        v-loading="loading"
        border
        class="data-table"
        :row-key="getRowKey"
        :data="roles.slice((pageNum - 1) * pageSize, pageNum * pageSize)"
        @row-click="clickRow"
        @selection-change="handleSelectionChange"
      >
        <el-table-column label="序号" width="55" type="index" align="center">
          <template #default="scope">
            <span>{{ (pageNum - 1) * pageSize + scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column
          type="selection"
          :reserve-selection="true"
          :selectable="checkSelectable"
          width="55"
        ></el-table-column>
        <el-table-column label="角色编号" align="center" prop="roleId" />
        <el-table-column label="角色名称" align="center" prop="roleName">
          <template #default="scope">
            <span>{{ scope.row.roleName }}</span>
            <el-tag v-if="scope.row.clientDefault" class="ml-1" size="small" type="info">默认角色</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限字符" align="center" prop="roleKey" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="pageNum" v-model:limit="pageSize" :total="total" />
    </el-card>
  </div>
</template>

<script setup name="AuthRole" lang="ts">
import type { ClientVO, RoleVO, UserForm } from '@namewta/domain-system';
import type { TableInstance as ElTableInstance } from 'element-plus';
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import type { SystemWebRuntime } from '../runtime';
import { parseTime } from '../utils';

const { runtime } = defineProps<{ runtime: SystemWebRuntime }>();
const route = useRoute();
const listClientOptions = runtime.service.clients.options;
const { authRoles: getAuthRole, get: getUser, updateAuthRoles: updateAuthRole } = runtime.service.users;
const modal = { msgWarning: runtime.warning, msgSuccess: runtime.success };

const loading = ref(true);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const clientId = ref<string | number>();
const clientOptions = ref<ClientVO[]>([]);
const assignedRoleIds = ref<Set<string>>(new Set());
const roleIds = ref<Array<string | number>>([]);
const roles = ref<RoleVO[]>([]);
const form = ref<Partial<UserForm>>({
  nickName: undefined,
  userName: '',
  userId: undefined
});

const tableRef = ref<ElTableInstance>();
const syncingSelection = ref(false);
let clientRequestId = 0;

const currentClientRoleIdSet = computed(() => new Set(roles.value.map(role => String(role.roleId))));

/** 单击选中行数据 */
const clickRow = (row: RoleVO) => {
  if (checkSelectable(row)) {
    row.flag = !row.flag;
    tableRef.value?.toggleRowSelection(row, row.flag);
  }
};
/** 多选框选中数据 */
const handleSelectionChange = (selection: RoleVO[]) => {
  if (syncingSelection.value) {
    return;
  }
  const selectedIds = new Set(selection.map(item => String(item.roleId)));
  currentClientRoleIdSet.value.forEach(id => assignedRoleIds.value.delete(id));
  selectedIds.forEach(id => assignedRoleIds.value.add(id));
  roleIds.value = [...assignedRoleIds.value];
};
/** 保存选中的数据编号 */
const getRowKey = (row: RoleVO): string => {
  return String(row.roleId);
};
/** 检查角色状态 */
const checkSelectable = (row: RoleVO): boolean => {
  return row.status === '0' && !row.clientDefault;
};
/** 关闭按钮 */
const close = () => {
  runtime.closeAndOpenPage({ path: '/system/user' });
};
/** 提交按钮 */
const submitForm = async () => {
  if (!clientId.value) {
    modal.msgWarning('请先选择客户端');
    return;
  }
  const userId = form.value.userId;
  const rIds = roles.value
    .filter(row => assignedRoleIds.value.has(String(row.roleId)) && !row.clientDefault)
    .map(row => row.roleId)
    .join(',');
  await updateAuthRole({ userId: userId as string, roleIds: rIds, clientId: clientId.value });
  modal.msgSuccess('授权成功');
  close();
};

const applyRowSelection = async () => {
  syncingSelection.value = true;
  await nextTick();
  tableRef.value?.clearSelection();
  roles.value.forEach(row => {
    row.flag = assignedRoleIds.value.has(String(row.roleId));
    if (row.flag) {
      tableRef.value?.toggleRowSelection(row, true);
    }
  });
  await nextTick();
  syncingSelection.value = false;
};

const handleClientChange = async () => {
  const requestId = ++clientRequestId;
  pageNum.value = 1;
  if (!clientId.value) {
    roles.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  roles.value = [];
  total.value = 0;
  const userId = form.value.userId;
  if (!userId) {
    loading.value = false;
    return;
  }
  try {
    const res = await getAuthRole(userId, clientId.value);
    if (requestId !== clientRequestId) {
      return;
    }
    roles.value = res.data?.roles ?? [];
    const flaggedRoles = roles.value.filter(row => row?.flag && !row.clientDefault);
    flaggedRoles.forEach(row => assignedRoleIds.value.add(String(row.roleId)));
    roleIds.value = [...assignedRoleIds.value];
    total.value = roles.value.length;
    await applyRowSelection();
  } finally {
    if (requestId === clientRequestId) {
      loading.value = false;
    }
  }
};

const getList = async () => {
  const userId = route.params && route.params.userId;
  if (userId) {
    loading.value = true;
    try {
      const clients = await listClientOptions();
      const res = await getUser(userId as string);
      Object.assign(form.value, res.data.user);
      const userTypeIds = new Set((res.data.user?.userTypeIds ?? []).map(String));
      clientOptions.value = clients.filter(
        client => client.status === '0' && client.userTypeId != null && userTypeIds.has(String(client.userTypeId))
      );
    } finally {
      loading.value = false;
    }
  }
};
onMounted(() => {
  getList();
});
</script>

<style lang="scss" scoped>
.auth-role-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.auth-role-form :deep(.el-input) {
  width: 220px;
}

.auth-role-info :deep(.el-card__body) {
  padding-top: 14px !important;
}
</style>
