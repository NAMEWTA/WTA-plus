<template>
  <div v-loading="loading">
    <template v-if="detail">
      <el-alert v-if="readonly" title="该企业档案已注销，全部历史只读" type="info" :closable="false" />
      <div class="actions">
        <el-button v-if="actions.revise" @click="openCommand('revision')">修订核心字段</el-button>
        <el-button v-if="actions.assign" @click="openCommand('assign')">指定认证负责人</el-button>
        <el-button v-if="actions.manageBinding" @click="openCommand('binding')">处置负责人绑定</el-button>
        <el-button v-if="actions.revoke" type="danger" @click="openCommand('revoke')">注销企业档案</el-button>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="企业名称">{{ detail.profile.enterpriseName }}</el-descriptions-item>
        <el-descriptions-item label="统一信用代码">{{ detail.profile.unifiedCreditCode }}</el-descriptions-item>
        <el-descriptions-item label="企业类型">{{ detail.profile.enterpriseType }}</el-descriptions-item>
        <el-descriptions-item label="法定代表人">{{ detail.profile.legalRepresentativeName }}</el-descriptions-item>
        <el-descriptions-item label="认证负责人账户">
          {{ detail.profile.bindingUserId ?? '未绑定' }}
        </el-descriptions-item>
        <el-descriptions-item label="负责人绑定状态">
          {{ detail.profile.bindingStatus ?? '未绑定' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-alert
        class="role-alert"
        title="法定代表人是企业法定字段；认证负责人是当前绑定的系统账户，两者不会自动等同。"
        type="info"
        :closable="false"
      />
      <el-descriptions v-if="currentVersion" class="current-version" title="当前企业版本" :column="2" border>
        <el-descriptions-item label="法定证件类型">{{ currentVersion.legalDocumentTypeCode }}</el-descriptions-item>
        <el-descriptions-item label="法定证件号码">{{ currentVersion.legalDocumentNumber }}</el-descriptions-item>
        <el-descriptions-item label="成立日期">{{ currentVersion.establishedDate }}</el-descriptions-item>
        <el-descriptions-item label="营业期限">
          {{ currentVersion.businessTermFrom || '未填写' }} 至 {{ currentVersion.businessTermUntil || '长期' }}
        </el-descriptions-item>
        <el-descriptions-item label="注册地址" :span="2">{{ currentVersion.registeredAddress }}</el-descriptions-item>
        <el-descriptions-item label="经营范围" :span="2">{{ currentVersion.businessScope }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentVersion.contactName || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentVersion.contactPhone || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="企业邮箱">{{ currentVersion.email || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="注册资本">{{ currentVersion.registeredCapital }}</el-descriptions-item>
        <el-descriptions-item label="行业编码">{{ currentVersion.industryCode || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="企业网站">{{ currentVersion.website || '未填写' }}</el-descriptions-item>
      </el-descriptions>
      <el-tabs>
        <el-tab-pane label="版本历史">
          <el-table :data="detail.versions" row-key="versionId">
            <el-table-column prop="versionNo" label="版本" width="70" />
            <el-table-column prop="enterpriseName" label="企业名称" min-width="180" />
            <el-table-column prop="legalRepresentativeName" label="法定代表人" min-width="120" />
            <el-table-column prop="sourceType" label="来源" />
            <el-table-column prop="status" label="状态" />
            <el-table-column prop="publishedTime" label="发布时间" min-width="170" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="不可变来源">
          <el-collapse>
            <el-collapse-item v-for="source in detail.sources" :key="source.sourceId" :name="source.sourceId">
              <template #title>
                <span>{{ source.sourceType }} · {{ source.occurredTime }} · {{ source.reason }}</span>
              </template>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="来源编号">{{ source.sourceId }}</el-descriptions-item>
                <el-descriptions-item label="操作账户">{{ source.operatorUserId }}</el-descriptions-item>
              </el-descriptions>
              <pre>{{ formatSnapshot(source.fieldSnapshotJson) }}</pre>
            </el-collapse-item>
          </el-collapse>
        </el-tab-pane>
        <el-tab-pane label="负责人历史">
          <el-table :data="detail.bindings" row-key="bindingId">
            <el-table-column prop="userId" label="负责人账户" />
            <el-table-column prop="status" label="状态" />
            <el-table-column prop="sourceType" label="变更来源" />
            <el-table-column prop="bindingVersion" label="绑定版本" />
            <el-table-column prop="boundTime" label="绑定时间" min-width="170" />
            <el-table-column prop="unboundTime" label="解绑时间" min-width="170" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="材料">
          <el-table :data="detail.currentMaterials" row-key="materialRefId">
            <el-table-column prop="materialTagName" label="标签" />
            <el-table-column prop="fileName" label="文件" />
            <el-table-column prop="fileSize" label="大小" width="120" />
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
            <el-table-column prop="capability" label="能力" />
            <el-table-column prop="operatorUserId" label="操作账户" />
            <el-table-column prop="reason" label="原因" min-width="180" />
            <el-table-column prop="result" label="结果" />
            <el-table-column prop="occurredTime" label="时间" min-width="170" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </template>
    <el-dialog v-model="commandVisible" :title="commandTitle" width="min(900px, 94vw)" top="4vh" append-to-body>
      <EnterpriseIdentityForm v-if="command === 'revision'" :model="identity" />
      <el-form label-width="112px">
        <el-form-item v-if="command === 'assign'" label="认证负责人">
          <el-select
            v-model="userId"
            filterable
            remote
            reserve-keyword
            placeholder="检索已完成个人实名认证的账户"
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
import type { BindingCommand, EnterpriseIdentity, EnterpriseProfileDetail, Identifier } from '@namewta/domain-profile';
import { computed, onMounted, reactive, ref } from 'vue';
import type { ProfileUserOption, ProfileWebRuntime } from '../runtime';
import EnterpriseIdentityForm from './EnterpriseIdentityForm.vue';
import {
  enterpriseActionMatrix,
  enterpriseBindingAction,
  isEnterpriseIdentityComplete,
  safeEnterpriseErrorMessage
} from './logic';

const props = defineProps<{ runtime: ProfileWebRuntime; profileId: Identifier }>();
const detail = ref<EnterpriseProfileDetail>();
const loading = ref(false);
const saving = ref(false);
const commandVisible = ref(false);
const command = ref<'assign' | 'binding' | 'revision' | 'revoke'>('revision');
const reason = ref('');
const userId = ref<Identifier | ''>('');
const userLoading = ref(false);
const userOptions = ref<readonly ProfileUserOption[]>([]);
const binding = ref<BindingCommand['action']>('SUSPEND');
const identity = reactive<EnterpriseIdentity>({
  enterpriseName: '',
  unifiedCreditCode: '',
  enterpriseType: '',
  legalRepresentativeName: '',
  legalDocumentTypeCode: '',
  legalDocumentNumber: '',
  establishedDate: '',
  businessTermFrom: '',
  businessTermUntil: '',
  registeredAddress: '',
  businessScope: '',
  contactName: '',
  contactPhone: '',
  email: '',
  registeredCapital: 0,
  industryCode: '',
  website: ''
});
const readonly = computed(() => detail.value?.profile.status === 'REVOKED');
const currentBinding = computed(() =>
  detail.value?.bindings.find(item => item.status === 'ACTIVE' || item.status === 'SUSPENDED')
);
const currentVersion = computed(
  () => detail.value?.versions.find(item => item.status === 'CURRENT') ?? detail.value?.versions[0]
);
const actions = computed(() =>
  detail.value
    ? enterpriseActionMatrix(detail.value, props.runtime.hasPermission)
    : { assign: false, manageBinding: false, revise: false, revoke: false }
);
const commandTitle = computed(
  () =>
    ({
      assign: '指定企业认证负责人',
      binding: '处置负责人绑定',
      revision: '修订企业核心字段',
      revoke: '注销企业档案'
    })[command.value]
);

function formatSnapshot(value: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}
async function load() {
  loading.value = true;
  try {
    detail.value = (await props.runtime.service.enterprise.archive.detail(props.profileId)).data;
    if (currentVersion.value) Object.assign(identity, currentVersion.value);
  } catch (error) {
    props.runtime.error(safeEnterpriseErrorMessage(error));
  } finally {
    loading.value = false;
  }
}
function openCommand(value: typeof command.value) {
  command.value = value;
  reason.value = '';
  userId.value = '';
  userOptions.value = [];
  if (value === 'binding' && currentBinding.value) binding.value = enterpriseBindingAction(currentBinding.value.status);
  commandVisible.value = true;
}
async function searchUsers(keyword: string) {
  if (!keyword.trim()) {
    userOptions.value = [];
    return;
  }
  userLoading.value = true;
  try {
    userOptions.value = await props.runtime.findUsers('ENTERPRISE', keyword.trim());
  } catch (error) {
    props.runtime.error(safeEnterpriseErrorMessage(error));
  } finally {
    userLoading.value = false;
  }
}
async function download(materialRefId: Identifier) {
  try {
    const access = (await props.runtime.service.enterprise.archive.material(props.profileId, materialRefId)).data;
    await props.runtime.downloadMaterial(access);
  } catch (error) {
    props.runtime.error(safeEnterpriseErrorMessage(error));
  }
}
async function execute() {
  if (!detail.value || !reason.value.trim()) return props.runtime.warning('请填写操作原因');
  if (command.value === 'revision' && !isEnterpriseIdentityComplete(identity))
    return props.runtime.warning('请完整填写企业法定信息');
  if (command.value === 'assign' && userId.value === '') return props.runtime.warning('请选择认证负责人账户');
  if (command.value === 'binding' && !currentBinding.value) return props.runtime.warning('当前没有可处置的负责人绑定');
  await props.runtime.confirm(`确认执行“${commandTitle.value}”？`);
  saving.value = true;
  try {
    const archive = props.runtime.service.enterprise.archive;
    if (command.value === 'revision')
      await archive.revise(props.profileId, {
        identity,
        reason: reason.value.trim(),
        expectedVersion: currentVersion.value?.versionNo ?? 0
      });
    else if (command.value === 'assign')
      await archive.assign(props.profileId, { userId: userId.value, reason: reason.value.trim() });
    else if (command.value === 'binding')
      await archive.manageBinding(props.profileId, {
        action: binding.value,
        reason: reason.value.trim(),
        expectedBindingVersion: currentBinding.value!.bindingVersion
      });
    else
      await archive.revoke(props.profileId, {
        reason: reason.value.trim(),
        expectedVersion: currentVersion.value?.versionNo ?? 0
      });
    props.runtime.success('企业档案状态已更新');
    commandVisible.value = false;
    await load();
  } catch (error) {
    props.runtime.error(safeEnterpriseErrorMessage(error));
    await load();
  } finally {
    saving.value = false;
  }
}
onMounted(load);
</script>

<style scoped>
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}
.role-alert {
  margin-top: 12px;
}
.current-version {
  margin-top: 16px;
}
pre {
  margin: 10px 0 0;
  padding: 12px;
  overflow: auto;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color);
}
</style>
