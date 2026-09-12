<template>
  <div class="p-2 app-container system-user-type-page">
    <div class="search-wrap">
      <el-card shadow="hover" class="search-panel" :class="{ 'is-collapsed': !showSearch }">
        <template #header>
          <div class="panel-heading search-panel-toggle" @click.stop="showSearch = !showSearch">
            <div><h3>筛选条件</h3></div>
          </div>
        </template>
        <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="85px" class="query-form">
          <el-form-item label="登录域编码" prop="userTypeCode">
            <el-input
              v-model="queryParams.userTypeCode"
              placeholder="请输入登录域编码"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="登录域名称" prop="userTypeName">
            <el-input
              v-model="queryParams.userTypeName"
              placeholder="请输入登录域名称"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="状态" clearable>
              <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <el-card shadow="hover" class="table-panel">
      <template #header>
        <div class="toolbar-shell">
          <div class="table-heading">
            <h3>登录域列表</h3>
          </div>
          <div class="toolbar-actions">
            <el-button v-hasPermi="['system:userType:add']" type="primary" plain icon="Plus" @click="handleAdd">
              新增
            </el-button>
            <el-button
              v-hasPermi="['system:userType:edit']"
              type="success"
              plain
              icon="Edit"
              :disabled="single"
              @click="handleUpdate()"
            >
              修改
            </el-button>
            <el-button
              v-hasPermi="['system:userType:remove']"
              type="danger"
              plain
              icon="Delete"
              :disabled="multiple"
              @click="handleDelete()"
            >
              删除
            </el-button>
            <el-button
              v-hasPermi="['system:userType:export']"
              type="warning"
              plain
              icon="Download"
              @click="handleExport"
            >
              导出
            </el-button>
            <right-toolbar v-model:show-search="showSearch" :search="false" @query-table="getList"></right-toolbar>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="userTypeList"
        border
        class="data-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column v-if="false" label="登录域编号" align="center" prop="userTypeId" />
        <el-table-column label="登录域编码" align="center" prop="userTypeCode" />
        <el-table-column label="登录域名称" align="center" prop="userTypeName" />
        <el-table-column label="显示顺序" align="center" prop="orderNum" width="100" />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="修改" placement="top">
              <el-button
                v-hasPermi="['system:userType:edit']"
                link
                type="primary"
                icon="Edit"
                @click="handleUpdate(scope.row)"
              ></el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button
                v-hasPermi="['system:userType:remove']"
                link
                type="primary"
                icon="Delete"
                @click="handleDelete(scope.row)"
              ></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>
    <!-- 添加或修改登录域对话框 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="500px" append-to-body>
      <el-form ref="userTypeFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="登录域编码" prop="userTypeCode">
          <el-input v-model="form.userTypeCode" :disabled="form.userTypeId != null" placeholder="请输入登录域编码" />
        </el-form-item>
        <el-form-item label="登录域名称" prop="userTypeName">
          <el-input v-model="form.userTypeName" placeholder="请输入登录域名称" />
        </el-form-item>
        <el-form-item label="显示顺序" prop="orderNum">
          <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button :loading="buttonLoading" type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="UserType" lang="ts">
import type { UserTypeForm, UserTypeQuery, UserTypeVO } from '@namewta/domain-system';
import type { FormInstance as ElFormInstance } from 'element-plus';
import { onMounted, reactive, ref, toRefs } from 'vue';
import type { SystemWebRuntime } from '../runtime';
import { useFormDialog, useLoading, useSearchReset, useSearchToggle, useTableSelection } from '../composables';

const { runtime } = defineProps<{ runtime: SystemWebRuntime }>();
const {
  add: addUserType,
  delete: delUserType,
  get: getUserType,
  list: listUserType,
  update: updateUserType
} = runtime.service.userTypes;
const modal = { confirm: runtime.confirm, msgSuccess: runtime.success };
const requestDownload = runtime.download;
const { sys_normal_disable } = runtime.dicts('sys_normal_disable');

const userTypeList = ref<UserTypeVO[]>([]);
const { loading, withLoading } = useLoading(true);
const { loading: buttonLoading, withLoading: withButtonLoading } = useLoading();
const { showSearch } = useSearchToggle();
const { ids, single, multiple, handleSelectionChange } = useTableSelection<UserTypeVO>(item => item.userTypeId);
const total = ref(0);

const queryFormRef = ref<ElFormInstance>();
const userTypeFormRef = ref<ElFormInstance>();

const initFormData: UserTypeForm = {
  userTypeId: undefined,
  userTypeCode: undefined,
  userTypeName: undefined,
  status: '0',
  orderNum: 0,
  remark: undefined
};
const data = reactive<PageData<UserTypeForm, UserTypeQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userTypeCode: undefined,
    userTypeName: undefined,
    status: undefined
  },
  rules: {
    userTypeCode: [{ required: true, message: '登录域编码不能为空', trigger: 'blur' }],
    userTypeName: [{ required: true, message: '登录域名称不能为空', trigger: 'blur' }],
    orderNum: [{ required: true, message: '显示顺序不能为空', trigger: 'blur' }]
  }
});

const { queryParams, form, rules } = toRefs(data);
const { dialog, resetForm, openDialog, showDialog, closeDialog } = useFormDialog({
  form,
  formRef: userTypeFormRef,
  initialFormData: initFormData
});
const { resetQuery } = useSearchReset({
  queryFormRef,
  queryParams,
  pageNumKey: 'pageNum',
  afterReset: () => {
    handleQuery();
  }
});

/** 查询登录域列表 */
const getList = async () => {
  await withLoading(async () => {
    const res = await listUserType(queryParams.value);
    userTypeList.value = res.data?.rows;
    total.value = res.data?.total;
  });
};

/** 取消按钮 */
const cancel = () => {
  closeDialog();
  resetForm();
};

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

/** 新增按钮操作 */
const handleAdd = () => {
  openDialog('添加登录域');
};

/** 修改按钮操作 */
const handleUpdate = async (row?: Partial<UserTypeVO>) => {
  resetForm();
  const userTypeId = row?.userTypeId || ids.value[0];
  const res = await getUserType(userTypeId);
  Object.assign(form.value, res.data);
  showDialog('修改登录域');
};

/** 提交按钮 */
const submitForm = () => {
  userTypeFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      await withButtonLoading(async () => {
        if (form.value.userTypeId) {
          await updateUserType(form.value);
        } else {
          await addUserType(form.value);
        }
      });
      modal.msgSuccess('操作成功');
      closeDialog();
      await getList();
    }
  });
};

/** 删除按钮操作 */
const handleDelete = async (row?: Partial<UserTypeVO>) => {
  const userTypeIds = row?.userTypeId || ids.value;
  await modal.confirm('是否确认删除登录域编号为"' + userTypeIds + '"的数据项？');
  await delUserType(userTypeIds);
  modal.msgSuccess('删除成功');
  await getList();
};

/** 导出按钮操作 */
const handleExport = () => {
  requestDownload(
    'system/userType/export',
    {
      ...queryParams.value
    },
    `userType_${new Date().getTime()}.xlsx`
  );
};

onMounted(() => {
  getList();
});
</script>
