import type { HttpClient } from '@namewta/platform-contracts';

export interface PublicMenuOption {
  id: string | number;
  label: string;
  children?: PublicMenuOption[];
}

export interface MenuQueryPort {
  options(clientId?: string | number): Promise<PublicMenuOption[]>;
}

function menuRecord(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('菜单响应不可用');
  return value as Record<string, unknown>;
}

function projectMenuOption(value: unknown): PublicMenuOption {
  const option = menuRecord(value);
  const { id, label } = option;
  if ((!(typeof id === 'string' && id.trim()) && !(typeof id === 'number' && Number.isFinite(id))) || typeof label !== 'string') {
    throw new Error('菜单响应不可用');
  }
  if (option.children === undefined || option.children === null) return { id, label };
  if (!Array.isArray(option.children)) throw new Error('菜单响应不可用');
  return { id, label, children: option.children.map(projectMenuOption) };
}

export function createMenuQueryPort(http: HttpClient): MenuQueryPort {
  return Object.freeze<MenuQueryPort>({
    async options(clientId) {
      const response = menuRecord(await http.request<unknown>({
        url: '/system/menu/treeselect',
        method: 'get',
        params: { clientId }
      }));
      if (!Array.isArray(response.data)) throw new Error('菜单响应不可用');
      return response.data.map(projectMenuOption);
    }
  });
}
