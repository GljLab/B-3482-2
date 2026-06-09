<script setup>
import { onMounted, reactive, ref, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import dayjs from 'dayjs';
import traceApi from '../api/traceability';
import { useAuthStore } from '../stores/auth';

const authStore = useAuthStore();
const isAdmin = computed(() => authStore.role === 'ADMIN');

const tabActive = ref('hot');
const loading = ref(false);

const dimensionOptions = [
  { label: '综合', value: 'OVERALL' },
  { label: '下载', value: 'DOWNLOAD' },
  { label: '收藏', value: 'FAVORITE' },
  { label: '项目使用', value: 'PROJECT_USE' },
  { label: '素材集收录', value: 'COLLECTION' },
  { label: '分享', value: 'SHARE' }
];
const currentDimension = ref('OVERALL');
const hotMaterials = ref([]);

const thresholdOptions = [
  { label: '2+ 次', value: 2 },
  { label: '3+ 次', value: 3 },
  { label: '5+ 次', value: 5 }
];
const currentThreshold = ref(2);
const highReuseMaterials = ref([]);

const unusedMaterials = ref([]);
const selectedUnused = ref([]);
const cleanupLoading = ref(false);

const teamContribution = ref([]);

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

const idleDays = (date) => {
  if (!date) return 0;
  return dayjs().diff(dayjs(date), 'day');
};

const rankBadgeType = (index) => {
  if (index === 0) return 'gold';
  if (index === 1) return 'silver';
  if (index === 2) return 'bronze';
  return '';
};

const loadHotMaterials = async () => {
  loading.value = true;
  try {
    const res = await traceApi.getHotMaterials(currentDimension.value, 10);
    hotMaterials.value = res?.data || [];
  } finally {
    loading.value = false;
  }
};

const loadHighReuseMaterials = async () => {
  loading.value = true;
  try {
    const res = await traceApi.getHighReuseMaterials(currentThreshold.value);
    highReuseMaterials.value = res?.data || [];
  } finally {
    loading.value = false;
  }
};

const loadUnusedMaterials = async () => {
  loading.value = true;
  try {
    const res = await traceApi.getUnusedMaterials();
    unusedMaterials.value = res?.data || [];
    selectedUnused.value = [];
  } finally {
    loading.value = false;
  }
};

const loadTeamContribution = async () => {
  loading.value = true;
  try {
    const res = await traceApi.getTeamContribution();
    teamContribution.value = res?.data || [];
  } finally {
    loading.value = false;
  }
};

const handleTabChange = async (name) => {
  tabActive.value = name;
  if (name === 'hot') await loadHotMaterials();
  if (name === 'reuse') await loadHighReuseMaterials();
  if (name === 'unused') await loadUnusedMaterials();
  if (name === 'team') await loadTeamContribution();
};

const handleDimensionChange = () => {
  loadHotMaterials();
};

const handleThresholdChange = () => {
  loadHighReuseMaterials();
};

const handleSimulateCleanup = async () => {
  if (selectedUnused.value.length === 0) {
    ElMessage.warning('请先选择要清理的素材');
    return;
  }
  try {
    cleanupLoading.value = true;
    const res = await traceApi.simulateCleanupUnused(selectedUnused.value);
    const saved = res?.data?.savedBytes || 0;
    const count = res?.data?.count || selectedUnused.value.length;
    await ElMessageBox.alert(
      `模拟清理完成：将释放 ${count} 个素材，约 ${formatBytes(saved)} 存储空间`,
      '模拟清理结果',
      { confirmButtonText: '我知道了' }
    );
  } finally {
    cleanupLoading.value = false;
  }
};

const handleCleanup = async () => {
  if (selectedUnused.value.length === 0) {
    ElMessage.warning('请先选择要清理的素材');
    return;
  }
  await ElMessageBox.confirm(
    `确认清理选中的 ${selectedUnused.value.length} 个孤岛素材？清理后将移入回收站，可在30天内恢复。`,
    '清理确认',
    { type: 'warning' }
  );
  try {
    cleanupLoading.value = true;
    await traceApi.cleanupUnused(selectedUnused.value);
    ElMessage.success('清理成功，素材已移入回收站');
    await loadUnusedMaterials();
  } finally {
    cleanupLoading.value = false;
  }
};

const contributionScore = (row) => {
  const upload = row.uploadCount || 0;
  const download = row.totalDownload || 0;
  const favorite = row.totalFavorite || 0;
  const projectUse = row.projectUseCount || 0;
  return upload * 2 + download + favorite * 3 + projectUse * 5;
};

onMounted(loadHotMaterials);
</script>

<template>
  <div class="analytics-wrap">
    <section class="page-card panel-header">
      <div>
        <h2 class="section-title">数据分析中心</h2>
        <p class="section-subtitle">多维度洞察素材热度、复用价值、冗余资产与团队贡献</p>
      </div>
    </section>

    <section class="page-card tabs-panel">
      <el-tabs v-model="tabActive" @tab-change="handleTabChange">
        <el-tab-pane label="素材热度排行" name="hot">
          <div class="tab-toolbar">
            <div class="dimension-group">
              <span class="label">维度选择：</span>
              <el-radio-group v-model="currentDimension" @change="handleDimensionChange">
                <el-radio-button
                  v-for="opt in dimensionOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <el-table :data="hotMaterials" v-loading="loading" stripe>
            <el-table-column label="排名" width="90" align="center">
              <template #default="{ $index }">
                <el-badge
                  v-if="$index < 3"
                  :value="rankBadgeType($index)"
                  :hidden="false"
                  class="rank-badge"
                >
                  <span class="rank-num rank-{{ $index + 1 }}">{{ $index + 1 }}</span>
                </el-badge>
                <span v-else class="rank-num">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="标题" min-width="220" prop="title" />
            <el-table-column label="类型" width="100" prop="type" />
            <el-table-column label="下载" width="100" prop="downloadCount" align="center" />
            <el-table-column label="收藏" width="100" prop="favoriteCount" align="center" />
            <el-table-column label="项目使用" width="110" prop="projectUseCount" align="center" />
            <el-table-column label="素材集收录" width="120" prop="collectionCount" align="center" />
            <el-table-column label="分享" width="90" prop="shareCount" align="center" />
            <el-table-column label="综合分数" width="120" align="center">
              <template #default="{ row }">
                <strong class="score-text">{{ row.score || 0 }}</strong>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="高频复用素材" name="reuse">
          <div class="tab-toolbar">
            <div class="dimension-group">
              <span class="label">复用阈值：</span>
              <el-radio-group v-model="currentThreshold" @change="handleThresholdChange">
                <el-radio-button
                  v-for="opt in thresholdOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <el-table :data="highReuseMaterials" v-loading="loading" stripe>
            <el-table-column label="素材ID" width="90" prop="id" />
            <el-table-column label="标题" min-width="240">
              <template #default="{ row }">
                <div class="title-with-tag">
                  <span>{{ row.title }}</span>
                  <el-tag
                    v-if="row.isTeamPick"
                    type="warning"
                    size="small"
                    effect="dark"
                    class="team-pick-tag"
                  >
                    团队精选
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="100" prop="type" />
            <el-table-column label="复用项目数" width="130" prop="reuseProjectCount" align="center">
              <template #default="{ row }">
                <el-tag type="success" size="small">
                  {{ row.reuseProjectCount || 0 }} 个
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="总使用次数" width="120" prop="totalUseCount" align="center" />
            <el-table-column label="上传者" width="140" prop="uploaderName" />
            <el-table-column label="最近使用" width="170">
              <template #default="{ row }">{{ formatDate(row.lastUsedAt) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="孤岛素材/冗余清理" name="unused">
          <div class="tab-toolbar">
            <div class="unused-summary">
              <span class="summary-text">
                共 <strong>{{ unusedMaterials.length }}</strong> 个闲置素材，
                累计约 <strong>{{ formatBytes(unusedMaterials.reduce((s, r) => s + (r.fileSize || 0), 0)) }}</strong>
              </span>
            </div>
            <div v-if="isAdmin" class="unused-actions">
              <el-button
                plain
                :loading="cleanupLoading"
                :disabled="selectedUnused.length === 0"
                @click="handleSimulateCleanup"
              >
                模拟清理
              </el-button>
              <el-button
                type="danger"
                :loading="cleanupLoading"
                :disabled="selectedUnused.length === 0"
                @click="handleCleanup"
              >
                清理选中
              </el-button>
            </div>
          </div>

          <el-table
            :data="unusedMaterials"
            v-loading="loading"
            stripe
            @selection-change="(val) => selectedUnused = val.map(i => i.id)"
          >
            <el-table-column v-if="isAdmin" type="selection" width="55" />
            <el-table-column label="标题" min-width="240" prop="title" />
            <el-table-column label="类型" width="100" prop="type" />
            <el-table-column label="大小" width="120" align="right">
              <template #default="{ row }">{{ formatBytes(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column label="上传时间" width="170">
              <template #default="{ row }">{{ formatDate(row.uploadedAt) }}</template>
            </el-table-column>
            <el-table-column label="闲置天数" width="120" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="idleDays(row.uploadedAt) >= 90 ? 'danger' : idleDays(row.uploadedAt) >= 30 ? 'warning' : 'info'"
                  size="small"
                >
                  {{ idleDays(row.uploadedAt) }} 天
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <el-empty
            v-if="!loading && unusedMaterials.length === 0"
            description="暂无闲置素材，资产流转健康"
            :image-size="120"
          />
        </el-tab-pane>

        <el-tab-pane label="团队贡献排行" name="team">
          <el-table :data="teamContribution" v-loading="loading" stripe>
            <el-table-column label="排名" width="80" align="center">
              <template #default="{ $index }">
                <el-badge
                  v-if="$index < 3"
                  :value="rankBadgeType($index)"
                  :hidden="false"
                  class="rank-badge"
                >
                  <span class="rank-num rank-{{ $index + 1 }}">{{ $index + 1 }}</span>
                </el-badge>
                <span v-else class="rank-num">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="用户" min-width="180">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :size="36" style="background: var(--accent-main)">
                    {{ (row.displayName || row.username || 'U').charAt(0) }}
                  </el-avatar>
                  <div class="user-info">
                    <strong>{{ row.displayName || row.username }}</strong>
                    <span>{{ row.username }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="上传数" width="100" prop="uploadCount" align="center" />
            <el-table-column label="总下载" width="110" prop="totalDownload" align="center" />
            <el-table-column label="总收藏" width="110" prop="totalFavorite" align="center" />
            <el-table-column label="项目使用数" width="130" prop="projectUseCount" align="center" />
            <el-table-column label="贡献分数" width="130" align="center">
              <template #default="{ row }">
                <strong class="score-text">{{ contributionScore(row) }}</strong>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<style scoped>
.analytics-wrap {
  display: grid;
  gap: 1rem;
  min-width: 0;
}

.panel-header {
  padding: 1rem 1.2rem;
  min-width: 0;
}

.tabs-panel {
  padding: 1rem;
  min-width: 0;
  overflow: hidden;
}

.tab-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.dimension-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.dimension-group .label {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.rank-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--bg-soft);
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--text-primary);
}

