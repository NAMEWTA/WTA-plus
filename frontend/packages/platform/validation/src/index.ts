export type ValidationFormat =
  | 'EMAIL'
  | 'MAINLAND_MOBILE'
  | 'TELEPHONE'
  | 'E164_PHONE'
  | 'MAINLAND_ID_CARD'
  | 'HK_RESIDENT_ID'
  | 'MO_RESIDENT_ID'
  | 'TW_RESIDENT_ID'
  | 'HK_MACAO_RESIDENCE_PERMIT'
  | 'TW_RESIDENCE_PERMIT'
  | 'MAINLAND_TRAVEL_PERMIT_HK_MACAO'
  | 'MAINLAND_TRAVEL_PERMIT_TW'
  | 'UNIFIED_SOCIAL_CREDIT_CODE';

export interface ValidationIssue {
  code: string;
  args: Readonly<Record<string, string | number>>;
  field?: string;
}

export interface ValidationResult {
  valid: boolean;
  issue?: ValidationIssue;
  violations?: readonly ValidationIssue[];
}

const result = (code?: string, args: Readonly<Record<string, string | number>> = {}): ValidationResult =>
  (code ? { valid: false, issue: { code, args }, violations: [{ code, args }] } : { valid: true, violations: [] });
const match = (value: string, pattern: RegExp, code: string) => result(pattern.test(value) ? undefined : code);
const email = /^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/;
const mobile = /^1[3-9]\d{9}$/;
const telephone = /^(?:0\d{2,3}-?\d{7,8}|\d{7,8})$/;
const e164 = /^\+[1-9]\d{6,14}$/;
const creditChars = '0123456789ABCDEFGHJKLMNPQRTUWXY';
const creditWeights = [1, 3, 9, 7, 1, 3, 9, 7, 1, 3, 9, 7, 1, 3, 9, 7, 1];

function mainlandIdCard(value: string): ValidationResult {
  if (!/^\d{17}[0-9Xx]$/.test(value)) return result('validation.idCard.mainland.format');
  const date = value.slice(6, 14);
  const year = Number(date.slice(0, 4));
  const month = Number(date.slice(4, 6));
  const day = Number(date.slice(6, 8));
  const parsed = new Date(Date.UTC(year, month - 1, day));
  if (parsed.getUTCFullYear() !== year || parsed.getUTCMonth() !== month - 1 || parsed.getUTCDate() !== day) {
    return result('validation.idCard.mainland.date');
  }
  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
  const checks = '10X98765432';
  const sum = Array.from(value.slice(0, 17)).reduce((total, char, index) => total + Number(char) * weights[index], 0);
  return result(checks[sum % 11] === value[17].toUpperCase() ? undefined : 'validation.idCard.mainland.checksum');
}

function unifiedCreditCode(value: string): ValidationResult {
  if (!/^[0-9A-Z]{18}$/.test(value)) return result('validation.creditCode.format');
  const indexes = Array.from(value.slice(0, 17)).map(char => creditChars.indexOf(char));
  if (indexes.some(index => index < 0)) return result('validation.creditCode.character');
  const sum = indexes.reduce((total, index, position) => total + index * creditWeights[position], 0);
  const check = (31 - (sum % 31)) % 31;
  return result(creditChars[check] === value[17] ? undefined : 'validation.creditCode.checksum');
}

export function validateFormat(value: string | null | undefined, format: ValidationFormat): ValidationResult {
  if (value == null || value.trim() === '') return { valid: true };
  const normalized = value.trim();
  switch (format) {
    case 'EMAIL': return match(normalized, email, 'validation.email.invalid');
    case 'MAINLAND_MOBILE': return match(normalized, mobile, 'validation.phone.mobile.invalid');
    case 'TELEPHONE': return match(normalized, telephone, 'validation.phone.telephone.invalid');
    case 'E164_PHONE': return match(normalized, e164, 'validation.phone.e164.invalid');
    case 'MAINLAND_ID_CARD': return mainlandIdCard(normalized);
    case 'HK_RESIDENT_ID': return match(normalized.toUpperCase(), /^[A-Z]{1,2}\d{6}\([0-9A]\)$/, 'validation.idCard.hk.invalid');
    case 'MO_RESIDENT_ID': return match(normalized.toUpperCase(), /^[157]\d{6}\([0-9A]\)$/, 'validation.idCard.mo.invalid');
    case 'TW_RESIDENT_ID': return match(normalized.toUpperCase(), /^[A-Z][12]\d{8}$/, 'validation.idCard.tw.invalid');
    case 'HK_MACAO_RESIDENCE_PERMIT': return match(normalized.toUpperCase(), /^8\d{7}[0-9X]$/, 'validation.permit.hkMacao.invalid');
    case 'TW_RESIDENCE_PERMIT': return match(normalized.toUpperCase(), /^[A-Z0-9]{8,10}$/, 'validation.permit.tw.invalid');
    case 'MAINLAND_TRAVEL_PERMIT_HK_MACAO': return match(normalized.toUpperCase(), /^[HM]\d{8}$/, 'validation.travelPermit.hkMacao.invalid');
    case 'MAINLAND_TRAVEL_PERMIT_TW': return match(normalized.toUpperCase(), /^[A-Z0-9]{8}$/, 'validation.travelPermit.tw.invalid');
    case 'UNIFIED_SOCIAL_CREDIT_CODE': return unifiedCreditCode(normalized.toUpperCase());
  }
}

