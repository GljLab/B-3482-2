<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import http from '../api/http';

const loading = ref(false);
const projects = ref([]);
const materials = ref([]);
const currentVersions = ref([]);

const createDialog = ref(false);
const versionDialog = ref(false);
const collaboratorDialog = ref(false);
const versionsDrawer = ref(false);

const createForm = reactive({
  name: '',
  description: '',
  teamId: undefined,
  exportFormat: 'mp4',
  materialIds: []
});

const versionForm = reactive({
  projectId: null,
  versionName: '',
  contentJson: '{"timeline":[],"tracks":[]}'
});

const collaboratorForm = reactive({
  projectId: null,
  userId: undefined,
  role: 'EDITOR'
});

const loadMaterials = async () => {
  const res = await http.get('/materials', { params: { page: 1, size: 200 } });
  materials.value = res?.data || [];
};

const collectionList = ref([]);

const loadCollections = async () => {
  const res = await http.get('/collections');
  collectionList.value = res?.data || [];
};

const importFromCollection = async (projectId, collectionId) => {
  const res = await http.post(`/projects/${projectId}/import-collection/${collectionId}`);
  ElMessage.success(`成功导入 ${res?.data?.boundCount || 0} 个素材`);
  await loadProjects();
};

const loadProjects = async () => {
  loading.value = true;
  try {
    const res = await http.get('/projects');
    projects.value = res?.data || [];
  } finally {
    loading.value = false;
  }
};

const submitCreate = async () => {
  loading.value = true;
  try {
    await http.post('/projects', createForm);
    ElMessage.success('项目创建成功');
    createDialog.value = false;
    createForm.name = '';
    createForm.description = '';
    createForm.teamId = undefined;
    createForm.exportFormat = 'mp4';
    createForm.materialIds = [];
    await loadProjects();
  } finally {
    loading.value = false;
  }
};

const openVersionDialog = (projectId) => {
  versionForm.projectId = projectId;
  versionForm.versionName = '';
  versionForm.contentJson = '{"timeline":[],"tracks":[]}';
  versionDialog.value = true;
};

const submitVersion = async () => {
  loading.value = true;
  try {
    await http.post(`/projects/${versionForm.projectId}/versions`, {
      versionName: versionForm.versionName,
      contentJson: versionForm.contentJson
    });
    ElMessage.success('版本已保存');
    versionDialog.value = false;
    await loadProjects();
  } finally {
    loading.value = false;
  }
};

const openVersions = async (projectId) => {
  loading.value = true;
  try {
    const res = await http.get(`/projects/${projectId}/versions`);
    currentVersions.value = res?.data || [];
    versionForm.projectId = projectId;
    versionsDrawer.value = true;
  } finally {
    loading.value = false;
  }
};

const rollbackVersion = async (versionId) => {
  await http.post(`/projects/${versionForm.projectId}/rollback/${versionId}`);
  ElMessage.success('已回滚到目标版本');
  await openVersions(versionForm.projectId);
  await loadProjects();
};

const exportProject = async (project) => {
  const res = await http.post(`/projects/${project.id}/export`, null, { params: { format: project.exportFormat || 'mp4' } });
  const downloadUrl = res?.data?.downloadUrl;
  if (!downloadUrl) {
    ElMessage.success('导出任务已完成');
    return;
  }

  const token = localStorage.getItem('clip_hub_token');
  const response = await fetch(downloadUrl, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  });

  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `project-${project.id}.${project.exportFormat || 'mp4'}`;
  a.click();
  URL.revokeObjectURL(url);
  ElMessage.success('项目导出成功');
};

const openCollaboratorDialog = (projectId) => {
  collaboratorForm.projectId = projectId;
  collaboratorForm.userId = undefined;
  collaboratorForm.role = 'EDITOR';
  collaboratorDialog.value = true;
};

const submitCollaborator = async () => {
  await http.post(`/projects/${collaboratorForm.projectId}/collaborators`, {
    userId: collaboratorForm.userId,
    role: collaboratorForm.role
  });
  collaboratorDialog.value = false;
  ElMessage.success('协作成员已更新');
  await loadProjects();
};

const bindMaterial = async (projectId, materialId) => {
  await http.post(`/projects/${projectId}/materials/${materialId}`);
  ElMessage.success('素材绑定成功');
  await loadProjects();
};

onMounted(async () => {
  await Promise.all([loadProjects(), loadMaterials(), loadCollections()]);
});
</script>

