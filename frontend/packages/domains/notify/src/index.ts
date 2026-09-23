import type { DomainModule } from '@namewta/platform-app-runtime';

export const notifyDomainModule: DomainModule = Object.freeze({
  id: 'notify',
  backendModules: ['wta-notify'],
  capabilities: ['notification-control-plane', 'notification-monitor']
});

export { createNotificationService } from './transport';
export type {
  NotificationChannel,
  NotificationDelivery,
  NotificationDeliveryStatus,
  NotificationDeliveryQuery,
  NotificationSnapshot,
  NotificationStatus,
  RetryReceipt,
  CancelReceipt,
  NotifyInboxMessage,
  NotifyInboxPage,
  NotifyChannelAccount,
  NotifyConfigChannel,
  NotifyNotice,
  NotifyNoticeQuery,
  NotifyRecipientType,
  NotifySceneBinding,
  NotifySceneVariable,
  NotifyUserCandidate,
  NotifyUserCandidatePage,
  NotifyUserTypeOption
} from './types';
