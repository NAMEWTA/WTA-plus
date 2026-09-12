import { describe, expect, it } from 'vitest';
import { vi } from 'vitest';
import { mergeUserSelection, normalizeUserIds, prepareUserSelection, serializeCandidateUserIds } from './index';

const user = (userId: string) => ({ userId, nickName: `用户${userId}` });

describe('workflow user selection compatibility', () => {
  it('normalizes scalar and list preselection ids', () => {
    expect(normalizeUserIds('7')).toEqual(['7']);
    expect(normalizeUserIds([7, '8'])).toEqual([7, '8']);
    expect(normalizeUserIds(undefined)).toEqual([]);
    expect(serializeCandidateUserIds(['7', 8])).toBe('7,8');
    expect(serializeCandidateUserIds('7,8')).toBe('7,8');
    expect(serializeCandidateUserIds(undefined)).toBeUndefined();
  });

  it('retains selections from other pages and replaces current-page selection', () => {
    expect(mergeUserSelection([user('1'), user('9')], [user('1'), user('2')], [user('2')], true)).toEqual([
      user('9'),
      user('2')
    ]);
    expect(mergeUserSelection([user('1')], [user('2')], [user('2')], false)).toEqual([user('2')]);
  });

  it('keeps preselection and list limiting independent', async () => {
    const options = vi.fn(async () => ({ data: [user('7')] }));
    const prepared = await prepareUserSelection({ users: { options } } as never, {
      data: ['7'],
      userIds: ['8'],
      modelValue: undefined
    });
    expect(options).toHaveBeenCalledWith(['7']);
    expect(prepared).toEqual({ selected: [user('7')], listUserIds: '8' });

    const model = user('9');
    await expect(
      prepareUserSelection({ users: { options } } as never, { modelValue: model, userIds: ['8'] })
    ).resolves.toEqual({
      selected: [model],
      listUserIds: '8'
    });
    expect(options).toHaveBeenCalledTimes(1);
  });
});
