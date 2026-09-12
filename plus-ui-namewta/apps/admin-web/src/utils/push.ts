import type { NotifyInboxMessage } from '@namewta/domain-notify';
import { ElNotification } from 'element-plus';
import { adminHttp } from '@/application/http';
import { notificationService } from '@/application/services';
import { getToken } from '@/application/session';
import { useNoticeStore } from '@/store/modules/notice';
import { createPushConnection, createPushUrl } from '@/utils/push-connection';
import { parsePushMessage, resolveNoticeGroup, resolveNoticeTitle, shouldAppendNotice } from '@/utils/push-message';

let pushConnection: ReturnType<typeof createPushConnection> | undefined;
let pushKicked = false;
let resumePushTimer: ReturnType<typeof setTimeout> | undefined;
let inboxRequest = 0;
let removeResumeListeners: (() => void) | undefined;

const refreshInbox = () => {
  void refreshMessageInbox().catch(error => console.warn('消息盒子刷新失败:', error));
};

/** 通知所有收件箱视图刷新，消息盒子也重新读取服务端事实。 */
export const refreshMessageInbox = async () => {
  window.dispatchEvent(new Event('notify:inbox-updated'));
  await initMessageBox();
};

const handlePushMessage = (raw: string) => {
  if (raw === 'kicked') {
    pushKicked = true;
    pushConnection?.close();
    return;
  }
  const payload = parsePushMessage(raw);
  if (!shouldAppendNotice(payload)) return;

  // 实时事件只提示和刷新；消息列表与已读状态统一来自 Notify 收件箱。
  refreshInbox();
  ElNotification({
    title: payload.title || resolveNoticeTitle(payload),
    message: payload.message ?? '',
    type: 'success',
    duration: 3000
  });
};

const toNoticeItem = (item: NotifyInboxMessage) => {
  const parsedTime = item.createTime ? new Date(item.createTime).getTime() : 0;
  const timestamp = Number.isFinite(parsedTime) ? parsedTime : 0;
  return {
    messageId: item.messageId,
    title: item.title,
    category: resolveNoticeGroup(item),
    type: item.type ?? 'message',
    source: item.source ?? 'backend',
    message: item.message ?? '',
    content: item.content,
    data: item.data ?? null,
    path: item.path,
    read: Boolean(item.readTime),
    timestamp,
    time: timestamp ? new Date(timestamp).toLocaleString() : ''
  };
};

const requestPushTicket = async () => {
  const body = await adminHttp.request<{ data?: unknown }>({
    url: `${import.meta.env.VITE_APP_MESSAGE_PATH || '/resource/message'}/ticket`,
    method: 'get',
    timeout: 10000
  });
  if (typeof body.data !== 'string' || !body.data) throw new Error('推送票据不可用');
  return body.data;
};

export const initPush = async () => {
  closePush();
  if (import.meta.env.VITE_APP_MESSAGE_ENABLED === 'false' || !getToken()) return;

  const path = import.meta.env.VITE_APP_MESSAGE_PATH || '/resource/message';
  const transport = import.meta.env.VITE_APP_MESSAGE_TRANSPORT?.toLowerCase() === 'websocket' ? 'websocket' : 'sse';
  pushConnection = createPushConnection({
    transport,
    requestTicket: requestPushTicket,
    createUrl: ticket =>
      createPushUrl(import.meta.env.VITE_APP_BASE_API, path, window.location.origin, ticket, transport),
    onMessage: handlePushMessage,
    onConnected: refreshInbox,
    onError: error => console.warn('推送连接中断，正在重连:', error)
  });
  window.addEventListener('focus', resumePushIfNeeded);
  document.addEventListener('visibilitychange', resumePushIfNeeded);
  window.addEventListener('online', resumePushIfNeeded);
  removeResumeListeners = () => {
    window.removeEventListener('focus', resumePushIfNeeded);
    document.removeEventListener('visibilitychange', resumePushIfNeeded);
    window.removeEventListener('online', resumePushIfNeeded);
  };
  await pushConnection.start();
};

export const initMessageBox = async () => {
  const currentRequest = ++inboxRequest;
  const token = getToken();
  if (import.meta.env.VITE_APP_MESSAGE_ENABLED === 'false' || !token) {
    useNoticeStore().clearNotice();
    return;
  }
  const { data } = await notificationService.inbox.list();
  if (currentRequest !== inboxRequest || token !== getToken()) return;
  useNoticeStore().setNotices((data ?? []).map(toNoticeItem));
};

export const closePush = () => {
  pushKicked = false;
  inboxRequest++;
  clearTimeout(resumePushTimer);
  resumePushTimer = undefined;
  pushConnection?.close();
  pushConnection = undefined;
  removeResumeListeners?.();
  removeResumeListeners = undefined;
};

const resumePushIfNeeded = () => {
  if (!pushKicked || !getToken() || document.visibilityState !== 'visible') return;
  clearTimeout(resumePushTimer);
  resumePushTimer = setTimeout(() => {
    resumePushTimer = undefined;
    if (pushKicked && getToken() && document.visibilityState === 'visible') void initPush();
  }, 300);
};

if (import.meta.hot) import.meta.hot.dispose(closePush);
