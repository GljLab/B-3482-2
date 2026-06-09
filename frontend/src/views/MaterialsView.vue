<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import dayjs from 'dayjs';
import { ElMessage, ElMessageBox } from 'element-plus';
import { UploadFilled, Film, Collection } from '@element-plus/icons-vue';
import http from '../api/http';
import traceApi from '../api/traceability';

const router = useRouter();

const loading = ref(false);
const materials = ref([]);
const categories = ref([]);
const tags = ref([]);

const searchForm = reactive({
  keyword: '',
  categoryId: undefined,
  tagId: undefined,
  type: '',
  dateRange: [],
  inMyCollections: false,
  collectionId: undefined,
  page: 1,
  size: 10
});

const uploadDialog = ref(false);
const editDialog = ref(false);
const previewDialog = ref(false);

const uploadForm = reactive({
  title: '',
  description: '',
  type: 'VIDEO',
  categoryId: undefined,
  visibility: 'PUBLIC',
  durationSeconds: undefined,
  resolution: '',
  tagIds: []
});

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

const uploadFile = ref(null);
const uploadRef = ref(null);
const uploadFileList = ref([]);

const previewState = reactive({
  url: '',
  type: ''
});

const typeOptions = [
  { label: '视频', value: 'VIDEO' },
  { label: '音频', value: 'AUDIO' },
  { label: '图片', value: 'IMAGE' },
  { label: '模板', value: 'TEMPLATE' }
];

const visibilityOptions = [
  { label: '公开', value: 'PUBLIC' },
  { label: '私有', value: 'PRIVATE' },
  { label: '团队可见', value: 'TEAM' }
];

const mapSearchParams = () => {
  const params = {
    keyword: searchForm.keyword || undefined,
    categoryId: searchForm.categoryId || undefined,
    tagId: searchForm.tagId || undefined,
    type: searchForm.type || undefined,
    inMyCollections: searchForm.inMyCollections || undefined,
    collectionId: searchForm.collectionId || undefined,
    page: searchForm.page,
    size: searchForm.size
  };

  if (Array.isArray(searchForm.dateRange) && searchForm.dateRange.length === 2) {
    params.startAt = dayjs(searchForm.dateRange[0]).format('YYYY-MM-DDTHH:mm:ss');
    params.endAt = dayjs(searchForm.dateRange[1]).format('YYYY-MM-DDTHH:mm:ss');
  }

  return params;
};

const loadTaxonomy = async () => {
  const [categoryRes, tagRes] = await Promise.all([
    http.get('/categories'),
    http.get('/tags')
  ]);
  categories.value = categoryRes?.data || [];
  tags.value = tagRes?.data || [];
  await loadCollections();
};

const loadMaterials = async () => {
  loading.value = true;
  try {
    const res = await http.get('/materials', { params: mapSearchParams() });
    materials.value = res?.data || [];
  } finally {
    loading.value = false;
  }
};

const handleUploadChange = (file, files) => {
  const latestFile = files[files.length - 1] || file;
  uploadFileList.value = latestFile ? [latestFile] : [];
  uploadFile.value = latestFile?.raw || null;
};

const handleUploadRemove = () => {
  uploadFileList.value = [];
  uploadFile.value = null;
};

const resetUploadForm = () => {
  uploadForm.title = '';
  uploadForm.description = '';
  uploadForm.type = 'VIDEO';
  uploadForm.categoryId = undefined;
  uploadForm.visibility = 'PUBLIC';
  uploadForm.durationSeconds = undefined;
  uploadForm.resolution = '';
  uploadForm.tagIds = [];
  uploadFileList.value = [];
  uploadFile.value = null;
  uploadRef.value?.clearFiles();
};

const openUploadDialog = () => {
  resetUploadForm();
  uploadDialog.value = true;
};

const closeUploadDialog = () => {
  uploadDialog.value = false;
  resetUploadForm();
};

const submitUpload = async () => {
  if (!uploadFile.value) {
    ElMessage.warning('请先选择上传文件');
    return;
  }

  const formData = new FormData();
  Object.entries(uploadForm).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      value.forEach((item) => formData.append(key, item));
      return;
    }
    if (value !== undefined && value !== null && value !== '') {
      formData.append(key, value);
    }
  });
  formData.append('file', uploadFile.value);

  loading.value = true;
  try {
    await http.post('/materials/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    ElMessage.success('素材上传成功');
    uploadDialog.value = false;
    resetUploadForm();
    await loadMaterials();
  } finally {
    loading.value = false;
  }
};

