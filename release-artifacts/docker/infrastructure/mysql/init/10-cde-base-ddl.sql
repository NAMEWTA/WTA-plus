SET NAMES utf8mb4;

-- NAMEWTA / WTA 业务库结构快照（完整最新基座，直接修改本文件）
-- 来源：10-wta-base.sql 的 CREATE TABLE + 50-namewta-ddl.sql 的 CREATE（ALTER 已折入平台表）

-- ----------------------------
create table sys_social
(
    id                 bigint           not null        comment '主键',
    user_id            bigint           not null        comment '用户ID',
    auth_id            varchar(255)     not null        comment '平台+平台唯一id',
    source             varchar(255)     not null        comment '用户来源',
    open_id            varchar(255)     default null    comment '平台编号唯一id',
    user_name          varchar(30)      not null        comment '登录账号',
    nick_name          varchar(30)      default ''      comment '用户昵称',
    email              varchar(255)     default ''      comment '用户邮箱',
    avatar             varchar(500)     default ''      comment '头像地址',
    access_token       varchar(2000)     not null       comment '用户的授权令牌',
    expire_in          int              default null    comment '用户的授权令牌的有效期，部分平台可能没有',
    refresh_token      varchar(2000)     default null    comment '刷新令牌，部分平台可能没有',
    access_code        varchar(255)     default null    comment '平台的授权信息，部分平台可能没有',
    union_id           varchar(255)     default null    comment '用户的 unionid',
    scope              varchar(255)     default null    comment '授予的权限，部分平台可能没有',
    token_type         varchar(255)     default null    comment '个别平台的授权信息，部分平台可能没有',
    id_token           varchar(2000)    default null    comment 'id token，部分平台可能没有',
    mac_algorithm      varchar(255)     default null    comment '小米平台用户的附带属性，部分平台可能没有',
    mac_key            varchar(255)     default null    comment '小米平台用户的附带属性，部分平台可能没有',
    code               varchar(255)     default null    comment '用户的授权code，部分平台可能没有',
    oauth_token        varchar(255)     default null    comment 'Twitter平台用户的附带属性，部分平台可能没有',
    oauth_token_secret varchar(255)     default null    comment 'Twitter平台用户的附带属性，部分平台可能没有',
    create_dept        bigint(20)                       comment '创建部门',
    create_by          bigint(20)                       comment '创建者',
    create_time        datetime                         comment '创建时间',
    update_by          bigint(20)                       comment '更新者',
    update_time        datetime                         comment '更新时间',
    del_flag           char(1)          default '0'     comment '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (id)
) engine=innodb comment = '社会化关系表';

-- ----------------------------
create table sys_dept (
    dept_id           bigint(20)      not null                   comment '部门id',
    parent_id         bigint(20)      default 0                  comment '父部门id',
    ancestors         varchar(500)    default ''                 comment '祖级列表',
    dept_name         varchar(30)     default ''                 comment '部门名称',
    dept_category     varchar(100)    default null               comment '部门类别编码',
    order_num         int(4)          default 0                  comment '显示顺序',
    leader            bigint(20)      default null               comment '负责人',
    phone             varchar(11)     default null               comment '联系电话',
    email             varchar(50)     default null               comment '邮箱',
    status            char(1)         default '0'                comment '部门状态（0正常 1停用）',
    del_flag          char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (dept_id),
    key idx_sys_dept_parent_id (parent_id)
) engine=innodb comment = '部门表';

-- ----------------------------
create table sys_user (
    user_id           bigint(20)      not null                   comment '用户ID',
    dept_id           bigint(20)      default null               comment '部门ID',
    user_name         varchar(30)     not null                   comment '用户账号',
    nick_name         varchar(30)     not null                   comment '用户昵称',
    email             varchar(50)     default ''                 comment '用户邮箱',
    phone_number      varchar(11)     default ''                 comment '手机号码',
    gender            char(1)         default '0'                comment '用户性别（0男 1女 2未知）',
    avatar            bigint(20)                                 comment '头像地址',
    password          varchar(100)    default ''                 comment '密码',
    status            char(1)         default '0'                comment '账号状态（0正常 1停用）',
    del_flag          char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    login_ip          varchar(128)    default ''                 comment '最后登录IP',
    login_date        datetime                                   comment '最后登录时间',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)    default null               comment '备注',
    primary key (user_id),
    key idx_sys_user_dept_id   (dept_id),
    key idx_sys_user_create_by (create_by),
    key idx_sys_user_user_name (user_name),
    key idx_sys_user_phone     (phone_number)
) engine=innodb comment = '用户信息表';

-- ----------------------------
create table sys_post
(
    post_id       bigint(20)      not null                   comment '岗位ID',
    dept_id       bigint(20)      not null                   comment '部门id',
    post_code     varchar(64)     not null                   comment '岗位编码',
    post_category varchar(100)    default null               comment '岗位类别编码',
    post_name     varchar(50)     not null                   comment '岗位名称',
    post_sort     int(4)          not null                   comment '显示顺序',
    status        char(1)         not null                   comment '状态（0正常 1停用）',
    del_flag      char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    create_dept   bigint(20)      default null               comment '创建部门',
    create_by     bigint(20)      default null               comment '创建者',
    create_time   datetime                                   comment '创建时间',
    update_by     bigint(20)      default null               comment '更新者',
    update_time   datetime                                   comment '更新时间',
    remark        varchar(500)    default null               comment '备注',
    primary key (post_id),
    key idx_sys_post_dept_id (dept_id)
) engine=innodb comment = '岗位信息表';

-- ----------------------------
create table sys_role (
    role_id              bigint(20)      not null                   comment '角色ID',
    client_id            bigint(20)      default null               comment '归属客户端主键',
    role_name            varchar(30)     not null                   comment '角色名称',
    role_key             varchar(100)    not null                   comment '角色权限字符串',
    role_sort            int(4)          not null                   comment '显示顺序',
    data_scope           char(1)         default '1'                comment '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）',
    menu_check_strictly  tinyint(1)      default 1                  comment '菜单树选择项是否关联显示',
    dept_check_strictly  tinyint(1)      default 1                  comment '部门树选择项是否关联显示',
    status               char(1)         not null                   comment '角色状态（0正常 1停用）',
    del_flag             char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    create_dept          bigint(20)      default null               comment '创建部门',
    create_by            bigint(20)      default null               comment '创建者',
    create_time          datetime                                   comment '创建时间',
    update_by            bigint(20)      default null               comment '更新者',
    update_time          datetime                                   comment '更新时间',
    remark               varchar(500)    default null               comment '备注',
    primary key (role_id),
    key idx_sys_role_client_id (client_id),
    key idx_sys_role_create_dept (create_dept),
    key idx_sys_role_create_by   (create_by)
) engine=innodb comment = '角色信息表';

-- ----------------------------
create table sys_menu (
    menu_id           bigint(20)      not null                   comment '菜单ID',
    client_id         bigint(20)      default null               comment '归属客户端主键',
    menu_name         varchar(50)     not null                   comment '菜单名称',
    parent_id         bigint(20)      default 0                  comment '父菜单ID',
    order_num         int(4)          default 0                  comment '显示顺序',
    path              varchar(200)    default ''                 comment '路由地址',
    component         varchar(255)    default null               comment '组件路径',
    query_param       varchar(255)    default null               comment '路由参数',
    is_frame          char(1)         default 'N'                comment '是否为外链（Y是 N否）',
    is_cache          char(1)         default 'Y'                comment '是否缓存（Y缓存 N不缓存）',
    menu_type         char(1)         default ''                 comment '菜单类型（M目录 C菜单 F按钮）',
    visible           char(1)         default 0                  comment '显示状态（0显示 1隐藏）',
    status            char(1)         default 0                  comment '菜单状态（0正常 1停用）',
    perms             varchar(100)    default null               comment '权限标识',
    icon              varchar(100)    default '#'                comment '菜单图标',
    active_menu       varchar(255)    default ''                 comment '激活菜单路径',
    ext               varchar(2000)   default ''                 comment '扩展字段',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)    default ''                 comment '备注',
    primary key (menu_id),
    key idx_sys_menu_client_id (client_id)
) engine=innodb comment = '菜单权限表';

-- ----------------------------
create table sys_user_role (
    user_id   bigint(20) not null comment '用户ID',
    role_id   bigint(20) not null comment '角色ID',
    primary key(user_id, role_id),
    key idx_sys_user_role_rid (role_id)
) engine=innodb comment = '用户和角色关联表';

-- ----------------------------
create table sys_role_menu (
    role_id   bigint(20) not null comment '角色ID',
    menu_id   bigint(20) not null comment '菜单ID',
    primary key(role_id, menu_id)
) engine=innodb comment = '角色和菜单关联表';

-- ----------------------------
create table sys_role_dept (
    role_id   bigint(20) not null comment '角色ID',
    dept_id   bigint(20) not null comment '部门ID',
    primary key(role_id, dept_id)
) engine=innodb comment = '角色和部门关联表';

-- ----------------------------
create table sys_user_post
(
    user_id   bigint(20) not null comment '用户ID',
    post_id   bigint(20) not null comment '岗位ID',
    primary key (user_id, post_id)
) engine=innodb comment = '用户与岗位关联表';

-- ----------------------------
create table sys_oper_log (
    oper_id           bigint(20)      not null                   comment '日志主键',
    title             varchar(50)     default ''                 comment '模块标题',
    business_type     int(2)          default 0                  comment '业务类型（0其它 1新增 2修改 3删除）',
    method            varchar(100)    default ''                 comment '方法名称',
    request_method    varchar(10)     default ''                 comment '请求方式',
    operator_type     int(1)          default 0                  comment '操作类别（0其它 1后台用户 2手机端用户）',
    oper_name         varchar(50)     default ''                 comment '操作人员',
    user_id           bigint(20)      default null               comment '操作用户ID',
    dept_id           bigint(20)      default null               comment '操作部门ID',
    dept_name         varchar(50)     default ''                 comment '部门名称',
    client_key        varchar(32)     default ''                 comment '客户端',
    device_type       varchar(32)     default ''                 comment '设备类型',
    browser           varchar(50)     default ''                 comment '浏览器类型',
    os                varchar(50)     default ''                 comment '操作系统',
    oper_url          varchar(255)    default ''                 comment '请求URL',
    oper_ip           varchar(128)    default ''                 comment '主机地址',
    oper_location     varchar(255)    default ''                 comment '操作地点',
    oper_param        varchar(4000)   default ''                 comment '请求参数',
    json_result       varchar(4000)   default ''                 comment '返回参数',
    status            int(1)          default 0                  comment '操作状态（0正常 1异常）',
    error_msg         varchar(4000)   default ''                 comment '错误消息',
    oper_time         datetime                                   comment '操作时间',
    cost_time         bigint(20)      default 0                  comment '消耗时间',
    primary key (oper_id),
    key idx_sys_oper_log_bt (business_type),
    key idx_sys_oper_log_uid (user_id),
    key idx_sys_oper_log_s  (status),
    key idx_sys_oper_log_ot (oper_time)
) engine=innodb comment = '操作日志记录';

