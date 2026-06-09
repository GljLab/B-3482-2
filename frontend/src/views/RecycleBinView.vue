<script setup>
import { onMounted, ref, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import dayjs from 'dayjs';
import traceApi from '../api/traceability';
import { useAuthStore } from '../stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const actionLoading = ref(false);

const recycleList = ref([]);

const totalCount = computed(() => recycleList.value.length);
const restorableCount = computed(() => {
  return recycleList.value.filter((item) => item.restorable !== false).length;
});
const expiringCount = computed(() => {
  return recycleList.value.filter((item) => {
    const days = remainingDays(item.expireAt);
    return days <= 3 && days >= 0;
  }).length;
});

const formatBytes = (bytes) => {
  if (!bytes) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  let idx = 0;
  let size = bytes;
  while (size >= 1024 && idx < units.length - 1) {
    size /= 1024;
    idx++;
  }
  return `${size.toFixed(size >= 10 || idx === 0 ? 0 : 1)} ${units[idx]}`;
};

const formatDate = (date) => {
  if (!date) return '-';
  return dayjs(date).format('YYYY-MM-DD HH:mm');
};

const remainingDays = (expireAt) => {
  if (!expireAt) return 0;
  return dayjs(expireAt).diff(dayjs(), 'day');
};

const isExpiring = (expireAt) => {
  const days = remainingDays(expireAt);
  return days <= 3 && days >= 0;
};

const canOperate = (row) => {
  if (authStore.role === 'ADMIN') return true;
  const currentUserId = authStore.user?.userId;
  return row.deletedById === currentUserId || row.ownerId === currentUserId;
};

const loadRecycleBin = async () => {
  loading.value = true;
  try {
    const res = await traceApi.getRecycleBin();
    recycleList.value = res?.data || [];
  } finally {
    loading.value = false;
  }
};

const handleRestore = async (row) => {
  try {
    actionLoading.value = true;
    await traceApi.restoreFromRecycle(row.id);
    ElMessage.success(`素材「${row.title}」已恢复`);
    await loadRecycleBin();
  } finally {
    actionLoading.value = false;
  }
};

const handlePermanentDelete = async (row) => {
  await ElMessageBox.confirm(
    `确定要永久删除素材「${row.title}」吗？此操作不可撤销！`,
    '永久删除确认',
    {
      type: 'error',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    }
  );
  try {
    actionLoading.value = true;
    await traceApi.permanentDelete(row.id);
    ElMessage.success(`素材「${row.title}」已永久删除`);
    await loadRecycleBin();
  } finally {
    actionLoading.value = false;
  }
};

const rowClassName = ({ row }) => {
  if (isExpiring(row.expireAt)) {
    return 'expiring-row';
  }
  return '';
};

onMounted(loadRecycleBin);
</script>

<template>
  <div class="recycle-wrap">
    <section class="page-card panel-header">
      <div>
        <h2 class="section-title">回收站管理</h2>
        <p class="section-subtitle">已删除素材暂存区，默认保留 30 天后自动清除</p>
      </div>
      <el-button plain @click="loadRecycleBin" :loading="loading">刷新</el-button>
    </section>

    <section class="grid-3">
      <article class="page-card metric-card">
        <div class="metric-icon total-icon">
          <el-icon :size="22"><FolderDelete /></el-icon>
        </div>
        <div class="metric-content">
          <span class="metric-label">素材总数</span>
          <strong class="metric-value">{{ totalCount }}</strong>
        </div>
      </article>

      <article class="page-card metric-card">
        <div class="metric-icon restore-icon">
          <el-icon :size="22"><RefreshLeft /></el-icon>
        </div>
        <div class="metric-content">
          <span class="metric-label">可恢复数量</span>
          <strong class="metric-value">{{ restorableCount }}</strong>
        </div>
      </article>

      <article class="page-card metric-card">
        <div class="metric-icon expire-icon">
          <el-icon :size="22"><Clock /></el-icon>
        </div>
        <div class="metric-content">
          <span class="metric-label">将到期数量</span>
          <strong class="metric-value danger-value">{{ expiringCount }}</strong>
        </div>
      </article>
    </section>

    <section class="page-card table-panel">
      <el-table
        :data="recycleList"
        v-loading="loading"
        stripe
        :row-class-name="rowClassName"
        style="width: 100%"
      >
        <el-table-column label="素材标题" min-width="220">
          <template #default="{ row }">
            <div class="title-block">
              <strong>{{ row.title }}</strong>
              <span class="subtitle">{{ row.fileName || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" prop="type" />
        <el-table-column label="大小" width="110" align="right">
          <template #default="{ row }">{{ formatBytes(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="删除人" width="140">
          <template #default="{ row }">{{ row.deletedByName || row.deletedByUsername || '-' }}</template>
        </el-table-column>
        <el-table-column label="删除时间" width="170">
          <template #default="{ row }">{{ formatDate(row.deletedAt) }}</template>
        </el-table-column>
        <el-table-column label="剩余天数" width="120" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="remainingDays(row.expireAt) <= 0"
              type="danger"
              size="small"
              effect="dark"
            >
              已过期
            </el-tag>
            <el-tag
              v-else-if="isExpiring(row.expireAt)"
              type="danger"
              size="small"
            >
              {{ remainingDays(row.expireAt) }} 天
            </el-tag>
            <el-tag
              v-else-if="remainingDays(row.expireAt) <= 7"
              type="warning"
              size="small"
            >
              {{ remainingDays(row.expireAt) }} 天
            </el-tag>
            <el-tag v-else type="info" size="small">
              {{ remainingDays(row.expireAt) }} 天
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <el-space>
              <el-button
                size="small"
                type="primary"
                plain
                :loading="actionLoading"
                :disabled="!canOperate(row) || row.restorable === false"
                @click="handleRestore(row)"
              >
                恢复
              </el-button>
              <el-button
                size="small"
                type="danger"
                plain
                :loading="actionLoading"
                :disabled="!canOperate(row)"
                @click="handlePermanentDelete(row)"
              >
                永久删除
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && recycleList.length === 0"
        description="回收站为空，暂无已删除的素材"
        :image-size="140"
      >
        <template #image>
          <div class="empty-illustration">
            <el-icon :size="64" color="var(--border-subtle)"><Delete /></el-icon>
          </div>
        </template>
        <template #description>
          <div class="empty-desc">
            <p>回收站为空</p>
            <p class="empty-sub">删除的素材会在这里保留 30 天，期间可随时恢复</p>
          </div>
        </template>
      </el-empty>
    </section>
  </div>
</template>

<style scoped>
.recycle-wrap {
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
  padding: 1rem 1.2rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  min-height: 100px;
}

.metric-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.metric-icon.total-icon {
  background: rgba(26, 143, 91, 0.12);
  color: var(--accent-main);
}

.metric-icon.restore-icon {
  background: rgba(64, 158, 255, 0.12);
  color: #409eff;
}

.metric-icon.expire-icon {
  background: rgba(231, 76, 60, 0.12);
  color: #e74c3c;
}

.metric-content {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  min-width: 0;
}

.metric-label {
  color: var(--text-secondary);
  font-size: 0.88rem;
}

.metric-value {
  font-size: 1.8rem;
  color: var(--accent-strong);
  line-height: 1.1;
}

.metric-value.danger-value {
  color: #e74c3c;
}

.table-panel {
  padding: 1rem;
  min-width: 0;
  overflow: hidden;
}

.title-block {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.title-block strong {
  font-size: 0.94rem;
}

.title-block .subtitle {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

:deep(.expiring-row) {
  --el-table-tr-bg-color: rgba(231, 76, 60, 0.06);
}

:deep(.expiring-row:hover > td) {
  background-color: rgba(231, 76, 60, 0.1) !important;
}

.table-panel :deep(.el-table) {
  width: 100%;
}

.table-panel :deep(.el-table .cell) {
  word-break: break-word;
}

.empty-illustration {
  display: grid;
  place-items: center;
  padding: 1rem;
}

.empty-desc {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  align-items: center;
}

.empty-desc p {
  margin: 0;
}

.empty-desc .empty-sub {
  color: var(--text-secondary);
  font-size: 0.85rem;
}

@media (max-width: 900px) {
  .panel-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
