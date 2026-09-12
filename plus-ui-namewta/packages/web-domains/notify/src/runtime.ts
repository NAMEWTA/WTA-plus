import type { createNotificationService } from '@namewta/domain-notify';
import type { NotifyUserCandidatePage, NotifyUserTypeOption } from '@namewta/domain-notify';
import type { Ref } from 'vue';

export interface NotifyDictOption {
  label: string;
  value: string;
  listClass?: string;
}

/** 由宿主组合系统公开目录，通知页面不依赖系统领域的内部实现。 */
export interface NotifyUserDirectory {
  searchUsers: (keyword: string, page: number, pageSize: number) => Promise<{ data: NotifyUserCandidatePage }>;
  usersByIds: (ids: readonly (string | number)[]) => Promise<{ data: NotifyUserCandidatePage['rows'] }>;
  userTypes: () => Promise<{ data: NotifyUserTypeOption[] }>;
}

export interface NotifyWebRuntime {
  /** 宿主推送重连/到达时通知收件箱刷新；返回解绑函数。 */
  subscribeInbox?: (handler: () => void) => () => void;
  /** 收件箱已读状态变更后，请宿主同步消息盒子。 */
  inboxChanged?: () => void;
  service: ReturnType<typeof createNotificationService>;
  hasPermission: (permission: string) => boolean;
  navigate: (path: string) => Promise<void> | void;
  dicts: (...types: string[]) => Record<string, Readonly<Ref<NotifyDictOption[]>>>;
  directory: NotifyUserDirectory;
}
