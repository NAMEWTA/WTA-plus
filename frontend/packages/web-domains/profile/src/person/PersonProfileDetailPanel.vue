<template>
  <div v-loading="loading">
    <template v-if="detail">
      <el-alert v-if="readonly" title="该档案已注销，全部历史只读" type="info" :closable="false" />
      <div class="actions">
        <el-button v-if="actions.revise" @click="openCommand('revision')">修订核心字段</el-button>
        <el-button v-if="actions.assign" @click="openCommand('assign')">指定账户</el-button>
        <el-button v-if="actions.manageBinding" @click="openCommand('binding')">处置绑定</el-button>
        <el-button v-if="actions.revoke" type="danger" @click="openCommand('revoke')">注销档案</el-button>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">{{ detail.profile.fullName }}</el-descriptions-item>
        <el-descriptions-item label="证件类型">{{ detail.profile.documentTypeCode }}</el-descriptions-item>
        <el-descriptions-item label="证件号码" :span="2">{{ detail.profile.documentNumber }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ detail.profile.gender }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ detail.profile.birthDate }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions v-if="currentVersion" class="current-version" title="当前个人版本" :column="2" border>
        <el-descriptions-item label="有效期起">{{ currentVersion.validFrom || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="有效期止">{{ currentVersion.validUntil || '长期' }}</el-descriptions-item>
        <el-descriptions-item label="来源类型">{{ currentVersion.sourceType }}</el-descriptions-item>
        <el-descriptions-item label="来源编号">{{ currentVersion.sourceId }}</el-descriptions-item>
      </el-descriptions>
      <el-tabs>
        <el-tab-pane label="版本历史">
          <el-table :data="detail.versions" row-key="versionId">
            <el-table-column prop="versionNo" label="版本" width="70" />
            <el-table-column prop="sourceType" label="来源" />
            <el-table-column prop="status" label="状态" />
            <el-table-column prop="publishedTime" label="发布时间" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="不可变来源">
          <el-collapse>
            <el-collapse-item
              v-for="source in detail.sources"
              :key="source.sourceId"
              :title="`${source.sourceType} #${source.sourceId}`"
            >
              <el-descriptions :column="2" border>
                <el-descriptions-item label="操作账户">{{ source.operatorUserId }}</el-descriptions-item>
                <el-descriptions-item label="发生时间">{{ source.occurredTime }}</el-descriptions-item>
                <el-descriptions-item label="原因" :span="2">{{ source.reason }}</el-descriptions-item>
              </el-descriptions>
              <pre>{{ formatSnapshot(source.fieldSnapshotJson) }}</pre>
            </el-collapse-item>
          </el-collapse>
        </el-tab-pane>
        <el-tab-pane label="绑定历史">
          <el-table :data="detail.bindings" row-key="bindingId">
            <el-table-column prop="userId" label="账户" />
            <el-table-column prop="status" label="状态" />
            <el-table-column prop="bindingVersion" label="版本" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="材料">
          <el-table :data="detail.currentMaterials" row-key="materialRefId">
            <el-table-column prop="materialTagName" label="标签" />
            <el-table-column prop="fileName" label="文件" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="primary" @click="download(row.materialRefId)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="操作审计">
          <el-table :data="detail.audits" row-key="auditId">
            <el-table-column prop="operationType" label="操作" />
            <el-table-column prop="reason" label="原因" />
            <el-table-column prop="occurredTime" label="时间" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </template>
    <el-dialog v-model="commandVisible" :title="commandTitle" width="min(680px, 92vw)" append-to-body>
      <PersonIdentityForm v-if="command === 'revision'" :model="identity" />
      <el-form label-width="92px">
        <el-form-item v-if="command === 'assign'" label="目标账户">
          <el-select
            v-model="userId"
            filterable
            remote
            reserve-keyword
            placeholder="输入用户名、姓名或手机号检索"
            :loading="userLoading"
            :remote-method="searchUsers"
          >
            <el-option v-for="user in userOptions" :key="user.userId" :label="user.label" :value="user.userId" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="command === 'binding'" label="绑定动作">
          <el-select v-model="binding">
            <el-option label="暂停" value="SUSPEND" />
            <el-option label="恢复" value="RESUME" />
            <el-option label="解绑" value="UNBIND" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作原因"><el-input v-model="reason" type="textarea" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="commandVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="execute">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { BindingCommand, Identifier, PersonIdentity, PersonProfileDetail } from '@namewta/domain-profile';
import { computed, onMounted, reactive, ref } from 'vue';
import type { ProfileUserOption, ProfileWebRuntime } from '../runtime';
import { bindingAction, isPersonIdentityComplete, personActionMatrix, safeErrorMessage } from './logic';
import PersonIdentityForm from './PersonIdentityForm.vue';
const props = defineProps<{ runtime: ProfileWebRuntime; profileId: Identifier }>();
const detail = ref<PersonProfileDetail>();
const loading = ref(false);
const saving = ref(false);
const commandVisible = ref(false);
const command = ref<'assign' | 'binding' | 'revision' | 'revoke'>('revision');
const reason = ref('');
const userId = ref<Identifier | ''>('');
const userLoading = ref(false);
const userOptions = ref<readonly ProfileUserOption[]>([]);
const binding = ref<BindingCommand['action']>('SUSPEND');
const identity = reactive<PersonIdentity>({
  fullName: '',
  documentTypeCode: '',
  documentNumber: '',
  gender: '',
  birthDate: '',
  validFrom: '',
  validUntil: ''
});
const readonly = computed(() => detail.value?.profile.status === 'REVOKED');
const currentVersion = computed(
  () => detail.value?.versions.find(item => item.status === 'CURRENT') ?? detail.value?.versions[0]
);
const currentBinding = computed(() =>
  detail.value?.bindings.find(item => item.status === 'ACTIVE' || item.status === 'SUSPENDED')
);
const actions = computed(() =>
  detail.value
    ? personActionMatrix(detail.value, props.runtime.hasPermission)
    : { assign: false, manageBinding: false, revise: false, revoke: false }
);
const commandTitle = computed(
  () =>
    ({ assign: '指定绑定账户', binding: '处置档案绑定', revision: '修订核心字段', revoke: '注销个人档案' })[
      command.value
    ]
);
async function load() {
  loading.value = true;
  try {
    detail.value = (await props.runtime.service.person.archive.detail(props.profileId)).data;
    if (currentVersion.value) Object.assign(identity, currentVersion.value);
  } catch (error) {
    props.runtime.error(safeErrorMessage(error));
  } finally {
    loading.value = false;
  }
}
function openCommand(value: typeof command.value) {
  command.value = value;
  reason.value = '';
  userId.value = '';
  userOptions.value = [];
  if (value === 'binding' && currentBinding.value) binding.value = bindingAction(currentBinding.value.status);
  commandVisible.value = true;
}
async function searchUsers(keyword: string) {
  if (!keyword.trim()) {
    userOptions.value = [];
    return;
  }
  userLoading.value = true;
  try {
    userOptions.value = await props.runtime.findUsers('PERSON', keyword.trim());
  } catch (error) {
    props.runtime.error(safeErrorMessage(error));
  } finally {
    userLoading.value = false;
  }
}
async function download(materialRefId: Identifier) {
  try {
    const access = (await props.runtime.service.person.archive.material(props.profileId, materialRefId)).data;
    await props.runtime.downloadMaterial(access);
  } catch (error) {
    props.runtime.error(safeErrorMessage(error));
  }
}
async function execute() {
  if (!detail.value || !reason.value.trim()) return props.runtime.warning('请填写操作原因');
  if (command.value === 'revision' && !isPersonIdentityComplete(identity))
    return props.runtime.warning('请完整填写身份信息');
  if (command.value === 'assign' && userId.value === '') return props.runtime.warning('请选择目标账户');
  if (command.value === 'binding' && !currentBinding.value) return props.runtime.warning('当前没有可处置的有效绑定');
  await props.runtime.confirm(`确认执行“${commandTitle.value}”？`);
  saving.value = true;
  try {
    const archive = props.runtime.service.person.archive;
    if (command.value === 'revision')
      await archive.revise(props.profileId, {
        identity,
        reason: reason.value.trim(),
        expectedVersion: currentVersion.value?.versionNo ?? 0
      });
    else if (command.value === 'assign')
      await archive.assign(props.profileId, { userId: userId.value, reason: reason.value.trim() });
    else if (command.value === 'binding') {
      const current = currentBinding.value!;
      await archive.manageBinding(props.profileId, {
        action: binding.value,
        reason: reason.value.trim(),
        expectedBindingVersion: current.bindingVersion
      });
    } else
      await archive.revoke(props.profileId, {
        reason: reason.value.trim(),
        expectedVersion: currentVersion.value?.versionNo ?? 0
      });
    props.runtime.success('档案状态已更新');
    commandVisible.value = false;
    await load();
  } catch (error) {
    props.runtime.error(safeErrorMessage(error));
    await load();
  } finally {
    saving.value = false;
  }
}
onMounted(load);

function formatSnapshot(value: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}
</script>

<style scoped>
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}
.current-version {
  margin-top: 12px;
}
pre {
  margin: 8px 0;
  padding: 12px;
  overflow: auto;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color);
}
</style>
