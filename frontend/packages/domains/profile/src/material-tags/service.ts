import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import type { ApiResponse, Identifier, ProfileType } from '../types';
import type { MaterialNode, MaterialNodeCommand, MaterialRequirement, MaterialScope } from './types';

export interface MaterialTagService {
  requirements(profileType: ProfileType, documentTypeCode: string, handlerIsLegalRepresentative: boolean): Promise<ApiResponse<MaterialRequirement[]>>;
  archive(materialNodeId: Identifier, expectedVersion: number): Promise<ApiResponse<null>>;
  changeStatus(materialNodeId: Identifier, enabled: boolean, expectedVersion: number): Promise<ApiResponse<null>>;
  create(input: MaterialNodeCommand): Promise<ApiResponse<MaterialNode>>;
  tree(scope: MaterialScope, includeDisabled?: boolean): Promise<ApiResponse<MaterialNode[]>>;
  update(materialNodeId: Identifier, input: MaterialNodeCommand): Promise<ApiResponse<MaterialNode>>;
}

const segment = (value: Identifier) => encodeURIComponent(String(value));

export function createMaterialTagService(http: HttpClient): MaterialTagService {
  const request = <T>(config: HttpRequest) => http.request<ApiResponse<T>>(config);
  return Object.freeze<MaterialTagService>({
    requirements: (profileType, documentTypeCode, handlerIsLegalRepresentative) =>
      request({ url: '/profile/material-tags/requirements', method: 'get', params: { profileType, documentTypeCode, handlerIsLegalRepresentative } }),
    tree: (scope, includeDisabled = false) =>
      request({ url: '/profile/material-tags/tree', method: 'get', params: { scope, includeDisabled } }),
    create: data => request({ url: '/profile/material-tags', method: 'post', data }),
    update: (materialNodeId, data) =>
      request({ url: `/profile/material-tags/${segment(materialNodeId)}`, method: 'post', data }),
    changeStatus: (materialNodeId, enabled, expectedVersion) =>
      request({
        url: `/profile/material-tags/${segment(materialNodeId)}/status`,
        method: 'post',
        data: { enabled, expectedVersion }
      }),
    archive: (materialNodeId, expectedVersion) =>
      request({
        url: `/profile/material-tags/${segment(materialNodeId)}/archive`,
        method: 'post',
        data: { expectedVersion }
      })
  });
}
