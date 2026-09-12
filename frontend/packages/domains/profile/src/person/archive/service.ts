import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import type {
  AccountCandidate,
  ApiResponse,
  AssignCommand,
  BindingCommand,
  DecisionCommand,
  Identifier,
  OssAccessUrl,
  PageResult,
  ProfileCommandResult,
  ReviewContext,
  RevokeCommand
} from '../../types';
import type {
  PersonArchiveQuery,
  PersonCreateCommand,
  PersonProfileDetail,
  PersonProfileSummary,
  PersonReviseCommand
} from './types';

export interface PersonArchiveService {
  assign(profileId: Identifier, input: AssignCommand): Promise<ApiResponse<ProfileCommandResult>>;
  create(input: PersonCreateCommand): Promise<ApiResponse<ProfileCommandResult>>;
  decide(applicationId: Identifier, input: DecisionCommand): Promise<ApiResponse<ProfileCommandResult>>;
  detail(profileId: Identifier): Promise<ApiResponse<PersonProfileDetail>>;
  eligibleUsers(keyword: string): Promise<ApiResponse<AccountCandidate[]>>;
  manageBinding(profileId: Identifier, input: BindingCommand): Promise<ApiResponse<ProfileCommandResult>>;
  material(profileId: Identifier, materialRefId: Identifier): Promise<ApiResponse<OssAccessUrl>>;
  page(query: PersonArchiveQuery): Promise<ApiResponse<PageResult<PersonProfileSummary>>>;
  review(applicationId: Identifier): Promise<ApiResponse<ReviewContext>>;
  reviewMaterial(applicationId: Identifier, materialRefId: Identifier): Promise<ApiResponse<OssAccessUrl>>;
  revise(profileId: Identifier, input: PersonReviseCommand): Promise<ApiResponse<ProfileCommandResult>>;
  revoke(profileId: Identifier, input: RevokeCommand): Promise<ApiResponse<ProfileCommandResult>>;
}

const segment = (value: Identifier) => encodeURIComponent(String(value));

export function createPersonArchiveService(http: HttpClient): PersonArchiveService {
  const request = <T>(config: HttpRequest) => http.request<ApiResponse<T>>(config);
  const base = '/profile/person/archive';
  return Object.freeze<PersonArchiveService>({
    page: query => request({ url: base, method: 'get', params: query }),
    eligibleUsers: keyword => request({ url: `${base}/eligible-users`, method: 'get', params: { keyword } }),
    detail: profileId => request({ url: `${base}/${segment(profileId)}`, method: 'get' }),
    review: applicationId =>
      request({ url: `${base}/application/${segment(applicationId)}/review-context`, method: 'get' }),
    reviewMaterial: (applicationId, materialRefId) =>
      request({
        url: `${base}/application/${segment(applicationId)}/material/${segment(materialRefId)}/access-url`,
        method: 'get'
      }),
    material: (profileId, materialRefId) =>
      request({ url: `${base}/${segment(profileId)}/material/${segment(materialRefId)}/access-url`, method: 'get' }),
    decide: (applicationId, data) =>
      request({ url: `${base}/application/${segment(applicationId)}/decision`, method: 'post', data }),
    create: data => request({ url: `${base}/admin-create`, method: 'post', data }),
    revise: (profileId, data) => request({ url: `${base}/${segment(profileId)}/revision`, method: 'post', data }),
    assign: (profileId, data) => request({ url: `${base}/${segment(profileId)}/assign`, method: 'post', data }),
    manageBinding: (profileId, data) => request({ url: `${base}/${segment(profileId)}/binding`, method: 'post', data }),
    revoke: (profileId, data) => request({ url: `${base}/${segment(profileId)}/revoke`, method: 'post', data })
  });
}
