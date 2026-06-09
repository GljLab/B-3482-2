<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '../api/http';

const router = useRouter();
const loading = ref(false);
const collections = ref([]);

const searchForm = reactive({
  keyword: '',
  sortBy: '',
  visibility: ''
});

const createDialog = ref(false);
const editDialog = ref(false);
const deleteDialog = ref(false);
const shareDrawer = ref(false);

const createForm = reactive({
  name: '',
  description: '',
  coverMaterialId: undefined,
  visibility: 'PRIVATE'
});

const editForm = reactive({
  id: null,
  name: '',
  description: '',
  coverMaterialId: undefined,
  visibility: 'PRIVATE'
});

const deleteTarget = reactive({
  id: null,
  name: '',
  removeFavorites: false
});

const shareTarget = ref(null);
const shares = ref([]);
const shareForm = reactive({
  expireDays: 7,
  password: ''
});
const shareDialog = ref(false);

const visibilityOptions = [
  { label: '私有', value: 'PRIVATE' },
  { label: '公开', value: 'PUBLIC' }
];

const sortOptions = [
  { label: '最近更新', value: '' },
  { label: '名称排序', value: 'name' },
  { label: '素材数量', value: 'materialCount' }
];

const loadCollections = async () => {
  loading.value = true;
  try {
    const params = {};
    if (searchForm.keyword) params.keyword = searchForm.keyword;
    if (searchForm.sortBy) params.sortBy = searchForm.sortBy;
    if (searchForm.visibility) params.visibility = searchForm.visibility;
    const res = await http.get('/collections', { params });
    collections.value = res?.data || [];
  } finally {
    loading.value = false;
  }
};

const openCreateDialog = () => {
  createForm.name = '';
  createForm.description = '';
  createForm.coverMaterialId = undefined;
  createForm.visibility = 'PRIVATE';
  createDialog.value = true;
};

const submitCreate = async () => {
  if (!createForm.name.trim()) {
    ElMessage.warning('素材集名称不能为空');
    return;
  }
  loading.value = true;
  try {
    await http.post('/collections', createForm);
    ElMessage.success('素材集创建成功');
    createDialog.value = false;
    await loadCollections();
  } finally {
    loading.value = false;
  }
};

const openEdit = (row) => {
  editForm.id = row.id;
  editForm.name = row.name;
  editForm.description = row.description;
  editForm.coverMaterialId = row.coverMaterialId;
  editForm.visibility = row.visibility;
  editDialog.value = true;
};

const submitEdit = async () => {
  loading.value = true;
  try {
    await http.put(`/collections/${editForm.id}`, {
      name: editForm.name,
      description: editForm.description,
      coverMaterialId: editForm.coverMaterialId,
      visibility: editForm.visibility
    });
    ElMessage.success('素材集已更新');
    editDialog.value = false;
    await loadCollections();
  } finally {
    loading.value = false;
  }
};

const openDelete = (row) => {
  deleteTarget.id = row.id;
  deleteTarget.name = row.name;
  deleteTarget.removeFavorites = false;
  deleteDialog.value = true;
};

const confirmDelete = async () => {
  loading.value = true;
  try {
    await http.delete(`/collections/${deleteTarget.id}`, {
      params: { removeFavorites: deleteTarget.removeFavorites }
    });
    ElMessage.success('素材集已删除');
    deleteDialog.value = false;
    await loadCollections();
  } finally {
    loading.value = false;
  }
};

const openShare = async (row) => {
  shareTarget.value = row;
  shareDrawer.value = true;
  await loadShares(row.id);
};

const loadShares = async (collectionId) => {
  const res = await http.get(`/collections/${collectionId}/shares`);
  shares.value = res?.data || [];
};

const openShareDialog = () => {
  shareForm.expireDays = 7;
  shareForm.password = '';
  shareDialog.value = true;
};

const submitShare = async () => {
  loading.value = true;
  try {
    const res = await http.post(`/collections/${shareTarget.value.id}/share`, {
      expireDays: shareForm.expireDays,
      password: shareForm.password || undefined
    });
    shareDialog.value = false;
    const link = `${window.location.origin}${res?.data?.shareUrl || ''}`;
    try {
      await navigator.clipboard.writeText(link);
      ElMessage.success('分享链接已复制到剪贴板');
    } catch {
      await ElMessageBox.alert(link, '请手动复制以下链接', { confirmButtonText: '我知道了' });
    }
    await loadShares(shareTarget.value.id);
  } finally {
    loading.value = false;
  }
};

const revokeShare = async (shareId) => {
  await ElMessageBox.confirm('确定要使该分享链接失效吗？', '确认操作', { type: 'warning' });
  await http.delete(`/collections/shares/${shareId}`);
  ElMessage.success('分享链接已失效');
  await loadShares(shareTarget.value.id);
};

const goDetail = (id) => {
  router.push(`/collections/${id}`);
};

const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN') : '-';

onMounted(loadCollections);
</script>