export const isValidFormat = (value: string | null | undefined, format: ValidationFormat): boolean =>
  validateFormat(value, format).valid;

export function toValidator(format: ValidationFormat, translate: (code: string, args: Readonly<Record<string, string | number>>) => string) {
  return (_rule: unknown, value: unknown, callback: (error?: Error) => void): void => {
    const check = validateFormat(typeof value === 'string' ? value : '', format);
    callback(check.valid ? undefined : new Error(translate(check.issue!.code, check.issue!.args)));
  };
}

export interface PasswordPolicy {
  minimumLength: number;
  maximumLength: number;
  allowedSpecialCharacters: string;
  requiredCharacterClasses?: readonly ('UPPERCASE' | 'LOWERCASE' | 'DIGIT' | 'SPECIAL')[];
  requireUppercase?: boolean;
  requireLowercase?: boolean;
  requireDigit?: boolean;
  requireSpecial?: boolean;
}

export type PasswordViolationReason =
  | 'PASSWORD_TOO_SHORT'
  | 'PASSWORD_TOO_LONG'
  | 'PASSWORD_MISSING_UPPERCASE'
  | 'PASSWORD_MISSING_LOWERCASE'
  | 'PASSWORD_MISSING_DIGIT'
  | 'PASSWORD_MISSING_SPECIAL'
  | 'PASSWORD_CONTAINS_DISALLOWED_CHARACTER';

export interface PasswordValidationIssue extends ValidationIssue {
  reason: PasswordViolationReason;
}

export function validatePassword(policy: PasswordPolicy | null | undefined, password: string | null | undefined): readonly PasswordValidationIssue[] {
  if (!policy) return [{ code: 'validation.password.policy.unavailable', args: {}, reason: 'PASSWORD_TOO_SHORT' }];
  const value = password ?? '';
  const required = new Set(policy.requiredCharacterClasses ?? [
    ...(policy.requireUppercase ? ['UPPERCASE' as const] : []),
    ...(policy.requireLowercase ? ['LOWERCASE' as const] : []),
    ...(policy.requireDigit ? ['DIGIT' as const] : []),
    ...(policy.requireSpecial ? ['SPECIAL' as const] : [])
  ]);
  const issues: PasswordValidationIssue[] = [];
  const add = (reason: PasswordViolationReason, code: string, args: Readonly<Record<string, string | number>> = {}) =>
    issues.push({ code, args, reason });
  if (value.length < policy.minimumLength) add('PASSWORD_TOO_SHORT', 'validation.password.tooShort', { min: policy.minimumLength });
  if (value.length > policy.maximumLength) add('PASSWORD_TOO_LONG', 'validation.password.tooLong', { max: policy.maximumLength });
  if (required.has('UPPERCASE') && !/[A-Z]/.test(value)) add('PASSWORD_MISSING_UPPERCASE', 'validation.password.missingUppercase');
  if (required.has('LOWERCASE') && !/[a-z]/.test(value)) add('PASSWORD_MISSING_LOWERCASE', 'validation.password.missingLowercase');
  if (required.has('DIGIT') && !/\d/.test(value)) add('PASSWORD_MISSING_DIGIT', 'validation.password.missingDigit');
  if (required.has('SPECIAL') && ![...value].some(ch => policy.allowedSpecialCharacters.includes(ch))) add('PASSWORD_MISSING_SPECIAL', 'validation.password.missingSpecial', { specials: policy.allowedSpecialCharacters });
  if ([...value].some(ch => !/[A-Za-z0-9]/.test(ch) && !policy.allowedSpecialCharacters.includes(ch))) add('PASSWORD_CONTAINS_DISALLOWED_CHARACTER', 'validation.password.disallowedCharacter', { specials: policy.allowedSpecialCharacters });
  return Object.freeze(issues);
}

export const toElementPlusValidator = toValidator;
