SET NAMES utf8mb4;

-- ============================================================================
-- NAMEWTA 数据 SQL
-- 本文件中的 DSL 是项目约定的数据类 SQL，包含初始化、回填和补偿语句。
-- 本文件是可直接修改的当前完整 MySQL 8.4 数据基座，仅用于全新数据库初始化。
-- 历史变更标识和执行说明只保留追溯语义，不是已有数据库的升级步骤。
-- 已有数据库必须按源/目标 Git Tag 生成并评审差异，禁止重放本文件。
-- ============================================================================

-- ============================================================================
-- 变更标识：NAMEWTA-BASE-DSL-001
-- 变更内容：登录域、Client、角色、菜单及关系初始化
-- 执行前置：已完整执行 DDL.sql
-- 适用范围：全新环境；仅有 ry_vue.sql 基线且尚未执行旧 003 的升级环境
-- 重复执行：否
-- 回滚方式：按本块固定主键逆序删除新增关系与数据，并恢复被回填字段及全局注册配置
-- ============================================================================

-- ----------------------------
-- 两个登录域
-- ----------------------------
insert into sys_user_type values (1762100000000000001, 'sys_user', '系统用户', 1, '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '管理后台登录域');
insert into sys_user_type values (1762100000000000002, 'app_user', '应用用户', 2, '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '用户端登录域');

-- ----------------------------
-- 回填已有用户的系统登录域
-- ----------------------------
insert into sys_user_type_rel values (1762200000000000001, 1761100000000000001, 1762100000000000001, 'SYSTEM_INIT', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
insert into sys_user_type_rel values (1762200000000000002, 1761100000000000003, 1762100000000000001, 'SYSTEM_INIT', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
insert into sys_user_type_rel values (1762200000000000003, 1761100000000000004, 1762100000000000001, 'SYSTEM_INIT', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);

-- ----------------------------
-- 已有角色、菜单归属管理端 Client（sys_client.id = 1762000000000000001）
-- ----------------------------
update sys_role set client_id = 1762000000000000001 where client_id is null;
update sys_menu set client_id = 1762000000000000001 where client_id is null;

-- ----------------------------
-- 只保留管理端和用户端两个 Client，以及各自的默认角色
-- ----------------------------
delete from sys_role_menu where role_id in (1761300000000000003, 1761300000000000004);
delete from sys_user_role where role_id in (1761300000000000003, 1761300000000000004);
delete from sys_role where role_id in (1761300000000000003, 1761300000000000004);
delete from sys_menu where client_id in (1762000000000000003, 1762000000000000004);
delete from sys_client where id in (1762000000000000003, 1762000000000000004);

insert into sys_role (role_id, client_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, remark)
values (1761300000000000010, 1762000000000000002, '应用用户', 'app_user', 1, '5', 1, 1, '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), '用户端默认角色');

update sys_client
set user_type_id     = 1762100000000000001,
    register_enabled = 0,
    default_role_id  = null
where id = 1762000000000000001;

update sys_client
set user_type_id     = 1762100000000000002,
    register_enabled = 1,
    default_role_id  = 1761300000000000010,
    client_key       = 'home',
    client_secret    = 'home123',
    grant_type       = 'password,sms,social',
    device_type      = 'pc',
    access_path      = '/home/**'
where id = 1762000000000000002;

-- ----------------------------
-- 登录域管理菜单（归属管理端 Client）
-- ----------------------------
insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values (1761400000000000124, 1762000000000000001, '登录域管理', 1761400000000000001, 12, 'userType', 'system/userType/index', '', 'N', 'Y', 'C', '0', '0', 'system:userType:list', 'tabler:users', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '登录域管理菜单');

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values (1761400000000001070, 1762000000000000001, '登录域查询', 1761400000000000124, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:userType:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), ''),
       (1761400000000001071, 1762000000000000001, '登录域新增', 1761400000000000124, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:userType:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), ''),
       (1761400000000001072, 1762000000000000001, '登录域修改', 1761400000000000124, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:userType:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), ''),
       (1761400000000001073, 1762000000000000001, '登录域删除', 1761400000000000124, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:userType:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), ''),
       (1761400000000001074, 1762000000000000001, '登录域导出', 1761400000000000124, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:userType:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '');

-- 将登录域菜单授予管理端超管角色（sys_client.id = 1762000000000000001，OAuth client_id = e5cd7e4891bf95d1d19206ce24a7b32e）
insert into sys_role_menu (role_id, menu_id)
values (1761300000000000001, 1761400000000000124),
       (1761300000000000001, 1761400000000001070),
       (1761300000000000001, 1761400000000001071),
       (1761300000000000001, 1761400000000001072),
       (1761300000000000001, 1761400000000001073),
       (1761300000000000001, 1761400000000001074);

-- ----------------------------
-- 删除全局注册开关，改由 Client.register_enabled 控制
-- ----------------------------
delete from sys_config where config_key = 'sys.account.registerUser';

-- ============================================================================
-- 变更标识：NAMEWTA-BASE-DSL-002
-- 用户端基础菜单已由当前基座统一初始化，旧补偿块不再执行。
-- 执行前置：已执行 NAMEWTA-BASE-DSL-001 或旧 003_initial_data.sql
-- 适用范围：历史升级记录；当前完整基座不执行旧菜单补偿。
-- 重复执行：是
-- 回滚方式：无。
-- ============================================================================

-- ============================================================================
-- 变更标识：NAMEWTA-OSS-NOTIFY-DSL-001
-- 变更内容：通知监控动态菜单与功能权限
-- 执行前置：已执行 NAMEWTA-OSS-NOTIFY-DDL-001
-- 适用范围：全新环境；已完成当前基础数据初始化
-- 重复执行：是
-- 回滚方式：先撤销角色授权，再删除以下固定 menu_id；不删除通知日志数据
-- ============================================================================

-- 通知监控是全局运维功能。此处只定义菜单和权限，不自动扩大普通角色授权。
insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
select 1761400000000000125, 1762000000000000001, '通知监控', 1761400000000000108, 3, 'notify-monitor', 'notify/monitor/index', '', 'N', 'Y', 'C', '0', '0', 'notify:monitor:list', 'tabler:messages', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '通知监控菜单'
from dual
where not exists (select 1 from sys_menu where menu_id = 1761400000000000125);

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
select 1761400000000001080, 1762000000000000001, '通知查询', 1761400000000000125, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:notify:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 1761400000000001080);

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
select 1761400000000001081, 1762000000000000001, '通知删除', 1761400000000000125, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:notify:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 1761400000000001081);

-- ============================================================================
-- NAMEWTA-BASE-DSL-004
-- 当前完整基座只保留管理端与用户端两个 Client、两个角色和用户档案中心。
delete from sys_role_menu
where role_id in (1761300000000000003, 1761300000000000004, 1761300000000000011, 1761300000000000012)
   or menu_id in (1761400000000002001, 1761400000000002002, 1761400000000002003);
delete from sys_user_role
where role_id in (1761300000000000003, 1761300000000000004, 1761300000000000011, 1761300000000000012);
delete from sys_menu
where menu_id in (1761400000000002001, 1761400000000002002, 1761400000000002003)
   or client_id in (1762000000000000003, 1762000000000000004);
delete from sys_role
where role_id in (1761300000000000003, 1761300000000000004, 1761300000000000011, 1761300000000000012);
delete from sys_client where id in (1762000000000000003, 1762000000000000004);

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values (2100500000000000100, 1762000000000000002, '档案中心', 0, 1, 'profile', 'profile/center/index', '', 'N', 'Y', 'M', '0', '0', '', 'tabler:id', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '用户档案认证中心'),
       (2100500000000000101, 1762000000000000002, '个人认证', 2100500000000000100, 1, 'person', 'profile/person/application', '', 'N', 'Y', 'C', '1', '0', 'profile:person:apply', 'tabler:user', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '个人实名认证申请'),
       (2100500000000000102, 1762000000000000002, '企业认证', 2100500000000000100, 2, 'enterprise', 'profile/enterprise/application', '', 'N', 'Y', 'C', '1', '0', 'profile:enterprise:apply', 'tabler:building', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '企业实名认证申请');
insert into sys_role_menu (role_id, menu_id)
values (1761300000000000010, 2100500000000000100),
       (1761300000000000010, 2100500000000000101),
       (1761300000000000010, 2100500000000000102);

-- NAMEWTA-BASE-DSL-004-END

-- NAMEWTA-PASSWORD-DSL-001
-- ============================================================================
-- 变更内容：启用统一密码策略、随机化退役初始密码并新增临时密码独立权限
-- 变更标识：2026-08-28_18:21:43
-- 执行前置：已按 ry_vue.sql -> NAMEWTA DDL.sql -> NAMEWTA DML.sql 顺序建立当前基线；发布前暂停旧密码写入口
-- 适用范围：fresh 与尚未执行本块的 upgrade 环境；只支持 MySQL 8.4
-- 重复执行：是；策略或菜单已按本块 ID 存在时保留当前配置，旧键仅首次随机化
-- 回滚方式：按下方回滚步骤使用迁移前备份恢复旧键，删除本块 config/menu；绝不修改 sys_user.password
-- ============================================================================

