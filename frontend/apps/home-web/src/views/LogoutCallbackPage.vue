<template><LogoutCallbackPage :runtime="runtime" /></template>
<script setup lang="ts">
import { LogoutCallbackPage } from '@namewta/web-domain-admin';
import { createAppSocialRuntime } from '@/application/social';
const runtime = createAppSocialRuntime();
</script>
