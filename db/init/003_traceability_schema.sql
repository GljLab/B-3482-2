USE clip_hub_db;

CREATE TABLE IF NOT EXISTS material_usage_trails (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    username VARCHAR(64) NULL,
    action VARCHAR(80) NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    target_type VARCHAR(40) NULL,
    target_id VARCHAR(80) NULL,
    target_name VARCHAR(255) NULL,
    detail TEXT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_trail_material_id (material_id),
    INDEX idx_trail_user_id (user_id),
    INDEX idx_trail_created_at (created_at),
    INDEX idx_trail_action_type (action_type)
);

ALTER TABLE materials
    ADD COLUMN project_usage_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN collection_usage_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN source_type VARCHAR(40) NOT NULL DEFAULT 'USER_UPLOAD',
    ADD COLUMN source_note VARCHAR(500) DEFAULT '',
    ADD COLUMN ownership_type VARCHAR(40) NOT NULL DEFAULT 'PERSONAL',
    ADD COLUMN department_id BIGINT NULL,
    ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0,
    ADD COLUMN deleted_by BIGINT NULL,
    ADD COLUMN deleted_at DATETIME NULL,
    ADD COLUMN recycle_expire_at DATETIME NULL,
    ADD COLUMN original_title VARCHAR(255) NULL,
    ADD COLUMN original_preview_path VARCHAR(500) NULL;

ALTER TABLE project_material_rel
    ADD COLUMN added_by BIGINT NULL,
    ADD COLUMN added_at DATETIME NULL,
    ADD COLUMN source_type VARCHAR(40) NOT NULL DEFAULT 'DIRECT_BIND',
    ADD COLUMN source_note VARCHAR(500) DEFAULT '',
    ADD COLUMN material_owner_id BIGINT NULL,
    ADD COLUMN material_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE collection_material_rel
    ADD COLUMN added_by BIGINT NULL,
    ADD COLUMN added_at DATETIME NULL;

CREATE TABLE IF NOT EXISTS project_version_material_snapshots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    material_title VARCHAR(255) NOT NULL,
    material_owner_id BIGINT NULL,
    material_visibility VARCHAR(20) NULL,
    material_snapshot JSON NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_snapshot_version_id (version_id),
    INDEX idx_snapshot_project_id (project_id),
    INDEX idx_snapshot_material_id (material_id)
);

CREATE TABLE IF NOT EXISTS material_modification_histories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    username VARCHAR(64) NULL,
    field_name VARCHAR(64) NOT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_mod_history_material_id (material_id),
    INDEX idx_mod_history_user_id (user_id),
    INDEX idx_mod_history_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS material_transfer_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    transfer_note VARCHAR(500) DEFAULT '',
    operator_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_transfer_material_id (material_id),
    INDEX idx_transfer_from_user (from_user_id),
    INDEX idx_transfer_to_user (to_user_id)
);

CREATE TABLE IF NOT EXISTS admin_cleanup_batches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(64) NULL,
    total_count INT NOT NULL DEFAULT 0,
    cleaned_count INT NOT NULL DEFAULT 0,
    total_bytes BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    material_ids JSON NULL,
    created_at DATETIME NOT NULL,
    completed_at DATETIME NULL
);
