import type { DomainModule } from '@namewta/platform-app-runtime';
import type { ApiErrorInfo, HttpClient } from '@namewta/platform-contracts';
import {
  createUserQueryPort,
  projectUserSummary,
  type UserQueryPort,
  type UserSummary
} from '@namewta/domain-system/user';
import { projectWorkflowTaskTransport, type WorkflowTaskTransport } from './transport';

export * from './transport';

export type { DepartmentSummary, UserPage, UserQuery, UserQueryPort, UserSummary } from '@namewta/domain-system/user';

export interface ApiResponse<T = unknown> {
  code?: number;
  data?: T;
  msg?: string;
  error?: ApiErrorInfo;
}
export interface PageResult<T> {
  rows: T[];
  total: number;
}
export interface PageQuery {
  pageNum?: number;
  pageSize?: number;
}
export interface NextNodesQuery {
  taskId: string | number;
  variables?: Record<string, unknown>;
}
export interface BaseEntity {
  createBy?: string;
  createTime?: string;
  params?: Record<string, unknown>;
  remark?: string;
  updateBy?: string;
  updateTime?: string;
}
export interface CategoryTreeVO {
  children: CategoryTreeVO[];
  id: number | string;
  label: string;
  parentId: number | string;
  weight: number;
}
export interface CategoryVO {
  categoryId: string | number;
  categoryName: string;
  children: CategoryVO[];
  createTime: string;
  orderNum: number;
  parentId: string | number;
}
export interface CategoryForm extends BaseEntity {
  categoryId?: string | number;
  categoryName?: string;
  orderNum?: number;
  parentId?: string | number;
}
export interface CategoryQuery {
  categoryName?: string;
}
export interface FlowDefinitionQuery extends PageQuery {
  category: string | number;
  flowCode?: string;
  flowName?: string;
  isPublish?: number;
}
export interface FlowDefinitionVO {
  activityStatus: number;
  createTime: Date;
  flowCode: string;
  flowName: string;
  formPath: string;
  id: string;
  isPublish: number;
  updateTime: Date;
  version: string;
}
export interface FlowDefinitionForm {
  category: string;
  ext: string;
  flowCode: string;
  flowName: string;
  formCustom: string;
  formPath: string;
  id: string;
  modelValue: string;
}
export interface DefinitionXmlVO {
  xml: string[];
  xmlStr: string;
}
export interface SpelVO {
  componentName: string;
  id: string | number;
  methodName: string;
  methodParams: string;
  remark?: string;
  status: string;
  viewSpel: string;
}
export interface SpelForm extends BaseEntity {
  componentName?: string;
  id?: string | number;
  methodName?: string;
  methodParams?: string;
  status?: string;
  viewSpel?: string;
}
export interface SpelQuery extends PageQuery {
  componentName?: string;
  methodName?: string;
  methodParams?: string;
  params?: Record<string, unknown>;
  status?: string;
  viewSpel?: string;
}

export interface TaskQuery extends PageQuery {
  createByIds?: readonly (string | number)[];
  flowCode?: string;
  flowName?: string;
  nodeName?: string;
}

export interface FlowCopy {
  nickName: string;
  userId: string | number;
}

export interface WorkflowTask {
  applyNode?: boolean;
  businessCode: string;
  businessId: string;
  businessTitle: string;
  buttonList?: { code: string; show: boolean }[];
  copyList?: FlowCopy[];
  createTime?: Date;
  definitionId?: string;
  flowCode: string;
  flowName: string;
  flowStatus: string;
  formCustom: string;
  formPath: string;
  id: string | number;
  instanceId: string;
  nodeCode: string;
  nodeName: string;
  nodeRatio: string | number;
  nodeType: number;
  tenantId?: string;
  updateTime?: Date;
  varList?: Map<string, string>;
  version?: string;
}

export interface WorkflowHistory {
  instanceId: string | number;
  list: Record<string, unknown>[];
}

export interface WorkflowInstance extends BaseEntity {
  activityStatus: number;
  businessCode: string;
  businessId: string;
  businessTitle: string;
  definitionId: string;
  flowCode: string;
  flowName: string;
  flowStatus: string;
  flowStatusName: string;
  formCustom?: string;
  formPath?: string;
  flowTaskList: WorkflowTask[];
  id: string | number;
  version: string;
}

