import type { HttpClient } from '@namewta/platform-contracts';

export interface PublicDictOption {
  label: string;
  value: string;
}

export interface PublicDictType {
  name: string;
  type: string;
}

export interface DictQueryPort {
  get(type: string): Promise<PublicDictOption[]>;
}

export interface DictTypeCatalogPort {
  list(clientId?: string | number): Promise<PublicDictType[]>;
}

export function createDictQueryPort(http: HttpClient): DictQueryPort {
  return Object.freeze({
    async get(type: string) {
      const response = await http.request<{ data: Array<{ dictLabel: string; dictValue: string }> }>({
        url: '/system/dict/data/type/' + encodeURIComponent(type),
        method: 'get'
      });
      return response.data.map(item => ({ label: item.dictLabel, value: item.dictValue }));
    }
  });
}

export function createDictTypeCatalogPort(http: HttpClient): DictTypeCatalogPort {
  return Object.freeze({
    async list(clientId) {
      const response = await http.request<{ data: Array<{ dictName: string; dictType: string }> }>({
        url: '/system/dict/type/optionselect',
        method: 'get',
        params: { clientId }
      });
      return response.data.map(item => ({ name: item.dictName, type: item.dictType }));
    }
  });
}
