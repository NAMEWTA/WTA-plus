-- Existing-DB: default superadmin user_name/nick_name admin → WTA.
-- Fresh installs already seed WTA via 10-wta-base.sql. Password hashes unchanged.

SET NAMES utf8mb4;

UPDATE sys_user AS owned
  LEFT JOIN sys_user AS taken ON taken.user_name = 'WTA' AND taken.user_id <> owned.user_id
   SET owned.user_name = 'WTA',
       owned.nick_name = 'WTA',
       owned.update_time = sysdate()
 WHERE owned.user_id = 1761100000000000001
   AND owned.user_name = 'admin'
   AND taken.user_id IS NULL;
