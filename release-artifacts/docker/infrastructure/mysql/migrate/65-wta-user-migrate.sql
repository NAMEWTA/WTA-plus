-- Existing-DB username migrate: default/demo login ruoyi → wta (ADR-013).
-- Fresh installs already seed admin/test, not ruoyi. Run this only on databases
-- that still have user_name = 'ruoyi'. Stop if user_name = 'wta' already exists.
-- Password hashes are unchanged.

SET NAMES utf8mb4;

SELECT COUNT(*) INTO @wta_count FROM sys_user WHERE user_name = 'wta';
SELECT COUNT(*) INTO @ruoyi_count FROM sys_user WHERE user_name = 'ruoyi';

-- Collision: both names present. Do not overwrite.
-- MySQL client should inspect @wta_count/@ruoyi_count before continuing.

UPDATE sys_user AS owned
  LEFT JOIN sys_user AS taken ON taken.user_name = 'wta'
   SET owned.user_name = 'wta',
       owned.update_time = sysdate()
 WHERE owned.user_name = 'ruoyi'
   AND taken.user_id IS NULL;
