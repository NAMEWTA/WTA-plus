export type { OssConfigForm, OssConfigQuery, OssConfigVO } from '../resource-types';

export type OssDiagnosticObservation = 'ALLOWED' | 'DENIED' | 'UNKNOWN';
export type OssDiagnosticSubject =
  | 'POLICY_READ' | 'POLICY_WRITE' | 'ACL_LIST' | 'ACL_WRITE_RISK' | 'OBJECT_HEAD' | 'OBJECT_GET';
export type OssDiagnosticSource = 'BUCKET_POLICY' | 'BUCKET_ACL' | 'ANONYMOUS_HEAD' | 'ANONYMOUS_GET';
export type OssDiagnosticScope = 'BUCKET' | 'OBJECT';
export type OssDiagnosticBasis =
  | 'POLICY_ALLOW' | 'POLICY_DENY' | 'NO_SUCH_POLICY' | 'POLICY_UNREADABLE' | 'COMPLEX_POLICY'
  | 'INVALID_POLICY' | 'ACL_GRANT' | 'ACL_NO_GRANT' | 'ACL_UNREADABLE' | 'HTTP_SUCCESS'
  | 'HTTP_DENIED' | 'HTTP_NOT_FOUND' | 'REDIRECT' | 'HTTP_ERROR' | 'TIMEOUT'
  | 'NETWORK_ERROR' | 'INTERRUPTED' | 'NOT_EVALUATED' | 'UNSUPPORTED';

export interface OssDiagnosticFact {
  subject: OssDiagnosticSubject;
  observation: OssDiagnosticObservation;
  source: OssDiagnosticSource;
  scope: OssDiagnosticScope;
  basis: OssDiagnosticBasis;
  observedAt: string;
}

export interface OssStorageDiagnostic {
  status: 'SERVING' | 'NOT_SERVING';
  reason:
    | 'READY' | 'CONFIG_MISSING' | 'INVALID_ACCESS_POLICY' | 'DOMAIN_REQUIRED'
    | 'DIAGNOSTIC_OBJECT_MISSING' | 'DIAGNOSTIC_CONFIG_INVALID' | 'DIAGNOSTIC_UNVERIFIED'
    | 'PROVIDER_MISMATCH' | 'DISCOVERY_FAILED' | 'STALE';
  checkedAt: string;
  facts: readonly OssDiagnosticFact[];
}
