<template>
  <div class="p-2 app-container system-oss-page">
    <div class="search-wrap">
      <el-card shadow="hover" class="search-panel" :class="{ 'is-collapsed': !showSearch }">
        <template #header>
          <div class="panel-heading search-panel-toggle" @click.stop="showSearch = !showSearch">
            <div>
              <span class="panel-kicker">Search Filters</span>
              <h3>筛选条件</h3>
            </div>
          </div>
        </template>
        <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="query-form">
          <el-form-item label="文件名" prop="fileName">
            <el-input v-model="queryParams.fileName" placeholder="请输入文件名" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="原名" prop="originalName">
            <el-input
              v-model="queryParams.originalName"
              placeholder="请输入原名"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="文件后缀" prop="fileSuffix">
            <el-input
              v-model="queryParams.fileSuffix"
              placeholder="请输入文件后缀"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="创建时间" style="width: 308px">
            <el-date-picker
              v-model="dateRangeCreateTime"
              value-format="YYYY-MM-DD HH:mm:ss"
              type="daterange"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
            ></el-date-picker>
          </el-form-item>
          <el-form-item label="服务商" prop="service">
            <el-input v-model="queryParams.service" placeholder="请输入服务商" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <el-card shadow="hover" class="table-panel">
      <template #header>
        <div class="toolbar-shell">
          <div class="table-heading">
            <span class="panel-kicker">Object Storage</span>
            <h3>文件列表</h3>
            <p>共 {{ total }} 条记录，支持文件上传、预览切换和 OSS 配置跳转。</p>
          </div>
          <div class="toolbar-actions">
            <el-button v-hasPermi="['system:oss:upload']" type="primary" plain icon="Upload" @click="handleFile">
              上传文件
            </el-button>
            <el-button v-hasPermi="['system:oss:upload']" type="primary" plain icon="Upload" @click="handleImage">
              上传图片
            </el-button>
            <el-button
              v-hasPermi="['system:oss:remove']"
              type="danger"
              plain
              icon="Delete"
              :disabled="multiple"
              @click="handleDelete()"
            >
              删除
            </el-button>
            <el-button
              v-hasPermi="['system:oss:edit']"
              :type="previewListResource ? 'danger' : 'warning'"
              plain
              @click="handlePreviewListResource(!previewListResource)"
            >
              预览开关 : {{ previewListResource ? '禁用' : '启用' }}
            </el-button>
            <el-button
              v-hasPermi="['system:ossConfig:list']"
              type="info"
              plain
              icon="Operation"
              @click="handleOssConfig"
            >
              配置管理
            </el-button>
            <right-toolbar v-model:show-search="showSearch" :search="false" @query-table="getList"></right-toolbar>
          </div>
        </div>
      </template>

      <el-table
        v-if="showTable"
        v-loading="loading"
        :data="ossList"
        class="data-table"
        border
        :header-cell-class-name="handleHeaderClass"
        @selection-change="handleSelectionChange"
        @header-click="handleHeaderCLick"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column v-if="false" label="对象存储主键" align="center" prop="ossId" />
        <el-table-column label="文件名" align="center" prop="fileName" />
        <el-table-column label="原名" align="center" prop="originalName" />
        <el-table-column label="文件后缀" align="center" prop="fileSuffix" />
        <el-table-column label="文件展示" align="center" prop="url">
          <template #default="scope">
            <ImagePreview
              v-if="filePresentation(scope.row) === 'image' && previewUrl(scope.row)"
              :width="100"
              :height="100"
              :src="previewUrl(scope.row)"
              :preview-src-list="[previewUrl(scope.row)]"
            />
            <span v-else-if="filePresentation(scope.row) === 'deleted'">{{ deletedMessage }}</span>
            <span v-else-if="filePresentation(scope.row) !== 'image'" v-text="scope.row.url" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="180" sortable="custom">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上传人" align="center" prop="createByName" />
        <el-table-column label="服务商" align="center" prop="service" sortable="custom" />
        <el-table-column label="访问类型" align="center" prop="accessPolicy" width="110">
          <template #default="scope">
            <el-tag v-if="scope.row.accessPolicy === 'PUBLIC_READ'" type="success">公开只读</el-tag>
            <el-tag v-else-if="scope.row.accessPolicy === 'PRIVATE'" type="warning">私有</el-tag>
            <span v-else>未知</span>
          </template>
        </el-table-column>
        <el-table-column label="生命周期" align="center" width="116">
          <template #default="scope">
            <el-tag v-if="scope.row.deleteState === 'PENDING'" type="danger" effect="light">待删除</el-tag>
            <el-tag v-else-if="scope.row.isTemp === 'Y'" type="warning" effect="light">临时</el-tag>
            <el-tag v-else type="success" effect="light">已绑定</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="过期时间" align="center" width="180">
          <template #default="scope">
            <span>{{ scope.row.expireTime ? parseTime(scope.row.expireTime) : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="引用数" align="center" prop="referenceCount" width="90">
          <template #default="scope">
            <el-tooltip :disabled="!scope.row.references?.length" :content="referenceSummary(scope.row)">
              <span>{{ scope.row.referenceCount ?? 0 }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="下载" placement="top">
              <el-button
                v-hasPermi="['system:oss:download']"
                aria-label="下载"
                link
                type="primary"
                icon="Download"
                @click="handleDownload(scope.row)"
              ></el-button>
            </el-tooltip>
            <el-tooltip v-if="rowMutation(scope.row) === 'delete'" content="删除" placement="top">
              <el-button
                v-hasPermi="['system:oss:remove']"
                aria-label="删除"
                link
                type="primary"
                icon="Delete"
                @click="handleDelete(scope.row)"
              ></el-button>
            </el-tooltip>
            <el-tooltip v-if="canPublish(scope.row)" content="复制到公开配置" placement="top">
              <el-button
                v-hasPermi="['system:oss:publish']"
                aria-label="公开"
                link
                type="primary"
                icon="Share"
                @click="openPublish(scope.row)"
              ></el-button>
            </el-tooltip>
            <el-tooltip v-if="scope.row.restorable" content="用工单恢复为私有，不删除公开桶副本" placement="top">
              <el-button
                v-hasPermi="['system:oss:publish']"
                aria-label="恢复私有"
                link
                type="primary"
                icon="Lock"
                @click="handleUnpublish(scope.row)"
              ></el-button>
            </el-tooltip>
            <el-tooltip v-if="rowMutation(scope.row) === 'restore'" content="恢复" placement="top">
              <el-button
                v-hasPermi="['system:oss:remove']"
                aria-label="恢复"
                link
                type="primary"
                icon="RefreshLeft"
                @click="handleRestore(scope.row)"
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
    <!-- 添加或修改OSS对象存储对话框 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="500px" append-to-body destroy-on-close>
      <el-form ref="ossFormRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="文件名">
          <fileUpload v-if="type === 0" v-model="form.file" @busy="uploadBusy = $event" />
          <imageUpload v-if="type === 1" v-model="form.file" @busy="uploadBusy = $event" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button :loading="buttonLoading || uploadBusy" :disabled="uploadBusy" type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
    <el-dialog v-model="publishVisible" title="公开文件" width="480px" append-to-body destroy-on-close>
      <p>将这一条记录复制到所选公开只读配置。文件编号不变，私有桶里的原件先保留。</p>
      <el-select v-model="publishTarget" placeholder="选择公开配置" style="width: 100%">
        <el-option
          v-for="config in publicConfigs"
          :key="String(config.ossConfigId)"
          :label="`${config.configKey} / ${config.bucketName}`"
          :value="config.configKey"
        />
      </el-select>
      <p v-if="publicConfigs.length === 0">没有可选的公开只读配置。请先在 OSS 配置里新增一张不能设为默认的 PUBLIC_READ 配置。</p>
      <template #footer>
        <el-button @click="publishVisible = false">取 消</el-button>
        <el-button type="primary" :loading="buttonLoading" :disabled="!publishTarget" @click="submitPublish">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Oss" lang="ts">
import type { OssConfigVO, OssForm, OssQuery, OssVO } from '@namewta/domain-system';
import type { FormInstance as ElFormInstance } from 'element-plus';
import { onMounted, reactive, ref, toRefs } from 'vue';
import { useRouter } from 'vue-router';
import type { SystemWebRuntime } from '../runtime';
import {
  useDateRangeQuery,
  useFormDialog,
  useLoading,
  useSearchReset,
  useSearchToggle,
  useTableSelection
} from '../composables';
import { parseTime } from '../utils';
import {
  OSS_DELETED_MESSAGE,
  ossDeleteConfirmMessage,
  ossDeleteTargets,
  ossFilePresentation,
  ossRowMutation,
  type OssFilePresentation
} from './presentation';

const { runtime } = defineProps<{ runtime: SystemWebRuntime }>();
const ImagePreview = runtime.imagePreview;
const { byKey: getConfigKey, updateByKey: updateConfigByKey } = runtime.service.resources.configs;
const { list: listOss, delete: delOss, restore: restoreOss, downloadUrl, publish: publishOss, unpublish: unpublishOss } = runtime.service.resources.oss;
const { list: listOssConfigs } = runtime.service.resources.ossConfigs;
const download = { oss: runtime.downloadOss };
const modal = { confirm: runtime.confirm, msgSuccess: runtime.success, msgError: runtime.error };
const deletedMessage = OSS_DELETED_MESSAGE;
const router = useRouter();

const referenceSummary = (oss: any) =>
  oss.references?.map(reference => `${reference.refType}:${reference.refId}`).join(', ') || '无业务引用';

const ossList = ref<OssVO[]>([]);
const showTable = ref(true);
const buttonLoading = ref(false);
const uploadBusy = ref(false);
const { loading, setLoading, withLoading } = useLoading(true);
const { showSearch } = useSearchToggle();
const total = ref(0);
const type = ref(0);
const previewListResource = ref(true);
const {
  dateRange: dateRangeCreateTime,
  applyDateRange: applyCreateTimeDateRange,
  resetDateRange: resetCreateTimeDateRange
} = useDateRangeQuery('CreateTime');

// 默认排序
const defaultSort = ref({ prop: 'createTime', order: 'ascending' });

const ossFormRef = ref<ElFormInstance>();
const queryFormRef = ref<ElFormInstance>();

const initFormData = {
  file: undefined
};
const data = reactive<PageData<OssForm, OssQuery>>({
  form: { ...initFormData },
  // 查询参数
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    fileName: '',
    originalName: '',
    fileSuffix: '',
    createTime: '',
    service: '',
    orderByColumn: defaultSort.value.prop,
    isAsc: defaultSort.value.order
  },
  rules: {
    file: [{ required: true, message: '文件不能为空', trigger: 'blur' }]
  }
});

const { queryParams, form, rules } = toRefs(data);
const selectedRows = ref<OssVO[]>([]);
const { multiple, handleSelectionChange: syncSelection } = useTableSelection<OssVO>(item => item.ossId);
const handleSelectionChange = (selection: OssVO[]) => {
  selectedRows.value = selection;
  syncSelection(selection);
};
const previewUrls = ref<Record<string, string>>({});
const deletedPreviewIds = ref<Record<string, true>>({});
let listGeneration = 0;
const {
  dialog,
  resetForm: reset,
  openDialog,
  closeDialog
} = useFormDialog({
  form,
  formRef: ossFormRef,
  initialFormData: initFormData
});

const rowKey = (row: Partial<OssVO>) => String(row.ossId ?? '');
const filePresentation = (row: OssVO): OssFilePresentation =>
  deletedPreviewIds.value[rowKey(row)] ? 'deleted' : ossFilePresentation(row, previewListResource.value);
const rowMutation = (row: OssVO) => ossRowMutation(row);
const canPublish = (row: OssVO) => row.deleteState === 'ACTIVE' && row.accessPolicy === 'PRIVATE';
const publishVisible = ref(false);
const publishTarget = ref('');
const publishRow = ref<OssVO | null>(null);
const publicConfigs = ref<OssConfigVO[]>([]);
const previewUrl = (row: OssVO) => previewUrls.value[rowKey(row)] || '';

/** 查询OSS对象存储列表 */
const getList = async () => {
  const generation = ++listGeneration;
  previewUrls.value = {};
  deletedPreviewIds.value = {};
  await withLoading(async () => {
    const res = await getConfigKey('sys.oss.previewListResource');
    previewListResource.value = res?.data === undefined ? true : res.data === 'true';
    const response = await listOss(applyCreateTimeDateRange(queryParams.value));
    ossList.value = response.data?.rows ?? [];
    total.value = response.data?.total ?? 0;
    showTable.value = true;
  });
  if (generation !== listGeneration) return;
  await fillPreviewUrls(generation);
};

// 管理列表不带可访问地址。待删除对象不能申请下载地址；活动图片只使用本次查询的短时授权。
const fillPreviewUrls = async (generation: number) => {
  if (!previewListResource.value) return;
  const images = ossList.value.filter(row => ossFilePresentation(row, true) === 'image');
  const resolved = await Promise.all(
    images.map(async row => {
      try {
        const response = await downloadUrl(row.ossId);
        return { id: rowKey(row), url: response.data?.url ?? '', deleted: false };
      } catch (error) {
        const message = error instanceof Error ? error.message : '';
        return { id: rowKey(row), url: '', deleted: message === deletedMessage };
      }
    })
  );
  if (generation !== listGeneration) return;
  const urls: Record<string, string> = {};
  const deleted: Record<string, true> = {};
  for (const item of resolved) {
    if (item.deleted) deleted[item.id] = true;
    else if (item.url) urls[item.id] = item.url;
  }
  previewUrls.value = urls;
  deletedPreviewIds.value = deleted;
};
/** 取消按钮 */
function cancel() {
  reset();
  closeDialog();
}
/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}
const { resetQuery } = useSearchReset({
  queryFormRef,
  queryParams,
  pageNumKey: 'pageNum',
  resetExtras: () => {
    showTable.value = false;
    resetCreateTimeDateRange();
    queryParams.value.orderByColumn = defaultSort.value.prop;
    queryParams.value.isAsc = defaultSort.value.order;
  },
  afterReset: () => {
    handleQuery();
  }
});
/** 设置列的排序为我们自定义的排序 */
const handleHeaderClass = ({ column }: any): any => {
  column.order = column.multiOrder;
};
/** 点击表头进行排序 */
const handleHeaderCLick = (column: any) => {
  if (column.sortable !== 'custom') {
    return;
  }
  switch (column.multiOrder) {
    case 'descending':
      column.multiOrder = 'ascending';
      break;
    case 'ascending':
      column.multiOrder = '';
      break;
    default:
      column.multiOrder = 'descending';
      break;
  }
  handleOrderChange(column.property, column.multiOrder);
};
const handleOrderChange = (prop: string, order: string) => {
  const orderByArr = queryParams.value.orderByColumn ? queryParams.value.orderByColumn.split(',') : [];
  const isAscArr = queryParams.value.isAsc ? queryParams.value.isAsc.split(',') : [];
  const propIndex = orderByArr.indexOf(prop);
  if (propIndex !== -1) {
    if (order) {
      //排序里已存在 只修改排序
      isAscArr[propIndex] = order;
    } else {
      //如果order为null 则删除排序字段和属性
      isAscArr.splice(propIndex, 1); //删除排序
      orderByArr.splice(propIndex, 1); //删除属性
    }
  } else {
    //排序里不存在则新增排序
    orderByArr.push(prop);
    isAscArr.push(order);
  }
  //合并排序
  queryParams.value.orderByColumn = orderByArr.join(',');
  queryParams.value.isAsc = isAscArr.join(',');
  getList();
};
/** 任务日志列表查询 */
const handleOssConfig = () => {
  router.push('/system/oss-config/index');
};
/** 文件按钮操作 */
const handleFile = () => {
  type.value = 0;
  openDialog('上传文件');
};
/** 图片按钮操作 */
const handleImage = () => {
  type.value = 1;
  openDialog('上传图片');
};
/** 提交按钮 */
const submitForm = () => {
  if (uploadBusy.value) return;
  closeDialog();
  getList();
};
/** 下载按钮操作 */
const handleDownload = (row: Partial<OssVO>) => {
  if (row.deleteState === 'PENDING') {
    modal.msgError(deletedMessage);
    return;
  }
  download.oss(row.ossId);
};
/** 预览开关按钮  */
const handlePreviewListResource = async (preview: boolean) => {
  try {
    await updateConfigByKey('sys.oss.previewListResource', preview);
    await getList();
    modal.msgSuccess((preview ? '启用' : '停用') + '成功');
  } catch {
    return;
  }
};
/** 删除按钮操作 */
const handleDelete = async (row?: Partial<OssVO>) => {
  const targets = row?.ossId ? [row] : selectedRows.value;
  const { pending, removable } = ossDeleteTargets(targets);
  if (removable.length === 0) {
    modal.msgError(row?.ossId ? deletedMessage : '所选文件已处于待删除，请使用行内恢复');
    return;
  }
  const ossIds = removable.map(item => item.ossId).filter(id => id !== undefined);
  await modal.confirm(ossDeleteConfirmMessage(ossIds, pending.length));
  setLoading(true);
  await delOss(ossIds).finally(() => setLoading(false));
  await getList();
  modal.msgSuccess('删除成功');
};

const openPublish = async (row: OssVO) => {
  publishRow.value = row;
  publishTarget.value = '';
  const response = await listOssConfigs({ pageNum: 1, pageSize: 200, configKey: '', bucketName: '', status: '' });
  publicConfigs.value = (response.data?.rows ?? []).filter(config =>
    config.accessPolicy === 'PUBLIC_READ' && config.configKey !== row.service);
  publishVisible.value = true;
};

const submitPublish = async () => {
  if (!publishRow.value || !publishTarget.value) return;
  buttonLoading.value = true;
  try {
    await publishOss(publishRow.value.ossId, publishTarget.value);
    publishVisible.value = false;
    modal.msgSuccess('已复制到公开配置');
    await getList();
  } catch (error) {
    modal.msgError(error instanceof Error ? error.message : '公开失败');
  } finally {
    buttonLoading.value = false;
  }
};

const handleUnpublish = async (row: OssVO) => {
  await modal.confirm('恢复后这一条记录重新指向原来的私有配置。公开桶里已复制的文件不会删除。');
  setLoading(true);
  try {
    await unpublishOss(row.ossId);
    modal.msgSuccess('已恢复为私有');
    await getList();
  } catch (error) {
    modal.msgError(error instanceof Error ? error.message : '恢复私有失败');
  } finally {
    setLoading(false);
  }
};

/** 恢复待删除对象，生命周期按当前引用重算。 */
const handleRestore = async (row: OssVO) => {
  await modal.confirm('是否确认恢复OSS对象存储编号为"' + row.ossId + '"的数据项?');
  setLoading(true);
  await restoreOss(row.ossId).finally(() => setLoading(false));
  await getList();
  modal.msgSuccess('恢复成功');
};

onMounted(() => {
  getList();
});
</script>

<style lang="scss" scoped>
.system-oss-page {
  min-height: 0;
}

.data-table :deep(.el-button.is-link) {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: rgba(53, 109, 255, 0.08);
}

@media (max-width: 900px) {
  .toolbar-shell {
    align-items: flex-start;
  }
}
</style>