-- ----------------------------
create table sys_dict_type
(
    dict_id          bigint(20)      not null                   comment '字典主键',
    dict_name        varchar(100)    default ''                 comment '字典名称',
    dict_type        varchar(100)    default ''                 comment '字典类型',
    create_dept      bigint(20)      default null               comment '创建部门',
    create_by        bigint(20)      default null               comment '创建者',
    create_time      datetime                                   comment '创建时间',
    update_by        bigint(20)      default null               comment '更新者',
    update_time      datetime                                   comment '更新时间',
    remark           varchar(500)    default null               comment '备注',
    primary key (dict_id),
    unique (dict_type)
) engine=innodb comment = '字典类型表';

-- ----------------------------
create table sys_dict_data
(
    dict_code        bigint(20)      not null                   comment '字典编码',
    dict_sort        int(4)          default 0                  comment '字典排序',
    dict_label       varchar(100)    default ''                 comment '字典标签',
    dict_value       varchar(100)    default ''                 comment '字典键值',
    dict_type        varchar(100)    default ''                 comment '字典类型',
    css_class        varchar(100)    default null               comment '样式属性（其他样式扩展）',
    list_class       varchar(100)    default null               comment '表格回显样式',
    is_default       char(1)         default 'N'                comment '是否默认（Y是 N否）',
    create_dept      bigint(20)      default null               comment '创建部门',
    create_by        bigint(20)      default null               comment '创建者',
    create_time      datetime                                   comment '创建时间',
    update_by        bigint(20)      default null               comment '更新者',
    update_time      datetime                                   comment '更新时间',
    remark           varchar(500)    default null               comment '备注',
    primary key (dict_code),
    key idx_sys_dict_data_type (dict_type)
) engine=innodb comment = '字典数据表';

-- ----------------------------
create table sys_config (
    config_id         bigint(20)      not null                   comment '参数主键',
    config_name       varchar(100)    default ''                 comment '参数名称',
    config_key        varchar(100)    default ''                 comment '参数键名',
    config_value      varchar(500)    default ''                 comment '参数键值',
    config_type       char(1)         default 'N'                comment '系统内置（Y是 N否）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)    default null               comment '备注',
    primary key (config_id)
) engine=innodb comment = '参数配置表';

-- ----------------------------
create table sys_login_info (
    info_id        bigint(20)     not null                  comment '访问ID',
    user_name      varchar(50)    default ''                comment '用户账号',
    client_key     varchar(32)    default ''                comment '客户端',
    device_type    varchar(32)    default ''                comment '设备类型',
    ipaddr         varchar(128)   default ''                comment '登录IP地址',
    login_location varchar(255)   default ''                comment '登录地点',
    browser        varchar(50)    default ''                comment '浏览器类型',
    os             varchar(50)    default ''                comment '操作系统',
    status         char(1)        default '0'               comment '登录状态（0正常 1异常）',
    msg            varchar(255)   default ''                comment '提示消息',
    login_time     datetime                                 comment '访问时间',
    primary key (info_id),
    key idx_sys_login_info_s  (status),
    key idx_sys_login_info_lt (login_time)
) engine=innodb comment = '系统访问记录';

-- NAMEWTA-ADMIN-RUNTIME-RECONCILE-DDL-001
-- 代码生成器物理表已从基座移除：全新库不再创建 gen_table / gen_table_column。
-- 已有库不得重放本基座；升级使用源/目标 Git Tag 差异 SQL。
-- NAMEWTA-ADMIN-RUNTIME-RECONCILE-DDL-001-END

-- ----------------------------
-- NAMEWTA-OSS-NOTIFY-DDL-001
create table sys_oss (
    oss_id          bigint(20)   not null                   comment '对象存储主键',
    file_name       varchar(255) not null default ''        comment '文件名',
    original_name   varchar(255) not null default ''        comment '原名',
    file_suffix     varchar(10)  not null default ''        comment '文件后缀名',
    url             varchar(500) not null                   comment 'URL地址',
    ext1            text                  default null      comment '扩展字段',
    create_dept     bigint(20)            default null      comment '创建部门',
    create_time     datetime              default null      comment '创建时间',
    create_by       bigint(20)            default null      comment '上传人',
    update_time     datetime              default null      comment '更新时间',
    update_by       bigint(20)            default null      comment '更新人',
    service         varchar(20)  not null default 'minio'   comment '服务商',
    is_temp         char(1)      not null default 'N'        comment '是否临时对象（Y是 N否）',
    expire_time     datetime              default null      comment '临时对象过期时间',
    delete_state    varchar(16)  not null default 'ACTIVE'   comment '对象删除状态（ACTIVE正常 PENDING等待供应商删除）',
    primary key (oss_id),
    key idx_sys_oss_temp_expire (is_temp, expire_time)
) engine=innodb comment ='OSS对象存储表';

-- ----------------------------
-- 变更内容：收敛OSS访问类型并新增可审计的存储边界迁移表
-- 变更标识：2026-09-01_00:14:13
-- 逻辑标识：NAMEWTA-OSS-ACCESS-DDL-001
create table sys_oss_config (
    oss_config_id   bigint(20)    not null                  comment '主键',
    config_key      varchar(20)   not null  default ''      comment '配置key',
    access_key      varchar(255)            default ''      comment 'accessKey',
    secret_key      varchar(255)            default ''      comment '秘钥',
    bucket_name     varchar(255)            default ''      comment '桶名称',
    prefix          varchar(255)            default ''      comment '前缀',
    endpoint        varchar(255)            default ''      comment '访问站点',
    domain_url      varchar(255)            default ''      comment '自定义域名',
    is_https        char(1)                 default 'N'     comment '是否https（Y=是,N=否）',
    region          varchar(255)            default ''      comment '域',
    access_policy char(1) not null default '0' comment '桶权限类型（0=PRIVATE 2=PUBLIC_READ）',
    status          char(1)                 default 'N'     comment '是否默认（Y=是,N=否）',
    ext1            varchar(255)            default ''      comment '扩展字段',
    create_dept     bigint(20)              default null    comment '创建部门',
    create_by       bigint(20)              default null    comment '创建者',
    create_time     datetime                default null    comment '创建时间',
    update_by       bigint(20)              default null    comment '更新者',
    update_time     datetime                default null    comment '更新时间',
    remark          varchar(500)            default null    comment '备注',
    primary key (oss_config_id)
) engine=innodb comment='对象存储配置表';

-- ----------------------------
create table sys_client (
    id                  bigint(20)    not null            comment 'id',
    client_id           varchar(64)   default null        comment '客户端id',
    client_key          varchar(32)   default null        comment '客户端key',
    client_secret       varchar(255)  default null        comment '客户端秘钥',
    grant_type          varchar(255)  default null        comment '授权类型',
    device_type         varchar(32)   default null        comment '设备类型',
    access_path         varchar(2000) default null        comment '允许访问路径',
    ip_whitelist        varchar(1000) default null        comment 'IP白名单',
    active_timeout      int(11)       default 1800        comment 'token活跃超时时间',
    timeout             int(11)       default 604800      comment 'token固定超时',
    user_type_id        bigint(20)    default null        comment '登录域ID',
    register_enabled    tinyint(1)    default 0           comment '是否开放公开注册（0否 1是）',
    default_role_id     bigint(20)    default null        comment '默认角色ID',
    sso_enabled         tinyint(1)    default 0           comment '是否启用 SSO 接入（0否 1是）',
    sso_auth_mode       varchar(16)   default 'local'     comment '登录模式 local/sso/both',
    sso_client_kind     varchar(16)   default 'public'    comment 'OAuth 客户端类型 public/confidential',
    sso_redirect_uris   varchar(2000) default null        comment 'SSO 精确回调白名单',
    sso_pkce_required   tinyint(1)    default 1           comment '是否强制 PKCE（0否 1是）',
    sso_auto_consent    tinyint(1)    default 1           comment '是否自动同意（0否 1是）',
    sso_scope           varchar(255)  default null        comment 'SSO 默认 scope',
    sso_secret_hash     varchar(255)  default null        comment 'SSO 客户端密钥哈希',
    sso_secret_rotated_at datetime    default null        comment 'SSO 密钥最近轮换时间',
    status              char(1)       default '0'         comment '状态（0正常 1停用）',
    del_flag            char(1)       default '0'         comment '删除标志（0代表存在 1代表删除）',
    create_dept         bigint(20)    default null        comment '创建部门',
    create_by           bigint(20)    default null        comment '创建者',
    create_time         datetime      default null        comment '创建时间',
    update_by           bigint(20)    default null        comment '更新者',
    update_time         datetime      default null        comment '更新时间',
    primary key (id)
) engine=innodb comment='系统授权表';

-- ----------------------------
-- NAMEWTA 自有表
-- ----------------------------

create table test_rich_text (
    rich_text_id bigint(20) not null comment '富文本文档主键',
    client_pk bigint(20) not null comment '归属客户端主键',
    title varchar(120) not null comment '文档标题',
    content_html mediumtext not null comment '规范化富文本 HTML',
    version bigint(20) not null default 0 comment '乐观锁版本',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime not null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (rich_text_id),
    key idx_test_rich_text_owner (client_pk, create_by, del_flag, update_time)
) engine=innodb comment='富文本演示文档';

create table sys_user_type (
    user_type_id    bigint(20)      not null                   comment '登录域ID',
    user_type_code  varchar(32)     not null                   comment '登录域编码',
    user_type_name  varchar(30)     not null                   comment '登录域名称',
    order_num       int(4)          default 0                  comment '显示顺序',
    status          char(1)         default '0'                comment '状态（0正常 1停用）',
    del_flag        char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    create_dept     bigint(20)      default null               comment '创建部门',
    create_by       bigint(20)      default null               comment '创建者',
    create_time     datetime                                   comment '创建时间',
    update_by       bigint(20)      default null               comment '更新者',
    update_time     datetime                                   comment '更新时间',
    remark          varchar(500)    default null               comment '备注',
    primary key (user_type_id),
    unique key uk_sys_user_type_code (user_type_code)
) engine=innodb comment = '登录域定义表';

create table sys_user_type_rel (
    rel_id          bigint(20)      not null                   comment '关系ID',
    user_id         bigint(20)      not null                   comment '用户ID',
    user_type_id    bigint(20)      not null                   comment '登录域ID',
    grant_source    varchar(32)     not null                   comment '授权来源（SELF_REGISTER/ADMIN_CREATE/ADMIN_GRANT/SYSTEM_INIT）',
    status          char(1)         default '0'                comment '状态（0正常 1停用）',
    create_dept     bigint(20)      default null               comment '创建部门',
    create_by       bigint(20)      default null               comment '创建者',
    create_time     datetime                                   comment '创建时间',
    update_by       bigint(20)      default null               comment '更新者',
    update_time     datetime                                   comment '更新时间',
    primary key (rel_id),
    unique key uk_sys_user_type_rel (user_id, user_type_id),
    key idx_sys_user_type_rel_type (user_type_id)
) engine=innodb comment = '用户登录域关系表';

