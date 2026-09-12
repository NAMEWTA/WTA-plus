<template>
  <div class="p-2">
    <el-card>
      <template #header>
        <div class="flex justify-between">
          <span>通知管理</span>
          <el-button v-if="runtime.hasPermission('notify:notice:add')" type="primary" @click="openAdd">新增</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent="searchNotices">
        <el-form-item label="标题"><el-input v-model="query.noticeTitle" clearable /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.noticeType" clearable style="width: 160px">
            <el-option v-for="item in noticeTypeDict" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="searchNotices">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="noticeTitle" label="标题" min-width="200" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ dictLabel(noticeTypeDict, row.noticeType) }}</template>
        </el-table-column>
        <el-table-column label="发送渠道" min-width="160">
          <template #default="{ row }">{{ channelLabel(row.channels) }}</template>
        </el-table-column>
        <el-table-column label="发送对象" min-width="170">
          <template #default="{ row }">{{ targetLabel(row as NotifyNotice) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="lifecycleMeta[row.lifecycle]?.type ?? 'info'">
              {{ dictLabel(lifecycleDict, row.lifecycle) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="editable(row as NotifyNotice) && runtime.hasPermission('notify:notice:edit')"
              link
              type="primary"
              :disabled="busyRows.has(row.noticeId)"
              @click="edit(row as NotifyNotice)"
            >
              编辑
            </el-button>
            <el-button
              v-if="editable(row as NotifyNotice) && runtime.hasPermission('notify:notice:publish')"
              link
              type="success"
              :disabled="busyRows.has(row.noticeId)"
              @click="runAction(row as NotifyNotice, 'publish')"
            >
              发布
            </el-button>
            <el-button
              v-if="row.lifecycle === 'PUBLISHED' && runtime.hasPermission('notify:notice:retract')"
              link
              type="warning"
              :disabled="busyRows.has(row.noticeId)"
              @click="runAction(row as NotifyNotice, 'retract')"
            >
              撤回
            </el-button>
            <el-button
              v-if="editable(row as NotifyNotice) && runtime.hasPermission('notify:notice:remove')"
              link
              type="danger"
              :disabled="busyRows.has(row.noticeId)"
              @click="runAction(row as NotifyNotice, 'remove')"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="load"
      />
    </el-card>
    <el-dialog
      v-model="dialogVisible"
      :title="form.noticeId ? '编辑通知' : '新增通知'"
      width="860px"
      style="max-width: 95vw"
      destroy-on-close
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      :show-close="!saving"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        :disabled="saving"
        @submit.prevent="save"
      >
        <el-form-item label="标题" prop="noticeTitle">
          <el-input v-model="form.noticeTitle" maxlength="50" show-word-limit placeholder="请输入通知标题" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="通知类型" prop="noticeType">
            <el-select v-model="form.noticeType">
              <el-option v-for="item in noticeTypeDict" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="发送渠道" prop="channels">
            <el-checkbox-group v-model="form.channels">
              <el-checkbox v-for="item in channelOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </div>
        <el-divider content-position="left">发送对象</el-divider>
        <el-form-item prop="recipientType">
          <el-radio-group v-model="form.recipientType">
            <el-radio-button value="ALL">全部用户</el-radio-button>
            <el-radio-button value="USER">指定用户</el-radio-button>
            <el-radio-button value="USER_TYPE">用户类型</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-alert
          v-if="form.recipientType === 'ALL'"
          title="发布时发送给全部正常且未删除的系统用户。"
          type="info"
          :closable="false"
          show-icon
          class="scope-alert"
        />
        <el-form-item v-if="form.recipientType === 'USER'" prop="recipientIds">
          <RecipientUserPicker v-model="form.recipientIds" :directory="runtime.directory" />
        </el-form-item>
        <el-form-item v-if="form.recipientType === 'USER_TYPE'" label="选择用户类型" prop="userTypeIds">
          <el-select
            v-model="form.userTypeIds"
            multiple
            filterable
            :loading="typesLoading"
            placeholder="请选择一个或多个启用的用户类型"
          >
            <el-option
              v-for="item in userTypeOptions"
              :key="String(item.userTypeId)"
              :label="item.userTypeName"
              :value="String(item.userTypeId)"
            />
          </el-select>
          <p class="form-hint">匹配任意一个所选类型的正常用户都会收到通知，重复用户只发送一次。</p>
          <el-button v-if="typesFailed" link type="primary" @click="loadUserTypes">重新加载用户类型</el-button>
        </el-form-item>
        <el-divider content-position="left">通知内容</el-divider>
        <el-form-item prop="noticeContent">
          <el-input v-model="form.noticeContent" type="textarea" :rows="6" placeholder="请输入通知内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <span class="form-hint">保存草稿后，请在列表中点击“发布”发送。</span>
          <div>
            <el-button :disabled="saving" @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="save">保存草稿</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type {
  NotificationChannel,
  NotifyNotice,
  NotifyNoticeQuery,
  NotifyUserTypeOption
} from '@namewta/domain-notify';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import type { NotifyWebRuntime, NotifyDictOption } from './runtime';
import RecipientUserPicker from './notice/RecipientUserPicker.vue';

const { runtime } = defineProps<{ runtime: NotifyWebRuntime }>();
const dicts = runtime.dicts('notify_notice_lifecycle', 'sys_notice_type', 'notify_channel');
const noticeTypeDict = computed(() => dicts.sys_notice_type?.value ?? []);
const lifecycleDict = computed(() => dicts.notify_notice_lifecycle?.value ?? []);
const channelDict = computed(() => dicts.notify_channel?.value ?? []);
const supportedChannels: NotificationChannel[] = ['IN_APP', 'SMS', 'MAIL'];
const channelOptions = computed(() =>
  channelDict.value.filter(item => supportedChannels.includes(item.value as NotificationChannel))
);
const query = reactive<NotifyNoticeQuery>({ pageNum: 1, pageSize: 10 });
const rows = ref<NotifyNotice[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const editing = ref(false);
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const busyRows = reactive(new Set<string>());
const userTypeOptions = ref<NotifyUserTypeOption[]>([]);
const typesLoading = ref(false);
const typesFailed = ref(false);
let listGeneration = 0;
let typeGeneration = 0;
let alive = true;

type NoticeDraft = Omit<NotifyNotice, 'noticeId'> & { noticeId?: string };
const newDraft = (): NoticeDraft => ({
  noticeTitle: '',
  noticeType: '1',
  noticeContent: '',
  status: '1',
  recipientType: 'ALL',
  recipientIds: [],
  userTypeIds: [],
  channels: ['IN_APP']
});
const form = ref<NoticeDraft>(newDraft());
const lifecycleMeta: Record<string, { type: 'success' | 'warning' | 'info' }> = {
  DRAFT: { type: 'info' },
  PUBLISHED: { type: 'success' },
  RETRACTED: { type: 'warning' }
};
const rules: FormRules<NoticeDraft> = {
  noticeTitle: [{ required: true, whitespace: true, message: '请输入通知标题', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择通知类型', trigger: 'change' }],
  noticeContent: [{ required: true, whitespace: true, message: '请输入通知内容', trigger: 'blur' }],
  channels: [{ type: 'array', required: true, min: 1, message: '至少选择一个发送渠道', trigger: 'change' }],
  recipientIds: [
    {
      validator: (_rule, value: unknown[], done) =>
        done(form.value.recipientType === 'USER' && !value.length ? new Error('至少选择一名用户') : undefined),
      trigger: 'change'
    }
  ],
  userTypeIds: [
    {
      validator: (_rule, value: unknown[], done) =>
        done(form.value.recipientType === 'USER_TYPE' && !value.length ? new Error('至少选择一个用户类型') : undefined),
      trigger: 'change'
    }
  ]
};

function dictLabel(options: NotifyDictOption[], value?: string) {
  return options.find(item => item.value === value)?.label ?? '未知';
}
const channelFallback: Record<string, string> = {
  IN_APP: '\u7ad9\u5185\u4fe1',
  SMS: '\u77ed\u4fe1',
  MAIL: '\u90ae\u4ef6'
};
function channelLabel(channels: NotificationChannel[] = ['IN_APP']) {
  return channels
    .map(value => channelDict.value.find(item => item.value === value)?.label ?? channelFallback[value] ?? '\u5176\u4ed6')
    .join('\u3001');
}
function targetLabel(row: Pick<NotifyNotice, 'recipientType' | 'recipientIds' | 'userTypeIds'>) {
  if (!row.recipientType || row.recipientType === 'ALL') return '全部正常用户';
  if (row.recipientType === 'USER') return `${row.recipientIds.length} 名指定用户`;
  return `${row.userTypeIds.length} 个用户类型（匹配任一）`;
}
function editable(row: NotifyNotice) {
  return row.lifecycle === 'DRAFT' || row.lifecycle === 'RETRACTED';
}
function showError(error: unknown, fallback: string) {
  ElMessage.error(error instanceof Error ? error.message : fallback);
}

async function load() {
  const request = ++listGeneration;
  loading.value = true;
  try {
    const result = await runtime.service.notices.list({ ...query });
    if (!alive || request !== listGeneration) return;
    rows.value = result.data.rows;
    total.value = result.data.total;
  } catch (error) {
    if (alive && request === listGeneration) showError(error, '通知加载失败');
  } finally {
    if (request === listGeneration) loading.value = false;
  }
}
function searchNotices() {
  query.pageNum = 1;
  void load();
}
async function loadUserTypes() {
  const request = ++typeGeneration;
  typesLoading.value = true;
  typesFailed.value = false;
  try {
    const result = await runtime.directory.userTypes();
    if (alive && request === typeGeneration) userTypeOptions.value = result.data.filter(item => item.status === '0');
  } catch (error) {
    if (alive && request === typeGeneration) {
      typesFailed.value = true;
      showError(error, '用户类型加载失败');
    }
  } finally {
    if (request === typeGeneration) typesLoading.value = false;
  }
}
watch(
  [dialogVisible, () => form.value.recipientType],
  ([open, value]) => {
    formRef.value?.clearValidate(['recipientIds', 'userTypeIds']);
    if (open && value === 'USER_TYPE') void loadUserTypes();
  }
);
function openAdd() {
  if (editing.value || dialogVisible.value) return;
  form.value = newDraft();
  dialogVisible.value = true;
}
async function edit(row: NotifyNotice) {
  if (editing.value || busyRows.has(row.noticeId) || dialogVisible.value) return;
  editing.value = true;
  busyRows.add(row.noticeId);
  try {
    const result = await runtime.service.notices.get(row.noticeId);
    if (!alive) return;
    if (!editable(result.data)) {
      ElMessage.warning('此通知已经发布，不能编辑');
      await load();
      return;
    }
    form.value = {
      ...result.data,
      recipientType: result.data.recipientType ?? 'ALL',
      recipientIds: (result.data.recipientIds ?? []).map(String),
      userTypeIds: (result.data.userTypeIds ?? []).map(String),
      channels: [...(result.data.channels ?? ['IN_APP'])]
    };
    dialogVisible.value = true;
  } catch (error) {
    showError(error, '通知详情加载失败');
  } finally {
    editing.value = false;
    busyRows.delete(row.noticeId);
  }
}
async function save() {
  if (saving.value) return;
  saving.value = true;
  try {
    if (!(await formRef.value?.validate().catch(() => false))) return;
    const payload = {
      ...form.value,
      recipientIds: form.value.recipientType === 'USER' ? [...form.value.recipientIds] : [],
      userTypeIds: form.value.recipientType === 'USER_TYPE' ? [...form.value.userTypeIds] : []
    };
    await runtime.service.notices.save(payload);
    if (!alive) return;
    dialogVisible.value = false;
    ElMessage.success('草稿已保存，可在列表中发布');
    await load();
  } catch (error) {
    showError(error, '草稿保存失败');
  } finally {
    saving.value = false;
  }
}
async function runAction(row: NotifyNotice, action: 'publish' | 'retract' | 'remove') {
  if (busyRows.has(row.noticeId)) return;
  busyRows.add(row.noticeId);
  const label = { publish: '发布', retract: '撤回', remove: '删除' }[action];
  const message =
    action === 'publish'
      ? `确认发布“${row.noticeTitle}”？渠道：${channelLabel(row.channels)}；发送对象：${targetLabel(row)}。实际发送时将过滤停用、删除或不存在的用户。`
      : `确认${label}“${row.noticeTitle}”？`;
  try {
    await ElMessageBox.confirm(message, `${label}确认`, {
      type: 'warning',
      confirmButtonText: `确认${label}`,
      cancelButtonText: '取消'
    });
    if (action === 'remove') await runtime.service.notices.remove([row.noticeId]);
    else await runtime.service.notices[action](row.noticeId);
    ElMessage.success(`${label}成功`);
    await load();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') showError(error, `${label}失败`);
  } finally {
    busyRows.delete(row.noticeId);
  }
}
onMounted(load);
onBeforeUnmount(() => {
  alive = false;
  ++listGeneration;
  ++typeGeneration;
});
</script>
<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 24px;
}
.form-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin: 8px 0 0;
}
.scope-alert {
  margin-bottom: 20px;
}
.dialog-footer {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}
@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
  .dialog-footer {
    flex-direction: column;
    align-items: flex-end;
  }
}
</style>
