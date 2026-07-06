package com.jike.hotrank.engine.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

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
        String rankingMigration = read("src/main/resources/sql/20260706_add_rank_query_indexes.sql");
        String analysisMigration = read("src/main/resources/sql/20260706_add_analysis_query_indexes.sql");

        assertTrue(rankingMigration.contains("idx_status_score"));
        assertTrue(rankingMigration.contains("idx_user_device_created"));
        assertTrue(analysisMigration.contains("idx_status_created"));
        assertTrue(analysisMigration.contains("idx_created_type"));
        assertTrue(analysisMigration.contains("idx_valid_created_topic"));
    }

    @Test
    void publicSqlAndDocsShouldNotContainCommonMojibakeMarkers() throws IOException {
        List<String> files = List.of(
            "README.md",
            "docs/architecture.md",
            "docs/database-design.md",
            "docs/demo-guide.md",
            "docs/final-review-checklist.md",
            "docs/performance-analysis.md",
            "docs/presentation-outline.md",
            "docs/sql-explain-checklist.md",
            "src/main/resources/sql/data.sql",
            "src/main/resources/sql/repair.sql"
        );

        for (String file : files) {
            String content = read(file);
            for (String marker : mojibakeMarkers()) {
                assertFalse(content.contains(marker), file);
            }
        }
    }

    private List<String> mojibakeMarkers() {
        return List.of("\u9357", "\u9983", "\u9239", "\uFFFD");
    }

    private String read(String path) throws IOException {
        return java.nio.file.Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
