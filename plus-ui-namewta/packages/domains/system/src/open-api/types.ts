export type OpenApiIdentifier = string | number;
export type OpenApiCredentialStatus = '0' | '1';
export type OpenApiCredentialState = 'disabled' | 'enabled' | 'expired';
export type OpenApiHttpMethod = 'DELETE' | 'GET' | 'HEAD' | 'OPTIONS' | 'PATCH' | 'POST' | 'PUT' | 'TRACE';
export type OpenApiRuleMode = 'AND' | 'OR';
export type OpenApiParameterLocation = 'header' | 'path' | 'query';

export interface OpenApiResponse<T> {
  readonly code: number;
  readonly data: T;
  readonly msg?: string;
}

export interface OpenApiCredentialCreateInput {
  readonly appName: string;
  readonly expiresAt?: string | null;
  readonly remark?: string | null;
}

export interface OpenApiCredentialSummary {
  readonly credentialId: OpenApiIdentifier;
  readonly ownerUserId: OpenApiIdentifier;
  readonly appKey: string;
  readonly appName: string;
  readonly status: OpenApiCredentialStatus;
  readonly expiresAt: string | null;
  readonly remark: string | null;
  readonly createTime: string | null;
  readonly updateTime: string | null;
}

/** A command-only value. Never retain this object in a store or cache. */
export interface OpenApiCredentialIssued extends OpenApiCredentialSummary {
  readonly appSecret: string;
}

export interface OpenApiCredentialUserSummary {
  readonly userId: OpenApiIdentifier;
  readonly userName: string;
  readonly nickName: string;
  readonly credential: OpenApiCredentialSummary | null;
}

export interface OpenApiUserQuery {
  readonly keyword?: string;
  readonly limit?: number;
}

export interface OpenApiPermissionRule {
  readonly values: readonly string[];
  readonly mode: OpenApiRuleMode;
  readonly orRoles: readonly string[];
}

export interface OpenApiRoleRule {
  readonly values: readonly string[];
  readonly mode: OpenApiRuleMode;
}

export interface OpenApiAccessRule {
  readonly permissions: readonly OpenApiPermissionRule[];
  readonly roles: readonly OpenApiRoleRule[];
}

export interface OpenApiParameter {
  readonly name: string;
  readonly location: OpenApiParameterLocation;
  readonly required: boolean;
  readonly schema: string;
}

export interface OpenApiCatalogItem {
  readonly interfaceId: string;
  readonly summary: string;
  readonly method: OpenApiHttpMethod;
  readonly path: string;
  readonly accessRule: OpenApiAccessRule;
  readonly parameters: readonly OpenApiParameter[];
  readonly requestSchema: string | null;
  readonly responseSchema: string;
  readonly curlExample: string;
  readonly javaExample: string;
}

export interface OpenApiCatalogGroup {
  readonly key: string;
  readonly label: string;
  readonly items: readonly OpenApiCatalogItem[];
}

export interface CurrentUserOpenApiService {
  getCredential(): Promise<OpenApiResponse<OpenApiCredentialSummary>>;
  createCredential(input: OpenApiCredentialCreateInput): Promise<OpenApiResponse<OpenApiCredentialIssued>>;
  resetCredential(): Promise<OpenApiResponse<OpenApiCredentialIssued>>;
  enableCredential(): Promise<OpenApiResponse<OpenApiCredentialSummary>>;
  disableCredential(): Promise<OpenApiResponse<OpenApiCredentialSummary>>;
  deleteCredential(): Promise<OpenApiResponse<null>>;
  listInterfaces(): Promise<OpenApiResponse<readonly OpenApiCatalogItem[]>>;
  getInterface(interfaceId: string): Promise<OpenApiResponse<OpenApiCatalogItem>>;
}

export interface TargetUserOpenApiService {
  listUsers(query?: OpenApiUserQuery): Promise<OpenApiResponse<readonly OpenApiCredentialUserSummary[]>>;
  getCredential(userId: OpenApiIdentifier): Promise<OpenApiResponse<OpenApiCredentialSummary>>;
  createCredential(
    userId: OpenApiIdentifier,
    input: OpenApiCredentialCreateInput
  ): Promise<OpenApiResponse<OpenApiCredentialIssued>>;
  resetCredential(userId: OpenApiIdentifier): Promise<OpenApiResponse<OpenApiCredentialIssued>>;
  enableCredential(userId: OpenApiIdentifier): Promise<OpenApiResponse<OpenApiCredentialSummary>>;
  disableCredential(userId: OpenApiIdentifier): Promise<OpenApiResponse<OpenApiCredentialSummary>>;
  deleteCredential(userId: OpenApiIdentifier): Promise<OpenApiResponse<null>>;
  listInterfaces(userId: OpenApiIdentifier): Promise<OpenApiResponse<readonly OpenApiCatalogItem[]>>;
  getInterface(userId: OpenApiIdentifier, interfaceId: string): Promise<OpenApiResponse<OpenApiCatalogItem>>;
}

export interface OpenApiService {
  readonly currentUser: CurrentUserOpenApiService;
  readonly targetUser: TargetUserOpenApiService;
}
