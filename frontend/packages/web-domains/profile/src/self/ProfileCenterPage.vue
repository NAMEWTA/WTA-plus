<template>
  <main class="profile-center">
    <header class="profile-center__header">
      <span class="eyebrow">USER CENTER</span>
      <h1>档案中心</h1>
      <p>完善身份资料，完成个人或企业认证后即可使用对应服务。</p>
    </header>
    <section v-if="isCenter" class="profile-center__options" aria-label="认证类型">
      <article class="verification-option">
        <div class="verification-option__icon">人</div>
        <div>
          <h2>个人认证</h2>
          <p>提交个人身份信息，建立可信的个人档案。</p>
        </div>
        <el-button v-if="runtime.hasPermission('profile:person:apply')" type="primary" plain @click="open('/profile/person')">开始认证</el-button>
      </article>
      <article class="verification-option">
        <div class="verification-option__icon verification-option__icon--enterprise">企</div>
        <div>
          <h2>企业认证</h2>
          <p>提交企业主体与法定代表人资料，完成企业认证。</p>
        </div>
        <el-button v-if="runtime.hasPermission('profile:enterprise:apply')" type="primary" plain @click="open('/profile/enterprise')">开始认证</el-button>
      </article>
    </section>
    <router-view />
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { ProfileSelfWebRuntime } from './runtime';

const { runtime } = defineProps<{ runtime: ProfileSelfWebRuntime }>();
const router = useRouter();
const route = useRoute();
const isCenter = computed(() => route.path === '/profile' || route.path === '/profile/');
const open = (path: string) => router.push(path);
</script>

<style scoped>
.profile-center {
  max-width: 1040px;
  margin: 0 auto;
  padding: 48px 32px;
}

.profile-center__header {
  margin-bottom: 32px;
}

.eyebrow {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  margin-top: 8px;
  color: #172033;
  font-size: 34px;
}

.profile-center__header p {
  margin-top: 10px;
  color: #64748b;
}

.profile-center__options {
  display: grid;
  gap: 16px;
}

.verification-option {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 24px;
  border: 1px solid #dbe4ea;
  border-radius: 8px;
  background: #fff;
}

.verification-option__icon {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #0f766e;
  font-size: 22px;
  font-weight: 700;
}

.verification-option__icon--enterprise {
  background: #2563eb;
}

.verification-option h2 {
  color: #172033;
  font-size: 18px;
}

.verification-option p {
  margin-top: 6px;
  color: #64748b;
  line-height: 1.6;
}

@media (max-width: 640px) {
  .profile-center {
    padding: 32px 18px;
  }

  .verification-option {
    grid-template-columns: 44px minmax(0, 1fr);
  }

  .verification-option .el-button {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
