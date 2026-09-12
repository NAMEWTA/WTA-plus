export type NotificationChannel = 'IN_APP' | 'SMS' | 'MAIL';
export type NotifyRecipientType = 'ALL' | 'USER' | 'USER_TYPE';
export type NotificationStatus =
  | 'QUEUED'
  | 'PROCESSING'
  | 'ACCEPTED'
  | 'PARTIAL_FAILURE'
  | 'DELIVERED'
  | 'UNDELIVERABLE'
  | 'UNKNOWN'
  | 'FAILED'
  | 'CANCELLED'
  | 'EXPIRED';

export interface NotificationDelivery {
  deliveryId?: string;
  intentId?: string;
  userId: string | null;
  channel: NotificationChannel;
  status: NotificationStatus;
  providerMessageId: string | null;
  errorCode?: string | null;
  errorMessage?: string | null;
  attemptCount?: number;
  createdAt?: string | null;
}

export interface NotificationSnapshot {
  notificationId: string;
  status: NotificationStatus;
  createdAt: string | null;
  deliveries: NotificationDelivery[];
}

export interface NotificationDeliveryQuery {
  userId?: string;
  channel?: NotificationChannel;
  status?: NotificationStatus;
}

export interface NotifyNotice {
  noticeId: string;
  noticeTitle: string;
  noticeType: string;
  noticeContent?: string;
  recipientType: NotifyRecipientType;
  recipientIds: Array<string | number>;
  userTypeIds: Array<string | number>;
  channels: NotificationChannel[];
  status: string;
  lifecycle?: string;
  publishedAt?: string;
  retractedAt?: string;
  createTime?: string;
}

export interface NotifyUserCandidate {
  userId: string | number;
  userName?: string;
  nickName?: string;
  phoneNumber?: string;
  status?: string;
}

export interface NotifyUserCandidatePage {
  rows: NotifyUserCandidate[];
  total: number;
}

export interface NotifyUserTypeOption {
  userTypeId: string | number;
  userTypeName: string;
  status?: string;
}

export interface NotifyNoticeQuery {
  pageNum?: number;
  pageSize?: number;
  noticeTitle?: string;
  noticeType?: string;
  status?: string;
}

export type NotifyConfigChannel = 'MAIL' | 'SMS';

export interface NotifyChannelAccount {
  accountId?: string | number;
  channel: NotifyConfigChannel;
  configKey: string;
  enabled?: string;
  supplier?: string;
  host?: string;
  port?: number;
  mailFrom?: string;
  mailUser?: string;
  mailPass?: string;
  mailPassSet?: boolean;
  sslEnable?: string;
  starttlsEnable?: string;
  accessKeyId?: string;
  accessKeySecret?: string;
  accessKeySecretSet?: boolean;
  signature?: string;
  sdkAppId?: string;
  minuteMax: number;
  remark?: string;
  updateTime?: string;
}

export interface NotifySceneVariable {
  name: string;
  required: boolean;
  example?: string;
  description?: string;
}

export interface NotifySceneBinding {
  sceneCode: string;
  title: string;
  variables: NotifySceneVariable[];
  channel: NotifyConfigChannel;
  bindingId?: string | number;
  accountId?: string | number | null;
  accountConfigKey?: string;
  accountEnabled?: string;
  mailSubject?: string;
  mailBody?: string;
  smsTemplateCode?: string;
  smsParamMapping?: Record<string, string>;
  templateMinuteMax?: number;
  restricted?: string;
  recipientMinuteMax?: number;
  recipientDayMax?: number;
}

export interface NotifyInboxMessage {
  messageId: string;
  category: string;
  type?: string;
  noticeType?: string;
  channels?: string[];
  source?: string;
  title?: string;
  message?: string;
  content?: string;
  data?: Record<string, unknown> | null;
  path?: string;
  createTime?: string;
  seenTime?: string | null;
  readTime?: string | null;
}
