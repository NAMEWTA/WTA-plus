<template><SocialBindingsPage :runtime="runtime" :confirm="confirm" /></template>
<script setup lang="ts">
import { SocialBindingsPage } from '@namewta/web-domain-admin';
import { ElMessageBox } from 'element-plus';
import { createAppSocialRuntime } from '@/application/social';
const confirm = (message: string) => ElMessageBox.confirm(message, '确认').then(() => undefined);
const runtime = createAppSocialRuntime();
</script>