-- 前置不符时利用 CHECK 约束立即停止，避免在 key、ID 或父菜单冲突时部分写入。
drop temporary table if exists namewta_password_dsl_001_preflight;
create temporary table namewta_password_dsl_001_preflight (
    preflight_ok tinyint not null,
    constraint chk_namewta_password_dsl_001_preflight check (preflight_ok = 1)
);

insert into namewta_password_dsl_001_preflight (preflight_ok)
select if(
    (select count(*) from sys_config where config_key = 'sys.user.initPassword') = 1
    and (select count(*) from sys_config where config_key = 'sys.user.passwordPolicy') <= 1
    and not exists (
        select 1 from sys_config
        where config_id = 2093282875312267265 and not (config_key <=> 'sys.user.passwordPolicy')
    )
    and not exists (
        select 1 from sys_config
        where config_key = 'sys.user.passwordPolicy' and config_id <> 2093282875312267265
    )
    and exists (
        select 1 from sys_menu
        where menu_id = 1761400000000000100 and menu_type = 'C' and perms = 'system:user:list'
    )
    and (select count(*) from sys_menu where perms = 'system:user:temporaryPassword') <= 1
    and not exists (
        select 1 from sys_menu
        where menu_id = 2093282875312267266 and not (perms <=> 'system:user:temporaryPassword')
    )
    and not exists (
        select 1 from sys_menu
        where perms = 'system:user:temporaryPassword' and menu_id <> 2093282875312267266
    )
    and not exists (
        select 1 from sys_menu
        where menu_id = 2093282875312267266
          and not (client_id <=> 1762000000000000001
              and parent_id <=> 1761400000000000100
              and menu_type <=> 'F')
    ),
    1,
    0
);

drop temporary table namewta_password_dsl_001_preflight;

-- 旧 backend 在滚动窗口仍读取此键。首次执行时生成每环境独立、满足 v1 四类规则的兼容值；不输出生成结果。
update sys_config
set config_value = concat(char(65), char(97), char(49), char(33), upper(hex(random_bytes(8)))),
    update_by = 1761100000000000001,
    update_time = sysdate(),
    remark = concat_ws('；', nullif(remark, ''), 'NAMEWTA-PASSWORD-DSL-001：旧键已随机化并退役')
where config_key = 'sys.user.initPassword'
  and remark not like '%NAMEWTA-PASSWORD-DSL-001%';

insert into sys_config (config_id, config_name, config_key, config_value, config_type,
                        create_dept, create_by, create_time, update_by, update_time, remark)
select 2093282875312267265, '统一密码策略', 'sys.user.passwordPolicy',
       '{"version":1,"minimumLength":8,"maximumLength":30,"requireUppercase":true,"requireLowercase":true,"requireDigit":true,"requireSpecial":true,"allowedSpecialCharacters":"@$!%*?&","generator":{"length":12,"uppercaseCharacters":"ABCDEFGHJKLMNPQRSTUVWXYZ","lowercaseCharacters":"abcdefghijkmnopqrstuvwxyz","digitCharacters":"23456789","specialCharacters":"@$!%*?&"},"defaultPassword":{"mode":"RANDOM"}}',
       'Y', 1761000000000000103, 1761100000000000001, sysdate(), null, null,
       '统一密码策略 v1；保存后必须刷新 sys_config 集群缓存'
from dual
where not exists (select 1 from sys_config where config_id = 2093282875312267265);

-- 临时密码签发不继承永久重置权限；只定义功能权限，不自动授予普通角色。
insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext,
                      create_dept, create_by, create_time, remark)
select 2093282875312267266, 1762000000000000001, '签发临时密码', 1761400000000000100, 8, '', '', '',
       'N', 'Y', 'F', '0', '0', 'system:user:temporaryPassword', '#', '', '',
       1761000000000000103, 1761100000000000001, sysdate(), '60 秒、用户级、单次消费的临时密码签发权限'
from dual
where not exists (select 1 from sys_menu where menu_id = 2093282875312267266);

-- 回滚步骤（仅在 backend 回滚前执行）：
-- 1. 先回滚 frontend，并撤销各普通角色对 menu_id 2093282875312267266 的显式授权。
-- 2. 从迁移前加密备份恢复 sys.user.initPassword 的 config_value、remark 和审计字段；禁止恢复公开弱默认值。
-- 3. 删除 menu_id 2093282875312267266 及其 sys_role_menu 关系，再删除 config_id 2093282875312267265。
-- 4. 刷新 sys_config、菜单和权限的 Redis/JVM 缓存，确认旧 backend 健康后才恢复写入口。
-- 前向补偿：若 backend 已全部切换，不回退用户密码；修正冲突数据后重放本块，并刷新配置/权限缓存。

-- NAMEWTA-PASSWORD-DSL-001-END

-- ============================================================================
-- 变更标识：NAMEWTA-RUNTIME-GEN-RETIRE-DML-001
-- 变更内容：永久删除运行时代码生成器菜单及全部角色授权关系
-- 执行前置：九个固定菜单必须完整匹配冻结基线，或已被本块完整删除；
--           系统工具、代码生成、修改生成配置均不得存在非目标子菜单
-- 适用范围：全新或当前 NAMEWTA 基座初始化
-- 重复执行：是
-- 恢复方式：无；不备份、不归档、不恢复生成器权限
-- ============================================================================

create temporary table namewta_runtime_gen_retire_dml_001_preflight (
    preflight_passed tinyint not null,
    constraint chk_runtime_gen_retire_dml_001 check (preflight_passed = 1)
);

insert into namewta_runtime_gen_retire_dml_001_preflight (preflight_passed)
select if(
    (
        (select count(*) from sys_menu
         where menu_id in (
             1761400000000000003,
             1761400000000000115, 1761400000000000116,
             1761400000000001055, 1761400000000001056, 1761400000000001057,
             1761400000000001058, 1761400000000001059, 1761400000000001060
         )) = 0
        and not exists (
            select 1 from sys_menu
            where parent_id in (1761400000000000003, 1761400000000000115, 1761400000000000116)
        )
    )
    or
    (
        (select count(*) from sys_menu
         where menu_id in (
             1761400000000000003,
             1761400000000000115, 1761400000000000116,
             1761400000000001055, 1761400000000001056, 1761400000000001057,
             1761400000000001058, 1761400000000001059, 1761400000000001060
         )) = 9
        and exists (
            select 1 from sys_menu
            where menu_id = 1761400000000000003
              and client_id <=> 1762000000000000001
              and parent_id <=> 0
              and menu_type <=> 'M'
        )
        and exists (
            select 1 from sys_menu
            where menu_id = 1761400000000000115
              and client_id <=> 1762000000000000001
              and parent_id <=> 1761400000000000003
              and menu_type <=> 'C'
              and component <=> 'tool/gen/index'
              and perms <=> 'tool:gen:list'
        )
        and exists (
            select 1 from sys_menu
            where menu_id = 1761400000000000116
              and client_id <=> 1762000000000000001
              and parent_id <=> 1761400000000000003
              and menu_type <=> 'C'
              and component <=> 'tool/gen/editTable'
              and perms <=> 'tool:gen:edit'
              and active_menu <=> '/tool/gen'
        )
        and exists (
            select 1 from sys_menu
            where menu_id = 1761400000000001055
              and client_id <=> 1762000000000000001
              and parent_id <=> 1761400000000000115
              and menu_type <=> 'F'
              and perms <=> 'tool:gen:query'
        )
        and exists (
            select 1 from sys_menu
            where menu_id = 1761400000000001056
              and client_id <=> 1762000000000000001
              and parent_id <=> 1761400000000000115
              and menu_type <=> 'F'
              and perms <=> 'tool:gen:edit'
        )
        and exists (
            select 1 from sys_menu
            where menu_id = 1761400000000001057
              and client_id <=> 1762000000000000001
              and parent_id <=> 1761400000000000115
              and menu_type <=> 'F'
              and perms <=> 'tool:gen:remove'
        )
        and exists (
            select 1 from sys_menu
            where menu_id = 1761400000000001058
              and client_id <=> 1762000000000000001
              and parent_id <=> 1761400000000000115
              and menu_type <=> 'F'
              and perms <=> 'tool:gen:import'
        )
        and exists (
            select 1 from sys_menu
            where menu_id = 1761400000000001059
              and client_id <=> 1762000000000000001
              and parent_id <=> 1761400000000000115
              and menu_type <=> 'F'
              and perms <=> 'tool:gen:preview'
        )
        and exists (
            select 1 from sys_menu
            where menu_id = 1761400000000001060
              and client_id <=> 1762000000000000001
              and parent_id <=> 1761400000000000115
              and menu_type <=> 'F'
              and perms <=> 'tool:gen:code'
        )
        and not exists (
            select 1 from sys_menu
            where parent_id = 1761400000000000003
              and menu_id not in (1761400000000000115, 1761400000000000116)
        )
        and not exists (
            select 1 from sys_menu
            where parent_id = 1761400000000000115
              and menu_id not in (
                  1761400000000001055, 1761400000000001056, 1761400000000001057,
                  1761400000000001058, 1761400000000001059, 1761400000000001060
              )
        )
        and not exists (
            select 1 from sys_menu where parent_id = 1761400000000000116
        )
    ),
    1,
    0
);

