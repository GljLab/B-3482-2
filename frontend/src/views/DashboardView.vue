<script setup>
import { onMounted, reactive, ref } from 'vue';
import http from '../api/http';
import { useAuthStore } from '../stores/auth';

const authStore = useAuthStore();

const loading = ref(true);
const metrics = reactive({
  materialCount: 0,
  projectCount: 0,
  favoritesCount: 0,
  notificationCount: 0,
  storageUsage: '-'
});

const hotMaterials = ref([]);
const userActivities = ref([]);
const collectionStats = ref([]);

const loadDashboard = async () => {
  loading.value = true;
  try {
    const [materialsRes, projectsRes, favoritesRes, notificationsRes] = await Promise.all([
      http.get('/materials'),
      http.get('/projects'),
      http.get('/materials/favorites'),
      http.get('/notifications/public')
    ]);

    metrics.materialCount = (materialsRes?.data || []).length;
    metrics.projectCount = (projectsRes?.data || []).length;
    metrics.favoritesCount = (favoritesRes?.data || []).length;
    metrics.notificationCount = (notificationsRes?.data || []).length;

    if (authStore.role === 'ADMIN') {
      const [storageRes, hotRes, activityRes] = await Promise.all([
        http.get('/stats/storage'),
        http.get('/stats/hot-materials'),
        http.get('/stats/user-activity')
      ]);
      metrics.storageUsage = `${storageRes?.data?.usagePercent || 0}%`;
      hotMaterials.value = hotRes?.data || [];
      userActivities.value = activityRes?.data || [];
    } else {
      hotMaterials.value = (materialsRes?.data || []).slice(0, 6).map((item) => ({
        materialId: item.id,
        title: item.title,
        type: item.type,
        score: (item.downloadCount || 0) + (item.favoriteCount || 0)
      }));
      userActivities.value = [];
    }

    const colStatsRes = await http.get('/collections/stats');
    collectionStats.value = (colStatsRes?.data || []).slice(0, 5);
  } finally {
    loading.value = false;
  }
};

onMounted(loadDashboard);
</script>

<template>
  <div class="dashboard-wrap">
    <section class="page-card panel-header">
      <div>
        <h2 class="section-title">运营总览</h2>
        <p class="section-subtitle">围绕素材流转、项目协作与权限治理的统一观测面板</p>
      </div>
      <el-button plain @click="loadDashboard">刷新数据</el-button>
    </section>

    <section class="grid-3">
      <el-skeleton :loading="loading" animated>
        <template #template><el-skeleton-item variant="rect" style="height: 140px; border-radius: 16px" /></template>
        <template #default>
          <article class="page-card metric-card">
            <h3>素材总量</h3>
            <strong>{{ metrics.materialCount }}</strong>
            <p>覆盖视频、音频、图像与模板资产</p>
          </article>
        </template>
      </el-skeleton>

      <el-skeleton :loading="loading" animated>
        <template #template><el-skeleton-item variant="rect" style="height: 140px; border-radius: 16px" /></template>
        <template #default>
          <article class="page-card metric-card">
            <h3>项目数量</h3>
            <strong>{{ metrics.projectCount }}</strong>
            <p>支持版本迭代与导出落地</p>
          </article>
        </template>
      </el-skeleton>

      <el-skeleton :loading="loading" animated>
        <template #template><el-skeleton-item variant="rect" style="height: 140px; border-radius: 16px" /></template>
        <template #default>
          <article class="page-card metric-card">
            <h3>收藏与通知</h3>
            <strong>{{ metrics.favoritesCount }} / {{ metrics.notificationCount }}</strong>
            <p v-if="authStore.role === 'ADMIN'">当前存储使用率 {{ metrics.storageUsage }}</p>
            <p v-else>聚焦你当前可见素材与协作项目</p>
          </article>
        </template>
      </el-skeleton>
    </section>

    <section class="grid-2">
      <article class="page-card table-card">
        <div class="table-head">
          <h3>热门素材排行</h3>
        </div>
        <el-table :data="hotMaterials" height="300" stripe>
          <el-table-column label="素材ID" prop="materialId" width="90" />
          <el-table-column label="标题" prop="title" />
          <el-table-column label="类型" prop="type" width="120" />
          <el-table-column label="热度" prop="score" width="120" />
        </el-table>
      </article>

      <article class="page-card table-card">
        <div class="table-head">
          <h3>素材集使用排行</h3>
        </div>
        <el-table :data="collectionStats" height="300" stripe>
          <el-table-column label="素材集" prop="name" />
          <el-table-column label="素材数量" prop="materialCount" width="100" />
          <el-table-column label="总容量" width="120">
            <template #default="{ row }">{{ (row.totalBytes / 1048576).toFixed(1) }} MB</template>
          </el-table-column>
        </el-table>
      </article>
    </section>

    <section v-if="authStore.role === 'ADMIN'" class="grid-2">
      <article class="page-card table-card">
        <div class="table-head">
          <h3>用户活跃度（近30天）</h3>
        </div>
        <el-table :data="userActivities" height="300" stripe>
          <el-table-column label="用户" prop="displayName" />
          <el-table-column label="账号" prop="username" />
          <el-table-column label="操作次数" prop="activityCount" width="140" />
        </el-table>
      </article>
    </section>
  </div>
</template>

<style scoped>
.dashboard-wrap {
  display: grid;
  gap: 1rem;
  min-width: 0;
}

.panel-header {
  padding: 1rem 1.2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  min-width: 0;
}

.metric-card {
  padding: 1.2rem;
  min-height: 140px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.metric-card h3 {
  margin: 0;
  font-size: 0.96rem;
}

.metric-card strong {
  font-size: 2rem;
  color: var(--accent-strong);
}

.metric-card p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.table-card {
  padding: 1rem;
  min-width: 0;
  overflow: hidden;
}

.table-head {
  margin-bottom: 0.8rem;
}

.table-head h3 {
  margin: 0;
  font-size: 1rem;
}

.table-card :deep(.el-table) {
  width: 100%;
}
</style>
