-- Index migration for ranking queries, aggregation windows, and anti-spam checks.
-- Run after 20260706_add_interaction_weight_multiplier.sql on existing databases.

USE jike_hotrank;

ALTER TABLE topic
    ADD INDEX idx_status_score (status, current_score DESC),
    ADD INDEX idx_circle_status_score (circle_id, status, current_score DESC),
    ADD INDEX idx_newcomer_rank (status, publish_time, current_score DESC);

ALTER TABLE interaction_event
    ADD INDEX idx_user_topic_created (user_id, topic_id, created_at),
    ADD INDEX idx_created_topic_type (created_at, topic_id, interaction_type),
    ADD INDEX idx_device_created_user (device_fingerprint, created_at, user_id),
    ADD INDEX idx_user_device_created (user_id, device_fingerprint, created_at);
