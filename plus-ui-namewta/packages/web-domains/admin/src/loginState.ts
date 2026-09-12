import type { IdentitySession, LoginVerification } from '@namewta/domain-admin';
import { computed, reactive, ref } from 'vue';
import type { IdentityAccessWebRuntime } from './runtime';

const messageFor = (error: unknown) =>
  error instanceof Error && error.message ? error.message : '登录请求失败，请稍后重试';

export function createIdentityLoginState(runtime: IdentityAccessWebRuntime) {
  const ready = ref(false);
  const preparing = ref(false);
  const submitting = ref(false);
  const errorMessage = ref('');
  const verification = ref<LoginVerification>();
  const form = reactive({ username: '', password: '', code: '' });
  let active = true;
  let preparation: Promise<void> | undefined;

  const prepare = () => {
    if (preparation) return preparation;
    ready.value = false;
    preparing.value = true;
    errorMessage.value = '';
    verification.value = undefined;
    form.code = '';
    preparation = runtime.service
      .prepareLogin()
      .then(result => {
        if (!active) return;
        verification.value = result.verification;
        ready.value = true;
      })
      .catch(error => {
        if (active) errorMessage.value = messageFor(error);
      })
      .finally(() => {
        preparation = undefined;
        if (active) preparing.value = false;
      });
    return preparation;
  };

  const submit = async () => {
    if (!ready.value || preparing.value || submitting.value) return;
    submitting.value = true;
    errorMessage.value = '';
    try {
      const session: IdentitySession = await runtime.service.login({
        username: form.username,
        password: form.password,
        ...(form.code ? { code: form.code } : {}),
        ...(verification.value?.uuid ? { uuid: verification.value.uuid } : {})
      });
      if (active) await runtime.onAuthenticated(session);
    } catch (error) {
      if (active) errorMessage.value = messageFor(error);
    } finally {
      if (active) submitting.value = false;
    }
  };

  return Object.freeze({
    captchaImage: computed(() =>
      verification.value?.captchaEnabled ? `data:image/gif;base64,${verification.value.img}` : ''
    ),
    dispose: () => {
      active = false;
    },
    errorMessage,
    form,
    prepare,
    preparing,
    ready,
    submit,
    submitting,
    verification
  });
}