create table sys_oss_ref (
    oss_ref_id    bigint(20)   not null                   comment 'OSS引用主键',
    oss_id        bigint(20)   not null                   comment 'OSS对象存储主键',
    ref_type      varchar(64)  not null                   comment '引用方实际物理表名',
    ref_id        varchar(64)  not null                   comment '引用方真实主键',
    version       int(11)      default 0                  comment '乐观锁版本号',
    create_dept   bigint(20)   default null               comment '创建部门',
    create_time   datetime     default null               comment '创建时间',
    create_by     bigint(20)   default null               comment '创建者',
    update_time   datetime     default null               comment '更新时间',
    update_by     bigint(20)   default null               comment '更新者',
    del_flag      char(1)      default '0'                comment '删除标志（0代表存在 1代表删除）',
    primary key (oss_ref_id),
    unique key uk_sys_oss_ref_object (oss_id, ref_type, ref_id),
    key idx_sys_oss_ref_reverse (ref_type, ref_id)
) engine=innodb comment='OSS业务引用表';

-- NAMEWTA-OPENAPI-CREDENTIAL-DDL-001
-- 变更内容：新增每用户唯一的 OpenAPI 凭据表
-- 变更标识：2026-08-31_22:02:33
create table sys_open_api_credential (
    open_api_credential_id bigint(20)    not null                   comment 'OpenAPI凭据主键',
    owner_user_id          bigint(20)    not null                   comment '凭据所属用户主键',
    active_owner_user_id   bigint(20)    generated always as
        (case when del_flag = '0' then owner_user_id else null end) stored comment '未删除凭据所属用户唯一键',
    app_key                varchar(64)   not null                   comment '公开应用标识',
    app_name               varchar(100)  not null                   comment '应用名称',
    secret_ciphertext      varbinary(512) not null                  comment 'AES-256-GCM密文',
    secret_nonce           binary(12)    not null                   comment 'AES-GCM随机nonce',
    secret_tag             binary(16)    not null                   comment 'AES-GCM认证标签',
    kek_version            varchar(64)   not null                   comment '密钥加密密钥版本',
    status                 char(1)       not null default '0'       comment '状态（0启用 1停用）',
    expires_at             datetime      default null               comment '过期时间（空为永久）',
    remark                 varchar(500)  default null               comment '备注',
    version                int(11)       not null default 0         comment '乐观锁版本号',
    create_dept            bigint(20)    default null               comment '创建部门',
    create_time            datetime      default null               comment '创建时间',
    create_by              bigint(20)    default null               comment '创建者',
    update_time            datetime      default null               comment '更新时间',
    update_by              bigint(20)    default null               comment '更新者',
    del_flag               char(1)       not null default '0'       comment '删除标志（0代表存在 1代表删除）',
    primary key (open_api_credential_id),
    unique key uk_sys_open_api_credential_active_owner (active_owner_user_id),
    unique key uk_sys_open_api_credential_app_key (app_key),
    key idx_sys_open_api_credential_owner (owner_user_id)
) engine=innodb comment='用户OpenAPI凭据表';

create table sys_oss_migration_batch (
    oss_migration_batch_id bigint(20)    not null                   comment 'OSS迁移批次主键',
    target_config_key      varchar(20)   not null                   comment '目标OSS配置标识',
    status                 varchar(32)   not null                   comment '批次状态',
    dry_run                char(1)       not null default 'Y'       comment '是否仅预检（Y是 N否）',
    total_count            int(11)       not null default 0         comment '对象总数',
    success_count          int(11)       not null default 0         comment '成功对象数',
    failed_count           int(11)       not null default 0         comment '失败对象数',
    started_time           datetime      default null               comment '开始时间',
    completed_time         datetime      default null               comment '完成时间',
    error_message          varchar(2000) default null               comment '清洗后的批次错误信息',
    version                int(11)       not null default 0         comment '乐观锁版本号',
    create_dept            bigint(20)    default null               comment '创建部门',
    create_time            datetime      default null               comment '创建时间',
    create_by              bigint(20)    default null               comment '创建者',
    update_time            datetime      default null               comment '更新时间',
    update_by              bigint(20)    default null               comment '更新者',
    del_flag               char(1)       not null default '0'       comment '删除标志（0代表存在 1代表删除）',
    primary key (oss_migration_batch_id),
    key idx_sys_oss_migration_batch_status_time (status, create_time),
    key idx_sys_oss_migration_batch_target (target_config_key)
) engine=innodb comment='OSS存储边界迁移批次表';

create table sys_oss_migration_item (
    oss_migration_item_id bigint(20)    not null                   comment 'OSS迁移明细主键',
    oss_migration_batch_id bigint(20)   not null                   comment 'OSS迁移批次主键',
    oss_id                 bigint(20)   not null                   comment 'OSS对象存储主键',
    source_config_key      varchar(20)  not null                   comment '来源OSS配置标识',
    target_config_key      varchar(20)  not null                   comment '目标OSS配置标识',
    object_key             varchar(1024) not null                  comment '供应商对象键',
    status                 varchar(32)  not null                   comment '明细状态',
    stage                  varchar(32)  not null                   comment '当前迁移阶段',
    source_size            bigint(20)   default null               comment '来源对象字节数',
    target_size            bigint(20)   default null               comment '目标对象字节数',
    source_etag            varchar(255) default null               comment '来源对象内容标识',
    target_etag            varchar(255) default null               comment '目标对象内容标识',
    retry_count            int(11)      not null default 0         comment '重试次数',
    last_error_stage       varchar(32)  default null               comment '最近失败阶段',
    error_message          varchar(2000) default null              comment '清洗后的明细错误信息',
    service_switched_time  datetime     default null               comment '服务归属切换时间',
    cleanup_eligible_time  datetime     default null               comment '允许源对象清理时间',
    cleaned_time           datetime     default null               comment '源对象清理完成时间',
    version                int(11)      not null default 0         comment '乐观锁版本号',
    create_dept            bigint(20)   default null               comment '创建部门',
    create_time            datetime     default null               comment '创建时间',
    create_by              bigint(20)   default null               comment '创建者',
    update_time            datetime     default null               comment '更新时间',
    update_by              bigint(20)   default null               comment '更新者',
    del_flag               char(1)      not null default '0'       comment '删除标志（0代表存在 1代表删除）',
    primary key (oss_migration_item_id),
    unique key uk_sys_oss_migration_item_batch_oss (oss_migration_batch_id, oss_id),
    key idx_sys_oss_migration_item_batch_status (oss_migration_batch_id, status),
    key idx_sys_oss_migration_item_oss (oss_id),
    key idx_sys_oss_migration_item_source (source_config_key),
    key idx_sys_oss_migration_item_target (target_config_key)
) engine=innodb comment='OSS存储边界迁移明细表';

-- NAMEWTA-PROFILE-DDL-001
create table profile_identity_guard (
    identity_guard_id bigint(20) not null comment '活动身份守卫主键',
    profile_type varchar(16) not null comment '档案类型（PERSON/ENTERPRISE）',
    identity_key varchar(255) not null comment '规范化身份键',
    owner_type varchar(32) not null comment '占用方类型（APPLICATION/PROFILE）',
    owner_id bigint(20) not null comment '占用方主键',
    status varchar(16) not null comment '守卫状态（ACTIVE/RELEASED）',
    active_guard_key varchar(320) generated always as
        (case when status = 'ACTIVE' and del_flag = '0' then concat(profile_type, ':', identity_key) else null end) stored comment '活动身份唯一键',
    released_time datetime default null comment '释放时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (identity_guard_id),
    unique key uk_profile_identity_guard_active (active_guard_key),
    key idx_profile_identity_guard_owner (owner_type, owner_id)
) engine=innodb comment='档案活动身份守卫表';

create table profile_document_type (
    document_type_id bigint(20) not null comment '证件类型主键',
    document_type_code varchar(64) not null comment '稳定证件类型编码',
    issuing_region varchar(8) not null comment '签发地区（CN/HK/MO/TW）',
    document_type_name varchar(100) not null comment '证件类型名称',
    number_pattern varchar(500) not null comment '号码校验正则',
    validity_required char(1) not null default 'Y' comment '是否要求有效期限（Y是 N否）',
    status char(1) not null default '0' comment '状态（0正常 1停用）',
    order_num int(11) not null default 0 comment '显示顺序',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (document_type_id),
    unique key uk_profile_document_type_code (document_type_code)
) engine=innodb comment='个人认证证件类型目录';

create table profile_material_node (
    material_node_id bigint(20) not null comment '材料目录节点主键',
    parent_id bigint(20) not null default 0 comment '父节点主键',
    node_type varchar(16) not null comment '节点类型（CATEGORY/TAG）',
    node_depth tinyint(4) not null comment '节点深度（一级分类1、二级分类2、标签2或3）',
    profile_type varchar(16) not null comment '适用档案（PERSON/ENTERPRISE/COMMON）',
    material_tag_code varchar(64) default null comment '稳定材料标签编码',
    node_name varchar(100) not null comment '节点显示名称',
    system_required char(1) not null default 'N' comment '是否系统必传保护标签（Y是 N否）',
    status char(1) not null default '0' comment '状态（0正常 1停用）',
    order_num int(11) not null default 0 comment '显示顺序',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (material_node_id),
    unique key uk_profile_material_node_tag_code (material_tag_code),
    key idx_profile_material_node_parent (parent_id, order_num),
    constraint chk_profile_material_node_shape check (
        (node_type = 'CATEGORY' and material_tag_code is null and system_required = 'N'
            and ((node_depth = 1 and parent_id = 0) or (node_depth = 2 and parent_id <> 0)))
        or
        (node_type = 'TAG' and material_tag_code is not null and node_depth in (2, 3) and parent_id <> 0)
    )
) engine=innodb comment='档案材料受限树节点表';

create table profile_material_requirement (
    material_requirement_id bigint(20) not null comment '必传材料规则主键',
    profile_type varchar(16) not null comment '档案类型（PERSON/ENTERPRISE）',
    document_type_code varchar(64) not null default '*' comment '证件类型编码（*为任意）',
    handler_condition varchar(64) not null default 'ALWAYS' comment '办理条件',
    material_tag_code varchar(64) not null comment '必传材料标签编码',
    minimum_count int(11) not null default 1 comment '最少文件数',
    status char(1) not null default '0' comment '状态（0正常 1停用）',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (material_requirement_id),
    unique key uk_profile_material_requirement_rule (profile_type, document_type_code, handler_condition, material_tag_code)
) engine=innodb comment='档案必传材料规则表';

