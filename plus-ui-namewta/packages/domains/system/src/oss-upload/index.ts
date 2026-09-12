export type {
  OssCompletedPart,
  OssPresignedRequest,
  OssSignedPart,
  OssUploadedPart,
  OssUploadInitRequest,
  OssUploadInitResponse,
  OssUploadMode,
  OssUploadResumeResponse,
  OssUploadState
} from './types';

export const systemOssUploadResource = Object.freeze({
  controller: 'SysOssUploadController',
  basePath: '/resource/oss/uploads'
});
