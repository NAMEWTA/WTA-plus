select
    ((select count(*)
      from sys_menu
      where menu_id between 2094360621561675776 and 2094360621561675781) = 6)
        as openapi_menu_ok,
    exists(
        select 1
        from sys_menu
        where menu_id = 2094360621561675790
          and menu_name = 'Nacos配置中心'
          and parent_id = 1761400000000000002
          and component = 'monitor/nacos/index'
          and perms = 'system:nacos:console'
    ) as nacos_menu_ok,
    not exists(
        select 1
        from sys_menu
        where menu_id in (
            1761400000000000003,
            1761400000000000115, 1761400000000000116,
            1761400000000001055, 1761400000000001056, 1761400000000001057,
            1761400000000001058, 1761400000000001059, 1761400000000001060
        )
    ) as gen_menu_removed_ok,
    not exists(
        select 1
        from information_schema.tables
        where table_schema = database()
          and table_name in ('gen_table', 'gen_table_column')
    ) as gen_tables_removed_ok;
