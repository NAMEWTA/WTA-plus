import type { ApiErrorInfo } from '@namewta/platform-contracts';

export interface PageQuery {
  pageNum: number;
  pageSize: number;
}
export interface BaseEntity {
  createBy?: any;
  createDept?: any;
  createTime?: string;
  updateBy?: any;
  updateTime?: any;
}
export interface ApiResponse<T = unknown> {
  code?: number;
  data: T;
  msg?: string;
  error?: ApiErrorInfo;
}
export interface PageResult<T> {
  rows: T[];
  total: number;
}
export type Identifier = string | number;
export type IdentifierList = Identifier | readonly Identifier[];

export interface CacheVO {
  commandStats: Array<{ name: string; value: string }>;
  dbSize: number;
  info: Record<string, string>;
}
export interface LoginInfoVO {
  infoId: Identifier;
  tenantId: Identifier;
  userName: string;
  status: string;
  ipaddr: string;
  loginLocation: string;
  browser: string;
  os: string;
  msg: string;
  loginTime: string;
}
export interface LoginInfoQuery extends PageQuery {
  ipaddr: string;
  userName: string;
  status: string;
  orderByColumn: string;
  isAsc: string;
}
export interface OnlineQuery extends PageQuery {
  ipaddr: string;
  userName: string;
}
export interface OnlineVO extends BaseEntity {
  tokenId: string;
  deptName: string;
  userName: string;
  clientKey?: string;
  deviceType?: string;
  ipaddr: string;
  loginLocation: string;
  browser: string;
  os: string;
  loginTime: number;
}
export interface OperLogQuery extends PageQuery {
  operIp: string;
  title: string;
  operName: string;
  userId: string;
  deptId: string;
  clientKey: string;
  deviceType: string;
  browser: string;
  os: string;
  businessType: string;
  status: string;
  orderByColumn: string;
  isAsc: string;
}
export interface OperLogVO extends BaseEntity {
  operId: Identifier;
  tenantId: string;
  title: string;
  businessType: number;
  businessTypes: number[] | undefined;
  method: string;
  requestMethod: string;
  operatorType: number;
  operName: string;
  userId: Identifier;
  deptId: Identifier;
  deptName: string;
  clientKey: string;
  deviceType: string;
  browser: string;
  os: string;
  operUrl: string;
  operIp: string;
  operLocation: string;
  operParam: string;
  jsonResult: string;
  status: number;
  errorMsg: string;
  operTime: string;
  costTime: number;
}
export interface OperLogForm extends Omit<OperLogVO, keyof BaseEntity | 'operId' | 'tenantId' | 'userId' | 'deptId'> {
  operId: Identifier | undefined;
  tenantId: Identifier | undefined;
  userId: Identifier | undefined;
  deptId: Identifier | undefined;
}
export interface NotifyQuery extends PageQuery {
  requestId?: string;
  originalRequestId?: string;
  bizType?: string;
  bizId?: string;
  channel?: string;
  providerKey?: string;
  status?: string;
  providerMessageId?: string;
  traceId?: string;
  clientPk?: string;
  beginTime?: string;
  endTime?: string;
}
export interface NotifyListVO extends BaseEntity {
  notifyLogId: Identifier;
  requestId?: string;
  originalRequestId?: string;
  bizType?: string;
  bizId?: string;
  channel?: string;
  providerKey?: string;
  status?: string;
  errorCode?: string;
  errorMessage?: string;
  clientPk?: Identifier;
  userId?: Identifier;
  traceId?: string;
  maskedTargets: string[];
}
export interface NotifyLogVO extends BaseEntity {
  notifyLogId: Identifier;
  requestId?: string;
  originalRequestId?: string;
  bizType?: string;
  bizId?: string;
  channel?: string;
  providerKey?: string;
  subject?: string;
  content?: string;
  contentType?: string;
  templateCode?: string;
  templateParams?: string;
  contentSnapshot?: string;
  attachmentOssIds?: string;
  status?: string;
  errorCode?: string;
  errorMessage?: string;
  clientPk?: Identifier;
  userId?: Identifier;
  traceId?: string;
}
export interface NotifyDeliveryVO extends BaseEntity {
  notifyDeliveryLogId: Identifier;
  notifyLogId: Identifier;
  targetType?: string;
  targetRole?: string;
  targetValue?: string;
  providerKey?: string;
  providerMessageId?: string;
  attemptNo?: number;
  status?: string;
  costTime?: number;
  errorCode?: string;
  errorMessage?: string;
}
export interface NotifyDetailVO {
  notification: NotifyLogVO;
  deliveries: NotifyDeliveryVO[];
  attachmentOssIds: Identifier[];
}
export interface OssDownloadUrl {
  url: string;
  expiresAt: string;
  fileName: string;
}
