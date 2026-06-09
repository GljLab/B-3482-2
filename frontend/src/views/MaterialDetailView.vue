<script setup>
import { onMounted, reactive, ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '../api/http';
import traceApi from '../api/traceability';
import { useAuthStore } from '../stores/auth';
import dayjs from 'dayjs';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const materialId = computed(() => route.params.id);
const loading = ref(false);
const material = ref({});
const previewUrl = ref('');

const timeline = ref([]);
const refStats = ref({});
const projectList = ref([]);
const collectionList = ref([]);
const modHistory = ref([]);

const editDialog = ref(false);
const editForm = reactive({
  id: null,
  title: '',
  description: '',
  visibility: 'PUBLIC',
  categoryId: undefined,
  durationSeconds: undefined,
  resolution: '',
  tagIds: []
});

const categories = ref([]);
const tags = ref([]);

const deleteDialog = ref(false);
const deleteLoading = ref(false);
const deleteImpact = ref({});
const deleteStrategy = ref('RECYCLE_BIN');

const visibilityOptions = [
  { label: '公开', value: 'PUBLIC' },
  { label: '私有', value: 'PRIVATE' },
  { label: '团队可见', value: 'TEAM' }
];

const typeOptions = [
  { label: '视频', value: 'VIDEO' },
  { label: '音频', value: 'AUDIO' },
  { label: '图片', value: 'IMAGE' },
  { label: '模板', value: 'TEMPLATE' }
];

const actionTypeColorMap = {
  LIFECYCLE: 'primary',
  PROJECT_USAGE: 'success',
  COLLECTION_USAGE: 'warning',
  MODIFICATION: 'info',
  INTERACTION: '',
  SHARING: '',
  OWNERSHIP: 'danger'
};

const deleteStrategyOptions = [
  { label: '移入回收站（30天可恢复，默认）', value: 'RECYCLE_BIN' },
  { label: '通知用户后删除', value: 'NOTIFY_AND_DELETE' },
  { label: '强制删除不可恢复', value: 'PERMANENT_DELETE' }
];

const formatSize = (bytes) => {
  if (!bytes) return '-';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1048576).toFixed(1) + ' MB';
};

const formatDuration = (seconds) => {
  if (!seconds) return '-';
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
};

