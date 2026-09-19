import { requirePasswordPolicy, type ClientAuthContext } from '@namewta/domain-admin';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { identityAccessService } from '@/application/services';

/** 公开入口共享可见性；表单草稿和验证码仍由注册页持有。 */
export const useRegistrationAvailabilityStore = defineStore('registration-availability', () => {
  const enabled = ref(false);
  let generation = 0;
  let pending: Promise<void> | undefined;
  const reset = () => { generation++; enabled.value = false; pending = undefined; };
  const accept = (context: ClientAuthContext) => {
    enabled.value = false;
    if (context.clientEnabled && context.registerEnabled) {
      requirePasswordPolicy(context);
      enabled.value = true;
    }
  };
  const load = () => {
    if (pending) return pending;
    const current = ++generation;
    enabled.value = false;
    const attempt = (async () => {
      try {
        const context = await identityAccessService.getClientContext();
        if (current === generation) accept(context);
      } catch {
        if (current === generation) enabled.value = false;
      }
    })();
    pending = attempt;
    void attempt.finally(() => { if (pending === attempt) pending = undefined; });
    return attempt;
  };
  return { enabled, reset, accept, load };
});
