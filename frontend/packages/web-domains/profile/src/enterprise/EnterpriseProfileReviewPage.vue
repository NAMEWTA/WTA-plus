<template><ProfileReviewPage :runtime="runtime" profile-type="ENTERPRISE" /></template>
<script setup lang="ts">
import type { ProfileReviewWebRuntime } from '../review/runtime';
import ProfileReviewPage from '../review/ProfileReviewPage.vue';
defineProps<{ runtime: ProfileReviewWebRuntime }>();
</script>
