import type { BaseEntity, PageQuery } from '../types';

export interface ClientVO {
  /**
   * id
   */
  id: string | number;

  /**
   * 客户端id
   */
  clientId: string;

  /**
   * 客户端key
   */
  clientKey: string;

  /**
   * 客户端秘钥
   */
  clientSecret: string;

  /**
   * 授权类型
   */
  grantTypeList: string[];

  /**
   * 设备类型
   */
  deviceType: string;

  /**
   * 允许访问路径
   */
  accessPath?: string;

  /**
   * 允许访问路径列表
   */
  accessPathList?: string[];

  /**
   * IP白名单
   */
  ipWhitelist?: string;

  /**
   * IP白名单列表
   */
  ipWhitelistList?: string[];

  /**
   * token活跃超时时间
   */
  activeTimeout: number;

  /**
   * token固定超时
   */
  timeout: number;

  /**
   * 状态（0正常 1停用）
   */
  status: string;

  /**
   * 登录域ID
   */
  userTypeId: string | number;

  /**
   * 登录域编码
   */
  userTypeCode?: string;

  /**
   * 登录域名称
   */
  userTypeName?: string;

  /**
   * 公开注册开关。true 开放，false 关闭（与 SysClientVo.registerEnabled 布尔值一致）
   */
  registerEnabled: boolean;

  /**
   * 默认角色ID
   */
  defaultRoleId?: string | number;

  /**
   * 默认角色名称
   */
  defaultRoleName?: string;
}

export interface ClientForm extends BaseEntity {
  /**
   * id
   */
  id?: string | number;

  /**
   * 客户端id
   */
  clientId?: string | number;

  /**
   * 客户端key
   */
  clientKey?: string;

  /**
   * 客户端秘钥
   */
  clientSecret?: string;

  /**
   * 授权类型
   */
  grantTypeList?: string[];

  /**
   * 设备类型
   */
  deviceType?: string;

  /**
   * 允许访问路径
   */
  accessPath?: string;

  /**
   * 允许访问路径列表
   */
  accessPathList?: string[];

  /**
   * IP白名单
   */
  ipWhitelist?: string;

  /**
   * IP白名单列表
   */
  ipWhitelistList?: string[];

  /**
   * token活跃超时时间
   */
  activeTimeout?: number;

  /**
   * token固定超时
   */
  timeout?: number;

  /**
   * 状态（0正常 1停用）
   */
  status?: string;

  /**
   * 登录域ID
   */
  userTypeId?: string | number;

  /**
   * 公开注册开关。true 开放，false 关闭
   */
  registerEnabled?: boolean;

  /**
   * 默认角色ID
   */
  defaultRoleId?: string | number;
}

export interface ClientQuery extends PageQuery {
  /**
   * 客户端id
   */
  clientId?: string | number;

  /**
   * 客户端key
   */
  clientKey?: string;

  /**
   * 客户端秘钥
   */
  clientSecret?: string;

  /**
   * 授权类型
   */
  grantType?: string;

  /**
   * 设备类型
   */
  deviceType?: string;

  /**
   * 允许访问路径
   */
  accessPath?: string;

  /**
   * IP白名单
   */
  ipWhitelist?: string;

  /**
   * token活跃超时时间
   */
  activeTimeout?: number;

  /**
   * token固定超时
   */
  timeout?: number;

  /**
   * 状态（0正常 1停用）
   */
  status?: string;

  /**
   * 登录域ID
   */
  userTypeId?: string | number;
}
