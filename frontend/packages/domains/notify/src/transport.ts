import type { components, paths } from '@namewta/api-contracts';
import type { ApiErrorInfo, HttpClient } from '@namewta/platform-contracts';
import type {
  NotificationDelivery,
  NotificationDeliveryQuery,
  NotificationSnapshot,
  RetryReceipt,
  CancelReceipt,
  NotifyChannelAccount,
  NotifyConfigChannel,
  NotifyInboxMessage,
  NotifyInboxPage,
  NotifyNotice,
  NotifyNoticeQuery,
  NotifySceneBinding
} from './types';

type ApiResponse<T> = { data: T; code?: number; msg?: string; error?: ApiErrorInfo };
type InboxMessageWire = components['schemas']['NotifyInboxMessageVo'];
type InboxPageWire = components['schemas']['NotifyInboxPageVo'];

const inboxId = (value: string | number | undefined): string => {
  if ((typeof value !== 'string' && (typeof value !== 'number' || !Number.isSafeInteger(value))) ||
      !/^[1-9]\d*$/.test(String(value))) {
    throw new Error('收件箱消息编号无效');
  }
  return String(value);
};

const inboxMessage = (wire: InboxMessageWire): NotifyInboxMessage => ({
  ...wire,
  messageId: inboxId(wire.messageId),
  category: wire.category ?? 'system'
});

const inboxPage = (wire: InboxPageWire | undefined): NotifyInboxPage => {
  const total = wire?.total;
  const unreadTotal = wire?.unreadTotal;
  if (!wire || !Array.isArray(wire.rows) || typeof total !== 'number' || typeof unreadTotal !== 'number' ||
      !Number.isSafeInteger(total) || !Number.isSafeInteger(unreadTotal) || total < 0 || unreadTotal < 0) {
    throw new Error('收件箱分页响应无效');
  }
  return { rows: wire.rows.map(inboxMessage), total, unreadTotal };
};

export function createNotificationService(http: HttpClient) {
  const request = <T>(config: Parameters<HttpClient['request']>[0]) =>
    http.request<T>(config) as Promise<ApiResponse<T>>;
  const snapshotPath: keyof paths = '/notify/monitor/snapshot';
  const deliveriesPath: keyof paths = '/notify/monitor/deliveries';
  const positiveId = (id: string) => {
    if (!/^[1-9]\d*$/.test(id)) throw new Error('通知或投递编号无效');
    return id;
  };
  return Object.freeze({
    snapshot: (notificationId: string) =>
      request<NotificationSnapshot>({ url: snapshotPath, method: 'get', params: { notificationId } }),
    deliveries: (params: NotificationDeliveryQuery = {}) =>
      request<NotificationDelivery[]>({ url: deliveriesPath, method: 'get', params }),
    notification: {
      retry: (notificationId: string, deliveryId: string) =>
        request<RetryReceipt>({
          url: `/notify/notification/${positiveId(notificationId)}/retry`,
          method: 'post',
          data: { deliveryId: positiveId(deliveryId), reason: 'manual' }
        }),
      cancel: (notificationId: string) =>
        request<CancelReceipt>({ url: `/notify/notification/${positiveId(notificationId)}/cancel`, method: 'post' })
    },
    notices: {
      list: (params: NotifyNoticeQuery = {}) =>
        request<{ rows: NotifyNotice[]; total: number }>({ url: '/notify/notice/list', method: 'get', params }),
      get: (noticeId: string | number) => request<NotifyNotice>({ url: `/notify/notice/${noticeId}`, method: 'get' }),
      save: (data: Partial<NotifyNotice>) => request<void>({ url: '/notify/notice/save', method: 'post', data }),
      publish: (noticeId: string | number) =>
        request<void>({ url: `/notify/notice/${noticeId}/publish`, method: 'post' }),
      retract: (noticeId: string | number) =>
        request<void>({ url: `/notify/notice/${noticeId}/retract`, method: 'post' }),
      remove: (noticeIds: Array<string | number>) =>
        request<void>({ url: '/notify/notice/remove', method: 'post', data: noticeIds })
    },
    inbox: {
      list: async (pageNum = 1, pageSize = 20): Promise<ApiResponse<NotifyInboxPage>> => {
        const response = await request<InboxPageWire>({
          url: '/notify/inbox', method: 'get', params: { pageNum, pageSize }
        });
        return { ...response, data: inboxPage(response.data) };
      },
      detail: async (messageId: string | number): Promise<ApiResponse<NotifyInboxMessage>> => {
        const response = await request<InboxMessageWire>({
          url: `/notify/inbox/${inboxId(messageId)}`, method: 'get'
        });
        if (!response.data) throw new Error('收件箱消息不存在');
        return { ...response, data: inboxMessage(response.data) };
      },
      seen: (messageId: string | number) => request<void>({ url: `/notify/inbox/${inboxId(messageId)}/seen`, method: 'post' }),
      read: (messageId: string | number) => request<void>({ url: `/notify/inbox/${inboxId(messageId)}/read`, method: 'post' }),
      readAll: () => request<void>({ url: '/notify/inbox/read-all', method: 'post' })
    },
    config: {
      accounts: (channel: NotifyConfigChannel, pageNum = 1, pageSize = 10) =>
        request<{ rows: NotifyChannelAccount[]; total: number }>({
          url: '/notify/config/account/list',
          method: 'get',
          params: { channel, pageNum, pageSize }
        }),
      account: (accountId: string | number) =>
        request<NotifyChannelAccount>({ url: `/notify/config/account/${accountId}`, method: 'get' }),
      addAccount: (data: NotifyChannelAccount) =>
        request<void>({ url: '/notify/config/account', method: 'post', data }),
      editAccount: (data: NotifyChannelAccount) =>
        request<void>({ url: '/notify/config/account/edit', method: 'post', data }),
      changeStatus: (accountId: string | number, enabled: string) =>
        request<void>({ url: '/notify/config/account/changeStatus', method: 'post', data: { accountId, enabled } }),
      removeAccount: (accountId: string | number) =>
        request<void>({ url: '/notify/config/account/remove', method: 'post', data: accountId }),
      scenes: (channel: NotifyConfigChannel) =>
        request<NotifySceneBinding[]>({ url: '/notify/config/scene/list', method: 'get', params: { channel } }),
      saveScene: (data: Partial<NotifySceneBinding>) =>
        request<void>({ url: '/notify/config/scene/save', method: 'post', data }),
      testAccount: (data: { accountId: string | number; sceneCode?: string; target: string }) =>
        request<string>({ url: '/notify/config/test/account', method: 'post', data }),
      testTemplate: (data: { sceneCode: string; channel: NotifyConfigChannel; target: string }) =>
        request<string>({ url: '/notify/config/test/template', method: 'post', data })
    }
  });
}