delete from sys_role_menu
where menu_id in (
    1761400000000000003,
    1761400000000000115, 1761400000000000116,
    1761400000000001055, 1761400000000001056, 1761400000000001057,
    1761400000000001058, 1761400000000001059, 1761400000000001060
);

delete from sys_menu
where menu_id in (
    1761400000000001055, 1761400000000001056, 1761400000000001057,
    1761400000000001058, 1761400000000001059, 1761400000000001060
);

delete from sys_menu
where menu_id in (1761400000000000115, 1761400000000000116);

delete from sys_menu
where menu_id = 1761400000000000003;

drop temporary table namewta_runtime_gen_retire_dml_001_preflight;

-- NAMEWTA-OPENAPI-CREDENTIAL-DML-001
-- ============================================================================
-- 变更内容：新增应用开放管理菜单、管理员按钮与个人开放应用权限
-- 变更标识：2026-08-31_22:02:33
-- 执行前置：已执行 NAMEWTA-OPENAPI-CREDENTIAL-DDL-001；应用仍保持 openapi.enabled=false
-- 适用范围：全新或当前 NAMEWTA 基座初始化
-- 重复执行：否
-- 回滚方式：先删除对应 sys_role_menu 关系，再按固定主键逆序删除以下菜单
-- ============================================================================

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext,
                      create_dept, create_by, create_time, remark)
values (2094360621561675776, 1762000000000000001, '应用开放管理', 1761400000000000001, 13,
        'openApi', 'system/openApi/index', '', 'N', 'Y', 'C', '0', '0',
        'system:openApi:list', 'tabler:api', '', '', 1761000000000000103, 1761100000000000001,
        sysdate(), 'OpenAPI凭据与接口目录管理菜单');

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext,
                      create_dept, create_by, create_time, remark)
values (2094360621561675777, 1762000000000000001, '开放应用查询', 2094360621561675776, 1,
        '', '', '', 'N', 'Y', 'F', '0', '0', 'system:openApi:query', '#', '', '',
        1761000000000000103, 1761100000000000001, sysdate(), ''),
       (2094360621561675778, 1762000000000000001, '开放应用新增', 2094360621561675776, 2,
        '', '', '', 'N', 'Y', 'F', '0', '0', 'system:openApi:add', '#', '', '',
        1761000000000000103, 1761100000000000001, sysdate(), ''),
       (2094360621561675779, 1762000000000000001, '开放应用修改', 2094360621561675776, 3,
        '', '', '', 'N', 'Y', 'F', '0', '0', 'system:openApi:edit', '#', '', '',
        1761000000000000103, 1761100000000000001, sysdate(), ''),
       (2094360621561675780, 1762000000000000001, '开放应用删除', 2094360621561675776, 4,
        '', '', '', 'N', 'Y', 'F', '0', '0', 'system:openApi:remove', '#', '', '',
        1761000000000000103, 1761100000000000001, sysdate(), ''),
       (2094360621561675781, 1762000000000000001, '个人开放应用', 2094360621561675776, 5,
        '', '', '', 'N', 'Y', 'F', '0', '0', 'system:openApi:self', '#', '', '',
        1761000000000000103, 1761100000000000001, sysdate(), '个人中心开放应用权限');

-- 变更内容：将全部历史OSS访问类型保守回填为PRIVATE
-- 变更标识：2026-09-01_00:14:13
-- 执行前置：已执行 NAMEWTA-OSS-ACCESS-DDL-001；执行前应完成配置与匿名访问盘点及备份
-- 适用范围：全新环境；含旧0/1/2或未知访问类型的升级环境
-- 重复执行：是
-- 回滚方式：不恢复旧值；确需公开访问时在Provider就绪后显式新建PUBLIC_READ配置并迁移对象
-- 逻辑标识：NAMEWTA-OSS-ACCESS-DML-001
-- ============================================================================

-- 唯一默认配置是迁移前置条件。异常时故意插入两行同主键哨兵，使本块在任何UPDATE前原子失败。
insert into sys_oss_config (oss_config_id, config_key, access_policy)
select -9223372036854775808, '__oss_preflight__', '0'
from dual
where (select count(*) from sys_oss_config where status = 'Y') <> 1
union all
select -9223372036854775808, '__oss_preflight__', '0'
from dual
where (select count(*) from sys_oss_config where status = 'Y') <> 1;

select count(*) as sys_oss_access_policy_backfill_count
from sys_oss_config
where access_policy <> '0';

update sys_oss_config
set access_policy = '0'
where access_policy <> '0';

-- NAMEWTA-NACOS-CONSOLE-DML-001
-- ============================================================================
-- 变更内容：在系统管理下增加 Nacos 官方配置中心入口
-- 执行前置：系统管理父菜单 1761400000000000001 已存在
-- 适用范围：全新或当前 NAMEWTA 基座初始化
-- 重复执行：是；固定菜单存在且合同一致时无操作
-- 回滚方式：先撤销显式角色授权，再删除 menu_id 2094360621561675790
-- ============================================================================

drop temporary table if exists namewta_nacos_console_dml_001_preflight;
create temporary table namewta_nacos_console_dml_001_preflight (
    preflight_ok tinyint not null,
    constraint chk_namewta_nacos_console_dml_001 check (preflight_ok = 1)
);

insert into namewta_nacos_console_dml_001_preflight (preflight_ok)
select if(
    exists (select 1 from sys_menu where menu_id = 1761400000000000001 and menu_type = 'M')
    and not exists (
        select 1 from sys_menu
        where menu_id = 2094360621561675790
          and not (client_id <=> 1762000000000000001
              and menu_name <=> '配置中心'
              and parent_id <=> 1761400000000000001
              and path <=> 'nacos'
              and component <=> 'monitor/nacos/index'
              and menu_type <=> 'C'
              and perms <=> 'system:nacos:console')
    )
    and not exists (
        select 1 from sys_menu
        where menu_id <> 2094360621561675790
          and (component = 'monitor/nacos/index' or perms = 'system:nacos:console')
    ),
    1,
    0
);

drop temporary table namewta_nacos_console_dml_001_preflight;

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext,
                      create_dept, create_by, create_time, remark)
select 2094360621561675790, 1762000000000000001, '配置中心', 1761400000000000001, 14,
       'nacos', 'monitor/nacos/index', '', 'N', 'Y', 'C', '0', '0',
       'system:nacos:console', 'tabler:server', '', '', 1761000000000000103, 1761100000000000001,
       sysdate(), 'Nacos 官方控制台入口；配置权限由 Nacos 独立鉴权'
from dual
where not exists (select 1 from sys_menu where menu_id = 2094360621561675790);

-- 不在迁移脚本中向普通角色授予入口；非超级管理员必须由管理员显式授权。
-- NAMEWTA-NACOS-CONSOLE-DML-001-END

-- NAMEWTA-ADMIN-RUNTIME-RECONCILE-DML-001
-- ============================================================================
-- 变更内容：收敛 OpenAPI、Nacos 与已退役代码生成器的 Admin 菜单最终态
-- 执行前置：系统管理/系统监控父菜单存在；目标固定 ID 只允许缺失、历史态或最终态
-- 适用范围：全新初始化、当前混合升级环境或已完成状态重放
-- 重复执行：是
-- 恢复方式：本次无迁移前备份；失败时保持 OpenAPI disabled，修正冲突后前向重放
-- ============================================================================

drop temporary table if exists namewta_admin_runtime_reconcile_dml_001_preflight;
create temporary table namewta_admin_runtime_reconcile_dml_001_preflight (
    preflight_ok tinyint not null,
    constraint chk_namewta_admin_runtime_reconcile_dml_001 check (preflight_ok = 1)
);