<template>
  <div class="collections-wrap">
    <section class="page-card toolbar">
      <div>
        <h2 class="section-title">我的素材集</h2>
        <p class="section-subtitle">创建素材集分类整理收藏的素材，支持多素材集归属、排序备注与分享展示</p>
      </div>
      <div class="toolbar-actions">
        <el-button type="primary" @click="openCreateDialog">新建素材集</el-button>
        <el-button plain @click="loadCollections">刷新</el-button>
      </div>
    </section>

    <section class="page-card search-panel">
      <el-form inline class="search-form">
        <el-form-item>
          <el-input v-model="searchForm.keyword" placeholder="搜索素材集名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.visibility" placeholder="可见性" clearable style="width: 140px">
            <el-option v-for="item in visibilityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.sortBy" placeholder="排序方式" clearable style="width: 150px">
            <el-option v-for="item in sortOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="loadCollections">搜索</el-button>
          <el-button plain @click="searchForm.keyword = ''; searchForm.sortBy = ''; searchForm.visibility = ''; loadCollections()">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="page-card card-grid" v-loading="loading">
      <el-empty v-if="collections.length === 0" description="暂无素材集，点击上方按钮创建" />
      <article v-else v-for="item in collections" :key="item.id" class="collection-card" @click="goDetail(item.id)">
        <header>
          <div class="card-title-row">
            <h3>{{ item.name }}</h3>
            <el-tag v-if="item.isDefault" type="warning" size="small">默认</el-tag>
            <el-tag :type="item.visibility === 'PUBLIC' ? 'success' : 'info'" size="small">
              {{ item.visibility === 'PUBLIC' ? '公开' : '私有' }}
            </el-tag>
          </div>
        </header>
        <p class="card-desc">{{ item.description || '暂无描述' }}</p>
        <div class="card-meta">
          <span>素材数：{{ item.materialCount }}</span>
          <span>更新：{{ formatTime(item.updatedAt) }}</span>
        </div>
        <footer class="card-actions" @click.stop>
          <el-space wrap>
            <el-button size="small" plain @click="openEdit(item)">编辑</el-button>
            <el-button size="small" plain @click="openShare(item)">分享</el-button>
            <el-button size="small" type="danger" plain v-if="!item.isDefault" @click="openDelete(item)">删除</el-button>
          </el-space>
        </footer>
      </article>
    </section>

    <el-dialog v-model="createDialog" title="新建素材集" width="min(600px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="名称" required><el-input v-model="createForm.name" placeholder="请输入素材集名称" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="可选描述" /></el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="createForm.visibility">
            <el-option v-for="item in visibilityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialog" title="编辑素材集" width="min(600px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="editForm.visibility">
            <el-option v-for="item in visibilityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deleteDialog" title="删除素材集" width="min(500px, 92vw)">
      <p>确定要删除素材集「{{ deleteTarget.name }}」吗？</p>
      <el-checkbox v-model="deleteTarget.removeFavorites" style="margin-top: 1rem">
        同时取消其中素材的收藏状态
      </el-checkbox>
      <template #footer>
        <el-button @click="deleteDialog = false">取消</el-button>
        <el-button type="danger" :loading="loading" @click="confirmDelete">确认删除</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="shareDrawer" :title="`分享管理 - ${shareTarget?.name || ''}`" size="min(560px, 92vw)">
      <div style="margin-bottom: 1rem">
        <el-button type="primary" @click="openShareDialog">生成新分享链接</el-button>
      </div>
      <el-table :data="shares" stripe>
        <el-table-column label="分享码" prop="shareCode" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '有效' : '已失效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="密码保护" width="90">
          <template #default="{ row }">{{ row.hasPassword ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="访问次数" prop="accessCount" width="90" />
        <el-table-column label="过期时间" width="160">
          <template #default="{ row }">{{ row.expireAt || '永久' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button v-if="row.status === 'ACTIVE'" size="small" type="danger" plain @click="revokeShare(row.id)">失效</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog v-model="shareDialog" title="生成分享链接" width="min(480px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="有效期(天)">
          <el-select v-model="shareForm.expireDays">
            <el-option label="7天" :value="7" />
            <el-option label="30天" :value="30" />
            <el-option label="永久有效" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问密码(可选)">
          <el-input v-model="shareForm.password" placeholder="留空则无需密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shareDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitShare">生成链接</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.collections-wrap {
  display: grid;
  gap: 1rem;
  min-width: 0;
}

.toolbar {
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  min-width: 0;
}

.toolbar-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.search-panel {
  padding: 1rem;
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
.search-panel :deep(.search-form .el-select) {
  width: 100% !important;
}

.search-panel :deep(.search-actions .el-form-item__content) {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.card-grid {
  padding: 1rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.collection-card {
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
  padding: 1rem;
  display: grid;
  gap: 0.6rem;
  min-width: 0;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.15s;
}

.collection-card:hover {
  box-shadow: 0 8px 24px rgba(31, 64, 47, 0.14);
  transform: translateY(-2px);
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.card-title-row h3 {
  margin: 0;
  flex: 1;
}

.card-desc {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  gap: 0.6rem;
  flex-wrap: wrap;
  color: var(--text-secondary);
  font-size: 0.82rem;
}

@media (max-width: 980px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .card-grid {
    grid-template-columns: 1fr;
  }

  .search-panel :deep(.search-form.el-form--inline) {
    grid-template-columns: 1fr;
  }
}
</style>
