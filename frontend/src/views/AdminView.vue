<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import http from '../api/http';

const loading = ref(false);
const users = ref([]);
const categories = ref([]);
const tags = ref([]);
const settings = ref([]);
const notifications = ref([]);
const stats = reactive({
  storageUsage: 0,
  hotCount: 0,
  activeUsers: 0
});

const categoryForm = reactive({ name: '', parentId: undefined, description: '' });
const tagForm = reactive({ name: '' });
const settingForm = reactive({ settingKey: '', settingValue: '', description: '' });
const notificationForm = reactive({ title: '', content: '', level: 'INFO', status: 'ENABLED', publishAt: '' });

const loadAdminData = async () => {
  loading.value = true;
  try {
    const [
      usersRes,
      categoryRes,
      tagRes,
      settingsRes,
      notificationRes,
      storageRes,
      hotRes,
      activityRes
    ] = await Promise.all([
      http.get('/users'),
      http.get('/admin/categories'),
      http.get('/admin/tags'),
      http.get('/admin/settings'),
      http.get('/admin/notifications'),
      http.get('/stats/storage'),
      http.get('/stats/hot-materials'),
      http.get('/stats/user-activity')
    ]);

    users.value = usersRes?.data || [];
    categories.value = categoryRes?.data || [];
    tags.value = tagRes?.data || [];
    settings.value = settingsRes?.data || [];
    notifications.value = notificationRes?.data || [];
    stats.storageUsage = storageRes?.data?.usagePercent || 0;
    stats.hotCount = (hotRes?.data || []).length;
    stats.activeUsers = (activityRes?.data || []).length;
  } finally {
    loading.value = false;
  }
};

const updateRole = async (row) => {
  await http.put(`/users/${row.id}/role`, { role: row.role });
  ElMessage.success('用户角色已更新');
};

const addCategory = async () => {
  await http.post('/admin/categories', categoryForm);
  categoryForm.name = '';
  categoryForm.parentId = undefined;
  categoryForm.description = '';
  await loadAdminData();
  ElMessage.success('分类已创建');
};

const deleteCategory = async (id) => {
  await http.delete(`/admin/categories/${id}`);
  await loadAdminData();
  ElMessage.success('分类已删除');
};

const addTag = async () => {
  await http.post('/admin/tags', tagForm);
  tagForm.name = '';
  await loadAdminData();
  ElMessage.success('标签已创建');
};

const deleteTag = async (id) => {
  await http.delete(`/admin/tags/${id}`);
  await loadAdminData();
  ElMessage.success('标签已删除');
};

const saveSetting = async () => {
  await http.post('/admin/settings', settingForm);
  settingForm.settingKey = '';
  settingForm.settingValue = '';
  settingForm.description = '';
  await loadAdminData();
  ElMessage.success('配置已保存');
};

const addNotification = async () => {
  await http.post('/admin/notifications', {
    ...notificationForm,
    publishAt: notificationForm.publishAt || null
  });
  notificationForm.title = '';
  notificationForm.content = '';
  notificationForm.level = 'INFO';
  notificationForm.status = 'ENABLED';
  notificationForm.publishAt = '';
  await loadAdminData();
  ElMessage.success('通知已发布');
};

const deleteNotification = async (id) => {
  await http.delete(`/admin/notifications/${id}`);
  await loadAdminData();
  ElMessage.success('通知已删除');
};

onMounted(loadAdminData);
</script>

