<template><SocialCallbackPage :runtime="runtime" /></template>
<script setup lang="ts">
import { SocialCallbackPage } from '@namewta/web-domain-admin';
import { createAppSocialRuntime } from '@/application/social';
const runtime = createAppSocialRuntime();
</script>