export interface InstanceQuery extends PageQuery {
  businessId?: string;
  category?: string | number;
  createByIds?: readonly (string | number)[];
  flowCode?: string;
  flowName?: string;
  nodeName?: string;
}

export interface LeaveRecord {
  applyCode?: string;
  endDate: string;
  id: string | number;
  leaveDays: number;
  leaveType: string;
  remark: string;
  startDate: string;
  status?: string;
}

export interface LeaveForm extends BaseEntity {
  applyCode?: string;
  endDate?: string;
  id?: string | number;
  leaveDays?: number;
  leaveType?: string;
  startDate?: string;
  status?: string;
}

export interface LeaveQuery extends PageQuery {
  endLeaveDays?: number;
  startLeaveDays?: number;
}

export type WorkflowPayload = Record<string, unknown>;
export interface FlowInvalidPayload {
  comment: string;
  id: string | number;
}
export interface UrgeTaskPayload {
  message: string;
  messageType: string[];
  taskIdList: (string | number)[];
}
export interface TerminateTaskPayload {
  comment: string;
  taskId: string | number;
}
export interface TaskOperationPayload {
  message?: string;
  messageType?: string[];
  taskId: string | number;
  userId?: string | number;
  userIds?: (string | number)[];
}

type Identifier = string | number;
type IdentifierList = Identifier | readonly Identifier[];
const segment = (value: IdentifierList) =>
  (Array.isArray(value) ? value : [value]).map(item => encodeURIComponent(String(item))).join(',');

const projectTaskPage = (
  response: ApiResponse<PageResult<WorkflowTaskTransport>>
): ApiResponse<PageResult<WorkflowTask>> => {
  const { data, ...metadata } = response;
  if (!data) return metadata;
  return {
    ...metadata,
    data: {
      rows: Array.isArray(data.rows) ? data.rows.map(projectWorkflowTaskTransport) : [],
      total: typeof data.total === 'number' ? data.total : 0
    }
  };
};

const projectTask = (response: ApiResponse<WorkflowTaskTransport>): ApiResponse<WorkflowTask> => {
  const { data, ...metadata } = response;
  return data ? { ...metadata, data: projectWorkflowTaskTransport(data) } : metadata;
};

