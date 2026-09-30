import type { CertificationStatus } from '@namewta/domain-profile';
export const certificationLabel = (status: CertificationStatus | '') =>
  status === ''
    ? ''
    : ({ UNVERIFIED: '未认证', DRAFT: '草稿', WAITING: '审核中', BACK: '已退回', CANCEL: '已撤回', VERIFIED: '已认证' }[
        status
      ] ?? status);
export const certificationAction = (status: CertificationStatus) =>
  status === 'VERIFIED'
    ? '查看认证资料'
    : status === 'WAITING'
      ? '查看申请'
      : status === 'DRAFT' || status === 'BACK' || status === 'CANCEL'
        ? '继续填写'
        : '开始认证';
