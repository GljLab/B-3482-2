<script setup>
import { onMounted, reactive, ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { CircleCheck, Star, InfoFilled } from '@element-plus/icons-vue';
import http from '../api/http';
import traceApi from '../api/traceability';
import dayjs from 'dayjs';

const route = useRoute();
const router = useRouter();

const projectId = computed(() => Number(route.params.id));

const loading = ref(false);
const project = ref({});
const materials = ref([]);
const availability = ref(null);
const contributors = ref([]);
const recommendations = ref([]);

const copyDialog = ref(false);
const rollbackDialog = ref(false);
const rollbackResult = ref(null);

const copyForm = reactive({
  name: '',
  copyMaterials: false
});

const filterForm = reactive({
  ownership: '',
  status: ''
});

const ownershipOptions = [
  { label: '全部', value: '' },
  { label: '自有', value: 'OWNER' },
  { label: '共享', value: 'SHARED' },
  { label: '外部', value: 'EXTERNAL' }
];

const statusOptions = [
  { label: '全部', value: '' },
  { label: '正常', value: 'ACTIVE' },
  { label: '已失效', value: 'DELETED' }
];

const filteredMaterials = computed(() => {
  let list = [...materials.value];
  if (filterForm.ownership) {
    list = list.filter(m => m.ownership === filterForm.ownership);
  }
  if (filterForm.status) {
    if (filterForm.status === 'DELETED') {
      list = list.filter(m => m.deleted || m.status === 'DELETED');
    } else {
      list = list.filter(m => !m.deleted && m.status !== 'DELETED');
    }
  }
  return list;
});

const loadProject = async () => {
  loading.value = true;
  try {
    const res = await http.get(`/projects/${projectId.value}`);
    project.value = res?.data || {};
    materials.value = (res?.data?.materials || []).map(m => ({
      ...m,
      deleted: m.deleted || m.status === 'DELETED'
    }));
  } finally {
    loading.value = false;
  }
};

const loadAvailability = async () => {
  try {
    const res = await traceApi.getProjectAvailability(projectId.value);
    availability.value = res?.data || null;
  } catch (e) {
    availability.value = null;
  }
};

const loadContributors = async () => {
  try {
    const res = await traceApi.getProjectContributors(projectId.value);
    contributors.value = res?.data || [];
  } catch (e) {
    contributors.value = [];
  }
};

const loadRecommendations = async () => {
  try {
    const res = await traceApi.recommendMaterials(projectId.value, { limit: 10 });
    recommendations.value = res?.data || [];
  } catch (e) {
    recommendations.value = [];
  }
};

const exportInventory = () => {
  window.location.href = traceApi.exportProjectInventory(projectId.value);
};

const openCopyDialog = () => {
  copyForm.name = `${project.value.name || '项目'}_副本`;
  copyForm.copyMaterials = false;
  copyDialog.value = true;
};

const submitCopy = async () => {
  if (!copyForm.name.trim()) {
    ElMessage.warning('请输入新项目名称');
    return;
  }
  loading.value = true;
  try {
    await traceApi.copyProject(projectId.value, {
      name: copyForm.name,
      copyMaterials: copyForm.copyMaterials
    });
    ElMessage.success('项目复制成功');
    copyDialog.value = false;
    router.push('/projects');
  } finally {
    loading.value = false;
  }
};

const openRollbackCheck = async () => {
  loading.value = true;
  try {
    const res = await traceApi.rollbackCheck(projectId.value);
    rollbackResult.value = res?.data || null;
    rollbackDialog.value = true;
  } finally {
    loading.value = false;
  }
};

const bindRecommendMaterial = async (materialId) => {
  await http.post(`/projects/${projectId.value}/materials/${materialId}`);
  ElMessage.success('素材绑定成功');
  await Promise.all([loadProject(), loadRecommendations()]);
};

const unbindMaterial = async (materialId) => {
  await ElMessageBox.confirm('确定从项目中移除此素材？', '确认操作', { type: 'warning' });
  await http.delete(`/projects/${projectId.value}/materials/${materialId}`);
  ElMessage.success('素材已移除');
  await loadProject();
};

const goBack = () => {
  router.push('/projects');
};

const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-';

const formatSize = (bytes) => {
  if (!bytes) return '-';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1048576).toFixed(1) + ' MB';
};

