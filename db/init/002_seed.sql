USE clip_hub_db;
SET NAMES utf8mb4;

INSERT IGNORE INTO users (id, username, email, password, role, team_id, display_name, bio, avatar_url, status, created_at, updated_at)
VALUES
    (1, 'admin_3482', 'admin3482@cliphub.local', '$2y$10$7LdSpmmRqEz7pEmNHqV6ne.JPCoPeRu4ZR.Pe.xQNHo5DXzaLOHOq', 'ADMIN', 1001, '系统管理员', '负责系统治理与权限策略', '', 1, NOW(), NOW()),
    (2, 'vip_3482', 'vip3482@cliphub.local', '$2y$10$9eTIW6muei4alZ1lSXvDEeonO/ZcUl7yBgcORx1yfcBgN3.1EpWU2', 'VIP', 1001, '资深剪辑师', '负责重点素材编排', '', 1, NOW(), NOW()),
    (3, 'user_3482', 'user3482@cliphub.local', '$2y$10$UV9oj5UZYbIYU0GzSDO7Oe/eU.7r/dZPbA7KZsTP7/bXZN2swaL7.', 'USER', 1002, '普通用户', '日常上传与检索素材', '', 1, NOW(), NOW());

INSERT IGNORE INTO categories (id, name, parent_id, description, created_at, updated_at)
VALUES
    (1, '品牌宣传', NULL, '企业品牌与活动宣传内容', NOW(), NOW()),
    (2, '产品展示', NULL, '产品功能展示与开箱素材', NOW(), NOW()),
    (3, '音频资源', NULL, '背景音乐、音效与语音包', NOW(), NOW()),
    (4, '模板工程', NULL, '剪辑模板与转场预设', NOW(), NOW());

INSERT IGNORE INTO tags (id, name, created_at, updated_at)
VALUES
    (1, '4k', NOW(), NOW()),
    (2, '商用', NOW(), NOW()),
    (3, '转场', NOW(), NOW()),
    (4, '人像', NOW(), NOW()),
    (5, '快剪', NOW(), NOW());

INSERT IGNORE INTO materials (
    id, title, description, type, category_id, owner_id, visibility,
    file_name, storage_path, preview_path, mime_type, format, size_bytes,
    duration_seconds, resolution, download_count, favorite_count, share_count,
    created_at, updated_at
)
VALUES
    (1, '城市夜景延时素材', '适用于品牌片头场景', 'VIDEO', 1, 2, 'PUBLIC',
     'city-night-time-lapse.mp4', '/data/uploads/seed/city-night-time-lapse.mp4', '/data/uploads/seed/city-night-time-lapse.mp4',
     'video/mp4', 'mp4', 20971520, 30, '3840x2160', 35, 12, 3, NOW(), NOW()),
    (2, '轻快节奏背景音乐', '适用于短视频快剪节奏', 'AUDIO', 3, 2, 'TEAM',
     'upbeat-background-track.mp3', '/data/uploads/seed/upbeat-background-track.mp3', '/data/uploads/seed/upbeat-background-track.mp3',
     'audio/mpeg', 'mp3', 6291456, 120, '', 18, 6, 2, NOW(), NOW()),
    (3, '横版产品开箱模板', '用于新品发布剪辑模板', 'TEMPLATE', 4, 1, 'PRIVATE',
     'product-unbox-template.json', '/data/uploads/seed/product-unbox-template.json', '/data/uploads/seed/product-unbox-template.json',
     'application/json', 'json', 20480, NULL, '1920x1080', 5, 2, 1, NOW(), NOW());

INSERT IGNORE INTO material_tag_rel (id, material_id, tag_id) VALUES
    (1, 1, 1), (2, 1, 2), (3, 2, 5), (4, 3, 3), (5, 3, 2);

INSERT IGNORE INTO favorites (id, user_id, material_id, created_at)
VALUES
    (1, 3, 1, NOW()),
    (2, 2, 1, NOW()),
    (3, 2, 2, NOW());

INSERT IGNORE INTO projects (
    id, name, description, owner_id, status, team_id, export_format, current_version_id, created_at, updated_at
)
VALUES
    (1, '春季新品发布片', '主视频发布项目，适配抖音与B站', 2, 'ACTIVE', 1001, 'mp4', 1, NOW(), NOW());

INSERT IGNORE INTO project_material_rel (id, project_id, material_id, created_at)
VALUES
    (1, 1, 1, NOW()),
    (2, 1, 2, NOW());

INSERT IGNORE INTO project_versions (id, project_id, version_no, version_name, content_json, created_by, is_current, created_at)
VALUES
    (1, 1, 1, 'v1-initial', '{"timeline":[{"materialId":1,"start":0,"end":12}],"tracks":[{"id":"video-main"}]}', 2, 1, NOW()),
    (2, 1, 2, 'v2-voice-over', '{"timeline":[{"materialId":1,"start":0,"end":15},{"materialId":2,"start":2,"end":15}],"tracks":[{"id":"video-main"},{"id":"audio-bgm"}]}', 2, 0, NOW());

INSERT IGNORE INTO project_collaborators (id, project_id, user_id, role, created_at)
VALUES
    (1, 1, 1, 'EDITOR', NOW()),
    (2, 1, 3, 'VIEWER', NOW());

INSERT IGNORE INTO system_settings (id, setting_key, setting_value, description, updated_by, updated_at)
VALUES
    (1, 'upload_max_mb', '512', '上传文件大小限制(MB)', 1, NOW()),
    (2, 'storage_root', '/data/uploads', '素材存储根路径', 1, NOW()),
    (3, 'storage_limit_gb', '20', '存储空间阈值(GB)', 1, NOW()),
    (4, 'alert_email', 'ops@cliphub.local', '存储告警通知邮箱', 1, NOW());

INSERT IGNORE INTO notifications (id, title, content, level, status, publish_at, created_at)
VALUES
    (1, '系统升级窗口通知', '本周六凌晨将进行素材索引升级，期间下载接口可能短暂抖动。', 'INFO', 'ENABLED', NOW(), NOW()),
    (2, '存储空间巡检提醒', '请管理员关注存储报表，当使用率超过80%时建议清理历史素材。', 'WARN', 'ENABLED', NOW(), NOW());

INSERT IGNORE INTO collections (id, name, description, cover_material_id, owner_id, visibility, sort_order, is_default, created_at, updated_at)
VALUES
    (1, '我的收藏', '默认收藏素材集', NULL, 1, 'PRIVATE', 0, 1, NOW(), NOW()),
    (2, '我的收藏', '默认收藏素材集', NULL, 2, 'PRIVATE', 0, 1, NOW(), NOW()),
    (3, '我的收藏', '默认收藏素材集', NULL, 3, 'PRIVATE', 0, 1, NOW(), NOW());

INSERT IGNORE INTO collection_material_rel (id, collection_id, material_id, sort_order, note, created_at)
VALUES
    (1, 2, 1, 0, '', NOW()),
    (2, 2, 2, 1, '', NOW()),
    (3, 3, 1, 0, '', NOW());

INSERT IGNORE INTO operation_logs (id, user_id, username, action, target_type, target_id, detail, created_at)
VALUES
    (1, 2, 'vip_3482', 'UPLOAD_MATERIAL', 'MATERIAL', '1', '上传城市夜景延时素材', NOW()),
    (2, 2, 'vip_3482', 'CREATE_PROJECT', 'PROJECT', '1', '创建春季新品发布片项目', NOW()),
    (3, 1, 'admin_3482', 'SAVE_SETTING', 'SYSTEM_SETTING', '3', '更新存储阈值配置', NOW());