create table profile_person (
    person_profile_id bigint(20) not null comment '个人档案主键',
    previous_profile_id bigint(20) default null comment '已注销前序个人档案主键',
    current_version_id bigint(20) default null comment '当前个人档案版本主键',
    full_name varchar(100) not null comment '姓名（明文）',
    document_type_code varchar(64) not null comment '证件类型编码',
    document_number varchar(128) not null comment '证件号码（明文）',
    identity_key varchar(255) not null comment '规范化个人身份键',
    gender varchar(16) not null comment '性别',
    birth_date date not null comment '出生日期',
    valid_from date default null comment '证件有效期起',
    valid_until date default null comment '证件有效期止',
    status varchar(16) not null comment '档案状态（ACTIVE/REVOKED）',
    active_identity_key varchar(255) generated always as
        (case when status <> 'REVOKED' and del_flag = '0' then identity_key else null end) stored comment '未注销个人身份唯一键',
    revoked_time datetime default null comment '注销时间',
    revoked_reason varchar(500) default null comment '注销原因',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (person_profile_id),
    unique key uk_profile_person_active_identity (active_identity_key),
    key idx_profile_person_previous (previous_profile_id)
) engine=innodb comment='个人档案主体表';

create table profile_person_version (
    person_version_id bigint(20) not null comment '个人档案版本主键',
    person_profile_id bigint(20) not null comment '个人档案主键',
    version_no int(11) not null comment '档案版本号',
    source_type varchar(32) not null comment '来源类型（USER_SUBMISSION/ADMIN_CREATE/ADMIN_OVERRIDE）',
    source_id bigint(20) not null comment '不可变来源主键',
    full_name varchar(100) not null comment '姓名（明文）',
    document_type_code varchar(64) not null comment '证件类型编码',
    document_number varchar(128) not null comment '证件号码（明文）',
    identity_key varchar(255) not null comment '规范化个人身份键',
    gender varchar(16) not null comment '性别',
    birth_date date not null comment '出生日期',
    valid_from date default null comment '证件有效期起',
    valid_until date default null comment '证件有效期止',
    status varchar(16) not null comment '版本状态（CURRENT/SUPERSEDED）',
    current_profile_id bigint(20) generated always as
        (case when status = 'CURRENT' and del_flag = '0' then person_profile_id else null end) stored comment '当前个人版本唯一键',
    published_time datetime not null comment '发布时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (person_version_id),
    unique key uk_profile_person_version_no (person_profile_id, version_no),
    unique key uk_profile_person_version_current (current_profile_id),
    key idx_profile_person_version_source (source_type, source_id)
) engine=innodb comment='个人档案不可变版本表';

create table profile_person_application (
    person_application_id bigint(20) not null comment '个人认证申请主键',
    applicant_user_id bigint(20) not null comment '申请账户主键',
    target_profile_id bigint(20) default null comment '重新认证或换绑目标档案主键',
    status varchar(16) not null comment '申请状态（DRAFT/BACK/CANCEL/WAITING/FINISH/INVALID/TERMINATION）',
    open_user_id bigint(20) generated always as
        (case when status in ('DRAFT','BACK','CANCEL','WAITING') and del_flag = '0' then applicant_user_id else null end) stored comment '进行中申请账户唯一键',
    full_name varchar(100) default null comment '姓名（明文）',
    document_type_code varchar(64) default null comment '证件类型编码',
    document_number varchar(128) default null comment '证件号码（明文）',
    identity_key varchar(255) default null comment '规范化个人身份键',
    open_identity_key varchar(255) generated always as
        (case when status in ('DRAFT','BACK','CANCEL','WAITING') and del_flag = '0' then identity_key else null end) stored comment '进行中个人身份唯一键',
    gender varchar(16) default null comment '性别',
    birth_date date default null comment '出生日期',
    valid_from date default null comment '证件有效期起',
    valid_until date default null comment '证件有效期止',
    provider_code varchar(64) not null comment '固定验证供应商编码',
    submission_seq int(11) not null default 0 comment '最近提交序号',
    rebind_intent char(1) not null default 'N' comment '是否个人换绑申请（Y是 N否）',
    expected_binding_id bigint(20) default null comment '预期旧绑定主键',
    expected_binding_version int(11) default null comment '预期旧绑定版本',
    decision_version int(11) not null default 0 comment '决定围栏版本',
    decision_source varchar(32) default null comment '最终决定来源',
    decision_result varchar(16) default null comment '最终决定结果',
    decision_reason varchar(500) default null comment '决定原因',
    submitted_time datetime default null comment '最近提交时间',
    finished_time datetime default null comment '完成时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (person_application_id),
    unique key uk_profile_person_application_open_user (open_user_id),
    unique key uk_profile_person_application_open_identity (open_identity_key),
    key idx_profile_person_application_target (target_profile_id),
    key idx_profile_person_application_status (status, create_time)
) engine=innodb comment='个人认证申请工作副本表';

create table profile_person_submission (
    person_submission_id bigint(20) not null comment '个人提交快照主键',
    person_application_id bigint(20) not null comment '个人认证申请主键',
    submission_seq int(11) not null comment '提交序号',
    full_name varchar(100) not null comment '姓名快照（明文）',
    document_type_code varchar(64) not null comment '证件类型编码快照',
    document_number varchar(128) not null comment '证件号码快照（明文）',
    identity_key varchar(255) not null comment '规范化个人身份键快照',
    gender varchar(16) not null comment '性别快照',
    birth_date date not null comment '出生日期快照',
    valid_from date default null comment '证件有效期起快照',
    valid_until date default null comment '证件有效期止快照',
    provider_code varchar(64) not null comment '验证供应商编码快照',
    rebind_intent char(1) not null default 'N' comment '是否换绑意图快照',
    target_profile_id bigint(20) default null comment '目标档案快照',
    expected_binding_id bigint(20) default null comment '旧绑定主键快照',
    expected_binding_version int(11) default null comment '旧绑定版本快照',
    field_snapshot_json longtext not null comment '完整结构化字段快照JSON（明文）',
    submitted_time datetime not null comment '提交时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (person_submission_id),
    unique key uk_profile_person_submission_seq (person_application_id, submission_seq)
) engine=innodb comment='个人认证不可变提交快照表';

create table profile_person_source (
    person_source_id bigint(20) not null comment '个人管理员来源快照主键',
    person_profile_id bigint(20) not null comment '个人档案主键',
    source_type varchar(32) not null comment '来源类型（ADMIN_CREATE/ADMIN_OVERRIDE）',
    operator_user_id bigint(20) not null comment '操作管理员账户主键',
    operation_reason varchar(500) not null comment '操作原因',
    full_name varchar(100) not null comment '姓名快照（明文）',
    document_type_code varchar(64) not null comment '证件类型编码快照',
    document_number varchar(128) not null comment '证件号码快照（明文）',
    identity_key varchar(255) not null comment '规范化个人身份键快照',
    gender varchar(16) not null comment '性别快照',
    birth_date date not null comment '出生日期快照',
    valid_from date default null comment '证件有效期起快照',
    valid_until date default null comment '证件有效期止快照',
    field_snapshot_json longtext not null comment '完整管理员来源快照JSON（明文）',
    occurred_time datetime not null comment '来源形成时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (person_source_id),
    key idx_profile_person_source_profile (person_profile_id, occurred_time)
) engine=innodb comment='个人档案管理员不可变来源快照表';

create table profile_person_binding (
    person_binding_id bigint(20) not null comment '个人有效绑定主键',
    person_profile_id bigint(20) not null comment '个人档案主键',
    user_id bigint(20) not null comment '绑定账户主键',
    status varchar(16) not null comment '绑定状态（ACTIVE/SUSPENDED/UNBOUND）',
    effective_user_id bigint(20) generated always as
        (case when status in ('ACTIVE','SUSPENDED') and del_flag = '0' then user_id else null end) stored comment '个人有效绑定账户唯一键',
    effective_profile_id bigint(20) generated always as
        (case when status in ('ACTIVE','SUSPENDED') and del_flag = '0' then person_profile_id else null end) stored comment '个人有效绑定档案唯一键',
    binding_version int(11) not null default 1 comment '绑定竞态版本',
    source_type varchar(32) not null default 'USER_SUBMISSION' comment '绑定来源',
    source_id bigint(20) default null comment '绑定来源主键',
    bound_time datetime not null comment '绑定时间',
    unbound_time datetime default null comment '解绑时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (person_binding_id),
    unique key uk_profile_person_binding_effective_user (effective_user_id),
    unique key uk_profile_person_binding_effective_profile (effective_profile_id),
    key idx_profile_person_binding_history (person_profile_id, user_id, create_time)
) engine=innodb comment='个人档案当前绑定表';

create table profile_person_binding_event (
    person_binding_event_id bigint(20) not null comment '个人绑定事件主键',
    person_binding_id bigint(20) not null comment '个人绑定主键',
    person_profile_id bigint(20) not null comment '个人档案主键',
    user_id bigint(20) not null comment '绑定账户主键',
    event_type varchar(16) not null comment '事件类型（ACTIVE/SUSPENDED/UNBOUND）',
    binding_version int(11) not null comment '事件后绑定版本',
    source_type varchar(32) not null comment '事件来源',
    source_id bigint(20) default null comment '事件来源主键',
    reason varchar(500) default null comment '事件原因',
    occurred_time datetime not null comment '事件时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (person_binding_event_id),
    unique key uk_profile_person_binding_event_version (person_binding_id, binding_version),
    key idx_profile_person_binding_event_profile (person_profile_id, occurred_time)
) engine=innodb comment='个人档案不可变绑定事件表';

create table profile_enterprise (
    enterprise_profile_id bigint(20) not null comment '企业档案主键',
    previous_profile_id bigint(20) default null comment '已注销前序企业档案主键',
    current_version_id bigint(20) default null comment '当前企业档案版本主键',
    enterprise_name varchar(255) not null comment '企业名称（明文）',
    unified_credit_code varchar(64) not null comment '统一社会信用代码（明文）',
    enterprise_type varchar(64) not null comment '单位性质',
    legal_representative_name varchar(100) not null comment '法定代表人姓名（明文）',
    legal_document_type_code varchar(64) not null comment '法定代表人证件类型编码',
    legal_document_number varchar(128) not null comment '法定代表人证件号码（明文）',
    established_date date not null comment '成立日期',
    business_term_from date default null comment '营业期限起',
    business_term_until date default null comment '营业期限止',
    registered_address varchar(500) not null comment '注册地址',
    business_scope longtext not null comment '经营范围',
    contact_name varchar(100) default null comment '联系姓名（明文）',
    contact_phone varchar(64) default null comment '联系电话（明文）',
    email varchar(255) default null comment '企业邮箱',
    registered_capital decimal(20,2) default null comment '注册资本',
    industry_code varchar(64) default null comment '行业编码',
    website varchar(500) default null comment '企业网站',
    status varchar(16) not null comment '档案状态（ACTIVE/REVOKED）',
    active_credit_code varchar(64) generated always as
        (case when status <> 'REVOKED' and del_flag = '0' then upper(trim(unified_credit_code)) else null end) stored comment '未注销统一社会信用代码唯一键',
    revoked_time datetime default null comment '注销时间',
    revoked_reason varchar(500) default null comment '注销原因',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (enterprise_profile_id),
    unique key uk_profile_enterprise_active_credit (active_credit_code),
    key idx_profile_enterprise_previous (previous_profile_id)
) engine=innodb comment='企业档案主体表';

