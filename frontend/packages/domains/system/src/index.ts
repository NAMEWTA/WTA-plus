import type { DomainModule } from '@namewta/platform-app-runtime';

export { createSystemService } from './service';
export type { SystemService } from './service';
export {
  projectResetPasswordCandidateTransport,
  projectSystemUserTransport,
  projectTemporaryPasswordTransport
} from './transport';
export type {
  ResetPasswordCandidateTransport,
  SystemUserTransport,
  TemporaryPasswordTransport
} from './transport';
export {
  createOpenApiService,
  groupOpenApiCatalog,
  projectOpenApiCatalog,
  projectOpenApiCatalogItem,
  projectOpenApiCredentialIssued,
  projectOpenApiCredentialSummary,
  projectOpenApiCredentialUserSummary,
  projectOpenApiResponse,
  projectOpenApiUsers,
  resolveOpenApiCredentialState
} from './open-api';
export type {
  CurrentUserOpenApiService,
  OpenApiAccessRule,
  OpenApiCatalogGroup,
  OpenApiCatalogItem,
  OpenApiCredentialCreateInput,
  OpenApiCredentialIssued,
  OpenApiCredentialState,
  OpenApiCredentialStatus,
  OpenApiCredentialSummary,
  OpenApiCredentialUserSummary,
  OpenApiHttpMethod,
  OpenApiIdentifier,
  OpenApiParameter,
  OpenApiParameterLocation,
  OpenApiPermissionRule,
  OpenApiResponse,
  OpenApiRoleRule,
  OpenApiRuleMode,
  OpenApiService,
  OpenApiUserQuery,
  TargetUserOpenApiService
} from './open-api';
export {
  createSystemResourceService,
  ResourceContractError,
  ResourceSecurityError
} from './resource-service';
export type { SystemResourceService } from './resource-service';
export type {
  ConfigForm,
  ConfigQuery,
  ConfigVO,
  DictDataForm,
  DictDataQuery,
  DictDataVO,
  DictTagType,
  DictTypeForm,
  DictTypeQuery,
  DictTypeVO,
  OssCompletedPart,
  OssConfigAccessPolicy,
  OssConfigForm,
  OssConfigQuery,
  OssConfigVO,
  OssDownloadUrl,
  OssForm,
  OssPresignedRequest,
  OssQuery,
  OssSignedPart,
  OssUploadInitRequest,
  OssUploadInitResponse,
  OssUploadMode,
  OssUploadResumeResponse,
  OssUploadState,
  OssUploadedPart,
  OssUploadVO,
  OssVO,
  ResourceIdentifier,
  ResourceIdentifierList,
  SocialAuthVO,
  SocialBindingUrl,
  SysOssExt
} from './resource-types';

export type { ClientForm, ClientQuery, ClientVO } from './client/types';
export type { DeptForm, DeptQuery, DeptTreeVO, DeptVO } from './dept/types';
export type {
  MenuForm,
  MenuQuery,
  MenuTreeOption,
  MenuType,
  MenuVO,
  RoleMenuButtonOption,
  RoleMenuTree
} from './menu/types';
export type { PostForm, PostQuery, PostVO } from './post/types';
export type { DeptTreeOption, RoleDeptTree, RoleForm, RoleQuery, RoleVO } from './role/types';
export type {
  ApiResponse,
  Identifier,
  IdentifierList,
  PageResult,
  RoleUserAssignment,
  RoleUserCancellation,
  UserRoleAssignment
} from './types';
export type { UserTypeForm, UserTypeQuery, UserTypeVO } from './user-type/types';
export type {
  ResetPwdForm,
  ResetPasswordCandidate,
  TemporaryPassword,
  UserForm,
  UserInfo,
  UserInfoVO,
  UserProfileForm,
  UserProfileInfoVO,
  UserQuery,
  UserVO
} from './user/types';

export const systemDomainModule: DomainModule = Object.freeze({
  id: 'system',
  backendModules: Object.freeze(['wta-system']),
  capabilities: Object.freeze([
    'client',
    'user',
    'user-type',
    'role',
    'menu',
    'department',
    'post',
    'dict',
    'config',
    'oss',
    'oss-config',
    'social',
    'monitor-cache',
    'monitor-login-info',
    'monitor-online',
    'monitor-operlog'
  ])
});