export interface WorkflowDefinitionService {
  addCategory(data: CategoryForm): Promise<ApiResponse>;
  addDefinition(data: FlowDefinitionForm | Record<string, unknown>): Promise<ApiResponse>;
  addSpel(data: SpelForm | Record<string, unknown>): Promise<ApiResponse>;
  categoryTree(query?: CategoryForm): Promise<ApiResponse<CategoryTreeVO[]>>;
  copyDefinition(id: string): Promise<ApiResponse>;
  deleteCategory(id: IdentifierList): Promise<ApiResponse>;
  deleteDefinition(id: string | readonly string[]): Promise<ApiResponse>;
  deleteSpel(id: IdentifierList): Promise<ApiResponse>;
  getCategory(id: Identifier): Promise<ApiResponse<CategoryVO>>;
  getDefinition(id: Identifier): Promise<ApiResponse<FlowDefinitionVO>>;
  getDefinitionXmlString(id: string): Promise<ApiResponse<string>>;
  getSpel(id: Identifier): Promise<ApiResponse<SpelVO>>;
  importDefinition(data: unknown, signal?: AbortSignal): Promise<ApiResponse>;
  legacyDefinitionXml(id: string): Promise<ApiResponse<DefinitionXmlVO>>;
  listCategories(query?: CategoryQuery): Promise<ApiResponse<CategoryVO[]>>;
  listDefinitions(query: FlowDefinitionQuery): Promise<ApiResponse<PageResult<FlowDefinitionVO>>>;
  listSpel(query?: SpelQuery): Promise<ApiResponse<PageResult<SpelVO>>>;
  listUnpublishedDefinitions(query: FlowDefinitionQuery): Promise<ApiResponse<PageResult<FlowDefinitionVO>>>;
  publishDefinition(id: string): Promise<ApiResponse>;
  setDefinitionActive(id: string, active: boolean): Promise<ApiResponse>;
  unpublishDefinition(id: string): Promise<ApiResponse>;
  updateCategory(data: CategoryForm): Promise<ApiResponse>;
  updateDefinition(data: FlowDefinitionForm | Record<string, unknown>): Promise<ApiResponse>;
  updateSpel(data: SpelForm | Record<string, unknown>): Promise<ApiResponse>;
  readonly users: UserQueryPort;
  pageTaskWaiting(query: TaskQuery): Promise<ApiResponse<PageResult<WorkflowTask>>>;
  pageTaskFinished(query: TaskQuery): Promise<ApiResponse<PageResult<WorkflowTask>>>;
  pageTaskCopies(query: TaskQuery): Promise<ApiResponse<PageResult<WorkflowTask>>>;
  pageAllTaskWaiting(query: TaskQuery): Promise<ApiResponse<PageResult<WorkflowTask>>>;
  pageAllTaskFinished(query: TaskQuery): Promise<ApiResponse<PageResult<WorkflowTask>>>;
  startWorkflow(data: WorkflowPayload): Promise<ApiResponse>;
  completeTask(data: WorkflowPayload): Promise<ApiResponse>;
  backProcess(data: WorkflowPayload): Promise<ApiResponse>;
  getTask(taskId: Identifier): Promise<ApiResponse<WorkflowTask>>;
  updateAssignee(taskIds: readonly string[], userId: Identifier): Promise<ApiResponse>;
  terminateTask(data: TerminateTaskPayload): Promise<ApiResponse>;
  getBackTaskNodes(taskId: Identifier, nodeCode: string): Promise<ApiResponse<Record<string, unknown>[]>>;
  operateTask(data: TaskOperationPayload, operation: string): Promise<ApiResponse>;
  currentTaskUsers(taskId: Identifier): Promise<ApiResponse<UserSummary[]>>;
  getNextNodes(data: NextNodesQuery): Promise<ApiResponse<Record<string, unknown>[]>>;
  urgeTask(data: UrgeTaskPayload): Promise<ApiResponse>;
  pageRunningInstances(query: InstanceQuery): Promise<ApiResponse<PageResult<WorkflowInstance>>>;
  pageFinishedInstances(query: InstanceQuery): Promise<ApiResponse<PageResult<WorkflowInstance>>>;
  pageCurrentInstances(query: InstanceQuery): Promise<ApiResponse<PageResult<WorkflowInstance>>>;
  flowHistory(businessId: Identifier): Promise<ApiResponse<WorkflowHistory>>;
  cancelProcess(data: WorkflowPayload): Promise<ApiResponse>;
  instanceVariables(instanceId: Identifier): Promise<ApiResponse<Record<string, unknown>>>;
  deleteInstances(instanceIds: IdentifierList): Promise<ApiResponse>;
  deleteHistoricInstances(instanceIds: IdentifierList): Promise<ApiResponse>;
  invalidateInstance(data: FlowInvalidPayload): Promise<ApiResponse>;
  setInstanceActive(id: Identifier, active: boolean): Promise<ApiResponse>;
  updateInstanceVariables(data: WorkflowPayload): Promise<ApiResponse>;
  listLeaves(query?: LeaveQuery): Promise<ApiResponse<PageResult<LeaveRecord>>>;
  getLeave(id: Identifier): Promise<ApiResponse<LeaveRecord>>;
  addLeave(data: LeaveForm | WorkflowPayload): Promise<ApiResponse<LeaveRecord>>;
  submitLeave(data: LeaveForm | WorkflowPayload): Promise<ApiResponse<LeaveRecord>>;
  updateLeave(data: LeaveForm | WorkflowPayload): Promise<ApiResponse<LeaveRecord>>;
  deleteLeaves(ids: IdentifierList): Promise<ApiResponse>;
}

export const workflowDomainModule: DomainModule = Object.freeze({
  id: 'workflow',
  backendModules: Object.freeze(['wta-workflow']),
  capabilities: Object.freeze([
    'category-admin',
    'definition-admin',
    'definition-design',
    'spel-admin',
    'task-runtime',
    'instance-runtime',
    'leave-runtime'
  ])
});