create table profile_enterprise_version (
    enterprise_version_id bigint(20) not null comment '企业档案版本主键',
    enterprise_profile_id bigint(20) not null comment '企业档案主键',
    version_no int(11) not null comment '档案版本号',
    source_type varchar(32) not null comment '来源类型（USER_SUBMISSION/ADMIN_CREATE/ADMIN_OVERRIDE）',
    source_id bigint(20) not null comment '不可变来源主键',
    enterprise_name varchar(255) not null comment '企业名称快照（明文）',
    unified_credit_code varchar(64) not null comment '统一社会信用代码快照（明文）',
    enterprise_type varchar(64) not null comment '单位性质快照',
    legal_representative_name varchar(100) not null comment '法定代表人姓名快照（明文）',
    legal_document_type_code varchar(64) not null comment '法定代表人证件类型编码快照',
    legal_document_number varchar(128) not null comment '法定代表人证件号码快照（明文）',
    established_date date not null comment '成立日期快照',
    business_term_from date default null comment '营业期限起快照',
    business_term_until date default null comment '营业期限止快照',
    registered_address varchar(500) not null comment '注册地址快照',
    business_scope longtext not null comment '经营范围快照',
    contact_name varchar(100) default null comment '联系姓名快照（明文）',
    contact_phone varchar(64) default null comment '联系电话快照（明文）',
    email varchar(255) default null comment '企业邮箱快照',
    registered_capital decimal(20,2) default null comment '注册资本快照',
    industry_code varchar(64) default null comment '行业编码快照',
    website varchar(500) default null comment '企业网站快照',
    status varchar(16) not null comment '版本状态（CURRENT/SUPERSEDED）',
    current_profile_id bigint(20) generated always as
        (case when status = 'CURRENT' and del_flag = '0' then enterprise_profile_id else null end) stored comment '当前企业版本唯一键',
    published_time datetime not null comment '发布时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (enterprise_version_id),
    unique key uk_profile_enterprise_version_no (enterprise_profile_id, version_no),
    unique key uk_profile_enterprise_version_current (current_profile_id),
    key idx_profile_enterprise_version_source (source_type, source_id)
) engine=innodb comment='企业档案不可变版本表';

create table profile_enterprise_application (
    enterprise_application_id bigint(20) not null comment '企业认证申请主键',
    applicant_user_id bigint(20) not null comment '申请账户主键',
    target_profile_id bigint(20) default null comment '重新认证目标企业档案主键',
    status varchar(16) not null comment '申请状态（DRAFT/BACK/CANCEL/WAITING/FINISH/INVALID/TERMINATION）',
    open_user_id bigint(20) generated always as
        (case when status in ('DRAFT','BACK','CANCEL','WAITING') and del_flag = '0' then applicant_user_id else null end) stored comment '进行中企业申请账户唯一键',
    enterprise_name varchar(255) default null comment '企业名称（明文）',
    unified_credit_code varchar(64) default null comment '统一社会信用代码（明文）',
    identity_key varchar(255) default null comment '规范化企业身份键',
    open_identity_key varchar(255) generated always as
        (case when status in ('DRAFT','BACK','CANCEL','WAITING') and del_flag = '0' then identity_key else null end) stored comment '进行中企业身份唯一键',
    enterprise_type varchar(64) default null comment '单位性质',
    legal_representative_name varchar(100) default null comment '法定代表人姓名（明文）',
    legal_document_type_code varchar(64) default null comment '法定代表人证件类型编码',
    legal_document_number varchar(128) default null comment '法定代表人证件号码（明文）',
    handler_is_legal_representative char(1) not null default 'N' comment '办理人是否法定代表人（Y是 N否）',
    established_date date default null comment '成立日期',
    business_term_from date default null comment '营业期限起',
    business_term_until date default null comment '营业期限止',
    registered_address varchar(500) default null comment '注册地址',
    business_scope longtext default null comment '经营范围',
    contact_name varchar(100) default null comment '联系姓名（明文）',
    contact_phone varchar(64) default null comment '联系电话（明文）',
    email varchar(255) default null comment '企业邮箱',
    registered_capital decimal(20,2) default null comment '注册资本',
    industry_code varchar(64) default null comment '行业编码',
    website varchar(500) default null comment '企业网站',
    provider_code varchar(64) not null comment '固定验证供应商编码',
    submission_seq int(11) not null default 0 comment '最近提交序号',
    decision_version int(11) not null default 0 comment '决定围栏版本',
    decision_source varchar(32) default null comment '最终决定来源',
    decision_result varchar(16) default null comment '最终决定结果',
    decision_reason varchar(500) default null comment '决定原因',
    submitted_time datetime default null comment '最近提交时间',
    finished_time datetime default null comment '完成时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (enterprise_application_id),
    unique key uk_profile_enterprise_application_open_user (open_user_id),
    unique key uk_profile_enterprise_application_open_identity (open_identity_key),
    key idx_profile_enterprise_application_target (target_profile_id),
    key idx_profile_enterprise_application_status (status, create_time)
) engine=innodb comment='企业认证申请工作副本表';

create table profile_enterprise_submission (
    enterprise_submission_id bigint(20) not null comment '企业提交快照主键',
    enterprise_application_id bigint(20) not null comment '企业认证申请主键',
    submission_seq int(11) not null comment '提交序号',
    enterprise_name varchar(255) not null comment '企业名称快照（明文）',
    unified_credit_code varchar(64) not null comment '统一社会信用代码快照（明文）',
    identity_key varchar(255) not null comment '规范化企业身份键快照',
    enterprise_type varchar(64) not null comment '单位性质快照',
    legal_representative_name varchar(100) not null comment '法定代表人姓名快照（明文）',
    legal_document_type_code varchar(64) not null comment '法定代表人证件类型编码快照',
    legal_document_number varchar(128) not null comment '法定代表人证件号码快照（明文）',
    handler_is_legal_representative char(1) not null comment '办理人是否法定代表人快照',
    established_date date not null comment '成立日期快照',
    business_term_from date default null comment '营业期限起快照',
    business_term_until date default null comment '营业期限止快照',
    registered_address varchar(500) not null comment '注册地址快照',
    business_scope longtext not null comment '经营范围快照',
    contact_name varchar(100) default null comment '联系姓名快照（明文）',
    contact_phone varchar(64) default null comment '联系电话快照（明文）',
    email varchar(255) default null comment '企业邮箱快照',
    registered_capital decimal(20,2) default null comment '注册资本快照',
    industry_code varchar(64) default null comment '行业编码快照',
    website varchar(500) default null comment '企业网站快照',
    provider_code varchar(64) not null comment '验证供应商编码快照',
    field_snapshot_json longtext not null comment '完整结构化字段快照JSON（明文）',
    submitted_time datetime not null comment '提交时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (enterprise_submission_id),
    unique key uk_profile_enterprise_submission_seq (enterprise_application_id, submission_seq)
) engine=innodb comment='企业认证不可变提交快照表';

create table profile_enterprise_source (
    enterprise_source_id bigint(20) not null comment '企业管理员来源快照主键',
    enterprise_profile_id bigint(20) not null comment '企业档案主键',
    source_type varchar(32) not null comment '来源类型（ADMIN_CREATE/ADMIN_OVERRIDE）',
    operator_user_id bigint(20) not null comment '操作管理员账户主键',
    operation_reason varchar(500) not null comment '操作原因',
    enterprise_name varchar(255) not null comment '企业名称快照（明文）',
    unified_credit_code varchar(64) not null comment '统一社会信用代码快照（明文）',
    identity_key varchar(255) not null comment '规范化企业身份键快照',
    enterprise_type varchar(64) not null comment '单位性质快照',
    legal_representative_name varchar(100) not null comment '法定代表人姓名快照（明文）',
    legal_document_type_code varchar(64) not null comment '法定代表人证件类型编码快照',
    legal_document_number varchar(128) not null comment '法定代表人证件号码快照（明文）',
    established_date date not null comment '成立日期快照',
    business_term_from date default null comment '营业期限起快照',
    business_term_until date default null comment '营业期限止快照',
    registered_address varchar(500) not null comment '注册地址快照',
    business_scope longtext not null comment '经营范围快照',
    contact_name varchar(100) default null comment '联系姓名快照（明文）',
    contact_phone varchar(64) default null comment '联系电话快照（明文）',
    email varchar(255) default null comment '企业邮箱快照',
    registered_capital decimal(20,2) default null comment '注册资本快照',
    industry_code varchar(64) default null comment '行业编码快照',
    website varchar(500) default null comment '企业网站快照',
    field_snapshot_json longtext not null comment '完整管理员来源快照JSON（明文）',
    occurred_time datetime not null comment '来源形成时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (enterprise_source_id),
    key idx_profile_enterprise_source_profile (enterprise_profile_id, occurred_time)
) engine=innodb comment='企业档案管理员不可变来源快照表';

create table profile_enterprise_binding (
    enterprise_binding_id bigint(20) not null comment '企业负责人有效绑定主键',
    enterprise_profile_id bigint(20) not null comment '企业档案主键',
    user_id bigint(20) not null comment '负责人账户主键',
    status varchar(16) not null comment '绑定状态（ACTIVE/SUSPENDED/UNBOUND）',
    effective_user_id bigint(20) generated always as
        (case when status in ('ACTIVE','SUSPENDED') and del_flag = '0' then user_id else null end) stored comment '企业有效绑定账户唯一键',
    effective_profile_id bigint(20) generated always as
        (case when status in ('ACTIVE','SUSPENDED') and del_flag = '0' then enterprise_profile_id else null end) stored comment '企业有效绑定档案唯一键',
    binding_version int(11) not null default 1 comment '绑定竞态版本',
    source_type varchar(32) not null default 'USER_SUBMISSION' comment '绑定来源',
    source_id bigint(20) default null comment '绑定来源主键',
    bound_time datetime not null comment '绑定时间',
    unbound_time datetime default null comment '解绑时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (enterprise_binding_id),
    unique key uk_profile_enterprise_binding_effective_user (effective_user_id),
    unique key uk_profile_enterprise_binding_effective_profile (effective_profile_id),
    key idx_profile_enterprise_binding_history (enterprise_profile_id, user_id, create_time)
) engine=innodb comment='企业负责人当前绑定表';

