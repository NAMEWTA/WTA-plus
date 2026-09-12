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
  NotificationDeliveryQuery,
  NotificationSnapshot,
  NotificationStatus,
  NotifyInboxMessage,
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
