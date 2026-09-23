import { defineStore } from 'pinia';
import { computed, reactive } from 'vue';

export interface NoticeItem {
  messageId: string;
  title?: string;
  category?: string;
  type?: string;
  source?: string;
  read: boolean;
  message: string;
  content?: string;
  data?: Record<string, unknown> | null;
  path?: string;
  timestamp?: number;
  time: string;
}

export const useNoticeStore = defineStore('notice', () => {
  let ownerToken: string | undefined;
  const state = reactive({
    notices: [] as NoticeItem[],
    unreadTotal: 0,
    loadState: 'idle' as 'idle' | 'loading' | 'ready' | 'error',
    identityVersion: 0
  });

  const unreadCount = computed(() => state.unreadTotal);

  const beginLoad = (token: string) => {
    if (ownerToken !== token) {
      ownerToken = token;
      state.notices = [];
      state.unreadTotal = 0;
      state.identityVersion++;
    }
    state.loadState = 'loading';
  };

  const setNotices = (notices: NoticeItem[], unreadTotal: number, token: string) => {
    if (ownerToken !== token) return;
    state.notices = [...notices];
    state.unreadTotal = unreadTotal;
    state.loadState = 'ready';
  };

  const failLoad = (token: string) => {
    if (ownerToken === token) state.loadState = 'error';
  };

  const clearNotice = () => {
    ownerToken = undefined;
    state.notices = [];
    state.unreadTotal = 0;
    state.loadState = 'idle';
    state.identityVersion++;
  };
  return {
    state,
    unreadCount,
    beginLoad,
    setNotices,
    failLoad,
    clearNotice
  };
});