create table profile_enterprise_binding_event (
    enterprise_binding_event_id bigint(20) not null comment '企业绑定事件主键',
    enterprise_binding_id bigint(20) not null comment '企业绑定主键',
    enterprise_profile_id bigint(20) not null comment '企业档案主键',
    user_id bigint(20) not null comment '负责人账户主键',
    event_type varchar(16) not null comment '事件类型（ACTIVE/SUSPENDED/UNBOUND）',
    binding_version int(11) not null comment '事件后绑定版本',
    source_type varchar(32) not null comment '事件来源',
    source_id bigint(20) default null comment '事件来源主键',
    reason varchar(500) default null comment '事件原因',
    occurred_time datetime not null comment '事件时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (enterprise_binding_event_id),
    unique key uk_profile_enterprise_binding_event_version (enterprise_binding_id, binding_version),
    key idx_profile_enterprise_binding_event_profile (enterprise_profile_id, occurred_time)
) engine=innodb comment='企业负责人不可变绑定事件表';

create table profile_material_ref (
    material_ref_id bigint(20) not null comment '档案材料引用主键',
    owner_type varchar(32) not null comment '材料归属类型（WORKING/SUBMISSION/SOURCE/VERSION）',
    owner_id bigint(20) not null comment '材料归属主键',
    profile_type varchar(16) not null comment '档案类型（PERSON/ENTERPRISE）',
    oss_id bigint(20) not null comment 'OSS对象主键',
    material_node_id bigint(20) not null comment '材料标签节点主键',
    material_tag_code varchar(64) not null comment '材料标签稳定编码快照',
    material_tag_name varchar(100) not null comment '材料标签显示名称快照',
    file_name varchar(255) not null comment '文件名快照',
    file_size bigint(20) not null comment '文件大小字节数快照',
    file_extension varchar(16) not null comment '文件扩展名快照',
    mime_type varchar(128) not null comment '文件MIME快照',
    status varchar(16) not null default 'ATTACHED' comment '引用状态（ATTACHED/DETACHED）',
    active_owner_oss_key varchar(160) generated always as
        (case when status = 'ATTACHED' and del_flag = '0'
            then concat(profile_type, ':', owner_type, ':', owner_id, ':', oss_id) else null end) stored comment '当前材料单标签唯一键',
    immutable_flag char(1) not null default 'N' comment '是否不可变证据（Y是 N否）',
    attached_time datetime not null comment '挂接时间',
    detached_time datetime default null comment '解除当前关系时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (material_ref_id),
    unique key uk_profile_material_ref_active_owner_oss (active_owner_oss_key),
    key idx_profile_material_ref_owner (owner_type, owner_id, status),
    key idx_profile_material_ref_oss (oss_id),
    key idx_profile_material_ref_tag (material_node_id)
) engine=innodb comment='档案材料单标签引用与历史表';

create table profile_verification_attempt (
    verification_attempt_id bigint(20) not null comment '验证尝试主键',
    profile_type varchar(16) not null comment '档案类型（PERSON/ENTERPRISE）',
    application_id bigint(20) not null comment '认证申请主键',
    submission_id bigint(20) not null comment '提交快照主键',
    provider_code varchar(64) not null comment '验证供应商编码',
    provider_request_id varchar(128) default null comment '供应商请求唯一标识',
    request_fingerprint varchar(128) not null comment '请求内容指纹',
    attempt_no int(11) not null comment '显式尝试序号',
    status varchar(32) not null comment '尝试状态',
    normalized_result_json longtext default null comment '规范化验证结果JSON',
    provider_evidence_json longtext default null comment '供应商证据JSON（明文）',
    error_code varchar(128) default null comment '安全错误分类',
    completed_time datetime default null comment '完成时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (verification_attempt_id),
    unique key uk_profile_verification_attempt_no (profile_type, application_id, attempt_no),
    unique key uk_profile_verification_provider_request (provider_code, provider_request_id),
    key idx_profile_verification_submission (profile_type, submission_id)
) engine=innodb comment='档案不可变供应商验证尝试表';

create table profile_decision_record (
    decision_record_id bigint(20) not null comment '审核决定记录主键',
    profile_type varchar(16) not null comment '档案类型（PERSON/ENTERPRISE）',
    application_id bigint(20) not null comment '认证申请主键',
    submission_id bigint(20) not null comment '决定针对的提交快照主键',
    decision_version int(11) not null comment '决定围栏版本',
    decision_source varchar(32) not null comment '决定来源（WORKFLOW/ADMIN_OVERRIDE）',
    decision_result varchar(16) not null comment '决定结果（APPROVED/REJECTED）',
    decision_status varchar(32) not null comment '决定状态（PENDING/FINAL/IGNORED/FAILED）',
    workflow_event_id varchar(128) default null comment '流程事件幂等标识',
    operator_user_id bigint(20) default null comment '决定操作者账户主键',
    reason varchar(500) default null comment '决定原因',
    occurred_time datetime not null comment '决定时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (decision_record_id),
    unique key uk_profile_decision_version (profile_type, application_id, decision_version, decision_source),
    key idx_profile_decision_workflow_event (workflow_event_id)
) engine=innodb comment='档案不可变审核决定记录表';

create table profile_operation_audit (
    operation_audit_id bigint(20) not null comment '档案操作审计主键',
    profile_type varchar(16) not null comment '档案类型（PERSON/ENTERPRISE/COMMON）',
    profile_id bigint(20) default null comment '档案主键',
    application_id bigint(20) default null comment '申请主键',
    binding_id bigint(20) default null comment '绑定主键',
    operation_type varchar(64) not null comment '操作类型',
    operator_user_id bigint(20) not null comment '操作者账户主键',
    capability varchar(100) not null comment '本次业务能力权限',
    reason varchar(500) default null comment '操作原因',
    before_status varchar(32) default null comment '操作前状态',
    after_status varchar(32) default null comment '操作后状态',
    result varchar(16) not null comment '操作结果（SUCCESS/FAILED/IGNORED）',
    failure_category varchar(64) default null comment '安全失败分类',
    occurred_time datetime not null comment '操作时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (operation_audit_id),
    key idx_profile_operation_subject (profile_type, profile_id, application_id, occurred_time),
    key idx_profile_operation_actor (operator_user_id, occurred_time)
) engine=innodb comment='档案业务操作审计表';

create table profile_notification_audit (
    notification_audit_id bigint(20) not null comment '档案通知审计主键',
    notification_type varchar(64) not null comment '通知类型',
    profile_type varchar(16) not null comment '档案类型（PERSON/ENTERPRISE）',
    profile_id bigint(20) default null comment '档案主键',
    application_id bigint(20) default null comment '申请主键',
    target_user_id bigint(20) not null comment '通知目标账户主键',
    notify_request_id varchar(64) default null comment '统一通知请求主键',
    status varchar(32) not null comment '通知状态',
    failure_category varchar(64) default null comment '安全失败分类',
    occurred_time datetime not null comment '通知时间',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (notification_audit_id),
    key idx_profile_notification_subject (profile_type, profile_id, application_id, occurred_time),
    key idx_profile_notification_request (notify_request_id)
) engine=innodb comment='档案无敏感载荷通知审计表';

create table profile_enterprise_transfer_record (
    enterprise_transfer_record_id bigint(20) not null comment '企业负责人转移记录主键',
    enterprise_profile_id bigint(20) not null comment '企业档案主键',
    source_binding_id bigint(20) not null comment '原负责人绑定主键',
    source_user_id bigint(20) not null comment '原负责人账户主键',
    target_user_id bigint(20) not null comment '目标负责人账户主键',
    challenge_id varchar(64) not null comment '专用短信挑战标识',
    notification_id varchar(64) not null comment '服务端关联的验证码通知标识',
    expected_binding_version int(11) not null comment '发起时绑定版本',
    status varchar(32) not null comment '转移状态（CHALLENGED/CONFIRMED/EXPIRED/FAILED）',
    failed_attempts int(11) not null default 0 comment '验证码错误次数',
    expires_time datetime not null comment '挑战过期时间',
    confirmed_time datetime default null comment '确认完成时间',
    failure_category varchar(64) default null comment '安全失败分类',
    version int(11) not null default 0 comment '乐观锁版本号',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime default null comment '创建时间',
    create_by bigint(20) default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint(20) default null comment '更新者',
    del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (enterprise_transfer_record_id),
    unique key uk_profile_enterprise_transfer_challenge (challenge_id),
    key idx_profile_enterprise_transfer_profile (enterprise_profile_id, create_time)
) engine=innodb comment='企业负责人转移挑战与结果审计表';

-- NAMEWTA-THIRD-HTTP-DDL-001
create table third_provider (
    provider_id bigint(20) not null comment '供应商主键',
    provider_code varchar(64) not null comment '供应商编码',
    provider_name varchar(128) not null comment '供应商名称',
    base_url varchar(512) not null comment '可信基础 URL',
    status char(1) not null default '0' comment '0启用 1停用',
    timeout_connect_ms int not null default 3000 comment '连接超时毫秒',
    timeout_read_ms int not null default 10000 comment '读取超时毫秒',
    rate_limit int not null default 0 comment '供应商每秒限流，0不启用',
    concurrency_limit int not null default 0 comment '供应商并发上限，0不启用',
    shared_headers_json json default null comment '共享请求头白名单配置',
    remark varchar(500) default null,
    version int not null default 0,
    create_dept bigint(20) default null, create_by bigint(20) default null,
    create_time datetime default null, update_by bigint(20) default null,
    update_time datetime default null, del_flag char(1) not null default '0',
    primary key (provider_id), unique key uk_third_provider_code(provider_code),
    key idx_third_provider_status(status)
) engine=innodb comment='第三方 HTTP 供应商';

create table third_endpoint (
    endpoint_id bigint(20) not null comment 'Endpoint主键',
    provider_id bigint(20) not null comment '供应商主键',
    provider_code varchar(64) not null comment '供应商编码快照',
    endpoint_code varchar(64) not null comment 'Endpoint编码',
    endpoint_name varchar(128) not null comment 'Endpoint名称',
    http_method varchar(10) not null comment 'HTTP方法白名单',
    relative_path varchar(512) not null comment '相对路径，不允许完整URL',
    request_mode varchar(16) not null default 'JSON' comment 'JSON/QUERY/FORM',
    response_mode varchar(16) not null default 'JSON' comment 'JSON/TEXT/BYTES',
    path_schema_json json default null, query_schema_json json default null,
    header_schema_json json default null, body_schema_json json default null,
    response_schema_json json default null, override_json json default null,
    status char(1) not null default '0', idempotent tinyint(1) not null default 0,
    rate_limit int not null default 0, concurrency_limit int not null default 0,
    retry_count int not null default 0, sensitive_fields_json json default null,
    adapter_code varchar(128) default null comment '显式SPI编码',
    version int not null default 0,
    create_dept bigint(20) default null, create_by bigint(20) default null,
    create_time datetime default null, update_by bigint(20) default null,
    update_time datetime default null, del_flag char(1) not null default '0',
    primary key (endpoint_id), unique key uk_third_endpoint_code(provider_code, endpoint_code),
    unique key uk_third_endpoint_owner(provider_id, endpoint_id),
    key idx_third_endpoint_provider(provider_id),
    constraint fk_third_endpoint_provider foreign key(provider_id) references third_provider(provider_id)
) engine=innodb comment='第三方 HTTP Endpoint 元数据';

