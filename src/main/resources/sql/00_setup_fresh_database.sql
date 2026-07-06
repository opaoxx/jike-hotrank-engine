-- ================================================================
-- 即刻 App 热点榜单引擎 - 新库一键初始化入口
-- 用途：从项目根目录执行，重建表结构并导入演示数据。
-- 注意：schema.sql 会 DROP 并重建业务表，只适合空库或演示库重置。
--
-- CLI 执行（推荐）：
--   mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/sql/schema.sql
--   mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/sql/data.sql
--
-- 或 IDEA Database Console 中按顺序打开并执行下方 SOURCE 标记的文件：
-- SOURCE src/main/resources/sql/schema.sql;
-- SOURCE src/main/resources/sql/data.sql;
-- ================================================================

SELECT 'fresh database setup finished: run schema.sql then data.sql manually, or use the CLI commands above' AS message;
