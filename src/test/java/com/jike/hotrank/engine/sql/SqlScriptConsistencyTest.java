package com.jike.hotrank.engine.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlScriptConsistencyTest {

    @Test
    void schemaShouldContainIndexesRequiredByCurrentQueryPaths() throws IOException {
        String schema = read("src/main/resources/sql/schema.sql");

        assertTrue(schema.contains("idx_status_score (status, current_score DESC)"));
        assertTrue(schema.contains("idx_circle_status_score (circle_id, status, current_score DESC)"));
        assertTrue(schema.contains("idx_newcomer_rank (status, publish_time, current_score DESC)"));
        assertTrue(schema.contains("idx_status_created (status, created_at DESC)"));
        assertTrue(schema.contains("idx_user_topic_created (user_id, topic_id, created_at)"));
        assertTrue(schema.contains("idx_created_topic_type (created_at, topic_id, interaction_type)"));
        assertTrue(schema.contains("idx_created_type (created_at, interaction_type)"));
        assertTrue(schema.contains("idx_device_created_user (device_fingerprint, created_at, user_id)"));
        assertTrue(schema.contains("idx_user_device_created (user_id, device_fingerprint, created_at)"));
        assertTrue(schema.contains("idx_user_topic_valid_created (user_id, topic_id, is_valid, created_at)"));
        assertTrue(schema.contains("idx_behavior_device_created_user (device_fingerprint, created_at, user_id)"));
        assertTrue(schema.contains("idx_valid_created_topic (is_valid, created_at DESC, topic_id)"));
    }

    @Test
    void migrationsShouldContainIndexesForExistingDatabases() throws IOException {
        String existingDatabaseMigration = read("src/main/resources/sql/20260706_upgrade_existing_database.sql");
        String redundantIndexCleanup = read("src/main/resources/sql/20260706_drop_legacy_redundant_indexes.sql");

        assertTrue(existingDatabaseMigration.contains("CREATE TABLE IF NOT EXISTS user_circle_preference"));
        assertTrue(existingDatabaseMigration.contains("weight_multiplier"));
        assertTrue(existingDatabaseMigration.contains("add_index_if_missing"));
        assertTrue(existingDatabaseMigration.contains("idx_status_score"));
        assertTrue(existingDatabaseMigration.contains("idx_user_device_created"));
        assertTrue(existingDatabaseMigration.contains("idx_status_created"));
        assertTrue(existingDatabaseMigration.contains("idx_created_type"));
        assertTrue(existingDatabaseMigration.contains("idx_valid_created_topic"));
        assertTrue(existingDatabaseMigration.contains("idx_device (device_fingerprint)"));
        assertTrue(redundantIndexCleanup.contains("drop_index_if_exists"));
        assertTrue(redundantIndexCleanup.contains("idx_circle_score"));
        assertTrue(redundantIndexCleanup.contains("idx_user_topic"));
    }

    @Test
    void entrypointScriptsShouldReferenceExistingSqlFiles() throws IOException {
        assertSourceTargetsExist("src/main/resources/sql/00_setup_fresh_database.sql");
        assertSourceTargetsExist("src/main/resources/sql/01_upgrade_existing_database.sql");
    }

    @Test
    void publicSqlAndDocsShouldNotContainCommonMojibakeMarkers() throws IOException {
        List<String> files = List.of(
            "README.md",
            "docs/系统架构说明.md",
            "docs/数据库设计说明.md",
            "docs/第七天答辩手册.md",
            "docs/最终交付报告.md",
            "docs/演示手册.md",
            "docs/最终检查清单.md",
            "docs/性能与数据分析说明.md",
            "docs/答辩演示大纲.md",
            "docs/数据库执行计划检查清单.md",
            "docs/热度算法对比报告.md",
            "docs/窗口函数实践说明.md",
            "docs/互动模拟与榜单变化记录.md",
            "src/main/resources/sql/数据库脚本执行顺序说明.md",
            "src/main/resources/sql/00_setup_fresh_database.sql",
            "src/main/resources/sql/01_upgrade_existing_database.sql",
            "src/main/resources/sql/data.sql",
            "src/main/resources/sql/repair.sql",
            "src/main/resources/sql/20260706_upgrade_existing_database.sql",
            "src/main/resources/sql/20260706_drop_legacy_redundant_indexes.sql"
        );

        for (String file : files) {
            String content = read(file);
            for (String marker : mojibakeMarkers()) {
                assertFalse(content.contains(marker), file);
            }
        }
    }

    private List<String> mojibakeMarkers() {
        return List.of("鍠", "倮", "鈸", "\uFFFD");
    }

    private void assertSourceTargetsExist(String path) throws IOException {
        String content = read(path);
        Pattern sourcePattern = Pattern.compile("(?m)^--\\s*SOURCE\\s+([^;]+);");
        Matcher matcher = sourcePattern.matcher(content);
        boolean foundSource = false;
        while (matcher.find()) {
            foundSource = true;
            Path target = Path.of(matcher.group(1).trim());
            assertTrue(java.nio.file.Files.exists(target), path + " references missing file: " + target);
        }
        assertTrue(foundSource, path + " should reference at least one SQL file");
    }

    private String read(String path) throws IOException {
        return java.nio.file.Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
