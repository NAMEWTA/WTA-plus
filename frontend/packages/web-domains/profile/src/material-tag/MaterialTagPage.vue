<template>
  <div class="profile-material-tag-page">
    <header class="page-toolbar">
      <div>
        <h2>材料标签</h2>
        <span>{{ nodeCount }} 个节点</span>
      </div>
      <div class="toolbar-controls">
        <el-segmented v-model="scope" :options="scopeOptions" @change="loadTree" />
        <el-checkbox v-model="includeDisabled" @change="loadTree">显示停用</el-checkbox>
        <el-button icon="Refresh" circle :loading="loading" aria-label="刷新" @click="loadTree" />
      </div>
    </header>

    <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" />

    <el-table
      v-loading="loading"
      :data="nodes"
      row-key="materialNodeId"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
      empty-text="暂无材料节点"
    >
      <el-table-column prop="nodeName" label="名称" min-width="260">
        <template #default="{ row }">
          <div class="node-name">
            <el-tag :type="row.nodeType === 'TAG' ? 'success' : 'info'" effect="plain" size="small">
              {{ materialNodeTypeLabel(row.nodeType) }}
            </el-tag>
            <span>{{ row.nodeName }}</span>
            <el-tag v-if="row.systemRequired" type="danger" size="small">系统必传</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="materialTagCode" label="标签编码" min-width="220">
        <template #default="{ row }">{{ row.materialTagCode || '-' }}</template>
      </el-table-column>
      <el-table-column prop="nodeDepth" label="层级" width="80" align="center" />
      <el-table-column prop="orderNum" label="排序" width="80" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.enabled"
            :disabled="!canManage || !canChangeMaterialLifecycle(row as MaterialNode)"
            @change="value => changeStatus(row as MaterialNode, Boolean(value))"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <el-tooltip
            v-if="canManage && allowedChildTypes(row as MaterialNode).length"
            content="新增子级"
            placement="top"
          >
            <el-button
              link
              type="primary"
              icon="Plus"
              aria-label="新增子级"
              @click="openCreate(row as MaterialNode)"
            />
          </el-tooltip>
          <el-tooltip v-if="canManage" content="修改" placement="top">
            <el-button
              link
              type="primary"
              icon="Edit"
              aria-label="修改"
              @click="openEdit(row as MaterialNode)"
            />
          </el-tooltip>
          <el-tooltip
            v-if="canManage && canChangeMaterialLifecycle(row as MaterialNode)"
            content="归档"
            placement="top"
          >
            <el-button
              link
              type="danger"
              icon="FolderDelete"
              aria-label="归档"
              @click="archiveNode(row as MaterialNode)"
            />
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId === null ? '新增材料节点' : '修改材料节点'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="上级分类">
          <el-input :model-value="parentName" disabled />
        </el-form-item>
        <el-form-item label="节点类型" prop="nodeType">
          <el-segmented v-model="form.nodeType" :options="typeOptions" :disabled="editingId !== null" />
        </el-form-item>
        <el-form-item label="显示名称" prop="nodeName">
          <el-input v-model="form.nodeName" maxlength="100" />
        </el-form-item>
        <el-form-item v-if="form.nodeType === 'TAG'" label="标签编码" prop="materialTagCode">
          <el-input
            v-model="form.materialTagCode"
            maxlength="100"
            :disabled="editingNode?.systemRequired || (editingId !== null && !canEditMaterialCode(editingNode!))"
          />
        </el-form-item>
        <el-form-item label="显示排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { MaterialNode, MaterialNodeCommand, MaterialNodeType, MaterialScope } from '@namewta/domain-profile';
import type { FormInstance, FormRules } from 'element-plus';
import { profilePermissions } from '@namewta/domain-profile';
import { computed, onMounted, reactive, ref } from 'vue';
import type { ProfileWebRuntime } from '../runtime';
import {
  allowedChildTypes,
  canChangeMaterialLifecycle,
  canEditMaterialCode,
  countMaterialNodes,
  executeMaterialCommand,
  materialNodeTypeLabel
} from './logic';

