<template>
  <section v-if="runtime.hasPermission('profile:enterprise:apply')" class="enterprise-transfer" aria-labelledby="enterprise-transfer-title" :aria-busy="busy">
    <h2 id="enterprise-transfer-title">转移企业负责人</h2>
    <p>当前负责人可将企业档案转移给已完成个人认证的账户。请由接收者提供验证码。</p>
    <form @submit.prevent="send">
      <fieldset :disabled="busy || Boolean(challengeId) || transferred">
        <label>接收者姓名<input v-model="target.fullName" required maxlength="100" autocomplete="off" /></label>
        <label>证件后四位<input v-model="target.documentLastFour" required pattern="[0-9A-Za-z]{4}" maxlength="4" autocomplete="off" /></label>
        <label>接收者手机号<input v-model="target.phone" required maxlength="32" type="tel" autocomplete="off" /></label>
        <el-button native-type="submit" type="primary" :disabled="busy || Boolean(challengeId) || transferred">发送转移验证码</el-button>
      </fieldset>
    </form>
    <p v-if="message" role="status" aria-live="polite">{{ message }}</p>
    <form v-if="challengeId" @submit.prevent="confirmTransfer">
      <label>转移验证码<input v-model="code" required pattern="[0-9]{6}" maxlength="6" inputmode="numeric" autocomplete="one-time-code" :disabled="busy || expired" /></label>
      <p v-if="!expired">验证码剩余 {{ remainingSeconds }} 秒。</p>
      <p v-else role="status">验证码已过期，请重新发码。</p>
      <div class="enterprise-transfer__actions">
        <el-button native-type="submit" type="primary" :loading="busy" :disabled="busy || expired">确认转移</el-button>
        <el-button :disabled="busy" @click="restart">重新发码</el-button>
      </div>
    </form>
    <el-button v-else-if="retryable" :disabled="busy" @click="restart">重新发码</el-button>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import type { EnterpriseTransferResult } from '@namewta/domain-profile/enterprise/transfer';
import type { ProfileSelfWebRuntime } from './runtime';

const { runtime } = defineProps<{ runtime: ProfileSelfWebRuntime }>();
const target = reactive({ fullName: '', documentLastFour: '', phone: '' });
const busy = ref(false);
const challengeId = ref('');
const code = ref('');
const message = ref('');
const retryable = ref(false);
const transferred = ref(false);
const deadline = ref(0);
const now = ref(Date.now());
const remainingSeconds = computed(() => Math.max(0, Math.ceil((deadline.value - now.value) / 1000)));
const expired = computed(() => Boolean(challengeId.value) && remainingSeconds.value === 0);
let disposed = false;
let pending: AbortController | undefined;
let timer: ReturnType<typeof setInterval> | undefined;
onMounted(() => { timer = setInterval(() => { now.value = Date.now(); }, 1000); });
onUnmounted(() => { disposed = true; pending?.abort(); clearInterval(timer); });

function receive(result: EnterpriseTransferResult, initial: boolean) {
  if (result.status === 'QUEUED' && result.challengeId && result.expiresInSeconds != null) {
    challengeId.value = result.challengeId;
    // 状态重查只缩短本地倒计时，不能重新开始五分钟窗口。
    const next = Date.now() + Math.max(0, result.expiresInSeconds) * 1000;
    deadline.value = initial ? next : Math.min(deadline.value, next);
    now.value = Date.now();
    message.value = '验证码已排队，请等待接收者收到短信后确认；排队不代表转移成功。';
    retryable.value = false;
  } else if (result.status === 'TRANSFERRED') {
    transferred.value = true; challengeId.value = ''; code.value = '';
    message.value = '企业负责人已转移。'; retryable.value = false;
  } else {
    challengeId.value = ''; code.value = ''; retryable.value = true;
    message.value = result.status === 'EXPIRED' ? '验证码已过期，请重新发码。'
      : result.status === 'NOT_AVAILABLE' ? '当前信息无法发起转移，请核对负责人资格与接收者资料。'
        : '验证码投递失败，请重新发码。';
  }
}

function failure(error: unknown) {
  const detail = error instanceof Error ? error.message : '';
  message.value = detail.includes('RATE_LIMITED') ? '请求过于频繁，请稍后重新发码。'
    : detail.includes('CHALLENGE_INVALID') ? '验证码不正确、已失效或已使用，请核对或重新发码。'
      : detail.includes('SOURCE_CHANGED') ? '负责人绑定已变化，请重新发起转移。'
        : '操作未完成，请稍后重试。';
}

async function send() {
  if (busy.value || challengeId.value || transferred.value || !runtime.hasPermission('profile:enterprise:apply')) return;
  busy.value = true;
  pending = new AbortController();
  try {
    const result = await runtime.service.enterprise.transfer.send({ ...target }, { signal: pending.signal });
    if (!disposed) receive(result.data, true);
  } catch (error) { if (!disposed) failure(error); }
  finally { pending = undefined; if (!disposed) busy.value = false; }
}

async function confirmTransfer() {
  if (busy.value || !challengeId.value || expired.value || !runtime.hasPermission('profile:enterprise:apply')) return;
  busy.value = true;
  pending = new AbortController();
  try {
    const result = await runtime.service.enterprise.transfer.confirm({ challengeId: challengeId.value, code: code.value }, { signal: pending.signal });
    if (!disposed) receive(result.data, false);
  } catch (error) { if (!disposed) failure(error); }
  finally { pending = undefined; if (!disposed) busy.value = false; }
}

function restart() {
  if (busy.value) return;
  challengeId.value = ''; code.value = ''; deadline.value = 0; retryable.value = false;
  message.value = '核对接收者资料后，可再次发送验证码。';
}
</script>

<style scoped>
.enterprise-transfer { margin-top: 24px; padding: 24px; border: 1px solid #dbe4ea; border-radius: 8px; background: #fff; }
h2 { margin: 0; font-size: 18px; color: #172033; }
p { color: #334155; line-height: 1.6; }
fieldset { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; border: 0; padding: 0; margin: 0; min-width: 0; }
label { display: grid; gap: 6px; color: #334155; }
input { min-width: 0; width: 100%; box-sizing: border-box; padding: 9px 12px; border: 1px solid #94a3b8; border-radius: 4px; font: inherit; }
input:focus-visible { outline: 2px solid #0f766e; outline-offset: 2px; }
.enterprise-transfer__actions { display: flex; gap: 12px; flex-wrap: wrap; }
@media (max-width: 640px) { fieldset { grid-template-columns: minmax(0, 1fr); } .enterprise-transfer { padding: 18px; } }
</style>
