import type { Identifier } from '../types';

export type MaterialScope = 'COMMON' | 'ENTERPRISE' | 'PERSON';
export type MaterialNodeType = 'CATEGORY' | 'TAG';

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
