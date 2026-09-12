import type { DomainModule } from '@namewta/platform-app-runtime';
import type { ApiErrorInfo, HttpClient } from '@namewta/platform-contracts';
import { projectDemoTransport, type DemoTransport } from './transport';
import { createRichTextService, type RichTextService } from './rich-text';
export * from './rich-text';

export * from './transport';

export interface AuditFields {
  createBy?: unknown;
  createDept?: unknown;
  createTime?: string;
  updateBy?: unknown;
  updateTime?: unknown;
}

export interface PageQuery {
  pageNum: number;
  pageSize: number;
}

export interface PageResult<T> {
  rows: T[];
  total: number;
}

export interface ApiResponse<T = unknown> {
  code?: number;
  data?: T;
  msg?: string;
  error?: ApiErrorInfo;
}

export interface DemoVO {
  deptId: string | number;
  id: string | number;
  orderNum: number;
  testKey: string;
  userId: string | number;
  value: string;
}

export interface DemoForm extends AuditFields {
  deptId?: string | number;
  id?: string | number;
  orderNum?: number;
  testKey?: string;
  userId?: string | number;
  value?: string;
}

export interface DemoQuery extends PageQuery {
  deptId?: string | number;
  orderNum?: number;
  testKey?: string;
  userId?: string | number;
  value?: string;
}

export interface TreeVO {
  children: TreeVO[];
  deptId: string | number;
  id: string | number;
  parentId: string | number;
  treeName: string;
  userId: string | number;
}

export interface TreeForm extends AuditFields {
  deptId?: string | number;
  id?: string | number;
  parentId?: string | number;
  treeName?: string;
  userId?: string | number;
}

export interface TreeQuery {
  deptId?: string | number;
  parentId?: string | number;
  treeName?: string;
  userId?: string | number;
}

export interface DemoService {
  richText: RichTextService;
  addDemo(data: DemoForm): Promise<ApiResponse>;
  addTree(data: TreeForm): Promise<ApiResponse>;
  deleteDemo(id: string | number | Array<string | number>): Promise<ApiResponse>;
  deleteTree(id: string | number | Array<string | number>): Promise<ApiResponse>;
  getDemo(id: string | number): Promise<ApiResponse<DemoVO>>;
  getTree(id: string | number): Promise<ApiResponse<TreeVO>>;
  listDemo(query?: DemoQuery): Promise<ApiResponse<PageResult<DemoVO>>>;
  listTree(query?: TreeQuery): Promise<ApiResponse<TreeVO[]>>;
  updateDemo(data: DemoForm): Promise<ApiResponse>;
  updateTree(data: TreeForm): Promise<ApiResponse>;
}

export const demoDomainModule: DomainModule = Object.freeze({
  id: 'demo',
  backendModules: ['wta-demo'],
  capabilities: ['demo-table', 'demo-tree', 'demo-rich-text']
});

const encodeId = (id: string | number): string => encodeURIComponent(String(id));

const encodeIds = (id: string | number | Array<string | number>): string =>
  Array.isArray(id) ? id.map(encodeId).join(',') : encodeId(id);

export function createDemoService(http: HttpClient, richText?: RichTextService): DemoService {
  return Object.freeze({
    richText: richText ?? createRichTextService(http, { upload: async () => { throw new Error('Rich-text assets port is not configured'); }, resolve: async () => [] }),
    listDemo: async query => {
      const response = await http.request<ApiResponse<PageResult<DemoTransport>>>({
        url: '/demo/demo/list',
        method: 'get',
        params: query
      });
      if (!response.data || !Array.isArray(response.data.rows)) {
        return response as unknown as ApiResponse<PageResult<DemoVO>>;
      }
      return { ...response, data: { ...response.data, rows: response.data.rows.map(projectDemoTransport) } };
    },
    getDemo: async id => {
      const response = await http.request<ApiResponse<DemoTransport>>({
        url: `/demo/demo/${encodeId(id)}`,
        method: 'get'
      });
      return response.data
        ? { ...response, data: projectDemoTransport(response.data) }
        : (response as ApiResponse<DemoVO>);
    },
    addDemo: data => http.request({ url: '/demo/demo', method: 'post', data }),
    updateDemo: data => http.request({ url: '/demo/demo', method: 'put', data }),
    deleteDemo: id => http.request({ url: `/demo/demo/${encodeIds(id)}`, method: 'delete' }),
    listTree: query => http.request({ url: '/demo/tree/list', method: 'get', params: query }),
    getTree: id => http.request({ url: `/demo/tree/${encodeId(id)}`, method: 'get' }),
    addTree: data => http.request({ url: '/demo/tree', method: 'post', data }),
    updateTree: data => http.request({ url: '/demo/tree', method: 'put', data }),
    deleteTree: id => http.request({ url: `/demo/tree/${encodeIds(id)}`, method: 'delete' })
  });
}
