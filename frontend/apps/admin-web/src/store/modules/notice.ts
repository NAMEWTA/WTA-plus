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
  const state = reactive({
    notices: [] as NoticeItem[]
  });

  const unreadCount = computed(() => state.notices.filter(item => !item.read).length);

  const sortNotices = () => {
    state.notices.sort((a, b) => Number(b.timestamp || 0) - Number(a.timestamp || 0));
  };

  const setNotices = (notices: NoticeItem[]) => {
    state.notices = [...notices];
    sortNotices();
  };

  const clearNotice = () => {
    state.notices = [];
  };
  return {
    state,
    unreadCount,
    setNotices,
    clearNotice
  };
});
