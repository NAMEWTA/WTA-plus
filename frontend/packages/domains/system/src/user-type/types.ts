import type { BaseEntity, PageQuery } from '../types';

export interface UserTypeVO extends BaseEntity {
  /**
   * 登录域ID
   */
  userTypeId: string | number;

  /**
   * 登录域编码
   */
  userTypeCode: string;

  /**
   * 登录域名称
   */
  userTypeName: string;

  /**
   * 状态（0正常 1停用）
   */
  status: string;

  /**
   * 显示顺序
   */
  orderNum: number;

  /**
   * 备注
   */
  remark?: string;
}

export interface UserTypeForm extends BaseEntity {
  /**
   * 登录域ID
   */
  userTypeId?: string | number;

  /**
   * 登录域编码
   */
  userTypeCode?: string;

  /**
   * 登录域名称
   */
  userTypeName?: string;

  /**
   * 状态（0正常 1停用）
   */
  status?: string;

  /**
   * 显示顺序
   */
  orderNum?: number;

  /**
   * 备注
   */
  remark?: string;
}

export interface UserTypeQuery extends PageQuery {
  /**
   * 登录域编码
   */
  userTypeCode?: string;

  /**
   * 登录域名称
   */
  userTypeName?: string;

  /**
   * 状态（0正常 1停用）
   */
  status?: string;
}
