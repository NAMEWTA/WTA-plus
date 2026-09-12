<template>
  <div class="person-detail-page">
    <PersonProfileDetailPanel v-if="profileId" :runtime="runtime" :profile-id="profileId" />
    <el-empty v-else description="缺少档案编号" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import type { ProfileWebRuntime } from '../runtime';
import PersonProfileDetailPanel from './PersonProfileDetailPanel.vue';

defineProps<{ runtime: ProfileWebRuntime }>();
const route = useRoute();
const profileId = computed(() => String(route.query.id ?? ''));
</script>

<style scoped>
.person-detail-page {
  padding: 16px;
}
</style>