<template>
  <div class="admin-wrap">
    <section class="grid-3">
      <article class="page-card stat-card">
        <h3>存储使用率</h3>
        <strong>{{ stats.storageUsage }}%</strong>
      </article>
      <article class="page-card stat-card">
        <h3>热门素材数量</h3>
        <strong>{{ stats.hotCount }}</strong>
      </article>
      <article class="page-card stat-card">
        <h3>活跃用户数</h3>
        <strong>{{ stats.activeUsers }}</strong>
      </article>
    </section>

    <section class="page-card panel">
      <h2 class="section-title">用户与角色管理</h2>
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="用户名" prop="username" />
        <el-table-column label="邮箱" prop="email" />
        <el-table-column label="显示名称" prop="displayName" />
        <el-table-column label="角色" width="180">
          <template #default="{ row }">
            <el-select v-model="row.role" @change="() => updateRole(row)">
              <el-option label="普通用户" value="USER" />
              <el-option label="VIP用户" value="VIP" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="grid-2">
      <article class="page-card panel">
        <h2 class="section-title">分类管理</h2>
        <el-form label-position="top" class="inline-form">
          <el-form-item label="分类名称"><el-input v-model="categoryForm.name" /></el-form-item>
          <el-form-item label="父级ID"><el-input-number v-model="categoryForm.parentId" :min="1" controls-position="right" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="categoryForm.description" /></el-form-item>
          <el-button type="primary" @click="addCategory">新增分类</el-button>
        </el-form>
        <el-table :data="categories" size="small">
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="名称" prop="name" />
          <el-table-column label="父级" prop="parentId" width="90" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="danger" plain @click="deleteCategory(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </article>

      <article class="page-card panel">
        <h2 class="section-title">标签管理</h2>
        <el-form label-position="top" class="inline-form">
          <el-form-item label="标签名称"><el-input v-model="tagForm.name" /></el-form-item>
          <el-button type="primary" @click="addTag">新增标签</el-button>
        </el-form>
        <el-table :data="tags" size="small">
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="名称" prop="name" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="danger" plain @click="deleteTag(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </article>
    </section>

    <section class="grid-2">
      <article class="page-card panel">
        <h2 class="section-title">系统参数配置</h2>
        <el-form label-position="top" class="inline-form">
          <el-form-item label="配置键"><el-input v-model="settingForm.settingKey" /></el-form-item>
          <el-form-item label="配置值"><el-input v-model="settingForm.settingValue" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="settingForm.description" /></el-form-item>
          <el-button type="primary" @click="saveSetting">保存配置</el-button>
        </el-form>
        <el-table :data="settings" size="small" max-height="280">
          <el-table-column label="键" prop="settingKey" min-width="150" />
          <el-table-column label="值" prop="settingValue" min-width="130" />
          <el-table-column label="描述" prop="description" min-width="180" />
        </el-table>
      </article>

      <article class="page-card panel">
        <h2 class="section-title">系统通知管理</h2>
        <el-form label-position="top" class="inline-form">
          <el-form-item label="标题"><el-input v-model="notificationForm.title" /></el-form-item>
          <el-form-item label="等级">
            <el-select v-model="notificationForm.level">
              <el-option label="信息" value="INFO" />
              <el-option label="告警" value="WARN" />
              <el-option label="错误" value="ERROR" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="notificationForm.status">
              <el-option label="启用" value="ENABLED" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item label="发布时间"><el-date-picker v-model="notificationForm.publishAt" type="datetime" /></el-form-item>
          <el-form-item label="内容" style="grid-column: 1 / -1"><el-input type="textarea" v-model="notificationForm.content" :rows="3" /></el-form-item>
          <el-button type="primary" @click="addNotification">发布通知</el-button>
        </el-form>
        <el-table :data="notifications" size="small" max-height="280">
          <el-table-column label="标题" prop="title" min-width="180" />
          <el-table-column label="等级" prop="level" width="90" />
          <el-table-column label="状态" prop="status" width="90" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="danger" plain @click="deleteNotification(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </article>
    </section>
  </div>
</template>

<style scoped>
.admin-wrap {
  display: grid;
  gap: 1rem;
  min-width: 0;
}

.panel {
  padding: 1rem;
  min-width: 0;
  overflow: hidden;
}

.stat-card {
  padding: 1rem;
  min-width: 0;
}

.stat-card h3 {
  margin: 0;
  font-size: 0.95rem;
}

.stat-card strong {
  margin-top: 0.5rem;
  display: inline-block;
  font-size: 1.8rem;
  color: var(--accent-strong);
}

.inline-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
  margin-bottom: 1rem;
}

@media (max-width: 980px) {
  .inline-form {
    grid-template-columns: 1fr;
  }
}
</style>