export function createWorkflowDefinitionService(http: HttpClient): WorkflowDefinitionService {
  const request = <T = unknown>(config: Parameters<HttpClient['request']>[0]) => http.request<ApiResponse<T>>(config);
  return Object.freeze({
    users: createUserQueryPort(http),
    listCategories: query => request<CategoryVO[]>({ url: '/workflow/category/list', method: 'get', params: query }),
    getCategory: id => request<CategoryVO>({ url: '/workflow/category/' + segment(id), method: 'get' }),
    addCategory: data => request({ url: '/workflow/category', method: 'post', data }),
    updateCategory: data => request({ url: '/workflow/category/update', method: 'post', data }),
    deleteCategory: id => request({ url: '/workflow/category/' + segment(id), method: 'post' }),
    categoryTree: query =>
      request<CategoryTreeVO[]>({ url: '/workflow/category/categoryTree', method: 'get', params: query }),
    listDefinitions: query =>
      request<PageResult<FlowDefinitionVO>>({ url: '/workflow/definition/list', method: 'get', params: query }),
    listUnpublishedDefinitions: query =>
      request<PageResult<FlowDefinitionVO>>({
        url: '/workflow/definition/unPublishList',
        method: 'get',
        params: query
      }),
    legacyDefinitionXml: id =>
      request<DefinitionXmlVO>({ url: '/workflow/definition/definitionXml/' + segment(id), method: 'get' }),
    deleteDefinition: id => request({ url: '/workflow/definition/' + segment(id), method: 'post' }),
    setDefinitionActive: (id, active) =>
      request({ url: '/workflow/definition/active/' + segment(id), method: 'post', params: { active } }),
    importDefinition: (data, signal) =>
      request({ url: '/workflow/definition/importDef', method: 'post', data, headers: { repeatSubmit: false }, ...(signal ? { signal } : {}) }),
    publishDefinition: id => request({ url: '/workflow/definition/publish/' + segment(id), method: 'post' }),
    unpublishDefinition: id => request({ url: '/workflow/definition/unPublish/' + segment(id), method: 'post' }),
    getDefinitionXmlString: id =>
      request<string>({ url: '/workflow/definition/xmlString/' + segment(id), method: 'get' }),
    addDefinition: data => request({ url: '/workflow/definition', method: 'post', data }),
    updateDefinition: data => request({ url: '/workflow/definition/update', method: 'post', data }),
    getDefinition: id => request<FlowDefinitionVO>({ url: '/workflow/definition/' + segment(id), method: 'get' }),
    copyDefinition: id => request({ url: '/workflow/definition/copy/' + segment(id), method: 'post' }),
    listSpel: query => request<PageResult<SpelVO>>({ url: '/workflow/spel/list', method: 'get', params: query }),
    getSpel: id => request<SpelVO>({ url: '/workflow/spel/' + segment(id), method: 'get' }),
    addSpel: data => request({ url: '/workflow/spel', method: 'post', data }),
    updateSpel: data => request({ url: '/workflow/spel/update', method: 'post', data }),
    deleteSpel: id => request({ url: '/workflow/spel/' + segment(id), method: 'post' }),
    pageTaskWaiting: async query =>
      projectTaskPage(
        await request<PageResult<WorkflowTaskTransport>>({
          url: '/workflow/task/pageByTaskWait',
          method: 'get',
          params: query
        })
      ),
    pageTaskFinished: async query =>
      projectTaskPage(
        await request<PageResult<WorkflowTaskTransport>>({
          url: '/workflow/task/pageByTaskFinish',
          method: 'get',
          params: query
        })
      ),
    pageTaskCopies: async query =>
      projectTaskPage(
        await request<PageResult<WorkflowTaskTransport>>({
          url: '/workflow/task/pageByTaskCopy',
          method: 'get',
          params: query
        })
      ),
    pageAllTaskWaiting: async query =>
      projectTaskPage(
        await request<PageResult<WorkflowTaskTransport>>({
          url: '/workflow/task/pageByAllTaskWait',
          method: 'get',
          params: query
        })
      ),
    pageAllTaskFinished: async query =>
      projectTaskPage(
        await request<PageResult<WorkflowTaskTransport>>({
          url: '/workflow/task/pageByAllTaskFinish',
          method: 'get',
          params: query
        })
      ),
    startWorkflow: data => request({ url: '/workflow/task/startWorkFlow', method: 'post', data }),
    completeTask: data => request({ url: '/workflow/task/completeTask', method: 'post', data }),
    backProcess: data => request({ url: '/workflow/task/backProcess', method: 'post', data }),
    getTask: async taskId =>
      projectTask(
        await request<WorkflowTaskTransport>({
          url: '/workflow/task/getTask/' + segment(taskId),
          method: 'get'
        })
      ),
    updateAssignee: (taskIds, userId) =>
      request({ url: '/workflow/task/updateAssignee/' + segment(userId), method: 'post', data: taskIds }),
    terminateTask: data => request({ url: '/workflow/task/terminationTask', method: 'post', data }),
    getBackTaskNodes: (taskId, nodeCode) =>
      request<Record<string, unknown>[]>({
        url: '/workflow/task/getBackTaskNode/' + segment(taskId) + '/' + segment(nodeCode),
        method: 'get'
      }),
    operateTask: (data, operation) =>
      request({ url: '/workflow/task/taskOperation/' + segment(operation), method: 'post', data }),
    currentTaskUsers: async taskId => {
      const response = await request<unknown[]>({
        url: '/workflow/task/currentTaskAllUser/' + segment(taskId),
        method: 'get'
      });
      return { ...response, data: response.data?.map(source => {
        // 任务参与人只需要展示身份，手机号仅由需要联系信息的目录消费者使用。
        const { phoneNumber: _phoneNumber, ...user } = projectUserSummary(source);
        return user;
      }) ?? [] };
    },
    getNextNodes: data =>
      request<Record<string, unknown>[]>({
        url: '/workflow/task/getNextNodeList',
        method: 'get',
        params: { taskId: data.taskId, ...(data.variables === undefined ? {} : { variables: JSON.stringify(data.variables) }) }
      }),
    urgeTask: data => request({ url: '/workflow/task/urgeTask', method: 'post', data }),
    pageRunningInstances: query =>
      request<PageResult<WorkflowInstance>>({ url: '/workflow/instance/pageByRunning', method: 'get', params: query }),
    pageFinishedInstances: query =>
      request<PageResult<WorkflowInstance>>({ url: '/workflow/instance/pageByFinish', method: 'get', params: query }),
    pageCurrentInstances: query =>
      request<PageResult<WorkflowInstance>>({ url: '/workflow/instance/pageByCurrent', method: 'get', params: query }),
    flowHistory: businessId =>
      request<WorkflowHistory>({ url: '/workflow/instance/flowHisTaskList/' + segment(businessId), method: 'get' }),
    cancelProcess: data => request({ url: '/workflow/instance/cancelProcessApply', method: 'post', data }),
    instanceVariables: instanceId =>
      request<Record<string, unknown>>({
        url: '/workflow/instance/instanceVariable/' + segment(instanceId),
        method: 'get'
      }),
    deleteInstances: instanceIds =>
      request({ url: '/workflow/instance/deleteByInstanceIds/' + segment(instanceIds), method: 'post' }),
    deleteHistoricInstances: instanceIds =>
      request({ url: '/workflow/instance/deleteHisByInstanceIds/' + segment(instanceIds), method: 'post' }),
    invalidateInstance: data => request({ url: '/workflow/instance/invalid', method: 'post', data }),
    setInstanceActive: (id, active) =>
      request({ url: '/workflow/instance/active/' + segment(id), method: 'post', params: { active } }),
    updateInstanceVariables: data => request({ url: '/workflow/instance/updateVariable', method: 'post', data }),
    listLeaves: query =>
      request<PageResult<LeaveRecord>>({ url: '/workflow/leave/list', method: 'get', params: query }),
    getLeave: id => request<LeaveRecord>({ url: '/workflow/leave/' + segment(id), method: 'get' }),
    addLeave: data => request<LeaveRecord>({ url: '/workflow/leave', method: 'post', data }),
    submitLeave: data => request<LeaveRecord>({ url: '/workflow/leave/submitAndFlowStart', method: 'post', data }),
    updateLeave: data => request<LeaveRecord>({ url: '/workflow/leave/update', method: 'post', data }),
    deleteLeaves: ids => request({ url: '/workflow/leave/' + segment(ids), method: 'post' })
  });
}
