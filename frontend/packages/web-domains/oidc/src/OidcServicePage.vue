<template>
  <section class="app-container oidc-service" v-loading="loading">
    <el-card>
      <template #header>
        <h2>统一登录服务</h2>
        <p>管理本服务对外提供的 OIDC 参数、密钥和退出通知。</p>
      </template>
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-button v-if="error" @click="load">重新加载</el-button>
      <el-tabs v-model="tab">
        <el-tab-pane v-if="runtime.hasPermission('oidc:service:query')" label="服务配置" name="settings">
          <template v-if="configuration && draft">
            <el-alert
              v-if="configuration.restartRequired"
              title="配置已保存，需要重启后生效"
              type="warning"
              :closable="false"
            />
            <el-descriptions :column="2" border>
              <el-descriptions-item label="当前生效 Issuer">
                {{ configuration.active.issuer || '尚未配置' }}
              </el-descriptions-item>
              <el-descriptions-item label="当前运行状态">
                {{ configuration.active.enabled ? '启用' : '停用' }}
              </el-descriptions-item>
              <el-descriptions-item label="签名密钥">
                {{ configuration.signingKeyReady ? '已就绪' : '未配置' }}
              </el-descriptions-item>
              <el-descriptions-item label="状态加密密钥">
                {{ configuration.stateKeyReady ? '已就绪' : '未配置' }}
              </el-descriptions-item>
            </el-descriptions>
            <el-form
              label-position="top"
              :disabled="!runtime.hasPermission('oidc:service:edit') || busy"
              @submit.prevent="save"
            >
              <div class="settings-grid">
                <el-form-item label="启用 OIDC"><el-switch v-model="draft.enabled" /></el-form-item>
                <el-form-item label="允许 HTTP"><el-switch v-model="draft.allowHttp" /></el-form-item>
                <el-form-item label="Issuer">
                  <el-input v-model="draft.issuer" placeholder="https://sso.example.com" />
                </el-form-item>
                <el-form-item label="登录页面地址">
                  <el-input v-model="draft.ssoWebUrl" placeholder="https://sso.example.com/" />
                </el-form-item>
                <el-form-item label="授权码有效期（秒）">
                  <el-input-number v-model="draft.codeTtlSeconds" :min="1" />
                </el-form-item>
                <el-form-item label="访问令牌有效期（秒）">
                  <el-input-number v-model="draft.accessTtlSeconds" :min="1" />
                </el-form-item>
                <el-form-item label="登录交互有效期（秒）">
                  <el-input-number v-model="draft.interactionTtlSeconds" :min="1" />
                </el-form-item>
              </div>
              <h3>中央登录会话</h3>
              <div class="settings-grid">
                <el-form-item label="启用中央登录"><el-switch v-model="draft.sso.enabled" /></el-form-item>
                <el-form-item label="仅 HTTPS Cookie"><el-switch v-model="draft.sso.cookieSecure" /></el-form-item>
                <el-form-item label="登录页 Origin">
                  <el-input v-model="draft.sso.webOrigin" placeholder="https://sso.example.com" />
                </el-form-item>
                <el-form-item label="登录页部署路径">
                  <el-input v-model="draft.sso.webBasePath" placeholder="/" />
                </el-form-item>
                <el-form-item label="会话 Cookie 名称"><el-input v-model="draft.sso.cookieName" /></el-form-item>
                <el-form-item label="会话有效期（秒）">
                  <el-input-number v-model="draft.sso.sessionTtlSeconds" :min="1" />
                </el-form-item>
                <el-form-item label="登录码有效期（秒）">
                  <el-input-number v-model="draft.sso.codeTtlSeconds" :min="1" />
                </el-form-item>
              </div>
              <p>
                更改 Issuer、登录地址或 Cookie
                设置前，请先关闭两项启用开关并保存，执行维护退出，待退出通知完成后保存新设置并重启。
              </p>
              <el-button
                v-if="runtime.hasPermission('oidc:service:edit')"
                native-type="submit"
                type="primary"
                :loading="busy"
              >
                保存配置
              </el-button>
              <el-button
                v-if="runtime.hasPermission('oidc:service:edit')"
                :disabled="configuration.active.enabled || configuration.active.sso.enabled"
                @click="prepareRestart"
              >
                执行维护退出
              </el-button>
            </el-form>
          </template>
        </el-tab-pane>
        <el-tab-pane v-if="runtime.hasPermission('oidc:key:manage')" label="密钥管理" name="keys">
          <p>生成或导入后激活新密钥；旧密钥保留用于验证已有会话。私钥不回显。</p>
          <el-space wrap>
            <el-button :disabled="busy" @click="generate('SIGNING')">生成签名密钥</el-button>
            <el-button :disabled="busy" @click="generate('STATE')">生成状态密钥</el-button>
            <el-button :disabled="busy" @click="keyDialog = true">导入密钥</el-button>
          </el-space>
          <el-table :data="keys">
            <el-table-column label="密钥 ID" prop="keyId" />
            <el-table-column label="类型">
              <template #default="{ row }">{{ row.kind === 'SIGNING' ? '签名' : '状态加密' }}</template>
            </el-table-column>
            <el-table-column label="状态">
              <template #default="{ row }">{{ row.active ? '当前使用' : '保留验证' }}</template>
            </el-table-column>
            <el-table-column label="创建时间" prop="createdAt" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane v-if="runtime.hasPermission('oidc:logout:query')" label="退出通知" name="deliveries">
          <el-button :disabled="busy" @click="loadDeliveries">刷新通知</el-button>
          <el-table :data="deliveries">
            <el-table-column label="客户端" prop="clientId" />
            <el-table-column label="状态" prop="status" />
            <el-table-column label="尝试次数" prop="attempts" width="100" />
            <el-table-column label="下次尝试" prop="nextAttemptAt" />
            <el-table-column label="失败原因" prop="lastError" show-overflow-tooltip />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button
                  v-if="runtime.hasPermission('oidc:logout:retry') && row.status === 'FAILED'"
                  link
                  type="primary"
                  :disabled="busy"
                  @click="retry(row.logoutOutboxId)"
                >
                  重试
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="20"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="loadDeliveries"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
    <el-dialog v-model="keyDialog" title="导入密钥" width="min(600px, 94vw)" destroy-on-close @closed="material = ''">
      <el-form label-position="top">
        <el-form-item label="类型">
          <el-radio-group v-model="keyKind">
            <el-radio value="SIGNING">签名 RSA JWK</el-radio>
            <el-radio value="STATE">状态 AES 密钥</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="keyKind === 'SIGNING' ? '私有 RSA JWK JSON' : 'Base64 编码的 32 字节密钥'">
          <el-input v-model="material" type="textarea" :rows="8" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="busy" @click="keyDialog = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="importKey">导入并激活</el-button>
      </template>
    </el-dialog>
  </section>
