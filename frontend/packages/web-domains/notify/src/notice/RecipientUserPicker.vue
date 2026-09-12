<template>
  <div class="recipient-picker">
    <div class="search-row">
      <el-input
        v-model="keyword"
        :validate-event="false"
        clearable
        placeholder="输入账号、昵称或手机号，支持模糊搜索"
        aria-label="搜索发送对象"
        @keyup.enter="search()"
      />
      <el-button :loading="loading" type="primary" :disabled="!keyword.trim()" @click="search()">搜索</el-button>
    </div>
    <el-table
      v-loading="loading"
      :data="rows"
      border
      max-height="240"
      :empty-text="searched ? '没有匹配的正常用户' : '输入关键词后查询用户'"
    >
      <el-table-column width="54" align="center">
        <template #header>
          <el-checkbox
            :model-value="allChecked"
            :indeterminate="someChecked"
            :disabled="!rows.length || loading"
            aria-label="全选当前结果"
            @change="value => togglePage(value === true)"
          />
        </template>
        <template #default="{ row }">
          <el-checkbox
            :model-value="selectedIds.has(String(row.userId))"
            :aria-label="`选择用户 ${row.userName || row.userId}`"
            @change="value => toggle(row as NotifyUserCandidate, value === true)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="userName" label="账号" min-width="130" />
      <el-table-column prop="nickName" label="昵称" min-width="130" />
      <el-table-column prop="phoneNumber" label="手机号" min-width="140" />
    </el-table>
    <el-pagination
      v-if="total > pageSize"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      :disabled="loading"
      @current-change="search"
    />
    <div class="selection-summary">
      <span>已选择 {{ selected.length }} 人</span>
      <el-button v-if="selected.length" link type="danger" @click="ids = []">清空已选</el-button>
    </div>
    <div v-if="selected.length" class="selected-users" aria-label="已选用户">
      <el-tag v-for="user in selected" :key="String(user.userId)" closable @close="toggle(user, false)">
        {{ user.userName || `用户 ${user.userId}` }}{{ user.nickName ? `（${user.nickName}）` : '' }}
      </el-tag>
    </div>
    <p class="selection-hint">表头复选框只选择当前页；切换关键词或翻页会保留已选用户。</p>
  </div>
</template>

<script setup lang="ts">
import type { NotifyUserCandidate } from '@namewta/domain-notify';
import { ElMessage } from 'element-plus';
import type { NotifyUserDirectory } from '../runtime';
import { useRecipientSelection } from './useRecipientSelection';

const { directory } = defineProps<{ directory: NotifyUserDirectory }>();
const ids = defineModel<Array<string | number>>({ required: true });
const {
  keyword,
  page,
  pageSize,
  rows,
  total,
  loading,
  searched,
  selected,
  selectedIds,
  allChecked,
  someChecked,
  search,
  toggle,
  togglePage
} = useRecipientSelection(directory, ids, message => ElMessage.error(message));
</script>

<style scoped>
.recipient-picker {
  width: 100%;
}
.search-row,
.selection-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.selection-summary {
  margin: 12px 0 8px;
}
.selected-users {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 100px;
  overflow: auto;
}
.selection-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-bottom: 0;
}
.el-pagination {
  margin-top: 12px;
}
</style>
