import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import { describe, expect, it } from 'vitest';
import { createProfileService } from './service';
describe('participant review HTTP contract', () => {
  it.each(['person', 'enterprise'] as const)(
    'uses task-scoped %s APIs including material access and frozen snapshot version',
    async kind => {
      const requests: HttpRequest[] = [];
      const http: HttpClient = {
        request: async <T>(request: HttpRequest) => {
          requests.push(request);
          return { data: null } as T;
        }
      };
      const review = createProfileService(http)[kind].taskReview;
      await review.context('task/1');
      await review.material('task/1', 'file/2');
      await review.decide('task/1', { decision: 'RETURN', reason: '请补充材料', snapshotVersion: 3 });
      expect(requests).toEqual([
        { url: `/profile/${kind}/review/tasks/task%2F1`, method: 'get' },
        { url: `/profile/${kind}/review/tasks/task%2F1/materials/file%2F2/access-url`, method: 'get' },
        {
          url: `/profile/${kind}/review/tasks/task%2F1/decision`,
          method: 'post',
          data: { decision: 'RETURN', reason: '请补充材料', snapshotVersion: 3 }
        }
      ]);
    }
  );
});