const openEdit = (row) => {
  editForm.id = row.id;
  editForm.title = row.title;
  editForm.description = row.description;
  editForm.visibility = row.visibility;
  editForm.categoryId = row.category?.id;
  editForm.durationSeconds = row.durationSeconds;
  editForm.resolution = row.resolution;
  editForm.tagIds = (row.tags || []).map((tag) => tag.id);
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
    await loadMaterials();
  } finally {
    loading.value = false;
  }
};

const deleteImpactDialog = ref(false);
const deleteImpactData = ref(null);
const deleteTargetId = ref(null);
const deleteStrategy = ref('RECYCLE');
const deleteNotify = ref(false);

const openDetail = (row) => {
  router.push(`/materials/${row.id}/detail`);
};

const safeDeleteMaterial = async (row) => {
  deleteTargetId.value = row.id;
  try {
    const res = await traceApi.getDeleteImpact(row.id);
    deleteImpactData.value = res?.data;
    deleteImpactDialog.value = true;
    deleteStrategy.value = 'RECYCLE';
    deleteNotify.value = false;
  } catch (e) {
    await ElMessageBox.confirm('获取影响范围失败，将直接删除。是否继续？', '删除确认', { type: 'warning' });
    await http.delete(`/materials/${deleteTargetId.value}?force=true`);
    ElMessage.success('素材已删除');
    await loadMaterials();
  }
};

const confirmDeleteWithStrategy = async () => {
  try {
    await traceApi.deleteWithStrategy(deleteTargetId.value, {
      strategy: deleteStrategy.value,
      notifyUsers: deleteNotify.value
    });
    ElMessage.success(deleteStrategy.value === 'FORCE_DELETE' ? '素材已永久删除' : '素材已移入回收站');
    deleteImpactDialog.value = false;
    await loadMaterials();
  } catch (e) {
    // error handled by interceptor
  }
};

const deleteMaterial = async (id) => {
  await ElMessageBox.confirm('删除后无法恢复，是否继续？', '删除确认', { type: 'warning' });
  await http.delete(`/materials/${id}?force=true`);
  ElMessage.success('素材已删除');
  await loadMaterials();
};

const toggleFavorite = async (id) => {
  const res = await http.post(`/materials/${id}/favorite`);
  ElMessage.success(res?.data?.favorited ? '已加入收藏' : '已取消收藏');
  await loadMaterials();
};

const collections = ref([]);
const collectionPanelVisible = ref(false);
const collectionPanelTarget = ref(null);
const collectionPanelSelections = ref([]);

const loadCollections = async () => {
  const res = await http.get('/collections');
  collections.value = res?.data || [];
};

const openCollectionPanel = async (material) => {
  collectionPanelTarget.value = material;
  collectionPanelVisible.value = true;
  await loadCollections();
  try {
    const res = await http.get(`/collections/materials/${material.id}/collections`);
    collectionPanelSelections.value = (res?.data || []).map(c => c.id);
  } catch {
    collectionPanelSelections.value = [];
  }
};

const toggleCollectionSelection = (collectionId) => {
  const idx = collectionPanelSelections.value.indexOf(collectionId);
  if (idx >= 0) {
    collectionPanelSelections.value.splice(idx, 1);
  } else {
    collectionPanelSelections.value.push(collectionId);
  }
};

