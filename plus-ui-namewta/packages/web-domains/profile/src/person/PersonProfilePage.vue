<template>
  <div class="person-profile-page">
    <header class="page-toolbar">
      <div>
        <h2>个人档案</h2>
        <span>共 {{ total }} 条</span>
      </div>
      <el-button v-if="canOverride" type="primary" icon="Plus" @click="openCreate">新建档案</el-button>
    </header>
    <el-form :model="query" inline class="filters" @submit.prevent="load">
      <el-form-item label="姓名"><el-input v-model="query.fullName" clearable /></el-form-item>
      <el-form-item label="证件号"><el-input v-model="query.documentNumber" clearable /></el-form-item>
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
      <el-table-column prop="fullName" label="姓名" min-width="120" />
      <el-table-column prop="documentTypeCode" label="证件类型" min-width="160" />
      <el-table-column prop="documentNumber" label="证件号码" min-width="220" />
      <el-table-column prop="bindingUserId" label="绑定账户" width="120" />
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

    <el-drawer v-model="detailVisible" title="个人档案详情" size="min(980px, 94vw)" destroy-on-close>
      <PersonProfileDetailPanel v-if="selectedId !== null" :runtime="runtime" :profile-id="selectedId" />
    </el-drawer>

    <el-dialog v-model="createVisible" title="新建个人档案" width="min(720px, 94vw)">
      <PersonIdentityForm :model="createForm.identity" />
      <el-form :model="createForm" label-width="96px">
        <el-form-item label="绑定账户">
          <el-select
            v-model="createForm.bindUserId"
            clearable
            filterable
            remote
            reserve-keyword
            placeholder="输入用户名、姓名或手机号检索，可留空"
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
            <h3>档案材料</h3>
            <span>每份材料必须选择标签，最多 10 份，单个文件不超过 10MB</span>
          </div>
          <el-button icon="Plus" :disabled="materialRows.length >= 10" @click="addMaterial">添加材料</el-button>
        </div>
        <el-empty v-if="materialRows.length === 0" description="请添加档案材料" :image-size="54" />
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
  type Identifier,
  type MaterialInput,
  type PersonIdentity,
  type PersonProfileSummary
} from '@namewta/domain-profile';
import { computed, onMounted, reactive, ref } from 'vue';
import type { ProfileUserOption, ProfileWebRuntime } from '../runtime';
import { flattenPersonMaterialOptions, isPersonIdentityComplete, safeErrorMessage } from './logic';
import PersonIdentityForm from './PersonIdentityForm.vue';
import PersonProfileDetailPanel from './PersonProfileDetailPanel.vue';

const { runtime } = defineProps<{ runtime: ProfileWebRuntime }>();
const rows = ref<PersonProfileSummary[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const detailVisible = ref(false);
const createVisible = ref(false);
const selectedId = ref<Identifier | null>(null);
const userLoading = ref(false);
const userOptions = ref<readonly ProfileUserOption[]>([]);
const materialOptionsLoading = ref(false);
const materialOptions = ref<ReturnType<typeof flattenPersonMaterialOptions>>([]);
let materialKey = 0;
interface MaterialDraft {
  key: number;
  materialNodeId: Identifier | '';
  ossId: string;
}
const materialRows = ref<MaterialDraft[]>([]);
const materialFileTypes = ['png', 'jpg', 'jpeg', 'pdf', 'doc', 'docx'];
const query = reactive({ fullName: '', documentNumber: '', status: '', pageNum: 1, pageSize: 10 });
const emptyIdentity = (): PersonIdentity => ({
  fullName: '',
  documentTypeCode: 'CN_RESIDENT_ID',
  documentNumber: '',
  gender: '0',
  birthDate: '',
  validFrom: '',
  validUntil: ''
});
const createForm = reactive<{ identity: PersonIdentity; bindUserId: Identifier | null; reason: string }>({
  identity: emptyIdentity(),
  bindUserId: null,
  reason: ''
});
const canOverride = computed(() => runtime.hasPermission(profilePermissions.person.override));
async function load() {
  loading.value = true;
  try {
    const response = await runtime.service.person.archive.page({ ...query, status: query.status || undefined });
    rows.value = response.data.rows;
    total.value = response.data.total;
  } catch (error) {
    runtime.error(safeErrorMessage(error));
  } finally {
    loading.value = false;
  }
}
function reset() {
  Object.assign(query, { fullName: '', documentNumber: '', status: '', pageNum: 1, pageSize: 10 });
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
    userOptions.value = await runtime.findUsers('PERSON', keyword.trim());
  } catch (error) {
    runtime.error(safeErrorMessage(error));
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
    const [person, common] = await Promise.all([
      runtime.service.materialTags.tree('PERSON'),
      runtime.service.materialTags.tree('COMMON')
    ]);
    materialOptions.value = flattenPersonMaterialOptions([...person.data, ...common.data]);
  } catch (error) {
    runtime.error(safeErrorMessage(error));
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
  if (!isPersonIdentityComplete(createForm.identity)) return runtime.warning('请完整填写身份信息');
  if (!createForm.reason.trim()) return runtime.warning('请填写操作原因');
  const materials = materialRows.value
    .filter(material => material.materialNodeId !== '' && material.ossId !== '')
    .map<MaterialInput>(material => ({
      materialNodeId: material.materialNodeId,
      ossId: material.ossId
    }));
  if (materials.length !== materialRows.value.length || materials.length === 0)
    return runtime.warning('请为每份材料选择标签并完成文件上传');
  await runtime.confirm('确认新建个人档案？');
  saving.value = true;
  try {
    await runtime.service.person.archive.create({
      identity: createForm.identity,
      bindUserId: createForm.bindUserId,
      reason: createForm.reason.trim(),
      materials
    });
    runtime.success('个人档案已创建');
    createVisible.value = false;
    Object.assign(createForm, { identity: emptyIdentity(), bindUserId: null, reason: '' });
    materialRows.value = [];
    userOptions.value = [];
    await load();
  } catch (error) {
    runtime.error(safeErrorMessage(error));
  } finally {
    saving.value = false;
  }
}
onMounted(load);
</script>

<style scoped>
.person-profile-page {
  padding: 16px;
  min-width: 0;
}
.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 12px;
}
.page-toolbar h2 {
  margin: 0 0 4px;
  font-size: 20px;
}
.page-toolbar span {
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
.section-heading,
.material-row {
  display: flex;
  gap: 12px;
}
.section-heading {
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}
.section-heading h3 {
  margin: 0 0 4px;
  font-size: 16px;
}
.section-heading span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
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
