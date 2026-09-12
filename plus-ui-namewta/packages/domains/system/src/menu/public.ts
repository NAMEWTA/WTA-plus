import type { HttpClient } from '@namewta/platform-contracts';

export interface PublicMenuOption {
  id: string | number;
  label: string;
  children?: PublicMenuOption[];
}

export interface MenuQueryPort {
  options(clientId?: string | number): Promise<PublicMenuOption[]>;
}

interface PublicMenuSource extends PublicMenuOption {
  children?: PublicMenuSource[];
}

function projectMenuOption(option: PublicMenuSource): PublicMenuOption {
  const children = option.children?.map(projectMenuOption);
  return children ? { id: option.id, label: option.label, children } : { id: option.id, label: option.label };
}

export function createMenuQueryPort(http: HttpClient): MenuQueryPort {
  return Object.freeze({
    async options(clientId) {
      const response = await http.request<{ data: PublicMenuSource[] }>({
        url: '/system/menu/treeselect',
        method: 'get',
        params: { clientId }
      });
      return response.data.map(projectMenuOption);
    }
  });
}
