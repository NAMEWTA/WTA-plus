import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import type {
  ApiResponse,
  Identifier,
  MaterialInput,
  MaterialOwnerType,
  MaterialReference,
  OssAccessUrl
} from '../../types';

export interface EnterpriseMaterialService {
  accessUrl(
    ownerType: MaterialOwnerType,
    ownerId: Identifier,
    materialRefId: Identifier
  ): Promise<ApiResponse<OssAccessUrl>>;
  attach(
    ownerType: MaterialOwnerType,
    ownerId: Identifier,
    input: MaterialInput
  ): Promise<ApiResponse<MaterialReference>>;
  detach(ownerType: MaterialOwnerType, ownerId: Identifier, materialRefId: Identifier): Promise<ApiResponse<null>>;
  list(ownerType: MaterialOwnerType, ownerId: Identifier): Promise<ApiResponse<MaterialReference[]>>;
}

const segment = (value: Identifier) => encodeURIComponent(String(value));

export function createEnterpriseMaterialService(http: HttpClient): EnterpriseMaterialService {
  const request = <T>(config: HttpRequest) => http.request<ApiResponse<T>>(config);
  const ownerPath = (ownerType: MaterialOwnerType, ownerId: Identifier) =>
    `/profile/enterprise/materials/${segment(ownerType)}/${segment(ownerId)}`;
  return Object.freeze<EnterpriseMaterialService>({
    list: (ownerType, ownerId) => request({ url: ownerPath(ownerType, ownerId), method: 'get' }),
    attach: (ownerType, ownerId, data) => request({ url: ownerPath(ownerType, ownerId), method: 'post', data }),
    detach: (ownerType, ownerId, materialRefId) =>
      request({ url: `${ownerPath(ownerType, ownerId)}/${segment(materialRefId)}/detach`, method: 'post' }),
    accessUrl: (ownerType, ownerId, materialRefId) =>
      request({ url: `${ownerPath(ownerType, ownerId)}/${segment(materialRefId)}/access-url`, method: 'get' })
  });
}
