import type { UserSummary, WorkflowDefinitionService } from '@namewta/domain-workflow';

type IdInput = string | number | readonly (string | number)[] | undefined;

export const normalizeUserIds = (input: IdInput): (string | number)[] =>
  input === undefined ? [] : Array.isArray(input) ? [...input] : [input as string | number];

export const serializeCandidateUserIds = (input: IdInput): string | undefined => {
  const values = normalizeUserIds(input)
    .flatMap(value => String(value).split(','))
    .filter(Boolean);
  return values.length ? values.join(',') : undefined;
};

export const mergeUserSelection = (
  retained: readonly UserSummary[],
  page: readonly UserSummary[],
  pageSelection: readonly UserSummary[],
  multiple: boolean
): UserSummary[] => {
  if (!multiple) return pageSelection.slice(-1);
  const pageIds = new Set(page.map(user => String(user.userId)));
  const merged = new Map(
    retained.filter(user => !pageIds.has(String(user.userId))).map(user => [String(user.userId), user])
  );
  for (const user of pageSelection) merged.set(String(user.userId), user);
  return [...merged.values()];
};

export async function prepareUserSelection(
  service: WorkflowDefinitionService,
  input: {
    data?: IdInput;
    modelValue?: UserSummary | UserSummary[];
    userIds?: IdInput;
  }
): Promise<{ listUserIds: string | undefined; selected: UserSummary[] }> {
  const model = input.modelValue
    ? Array.isArray(input.modelValue)
      ? [...input.modelValue]
      : [input.modelValue as UserSummary]
    : [];
  const preselectedIds = normalizeUserIds(input.data);
  const selected = model.length || !preselectedIds.length ? model : (await service.users.options(preselectedIds)).data;
  return { selected, listUserIds: serializeCandidateUserIds(input.userIds) };
}
