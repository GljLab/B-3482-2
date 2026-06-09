<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import http from '../api/http';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const active = ref('login');
const loading = ref(false);

const loginForm = reactive({ username: '', password: '' });
const registerForm = reactive({ username: '', email: '', password: '', displayName: '' });
const resetForm = reactive({ email: '', token: '', newPassword: '' });

const submitLogin = async () => {
  loading.value = true;
  try {
    const res = await http.post('/auth/login', loginForm);
    authStore.setAuth(res.data);
    ElMessage.success('登录成功');
    router.push('/');
  } finally {
    loading.value = false;
  }
};

const submitRegister = async () => {
  loading.value = true;
  try {
    const res = await http.post('/auth/register', registerForm);
    authStore.setAuth(res.data);
    ElMessage.success('注册成功');
    router.push('/');
  } finally {
    loading.value = false;
  }
};

const requestResetToken = async () => {
  loading.value = true;
  try {
    const res = await http.post('/auth/forgot-password', { email: resetForm.email });
    resetForm.token = res?.data?.resetToken || '';
    ElMessage.success('重置凭证已生成，请继续完成密码重置');
  } finally {
    loading.value = false;
  }
};

const submitReset = async () => {
  loading.value = true;
  try {
    await http.post('/auth/reset-password', {
      token: resetForm.token,
      newPassword: resetForm.newPassword
    });
    ElMessage.success('密码重置成功，请重新登录');
    active.value = 'login';
    loginForm.username = '';
    loginForm.password = '';
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <main class="auth-page">
    <section class="auth-hero">
      <div class="hero-content">
        <h1 class="brand-title">Clip Hub</h1>
        <p>面向创作团队的素材与项目中枢，上传、检索、协作与导出全流程闭环。</p>
        <div class="hero-badges">
          <el-tag type="success">RBAC</el-tag>
          <el-tag type="warning">版本回滚</el-tag>
          <el-tag type="primary">素材报表</el-tag>
        </div>
      </div>
    </section>

    <section class="auth-panel page-card">
      <el-tabs v-model="active" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form label-position="top" class="form-block" @submit.prevent="submitLogin">
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="submitLogin" class="submit-btn">立即登录</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form label-position="top" class="form-block" @submit.prevent="submitRegister">
            <el-form-item label="用户名">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="显示名称">
              <el-input v-model="registerForm.displayName" placeholder="请输入显示名称" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="submitRegister" class="submit-btn">创建账号</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="找回密码" name="reset">
          <el-form label-position="top" class="form-block" @submit.prevent="submitReset">
            <el-form-item label="邮箱">
              <el-input v-model="resetForm.email" placeholder="请输入注册邮箱" />
            </el-form-item>
            <div class="inline-actions">
              <el-button plain :loading="loading" @click="requestResetToken">生成重置凭证</el-button>
            </div>
            <el-form-item label="重置凭证">
              <el-input v-model="resetForm.token" placeholder="请输入重置凭证" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="submitReset" class="submit-btn">确认重置</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
  background:
    linear-gradient(150deg, rgba(255, 255, 255, 0.84), rgba(236, 245, 230, 0.7)),
    radial-gradient(circle at 85% 15%, rgba(123, 190, 151, 0.3), transparent 45%),
    radial-gradient(circle at 10% 85%, rgba(241, 198, 111, 0.26), transparent 40%);
}

.auth-hero {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem;
}

.hero-content {
  max-width: 520px;
}

.hero-content p {
  color: var(--text-secondary);
  line-height: 1.7;
}

.hero-badges {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.auth-panel {
  margin: 2rem;
  align-self: center;
  padding: 1.2rem;
}

.form-block {
  margin-top: 0.6rem;
}

.inline-actions {
  margin-bottom: 1rem;
}

.submit-btn {
  width: 100%;
  margin-top: 0.5rem;
}

@media (max-width: 980px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-hero {
    padding: 1.5rem;
  }

  .auth-panel {
    margin: 1rem;
  }
}
</style>
