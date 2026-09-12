export type { NotificationChannel, NotificationStatus } from '../types';
export { createNotificationService } from '../transport';
export const notifyNotificationResource = Object.freeze({
  controller: 'NotificationController',
  basePath: '/notify/notification'
});
