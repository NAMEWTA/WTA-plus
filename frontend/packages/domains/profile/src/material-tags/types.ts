import type { Identifier } from '../types';

export type MaterialScope = 'COMMON' | 'ENTERPRISE' | 'PERSON';
export type MaterialNodeType = 'CATEGORY' | 'TAG';

/** 服务端按证件和经办条件计算的材料要求；提交仍由服务端最终校验。 */
export interface MaterialRequirement {
  materialTagCode: string;
  minimumCount: number;
}

export interface MaterialNode {
  children: MaterialNode[];
  enabled: boolean;
  materialNodeId: Identifier;
  materialTagCode: string | null;
  nodeDepth: number;
  nodeName: string;
  nodeType: MaterialNodeType;
  orderNum: number;
  parentId: Identifier;
  scope: MaterialScope;
  systemRequired: boolean;
  version: number;
}

export interface MaterialNodeCommand {
  expectedVersion: number;
  materialTagCode: string | null;
  nodeName: string;
  nodeType: MaterialNodeType;
  orderNum: number;
  parentId: Identifier;
  scope: MaterialScope;
  systemRequired: boolean;
}
