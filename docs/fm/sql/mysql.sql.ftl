-- 合并目标：release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql
-- 这是菜单 DML 片段，不是独立部署脚本；clientPk 是 sys_client.id，不是 OAuth clientId。
insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values(${table.menuIds[0]}, ${clientPk}, '${functionName}', ${parentMenuId}, 1, '${businessName}', '${moduleName}/${businessName}/index', '', 'N', 'Y', 'C', '0', '0', '${permissionPrefix}:list', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '${functionName}菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values(${table.menuIds[1]}, ${clientPk}, '${functionName}查询', ${table.menuIds[0]}, 1,  '', '', '', 'N', 'Y', 'F', '0', '0', '${permissionPrefix}:query',        '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '');

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values(${table.menuIds[2]}, ${clientPk}, '${functionName}新增', ${table.menuIds[0]}, 2,  '', '', '', 'N', 'Y', 'F', '0', '0', '${permissionPrefix}:add',          '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '');

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values(${table.menuIds[3]}, ${clientPk}, '${functionName}修改', ${table.menuIds[0]}, 3,  '', '', '', 'N', 'Y', 'F', '0', '0', '${permissionPrefix}:edit',         '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '');

insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values(${table.menuIds[4]}, ${clientPk}, '${functionName}删除', ${table.menuIds[0]}, 4,  '', '', '', 'N', 'Y', 'F', '0', '0', '${permissionPrefix}:remove',       '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '');
<#if enableExport>
insert into sys_menu (menu_id, client_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, remark)
values(${table.menuIds[5]}, ${clientPk}, '${functionName}导出', ${table.menuIds[0]}, 5,  '', '', '', 'N', 'Y', 'F', '0', '0', '${permissionPrefix}:export',       '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), '');
</#if>
