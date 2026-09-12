-- Rollback for 65-wta-user-migrate.sql: restore login wta → ruoyi
-- only when ruoyi is free. Does not guess among multiple wta users.

SET NAMES utf8mb4;

UPDATE sys_user AS owned
  LEFT JOIN sys_user AS taken ON taken.user_name = 'ruoyi'
   SET owned.user_name = 'ruoyi',
       owned.update_time = sysdate()
 WHERE owned.user_name = 'wta'
   AND taken.user_id IS NULL
   AND owned.user_id = (
     SELECT user_id FROM (
       SELECT user_id FROM sys_user WHERE user_name = 'wta' ORDER BY user_id LIMIT 1
     ) AS one
   );