const saveCollectionSelections = async () => {
  if (!collectionPanelTarget.value) return;
  await http.post(`/collections/materials/${collectionPanelTarget.value.id}/set-collections`, {
    collectionIds: collectionPanelSelections.value
  });
  ElMessage.success('素材集归属已更新');
  collectionPanelVisible.value = false;
  await loadMaterials();
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

const createShare = async (id) => {
  const res = await http.post(`/materials/${id}/share`, { expireHours: 48 });
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

const fetchBlobWithAuth = async (url) => {
  const token = localStorage.getItem('clip_hub_token');
  const response = await fetch(url, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  });

  if (!response.ok) {
    throw new Error('文件读取失败');
  }

  return response.blob();
};

const previewMaterial = async (row) => {
  const blob = await fetchBlobWithAuth(`/api/materials/${row.id}/preview`);
  previewState.url = URL.createObjectURL(blob);
  previewState.type = row.type;
  previewDialog.value = true;
};

const downloadMaterial = async (row) => {
  const blob = await fetchBlobWithAuth(`/api/materials/${row.id}/download?quality=source&format=${row.format || ''}`);
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = row.fileName || `${row.title}.${row.format || 'bin'}`;
  a.click();
  URL.revokeObjectURL(url);
};

const resetFilters = async () => {
  searchForm.keyword = '';
  searchForm.categoryId = undefined;
  searchForm.tagId = undefined;
  searchForm.type = '';
  searchForm.dateRange = [];
  searchForm.inMyCollections = false;
  searchForm.collectionId = undefined;
  searchForm.page = 1;
  await loadMaterials();
};

onMounted(async () => {
  await loadTaxonomy();
  await loadMaterials();
});
</script>

<template>
  <div class="materials-wrap">
    <section class="page-card toolbar">
      <div>
        <h2 class="section-title">素材管理中心</h2>
        <p class="section-subtitle">支持上传、检索、收藏、分享、预览与多格式下载</p>
      </div>
      <div class="toolbar-actions">
        <el-button type="primary" @click="openUploadDialog">上传素材</el-button>
        <el-button plain @click="loadMaterials">刷新</el-button>
      </div>
    </section>

    <section class="page-card search-panel">
      <el-form inline class="search-form">
        <el-form-item>
          <el-input v-model="searchForm.keyword" placeholder="关键词搜索标题或描述" clearable />
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.type" placeholder="素材类型" clearable style="width: 150px">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.categoryId" placeholder="分类" clearable style="width: 170px">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.tagId" placeholder="标签" clearable style="width: 160px">
            <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-date-picker
            v-model="searchForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="上传开始"
            end-placeholder="上传结束"
            style="width: 320px"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="searchForm.inMyCollections" label="仅显示素材集中的素材" />
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="searchForm.collectionId"
            placeholder="指定素材集"
            clearable
            :disabled="!searchForm.inMyCollections"
            style="width: 180px"
          >
            <el-option v-for="item in collections" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="loadMaterials">搜索</el-button>
          <el-button plain @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="page-card table-panel">
      <el-table :data="materials" v-loading="loading" stripe>
        <el-table-column label="素材ID" prop="id" width="90" />
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <div class="title-block">
              <strong>{{ row.title }}</strong>
              <span>{{ row.description || '暂无描述' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="type" width="100" />
        <el-table-column label="分类" width="130">
          <template #default="{ row }">{{ row.category?.name || '未分类' }}</template>
        </el-table-column>
        <el-table-column label="权限" prop="visibility" width="110" />
        <el-table-column label="统计" width="180">
          <template #default="{ row }">
            <div class="stat-inline">
              <span>下载 {{ row.downloadCount }}</span>
              <span>收藏 {{ row.favoriteCount }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag v-for="tag in row.tags" :key="tag.id" size="small">{{ tag.name }}</el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="320">
          <template #default="{ row }">
            <el-space wrap>
              <el-button size="small" type="primary" plain @click="openDetail(row)">详情溯源</el-button>
              <el-button size="small" plain @click="previewMaterial(row)">预览</el-button>
              <el-button size="small" plain @click="downloadMaterial(row)">下载</el-button>
              <el-button size="small" plain @click="toggleFavorite(row.id)">快速收藏</el-button>
              <el-button size="small" plain @click="openCollectionPanel(row)">加入素材集</el-button>
              <el-button size="small" plain @click="createShare(row.id)">分享</el-button>
              <el-button size="small" plain @click="openEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="safeDeleteMaterial(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="uploadDialog" title="上传素材" width="min(760px, 92vw)" @closed="resetUploadForm">
      <el-form label-position="top" class="grid-2 dialog-grid">
        <el-form-item label="标题"><el-input v-model="uploadForm.title" /></el-form-item>
        <el-form-item label="素材类型">
          <el-select v-model="uploadForm.type">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="uploadForm.categoryId" clearable>
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="uploadForm.visibility">
            <el-option v-for="item in visibilityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="时长(秒)"><el-input-number v-model="uploadForm.durationSeconds" :min="0" controls-position="right" /></el-form-item>
        <el-form-item label="分辨率"><el-input v-model="uploadForm.resolution" /></el-form-item>
        <el-form-item label="标签" style="grid-column: 1 / -1">
          <el-select v-model="uploadForm.tagIds" multiple clearable>
            <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" style="grid-column: 1 / -1">
          <el-input v-model="uploadForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="文件" style="grid-column: 1 / -1">
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :file-list="uploadFileList"
            :show-file-list="true"
            :limit="1"
            :on-change="handleUploadChange"
            :on-remove="handleUploadRemove"
          >
            <el-icon><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或点击选择文件</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeUploadDialog">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitUpload">确认上传</el-button>
      </template>
    </el-dialog>

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

    <el-dialog v-model="previewDialog" title="素材预览" width="min(760px, 92vw)" @closed="previewState.url = ''">
      <div class="preview-wrap">
        <img v-if="previewState.type === 'IMAGE'" :src="previewState.url" alt="素材预览" class="preview-image" />
        <video v-else-if="previewState.type === 'VIDEO'" :src="previewState.url" controls class="preview-video" />
        <audio v-else-if="previewState.type === 'AUDIO'" :src="previewState.url" controls style="width: 100%" />
        <el-input v-else type="textarea" :rows="10" :model-value="previewState.url" disabled />
      </div>
    </el-dialog>

    <el-dialog v-model="deleteImpactDialog" title="删除影响评估" width="min(680px, 92vw)">
      <div v-if="deleteImpactData" class="delete-impact-content">
        <el-alert
          :title="deleteImpactData.canSafelyDelete ? '该素材无外部引用，可以安全删除' : `该素材正在被 ${deleteImpactData.projectCount} 个项目、${deleteImpactData.collectionCount} 个素材集使用，被 ${deleteImpactData.favoriteCount} 人收藏`"
          :type="deleteImpactData.canSafelyDelete ? 'success' : (deleteImpactData.projectCount > 0 ? 'error' : 'warning')"
          :closable="false"
          show-icon
          style="margin-bottom: 1rem"
        />
        <el-descriptions :column="2" border size="small" style="margin-bottom: 1rem">
          <el-descriptions-item label="素材ID">{{ deleteImpactData.materialId }}</el-descriptions-item>
          <el-descriptions-item label="素材标题">{{ deleteImpactData.materialTitle }}</el-descriptions-item>
          <el-descriptions-item label="影响项目数" :content-style="{ color: deleteImpactData.projectCount > 0 ? '#f56c6c' : '' }">{{ deleteImpactData.projectCount }}</el-descriptions-item>
          <el-descriptions-item label="影响素材集数">{{ deleteImpactData.collectionCount }}</el-descriptions-item>
          <el-descriptions-item label="收藏用户数">{{ deleteImpactData.favoriteCount }}</el-descriptions-item>
          <el-descriptions-item label="受影响用户数">{{ deleteImpactData.affectedUserCount }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="deleteImpactData.affectedProjects && deleteImpactData.affectedProjects.length > 0" class="affected-list">
          <h4>受影响的项目（{{ deleteImpactData.affectedProjects.length }}）</h4>
          <div class="affected-scroll">
            <div v-for="p in deleteImpactData.affectedProjects" :key="p.projectId" class="affected-item">
              <el-icon><Film /></el-icon>
              <span class="affected-name">{{ p.projectName }}</span>
              <el-tag size="small">{{ p.ownerName }}</el-tag>
            </div>
          </div>
        </div>
        <div v-if="deleteImpactData.affectedCollections && deleteImpactData.affectedCollections.length > 0" class="affected-list">
          <h4>受影响的素材集（{{ deleteImpactData.affectedCollections.length }}）</h4>
          <div class="affected-scroll">
            <div v-for="c in deleteImpactData.affectedCollections" :key="c.collectionId" class="affected-item">
              <el-icon><Collection /></el-icon>
              <span class="affected-name">{{ c.collectionName }}</span>
              <el-tag size="small">{{ c.ownerName }}</el-tag>
            </div>
          </div>
        </div>
        <el-divider>删除策略</el-divider>
        <el-radio-group v-model="deleteStrategy">
          <el-radio v-for="s in deleteImpactData.availableStrategies" :key="s.strategy" :value="s.strategy" :border="true">
            {{ s.label }}
            <el-tag v-if="s.recommended" size="small" type="success" style="margin-left: 4px">推荐</el-tag>
          </el-radio>
        </el-radio-group>
        <div style="margin-top: 0.75rem">
          <el-checkbox v-model="deleteNotify">删除前通知受影响的协作者</el-checkbox>
        </div>
      </div>
      <template #footer>
        <el-button @click="deleteImpactDialog = false">取消</el-button>
        <el-button type="danger" :loading="loading" @click="confirmDeleteWithStrategy">确认删除</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="collectionPanelVisible" title="选择素材集" width="min(480px, 92vw)">
      <p style="margin-bottom: 1rem; color: var(--text-secondary); font-size: 0.9rem">
        为素材「{{ collectionPanelTarget?.title || '' }}」选择归属的素材集，勾选即为加入，取消勾选即为移除
      </p>
      <div class="collection-check-list">
        <div v-for="col in collections" :key="col.id" class="collection-check-item" @click="toggleCollectionSelection(col.id)">
          <el-checkbox :model-value="collectionPanelSelections.includes(col.id)" />
          <span class="collection-name">{{ col.name }}</span>
          <el-tag :type="col.visibility === 'PUBLIC' ? 'success' : 'info'" size="small">
            {{ col.visibility === 'PUBLIC' ? '公开' : '私有' }}
          </el-tag>
          <span class="collection-count">{{ col.materialCount }}个素材</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="collectionPanelVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCollectionSelections">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.materials-wrap {
  display: grid;
  gap: 1rem;
  min-width: 0;
}

.toolbar,
.search-panel,
.table-panel {
  padding: 1rem;
  min-width: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.toolbar-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.title-block {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.title-block strong {
  font-size: 0.94rem;
}

.title-block span {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.stat-inline {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  font-size: 0.82rem;
  color: var(--text-secondary);
}

.search-panel :deep(.search-form.el-form--inline) {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.75rem;
  align-items: end;
}

.search-panel :deep(.search-form .el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
  min-width: 0;
}

.search-panel :deep(.search-form .el-form-item__content) {
  width: 100%;
}

.search-panel :deep(.search-form .el-input),
.search-panel :deep(.search-form .el-select),
.search-panel :deep(.search-form .el-date-editor) {
  width: 100% !important;
}

.search-panel :deep(.search-actions .el-form-item__content) {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.table-panel {
  overflow: hidden;
}

.table-panel :deep(.el-table) {
  width: 100%;
}

.table-panel :deep(.el-table .cell) {
  word-break: break-word;
}

.dialog-grid :deep(.el-select),
.dialog-grid :deep(.el-date-editor),
.dialog-grid :deep(.el-input-number) {
  width: 100%;
}

.preview-wrap {
  min-height: 280px;
  display: grid;
  place-items: center;
}

.preview-image,
.preview-video {
  max-width: 100%;
  border-radius: 14px;
}

.collection-check-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 360px;
  overflow-y: auto;
}

.collection-check-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}

.collection-check-item:hover {
  background: rgba(26, 143, 91, 0.06);
}

.collection-name {
  flex: 1;
  font-weight: 500;
}

.collection-count {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.delete-impact-content h4 {
  margin: 0 0 0.5rem;
  font-size: 0.95rem;
  color: var(--text-primary);
}

.affected-list {
  margin-bottom: 1rem;
}

.affected-scroll {
  max-height: 160px;
  overflow-y: auto;
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 0.5rem;
  background: #fafafa;
}

.affected-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.4rem;
  border-radius: 6px;
}

.affected-item:hover {
  background: rgba(26, 143, 91, 0.08);
}

.affected-item + .affected-item {
  margin-top: 4px;
}

.affected-name {
  flex: 1;
  font-size: 0.88rem;
}

@media (max-width: 980px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-panel :deep(.search-form.el-form--inline) {
    grid-template-columns: 1fr;
  }

  .search-panel :deep(.search-actions .el-form-item__content) {
    justify-content: flex-start;
  }
}
</style>
