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
        assertTrue(schema.contains("idx_user_topic_created (user_id, topic_id, created_at)"));
        assertTrue(schema.contains("idx_created_topic_type (created_at, topic_id, interaction_type)"));
        assertTrue(schema.contains("idx_device_created_user (device_fingerprint, created_at, user_id)"));
        assertTrue(schema.contains("idx_user_device_created (user_id, device_fingerprint, created_at)"));
    }

    @Test
    void publicSqlAndDocsShouldNotContainCommonMojibakeMarkers() throws IOException {
        List<String> files = List.of(
            "README.md",
            "docs/architecture.md",
            "docs/database-design.md",
            "src/main/resources/sql/data.sql",
            "src/main/resources/sql/repair.sql"
        );

        for (String file : files) {
            String content = read(file);
            assertFalse(content.contains("鍗"), file);
            assertFalse(content.contains("馃"), file);
            assertFalse(content.contains("鈹"), file);
            assertFalse(content.contains("�"), file);
        }
    }

    private String read(String path) throws IOException {
        return java.nio.file.Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