insert into namewta_admin_runtime_reconcile_dml_001_preflight (preflight_ok)
select if(
    exists (
        select 1 from sys_menu
        where menu_id = 1761400000000000001 and menu_type = 'M'
    )
    and exists (
        select 1 from sys_menu
        where menu_id = 1761400000000000002 and menu_type = 'M'
    )
    and (
        (
            (select count(*) from sys_menu
             where menu_id in (
                 1761400000000000003,
                 1761400000000000115, 1761400000000000116,
                 1761400000000001055, 1761400000000001056, 1761400000000001057,
                 1761400000000001058, 1761400000000001059, 1761400000000001060
             )) = 0
            and not exists (
                select 1 from sys_menu
                where parent_id in (1761400000000000003, 1761400000000000115, 1761400000000000116)
            )
        )
        or
        (
            (select count(*) from sys_menu
             where menu_id in (
                 1761400000000000003,
                 1761400000000000115, 1761400000000000116,
                 1761400000000001055, 1761400000000001056, 1761400000000001057,
                 1761400000000001058, 1761400000000001059, 1761400000000001060
             )) = 9
            and exists (
                select 1 from sys_menu
                where menu_id = 1761400000000000003
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '系统工具'
                  and parent_id <=> 0
                  and path <=> 'tool'
                  and coalesce(component, '') = ''
                  and menu_type <=> 'M'
                  and coalesce(perms, '') = ''
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 1761400000000000115
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '代码生成'
                  and parent_id <=> 1761400000000000003
                  and path <=> 'gen'
                  and menu_type <=> 'C'
                  and component <=> 'tool/gen/index'
                  and perms <=> 'tool:gen:list'
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 1761400000000000116
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '修改生成配置'
                  and parent_id <=> 1761400000000000003
                  and path <=> 'gen-edit/index/:tableId'
                  and menu_type <=> 'C'
                  and component <=> 'tool/gen/editTable'
                  and perms <=> 'tool:gen:edit'
                  and active_menu <=> '/tool/gen'
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 1761400000000001055
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '生成查询'
                  and parent_id <=> 1761400000000000115
                  and path <=> '#'
                  and component <=> ''
                  and menu_type <=> 'F'
                  and perms <=> 'tool:gen:query'
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 1761400000000001056
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '生成修改'
                  and parent_id <=> 1761400000000000115
                  and path <=> '#'
                  and component <=> ''
                  and menu_type <=> 'F'
                  and perms <=> 'tool:gen:edit'
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 1761400000000001057
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '生成删除'
                  and parent_id <=> 1761400000000000115
                  and path <=> '#'
                  and component <=> ''
                  and menu_type <=> 'F'
                  and perms <=> 'tool:gen:remove'
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 1761400000000001058
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '导入代码'
                  and parent_id <=> 1761400000000000115
                  and path <=> '#'
                  and component <=> ''
                  and menu_type <=> 'F'
                  and perms <=> 'tool:gen:import'
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 1761400000000001059
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '预览代码'
                  and parent_id <=> 1761400000000000115
                  and path <=> '#'
                  and component <=> ''
                  and menu_type <=> 'F'
                  and perms <=> 'tool:gen:preview'
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 1761400000000001060
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '生成代码'
                  and parent_id <=> 1761400000000000115
                  and path <=> '#'
                  and component <=> ''
                  and menu_type <=> 'F'
                  and perms <=> 'tool:gen:code'
            )
            and not exists (
                select 1 from sys_menu
                where parent_id = 1761400000000000003
                  and menu_id not in (1761400000000000115, 1761400000000000116)
            )
            and not exists (
                select 1 from sys_menu
                where parent_id = 1761400000000000115
                  and menu_id not in (
                      1761400000000001055, 1761400000000001056, 1761400000000001057,
                      1761400000000001058, 1761400000000001059, 1761400000000001060
                  )
            )
            and not exists (
                select 1 from sys_menu where parent_id = 1761400000000000116
            )
        )
    )
    and (
        (select count(*) from sys_menu
         where menu_id between 2094360621561675776 and 2094360621561675781) = 0
        or
        (
            (select count(*) from sys_menu
             where menu_id between 2094360621561675776 and 2094360621561675781) = 6
            and exists (
                select 1 from sys_menu
                where menu_id = 2094360621561675776
                  and client_id <=> 1762000000000000001
                  and menu_name in ('应用开放管理', 'OpenAPI管理')
                  and parent_id <=> 1761400000000000001
                  and order_num <=> 13
                  and path <=> 'openApi'
                  and component <=> 'system/openApi/index'
                  and query_param <=> ''
                  and is_frame <=> 'N'
                  and is_cache <=> 'Y'
                  and menu_type <=> 'C'
                  and visible <=> '0'
                  and status <=> '0'
                  and perms <=> 'system:openApi:list'
                  and icon <=> 'tabler:api'
                  and active_menu <=> ''
                  and ext <=> ''
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 2094360621561675777
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '开放应用查询'
                  and parent_id <=> 2094360621561675776
                  and order_num <=> 1
                  and path <=> ''
                  and component <=> ''
                  and query_param <=> ''
                  and is_frame <=> 'N'
                  and is_cache <=> 'Y'
                  and menu_type <=> 'F'
                  and visible <=> '0'
                  and status <=> '0'
                  and perms <=> 'system:openApi:query'
                  and icon <=> '#'
                  and active_menu <=> ''
                  and ext <=> ''
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 2094360621561675778
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '开放应用新增'
                  and parent_id <=> 2094360621561675776
                  and order_num <=> 2
                  and path <=> ''
                  and component <=> ''
                  and query_param <=> ''
                  and is_frame <=> 'N'
                  and is_cache <=> 'Y'
                  and menu_type <=> 'F'
                  and visible <=> '0'
                  and status <=> '0'
                  and perms <=> 'system:openApi:add'
                  and icon <=> '#'
                  and active_menu <=> ''
                  and ext <=> ''
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 2094360621561675779
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '开放应用修改'
                  and parent_id <=> 2094360621561675776
                  and order_num <=> 3
                  and path <=> ''
                  and component <=> ''
                  and query_param <=> ''
                  and is_frame <=> 'N'
                  and is_cache <=> 'Y'
                  and menu_type <=> 'F'
                  and visible <=> '0'
                  and status <=> '0'
                  and perms <=> 'system:openApi:edit'
                  and icon <=> '#'
                  and active_menu <=> ''
                  and ext <=> ''
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 2094360621561675780
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '开放应用删除'
                  and parent_id <=> 2094360621561675776
                  and order_num <=> 4
                  and path <=> ''
                  and component <=> ''
                  and query_param <=> ''
                  and is_frame <=> 'N'
                  and is_cache <=> 'Y'
                  and menu_type <=> 'F'
                  and visible <=> '0'
                  and status <=> '0'
                  and perms <=> 'system:openApi:remove'
                  and icon <=> '#'
                  and active_menu <=> ''
                  and ext <=> ''
            )
            and exists (
                select 1 from sys_menu
                where menu_id = 2094360621561675781
                  and client_id <=> 1762000000000000001
                  and menu_name <=> '个人开放应用'
                  and parent_id <=> 2094360621561675776
                  and order_num <=> 5
                  and path <=> ''
                  and component <=> ''
                  and query_param <=> ''
                  and is_frame <=> 'N'
                  and is_cache <=> 'Y'
                  and menu_type <=> 'F'
                  and visible <=> '0'
                  and status <=> '0'
                  and perms <=> 'system:openApi:self'
                  and icon <=> '#'
                  and active_menu <=> ''
                  and ext <=> ''
            )
        )
    )
    and not exists (
        select 1 from sys_menu
        where menu_id not between 2094360621561675776 and 2094360621561675781
          and (component = 'system/openApi/index'
            or perms in (
                'system:openApi:list', 'system:openApi:query', 'system:openApi:add',
                'system:openApi:edit', 'system:openApi:remove', 'system:openApi:self'
            ))
    )
    and not exists (
        select 1 from sys_menu
        where menu_id = 2094360621561675790
          and not (
              client_id <=> 1762000000000000001
              and menu_name in ('配置中心', 'Nacos配置中心')
              and (
                  (parent_id <=> 1761400000000000001 and order_num <=> 14 and menu_name <=> '配置中心')
                  or
                  (parent_id <=> 1761400000000000002 and order_num <=> 8 and menu_name <=> 'Nacos配置中心')
              )
              and path <=> 'nacos'
              and component <=> 'monitor/nacos/index'
              and query_param <=> ''
              and is_frame <=> 'N'
              and is_cache <=> 'Y'
              and menu_type <=> 'C'
              and visible <=> '0'
              and status <=> '0'
              and perms <=> 'system:nacos:console'
              and icon <=> 'tabler:server'
              and active_menu <=> ''
              and ext <=> ''
          )
    )
    and not exists (
        select 1 from sys_menu
        where menu_id <> 2094360621561675790
          and (component = 'monitor/nacos/index' or perms = 'system:nacos:console')
    ),
    1,
    0
);

drop temporary table namewta_admin_runtime_reconcile_dml_001_preflight;

start transaction;

delete from sys_role_menu
where menu_id in (
    1761400000000000003,
    1761400000000000115, 1761400000000000116,
    1761400000000001055, 1761400000000001056, 1761400000000001057,
    1761400000000001058, 1761400000000001059, 1761400000000001060
);

delete from sys_menu
where menu_id in (
    1761400000000001055, 1761400000000001056, 1761400000000001057,
    1761400000000001058, 1761400000000001059, 1761400000000001060
);
delete from sys_menu where menu_id in (1761400000000000115, 1761400000000000116);
delete from sys_menu where menu_id = 1761400000000000003;

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext,
                      create_dept, create_by, create_time, remark)
select 2094360621561675776, 1762000000000000001, 'OpenAPI管理', 1761400000000000001, 13,
       'openApi', 'system/openApi/index', '', 'N', 'Y', 'C', '0', '0',
       'system:openApi:list', 'tabler:api', '', '', 1761000000000000103, 1761100000000000001,
       sysdate(), 'OpenAPI凭据与接口目录管理菜单'
from dual
where not exists (select 1 from sys_menu where menu_id = 2094360621561675776);

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext,
                      create_dept, create_by, create_time, remark)
select 2094360621561675777, 1762000000000000001, '开放应用查询', 2094360621561675776, 1,
       '', '', '', 'N', 'Y', 'F', '0', '0', 'system:openApi:query', '#', '', '',
       1761000000000000103, 1761100000000000001, sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2094360621561675777);

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext,
                      create_dept, create_by, create_time, remark)