</template>
<script setup lang="ts">
import type {
  OidcLogoutDelivery,
  OidcServiceConfiguration,
  OidcServiceKey,
  OidcServiceSettings
} from '@namewta/domain-oidc';
import { onMounted, onBeforeUnmount, ref, shallowRef } from 'vue';
import type { OidcWebRuntime } from './runtime';
const { runtime } = defineProps<{ runtime: OidcWebRuntime }>();
const service = runtime.service.configuration;
const tab = ref('settings');
const loading = ref(false);
const busy = ref(false);
const error = ref('');
const configuration = shallowRef<OidcServiceConfiguration>();
const draft = ref<OidcServiceSettings>();
const keys = ref<OidcServiceKey[]>([]);
const deliveries = ref<OidcLogoutDelivery[]>([]);
const pageNum = ref(1);
const total = ref(0);
const keyDialog = ref(false);
const keyKind = ref<OidcServiceKey['kind']>('SIGNING');
const material = ref('');
const lifetime = new AbortController();
let deliveryVersion = 0;
function apply(value: OidcServiceConfiguration) {
  configuration.value = value;
  draft.value = { ...value.saved, sso: { ...value.saved.sso } };
}
async function loadDeliveries() {
  const version = ++deliveryVersion;
  if (!runtime.hasPermission('oidc:logout:query')) return;
  try {
    const result = await service.deliveries(pageNum.value, 20, lifetime.signal);
    if (!lifetime.signal.aborted && version === deliveryVersion) {
      deliveries.value = result.rows;
      total.value = result.total;
    }
  } catch {
    if (!lifetime.signal.aborted) error.value = '退出通知加载失败';
  }
}
async function load() {
  if (loading.value) return;
  loading.value = true;
  error.value = '';
  const results = await Promise.allSettled([
    runtime.hasPermission('oidc:service:query')
      ? service.get(lifetime.signal).then(value => {
          if (!lifetime.signal.aborted) apply(value);
        })
      : Promise.resolve(),
    runtime.hasPermission('oidc:key:manage')
      ? service.keys(lifetime.signal).then(value => {
          if (!lifetime.signal.aborted) keys.value = value;
        })
      : Promise.resolve(),
    loadDeliveries()
  ]);
  if (!lifetime.signal.aborted) {
    if (results.some(result => result.status === 'rejected')) error.value = '部分配置加载失败，请检查权限后重试';
    loading.value = false;
  }
}
async function run(action: () => Promise<void>) {
  if (busy.value) return;
  busy.value = true;
  error.value = '';
  try {
    await action();
    if (!lifetime.signal.aborted) {
      runtime.success('操作完成');
      await load();
    }
  } catch {
    if (!lifetime.signal.aborted) error.value = '操作未完成，请检查配置、权限与维护状态，刷新后重试。';
  } finally {
    if (!lifetime.signal.aborted) busy.value = false;
  }
}
async function save() {
  if (!configuration.value || !draft.value || !runtime.hasPermission('oidc:service:edit')) return;
  const value = draft.value;
  const version = configuration.value.version;
  await run(async () => {
    const result = await service.save(version, value, lifetime.signal);
    if (!lifetime.signal.aborted) apply(result);
  });
}
async function prepareRestart() {
  if (!runtime.hasPermission('oidc:service:edit')) return;
  try {
    await runtime.confirm('确认结束所有中央登录会话并通知关联应用退出？');
  } catch {
    return;
  }
  if (!lifetime.signal.aborted) await run(() => service.prepareRestart(lifetime.signal));
}
async function generate(kind: OidcServiceKey['kind']) {
  if (!runtime.hasPermission('oidc:key:manage')) return;
  try {
    await runtime.confirm('确认生成并激活新密钥？已有密钥将继续保留。');
  } catch {
    return;
  }
  if (!lifetime.signal.aborted) await run(() => service.generate(kind, lifetime.signal));
}
async function importKey() {
  if (!runtime.hasPermission('oidc:key:manage') || !material.value.trim()) return;
  const value = material.value;
  material.value = '';
  await run(async () => {
    await service.importKey(keyKind.value, value, lifetime.signal);
    if (!lifetime.signal.aborted) keyDialog.value = false;
  });
}
async function retry(id: string) {
  if (runtime.hasPermission('oidc:logout:retry')) await run(() => service.retry(id, lifetime.signal));
}
onMounted(load);
onBeforeUnmount(() => {
  lifetime.abort();
  material.value = '';
});
</script>
<style scoped>
.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 24px;
  margin-top: 24px;
}
.oidc-service :deep(.el-alert) {
  margin-bottom: 18px;
}
@media (max-width: 640px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
