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
    loadState: 'idle' as 'idle' | 'loading' | 'ready' | 'error',
    identityVersion: 0
  });

  const unreadCount = computed(() => state.notices.filter(item => !item.read).length);

  const sortNotices = () => {
    state.notices.sort((a, b) => Number(b.timestamp || 0) - Number(a.timestamp || 0));
  };

  const beginLoad = (token: string) => {
    if (ownerToken !== token) {
      ownerToken = token;
      state.notices = [];
      state.identityVersion++;
    }
    state.loadState = 'loading';
  };

  const setNotices = (notices: NoticeItem[], token: string) => {
    if (ownerToken !== token) return;
    state.notices = [...notices];
    sortNotices();
    state.loadState = 'ready';
  };

  const failLoad = (token: string) => {
    if (ownerToken === token) state.loadState = 'error';
  };

  const clearNotice = () => {
    ownerToken = undefined;
    state.notices = [];
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
