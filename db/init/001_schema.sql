CREATE DATABASE IF NOT EXISTS clip_hub_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clip_hub_db;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(128) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    team_id BIGINT NULL,
    display_name VARCHAR(100) NOT NULL,
    bio VARCHAR(500) DEFAULT '',
    avatar_url VARCHAR(255) DEFAULT '',
    status TINYINT NOT NULL DEFAULT 1,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL UNIQUE,
    expire_at DATETIME NOT NULL,
    used TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    INDEX idx_password_reset_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    parent_id BIGINT NULL,
    description VARCHAR(255) DEFAULT '',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(60) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS materials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    description TEXT NULL,
    type VARCHAR(30) NOT NULL,
    category_id BIGINT NULL,
    owner_id BIGINT NOT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    preview_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(120) DEFAULT '',
    format VARCHAR(30) DEFAULT '',
    size_bytes BIGINT NOT NULL DEFAULT 0,
    duration_seconds INT NULL,
    resolution VARCHAR(30) DEFAULT '',
    download_count BIGINT NOT NULL DEFAULT 0,
    favorite_count BIGINT NOT NULL DEFAULT 0,
    share_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_material_owner_id (owner_id),
    INDEX idx_material_category_id (category_id),
    INDEX idx_material_type (type),
    INDEX idx_material_visibility (visibility),
    INDEX idx_material_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS material_tag_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE KEY uk_material_tag (material_id, tag_id),
    INDEX idx_material_tag_material_id (material_id),
    INDEX idx_material_tag_tag_id (tag_id)
);

CREATE TABLE IF NOT EXISTS favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_favorites_user_material (user_id, material_id),
    INDEX idx_favorites_material_id (material_id)
);

CREATE TABLE IF NOT EXISTS share_links (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    shared_by BIGINT NOT NULL,
    share_code VARCHAR(64) NOT NULL UNIQUE,
    expire_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_share_material_id (material_id)
);

CREATE TABLE IF NOT EXISTS projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) DEFAULT '',
    owner_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    team_id BIGINT NULL,
    export_format VARCHAR(20) NOT NULL DEFAULT 'mp4',
    current_version_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_project_owner_id (owner_id),
    INDEX idx_project_status (status)
);

CREATE TABLE IF NOT EXISTS project_material_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_project_material (project_id, material_id),
    INDEX idx_project_material_project_id (project_id),
    INDEX idx_project_material_material_id (material_id)
);

CREATE TABLE IF NOT EXISTS project_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    version_name VARCHAR(100) NOT NULL,
    content_json LONGTEXT NOT NULL,
    created_by BIGINT NOT NULL,
    is_current TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    INDEX idx_project_versions_project_id (project_id),
    INDEX idx_project_versions_no (version_no)
);

CREATE TABLE IF NOT EXISTS project_collaborators (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_project_collaborator (project_id, user_id),
    INDEX idx_project_collaborator_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS system_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255) DEFAULT '',
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    level VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    publish_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_notifications_status_publish (status, publish_at)
);

CREATE TABLE IF NOT EXISTS collections (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) DEFAULT '',
    cover_material_id BIGINT NULL,
    owner_id BIGINT NOT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    sort_order INT NOT NULL DEFAULT 0,
    is_default TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_collection_owner_id (owner_id),
    INDEX idx_collection_visibility (visibility)
);

CREATE TABLE IF NOT EXISTS collection_material_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    collection_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    note VARCHAR(500) DEFAULT '',
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_collection_material (collection_id, material_id),
    INDEX idx_collection_material_collection_id (collection_id),
    INDEX idx_collection_material_material_id (material_id)
);

CREATE TABLE IF NOT EXISTS collection_shares (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    collection_id BIGINT NOT NULL,
    shared_by BIGINT NOT NULL,
    share_code VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(64) DEFAULT '',
    expire_at DATETIME NULL,
    access_count BIGINT NOT NULL DEFAULT 0,
    last_access_at DATETIME NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    INDEX idx_collection_share_collection_id (collection_id),
    INDEX idx_collection_share_code (share_code)
);

CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    username VARCHAR(64) NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(80) NOT NULL,
    detail VARCHAR(500) DEFAULT '',
    created_at DATETIME NOT NULL,
    INDEX idx_operation_logs_user_id (user_id),
    INDEX idx_operation_logs_created_at (created_at)
);
