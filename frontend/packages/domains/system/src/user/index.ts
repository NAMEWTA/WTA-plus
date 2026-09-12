export * from './public';
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
} from './types';

export const systemUserResource = Object.freeze({ controller: 'SysUserController', basePath: '/system/user' });
