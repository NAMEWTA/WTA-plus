-- 已有库升级差异（从 50-namewta-ddl.sql / 60-namewta-dml.sql 抽出）。
-- 不得对已填充库重放六份初始化基座全文。执行前备份，并按源/目标 Git Tag 评审。
-- 全新环境只跑 init/ 六份基座，不要再跑本文件。

create table if not exists notify_channel_account (
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
    create_dept bigint(20) default null comment '创建部门',
    create_by bigint(20) default null comment '创建者',
    create_time datetime not null comment '创建时间',
    update_by bigint(20) default null comment '更新者',
    update_time datetime default null comment '更新时间',
    primary key (account_id),
    unique key uk_notify_channel_account (channel, config_key)
) engine=innodb comment='通知渠道账号';

create table if not exists notify_scene_binding (
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

insert ignore into sys_menu
    (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values
    (2100600000000000050, 1762000000000000001, '通知配置', 2100600000000000001, 4, 'config', 'notify/config/index', '', 'N', 'Y', 'C', '0', '0', 'notify:config:list', 'tabler:settings', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '邮件短信渠道账号与场景绑定'),
    (2100600000000000051, 1762000000000000001, '配置查询', 2100600000000000050, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:config:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '查询渠道账号'),
    (2100600000000000052, 1762000000000000001, '配置新增', 2100600000000000050, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:config:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '新增渠道账号'),
    (2100600000000000053, 1762000000000000001, '配置修改', 2100600000000000050, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:config:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '修改渠道账号与场景绑定'),
    (2100600000000000054, 1762000000000000001, '配置删除', 2100600000000000050, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:config:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '删除渠道账号'),
    (2100600000000000055, 1762000000000000001, '配置试发', 2100600000000000050, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:config:test', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '测试发送邮件或短信');

insert ignore into sys_role_menu (role_id, menu_id)
select 1761300000000000001, menu_id from sys_menu where menu_id in
    (2100600000000000050, 2100600000000000051, 2100600000000000052,
     2100600000000000053, 2100600000000000054, 2100600000000000055);

insert ignore into notify_scene_binding
    (binding_id, scene_code, channel, account_id, mail_subject, mail_body, sms_template_code,
     sms_param_mapping_json, template_minute_max, restricted, recipient_minute_max, recipient_day_max,
     create_dept, create_by, create_time, update_by, update_time)
values
    (2100630000000000001, 'auth-captcha', 'MAIL', null, '登录验证码',
     '您的验证码为 ${code}，${expireMinutes} 分钟内有效。', '', '{}', 60, 'Y', 1, 30,
     1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate()),
    (2100630000000000002, 'auth-captcha', 'SMS', null, '', '', '',
     '{"code":"code","expireMinutes":"expireMinutes"}', 60, 'Y', 1, 30,
     1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate()),
    (2100630000000000003, 'person-rebind', 'MAIL', null, '实名认证绑定变更',
     '您的实名认证绑定已变更。', '', '{}', 60, 'N', 0, 0,
     1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate()),
    (2100630000000000004, 'person-rebind', 'SMS', null, '', '', '', '{}', 60, 'N', 0, 0,
     1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate()),
    (2100630000000000005, 'enterprise-transfer', 'MAIL', null, '企业负责人转移验证码',
     '您的企业负责人转移验证码为 ${code}。', '', '{}', 60, 'N', 0, 0,
     1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate()),
    (2100630000000000006, 'enterprise-transfer', 'SMS', null, '', '', '', '{"code":"code"}', 60, 'N', 0, 0,
     1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate()),
    (2100630000000000007, 'workflow-task', 'MAIL', null, '${title}',
     '${content}<p>${path}</p>', '', '{}', 60, 'N', 0, 0,
     1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate()),
    (2100630000000000008, 'workflow-task', 'SMS', null, '', '', '',
     '{"title":"title","content":"content","path":"path"}', 60, 'N', 0, 0,
     1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate()),
    (2100630000000000009, 'notice-published', 'MAIL', null, '${title}',
     '${content}<p>${path}</p>', '', '{}', 60, 'N', 0, 0,
     1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate()),
    (2100630000000000010, 'notice-published', 'SMS', null, '', '', '',
     '{"title":"title","content":"content","path":"path"}', 60, 'N', 0, 0,
     1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate());
