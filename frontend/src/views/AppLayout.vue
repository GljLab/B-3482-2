<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import http from '../api/http';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const mobileMenuOpen = ref(false);
const notifications = ref([]);

const menuItems = computed(() => {
  const base = [
    { path: '/', label: '总览看板', icon: 'DataBoard' },
    { path: '/materials', label: '素材中心', icon: 'FolderOpened' },
    { path: '/collections', label: '我的素材集', icon: 'Collection' },
    { path: '/projects', label: '项目工坊', icon: 'Film' },
    { path: '/profile', label: '个人中心', icon: 'User' }
  ];
  if (authStore.role === 'ADMIN') {
    base.splice(4, 0, { path: '/admin', label: '系统治理', icon: 'Setting' });
  }
  return base;
});

const loadNotifications = async () => {
  const res = await http.get('/notifications/public');
  notifications.value = res?.data || [];
};

const logout = async () => {
  try {
    await http.post('/auth/logout');
  } finally {
    authStore.clearAuth();
    router.push('/auth');
  }
};

loadNotifications();
</script>

<template>
  <div class="layout-shell">
    <header class="layout-header page-card">
      <div class="brand-block">
        <button class="mobile-menu-btn" @click="mobileMenuOpen = !mobileMenuOpen" aria-label="切换菜单">
          ☰
        </button>
        <div>
          <h1 class="brand-title">Clip Hub</h1>
          <p class="header-sub">视频素材与项目协作平台</p>
        </div>
      </div>
      <div class="header-actions">
        <el-popover placement="bottom-end" trigger="hover" width="360">
          <template #reference>
            <el-button plain>系统通知 ({{ notifications.length }})</el-button>
          </template>
          <div class="notification-box">
            <p v-if="notifications.length === 0" class="notification-empty">暂无通知</p>
            <div v-else v-for="item in notifications" :key="item.id" class="notification-item">
              <h4>{{ item.title }}</h4>
              <p>{{ item.content }}</p>
            </div>
          </div>
        </el-popover>
        <el-tag effect="dark" type="success">{{ authStore.user?.displayName || '未登录用户' }}</el-tag>
        <el-button type="danger" plain @click="logout">退出登录</el-button>
      </div>
    </header>

    <main class="layout-main">
      <aside class="layout-nav page-card" :class="{ mobile: mobileMenuOpen }">
        <el-menu :default-active="route.path" router>
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <section class="layout-content">
        <router-view />
      </section>
    </main>

    <footer class="layout-footer">
      <span>Clip Hub · 资产统一管理 · 权限可控 · 协作可追溯</span>
    </footer>
  </div>
</template>

<style scoped>
.layout-shell {
  min-height: 100vh;
  width: 100%;
  max-width: 100vw;
  padding: 1rem;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 80% 10%, rgba(140, 202, 165, 0.4), transparent 40%),
    radial-gradient(circle at 10% 90%, rgba(220, 182, 102, 0.24), transparent 30%),
    linear-gradient(155deg, #f7fbf3 0%, #ebf3ea 55%, #dce6de 100%);
}

.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.2rem;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.brand-title {
  margin: 0;
  font-size: 1.5rem;
}

.header-sub {
  margin: 0.2rem 0 0;
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.mobile-menu-btn {
  display: none;
  border: none;
  background: rgba(27, 98, 62, 0.12);
  color: var(--text-primary);
  border-radius: 10px;
  width: 2rem;
  height: 2rem;
  cursor: pointer;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.layout-main {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 1rem;
  min-width: 0;
}

.layout-nav {
  padding: 0.6rem;
  min-height: calc(100vh - 170px);
  min-width: 0;
  overflow: hidden;
}

.layout-nav :deep(.el-menu) {
  border-right: none;
  --el-menu-border-color: transparent;
}

.layout-nav :deep(.el-scrollbar__bar.is-vertical) {
  display: none;
}

.layout-content {
  min-height: calc(100vh - 170px);
  min-width: 0;
}

.layout-footer {
  margin-top: 1rem;
  text-align: center;
  color: var(--text-secondary);
  font-size: 0.82rem;
  padding-bottom: 0.5rem;
}

.notification-box {
  max-height: 320px;
  overflow: auto;
}

.notification-item {
  padding: 0.5rem 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.notification-item h4 {
  margin: 0;
  font-size: 0.95rem;
}

.notification-item p {
  margin: 0.4rem 0 0;
  font-size: 0.82rem;
  color: var(--text-secondary);
}

.notification-empty {
  color: var(--text-secondary);
  margin: 0;
}

@media (max-width: 980px) {
  .layout-shell {
    padding: 0.75rem;
  }

  .layout-main {
    grid-template-columns: 1fr;
  }

  .layout-nav {
    display: none;
    min-height: auto;
  }

  .layout-nav.mobile {
    display: block;
  }

  .mobile-menu-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .layout-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    flex-wrap: wrap;
  }
}
</style>