select 2094360621561675778, 1762000000000000001, '开放应用新增', 2094360621561675776, 2,
       '', '', '', 'N', 'Y', 'F', '0', '0', 'system:openApi:add', '#', '', '',
       1761000000000000103, 1761100000000000001, sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2094360621561675778);

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext,
                      create_dept, create_by, create_time, remark)
select 2094360621561675779, 1762000000000000001, '开放应用修改', 2094360621561675776, 3,
       '', '', '', 'N', 'Y', 'F', '0', '0', 'system:openApi:edit', '#', '', '',
       1761000000000000103, 1761100000000000001, sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2094360621561675779);

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext,
                      create_dept, create_by, create_time, remark)
select 2094360621561675780, 1762000000000000001, '开放应用删除', 2094360621561675776, 4,
       '', '', '', 'N', 'Y', 'F', '0', '0', 'system:openApi:remove', '#', '', '',
       1761000000000000103, 1761100000000000001, sysdate(), ''
from dual
where not exists (select 1 from sys_menu where menu_id = 2094360621561675780);

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext,
                      create_dept, create_by, create_time, remark)
select 2094360621561675781, 1762000000000000001, '个人开放应用', 2094360621561675776, 5,
       '', '', '', 'N', 'Y', 'F', '0', '0', 'system:openApi:self', '#', '', '',
       1761000000000000103, 1761100000000000001, sysdate(), '个人中心开放应用权限'
from dual
where not exists (select 1 from sys_menu where menu_id = 2094360621561675781);

update sys_menu
set menu_name = 'OpenAPI管理', parent_id = 1761400000000000001, order_num = 13
where menu_id = 2094360621561675776
  and not (menu_name <=> 'OpenAPI管理'
    and parent_id <=> 1761400000000000001
    and order_num <=> 13);

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext,
                      create_dept, create_by, create_time, remark)
select 2094360621561675790, 1762000000000000001, 'Nacos配置中心', 1761400000000000002, 8,
       'nacos', 'monitor/nacos/index', '', 'N', 'Y', 'C', '0', '0',
       'system:nacos:console', 'tabler:server', '', '', 1761000000000000103, 1761100000000000001,
       sysdate(), 'Nacos 官方控制台入口；配置权限由 Nacos 独立鉴权'
from dual
where not exists (select 1 from sys_menu where menu_id = 2094360621561675790);

update sys_menu
set menu_name = 'Nacos配置中心', parent_id = 1761400000000000002, order_num = 8
where menu_id = 2094360621561675790
  and not (menu_name <=> 'Nacos配置中心'
    and parent_id <=> 1761400000000000002
    and order_num <=> 8);

commit;

-- 本块不向普通角色授予 OpenAPI 或 Nacos 菜单；授权继续由管理员显式管理。
-- NAMEWTA-ADMIN-RUNTIME-RECONCILE-DML-001-END

-- NAMEWTA-PROFILE-DML-001
-- ============================================================================
-- 变更内容：新增区域证件目录、系统必传材料规则、档案配置及完整能力菜单
-- 变更标识：2026-09-01_11:50:00
-- 执行前置：已执行 NAMEWTA-PROFILE-DDL-001
-- 适用范围：全新环境；尚未应用本变更的升级环境
-- 重复执行：否；稳定主键与编码冲突时失败关闭
-- 回滚方式：仅上线前可按引用逆序回滚；上线后标签编码和证件编码只允许兼容追加
-- ============================================================================

insert into profile_document_type
    (document_type_id, document_type_code, issuing_region, document_type_name, number_pattern,
     validity_required, status, order_num, create_dept, create_by, create_time)
values
    (2100100000000000001, 'CN_RESIDENT_ID', 'CN', '中华人民共和国居民身份证', '^[0-9]{17}[0-9Xx]$', 'Y', '0', 1, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100100000000000002, 'HK_RESIDENT_ID', 'HK', '香港永久性居民身份证', '^[A-Za-z]{1,2}[0-9]{6}[0-9A-Za-z]$', 'Y', '0', 2, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100100000000000003, 'MO_RESIDENT_ID', 'MO', '澳门居民身份证', '^[0-9]{8}$', 'Y', '0', 3, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100100000000000004, 'TW_RESIDENT_ID', 'TW', '台湾地区身份证件', '^[A-Za-z][0-9]{9}$', 'Y', '0', 4, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100100000000000005, 'HK_MACAO_RESIDENCE_PERMIT', 'CN', '港澳居民居住证', '^8[123]0000[0-9]{11}[0-9Xx]$', 'Y', '0', 5, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100100000000000006, 'TW_RESIDENCE_PERMIT', 'CN', '台湾居民居住证', '^830000[0-9]{11}[0-9Xx]$', 'Y', '0', 6, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100100000000000007, 'MAINLAND_TRAVEL_PERMIT_HK_MACAO', 'CN', '港澳居民来往内地通行证', '^[HMhm][0-9]{8,10}$', 'Y', '0', 7, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100100000000000008, 'MAINLAND_TRAVEL_PERMIT_TW', 'CN', '台湾居民来往大陆通行证', '^[0-9]{8,10}$', 'Y', '0', 8, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100100000000000009, 'CN_PASSPORT', 'CN', '中华人民共和国护照或旅行证', '^[A-Za-z0-9]{5,17}$', 'Y', '0', 9, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100100000000000010, 'HK_PASSPORT', 'HK', '香港特别行政区护照或签证身份书', '^[A-Za-z0-9]{5,17}$', 'Y', '0', 10, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100100000000000011, 'MO_PASSPORT', 'MO', '澳门特别行政区护照或旅行证件', '^[A-Za-z0-9]{5,17}$', 'Y', '0', 11, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100100000000000012, 'TW_TRAVEL_DOCUMENT', 'TW', '台湾地区护照或旅行证件', '^[A-Za-z0-9]{5,17}$', 'Y', '0', 12, 1761000000000000103, 1761100000000000001, sysdate());

insert into profile_material_node
    (material_node_id, parent_id, node_type, node_depth, profile_type, material_tag_code, node_name,
     system_required, status, order_num, create_dept, create_by, create_time)
values
    (2100200000000000001, 0, 'CATEGORY', 1, 'PERSON', null, '个人材料', 'N', '0', 1, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000002, 0, 'CATEGORY', 1, 'ENTERPRISE', null, '企业材料', 'N', '0', 2, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000003, 0, 'CATEGORY', 1, 'COMMON', null, '通用材料', 'N', '0', 3, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000011, 2100200000000000001, 'CATEGORY', 2, 'PERSON', null, '个人身份证明', 'N', '0', 1, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000012, 2100200000000000002, 'CATEGORY', 2, 'ENTERPRISE', null, '企业主体证明', 'N', '0', 1, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000013, 2100200000000000002, 'CATEGORY', 2, 'ENTERPRISE', null, '企业办理证明', 'N', '0', 2, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000101, 2100200000000000011, 'TAG', 3, 'PERSON', 'PERSON_ID_CARD_PORTRAIT', '居民身份证人像面', 'Y', '0', 1, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000102, 2100200000000000011, 'TAG', 3, 'PERSON', 'PERSON_ID_CARD_EMBLEM', '居民身份证国徽面', 'Y', '0', 2, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000103, 2100200000000000011, 'TAG', 3, 'PERSON', 'PERSON_IDENTITY_FRONT', '身份证明正面', 'Y', '0', 3, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000104, 2100200000000000011, 'TAG', 3, 'PERSON', 'PERSON_IDENTITY_BACK', '身份证明背面', 'Y', '0', 4, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000105, 2100200000000000011, 'TAG', 3, 'PERSON', 'PERSON_PASSPORT_DATA_PAGE', '护照或旅行证件资料页', 'Y', '0', 5, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000201, 2100200000000000012, 'TAG', 3, 'ENTERPRISE', 'ENTERPRISE_BUSINESS_LICENSE', '营业执照', 'Y', '0', 1, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000202, 2100200000000000012, 'TAG', 3, 'ENTERPRISE', 'ENTERPRISE_LEGAL_REPRESENTATIVE_DOCUMENT', '法定代表人身份证明', 'Y', '0', 2, 1761000000000000103, 1761100000000000001, sysdate()),
    (2100200000000000203, 2100200000000000013, 'TAG', 3, 'ENTERPRISE', 'ENTERPRISE_AUTHORIZATION_LETTER', '企业授权委托书', 'Y', '0', 1, 1761000000000000103, 1761100000000000001, sysdate());

insert into profile_material_requirement
    (material_requirement_id, profile_type, document_type_code, handler_condition, material_tag_code,
     minimum_count, status, create_dept, create_by, create_time)