<template>
  <div class="projects-wrap">
    <section class="page-card toolbar">
      <div>
        <h2 class="section-title">项目管理工坊</h2>
        <p class="section-subtitle">创建剪辑项目，绑定素材，管理版本，支持导出与协作</p>
      </div>
      <div class="toolbar-actions">
        <el-button type="primary" @click="createDialog = true">新建项目</el-button>
        <el-button plain @click="loadProjects">刷新</el-button>
      </div>
    </section>

    <section class="page-card card-grid">
      <el-empty v-if="projects.length === 0" description="暂无项目，请先创建" />
      <article v-else v-for="item in projects" :key="item.id" class="project-card">
        <header>
          <h3>{{ item.name }}</h3>
          <el-tag :type="item.status === 'ACTIVE' ? 'success' : 'info'">{{ item.status }}</el-tag>
        </header>
        <p>{{ item.description || '暂无项目描述' }}</p>
        <div class="meta-row">
          <span>素材数：{{ item.materials?.length || 0 }}</span>
          <span>导出格式：{{ item.exportFormat }}</span>
        </div>
        <div class="meta-row">
          <span>协作成员：{{ item.collaborators?.length || 0 }}</span>
          <span>当前版本ID：{{ item.currentVersionId || '-' }}</span>
        </div>
        <footer>
          <el-space wrap>
            <el-button size="small" plain @click="openVersionDialog(item.id)">保存版本</el-button>
            <el-button size="small" plain @click="openVersions(item.id)">版本列表</el-button>
            <el-button size="small" plain @click="exportProject(item)">导出</el-button>
            <el-button size="small" plain @click="openCollaboratorDialog(item.id)">协作设置</el-button>
            <el-popover placement="top" trigger="click" width="280">
              <template #reference>
                <el-button size="small" plain>绑定素材</el-button>
              </template>
              <el-select placeholder="选择素材" style="width: 100%" @change="(id) => bindMaterial(item.id, id)">
                <el-option v-for="material in materials" :key="material.id" :label="material.title" :value="material.id" />
              </el-select>
            </el-popover>
            <el-popover placement="top" trigger="click" width="280">
              <template #reference>
                <el-button size="small" plain>从素材集导入</el-button>
              </template>
              <el-select placeholder="选择素材集" style="width: 100%" @change="(colId) => importFromCollection(item.id, colId)">
                <el-option v-for="col in collectionList" :key="col.id" :label="`${col.name} (${col.materialCount}个素材)`" :value="col.id" />
              </el-select>
            </el-popover>
          </el-space>
        </footer>
      </article>
    </section>

    <el-dialog v-model="createDialog" title="新建项目" width="min(760px, 92vw)">
      <el-form label-position="top" class="grid-2">
        <el-form-item label="项目名称"><el-input v-model="createForm.name" /></el-form-item>
        <el-form-item label="导出格式">
          <el-select v-model="createForm.exportFormat">
            <el-option label="MP4" value="mp4" />
            <el-option label="MOV" value="mov" />
            <el-option label="WEBM" value="webm" />
          </el-select>
        </el-form-item>
        <el-form-item label="团队ID"><el-input-number v-model="createForm.teamId" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="绑定素材" style="grid-column: 1 / -1">
          <el-select v-model="createForm.materialIds" multiple clearable>
            <el-option v-for="material in materials" :key="material.id" :label="material.title" :value="material.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目描述" style="grid-column: 1 / -1">
          <el-input type="textarea" :rows="4" v-model="createForm.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitCreate">创建项目</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="versionDialog" title="保存项目版本" width="min(760px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="版本名称"><el-input v-model="versionForm.versionName" /></el-form-item>
        <el-form-item label="版本内容(JSON)">
          <el-input type="textarea" :rows="10" v-model="versionForm.contentJson" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="versionDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitVersion">保存版本</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="collaboratorDialog" title="协作成员管理" width="min(500px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="用户ID"><el-input-number v-model="collaboratorForm.userId" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="权限角色">
          <el-select v-model="collaboratorForm.role">
            <el-option label="编辑者" value="EDITOR" />
            <el-option label="查看者" value="VIEWER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="collaboratorDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCollaborator">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="versionsDrawer" title="版本历史" size="min(520px, 92vw)">
      <el-timeline>
        <el-timeline-item
          v-for="version in currentVersions"
          :key="version.id"
          :timestamp="version.createdAt"
          :type="version.isCurrent ? 'primary' : 'info'"
        >
          <div class="version-item">
            <strong>{{ version.versionName }}</strong>
            <span>序号 v{{ version.versionNo }}</span>
            <el-button size="small" plain @click="rollbackVersion(version.id)">回滚到该版本</el-button>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>

<style scoped>
.projects-wrap {
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

.card-grid {
  padding: 1rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.project-card {
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
  padding: 1rem;
  display: grid;
  gap: 0.8rem;
  min-width: 0;
}

.project-card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.project-card h3 {
  margin: 0;
}

.project-card p {
  margin: 0;
  color: var(--text-secondary);
}

.meta-row {
  display: flex;
  justify-content: space-between;
  gap: 0.6rem;
  flex-wrap: wrap;
  color: var(--text-secondary);
  font-size: 0.83rem;
}

.version-item {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

@media (max-width: 980px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