create table third_credential (
    credential_id bigint(20) not null comment '凭据主键',
    provider_id bigint(20) default null, endpoint_id bigint(20) default null,
    scope_type varchar(16) not null comment 'PROVIDER/ENDPOINT',
    credential_type varchar(32) not null comment 'API_KEY/SECRET/PRIVATE_KEY/ENCRYPTION_KEY',
    ciphertext varbinary(4096) not null, nonce binary(12) not null, auth_tag binary(16) not null,
    kek_version varchar(64) not null, expires_at datetime default null,
    version int not null default 0, create_dept bigint(20) default null, create_by bigint(20) default null,
    create_time datetime default null, update_by bigint(20) default null, update_time datetime default null,
    del_flag char(1) not null default '0',
    active_scope_key varchar(192) generated always as (
        case when del_flag='0' then concat(scope_type, ':',
            case when scope_type='PROVIDER' then cast(provider_id as char) else cast(endpoint_id as char) end,
            ':', credential_type) else null end
    ) stored comment '仅活动凭据唯一键',
    primary key (credential_id),
    unique key uk_third_credential_active_scope(active_scope_key),
    key idx_third_credential_provider(provider_id), key idx_third_credential_endpoint(endpoint_id),
    constraint ck_third_credential_scope check ((scope_type='PROVIDER' and provider_id is not null and endpoint_id is null) or (scope_type='ENDPOINT' and provider_id is not null and endpoint_id is not null)),
    constraint fk_third_credential_provider foreign key(provider_id) references third_provider(provider_id),
    constraint fk_third_credential_endpoint_owner foreign key(provider_id, endpoint_id) references third_endpoint(provider_id, endpoint_id)
) engine=innodb comment='第三方 HTTP 密文凭据';

create table third_invocation (
    invocation_id bigint(20) not null, request_id varchar(64) not null,
    provider_code varchar(64) not null, endpoint_code varchar(64) not null,
    attempt_count int not null default 0, logical_status varchar(16) not null,
    failure_category varchar(32) default null, http_status int default null,
    provider_error_code varchar(128) default null, duration_ms bigint not null default 0,
    sanitized_request_json json default null, sanitized_response_json json default null,
    create_time datetime not null, primary key(invocation_id),
    unique key uk_third_invocation_request(request_id),
    key idx_third_invocation_provider_time(provider_code, create_time),
    key idx_third_invocation_endpoint_time(provider_code, endpoint_code, create_time)
) engine=innodb comment='第三方 HTTP 调用明细';

create table third_statistic (
    statistic_id bigint(20) not null, provider_code varchar(64) not null,
    endpoint_code varchar(64) default null, stat_date date not null,
    attempt_count bigint not null default 0, success_count bigint not null default 0,
    failure_count bigint not null default 0, timeout_count bigint not null default 0,
    rejected_count bigint not null default 0, quota_value bigint not null default 0,
    endpoint_dimension varchar(64) generated always as (ifnull(endpoint_code, '')) stored comment '供应商汇总使用空维度',
    primary key(statistic_id), unique key uk_third_statistic_dimension(provider_code, endpoint_dimension, stat_date),
    constraint ck_third_statistic_endpoint check (endpoint_code is null or endpoint_code <> ''),
    key idx_third_statistic_date(stat_date)
) engine=innodb comment='第三方 HTTP 调用聚合统计';

create table notify_notice (
    notice_id bigint(20) not null comment '通知公告主键',
    notice_title varchar(50) not null comment '公告标题',
    notice_type varchar(10) not null comment '公告类型',
    notice_content longblob comment '公告内容',
    recipient_type varchar(16) not null default 'ALL' comment '发送对象类型（ALL USER USER_TYPE）',
    recipient_ids_json json default null comment '指定用户 ID 快照',
    user_type_ids_json json default null comment '指定用户类型 ID 快照',
    channels_json json default null comment '发送渠道快照',
    status char(1) not null default '1' comment '草稿状态（0正常 1关闭）',
    lifecycle varchar(16) not null default 'DRAFT' comment '生命周期（DRAFT PUBLISHED RETRACTED）',
    published_at datetime default null comment '发布时间',
    retracted_at datetime default null comment '撤回时间',
    remark varchar(500) default null comment '备注',
    create_dept bigint(20) default null comment '创建部门',
    create_by bigint(20) default null comment '创建者',
    create_time datetime not null comment '创建时间',
    update_by bigint(20) default null comment '更新者',
    update_time datetime default null comment '更新时间',
    primary key (notice_id),
    key idx_notify_notice_status_time (lifecycle, status, create_time)
) engine=innodb comment='通知中心公告';

create table notify_notice_snapshot (
    snapshot_id bigint(20) not null comment '公告快照主键',
    notice_id bigint(20) not null comment '公告主键',
    snapshot_version int not null default 1 comment '公告发布快照版本',
    title_snapshot varchar(500) not null comment '标题快照',
    content_snapshot longblob comment '内容快照',
    notice_type varchar(10) not null comment '公告类型快照',
    path_snapshot varchar(500) default null comment '跳转路径快照',
    published_at datetime not null comment '快照发布时间',
    create_by bigint(20) default null comment '创建者',
    create_time datetime not null comment '创建时间',
    primary key (snapshot_id),
    unique key uk_notify_notice_snapshot_notice_version (notice_id, snapshot_version),
    key idx_notify_notice_snapshot_time (published_at)
) engine=innodb comment='已发布公告不可变快照';

create table notify_intent (
    intent_id bigint(20) not null comment '通知意图主键',
    app_id varchar(64) not null comment '通知应用标识',
    scene_code varchar(128) not null comment '业务场景编码',
    biz_type varchar(64) default null comment '业务类型',
    biz_id varchar(128) default null comment '业务主键',
    template_code varchar(128) not null comment '逻辑模板编码',
    template_params_json json default null comment '模板参数快照',
    strategy varchar(32) not null comment '编排策略',
    mode varchar(16) not null comment '执行模式',
    priority int not null default 0 comment '优先级',
    scheduled_at datetime default null comment '计划发送时间',
    expires_at datetime default null comment '截止时间',
    idempotency_key varchar(255) default null comment '应用幂等键',
    status varchar(32) not null comment '聚合状态',
    title_snapshot varchar(500) default null comment '标题快照',
    content_snapshot longtext default null comment '内容快照',
    path_snapshot varchar(500) default null comment '跳转路径快照',
    metadata_json json default null comment '脱敏元数据',
    version int not null default 0 comment '乐观锁版本',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime not null comment '创建时间',
    update_time datetime default null comment '更新时间',
    create_by bigint(20) default null comment '创建者',
    update_by bigint(20) default null comment '更新者',
    primary key (intent_id),
    unique key uk_notify_intent_app_idempotency (app_id, idempotency_key),
    key idx_notify_intent_scene_time (scene_code, create_time),
    key idx_notify_intent_status_time (status, create_time),
    key idx_notify_intent_biz (biz_type, biz_id)
) engine=innodb comment='统一通知意图';

create table notify_recipient (
    recipient_id bigint(20) not null comment '接收者主键',
    intent_id bigint(20) not null comment '通知意图主键',
    recipient_type varchar(16) not null comment '接收者类型',
    recipient_key varchar(128) not null comment '接收者逻辑键',
    user_id bigint(20) default null comment '系统用户主键',
    target_snapshot_json json default null comment '联系方式快照',
    status varchar(32) not null comment '接收者状态',
    suppress_reason varchar(128) default null comment '抑制原因',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime not null comment '创建时间',
    update_time datetime default null comment '更新时间',
    create_by bigint(20) default null comment '创建者',
    update_by bigint(20) default null comment '更新者',
    primary key (recipient_id),
    unique key uk_notify_recipient_intent_key (intent_id, recipient_key),
    key idx_notify_recipient_user (user_id, create_time)
) engine=innodb comment='通知接收者快照';

create table notify_delivery (
    delivery_id bigint(20) not null comment '投递主键',
    intent_id bigint(20) not null comment '通知意图主键',
    recipient_id bigint(20) not null comment '接收者主键',
    user_id bigint(20) default null comment '系统用户主键',
    channel varchar(32) not null comment '通知渠道',
    target_value varchar(1000) default null comment '脱敏或加密目标',
    status varchar(32) not null comment '投递状态',
    attempt_count int not null default 0 comment '尝试次数',
    provider_key varchar(64) default null comment '供应商标识',
    provider_message_id varchar(255) default null comment '供应商消息标识',
    error_code varchar(128) default null comment '错误码',
    error_message varchar(2000) default null comment '清洗后的错误信息',
    accepted_at datetime default null comment '供应商接受时间',
    receipt_query_at datetime default null comment '短信状态下次核对时间，不触发重发',
    delivered_at datetime default null comment '最终送达时间',
    read_at datetime default null comment '用户阅读时间',
    version int not null default 0 comment '乐观锁版本',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime not null comment '创建时间',
    update_time datetime default null comment '更新时间',
    create_by bigint(20) default null comment '创建者',
    update_by bigint(20) default null comment '更新者',
    primary key (delivery_id),
    unique key uk_notify_delivery_recipient_channel (recipient_id, channel),
    key idx_notify_delivery_intent (intent_id),
    key idx_notify_delivery_status_time (status, create_time),
    key idx_notify_delivery_receipt_query (channel, status, receipt_query_at, accepted_at),
    key idx_notify_delivery_account_message (provider_key, channel, provider_message_id),
    key idx_notify_delivery_provider_message (provider_message_id)
) engine=innodb comment='用户渠道投递';

create table notify_provider_receipt (
    provider_receipt_id bigint not null comment '回执主键',
    event_key char(64) character set ascii collate ascii_bin not null comment '渠道账号事件唯一摘要',
    facts_hash char(64) character set ascii collate ascii_bin not null comment '不含传输时间戳的业务事实摘要',
    delivery_id bigint not null comment '已关联的投递主键',
    version int not null default 0 comment '乐观锁版本',
    create_dept bigint default null comment '创建部门',
    create_time datetime not null comment '首次处理时间',
    create_by bigint default null comment '创建者',
    update_time datetime default null comment '更新时间',
    update_by bigint default null comment '更新者',
    del_flag char(1) not null default '0' comment '逻辑删除标志（0正常 1删除）',
    primary key (provider_receipt_id),
    unique key uk_notify_provider_receipt_event (event_key),
    key idx_notify_provider_receipt_delivery (delivery_id)
) engine=innodb comment='通知回执持久幂等凭据';