const ownershipLabel = (type) => {
  const map = { OWNER: '自有', SHARED: '共享', EXTERNAL: '外部' };
  return map[type] || type || '-';
};

const ownershipType = (type) => {
  const map = { OWNER: 'success', SHARED: 'warning', EXTERNAL: 'info' };
  return map[type] || 'info';
};

const getContributorPercent = (count) => {
  const total = contributors.value.reduce((sum, c) => sum + (c.materialCount || 0), 0);
  if (total === 0) return 0;
  return ((count / total) * 100).toFixed(1);
};

const tableRowClassName = ({ row }) => {
  if (row.deleted || row.status === 'DELETED') {
    return 'row-deleted';
  }
  return '';
};

const getMaterialStatus = (row) => {
  if (row.deleted || row.status === 'DELETED') {
    return '素材已失效';
  }
  return row.status || 'ACTIVE';
};

const hasAvailabilityIssues = computed(() => {
  if (!availability.value) return false;
  return (availability.value.warnings && availability.value.warnings.length > 0) ||
    (availability.value.errors && availability.value.errors.length > 0);
});

onMounted(async () => {
  await Promise.all([
    loadProject(),
    loadAvailability(),
    loadContributors(),
    loadRecommendations()
  ]);
});
</script>

<template>
  <div class="detail-wrap">
    <section class="page-card toolbar">
      <div>
        <div class="back-row">
          <el-button text @click="goBack">&larr; 返回项目列表</el-button>
        </div>
        <div class="title-row">
          <h2 class="section-title">{{ project.name || '项目详情' }}</h2>
          <el-tag :type="project.status === 'ACTIVE' ? 'success' : 'info'" class="status-tag">
            {{ project.status || 'UNKNOWN' }}
          </el-tag>
        </div>
        <p class="section-subtitle">ID：{{ project.id || '-' }}</p>
      </div>
      <div class="toolbar-actions">
        <el-button plain @click="exportInventory">导出清单</el-button>
        <el-button plain @click="openCopyDialog">复制项目</el-button>
        <el-button plain @click="openRollbackCheck">版本回滚检查</el-button>
        <el-button type="primary" plain @click="loadProject">刷新</el-button>
      </div>
    </section>

    <section class="page-card overview-card">
      <h3 class="card-title">项目概览</h3>
      <div class="overview-grid">
        <div class="overview-item">
          <span class="overview-label">项目ID</span>
          <span class="overview-value">{{ project.id || '-' }}</span>
        </div>
        <div class="overview-item">
          <span class="overview-label">项目描述</span>
          <span class="overview-value">{{ project.description || '暂无描述' }}</span>
        </div>
        <div class="overview-item">
          <span class="overview-label">创建时间</span>
          <span class="overview-value">{{ formatTime(project.createdAt) }}</span>
        </div>
        <div class="overview-item">
          <span class="overview-label">更新时间</span>
          <span class="overview-value">{{ formatTime(project.updatedAt) }}</span>
        </div>
        <div class="overview-item">
          <span class="overview-label">导出格式</span>
          <span class="overview-value">{{ project.exportFormat || 'mp4' }}</span>
        </div>
        <div class="overview-item">
          <span class="overview-label">素材数量</span>
          <span class="overview-value">{{ materials.length }}</span>
        </div>
      </div>
    </section>

    <section v-if="hasAvailabilityIssues" class="page-card availability-card">
      <h3 class="card-title">素材可用性检查</h3>
      <div v-if="availability.errors && availability.errors.length > 0" class="issue-list">
        <el-alert
          v-for="(err, idx) in availability.errors"
          :key="'err-' + idx"
          :title="err.message || err"
          type="error"
          show-icon
          class="issue-alert"
        />
      </div>
      <div v-if="availability.warnings && availability.warnings.length > 0" class="issue-list">
        <el-alert
          v-for="(warn, idx) in availability.warnings"
          :key="'warn-' + idx"
          :title="warn.message || warn"
          type="warning"
          show-icon
          class="issue-alert"
        />
      </div>
    </section>

    <section class="page-card table-panel" v-loading="loading">
      <div class="panel-header">
        <h3 class="card-title">素材清单</h3>
        <div class="filter-inline">
          <el-select v-model="filterForm.ownership" placeholder="归属筛选" style="width: 140px" clearable>
            <el-option v-for="item in ownershipOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="filterForm.status" placeholder="状态筛选" style="width: 140px" clearable>
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
      </div>
      <el-empty v-if="filteredMaterials.length === 0" description="暂无素材" />
      <el-table
        v-else
        :data="filteredMaterials"
        stripe
        row-key="id"
        :row-class-name="tableRowClassName"
      >
        <el-table-column label="素材ID" prop="id" width="80" />
        <el-table-column label="标题" min-width="180">
          <template #default="{ row }">
            <span :class="{ 'text-deleted': row.deleted || row.status === 'DELETED' }">{{ row.title || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="type" width="90" />
        <el-table-column label="分类" prop="category" width="100" />
        <el-table-column label="上传者" prop="uploaderName" width="110" />
        <el-table-column label="贡献者" prop="contributorName" width="110" />
        <el-table-column label="归属" width="90">
          <template #default="{ row }">
            <el-tag :type="ownershipType(row.ownership)" size="small">{{ ownershipLabel(row.ownership) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span v-if="row.deleted || row.status === 'DELETED'" class="status-deleted">素材已失效</span>
            <el-tag v-else type="success" size="small">{{ row.status || 'ACTIVE' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="110">
          <template #default="{ row }">{{ formatSize(row.sizeBytes) }}</template>
        </el-table-column>
        <el-table-column label="添加时间" width="160">
          <template #default="{ row }">{{ formatTime(row.addedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="danger"
              plain
              :disabled="row.deleted || row.status === 'DELETED'"
              @click="unbindMaterial(row.id)"
            >移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="page-card contributors-panel" v-loading="loading">
      <h3 class="card-title">贡献者分析</h3>
      <el-empty v-if="contributors.length === 0" description="暂无贡献者数据" />
      <el-row v-else :gutter="16">
        <el-col v-for="c in contributors" :key="c.userId || c.id" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card shadow="hover" class="contributor-card">
            <div class="contributor-header">
              <div class="avatar-block">
                <el-avatar :size="42">{{ (c.userName || c.name || 'U').charAt(0) }}</el-avatar>
              </div>
              <div class="contributor-info">
                <div class="contributor-name">{{ c.userName || c.name || '未知用户' }}</div>
                <div class="contributor-count">{{ c.materialCount || 0 }} 个素材</div>
              </div>
            </div>
            <div class="contributor-progress">
              <el-progress
                :percentage="Number(getContributorPercent(c.materialCount || 0))"
                :stroke-width="10"
                :show-text="true"
              />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <section class="page-card recommend-panel" v-loading="loading">
      <h3 class="card-title">素材推荐</h3>
      <el-empty v-if="recommendations.length === 0" description="暂无推荐素材" />
      <el-row v-else :gutter="16">
        <el-col v-for="rec in recommendations" :key="rec.id" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card shadow="hover" class="recommend-card">
            <div class="rec-title">{{ rec.title }}</div>
            <div class="rec-meta">
              <el-tag size="small" type="info">{{ rec.type }}</el-tag>
              <span v-if="rec.category" class="rec-category">{{ rec.category }}</span>
            </div>
            <div class="rec-reasons">
              <div v-if="rec.matchCategory" class="reason-item">
                <el-icon color="#67c23a"><CircleCheck /></el-icon>
                <span>匹配分类：{{ rec.matchCategory }}</span>
              </div>
              <div v-if="rec.matchUploader" class="reason-item">
                <el-icon color="#409eff"><CircleCheck /></el-icon>
                <span>匹配上传者：{{ rec.matchUploader }}</span>
              </div>
              <div v-if="rec.reuseCount" class="reason-item">
                <el-icon color="#e6a23c"><Star /></el-icon>
                <span>项目复用：{{ rec.reuseCount }} 次</span>
              </div>
              <div v-if="rec.reason" class="reason-item">
                <el-icon color="#909399"><InfoFilled /></el-icon>
                <span>{{ rec.reason }}</span>
              </div>
            </div>
            <div class="rec-footer">
              <el-button type="primary" size="small" @click="bindRecommendMaterial(rec.id)">绑定到项目</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <el-dialog v-model="copyDialog" title="复制项目" width="min(500px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="新项目名称">
          <el-input v-model="copyForm.name" placeholder="请输入新项目名称" />
        </el-form-item>
        <el-form-item label="素材复制方式">
          <el-radio-group v-model="copyForm.copyMaterials">
            <el-radio :value="false">仅引用原素材</el-radio>
            <el-radio :value="true">复制素材实体</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitCopy">确认复制</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rollbackDialog" title="版本回滚检查" width="min(560px, 92vw)">
      <el-empty v-if="!rollbackResult" description="暂无检查数据" />
      <div v-else class="rollback-content">
        <el-alert
          v-if="rollbackResult.canRollback"
          title="可以安全回滚"
          type="success"
          show-icon
          :closable="false"
          class="rollback-alert"
        />
        <el-alert
          v-else
          title="存在回滚风险"
          type="error"
          show-icon
          :closable="false"
          class="rollback-alert"
        />
        <div v-if="rollbackResult.details && rollbackResult.details.length > 0" class="rollback-details">
          <h4>检查详情</h4>
          <ul>
            <li v-for="(d, i) in rollbackResult.details" :key="i">
              <el-tag :type="d.level === 'error' ? 'danger' : d.level === 'warning' ? 'warning' : 'info'" size="small">
                {{ d.level }}
              </el-tag>
              <span>{{ d.message }}</span>
            </li>
          </ul>
        </div>
        <div v-if="rollbackResult.currentVersion" class="rollback-version">
          <h4>当前版本信息</h4>
          <p>版本名称：{{ rollbackResult.currentVersion.versionName || '-' }}</p>
          <p>创建时间：{{ formatTime(rollbackResult.currentVersion.createdAt) }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-wrap {
  display: grid;
  gap: 1rem;
  min-width: 0;
}

.toolbar {
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  flex-wrap: wrap;
  min-width: 0;
}

.toolbar-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.back-row {
  margin-bottom: 0.4rem;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.status-tag {
  margin-left: 0.25rem;
}

.card-title {
  margin: 0 0 1rem 0;
  font-size: 1.05rem;
  font-weight: 600;
}

.overview-card {
  padding: 1rem;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.overview-item {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  background: rgba(102, 126, 234, 0.06);
  border: 1px solid var(--border-subtle);
}

.overview-label {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.overview-value {
  font-weight: 500;
  word-break: break-all;
}

.availability-card {
  padding: 1rem;
}

.issue-list {
  display: grid;
  gap: 0.6rem;
}

.issue-alert {
  border-radius: 10px;
}

.table-panel {
  padding: 1rem;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.filter-inline {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.table-panel :deep(.el-table) {
  width: 100%;
}

:deep(.row-deleted) {
  background-color: rgba(245, 108, 108, 0.08) !important;
}

:deep(.row-deleted td) {
  color: #f56c6c;
}

.text-deleted {
  color: #f56c6c;
  text-decoration: line-through;
}

.status-deleted {
  color: #f56c6c;
  font-size: 0.85rem;
}

.contributors-panel {
  padding: 1rem;
}

.contributor-card {
  margin-bottom: 1rem;
  border-radius: 14px;
}

.contributor-header {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  margin-bottom: 0.75rem;
}

.avatar-block {
  flex-shrink: 0;
}

.contributor-info {
  flex: 1;
  min-width: 0;
}

.contributor-name {
  font-weight: 600;
  font-size: 0.95rem;
}

.contributor-count {
  color: var(--text-secondary);
  font-size: 0.82rem;
  margin-top: 0.15rem;
}

.contributor-progress {
  padding-top: 0.25rem;
}

.recommend-panel {
  padding: 1rem;
}

.recommend-card {
  margin-bottom: 1rem;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.rec-title {
  font-weight: 600;
  font-size: 0.95rem;
  margin-bottom: 0.5rem;
  word-break: break-all;
}

.rec-meta {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.rec-category {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.rec-reasons {
  display: grid;
  gap: 0.35rem;
  flex: 1;
  margin-bottom: 0.75rem;
}

.reason-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.82rem;
  color: var(--text-secondary);
}

.rec-footer {
  padding-top: 0.5rem;
  border-top: 1px solid var(--border-subtle);
}

.rollback-content {
  display: grid;
  gap: 1rem;
}

.rollback-alert {
  border-radius: 10px;
}

.rollback-details h4,
.rollback-version h4 {
  margin: 0 0 0.5rem 0;
  font-size: 0.92rem;
}

.rollback-details ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.4rem;
}

.rollback-details li {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.88rem;
}

.rollback-version p {
  margin: 0.25rem 0;
  font-size: 0.88rem;
  color: var(--text-secondary);
}

@media (max-width: 980px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-header {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-inline {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
