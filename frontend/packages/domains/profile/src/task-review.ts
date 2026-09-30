import type { HttpClient } from '@namewta/platform-contracts';
import type { ApiResponse, Identifier, OssAccessUrl, ReviewContext } from './types';

export interface TaskReviewDecision {
  decision: 'APPROVE' | 'RETURN';
  reason: string;
  snapshotVersion: number;
}
export interface ProfileTaskReviewService {
  context(taskId: Identifier): Promise<ApiResponse<ReviewContext>>;
  material(taskId: Identifier, materialRefId: Identifier): Promise<ApiResponse<OssAccessUrl>>;
  decide(taskId: Identifier, input: TaskReviewDecision): Promise<ApiResponse<unknown>>;
}
/** 申请与材料的范围由任务参与关系决定，不复用档案管理的广域读取接口。 */
export function createProfileTaskReviewService(
  http: HttpClient,
  profileType: 'person' | 'enterprise'
): ProfileTaskReviewService {
  const task = (id: Identifier) => `/profile/${profileType}/review/tasks/${encodeURIComponent(String(id))}`;
  return Object.freeze<ProfileTaskReviewService>({
    context: taskId => http.request({ url: task(taskId), method: 'get' }),
    material: (taskId, refId) =>
      http.request({ url: `${task(taskId)}/materials/${encodeURIComponent(String(refId))}/access-url`, method: 'get' }),
    decide: (taskId, data) => http.request({ url: `${task(taskId)}/decision`, method: 'post', data })
  });
}
