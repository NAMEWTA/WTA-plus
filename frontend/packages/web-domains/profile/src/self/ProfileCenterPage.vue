<template>
  <div class="profile-center">
    <template v-if="isCenter">
      <header class="profile-center__header">
        <h1>个人中心</h1>
        <p>管理实名认证与企业认证资料，查看申请进度。</p>
      </header>
      <el-alert v-if="failure" :title="failure" type="error" :closable="false" show-icon>
        <el-button link @click="load">重新加载</el-button>
      </el-alert>
      <section class="profile-center__options" aria-label="认证类型" v-loading="loading">
        <el-card v-for="option in options" :key="option.kind" shadow="never">
          <div class="verification-option">
            <SvgIcon :icon-class="option.icon" size="32px" />
            <div>
              <h2>{{ option.label }}</h2>
              <p>{{ option.description }}</p>
              <el-tag
                v-if="option.summary"
                :type="
                  option.summary.status === 'VERIFIED'
                    ? 'success'
                    : option.summary.status === 'WAITING'
                      ? 'warning'
                      : 'info'
                "
              >
                {{ certificationLabel(option.summary.status) }}
              </el-tag>
            </div>
            <el-button type="primary" plain :disabled="loading || !option.summary" @click="router.push(option.path)">
              {{ option.label }}
            </el-button>
          </div>
          <p v-if="option.summary?.returnReason" class="return-reason">退回原因：{{ option.summary.returnReason }}</p>
        </el-card>
      </section>
    </template>
    <router-view />
  </div>
</template>
<script setup lang="ts">
import type { PersonCertificationSummary, EnterpriseCertificationSummary } from '@namewta/domain-profile';
import SvgIcon from '@namewta/web-kit-ui-element/icon';
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { ProfileSelfWebRuntime } from './runtime';
import { certificationLabel } from './certification-status';
const { runtime } = defineProps<{ runtime: ProfileSelfWebRuntime }>();
const router = useRouter();
const route = useRoute();
const isCenter = computed(() => route.path === '/profile' || route.path === '/profile/');
const person = ref<PersonCertificationSummary>();
const enterprise = ref<EnterpriseCertificationSummary>();
const loading = ref(false);
const failure = ref('');
let generation = 0;
onBeforeUnmount(() => {
  generation++;
});
const options = computed(() =>
  [
    {
      kind: 'enterprise',
      label: '企业认证',
      description: '填写企业主体、法定代表人与联系资料。',
      icon: 'tabler:building',
      path: '/profile/enterprise',
      summary: enterprise.value
    },
    {
      kind: 'person',
      label: '实名认证',
      description: '填写个人身份信息并提交认证材料。',
      icon: 'tabler:user',
      path: '/profile/person',
      summary: person.value
    }
  ].filter(option => runtime.hasPermission(`profile:${option.kind}:apply`))
);
async function load() {
  const current = ++generation;
  loading.value = true;
  failure.value = '';
  const results = await Promise.allSettled([
    runtime.hasPermission('profile:person:apply')
      ? runtime.service.person.application.summary()
      : Promise.resolve(undefined),
    runtime.hasPermission('profile:enterprise:apply')
      ? runtime.service.enterprise.application.summary()
      : Promise.resolve(undefined)
  ]);
  if (current !== generation) return;
  const [personResult, enterpriseResult] = results;
  person.value = personResult.status === 'fulfilled' ? personResult.value?.data : undefined;
  enterprise.value = enterpriseResult.status === 'fulfilled' ? enterpriseResult.value?.data : undefined;
  if (results.some(result => result.status === 'rejected')) failure.value = '部分认证状态加载失败，请重新加载。';
  loading.value = false;
}
watch(
  isCenter,
  center => {
    if (center) void load();
  },
  { immediate: true }
);
</script>
<style scoped>
.profile-center {
  min-width: 0;
}
.profile-center__header {
  margin-bottom: 16px;
}
h1 {
  margin: 0;
  font-size: 20px;
}
h2 {
  margin: 0;
  font-size: 16px;
}
p {
  color: var(--app-text-muted);
  margin: 8px 0;
}
.profile-center__options {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}
.verification-option {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
}
.verification-option > .svg-icon {
  color: var(--app-text-accent);
}
.return-reason {
  color: var(--app-text-danger);
}
@media (max-width: 600px) {
  .verification-option {
    grid-template-columns: 32px minmax(0, 1fr);
  }
  .verification-option .el-button {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