values
    (2100300000000000001, 'PERSON', 'CN_RESIDENT_ID', 'ALWAYS', 'PERSON_ID_CARD_PORTRAIT', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000002, 'PERSON', 'CN_RESIDENT_ID', 'ALWAYS', 'PERSON_ID_CARD_EMBLEM', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000003, 'PERSON', 'HK_RESIDENT_ID', 'ALWAYS', 'PERSON_IDENTITY_FRONT', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000004, 'PERSON', 'HK_RESIDENT_ID', 'ALWAYS', 'PERSON_IDENTITY_BACK', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000005, 'PERSON', 'MO_RESIDENT_ID', 'ALWAYS', 'PERSON_IDENTITY_FRONT', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000006, 'PERSON', 'MO_RESIDENT_ID', 'ALWAYS', 'PERSON_IDENTITY_BACK', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000007, 'PERSON', 'TW_RESIDENT_ID', 'ALWAYS', 'PERSON_IDENTITY_FRONT', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000008, 'PERSON', 'TW_RESIDENT_ID', 'ALWAYS', 'PERSON_IDENTITY_BACK', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000009, 'PERSON', 'HK_MACAO_RESIDENCE_PERMIT', 'ALWAYS', 'PERSON_IDENTITY_FRONT', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000010, 'PERSON', 'HK_MACAO_RESIDENCE_PERMIT', 'ALWAYS', 'PERSON_IDENTITY_BACK', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000011, 'PERSON', 'TW_RESIDENCE_PERMIT', 'ALWAYS', 'PERSON_IDENTITY_FRONT', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000012, 'PERSON', 'TW_RESIDENCE_PERMIT', 'ALWAYS', 'PERSON_IDENTITY_BACK', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000013, 'PERSON', 'MAINLAND_TRAVEL_PERMIT_HK_MACAO', 'ALWAYS', 'PERSON_IDENTITY_FRONT', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000014, 'PERSON', 'MAINLAND_TRAVEL_PERMIT_HK_MACAO', 'ALWAYS', 'PERSON_IDENTITY_BACK', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000015, 'PERSON', 'MAINLAND_TRAVEL_PERMIT_TW', 'ALWAYS', 'PERSON_IDENTITY_FRONT', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000016, 'PERSON', 'MAINLAND_TRAVEL_PERMIT_TW', 'ALWAYS', 'PERSON_IDENTITY_BACK', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000017, 'PERSON', 'CN_PASSPORT', 'ALWAYS', 'PERSON_PASSPORT_DATA_PAGE', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000018, 'PERSON', 'HK_PASSPORT', 'ALWAYS', 'PERSON_PASSPORT_DATA_PAGE', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000019, 'PERSON', 'MO_PASSPORT', 'ALWAYS', 'PERSON_PASSPORT_DATA_PAGE', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000020, 'PERSON', 'TW_TRAVEL_DOCUMENT', 'ALWAYS', 'PERSON_PASSPORT_DATA_PAGE', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000101, 'ENTERPRISE', '*', 'ALWAYS', 'ENTERPRISE_BUSINESS_LICENSE', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000102, 'ENTERPRISE', '*', 'ALWAYS', 'ENTERPRISE_LEGAL_REPRESENTATIVE_DOCUMENT', 1, '0', 1761000000000000103, 1761100000000000001, sysdate()),
    (2100300000000000103, 'ENTERPRISE', '*', 'HANDLER_NOT_LEGAL_REPRESENTATIVE', 'ENTERPRISE_AUTHORIZATION_LETTER', 1, '0', 1761000000000000103, 1761100000000000001, sysdate());

insert into sys_config
    (config_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, remark)
values
    (2100400000000000001, '个人实名认证流程编码', 'profile.person.flowCode', 'profile_person_verification', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), '个人与企业使用独立流程编码'),
    (2100400000000000002, '企业实名认证流程编码', 'profile.enterprise.flowCode', 'profile_enterprise_verification', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), '个人与企业使用独立流程编码'),
    (2100400000000000003, '个人默认实名认证供应商', 'profile.person.provider.default', 'manual', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), '首期人工复核'),
    (2100400000000000004, '企业默认实名认证供应商', 'profile.enterprise.provider.default', 'manual', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), '首期人工复核'),
    (2100400000000000005, '个人启用实名认证供应商', 'profile.person.provider.enabled', 'manual', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), '逗号分隔稳定providerCode'),
    (2100400000000000006, '企业启用实名认证供应商', 'profile.enterprise.provider.enabled', 'manual', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), '逗号分隔稳定providerCode');

-- 两套实名认证流程独立发布；审核任务由超级管理员角色承接，业务权限仍由 profile:*:review 控制。
insert into flow_definition
    (id, flow_code, flow_name, model_value, category, version, is_publish, form_custom, form_path,
     activity_status, create_time, create_by, update_time, update_by, del_flag)
values
    (2100600000000000001, 'profile_person_verification', '个人实名认证', 'CLASSICS',
     '1762300000000000102', '1', 1, 'Y', 'profile/person/review', 1, sysdate(),
     '1761100000000000001', sysdate(), '1761100000000000001', '0'),
    (2100600000000000002, 'profile_enterprise_verification', '企业实名认证', 'CLASSICS',
     '1762300000000000102', '1', 1, 'Y', 'profile/enterprise/review', 1, sysdate(),
     '1761100000000000001', sysdate(), '1761100000000000001', '0');

insert into flow_node
    (id, node_type, definition_id, node_code, node_name, permission_flag, node_ratio, coordinate,
     form_custom, form_path, version, create_time, create_by, update_time, update_by, del_flag)
values
    (2100610000000000001, 0, 2100600000000000001, 'person_apply', '提交申请', null,
     '0', '100,100|100,100', 'N', null, '1', sysdate(), '1761100000000000001', sysdate(), '1761100000000000001', '0'),
    (2100610000000000002, 1, 2100600000000000001, 'person_review', '人工复核',
     'role:1761300000000000001', '0', '300,100|300,100', 'Y', 'profile/person/review', '1', sysdate(),
     '1761100000000000001', sysdate(), '1761100000000000001', '0'),
    (2100610000000000003, 2, 2100600000000000001, 'person_finish', '复核完成', null,
     '0', '500,100|500,100', 'N', null, '1', sysdate(), '1761100000000000001', sysdate(), '1761100000000000001', '0'),
    (2100610000000000011, 0, 2100600000000000002, 'enterprise_apply', '提交申请', null,
     '0', '100,100|100,100', 'N', null, '1', sysdate(), '1761100000000000001', sysdate(), '1761100000000000001', '0'),
    (2100610000000000012, 1, 2100600000000000002, 'enterprise_review', '人工复核',
     'role:1761300000000000001', '0', '300,100|300,100', 'Y', 'profile/enterprise/review', '1', sysdate(),
     '1761100000000000001', sysdate(), '1761100000000000001', '0'),
    (2100610000000000013, 2, 2100600000000000002, 'enterprise_finish', '复核完成', null,
     '0', '500,100|500,100', 'N', null, '1', sysdate(), '1761100000000000001', sysdate(), '1761100000000000001', '0');

insert into flow_skip
    (id, definition_id, now_node_code, now_node_type, next_node_code, next_node_type,
     skip_name, skip_type, coordinate, create_time, create_by, update_time, update_by, del_flag)
values
    (2100620000000000001, 2100600000000000001, 'person_apply', 0, 'person_review', 1,
     '提交', 'PASS', '120,100;250,100', sysdate(), '1761100000000000001', sysdate(), '1761100000000000001', '0'),
    (2100620000000000002, 2100600000000000001, 'person_review', 1, 'person_finish', 2,
     '完成', 'PASS', '350,100;480,100', sysdate(), '1761100000000000001', sysdate(), '1761100000000000001', '0'),
    (2100620000000000011, 2100600000000000002, 'enterprise_apply', 0, 'enterprise_review', 1,
     '提交', 'PASS', '120,100;250,100', sysdate(), '1761100000000000001', sysdate(), '1761100000000000001', '0'),
    (2100620000000000012, 2100600000000000002, 'enterprise_review', 1, 'enterprise_finish', 2,
     '完成', 'PASS', '350,100;480,100', sysdate(), '1761100000000000001', sysdate(), '1761100000000000001', '0');

insert into sys_dict_type
    (dict_id, dict_name, dict_type, create_dept, create_by, create_time, remark)
values
    (2100450000000000001, '档案主体状态', 'profile_subject_status', 1761000000000000103, 1761100000000000001, sysdate(), '个人与企业主体共用'),
    (2100450000000000002, '档案申请状态', 'profile_application_status', 1761000000000000103, 1761100000000000001, sysdate(), '个人与企业申请共用'),
    (2100450000000000003, '档案绑定状态', 'profile_binding_status', 1761000000000000103, 1761100000000000001, sysdate(), '个人与企业绑定共用');

insert into sys_dict_data
    (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
     create_dept, create_by, create_time, remark)
