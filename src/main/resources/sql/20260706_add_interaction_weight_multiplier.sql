USE jike_hotrank;

ALTER TABLE interaction_event
    ADD COLUMN weight_multiplier DECIMAL(6, 3) NOT NULL DEFAULT 1.000 COMMENT 'anti-spam heat multiplier'
    AFTER ip_address;
