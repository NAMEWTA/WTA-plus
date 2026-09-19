<template>
  <el-dialog v-model="visible" title="流程办理" :width="width" :close-on-click-modal="false">
    <el-alert v-if="failure" :title="failure" type="error" show-icon :closable="false" />
    <div v-loading="loading">
      <el-descriptions v-if="task" :column="2" border>
        <el-descriptions-item label="流程名称">{{ task.flowName }}</el-descriptions-item>
        <el-descriptions-item label="任务节点">{{ task.nodeName }}</el-descriptions-item>
        <el-descriptions-item label="节点编码">{{ task.nodeCode }}</el-descriptions-item>
        <el-descriptions-item label="流程实例 ID">{{ task.instanceId }}</el-descriptions-item>
        <el-descriptions-item label="业务 ID">{{ task.businessId }}</el-descriptions-item>
        <el-descriptions-item label="版本号">{{ task.version }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ task.createTime }}</el-descriptions-item>
      </el-descriptions>
      <el-form v-if="task" :key="generation" label-width="110px" :disabled="submitting">
        <el-form-item label="消息提醒">
          <el-checkbox-group v-model="messageType">
            <el-checkbox value="1" disabled>站内信</el-checkbox>
            <el-checkbox value="2">邮件</el-checkbox>
            <el-checkbox value="3">短信</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item v-if="enabled.has('file')" label="附件">
          <FileUpload
            v-model="fileId"
            :file-type="['png', 'jpg', 'jpeg', 'doc', 'docx', 'xlsx', 'xls', 'ppt', 'txt', 'pdf']"
            :file-size="20"
          />
        </el-form-item>
        <el-form-item v-if="enabled.has('copy')" label="抄送人">
          <el-tag v-for="user in copyUsers" :key="String(user.userId)" closable @close="removeCopy(user.userId)">
            {{ user.nickName }}
          </el-tag>
          <el-button @click="openSelector('copy', true)">选择抄送人</el-button>
        </el-form-item>
        <el-form-item
          v-for="node in enabled.has('pop') ? nextNodes : []"
          :key="node.nodeCode"
          :label="String(node.nodeName ?? '下一节点')"
        >
          <el-input :model-value="assigneeNames[node.nodeCode]" readonly>
            <template #append>
              <el-button :disabled="!node.permissionFlag" @click="selectNode(node)">选择</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="审批意见"><el-input v-model="message" type="textarea" :rows="3" /></el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        v-if="allowComplete && task?.flowStatus === 'waiting'"
        type="primary"
        :loading="submitting"
        :disabled="loading || submitting"
        @click="complete"
      >
        提交
      </el-button>
      <el-button v-if="can('trust')" :loading="submitting"
        :disabled="loading || submitting" @click="openSelector('delegateTask', false)">委托</el-button>
      <el-button v-if="canAction('transfer')" :loading="submitting"
        :disabled="loading || submitting" @click="openSelector('transferTask', false)">
        转办
      </el-button>
      <el-button v-if="canRatio('addSign')" :loading="submitting"
        :disabled="loading || submitting" @click="openSelector('addSignature', true)">
        加签
      </el-button>
      <el-button v-if="canRatio('subSign')" :loading="submitting"
        :disabled="loading || submitting" @click="openReduction">减签</el-button>
      <el-button v-if="can('back')" type="danger" :loading="submitting"
        :disabled="loading || submitting" @click="openBack">退回</el-button>
      <el-button v-if="canAction('termination')" type="danger" :loading="submitting"
        :disabled="loading || submitting" @click="terminate">终止</el-button>
    </template>
  </el-dialog>

  <UserSelect
    v-if="visible && task"
    :key="generation"
    ref="selector"
    :service="runtime.service"
    :multiple="selectorMultiple"
    :data="selectorData"
    :user-ids="selectorUserIds"
    @confirm="handleUsers"
  />

  <el-dialog v-if="visible && task" :key="`reduction-${generation}`" v-model="reductionVisible" title="选择减签人员" width="560px" append-to-body>
    <el-table :data="currentUsers" row-key="userId">
      <el-table-column prop="nickName" label="办理人" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="danger" @click="reduce(scope.row)">减签</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <el-dialog v-if="visible && task" :key="`back-${generation}`" v-model="backVisible" title="退回任务" width="560px" append-to-body>
    <el-alert v-if="backFailure" :title="backFailure" type="error" show-icon :closable="false" />
    <el-select v-model="backNodeCode" placeholder="请选择退回节点">
      <el-option v-for="node in backNodes" :key="node.nodeCode" :label="node.nodeName" :value="node.nodeCode" />
    </el-select>
    <el-input v-model="backMessage" type="textarea" :rows="3" placeholder="请输入退回意见" />
    <el-form-item label="附件">
      <FileUpload
        v-model="backFileId"
        :file-type="['png', 'jpg', 'jpeg', 'doc', 'docx', 'xlsx', 'xls', 'ppt', 'txt', 'pdf']"
        :file-size="20"
      />
    </el-form-item>
    <template #footer>
      <el-button type="primary" :loading="submitting"
        :disabled="loading || submitting" @click="back">确认退回</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { WorkflowWebRuntime } from '../runtime';
import { useProcessActionDialog } from './useProcessActionDialog';
import UserSelect from './UserSelect.vue';

const props = withDefaults(
  defineProps<{
    allowComplete?: boolean;
    mode?: 'intervention' | 'participant';
    runtime: WorkflowWebRuntime;
    taskVariables?: Readonly<Record<string, unknown>>;
    width?: string;
  }>(),
  { allowComplete: true, mode: 'participant', taskVariables: () => ({}), width: '720px' }
);
const emit = defineEmits<{ cancelled: []; completed: [] }>();
const FileUpload = props.runtime.fileUpload;
const { generation, visible, loading, submitting, failure, task, message, messageType, fileId, nextNodes, copyUsers, assigneeNames, enabled, selector, selectorMultiple, selectorData, selectorUserIds, reductionVisible, currentUsers, backVisible, backNodes, backNodeCode, backMessage, backFileId, backFailure, can, canAction, canRatio, open, openSelector, selectNode, handleUsers, removeCopy, complete, openReduction, reduce, terminate, openBack, back, close } = useProcessActionDialog(props, event => {
  if (event === 'completed') emit('completed');
  else emit('cancelled');
});
defineExpose({ open, close });
</script>
