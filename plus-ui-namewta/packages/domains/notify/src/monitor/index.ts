export type { NotificationDelivery, NotificationDeliveryQuery, NotificationSnapshot } from '../types';
export { createNotificationService } from '../transport';
export const notifyMonitorResource = Object.freeze({
  controller: 'NotificationMonitorController',
  basePath: '/notify/monitor'
});
