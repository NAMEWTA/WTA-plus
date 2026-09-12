<template>
  <div v-loading="loading" class="social-callback"></div>
</template>

<script setup lang="ts">
import type { SocialCallbackInput } from '@namewta/domain-admin';
import { identityAccessService } from '@/application/services';
import { getToken } from '@/application/session';

const route = useRoute();
const loading = ref(true);

/**
 * 接收Route传递的参数
 * @param {Object} route.query.
 */
const code = route.query.code as string;
const state = route.query.state as string;
const source = route.query.source as string;

const complete = (message = '操作成功') => {
  ElMessage.success(message);
  setTimeout(() => {
    location.href = import.meta.env.VITE_APP_CONTEXT_PATH + 'index';
  }, 2000);
};

const handleError = (error: any) => {
  ElMessage.error(error.message);
  setTimeout(() => {
    location.href = import.meta.env.VITE_APP_CONTEXT_PATH + 'index';
  }, 2000);
};

const callbackByCode = async (data: SocialCallbackInput) => {
  try {
    const result = await identityAccessService.socialCallback(data);
    complete(result.message);
    loading.value = false;
  } catch (error) {
    handleError(error);
  }
};

const loginByCode = async (data: SocialCallbackInput) => {
  try {
    await identityAccessService.socialLogin(data);
    complete();
    loading.value = false;
  } catch (error) {
    handleError(error);
  }
};

const init = async () => {
  const data: SocialCallbackInput = {
    socialCode: code,
    socialState: state,
    source
  };

  if (!getToken()) {
    await loginByCode(data);
  } else {
    await callbackByCode(data);
  }
};

onMounted(() => {
  nextTick(() => {
    init();
  });
});
</script>
