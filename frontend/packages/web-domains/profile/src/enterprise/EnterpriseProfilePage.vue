<template>
  <div class="enterprise-profile-page">
    <header class="page-toolbar">
      <div>
        <h2>企业档案</h2>
        <span>共 {{ total }} 条</span>
      </div>
      <el-button v-if="canOverride" type="primary" icon="Plus" @click="openCreate">新建档案</el-button>
    </header>
    <el-form :model="query" inline class="filters">
      <el-form-item label="企业名称"><el-input v-model="query.enterpriseName" clearable /></el-form-item>
      <el-form-item label="统一信用代码"><el-input v-model="query.unifiedCreditCode" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable>
          <el-option label="有效" value="ACTIVE" />
          <el-option label="已注销" value="REVOKED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="load">查询</el-button>
        <el-button icon="Refresh" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows" row-key="profileId" border>
      <el-table-column prop="enterpriseName" label="企业名称" min-width="200" />
      <el-table-column prop="unifiedCreditCode" label="统一信用代码" min-width="190" />
      <el-table-column prop="legalRepresentativeName" label="法定代表人" min-width="120" />
      <el-table-column prop="bindingUserId" label="认证负责人账户" min-width="140" />
      <el-table-column label="档案状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'REVOKED' ? 'info' : 'success'">
            {{ row.status === 'REVOKED' ? '已注销' : '有效' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right" align="center">
        <template #default="{ row }">
          <el-tooltip content="查看详情">
            <el-button link type="primary" icon="View" aria-label="查看详情" @click="openDetail(row.profileId)" />
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      layout="total, prev, pager, next, sizes"
      @change="load"
    />

    <el-drawer v-model="detailVisible" title="企业档案详情" size="min(1080px, 96vw)" destroy-on-close>
      <EnterpriseProfileDetailPanel v-if="selectedId !== null" :runtime="runtime" :profile-id="selectedId" />
    </el-drawer>

    <el-dialog v-model="createVisible" title="新建企业档案" width="min(900px, 96vw)" top="4vh">
      <EnterpriseIdentityForm :model="createForm.identity" />
      <el-form :model="createForm" label-width="112px">
        <el-form-item label="认证负责人">
          <el-select
            v-model="createForm.bindUserId"
            clearable
            filterable
            remote
            reserve-keyword
            placeholder="检索已完成个人实名认证的账户，可留空"
            :loading="userLoading"
            :remote-method="searchUsers"
          >
            <el-option v-for="user in userOptions" :key="user.userId" :label="user.label" :value="user.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作原因">
          <el-input v-model="createForm.reason" type="textarea" maxlength="500" />
        </el-form-item>
      </el-form>
      <section class="material-section">
        <div class="section-heading">
          <div>
            <h3>企业材料</h3>
            <span>每份材料必须选择标签，最多 10 份，单个文件不超过 10MB</span>
          </div>
          <el-button icon="Plus" :disabled="materialRows.length >= 10" @click="addMaterial">添加材料</el-button>
        </div>
        <el-empty v-if="materialRows.length === 0" description="请添加企业材料" :image-size="54" />
        <div v-for="(material, index) in materialRows" :key="material.key" class="material-row">
          <el-select
            v-model="material.materialNodeId"
            filterable
            placeholder="选择材料标签"
            :loading="materialOptionsLoading"
          >
            <el-option
              v-for="option in materialOptions"
              :key="option.materialNodeId"
              :label="option.label"
              :value="option.materialNodeId"
            />
          </el-select>
          <component
            :is="runtime.fileUpload"
            v-model="material.ossId"
            :limit="1"
            :file-size="10"
            :file-type="materialFileTypes"
          />
          <el-tooltip content="移除材料">
            <el-button type="danger" plain icon="Delete" aria-label="移除材料" @click="removeMaterial(index)" />
          </el-tooltip>
        </div>
      </section>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="createProfile">确认新建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {
  profilePermissions,
  type EnterpriseIdentity,
  type EnterpriseProfileSummary,
  type Identifier,
  type MaterialInput
} from '@namewta/domain-profile';
import { computed, onMounted, reactive, ref } from 'vue';
import type { ProfileUserOption, ProfileWebRuntime } from '../runtime';
import EnterpriseIdentityForm from './EnterpriseIdentityForm.vue';
import EnterpriseProfileDetailPanel from './EnterpriseProfileDetailPanel.vue';
import { flattenEnterpriseMaterialOptions, isEnterpriseIdentityComplete, safeEnterpriseErrorMessage } from './logic';

const { runtime } = defineProps<{ runtime: ProfileWebRuntime }>();
const rows = ref<EnterpriseProfileSummary[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const detailVisible = ref(false);
const createVisible = ref(false);
const selectedId = ref<Identifier | null>(null);
const userLoading = ref(false);
const userOptions = ref<readonly ProfileUserOption[]>([]);
const materialOptionsLoading = ref(false);
const materialOptions = ref<ReturnType<typeof flattenEnterpriseMaterialOptions>>([]);
let materialKey = 0;
interface MaterialDraft {
  key: number;
  materialNodeId: Identifier | '';
  ossId: string;
}
const materialRows = ref<MaterialDraft[]>([]);
const materialFileTypes = ['png', 'jpg', 'jpeg', 'pdf', 'doc', 'docx'];
const query = reactive({ enterpriseName: '', unifiedCreditCode: '', status: '', pageNum: 1, pageSize: 10 });
const emptyIdentity = (): EnterpriseIdentity => ({
  enterpriseName: '',
  unifiedCreditCode: '',
  enterpriseType: '',
  legalRepresentativeName: '',
  legalDocumentTypeCode: 'CN_RESIDENT_ID',
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
const createForm = reactive<{ identity: EnterpriseIdentity; bindUserId: Identifier | null; reason: string }>({
  identity: emptyIdentity(),
  bindUserId: null,
  reason: ''
});
const canOverride = computed(() => runtime.hasPermission(profilePermissions.enterprise.override));

async function load() {
  loading.value = true;
  try {
    const response = await runtime.service.enterprise.archive.page({
      ...query,
      status: query.status || undefined
    });
    rows.value = response.data.rows;
    total.value = response.data.total;
  } catch (error) {
    runtime.error(safeEnterpriseErrorMessage(error));
  } finally {
    loading.value = false;
  }
}
function reset() {
  Object.assign(query, { enterpriseName: '', unifiedCreditCode: '', status: '', pageNum: 1, pageSize: 10 });
  void load();
}
function openDetail(id: Identifier) {
  selectedId.value = id;
  detailVisible.value = true;
}
async function searchUsers(keyword: string) {
  if (!keyword.trim()) {
    userOptions.value = [];
    return;
  }
  userLoading.value = true;
  try {
    userOptions.value = await runtime.findUsers('ENTERPRISE', keyword.trim());
  } catch (error) {
    runtime.error(safeEnterpriseErrorMessage(error));
  } finally {
    userLoading.value = false;
  }
}
function addMaterial() {
  if (materialRows.value.length < 10) materialRows.value.push({ key: ++materialKey, materialNodeId: '', ossId: '' });
}
function removeMaterial(index: number) {
  materialRows.value.splice(index, 1);
}
async function loadMaterialOptions() {
  if (materialOptions.value.length > 0) return;
  materialOptionsLoading.value = true;
  try {
    const [enterprise, common] = await Promise.all([
      runtime.service.materialTags.tree('ENTERPRISE'),
      runtime.service.materialTags.tree('COMMON')
    ]);
    materialOptions.value = flattenEnterpriseMaterialOptions([...enterprise.data, ...common.data]);
  } catch (error) {
    runtime.error(safeEnterpriseErrorMessage(error));
  } finally {
    materialOptionsLoading.value = false;
  }
}
function openCreate() {
  createVisible.value = true;
  if (materialRows.value.length === 0) addMaterial();
  void loadMaterialOptions();
}
async function createProfile() {
  if (!isEnterpriseIdentityComplete(createForm.identity)) return runtime.warning('请完整填写企业法定信息');
  if (!createForm.reason.trim()) return runtime.warning('请填写操作原因');
  const materials = materialRows.value
    .filter(material => material.materialNodeId !== '' && material.ossId !== '')
    .map<MaterialInput>(material => ({ materialNodeId: material.materialNodeId, ossId: material.ossId }));
  if (materials.length !== materialRows.value.length || materials.length === 0)
    return runtime.warning('请为每份材料选择标签并完成文件上传');
  await runtime.confirm('确认新建企业档案？');
  saving.value = true;
  try {
    await runtime.service.enterprise.archive.create({
      identity: createForm.identity,
      bindUserId: createForm.bindUserId,
      reason: createForm.reason.trim(),
      materials
    });
    runtime.success('企业档案已创建');
    createVisible.value = false;
    Object.assign(createForm, { identity: emptyIdentity(), bindUserId: null, reason: '' });
    materialRows.value = [];
    userOptions.value = [];
    await load();
  } catch (error) {
    runtime.error(safeEnterpriseErrorMessage(error));
  } finally {
    saving.value = false;
  }
}
onMounted(load);
</script>

<style scoped>
.enterprise-profile-page {
  padding: 16px;
  min-width: 0;
}
.page-toolbar,
.section-heading,
.material-row {
  display: flex;
  gap: 12px;
}
.page-toolbar,
.section-heading {
  align-items: flex-start;
  justify-content: space-between;
}
.page-toolbar {
  margin-bottom: 12px;
}
.page-toolbar h2,
.section-heading h3 {
  margin: 0 0 4px;
}
.page-toolbar h2 {
  font-size: 20px;
}
.section-heading h3 {
  font-size: 16px;
}
.page-toolbar span,
.section-heading span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.filters {
  margin-bottom: 8px;
}
.el-pagination {
  justify-content: flex-end;
  margin-top: 12px;
}
.material-section {
  margin-top: 16px;
}
.section-heading {
  margin-bottom: 12px;
}
.material-row {
  align-items: flex-start;
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-lighter);
}
.material-row > .el-select {
  flex: 0 0 min(280px, 40%);
}
.material-row > :deep(.upload-file) {
  flex: 1;
  min-width: 0;
}
@media (max-width: 640px) {
  .material-row {
    flex-wrap: wrap;
  }
  .material-row > .el-select,
  .material-row > :deep(.upload-file) {
    flex: 1 1 100%;
  }
}
</style>
