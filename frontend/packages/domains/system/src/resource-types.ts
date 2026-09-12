import type { BaseEntity, PageQuery } from './types';

export type ResourceIdentifier = string | number;
export type ResourceIdentifierList = ResourceIdentifier | readonly ResourceIdentifier[];
export type DictTagType = 'primary' | 'success' | 'info' | 'warning' | 'danger';

export interface ConfigVO extends BaseEntity {
  configId: ResourceIdentifier;
  configName: string;
  configKey: string;
  configValue: string;
  configType: string;
  remark: string;
}
export interface ConfigForm {
  configId: ResourceIdentifier | undefined;
  configName: string;
  configKey: string;
  configValue: string;
  configType: string;
  remark: string;
}
export interface ConfigQuery extends PageQuery {
  configName: string;
  configKey: string;
  configType: string;
}
export interface DictDataQuery extends PageQuery {
  dictName: string;
  dictType: string;
  dictLabel: string;
}
export interface DictDataVO extends BaseEntity {
  dictCode: string;
  dictLabel: string;
  dictValue: string;
  cssClass: string;
  listClass: DictTagType;
  dictSort: number;
  remark: string;
}
export interface DictDataForm {
  dictType?: string;
  dictCode: string | undefined;
  dictLabel: string;
  dictValue: string;
  cssClass: string;
  listClass: DictTagType;
  dictSort: number;
  remark: string;
}
export interface DictTypeVO extends BaseEntity {
  dictId: ResourceIdentifier;
  dictName: string;
  dictType: string;
  remark: string;
}
export interface DictTypeForm {
  dictId: ResourceIdentifier | undefined;
  dictName: string;
  dictType: string;
  remark: string;
}
export interface DictTypeQuery extends PageQuery {
  dictName: string;
  dictType: string;
}
export type SocialBindingUrl = string;
export interface SocialAuthVO extends BaseEntity {
  id: ResourceIdentifier;
  source: string;
  avatar: string;
  userName: string;
}
export interface OssVO extends BaseEntity {
  ossId: ResourceIdentifier;
  fileName: string;
  originalName: string;
  fileSuffix: string;
  url: string;
  createByName: string;
  service: string;
  isTemp: 'Y' | 'N';
  expireTime?: string;
  deleteState: 'ACTIVE' | 'PENDING';
  referenceCount: number;
  references: Array<{ refType: string; refId: string }>;
}
export interface OssUploadVO {
  url: string;
  fileName: string;
  ossId: string;
}
export type OssUploadMode = 'SINGLE' | 'MULTIPART';
export type OssUploadState = 'INITIALIZED' | 'UPLOADING' | 'COMPLETING' | 'COMPLETED' | 'ABORTED' | 'EXPIRED';
export interface OssPresignedRequest {
  method: string;
  url: string;
  requiredHeaders: Record<string, string>;
  expiresAt: string;
}
export interface OssUploadInitRequest {
  policy: string;
  fileName: string;
  fileSize: number;
  contentType: string;
  fingerprint: string;
}
export interface OssUploadInitResponse {
  uploadToken: string;
  mode: OssUploadMode;
  expiresAt: string;
  presignedRequest?: OssPresignedRequest;
  partSize?: number;
  partCount?: number;
}
export interface OssSignedPart extends OssPresignedRequest {
  partNumber: number;
}
export interface OssUploadedPart {
  partNumber: number;
  eTag: string;
  size: number;
}
export interface OssUploadResumeResponse {
  uploadToken: string;
  mode: OssUploadMode;
  state: OssUploadState;
  completedOssId: string | null;
  fileName: string;
  fileSize: number;
  contentType: string;
  partSize: number;
  partCount: number;
  expiresAt: string;
  presignedRequest?: OssPresignedRequest;
  uploadedParts: OssUploadedPart[];
}
export interface OssCompletedPart {
  partNumber: number;
  eTag: string;
}
export interface OssDownloadUrl {
  accessType: 'PUBLIC' | 'PRIVATE';
  url: string;
  expiresAt: string | null;
  fileName: string;
}
export interface OssQuery extends PageQuery {
  fileName: string;
  originalName: string;
  fileSuffix: string;
  createTime: string;
  service: string;
  orderByColumn: string;
  isAsc: string;
}
export interface OssForm {
  file: undefined | string;
}
export interface SysOssExt {
  bizType?: string;
  fileSize?: number;
  contentType?: string;
  source?: string;
  uploadIp?: string;
  remark?: string;
  tags?: string[];
  refId?: string;
  refType?: string;
  isTemp?: boolean;
  md5?: string;
}
export type OssConfigAccessPolicy = 'PRIVATE' | 'PUBLIC_READ';
export interface OssConfigVO extends BaseEntity {
  ossConfigId: ResourceIdentifier;
  configKey: string;
  accessKey: string;
  secretKey: string;
  bucketName: string;
  prefix: string;
  endpoint: string;
  domainUrl: string;
  isHttps: string;
  region: string;
  status: string;
  ext1: string;
  remark: string;
  accessPolicy: OssConfigAccessPolicy;
}
export interface OssConfigQuery extends PageQuery {
  configKey: string;
  bucketName: string;
  status: string;
}
export interface OssConfigForm {
  ossConfigId: ResourceIdentifier | undefined;
  configKey: string;
  accessKey: string;
  secretKey: string;
  bucketName: string;
  prefix: string;
  endpoint: string;
  domainUrl: string;
  isHttps: string;
  accessPolicy: OssConfigAccessPolicy;
  region: string;
  status: string;
  remark: string;
}
