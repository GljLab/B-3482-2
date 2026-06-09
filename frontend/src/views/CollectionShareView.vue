<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import http from '../api/http';

const route = useRoute();
const shareCode = route.params.code;

const loading = ref(false);
const needPassword = ref(false);
const passwordForm = reactive({ password: '' });
const collectionData = ref(null);
const error = ref('');

const fetchShareData = async (password) => {
  loading.value = true;
  error.value = '';
  try {
    const params = password ? { password } : {};
    const res = await http.get(`/collections/share/${shareCode}`, { params });
    collectionData.value = res?.data || null;
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '加载失败';
    if (msg.includes('密码')) {
      needPassword.value = true;
      error.value = msg;
    } else {
      error.value = msg;
    }
  } finally {
    loading.value = false;
  }
};

const submitPassword = () => {
  if (!passwordForm.password.trim()) {
    ElMessage.warning('请输入访问密码');
    return;
  }
  fetchShareData(passwordForm.password);
};

const formatSize = (bytes) => {
  if (!bytes) return '-';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1048576).toFixed(1) + ' MB';
};

onMounted(() => fetchShareData());
</script>

<template>
  <div class="share-page">
    <div class="share-card page-card" v-loading="loading">
      <div v-if="error && !needPassword" class="error-state">
        <h2>无法访问</h2>
        <p>{{ error }}</p>
      </div>

      <div v-else-if="needPassword && !collectionData" class="password-state">
        <h2>该素材集需要密码访问</h2>
        <el-form @submit.prevent="submitPassword" class="password-form">
          <el-form-item>
            <el-input v-model="passwordForm.password" type="password" placeholder="请输入访问密码" show-password />
          </el-form-item>
          <el-button type="primary" @click="submitPassword" :loading="loading">验证</el-button>
        </el-form>
        <p v-if="error" class="error-hint">{{ error }}</p>
      </div>

      <div v-else-if="collectionData" class="content-state">
        <header class="share-header">
          <h1>{{ collectionData.name }}</h1>
          <p>{{ collectionData.description || '暂无描述' }}</p>
          <el-tag type="info" size="small">只读模式 · 素材集分享</el-tag>
        </header>

        <section class="materials-list">
          <el-empty v-if="!collectionData.materials?.length" description="该素材集暂无素材" />
          <article v-else v-for="item in collectionData.materials" :key="item.id" class="material-item page-card">
            <div class="material-info">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description || '暂无描述' }}</p>
              <div class="material-meta">
                <el-tag size="small">{{ item.type }}</el-tag>
                <span>格式：{{ item.format || '-' }}</span>
                <span>大小：{{ formatSize(item.sizeBytes) }}</span>
                <span v-if="item.durationSeconds">时长：{{ item.durationSeconds }}秒</span>
                <span v-if="item.resolution">分辨率：{{ item.resolution }}</span>
              </div>
              <div v-if="item.note" class="material-note">
                <strong>备注：</strong>{{ item.note }}
              </div>
            </div>
          </article>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.share-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 2rem;
  background: radial-gradient(circle at 10% 0%, #fbfef6 0%, #eef4ec 40%, #d7e2d8 100%);
}

.share-card {
  max-width: 900px;
  width: 100%;
  padding: 2rem;
}

.error-state,
.password-state {
  text-align: center;
  padding: 2rem 0;
}

.error-state h2,
.password-state h2 {
  margin: 0 0 1rem;
}

.password-form {
  max-width: 320px;
  margin: 1.5rem auto;
}

.error-hint {
  color: #e6a23c;
  margin-top: 0.5rem;
}

.share-header {
  margin-bottom: 1.5rem;
  border-bottom: 1px solid var(--border-subtle);
  padding-bottom: 1rem;
}

.share-header h1 {
  margin: 0 0 0.5rem;
}

.share-header p {
  margin: 0 0 0.5rem;
  color: var(--text-secondary);
}

.materials-list {
  display: grid;
  gap: 0.75rem;
}

.material-item {
  padding: 1rem;
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
}

.material-info h3 {
  margin: 0 0 0.3rem;
}

.material-info p {
  margin: 0 0 0.5rem;
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.material-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  font-size: 0.84rem;
  color: var(--text-secondary);
}

.material-note {
  margin-top: 0.5rem;
  padding: 0.5rem;
  background: rgba(26, 143, 91, 0.08);
  border-radius: 8px;
  font-size: 0.86rem;
  color: var(--accent-strong);
}
</style>
