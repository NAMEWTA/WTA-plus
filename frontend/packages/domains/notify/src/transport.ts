import type { paths } from '@namewta/api-contracts';
import type { ApiErrorInfo, HttpClient } from '@namewta/platform-contracts';
import type {
  NotificationDelivery,
  NotificationDeliveryQuery,
  NotificationSnapshot,
  NotifyChannelAccount,
  NotifyConfigChannel,
  NotifyInboxMessage,
  NotifyNotice,
  NotifyNoticeQuery,
  NotifySceneBinding
} from './types';

type ApiResponse<T> = { data: T; code?: number; msg?: string; error?: ApiErrorInfo };

export function createNotificationService(http: HttpClient) {
  const request = <T>(config: Parameters<HttpClient['request']>[0]) =>
    http.request<T>(config) as Promise<ApiResponse<T>>;
  const snapshotPath: keyof paths = '/notify/monitor/snapshot';
  const deliveriesPath: keyof paths = '/notify/monitor/deliveries';
  return Object.freeze({
    snapshot: (notificationId: string) =>
      request<NotificationSnapshot>({ url: snapshotPath, method: 'get', params: { notificationId } }),
    deliveries: (params: NotificationDeliveryQuery = {}) =>
      request<NotificationDelivery[]>({ url: deliveriesPath, method: 'get', params }),
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
      list: () => request<NotifyInboxMessage[]>({ url: '/notify/inbox', method: 'get' }),
      seen: (messageId: string | number) => request<void>({ url: `/notify/inbox/${messageId}/seen`, method: 'post' }),
      read: (messageId: string | number) => request<void>({ url: `/notify/inbox/${messageId}/read`, method: 'post' }),
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
