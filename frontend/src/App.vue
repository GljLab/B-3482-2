<script setup>
import { onErrorCaptured } from 'vue';
import { ElMessage } from 'element-plus';

onErrorCaptured((error) => {
  if (error?.isAxiosError) {
    return false;
  }
  ElMessage.error(error?.message || '页面出现异常，请稍后重试');
  return false;
});
</script>

<template>
  <el-config-provider>
    <router-view />
  </el-config-provider>
</template>