values
    (2100460000000000001, 1, '有效', 'ACTIVE', 'profile_subject_status', '', 'success', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), ''),
    (2100460000000000002, 2, '已注销', 'REVOKED', 'profile_subject_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), ''),
    (2100460000000000011, 1, '草稿', 'DRAFT', 'profile_application_status', '', 'info', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), ''),
    (2100460000000000012, 2, '退回', 'BACK', 'profile_application_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), ''),
    (2100460000000000013, 3, '已撤销', 'CANCEL', 'profile_application_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), ''),
    (2100460000000000014, 4, '审核中', 'WAITING', 'profile_application_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), ''),
    (2100460000000000015, 5, '已完成', 'FINISH', 'profile_application_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), ''),
    (2100460000000000016, 6, '已作废', 'INVALID', 'profile_application_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), ''),
    (2100460000000000017, 7, '已终止', 'TERMINATION', 'profile_application_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), ''),
    (2100460000000000021, 1, '有效', 'ACTIVE', 'profile_binding_status', '', 'success', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), ''),
    (2100460000000000022, 2, '已停用', 'SUSPENDED', 'profile_binding_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), ''),
    (2100460000000000023, 3, '已解绑', 'UNBOUND', 'profile_binding_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '');

insert into sys_menu
    (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values
    (2100500000000000001, 1762000000000000001, '档案管理', 0, 6, 'profile', null, '', 'N', 'Y', 'M', '0', '0', '', 'tabler:id', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '个人、企业与材料标签管理目录'),
    (2100500000000000010, 1762000000000000001, '个人档案', 2100500000000000001, 1, 'person', 'profile/person/index', '', 'N', 'Y', 'C', '0', '0', 'profile:person:query', 'tabler:user', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '个人档案管理'),
    (2100500000000000011, 1762000000000000001, '个人认证申请', 2100500000000000010, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'profile:person:apply', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '完整个人申请能力'),
    (2100500000000000012, 1762000000000000001, '个人材料办理', 2100500000000000010, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'profile:person:material', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '完整个人材料能力'),
    (2100500000000000013, 1762000000000000001, '个人档案审核', 2100500000000000010, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'profile:person:review', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '审核包含所需快照和材料读取'),
    (2100500000000000014, 1762000000000000001, '个人档案处置', 2100500000000000010, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'profile:person:manage', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '完整个人档案管理能力'),
    (2100500000000000015, 1762000000000000001, '个人档案覆盖', 2100500000000000010, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'profile:person:override', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '直建与管理员覆盖能力'),
    (2100500000000000020, 1762000000000000001, '企业档案', 2100500000000000001, 2, 'enterprise', 'profile/enterprise/index', '', 'N', 'Y', 'C', '0', '0', 'profile:enterprise:query', 'tabler:building', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '企业档案管理'),
    (2100500000000000021, 1762000000000000001, '企业认证申请', 2100500000000000020, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'profile:enterprise:apply', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '完整企业申请能力'),
    (2100500000000000022, 1762000000000000001, '企业材料办理', 2100500000000000020, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'profile:enterprise:material', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '完整企业材料能力'),
    (2100500000000000023, 1762000000000000001, '企业档案审核', 2100500000000000020, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'profile:enterprise:review', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '审核包含所需快照和材料读取'),
    (2100500000000000024, 1762000000000000001, '企业档案处置', 2100500000000000020, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'profile:enterprise:manage', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '完整企业档案管理能力'),
    (2100500000000000025, 1762000000000000001, '企业档案覆盖', 2100500000000000020, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'profile:enterprise:override', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '直建与管理员覆盖能力'),
    (2100500000000000030, 1762000000000000001, '材料标签', 2100500000000000001, 3, 'material-tag', 'profile/materialTag/index', '', 'N', 'Y', 'C', '0', '0', 'profile:material-tag:query', 'tabler:hierarchy-2', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '档案材料标签树'),
    (2100500000000000031, 1762000000000000001, '材料标签管理', 2100500000000000030, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'profile:material-tag:manage', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '完整材料目录管理能力');

insert into sys_menu
    (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values
    (2100600000000000001, 1762000000000000001, '通知中心', 0, 7, 'notify', null, '', 'N', 'Y', 'M', '0', '0', '', 'tabler:messages', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '统一通知控制面'),
    (2100600000000000010, 1762000000000000001, '通知监控', 2100600000000000001, 1, 'monitor', 'notify/monitor/index', '', 'N', 'Y', 'C', '0', '0', 'notify:monitor:list', 'tabler:chart-bar', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '跨渠道通知投递日志'),
    (2100600000000000011, 1762000000000000001, '通知详情', 2100600000000000010, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:monitor:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '查询通知快照'),
    (2100600000000000020, 1762000000000000001, '通知提交', 2100600000000000001, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:notification:submit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '提交统一通知'),
    (2100600000000000021, 1762000000000000001, '通知重试', 2100600000000000001, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:notification:retry', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '重试失败投递'),
    (2100600000000000022, 1762000000000000001, '通知取消', 2100600000000000001, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:notification:cancel', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '取消待发送通知'),
    (2100600000000000030, 1762000000000000001, '通知管理', 2100600000000000001, 2, 'notice', 'notify/notice/index', '', 'N', 'Y', 'C', '0', '0', 'notify:notice:list', 'tabler:messages', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '公告草稿发布与撤回'),
    (2100600000000000040, 1762000000000000001, '通知收件箱', 2100600000000000001, 3, 'inbox', 'notify/inbox/index', '', 'N', 'Y', 'C', '0', '0', 'notify:inbox:list', 'tabler:messages', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '用户通知收件箱'),
    (2100600000000000050, 1762000000000000001, '通知配置', 2100600000000000001, 4, 'config', 'notify/config/index', '', 'N', 'Y', 'C', '0', '0', 'notify:config:list', 'tabler:settings', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '邮件短信渠道账号与场景绑定');

insert ignore into sys_role_menu (role_id, menu_id)
select 1761300000000000001, menu_id from sys_menu where menu_id in
    (2100600000000000001, 2100600000000000010, 2100600000000000011, 2100600000000000020,
     2100600000000000021, 2100600000000000022, 2100600000000000030, 2100600000000000040,
     2100600000000000050);

-- 通知动作与收件箱阅读权限必须与 Controller/前端权限合同同步。
insert ignore into sys_menu
    (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values
    (2100600000000000031, 1762000000000000001, '通知新增', 2100600000000000030, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:notice:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '保存通知草稿'),
    (2100600000000000032, 1762000000000000001, '通知修改', 2100600000000000030, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:notice:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '修改通知草稿'),
    (2100600000000000033, 1762000000000000001, '通知发布', 2100600000000000030, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:notice:publish', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '发布通知'),
    (2100600000000000034, 1762000000000000001, '通知撤回', 2100600000000000030, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:notice:retract', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '撤回通知'),
    (2100600000000000035, 1762000000000000001, '通知删除', 2100600000000000030, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:notice:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '删除通知草稿'),
    (2100600000000000041, 1762000000000000001, '通知标记已见', 2100600000000000040, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:inbox:seen', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '标记通知已见'),
    (2100600000000000042, 1762000000000000001, '通知标记已读', 2100600000000000040, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:inbox:read', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '标记通知已读');

insert ignore into sys_role_menu (role_id, menu_id)
select 1761300000000000001, menu_id from sys_menu where menu_id in
    (2100600000000000031, 2100600000000000032, 2100600000000000033, 2100600000000000034,
     2100600000000000035, 2100600000000000041, 2100600000000000042);

insert ignore into sys_menu
    (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values
    (2100600000000000023, 1762000000000000001, '通知查询', 2100600000000000001, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:notification:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '查询统一通知'),
    (2100600000000000036, 1762000000000000001, '公告查询', 2100600000000000030, 6, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:notice:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '查询通知草稿'),
    (2100600000000000051, 1762000000000000001, '配置查询', 2100600000000000050, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:config:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '查询渠道账号'),
    (2100600000000000052, 1762000000000000001, '配置新增', 2100600000000000050, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:config:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '新增渠道账号'),
    (2100600000000000053, 1762000000000000001, '配置修改', 2100600000000000050, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:config:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '修改渠道账号与场景绑定'),
    (2100600000000000054, 1762000000000000001, '配置删除', 2100600000000000050, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:config:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '删除渠道账号'),
    (2100600000000000055, 1762000000000000001, '配置试发', 2100600000000000050, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'notify:config:test', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '测试发送邮件或短信');

insert ignore into sys_role_menu (role_id, menu_id)
select 1761300000000000001, menu_id from sys_menu where menu_id in
    (2100600000000000023, 2100600000000000036, 2100600000000000051, 2100600000000000052,
     2100600000000000053, 2100600000000000054, 2100600000000000055);

insert into notify_scene_binding
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

-- 通知域唯一入口：清理 System/日志管理中的旧菜单与权限。
delete from sys_role_menu where menu_id in (
    select menu_id from sys_menu where perms like 'system:notice:%' or perms like 'system:notify:%'
        or component in ('system/notice/index', 'monitor/notify/index')
);
delete from sys_menu where perms like 'system:notice:%' or perms like 'system:notify:%'
    or component in ('system/notice/index', 'monitor/notify/index');
delete from sys_menu where menu_id in (1761400000000000125, 1761400000000001080, 1761400000000001081);

-- 初始化通知中心公告数据。
insert ignore into notify_notice
    (notice_id, notice_title, notice_type, notice_content, recipient_type, recipient_ids_json,
     user_type_ids_json, channels_json, status, lifecycle, published_at, remark,
     create_dept, create_by, create_time, update_by, update_time)
values
    (1761800000000000001, '欢迎使用通知中心', '2', '通知中心用于查看系统公告和个人通知，请根据业务需要维护公告内容与发送对象。', 'ALL', '[]', '[]', '["IN_APP"]', '0', 'PUBLISHED', sysdate(), '系统初始化公告', 1761000000000000103, 1761100000000000001, sysdate(), null, null),
    (1761800000000000002, '通知使用说明', '1', '管理员保存草稿后需单独发布。接收者可在通知收件箱查看已投递的站内消息。', 'ALL', '[]', '[]', '["IN_APP"]', '0', 'PUBLISHED', sysdate(), '系统初始化通知', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
insert ignore into notify_notice_snapshot
    (snapshot_id, notice_id, snapshot_version, title_snapshot, content_snapshot, notice_type, path_snapshot, published_at, create_by, create_time)
select notice_id, notice_id, 1, notice_title, notice_content, notice_type,
       concat('/notify/notice?noticeId=', notice_id), published_at, create_by, create_time
from notify_notice where lifecycle = 'PUBLISHED';

-- ============================================================================
-- 变更标识：NAMEWTA-THIRD-MENU-DML-001
-- 变更内容：三方接口管理菜单及按钮权限
-- 执行前置：已完整执行 50-namewta-ddl.sql
-- 适用范围：全新环境；已有环境按源/目标 Git Tag 生成并评审差异 SQL
-- 重复执行：否
-- ============================================================================

insert into sys_menu
    (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values
    (2100700000000000001, 1762000000000000001, '三方接口管理', 1761400000000000001, 14, 'third', null, '', 'N', 'Y', 'M', '0', '0', '', 'tabler:plug', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '第三方 HTTP 供应商与接口管理'),
    (2100700000000000011, 1762000000000000001, '供应商管理', 2100700000000000001, 1, 'provider', 'third/provider/index', '', 'N', 'Y', 'C', '0', '0', 'third:provider:list', 'tabler:cloud', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '供应商开关、Base URL 与限额'),
    (2100700000000000012, 1762000000000000001, '接口管理', 2100700000000000001, 2, 'endpoint', 'third/endpoint/index', '', 'N', 'Y', 'C', '0', '0', 'third:endpoint:list', 'tabler:link', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '接口路径、白名单与覆盖配置'),
    (2100700000000000014, 1762000000000000001, '调用明细', 2100700000000000001, 3, 'invocation', 'third/invocation/index', '', 'N', 'Y', 'C', '0', '0', 'third:invocation:list', 'tabler:list', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '出站 HTTP 脱敏调用明细'),
    (2100700000000000015, 1762000000000000001, '调用统计', 2100700000000000001, 4, 'statistics', 'third/statistics/index', '', 'N', 'Y', 'C', '0', '0', 'third:statistics:list', 'tabler:chart-bar', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '供应商与接口维度聚合统计');

insert into sys_menu
    (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values
    (2100700000000000021, 1762000000000000001, '供应商查询', 2100700000000000011, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'third:provider:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '供应商详情查询'),
    (2100700000000000022, 1762000000000000001, '供应商新增', 2100700000000000011, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'third:provider:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '供应商新增'),
    (2100700000000000023, 1762000000000000001, '供应商修改', 2100700000000000011, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'third:provider:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '供应商修改、启停'),
    (2100700000000000024, 1762000000000000001, '供应商删除', 2100700000000000011, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'third:provider:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '供应商受控删除'),
    (2100700000000000025, 1762000000000000001, '接口查询', 2100700000000000012, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'third:endpoint:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '接口详情查询'),
    (2100700000000000026, 1762000000000000001, '接口新增', 2100700000000000012, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'third:endpoint:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '接口新增'),
    (2100700000000000027, 1762000000000000001, '接口修改', 2100700000000000012, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'third:endpoint:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '接口修改、启停'),
    (2100700000000000028, 1762000000000000001, '接口删除', 2100700000000000012, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'third:endpoint:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '接口受控删除'),
    (2100700000000000029, 1762000000000000001, '凭据查询', 2100700000000000011, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'third:credential:list', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '凭据摘要查询'),
    (2100700000000000030, 1762000000000000001, '凭据新增', 2100700000000000011, 6, '', '', '', 'N', 'Y', 'F', '0', '0', 'third:credential:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '凭据新增或替换'),
    (2100700000000000031, 1762000000000000001, '凭据删除', 2100700000000000011, 7, '', '', '', 'N', 'Y', 'F', '0', '0', 'third:credential:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '凭据受控删除');

insert into sys_dict_type
    (dict_id, dict_name, dict_type, create_dept, create_by, create_time, remark)
values
    (2100450000000000011, '通知公告生命周期', 'notify_notice_lifecycle', 1761000000000000103, 1761100000000000001, sysdate(), '通知公告草稿、发布和撤回状态'),
    (2100450000000000012, '通知渠道', 'notify_channel', 1761000000000000103, 1761100000000000001, sysdate(), '统一通知投递渠道'),
    (2100450000000000013, '通知投递状态', 'notify_delivery_status', 1761000000000000103, 1761100000000000001, sysdate(), '统一通知投递状态'),
    (2100450000000000014, '通知消息分类', 'notify_message_category', 1761000000000000103, 1761100000000000001, sysdate(), '消息盒子和通知收件箱分类');

insert into sys_dict_data
    (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
     create_dept, create_by, create_time, remark)
values
    (2100460000000000101, 1, '草稿', 'DRAFT', 'notify_notice_lifecycle', '', 'info', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), '可编辑未发布公告'),
    (2100460000000000102, 2, '已发布', 'PUBLISHED', 'notify_notice_lifecycle', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '已发布公告'),
    (2100460000000000103, 3, '已撤回', 'RETRACTED', 'notify_notice_lifecycle', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '已撤回公告，可再次编辑发布'),
    (2100460000000000111, 1, '站内信', 'IN_APP', 'notify_channel', '', 'primary', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), '站内收件箱'),
    (2100460000000000112, 2, '短信', 'SMS', 'notify_channel', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '短信供应商'),
    (2100460000000000113, 3, '邮件', 'MAIL', 'notify_channel', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '邮件供应商'),
    (2100460000000000121, 1, '排队中', 'QUEUED', 'notify_delivery_status', '', 'info', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), '等待投递'),
    (2100460000000000122, 2, '处理中', 'PROCESSING', 'notify_delivery_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '正在投递'),
    (2100460000000000123, 3, '已接受', 'ACCEPTED', 'notify_delivery_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '供应商已接受'),
    (2100460000000000124, 4, '部分失败', 'PARTIAL_FAILURE', 'notify_delivery_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '部分渠道失败'),
    (2100460000000000125, 5, '已送达', 'DELIVERED', 'notify_delivery_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '投递完成'),
    (2100460000000000126, 6, '不可送达', 'UNDELIVERABLE', 'notify_delivery_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '目标不可达'),
    (2100460000000000127, 7, '未知', 'UNKNOWN', 'notify_delivery_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '状态未知'),
    (2100460000000000128, 8, '失败', 'FAILED', 'notify_delivery_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '投递失败'),
    (2100460000000000129, 9, '已取消', 'CANCELLED', 'notify_delivery_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '投递已取消'),
    (2100460000000000130, 10, '已过期', 'EXPIRED', 'notify_delivery_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '超过截止时间'),
    (2100460000000000134, 11, '投递异常', 'DISPATCH_ERROR', 'notify_delivery_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '投递过程发生异常'),
    (2100460000000000131, 1, '系统', 'system', 'notify_message_category', '', 'info', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), '系统消息'),
    (2100460000000000132, 2, '通知', 'notice', 'notify_message_category', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '通知公告消息'),
    (2100460000000000133, 3, '工作流', 'workflow', 'notify_message_category', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), '工作流消息');

-- ============================================================================
-- NAMEWTA-RICHTEXT-DML-001
-- 富文本通用组件接入示例（测试菜单）。
-- ============================================================================
insert ignore into sys_menu
    (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values
    (2100800000000000001, 1762000000000000001, '富文本演示', 1761400000000000005, 3, 'rich-text', 'demo/rich-text/index', '', 'N', 'Y', 'C', '0', '0', 'demo:richtext:list', 'tabler:edit', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '富文本编辑、预览与 OSS 接入示例');

insert ignore into sys_menu
    (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values
    (2100800000000000011, 1762000000000000001, '富文本查询', 2100800000000000001, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'demo:richtext:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '富文本详情与资源访问'),
    (2100800000000000012, 1762000000000000001, '富文本新增', 2100800000000000001, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'demo:richtext:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '新建富文本文档'),
    (2100800000000000013, 1762000000000000001, '富文本修改', 2100800000000000001, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'demo:richtext:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '修改富文本文档'),
    (2100800000000000014, 1762000000000000001, '富文本删除', 2100800000000000001, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'demo:richtext:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '删除富文本文档'),
    (2100800000000000015, 1762000000000000001, '富文本上传', 2100800000000000001, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'common:richtext:upload', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '富文本图片、音频和附件上传');

insert ignore into sys_role_menu (role_id, menu_id)
select 1761300000000000001, menu_id from sys_menu where menu_id between 2100800000000000001 and 2100800000000000015;
