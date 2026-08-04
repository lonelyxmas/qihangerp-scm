<template>
    <div class="login-wrap">
        <el-card class="login-card" shadow="always">
            <div class="login-title">🔐 登录启航AI</div>
            <div class="login-sub">登录后使用完整功能</div>
            <el-form :model="form" @submit.prevent="onSubmit" size="large">
                <el-form-item>
                    <el-input v-model="form.username" placeholder="用户名" />
                </el-form-item>
                <el-form-item>
                    <el-input v-model="form.password" type="password" placeholder="密码" show-password @keyup.enter="onSubmit" />
                </el-form-item>
                <el-form-item v-if="error">
                    <div class="login-error">{{ error }}</div>
                </el-form-item>
                <el-button type="primary" class="login-btn" :loading="loading" @click="onSubmit">登 录</el-button>
            </el-form>
            <div class="login-hint">默认账号：admin / admin123</div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();

const form = ref({ username: 'admin', password: 'admin123' });
const loading = ref(false);
const error = ref('');

async function onSubmit(): Promise<void> {
    if (!form.value.username.trim() || !form.value.password) {
        error.value = '请输入用户名和密码';
        return;
    }
    loading.value = true;
    error.value = '';
    try {
        await auth.login(form.value.username.trim(), form.value.password);
        router.push('/home');
    } catch (e) {
        error.value = e instanceof Error ? e.message : '登录失败';
    } finally {
        loading.value = false;
    }
}
</script>

<style scoped>
.login-wrap {
    height: 100%; display: flex; align-items: center; justify-content: center;
    background: linear-gradient(135deg, #eef2ff 0%, #f8fafc 100%);
}
.login-card { width: 380px; padding: 8px 8px 16px; }
.login-title { font-size: 20px; font-weight: 700; color: var(--text-primary); text-align: center; margin-bottom: 4px; }
.login-sub { font-size: 13px; color: var(--text-muted); text-align: center; margin-bottom: 24px; }
.login-error { width: 100%; color: #dc2626; font-size: 13px; }
.login-btn { width: 100%; }
.login-hint { margin-top: 12px; text-align: center; font-size: 12px; color: var(--text-muted); }
</style>
