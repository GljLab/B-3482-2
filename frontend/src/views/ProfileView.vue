<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import http from '../api/http';
import { useAuthStore } from '../stores/auth';

const authStore = useAuthStore();

const loading = ref(false);
const profile = reactive({
  id: '',
  username: '',
  email: '',
  role: '',
  teamId: '',
  displayName: '',
  bio: '',
  avatarUrl: ''
});

const favorites = ref([]);

const loadProfile = async () => {
  loading.value = true;
  try {
    const [profileRes, favoritesRes] = await Promise.all([
      http.get('/users/me'),
      http.get('/materials/favorites')
    ]);

    const data = profileRes?.data || {};
    profile.id = data.id || '';
    profile.username = data.username || '';
    profile.email = data.email || '';
    profile.role = data.role || '';
    profile.teamId = data.teamId || '';
    profile.displayName = data.displayName || '';
    profile.bio = data.bio || '';
    profile.avatarUrl = data.avatarUrl || '';

    favorites.value = favoritesRes?.data || [];
  } finally {
    loading.value = false;
  }
};

const saveProfile = async () => {
  await http.put('/users/me', {
    displayName: profile.displayName,
    bio: profile.bio,
    avatarUrl: profile.avatarUrl
  });
  authStore.user = {
    ...authStore.user,
    displayName: profile.displayName
  };
  localStorage.setItem('clip_hub_user', JSON.stringify(authStore.user));
  ElMessage.success('资料更新成功');
  await loadProfile();
};

onMounted(loadProfile);
</script>

<template>
  <div class="profile-wrap">
    <section class="page-card profile-header" v-loading="loading">
      <div class="avatar-box">
        <el-avatar :size="72" :src="profile.avatarUrl || undefined">{{ profile.displayName?.slice(0, 1) || 'U' }}</el-avatar>
        <div>
          <h2 class="section-title">{{ profile.displayName || '个人中心' }}</h2>
          <p class="section-subtitle">账号 {{ profile.username }} · 角色 {{ profile.role }} · 团队 {{ profile.teamId || '-' }}</p>
        </div>
      </div>
      <el-tag type="success" effect="dark">{{ profile.email }}</el-tag>
    </section>

    <section class="grid-2">
      <article class="page-card panel">
        <h3 class="section-title">资料编辑</h3>
        <el-form label-position="top" style="margin-top: 1rem">
          <el-form-item label="显示名称"><el-input v-model="profile.displayName" /></el-form-item>
          <el-form-item label="头像地址"><el-input v-model="profile.avatarUrl" placeholder="请输入头像链接" /></el-form-item>
          <el-form-item label="简介"><el-input type="textarea" :rows="4" v-model="profile.bio" /></el-form-item>
          <el-button type="primary" @click="saveProfile">保存资料</el-button>
        </el-form>
      </article>

      <article class="page-card panel">
        <h3 class="section-title">我的收藏</h3>
        <el-empty v-if="favorites.length === 0" description="暂无收藏素材" :image-size="90" />
        <el-table v-else :data="favorites" size="small" max-height="320">
          <el-table-column label="标题" prop="title" min-width="180" />
          <el-table-column label="类型" prop="type" width="100" />
          <el-table-column label="分类" width="120">
            <template #default="{ row }">{{ row.category?.name || '-' }}</template>
          </el-table-column>
          <el-table-column label="下载" prop="downloadCount" width="80" />
        </el-table>
      </article>
    </section>
  </div>
</template>

<style scoped>
.profile-wrap {
  display: grid;
  gap: 1rem;
  min-width: 0;
}

.profile-header {
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  min-width: 0;
  flex-wrap: wrap;
}

.avatar-box {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.panel {
  padding: 1rem;
  min-width: 0;
  overflow: hidden;
}

@media (max-width: 980px) {
  .profile-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