const formatTime = (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-';

const getTypeLabel = (type) => {
  const opt = typeOptions.find(o => o.value === type);
  return opt ? opt.label : type || '-';
};

const getVisibilityLabel = (v) => {
  const opt = visibilityOptions.find(o => o.value === v);
  return opt ? opt.label : v || '-';
};

const isSafeToDelete = computed(() => {
  const stats = refStats.value || {};
  return (stats.projectCount || 0) === 0
    && (stats.collectionCount || 0) === 0
    && (stats.favoriteCount || 0) === 0;
});

const loadTaxonomy = async () => {
  const [categoryRes, tagRes] = await Promise.all([
    http.get('/categories'),
    http.get('/tags')
  ]);
  categories.value = categoryRes?.data || [];
  tags.value = tagRes?.data || [];
};

const loadMaterialDetail = async () => {
  loading.value = true;
  try {
    const res = await http.get(`/materials/${materialId.value}`);
    material.value = res?.data || {};
    await loadPreview();
  } finally {
    loading.value = false;
  }
};

const loadPreview = async () => {
  try {
    const token = localStorage.getItem('clip_hub_token');
    const response = await fetch(`/api/materials/${materialId.value}/preview`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (response.ok) {
      const blob = await response.blob();
      previewUrl.value = URL.createObjectURL(blob);
    }
  } catch (e) {
    console.warn('预览加载失败', e);
  }
};

const loadTimeline = async () => {
  try {
    const res = await traceApi.getMaterialTimeline(materialId.value);
    timeline.value = res?.data?.timeline || res?.data || [];
  } catch (e) {
    timeline.value = [];
  }
};

const loadRefStats = async () => {
  try {
    const res = await traceApi.getMaterialRefStats(materialId.value);
    refStats.value = res?.data || {};
  } catch (e) {
    refStats.value = {};
  }
};

const loadProjects = async () => {
  try {
    const res = await traceApi.getMaterialProjects(materialId.value);
    projectList.value = res?.data || [];
  } catch (e) {
    projectList.value = [];
  }
};

const loadCollections = async () => {
  try {
    const res = await traceApi.getMaterialCollections(materialId.value);
    collectionList.value = res?.data || [];
  } catch (e) {
    collectionList.value = [];
  }
};

const loadModHistory = async () => {
  try {
    const res = await traceApi.getMaterialModHistory(materialId.value);
    modHistory.value = res?.data || [];
  } catch (e) {
    modHistory.value = [];
  }
};

const loadAll = async () => {
  await Promise.all([
    loadMaterialDetail(),
    loadTimeline(),
    loadRefStats(),
    loadProjects(),
    loadCollections(),
    loadModHistory(),
    loadTaxonomy()
  ]);
};

const goBack = () => {
  router.push('/materials');
};

const openEdit = () => {
  const m = material.value;
  editForm.id = m.id;
  editForm.title = m.title || '';
  editForm.description = m.description || '';
  editForm.visibility = m.visibility || 'PUBLIC';
  editForm.categoryId = m.category?.id;
  editForm.durationSeconds = m.durationSeconds;
  editForm.resolution = m.resolution || '';
  editForm.tagIds = (m.tags || []).map((tag) => tag.id);
  editDialog.value = true;
};

const submitEdit = async () => {
  loading.value = true;
  try {
    await http.put(`/materials/${editForm.id}`, {
      title: editForm.title,
      description: editForm.description,
      visibility: editForm.visibility,
      categoryId: editForm.categoryId,
      durationSeconds: editForm.durationSeconds,
      resolution: editForm.resolution,
      tagIds: editForm.tagIds
    });
    ElMessage.success('素材已更新');
    editDialog.value = false;
    await loadAll();
  } finally {
    loading.value = false;
  }
};

const copyToClipboard = async (text) => {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch (error) {
    console.warn('Clipboard API unavailable:', error);
  }
  const input = document.createElement('textarea');
  input.value = text;
  input.setAttribute('readonly', 'readonly');
  input.style.position = 'fixed';
  input.style.left = '-9999px';
  document.body.appendChild(input);
  input.select();
  const copied = document.execCommand('copy');
  document.body.removeChild(input);
  return copied;
};

const createShare = async () => {
  const res = await http.post(`/materials/${materialId.value}/share`, { expireHours: 48 });
  const link = `${window.location.origin}${res?.data?.shareUrl || ''}`;
  const copied = await copyToClipboard(link);
  if (copied) {
    ElMessage.success('分享链接已复制到剪贴板');
    return;
  }
  await ElMessageBox.alert(link, '复制失败，请手动复制以下链接', {
    confirmButtonText: '我知道了'
  });
};

const downloadMaterial = async () => {
  const token = localStorage.getItem('clip_hub_token');
  const m = material.value;
  const response = await fetch(`/api/materials/${materialId.value}/download?quality=source&format=${m.format || ''}`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  if (!response.ok) {
    ElMessage.error('下载失败');
    return;
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = m.fileName || `${m.title}.${m.format || 'bin'}`;
  a.click();
  URL.revokeObjectURL(url);
};

const openDeleteDialog = async () => {
  deleteLoading.value = true;
  deleteStrategy.value = 'RECYCLE_BIN';
  try {
    const res = await traceApi.getDeleteImpact(materialId.value);
    deleteImpact.value = res?.data || {};
    deleteDialog.value = true;
  } finally {
    deleteLoading.value = false;
  }
};

const confirmDelete = async () => {
  deleteLoading.value = true;
  try {
    await traceApi.deleteWithStrategy(materialId.value, {
      strategy: deleteStrategy.value
    });
    ElMessage.success('删除操作已完成');
    deleteDialog.value = false;
    router.push('/materials');
  } finally {
    deleteLoading.value = false;
  }
};

const toggleFavorite = async () => {
  const res = await http.post(`/materials/${materialId.value}/favorite`);
  ElMessage.success(res?.data?.favorited ? '已加入收藏' : '已取消收藏');
  await loadAll();
};

onMounted(loadAll);
</script>

<template>
  <div class="detail-wrap" v-loading="loading">
    <section class="page-card toolbar">
      <div class="toolbar-left">
        <div class="back-row">
          <el-button text @click="goBack">&larr; 返回素材列表</el-button>
        </div>
        <h2 class="section-title">{{ material.title || '素材详情' }}</h2>
        <p class="section-subtitle">{{ material.description || '暂无描述' }}</p>
      </div>
      <div class="toolbar-actions">
        <el-button type="danger" plain @click="openDeleteDialog" :loading="deleteLoading">删除</el-button>
        <el-button plain @click="openEdit">编辑</el-button>
        <el-button plain @click="createShare">分享</el-button>
        <el-button type="primary" plain @click="downloadMaterial">下载</el-button>
        <el-button plain @click="toggleFavorite">
          {{ material.favorited ? '取消收藏' : '收藏' }}
        </el-button>
      </div>
    </section>

    <section class="detail-grid">
      <div class="preview-col page-card">
        <div class="section-head">
          <h3>素材预览</h3>
          <el-tag :type="material.type === 'IMAGE' ? 'success' : material.type === 'VIDEO' ? 'primary' : material.type === 'AUDIO' ? 'warning' : 'info'">
            {{ getTypeLabel(material.type) }}
          </el-tag>
        </div>
        <div class="preview-box">
          <template v-if="previewUrl">
            <img v-if="material.type === 'IMAGE'" :src="previewUrl" alt="素材预览" class="preview-media" />
            <video v-else-if="material.type === 'VIDEO'" :src="previewUrl" controls class="preview-media" />
            <audio v-else-if="material.type === 'AUDIO'" :src="previewUrl" controls class="preview-audio" />
            <el-input v-else type="textarea" :rows="10" :model-value="previewUrl" disabled />
          </template>
          <el-empty v-else description="暂无预览内容" />
        </div>
      </div>

      <div class="info-col">
        <div class="page-card info-card">
          <div class="section-head">
            <h3>基础信息</h3>
          </div>
          <el-descriptions :column="2" border size="default">
            <el-descriptions-item label="素材ID">{{ material.id || '-' }}</el-descriptions-item>
            <el-descriptions-item label="素材类型">{{ getTypeLabel(material.type) }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ material.category?.name || '未分类' }}</el-descriptions-item>
            <el-descriptions-item label="可见性">
              <el-tag :type="material.visibility === 'PUBLIC' ? 'success' : material.visibility === 'TEAM' ? 'warning' : 'info'" size="small">
                {{ getVisibilityLabel(material.visibility) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="文件大小">{{ formatSize(material.sizeBytes) }}</el-descriptions-item>
            <el-descriptions-item label="文件格式">{{ material.format || '-' }}</el-descriptions-item>
            <el-descriptions-item label="时长">{{ formatDuration(material.durationSeconds) }}</el-descriptions-item>
            <el-descriptions-item label="分辨率">{{ material.resolution || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上传者">{{ material.uploader?.displayName || material.uploader?.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上传时间">{{ formatTime(material.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="来源信息" :span="2">
              {{ material.sourceInfo || material.source || '未记录' }}
            </el-descriptions-item>
            <el-descriptions-item label="标签" :span="2">
              <el-space wrap v-if="material.tags && material.tags.length">
                <el-tag v-for="tag in material.tags" :key="tag.id" size="small">{{ tag.name }}</el-tag>
              </el-space>
              <span v-else class="empty-text">无标签</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </section>

    <section class="page-card stats-card">
      <div class="section-head">
        <h3>引用统计</h3>
        <el-alert
          v-if="isSafeToDelete"
          title="安全可删除：该素材未被任何项目或素材集引用，也未被收藏"
          type="success"
          :closable="false"
          show-icon
          size="small"
        />
      </div>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="12" :md="6">
          <el-card shadow="never" class="stat-item">
            <div class="stat-num" style="color: var(--accent-main)">{{ refStats.projectCount || 0 }}</div>
            <div class="stat-label">项目使用数</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <el-card shadow="never" class="stat-item">
            <div class="stat-num" style="color: var(--accent-warning)">{{ refStats.collectionCount || 0 }}</div>
            <div class="stat-label">素材集收录数</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <el-card shadow="never" class="stat-item">
            <div class="stat-num" style="color: #e74c8a">{{ refStats.favoriteCount || 0 }}</div>
            <div class="stat-label">收藏人数</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <el-card shadow="never" class="stat-item">
            <div class="stat-num" style="color: #6b5bff">{{ material.downloadCount || refStats.downloadCount || 0 }}</div>
            <div class="stat-label">下载次数</div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <section class="page-card timeline-card">
      <div class="section-head">
        <h3>使用历史时间轴</h3>
      </div>
      <el-empty v-if="timeline.length === 0" description="暂无历史记录" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="(item, idx) in timeline"
          :key="idx"
          :timestamp="formatTime(item.timestamp)"
          :type="actionTypeColorMap[item.actionType] || ''"
          placement="top"
        >
          <div class="timeline-item-content">
            <div class="timeline-title">
              <el-tag size="small" :type="actionTypeColorMap[item.actionType] || 'info'" effect="plain">
                {{ item.actionType || '操作' }}
              </el-tag>
              <strong>{{ item.title || item.content || '操作事件' }}</strong>
            </div>
            <div class="timeline-meta" v-if="item.username || item.userId">
              <span>操作人：{{ item.username || item.userDisplayName || '系统' }}</span>
            </div>
            <div class="timeline-detail" v-if="item.detail || item.description">
              {{ item.detail || item.description }}
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </section>

    <section class="page-card tabs-card">
      <el-tabs type="border-card">
        <el-tab-pane label="项目引用" name="projects">
          <el-table :data="projectList" stripe v-if="projectList.length > 0">
            <el-table-column label="项目ID" prop="projectId" width="100" />
            <el-table-column label="项目名称" prop="projectName" min-width="200" />
            <el-table-column label="项目状态" prop="status" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : row.status === 'ARCHIVED' ? 'info' : 'warning'">
                  {{ row.status || '未知' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="添加人" prop="addedBy" width="140" />
            <el-table-column label="添加时间" prop="addedAt" width="180">
              <template #default="{ row }">{{ formatTime(row.addedAt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无项目引用" />
        </el-tab-pane>

        <el-tab-pane label="素材集引用" name="collections">
          <el-table :data="collectionList" stripe v-if="collectionList.length > 0">
            <el-table-column label="素材集ID" prop="collectionId" width="100" />
            <el-table-column label="素材集名称" prop="collectionName" min-width="200" />
            <el-table-column label="可见性" prop="visibility" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="row.visibility === 'PUBLIC' ? 'success' : 'info'">
                  {{ row.visibility === 'PUBLIC' ? '公开' : '私有' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="添加人" prop="addedBy" width="140" />
            <el-table-column label="添加时间" prop="addedAt" width="180">
              <template #default="{ row }">{{ formatTime(row.addedAt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无素材集引用" />
        </el-tab-pane>

        <el-tab-pane label="修改历史" name="mod-history">
          <div v-if="modHistory.length > 0" class="mod-history-list">
            <el-card
              v-for="(item, idx) in modHistory"
              :key="idx"
              shadow="never"
              class="mod-history-card"
            >
              <div class="mod-head">
                <strong>{{ item.fieldName || item.field || '未知字段' }}</strong>
                <el-tag size="small" type="info">
                  {{ formatTime(item.modifiedAt || item.createdAt) }}
                </el-tag>
                <span class="mod-user">修改人：{{ item.modifiedBy || item.username || '系统' }}</span>
              </div>
              <div class="mod-body">
                <div class="mod-old">
                  <div class="mod-label">变更前</div>
                  <div class="mod-value">{{ item.oldValue || item.before || '(空)' }}</div>
                </div>
                <div class="mod-arrow">&rarr;</div>
                <div class="mod-new">
                  <div class="mod-label">变更后</div>
                  <div class="mod-value">{{ item.newValue || item.after || '(空)' }}</div>
                </div>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无修改历史" />
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="editDialog" title="编辑素材" width="min(680px, 92vw)">
      <el-form label-position="top" class="grid-2 dialog-grid">
        <el-form-item label="标题"><el-input v-model="editForm.title" /></el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="editForm.visibility">
            <el-option v-for="item in visibilityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editForm.categoryId" clearable>
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="时长(秒)"><el-input-number v-model="editForm.durationSeconds" :min="0" controls-position="right" /></el-form-item>
        <el-form-item label="分辨率"><el-input v-model="editForm.resolution" /></el-form-item>
        <el-form-item label="标签">
          <el-select v-model="editForm.tagIds" multiple clearable>
            <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" style="grid-column: 1 / -1">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="loading">保存变更</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="deleteDialog"
      title="删除影响分析与确认"
      width="min(680px, 92vw)"
      :close-on-click-modal="false"
    >
      <div v-loading="deleteLoading" class="delete-content">
        <el-alert
          title="删除操作将影响以下范围，请谨慎操作"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 1rem"
        />
        <el-row :gutter="16" class="impact-row">
          <el-col :span="8">
            <el-card shadow="never" class="impact-item">
              <div class="impact-num">{{ deleteImpact.projectCount || 0 }}</div>
              <div class="impact-label">影响项目数</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="impact-item">
              <div class="impact-num">{{ deleteImpact.collectionCount || 0 }}</div>
              <div class="impact-label">影响素材集数</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="impact-item">
              <div class="impact-num">{{ deleteImpact.favoriteCount || 0 }}</div>
              <div class="impact-label">影响收藏数</div>
            </el-card>
          </el-col>
        </el-row>

        <div v-if="deleteImpact.affectedProjects && deleteImpact.affectedProjects.length > 0" class="affected-list">
          <h4>受影响的项目列表：</h4>
          <el-table :data="deleteImpact.affectedProjects" size="small" stripe max-height="180">
            <el-table-column label="项目ID" prop="projectId" width="100" />
            <el-table-column label="项目名称" prop="projectName" />
            <el-table-column label="负责人" prop="owner" width="120" />
          </el-table>
        </div>

        <div class="strategy-section">
          <h4>请选择删除策略：</h4>
          <el-radio-group v-model="deleteStrategy">
            <div v-for="opt in deleteStrategyOptions" :key="opt.value" class="strategy-option">
              <el-radio :value="opt.value" :label="opt.value">
                <span class="strategy-label">{{ opt.label }}</span>
              </el-radio>
            </div>
          </el-radio-group>
        </div>
      </div>
      <template #footer>
        <el-button @click="deleteDialog = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete" :loading="deleteLoading">确认删除</el-button>
      </template>
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
  padding: 1rem 1.2rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  flex-wrap: wrap;
  min-width: 0;
}

.toolbar-left {
  flex: 1;
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

.detail-grid {
  display: grid;
  grid-template-columns: 40% 60%;
  gap: 1rem;
  min-width: 0;
}

.preview-col,
.info-card {
  padding: 1rem 1.2rem;
  min-width: 0;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.section-head h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
}

.preview-box {
  min-height: 320px;
  display: grid;
  place-items: center;
  background: rgba(220, 232, 220, 0.35);
  border-radius: 14px;
  padding: 1rem;
}

.preview-media {
  max-width: 100%;
  max-height: 420px;
  border-radius: 14px;
}

.preview-audio {
  width: 100%;
}

.info-card :deep(.el-descriptions) {
  width: 100%;
}

.empty-text {
  color: var(--text-secondary);
}

.stats-card,
.timeline-card,
.tabs-card {
  padding: 1rem 1.2rem;
  min-width: 0;
}

.stat-item {
  text-align: center;
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
}

.stat-num {
  font-size: 2rem;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 0.4rem;
}

.stat-label {
  color: var(--text-secondary);
  font-size: 0.88rem;
}

.timeline-item-content {
  padding: 0.5rem 0;
}

.timeline-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.3rem;
  flex-wrap: wrap;
}

.timeline-meta {
  color: var(--text-secondary);
  font-size: 0.84rem;
  margin-bottom: 0.2rem;
}

.timeline-detail {
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.tabs-card :deep(.el-tabs__content) {
  padding-top: 0.5rem;
}

.mod-history-list {
  display: grid;
  gap: 0.75rem;
}

.mod-history-card {
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
}

.mod-head {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.mod-user {
  color: var(--text-secondary);
  font-size: 0.84rem;
}

.mod-body {
  display: grid;
  grid-template-columns: 1fr 40px 1fr;
  gap: 0.75rem;
  align-items: center;
}

.mod-old,
.mod-new {
  padding: 0.75rem;
  border-radius: 10px;
  background: var(--bg-soft);
}

.mod-new {
  background: rgba(26, 143, 91, 0.1);
}

.mod-label {
  font-size: 0.8rem;
  color: var(--text-secondary);
  margin-bottom: 0.25rem;
}

.mod-value {
  font-size: 0.92rem;
  word-break: break-word;
}

.mod-arrow {
  text-align: center;
  font-size: 1.4rem;
  color: var(--text-secondary);
}

.dialog-grid :deep(.el-select),
.dialog-grid :deep(.el-date-editor),
.dialog-grid :deep(.el-input-number) {
  width: 100%;
}

.impact-row {
  margin-bottom: 1rem;
}

.impact-item {
  text-align: center;
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
}

.impact-num {
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--accent-warning);
  margin-bottom: 0.25rem;
}

.impact-label {
  color: var(--text-secondary);
  font-size: 0.84rem;
}

.affected-list {
  margin-bottom: 1rem;
}

.affected-list h4 {
  margin: 0 0 0.5rem;
  font-size: 0.94rem;
}

.strategy-section h4 {
  margin: 0 0 0.5rem;
  font-size: 0.94rem;
}

.strategy-option {
  padding: 0.35rem 0;
}

.strategy-label {
  font-size: 0.9rem;
}

@media (max-width: 1100px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .preview-box {
    min-height: 260px;
  }
}

@media (max-width: 700px) {
  .mod-body {
    grid-template-columns: 1fr;
  }

  .mod-arrow {
    transform: rotate(90deg);
  }

  .toolbar {
    padding: 1rem;
  }

  .toolbar-actions {
    width: 100%;
  }

  .toolbar-actions .el-button {
    flex: 1;
    min-width: 0;
  }
}
</style>
