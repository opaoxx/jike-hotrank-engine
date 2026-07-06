-- Index migration for Day5 analysis and anti-spam report query paths.
-- Run after 20260706_add_rank_query_indexes.sql on existing databases.

USE jike_hotrank;

ALTER TABLE topic
    ADD INDEX idx_status_created (status, created_at DESC);

ALTER TABLE interaction_event
    ADD INDEX idx_created_type (created_at, interaction_type);

ALTER TABLE user_behavior
    ADD INDEX idx_user_topic_valid_created (user_id, topic_id, is_valid, created_at),
    ADD INDEX idx_behavior_device_created_user (device_fingerprint, created_at, user_id),
    ADD INDEX idx_valid_created_topic (is_valid, created_at DESC, topic_id);
