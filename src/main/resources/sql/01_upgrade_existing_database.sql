-- ================================================================
-- 即刻 App 热点榜单引擎 - 旧库一键升级入口
-- 用途：从项目根目录执行，保留已有数据，补齐结构、修复统计并清理冗余索引。
--
-- CLI 执行（推荐）：
--   mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/sql/20260706_upgrade_existing_database.sql
--   mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/sql/repair.sql
--   mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/sql/20260706_drop_legacy_redundant_indexes.sql
--
-- 或 IDEA Database Console 中按顺序打开并执行下方 SOURCE 标记的文件：
-- SOURCE src/main/resources/sql/20260706_upgrade_existing_database.sql
-- SOURCE src/main/resources/sql/repair.sql
-- SOURCE src/main/resources/sql/20260706_drop_legacy_redundant_indexes.sql
-- ================================================================

SELECT 'existing database upgrade finished: run upgrade then repair then cleanup scripts manually, or use the CLI commands above' AS message;