create table notify_attempt (
    attempt_id bigint(20) not null comment 'Provider 尝试主键',
    intent_id bigint(20) not null comment '通知意图主键',
    delivery_id bigint(20) not null comment '投递主键',
    attempt_no int not null comment '尝试序号',
    provider_key varchar(64) default null comment '供应商标识',
    status varchar(32) not null comment '尝试状态',
    provider_message_id varchar(255) default null comment '供应商消息标识',
    error_category varchar(64) default null comment '错误分类',
    error_code varchar(128) default null comment '错误码',
    error_message varchar(2000) default null comment '清洗后的错误信息',
    cost_time bigint not null default 0 comment '调用耗时毫秒',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime not null comment '创建时间',
    update_time datetime default null comment '更新时间',
    create_by bigint(20) default null comment '创建者',
    update_by bigint(20) default null comment '更新者',
    primary key (attempt_id),
    unique key uk_notify_attempt_delivery_no (delivery_id, attempt_no),
    key idx_notify_attempt_provider_time (provider_key, create_time)
) engine=innodb comment='供应商尝试';

create table notify_outbox (
    outbox_id bigint(20) not null comment 'Outbox 主键',
    intent_id bigint(20) not null comment '通知意图主键',
    delivery_id bigint(20) not null comment '投递主键',
    status varchar(32) not null comment '任务状态',
    available_at datetime not null comment '可领取时间',
    attempt_count int not null default 0 comment '领取次数',
    next_attempt_at datetime default null comment '下次尝试时间',
    lease_owner varchar(128) default null comment '租约持有者',
    lease_until datetime default null comment '租约截止时间',
    lease_token varchar(64) default null comment '租约 fencing token',
    max_attempts int not null default 5 comment '最大尝试次数',
    last_error_code varchar(128) default null comment '最近错误码',
    last_error_message varchar(2000) default null comment '最近清洗错误',
    create_dept bigint(20) default null comment '创建部门',
    create_time datetime not null comment '创建时间',
    update_time datetime default null comment '更新时间',
    create_by bigint(20) default null comment '创建者',
    update_by bigint(20) default null comment '更新者',
    primary key (outbox_id),
    unique key uk_notify_outbox_delivery (delivery_id, status),
    key idx_notify_outbox_ready (status, available_at, next_attempt_at),
    key idx_notify_outbox_lease (lease_until)
) engine=innodb comment='通知可靠 Outbox';

create table notify_message (
    message_id bigint(20) not null comment '站内消息主键',
    category varchar(32) not null comment '消息分类',
    notice_type varchar(10) default null comment '通知类型',
    channels_json json default null comment '发送渠道快照',
    type varchar(32) not null comment '消息类型',
    source varchar(32) not null comment '消息来源',
    title varchar(255) default null comment '消息标题',
    message varchar(1000) default null comment '消息摘要',
    content longtext comment '消息内容快照',
    data_json json default null comment '扩展数据',
    path varchar(500) default null comment '跳转路径',
    send_user_ids varchar(4000) default null comment '发送用户快照',
    create_dept bigint(20) default null comment '创建部门',
    create_by bigint(20) default null comment '创建者',
    create_time datetime not null comment '创建时间',
    update_by bigint(20) default null comment '更新者',
    update_time datetime default null comment '更新时间',
    primary key (message_id),
    key idx_notify_message_category_time (category, create_time)
) engine=innodb comment='通知中心站内消息';

create table notify_message_recipient (
    message_recipient_id bigint(20) not null comment '收件关系主键',
    message_id bigint(20) not null comment '站内消息主键',
    user_id bigint(20) not null comment '用户主键',
    seen_time datetime default null comment '已见时间',
    read_time datetime default null comment '已读时间',
    create_dept bigint(20) default null comment '创建部门',
    create_by bigint(20) default null comment '创建者',
    create_time datetime not null comment '创建时间',
    update_by bigint(20) default null comment '更新者',
    update_time datetime default null comment '更新时间',
    primary key (message_recipient_id),
    unique key uk_notify_message_recipient (message_id, user_id),
    key idx_notify_message_recipient_user (user_id, read_time, create_time)
) engine=innodb comment='通知中心收件人及阅读状态';

create table notify_channel_account (
    account_id bigint(20) not null comment '渠道账号主键',
    channel varchar(16) not null comment '渠道 MAIL SMS',
    config_key varchar(64) not null comment '账号标识',
    enabled char(1) not null default 'N' comment '是否启用（Y是 N否）',
    supplier varchar(64) default null comment '短信厂商标识',
    host varchar(128) default null comment 'SMTP 主机',
    port int default null comment 'SMTP 端口',
    mail_from varchar(128) default null comment '发件人',
    mail_user varchar(128) default null comment 'SMTP 用户名',
    mail_pass varchar(256) default null comment 'SMTP 密码',
    ssl_enable char(1) default 'N' comment '是否启用 SSL',
    starttls_enable char(1) default 'N' comment '是否启用 STARTTLS',
    access_key_id varchar(128) default null comment '短信 AccessKey',
    access_key_secret varchar(256) default null comment '短信密钥',
    signature varchar(128) default null comment '短信签名',
    sdk_app_id varchar(128) default null comment '短信应用 ID',
    minute_max int not null default 60 comment '账号每分钟上限',
    remark varchar(500) default null comment '备注',
    version int not null default 0 comment '乐观锁版本',
    create_dept bigint(20) default null comment '创建部门',
    create_by bigint(20) default null comment '创建者',
    create_time datetime not null comment '创建时间',
    update_by bigint(20) default null comment '更新者',
    update_time datetime default null comment '更新时间',
    del_flag char(1) not null default '0' comment '逻辑删除标志（0正常 1删除，保留配置标识）',
    primary key (account_id),
    unique key uk_notify_channel_account (channel, config_key)
) engine=innodb comment='通知渠道账号';

create table notify_scene_binding (
    binding_id bigint(20) not null comment '场景绑定主键',
    scene_code varchar(64) not null comment '逻辑场景编码',
    channel varchar(16) not null comment '渠道 MAIL SMS',
    account_id bigint(20) default null comment '绑定账号主键',
    mail_subject varchar(500) default null comment '邮件主题模板',
    mail_body longtext comment '邮件正文模板',
    sms_template_code varchar(128) default null comment '短信供应商模板码',
    sms_param_mapping_json json default null comment '逻辑变量到供应商参数映射',
    template_minute_max int not null default 60 comment '模板每分钟上限',
    restricted char(1) not null default 'N' comment '是否启用收件人拦截',
    recipient_minute_max int not null default 0 comment '同一收件人每分钟上限',
    recipient_day_max int not null default 0 comment '同一收件人每天上限',
    create_dept bigint(20) default null comment '创建部门',
    create_by bigint(20) default null comment '创建者',
    create_time datetime not null comment '创建时间',
    update_by bigint(20) default null comment '更新者',
    update_time datetime default null comment '更新时间',
    primary key (binding_id),
    unique key uk_notify_scene_binding (scene_code, channel),
    key idx_notify_scene_binding_account (account_id)
) engine=innodb comment='通知场景渠道绑定';

create table sso_authorization_code (
    authorization_code_id bigint(20) not null comment '授权码主键',
    authorization_code varchar(128) not null comment '一次性授权码',
    client_id varchar(64) not null comment '目标业务客户端标识',
    redirect_uri varchar(1000) not null comment '绑定的回调地址',
    code_challenge varchar(128) not null comment 'PKCE S256 挑战',
    state varchar(128) default null comment 'CSRF state',
    user_id bigint(20) not null comment '已认证用户主键',
    username varchar(64) not null comment '已认证用户名',
    consumed tinyint(1) not null default 0 comment '是否已消费（0否 1是）',
    expire_time datetime not null comment '过期时间',
    version int default 0 comment '乐观锁版本',
    create_dept bigint(20) default null comment '创建部门',
    create_by bigint(20) default null comment '创建者',
    create_time datetime default null comment '创建时间',
    update_by bigint(20) default null comment '更新者',
    update_time datetime default null comment '更新时间',
    del_flag char(1) default '0' comment '删除标志（0代表存在 1代表删除）',
    primary key (authorization_code_id),
    unique key uk_sso_authorization_code (authorization_code),
    key idx_sso_authorization_code_expire (expire_time)
) engine=innodb comment='SSO 一次性授权码';

CREATE TABLE test_demo
(
    id          bigint(0)    NOT NULL COMMENT '主键',
    dept_id     bigint(0)    NULL DEFAULT NULL COMMENT '部门id',
    user_id     bigint(0)    NULL DEFAULT NULL COMMENT '用户id',
    order_num   int(0)       NULL DEFAULT 0 COMMENT '排序号',
    test_key    varchar(255) NULL DEFAULT NULL COMMENT 'key键',
    value       varchar(255) NULL DEFAULT NULL COMMENT '值',
    version     int(0)       NULL DEFAULT 0 COMMENT '版本',
    create_dept bigint(0)    NULL DEFAULT NULL COMMENT '创建部门',
    create_time datetime(0)  NULL DEFAULT NULL COMMENT '创建时间',
    create_by   bigint(0)    NULL DEFAULT NULL COMMENT '创建人',
    update_time datetime(0)  NULL DEFAULT NULL COMMENT '更新时间',
    update_by   bigint(0)    NULL DEFAULT NULL COMMENT '更新人',
    del_flag    int(0)       NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB COMMENT = '测试单表';

CREATE TABLE test_tree
(
    id          bigint(0)    NOT NULL COMMENT '主键',
    parent_id   bigint(0)    NULL DEFAULT 0 COMMENT '父id',
    dept_id     bigint(0)    NULL DEFAULT NULL COMMENT '部门id',
    user_id     bigint(0)    NULL DEFAULT NULL COMMENT '用户id',
    tree_name   varchar(255) NULL DEFAULT NULL COMMENT '值',
    version     int(0)       NULL DEFAULT 0 COMMENT '版本',
    create_dept bigint(0)    NULL DEFAULT NULL COMMENT '创建部门',
    create_time datetime(0)  NULL DEFAULT NULL COMMENT '创建时间',
    create_by   bigint(0)    NULL DEFAULT NULL COMMENT '创建人',
    update_time datetime(0)  NULL DEFAULT NULL COMMENT '更新时间',
    update_by   bigint(0)    NULL DEFAULT NULL COMMENT '更新人',
    del_flag    int(0)       NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id) USING BTREE,
    KEY idx_test_tree_parent_id (parent_id)
) ENGINE = InnoDB COMMENT = '测试树表';