const { runtime } = defineProps<{ runtime: ProfileWebRuntime }>();
const scopeOptions = [
  { label: '个人', value: 'PERSON' },
  { label: '企业', value: 'ENTERPRISE' },
  { label: '通用', value: 'COMMON' }
];
const scope = ref<MaterialScope>('PERSON');
const includeDisabled = ref(false);
const nodes = ref<MaterialNode[]>([]);
const loading = ref(false);
const saving = ref(false);
const loadError = ref('');
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const editingId = ref<string | number | null>(null);
const editingNode = ref<MaterialNode>();
const parentNode = ref<MaterialNode>();
const nodeCount = computed(() => countMaterialNodes(nodes.value));
const canManage = computed(() => runtime.hasPermission(profilePermissions.materialTag.manage));
const parentName = computed(() => parentNode.value?.nodeName ?? '固定一级分类');
const typeOptions = computed(() =>
  (editingNode.value ? [editingNode.value.nodeType] : allowedChildTypes(parentNode.value!)).map(value => ({
    label: materialNodeTypeLabel(value),
    value
  }))
);

const form = reactive<MaterialNodeCommand>({
  parentId: 0,
  nodeType: 'TAG',
  scope: 'PERSON',
  materialTagCode: '',
  nodeName: '',
  systemRequired: false,
  orderNum: 0,
  expectedVersion: 0
});
const rules: FormRules = {
  nodeName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  materialTagCode: [
    {
      validator: (_rule, value, callback) => {
        if (form.nodeType === 'TAG' && !String(value ?? '').trim()) callback(new Error('请输入标签编码'));
        else callback();
      },
      trigger: 'blur'
    }
  ]
};

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '操作失败';
}

async function loadTree() {
  loading.value = true;
  loadError.value = '';
  try {
    const response = await runtime.service.materialTags.tree(scope.value, includeDisabled.value);
    nodes.value = response.data;
  } catch (error) {
    loadError.value = errorMessage(error);
    runtime.error(loadError.value);
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  Object.assign(form, {
    parentId: 0,
    nodeType: 'TAG',
    scope: scope.value,
    materialTagCode: '',
    nodeName: '',
    systemRequired: false,
    orderNum: 0,
    expectedVersion: 0
  });
  formRef.value?.clearValidate();
}

function openCreate(parent: MaterialNode) {
  resetForm();
  editingId.value = null;
  editingNode.value = undefined;
  parentNode.value = parent;
  form.parentId = parent.materialNodeId;
  form.scope = parent.scope;
  form.nodeType = allowedChildTypes(parent)[0] ?? 'TAG';
  dialogVisible.value = true;
}

function openEdit(node: MaterialNode) {
  resetForm();
  editingId.value = node.materialNodeId;
  editingNode.value = node;
  parentNode.value = undefined;
  Object.assign(form, {
    parentId: node.parentId,
    nodeType: node.nodeType,
    scope: node.scope,
    materialTagCode: node.materialTagCode,
    nodeName: node.nodeName,
    systemRequired: node.systemRequired,
    orderNum: node.orderNum,
    expectedVersion: node.version
  });
  dialogVisible.value = true;
}

async function submit() {
  if (!(await formRef.value?.validate())) return;
  saving.value = true;
  try {
    const command = { ...form, materialTagCode: form.nodeType === 'TAG' ? form.materialTagCode?.trim() || null : null };
    const succeeded = await executeMaterialCommand(
      () =>
        editingId.value === null
          ? runtime.service.materialTags.create(command)
          : runtime.service.materialTags.update(editingId.value, command),
      () => runtime.success('材料节点已保存'),
      error => runtime.error(errorMessage(error))
    );
    if (succeeded) {
      dialogVisible.value = false;
      await loadTree();
    }
  } finally {
    saving.value = false;
  }
}

async function changeStatus(node: MaterialNode, enabled: boolean) {
  try {
    await runtime.confirm(enabled ? '确认启用该材料节点？' : '确认停用该材料节点？');
    await runtime.service.materialTags.changeStatus(node.materialNodeId, enabled, node.version);
    runtime.success('状态已更新');
    await loadTree();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') runtime.error(errorMessage(error));
  }
}

async function archiveNode(node: MaterialNode) {
  try {
    await runtime.confirm(`确认归档“${node.nodeName}”？`);
    await runtime.service.materialTags.archive(node.materialNodeId, node.version);
    runtime.success('材料节点已归档');
    await loadTree();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') runtime.error(errorMessage(error));
  }
}

onMounted(loadTree);
</script>

<style scoped>
.profile-material-tag-page {
  min-width: 0;
  padding: 16px;
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.page-toolbar h2 {
  margin: 0 0 4px;
  font-size: 20px;
}

.page-toolbar span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.toolbar-controls,
.node-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 760px) {
  .page-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-controls {
    flex-wrap: wrap;
  }
}
</style>
