<template>
    <div class="app-shell">
        <aside class="app-sidebar">
            <div class="app-brand">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
                </svg>
                <span>启航AI</span>
            </div>
            <nav class="app-nav">
                <div class="app-nav-group" v-for="g in navGroups" :key="g.label">
                    <div class="app-nav-label">{{ g.label }}</div>
                    <router-link
                        v-for="item in g.items"
                        :key="item.path"
                        :to="item.path"
                        class="app-nav-item"
                        :class="{ active: isActive(item.path) }"
                    >{{ item.icon }} {{ item.label }}</router-link>
                </div>
            </nav>
        </aside>
        <div class="app-main">
            <header class="app-header">
                <div class="app-title">{{ pageTitle }}</div>
                <div class="app-header-right">
                    <el-tag size="small" type="info" effect="plain">Vue 版</el-tag>
                    <a :href="legacyBase" target="_blank" class="legacy-link">原版 Web</a>
                    <div class="app-user">
                        <span class="app-avatar">{{ avatarText }}</span>
                        <span>{{ userName }}</span>
                    </div>
                    <el-button size="small" text type="danger" @click="onLogout">退出</el-button>
                </div>
            </header>
            <main class="app-content" :class="{ flush: route.meta.flush }">
                <router-view />
            </main>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const legacyBase = 'http://localhost:6790';

const navGroups = [
    {
        label: 'AI 工作台',
        items: [
            { path: '/home', label: '首页', icon: '🏠' },
            { path: '/chat', label: 'AI 对话', icon: '💬' },
        ],
    },
    {
        label: '工作中心',
        items: [
            { path: '/kb', label: '知识库', icon: '📚' },
            { path: '/data', label: '数据中心', icon: '🗂️' },
            { path: '/tasks', label: '任务中心', icon: '✅' },
            { path: '/reminders', label: '提醒中心', icon: '⏰' },
        ],
    },
    {
        label: '协作中心',
        items: [
            { path: '/activity', label: '动态流', icon: '📢' },
            { path: '/notifications', label: '通知中心', icon: '🔔' },
            { path: '/approvals', label: '审批中心', icon: '📋' },
        ],
    },
    {
        label: '系统',
        items: [
            { path: '/datacenter', label: '数据模块管理', icon: '📦' },
            { path: '/config', label: '系统配置', icon: '⚙️' },
        ],
    },
];

const pageTitle = computed(() => String(route.meta.title ?? '启航 AI'));
const userName = computed(() => auth.displayName());
const avatarText = computed(() => auth.displayName().charAt(0).toUpperCase());

function isActive(path: string): boolean {
    return route.path === path;
}

function onLogout(): void {
    auth.logout();
    router.push('/login');
}
</script>

<style scoped>
.app-shell { display: flex; height: 100%; }
.app-sidebar {
    width: 200px; background: var(--bg-sidebar); border-right: 1px solid var(--border);
    display: flex; flex-direction: column; padding: 12px 0; flex-shrink: 0;
}
.app-brand {
    padding: 4px 16px 12px; font-size: 20px; font-weight: 700; color: var(--primary);
    display: flex; align-items: center; gap: 8px;
}
.app-brand svg { width: 24px; height: 24px; }
.app-nav { flex: 1; overflow-y: auto; }
.app-nav-group { margin-bottom: 4px; }
.app-nav-label {
    font-size: 10px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px;
    padding: 12px 16px 4px; font-weight: 600;
}
.app-nav-item {
    display: flex; align-items: center; gap: 10px; padding: 8px 16px; cursor: pointer;
    color: var(--text-secondary); font-size: 13px; transition: all 0.2s; text-decoration: none;
    margin: 1px 6px; border-radius: var(--radius-sm);
}
.app-nav-item:hover { background: var(--hover); color: var(--text-primary); }
.app-nav-item.active { background: rgba(99, 102, 241, 0.1); color: var(--primary); font-weight: 500; }
.app-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.app-header {
    padding: 16px 24px; border-bottom: 1px solid var(--border); background: white;
    display: flex; align-items: center; justify-content: space-between; flex-shrink: 0;
}
.app-title { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.app-header-right { display: flex; align-items: center; gap: 12px; }
.legacy-link { font-size: 12px; color: var(--text-secondary); text-decoration: none; }
.legacy-link:hover { color: var(--primary); }
.app-user { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text-primary); }
.app-avatar {
    width: 30px; height: 30px; border-radius: 50%; background: var(--primary); color: white;
    display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600;
}
.app-content { flex: 1; overflow-y: auto; padding: 24px; position: relative; min-height: 0; }
.app-content.flush { padding: 0; overflow: hidden; display: flex; flex-direction: column; }
</style>