.rank-num.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ffb700);
  color: #5a3e00;
}

.rank-num.rank-2 {
  background: linear-gradient(135deg, #d6d6d6, #a8a8a8);
  color: #3d3d3d;
}

.rank-num.rank-3 {
  background: linear-gradient(135deg, #e3a275, #b87333);
  color: #3d2817;
}

:deep(.rank-badge .el-badge__content) {
  border: none;
  font-size: 0.65rem;
  padding: 0 4px;
  height: 16px;
  line-height: 16px;
  margin-top: -4px;
  margin-right: -4px;
  text-transform: capitalize;
}

:deep(.rank-badge .el-badge__content[value="gold"]) {
  background: linear-gradient(135deg, #ffd700, #ffb700);
  color: #5a3e00;
}

:deep(.rank-badge .el-badge__content[value="silver"]) {
  background: linear-gradient(135deg, #d6d6d6, #a8a8a8);
  color: #3d3d3d;
}

:deep(.rank-badge .el-badge__content[value="bronze"]) {
  background: linear-gradient(135deg, #e3a275, #b87333);
  color: #3d2817;
}

.score-text {
  font-size: 1rem;
  color: var(--accent-strong);
}

.title-with-tag {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.team-pick-tag {
  margin-left: 0.25rem;
}

.unused-summary .summary-text {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.unused-summary .summary-text strong {
  color: var(--accent-strong);
  font-size: 1rem;
  margin: 0 0.15rem;
}

.unused-actions {
  display: flex;
  gap: 0.5rem;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.user-info strong {
  font-size: 0.92rem;
}

.user-info span {
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.tabs-panel :deep(.el-table) {
  width: 100%;
}

.tabs-panel :deep(.el-table .cell) {
  word-break: break-word;
}

@media (max-width: 980px) {
  .tab-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .unused-actions {
    justify-content: flex-start;
  }
}
</style>
