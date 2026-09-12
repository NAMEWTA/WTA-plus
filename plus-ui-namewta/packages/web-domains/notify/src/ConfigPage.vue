<template>
  <div class="p-2">
    <el-card>
      <template #header>
        <div class="flex justify-between">
          <span>通知配置</span>
          <el-button
            v-if="runtime.hasPermission('notify:config:add')"
            type="primary"
            @click="openAccount()"
          >
            新增账号
          </el-button>
        </div>
      </template>
      <el-tabs v-model="channel" @tab-change="reload">
        <el-tab-pane label="邮件" name="MAIL" />
        <el-tab-pane label="短信" name="SMS" />
      </el-tabs>
      <el-table v-loading="loading" :data="accounts" border>
        <el-table-column prop="configKey" label="配置标识" min-width="140" />
        <el-table-column v-if="channel === 'MAIL'" prop="mailFrom" label="发件人" min-width="180" />
        <el-table-column v-if="channel === 'SMS'" prop="supplier" label="厂商" min-width="120" />
        <el-table-column prop="minuteMax" label="每分钟上限" width="120" />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 'Y' ? 'success' : 'info'">{{ row.enabled === 'Y' ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="密钥" width="90">
          <template #default="{ row }">
            {{ channel === 'MAIL' ? (row.mailPassSet ? '已设置' : '未设置') : row.accessKeySecretSet ? '已设置' : '未设置' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="runtime.hasPermission('notify:config:edit')"
              link
              type="primary"
              @click="openAccount(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="runtime.hasPermission('notify:config:edit')"
              link
              type="warning"
              @click="toggleAccount(row)"
            >
              {{ row.enabled === 'Y' ? '停用' : '启用' }}
            </el-button>
            <el-button
              v-if="runtime.hasPermission('notify:config:test')"
              link
              type="success"
              @click="openTest('account', row)"
            >
              试发
            </el-button>
            <el-button
              v-if="runtime.hasPermission('notify:config:remove')"
              link
              type="danger"
              @click="removeAccount(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="accountTotal > 0"
        v-model:page="pageNum"
        v-model:limit="pageSize"
        :total="accountTotal"
        @pagination="loadAccounts"
      />
    </el-card>

    <el-card class="mt-2">
      <template #header>
        <span>场景绑定（{{ channel === 'MAIL' ? '邮件文案' : '短信模板' }}）</span>
      </template>
      <el-table v-loading="sceneLoading" :data="scenes" border>
        <el-table-column prop="title" label="场景" min-width="140" />
        <el-table-column prop="sceneCode" label="编码" min-width="160" />
        <el-table-column label="绑定账号" min-width="140">
          <template #default="{ row }">{{ row.accountConfigKey || '未绑定' }}</template>
        </el-table-column>
        <el-table-column v-if="channel === 'MAIL'" prop="mailSubject" label="邮件主题" min-width="180" />
        <el-table-column v-if="channel === 'SMS'" prop="smsTemplateCode" label="供应商模板码" min-width="160" />
        <el-table-column label="变量" min-width="180">
          <template #default="{ row }">
            {{ row.variables.map(item => '${' + item.name + '}').join(' ') || '无' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="runtime.hasPermission('notify:config:edit')"
              link
              type="primary"
              @click="openScene(row)"
            >
              绑定
            </el-button>
            <el-button
              v-if="runtime.hasPermission('notify:config:test')"
              link
              type="success"
              @click="openTest('template', row)"
            >
              试发
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="accountVisible" :title="accountForm.accountId ? '编辑账号' : '新增账号'" width="640px">
      <el-form :model="accountForm" label-width="120px">
        <el-form-item label="配置标识" required>
          <el-input v-model="accountForm.configKey" :disabled="Boolean(accountForm.accountId)" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="accountForm.enabled" active-value="Y" inactive-value="N" />
        </el-form-item>
        <el-form-item label="每分钟上限" required>
          <el-input-number v-model="accountForm.minuteMax" :min="1" />
        </el-form-item>
        <template v-if="channel === 'MAIL'">
          <el-form-item label="SMTP 主机"><el-input v-model="accountForm.host" /></el-form-item>
          <el-form-item label="端口"><el-input-number v-model="accountForm.port" :min="1" /></el-form-item>
          <el-form-item label="发件人"><el-input v-model="accountForm.mailFrom" /></el-form-item>
          <el-form-item label="用户名"><el-input v-model="accountForm.mailUser" /></el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="accountForm.mailPass"
              type="password"
              show-password
              :placeholder="accountForm.mailPassSet ? '留空保持原密码' : '请输入密码'"
            />
          </el-form-item>
          <el-form-item label="SSL">
            <el-switch v-model="accountForm.sslEnable" active-value="Y" inactive-value="N" />
          </el-form-item>
          <el-form-item label="STARTTLS">
            <el-switch v-model="accountForm.starttlsEnable" active-value="Y" inactive-value="N" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="厂商"><el-input v-model="accountForm.supplier" placeholder="alibaba / tencent" /></el-form-item>
          <el-form-item label="AccessKey"><el-input v-model="accountForm.accessKeyId" /></el-form-item>
          <el-form-item label="密钥">
            <el-input
              v-model="accountForm.accessKeySecret"
              type="password"
              show-password
              :placeholder="accountForm.accessKeySecretSet ? '留空保持原密钥' : '请输入密钥'"
            />
          </el-form-item>
          <el-form-item label="签名"><el-input v-model="accountForm.signature" /></el-form-item>
          <el-form-item label="应用 ID"><el-input v-model="accountForm.sdkAppId" /></el-form-item>
        </template>
        <el-form-item label="备注"><el-input v-model="accountForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAccount">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sceneVisible" title="场景绑定" width="720px">
      <el-form v-if="sceneForm" :model="sceneForm" label-width="140px">
        <el-form-item label="场景">{{ sceneForm.title }}（{{ sceneForm.sceneCode }}）</el-form-item>
        <el-form-item label="渠道账号">
          <el-select v-model="sceneForm.accountId" clearable placeholder="未绑定则该渠道失败关闭" style="width: 100%">
            <el-option
              v-for="item in enabledAccounts"
              :key="String(item.accountId)"
              :label="item.configKey"
              :value="item.accountId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="声明变量">
          <el-space wrap>
            <el-tag v-for="item in sceneForm.variables" :key="item.name">
              ${{ item.name }}{{ item.required ? ' *' : '' }}
            </el-tag>
            <span v-if="!sceneForm.variables.length">无</span>
          </el-space>
        </el-form-item>
        <template v-if="channel === 'MAIL'">
          <el-form-item label="插入变量">
            <el-button
              v-for="item in sceneForm.variables"
              :key="item.name"
              size="small"
              @click="insertToken(item.name)"
            >
              ${{ item.name }}
            </el-button>
          </el-form-item>
          <el-form-item label="邮件主题">
            <el-input v-model="sceneForm.mailSubject" @focus="tokenTarget = 'subject'" />
          </el-form-item>
          <el-form-item label="邮件正文">
            <el-input v-model="sceneForm.mailBody" type="textarea" :rows="6" @focus="tokenTarget = 'body'" />
          </el-form-item>
          <el-form-item label="已占用变量">{{ occupiedTokens || '无' }}</el-form-item>
        </template>
        <template v-else>
          <el-form-item label="供应商模板码">
            <el-input v-model="sceneForm.smsTemplateCode" placeholder="厂商控制台模板码，禁止自由正文" />
          </el-form-item>
          <el-form-item v-for="item in sceneForm.variables" :key="item.name" :label="item.name">
            <el-input
              v-model="sceneForm.smsParamMapping![item.name]"
              :placeholder="'映射到供应商参数名，样例 ' + (item.example || item.name)"
            />
          </el-form-item>
        </template>
        <el-form-item label="模板每分钟上限">
          <el-input-number v-model="sceneForm.templateMinuteMax" :min="1" />
        </el-form-item>
        <el-form-item label="收件人拦截">
          <el-switch v-model="sceneForm.restricted" active-value="Y" inactive-value="N" />
        </el-form-item>
        <el-form-item label="每分钟/每天">
          <el-input-number v-model="sceneForm.recipientMinuteMax" :min="0" />
          <el-input-number v-model="sceneForm.recipientDayMax" :min="0" class="ml-2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sceneVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveScene">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="testVisible" title="测试发送" width="480px">
      <el-form label-width="100px">
        <el-form-item :label="channel === 'MAIL' ? '邮箱' : '手机号'">
          <el-input v-model="testTarget" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitTest">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { NotifyChannelAccount, NotifyConfigChannel, NotifySceneBinding } from '@namewta/domain-notify';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import type { NotifyWebRuntime } from './runtime';

const { runtime } = defineProps<{ runtime: NotifyWebRuntime }>();
const channel = ref<NotifyConfigChannel>('MAIL');
const accounts = ref<NotifyChannelAccount[]>([]);
const scenes = ref<NotifySceneBinding[]>([]);
const accountTotal = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const sceneLoading = ref(false);
const saving = ref(false);
const accountVisible = ref(false);
const sceneVisible = ref(false);
const testVisible = ref(false);
const tokenTarget = ref<'subject' | 'body'>('subject');
const testKind = ref<'account' | 'template'>('account');
const testTarget = ref('');
const testAccountId = ref<string | number | undefined>();
const testSceneCode = ref('');
const accountForm = ref<NotifyChannelAccount>(emptyAccount());
const sceneForm = ref<NotifySceneBinding | null>(null);

const enabledAccounts = computed(() => accounts.value.filter(item => item.enabled === 'Y'));
const occupiedTokens = computed(() => {
  const text = `${sceneForm.value?.mailSubject ?? ''}${sceneForm.value?.mailBody ?? ''}`;
  const names = [...text.matchAll(/\$\{([A-Za-z][A-Za-z0-9_]*)}/g)].map(item => item[1]);
  return [...new Set(names)].map(name => '${' + name + '}').join(' ');
});

function emptyAccount(): NotifyChannelAccount {
  return {
    channel: channel.value,
    configKey: '',
    enabled: 'N',
    minuteMax: 60,
    sslEnable: 'N',
    starttlsEnable: 'N',
    mailPass: '',
    accessKeySecret: ''
  };
}

function showError(error: unknown, fallback: string) {
  ElMessage.error(error instanceof Error ? error.message : fallback);
}

async function loadAccounts() {
  loading.value = true;
  try {
    const result = await runtime.service.config.accounts(channel.value, pageNum.value, pageSize.value);
    accounts.value = result.data?.rows ?? [];
    accountTotal.value = result.data?.total ?? 0;
  } catch (error) {
    showError(error, '加载渠道账号失败');
  } finally {
    loading.value = false;
  }
}

async function loadScenes() {
  sceneLoading.value = true;
  try {
    const result = await runtime.service.config.scenes(channel.value);
    scenes.value = result.data ?? [];
  } catch (error) {
    showError(error, '加载场景绑定失败');
  } finally {
    sceneLoading.value = false;
  }
}

async function reload() {
  pageNum.value = 1;
  await Promise.all([loadAccounts(), loadScenes()]);
}

function openAccount(row?: NotifyChannelAccount) {
  accountForm.value = row
    ? { ...row, channel: channel.value, mailPass: '', accessKeySecret: '' }
    : emptyAccount();
  accountVisible.value = true;
}

async function saveAccount() {
  saving.value = true;
  try {
    const payload = { ...accountForm.value, channel: channel.value };
    if (payload.accountId) {
      await runtime.service.config.editAccount(payload);
    } else {
      await runtime.service.config.addAccount(payload);
    }
    accountVisible.value = false;
    ElMessage.success('保存成功');
    await reload();
  } catch (error) {
    showError(error, '保存账号失败');
  } finally {
    saving.value = false;
  }
}

async function toggleAccount(row: NotifyChannelAccount) {
  if (!row.accountId) return;
  try {
    await runtime.service.config.changeStatus(row.accountId, row.enabled === 'Y' ? 'N' : 'Y');
    await loadAccounts();
  } catch (error) {
    showError(error, '启停失败');
  }
}

async function removeAccount(row: NotifyChannelAccount) {
  if (!row.accountId) return;
  await ElMessageBox.confirm('确认删除该渠道账号？', '提示');
  try {
    await runtime.service.config.removeAccount(row.accountId);
    await reload();
  } catch (error) {
    showError(error, '删除失败');
  }
}

function openScene(row: NotifySceneBinding) {
  sceneForm.value = {
    ...row,
    smsParamMapping: { ...(row.smsParamMapping ?? {}) },
    templateMinuteMax: row.templateMinuteMax ?? 60,
    restricted: row.restricted ?? 'N',
    recipientMinuteMax: row.recipientMinuteMax ?? 0,
    recipientDayMax: row.recipientDayMax ?? 0
  };
  tokenTarget.value = 'subject';
  sceneVisible.value = true;
}

function insertToken(name: string) {
  if (!sceneForm.value) return;
  const token = '${' + name + '}';
  if (tokenTarget.value === 'subject') {
    sceneForm.value.mailSubject = `${sceneForm.value.mailSubject ?? ''}${token}`;
  } else {
    sceneForm.value.mailBody = `${sceneForm.value.mailBody ?? ''}${token}`;
  }
}

async function saveScene() {
  if (!sceneForm.value) return;
  saving.value = true;
  try {
    await runtime.service.config.saveScene({
      sceneCode: sceneForm.value.sceneCode,
      channel: channel.value,
      accountId: sceneForm.value.accountId ?? undefined,
      mailSubject: sceneForm.value.mailSubject,
      mailBody: sceneForm.value.mailBody,
      smsTemplateCode: sceneForm.value.smsTemplateCode,
      smsParamMapping: sceneForm.value.smsParamMapping,
      templateMinuteMax: sceneForm.value.templateMinuteMax,
      restricted: sceneForm.value.restricted,
      recipientMinuteMax: sceneForm.value.recipientMinuteMax,
      recipientDayMax: sceneForm.value.recipientDayMax
    });
    sceneVisible.value = false;
    ElMessage.success('绑定已保存');
    await loadScenes();
  } catch (error) {
    showError(error, '保存绑定失败');
  } finally {
    saving.value = false;
  }
}

function openTest(kind: 'account' | 'template', row: NotifyChannelAccount | NotifySceneBinding) {
  testKind.value = kind;
  testTarget.value = '';
  if (kind === 'account') {
    testAccountId.value = (row as NotifyChannelAccount).accountId;
    testSceneCode.value = '';
  } else {
    testSceneCode.value = (row as NotifySceneBinding).sceneCode;
    testAccountId.value = undefined;
  }
  testVisible.value = true;
}

async function submitTest() {
  saving.value = true;
  try {
    const result =
      testKind.value === 'account'
        ? await runtime.service.config.testAccount({
            accountId: testAccountId.value as string | number,
            target: testTarget.value
          })
        : await runtime.service.config.testTemplate({
            sceneCode: testSceneCode.value,
            channel: channel.value,
            target: testTarget.value
          });
    ElMessage.success(`测试提交：${result.data ?? 'OK'}`);
    testVisible.value = false;
  } catch (error) {
    showError(error, '测试发送失败');
  } finally {
    saving.value = false;
  }
}

onMounted(reload);
</script>
