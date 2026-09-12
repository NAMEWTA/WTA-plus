<template>
  <div class="enterprise-detail-page">
    <EnterpriseProfileDetailPanel v-if="profileId" :runtime="runtime" :profile-id="profileId" />
    <el-empty v-else description="缺少企业档案编号" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import type { ProfileWebRuntime } from '../runtime';
import EnterpriseProfileDetailPanel from './EnterpriseProfileDetailPanel.vue';

defineProps<{ runtime: ProfileWebRuntime }>();
const route = useRoute();
const profileId = computed(() => String(route.query.id ?? ''));
</script>

<style scoped>
.enterprise-detail-page {
  padding: 16px;
}
</style>
