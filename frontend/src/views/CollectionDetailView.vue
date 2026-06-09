<script setup>
import { onMounted, reactive, ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '../api/http';

const route = useRoute();
const router = useRouter();
const collectionId = computed(() => route.params.id);
const loading = ref(false);
const collection = ref({});
const materials = ref([]);

const filterForm = reactive({
  type: '',
  keyword: '',
  sortBy: 'custom'
});

const noteDialog = ref(false);
const noteForm = reactive({
  materialId: null,
  note: ''
});

const previewDialog = ref(false);
const previewState = reactive({ url: '', type: '' });

const typeOptions = [
  { label: '全部', value: '' },
  { label: '视频', value: 'VIDEO' },
  { label: '音频', value: 'AUDIO' },
  { label: '图片', value: 'IMAGE' },
  { label: '模板', value: 'TEMPLATE' }
];

const filteredMaterials = computed(() => {
  let list = [...materials.value];
  if (filterForm.type) {
    list = list.filter(m => m.type === filterForm.type);
  }
  if (filterForm.keyword) {
    const kw = filterForm.keyword.toLowerCase();
    list = list.filter(m => m.title?.toLowerCase().includes(kw));
  }
  if (filterForm.sortBy === 'name') {
    list.sort((a, b) => (a.title || '').localeCompare(b.title || ''));
  } else if (filterForm.sortBy === 'size') {
    list.sort((a, b) => (b.sizeBytes || 0) - (a.sizeBytes || 0));
  } else if (filterForm.sortBy === 'time') {
    list.sort((a, b) => new Date(b.addedAt || 0) - new Date(a.addedAt || 0));
  }
  return list;
});

const loadCollection = async () => {
  loading.value = true;
  try {
    const res = await http.get(`/collections/${collectionId.value}`);
    collection.value = res?.data || {};
    materials.value = (res?.data?.materials || []).map(m => ({
      ...m,
      _dragging: false
    }));
  } finally {
    loading.value = false;
  }
};

const removeMaterial = async (materialId) => {
  await ElMessageBox.confirm('确定从该素材集中移除此素材？其他素材集不受影响', '确认操作', { type: 'warning' });
  await http.delete(`/collections/${collectionId.value}/materials/${materialId}`);
  ElMessage.success('素材已从素材集移除');
  await loadCollection();
};

const openNoteDialog = (material) => {
  noteForm.materialId = material.id;
  noteForm.note = material.note || '';
  noteDialog.value = true;
};

const submitNote = async () => {
  await http.put(`/collections/${collectionId.value}/materials/${noteForm.materialId}/note`, {
    note: noteForm.note
  });
  ElMessage.success('备注已更新');
  noteDialog.value = false;
  await loadCollection();
};

const onDragEnd = async () => {
  const items = filteredMaterials.value.map((m, idx) => ({
    materialId: m.id,
    sortOrder: idx
  }));
  await http.put(`/collections/${collectionId.value}/materials/sort`, { items });
  ElMessage.success('排序已保存');
};

const fetchBlobWithAuth = async (url) => {
  const token = localStorage.getItem('clip_hub_token');
  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` }
  });
  if (!response.ok) throw new Error('文件读取失败');
  return response.blob();
};

const previewMaterial = async (row) => {
  try {
    const blob = await fetchBlobWithAuth(`/api/materials/${row.id}/preview`);
    previewState.url = URL.createObjectURL(blob);
    previewState.type = row.type;
    previewDialog.value = true;
  } catch {
    ElMessage.error('预览加载失败');
  }
};

const goBack = () => {
  router.push('/collections');
};

const formatSize = (bytes) => {
  if (!bytes) return '-';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1048576).toFixed(1) + ' MB';
};

const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN') : '-';

onMounted(loadCollection);
</script>

<template>
  <div class="detail-wrap">
    <section class="page-card toolbar">
      <div>
        <div class="back-row">
          <el-button text @click="goBack">&larr; 返回素材集列表</el-button>
        </div>
        <h2 class="section-title">{{ collection.name || '素材集详情' }}</h2>
        <p class="section-subtitle">{{ collection.description || '暂无描述' }}</p>
        <div class="meta-row">
          <el-tag :type="collection.visibility === 'PUBLIC' ? 'success' : 'info'" size="small">
            {{ collection.visibility === 'PUBLIC' ? '公开' : '私有' }}
          </el-tag>
          <span>素材数：{{ materials.length }}</span>
          <span>更新时间：{{ formatTime(collection.updatedAt) }}</span>
          <span>创建时间：{{ formatTime(collection.createdAt) }}</span>
        </div>
      </div>
      <div class="toolbar-actions">
        <el-button plain @click="loadCollection">刷新</el-button>
      </div>
    </section>

    <section class="page-card filter-panel">
      <el-form inline class="filter-form">
        <el-form-item>
          <el-input v-model="filterForm.keyword" placeholder="搜索素材名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-select v-model="filterForm.type" placeholder="素材类型" style="width: 130px">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="filterForm.sortBy" placeholder="排序方式" style="width: 140px">
            <el-option label="自定义排序" value="custom" />
            <el-option label="添加时间" value="time" />
            <el-option label="名称排序" value="name" />
            <el-option label="文件大小" value="size" />
          </el-select>
        </el-form-item>
      </el-form>
    </section>

    <section class="page-card table-panel" v-loading="loading">
      <el-empty v-if="filteredMaterials.length === 0" description="该素材集中暂无素材" />
      <el-table v-else :data="filteredMaterials" stripe row-key="id">
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="素材标题" min-width="200">
          <template #default="{ row }">
            <div class="title-block">
              <strong :class="{ invalid: row.invalid }">{{ row.title }}</strong>
              <span v-if="row.invalid" class="invalid-tag">（已失效）</span>
              <span v-if="row.note" class="note-hint">备注：{{ row.note }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="type" width="90" />
        <el-table-column label="大小" width="110">
          <template #default="{ row }">{{ formatSize(row.sizeBytes) }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="160">
          <template #default="{ row }">
            <span v-if="row.note" class="note-text">{{ row.note }}</span>
            <span v-else class="note-empty">无备注</span>
          </template>
        </el-table-column>
        <el-table-column label="添加时间" width="160">
          <template #default="{ row }">{{ formatTime(row.addedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <el-space wrap>
              <el-button size="small" plain v-if="!row.invalid" @click="previewMaterial(row)">预览</el-button>
              <el-button size="small" plain @click="openNoteDialog(row)">备注</el-button>
              <el-button size="small" type="danger" plain @click="removeMaterial(row.id)">移除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="noteDialog" title="编辑备注" width="min(480px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="备注说明（仅在当前素材集内生效）">
          <el-input v-model="noteForm.note" type="textarea" :rows="4" placeholder="例如：这个视频的第30秒片段可用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noteDialog = false">取消</el-button>
        <el-button type="primary" @click="submitNote">保存备注</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewDialog" title="素材预览" width="min(760px, 92vw)" @closed="previewState.url = ''">
      <div class="preview-wrap">
        <img v-if="previewState.type === 'IMAGE'" :src="previewState.url" alt="预览" class="preview-image" />
        <video v-else-if="previewState.type === 'VIDEO'" :src="previewState.url" controls class="preview-video" />
        <audio v-else-if="previewState.type === 'AUDIO'" :src="previewState.url" controls style="width: 100%" />
        <el-input v-else type="textarea" :rows="10" :model-value="previewState.url" disabled />
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

.meta-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.5rem;
  flex-wrap: wrap;
  color: var(--text-secondary);
  font-size: 0.84rem;
}

.filter-panel {
  padding: 1rem;
}

.filter-panel :deep(.filter-form.el-form--inline) {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
}

.filter-panel :deep(.filter-form .el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}

.table-panel {
  padding: 1rem;
  overflow: hidden;
}

.table-panel :deep(.el-table) {
  width: 100%;
}

.title-block {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.invalid {
  color: #999;
  text-decoration: line-through;
}

.invalid-tag {
  color: #e6a23c;
  font-size: 0.82rem;
}

.note-hint {
  color: var(--accent-main);
  font-size: 0.82rem;
}

.note-text {
  font-size: 0.86rem;
  color: var(--text-secondary);
}

.note-empty {
  color: #c0c4cc;
  font-size: 0.84rem;
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

@media (max-width: 980px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-panel :deep(.filter-form.el-form--inline) {
    flex-direction: column;
  }
}
</style>
