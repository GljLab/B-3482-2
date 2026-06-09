# Clip Hub 素材协作平台（label-3482）

## 🛠 技术栈
- Frontend: Vue 3 + Vite + Element Plus + Pinia
- Backend: Spring Boot 3 + Spring Security + MyBatis Plus + Redis
- Database: MySQL 8.0

## 🚀 启动指南 (How to Run)
1. 确保 Docker Desktop 已启动。
2. 在项目根目录执行：`docker compose up --build`
3. 首次启动会自动初始化数据库结构与种子数据，等待所有容器健康后即可访问。

## 🔗 服务地址 (Services)
- Frontend: [http://localhost:3482](http://localhost:3482)
- Backend API: [http://localhost:8482/api](http://localhost:8482/api)
- Database: `localhost:13482` (user: `root` / pass: `Xy7441015888` / db: `clip_hub_db`)
- Redis: `localhost:16382`

## 🧪 测试账号
- Admin: `admin_3482` / `Admin@2026`
- VIP: `vip_3482` / `Vip@2026`
- User: `user_3482` / `User@2026`

## 📦 核心功能覆盖
- 用户管理：注册、登录、退出、角色管理、个人资料更新、密码找回与重置。
- 素材管理：上传、分类与标签、检索、预览、下载、编辑、删除、收藏、分享。
- 项目管理：项目创建、素材绑定、版本保存与回滚、项目导出、协作成员管理。
- 权限安全：JWT 登录态、RBAC 访问控制、素材公开/私有/团队可见、操作日志记录。
- 系统设置：参数配置、分类标签治理、通知管理、存储阈值告警配置。
- 统计报表：用户活跃度、素材使用频次、存储使用报表、热门素材排行。
