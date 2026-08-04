<template>
    <div>
        <div v-if="loading" v-loading="true" class="home-loading"></div>
        <div v-else-if="error" class="card empty-state">{{ error }}</div>
        <template v-else>
            <div class="stat-grid">
                <div v-for="s in statCards" :key="s.label" class="stat-card">
                    <div class="stat-label">{{ s.label }}</div>
                    <div class="stat-value">{{ s.value }}</div>
                </div>
            </div>
            <div class="home-cols">
                <div class="card">
                    <div class="card-title" style="margin-bottom: 12px;">📋 待办任务</div>
                    <div v-if="recentTasks.length === 0" class="text-muted">暂无任务</div>
                    <div v-for="t in recentTasks" :key="t.id" class="list-row">
                        <el-tag size="small" :type="statusType(t.status)" effect="plain">{{ statusLabel(t.status) }}</el-tag>
                        <span class="list-main">{{ t.title }}</span>
                        <span v-if="t.dueDate" class="text-muted list-date">{{ t.dueDate }}</span>
                    </div>
                </div>
                <div class="card">
                    <div class="card-title" style="margin-bottom: 12px;">🔔 未读通知</div>
                    <div v-if="unreadList.length === 0" class="text-muted">暂无通知</div>
                    <div v-for="n in unreadList" :key="n.id" class="list-row">
                        <span class="list-main">{{ n.title }}</span>
                        <span class="text-muted list-date">{{ n.content }}</span>
                    </div>
                </div>
            </div>
        </template>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { apiFetch } from '../api/client';

const loading = ref(true);
const error = ref('');
const stats = ref<any>({});

const businessStats = computed<any>(() => stats.value.businessStats ?? {});

const statCards = computed(() => [
    { label: '知识库', value: businessStats.value.knowledgeBases?.count ?? 0 },
    { label: '索引文件', value: businessStats.value.indexedFiles?.count ?? 0 },
    { label: '数据集', value: businessStats.value.dataSets?.count ?? 0 },
    { label: '数据记录', value: businessStats.value.dataRecords?.count ?? 0 },
    { label: '任务', value: businessStats.value.tasks?.total ?? 0 },
    { label: '用户', value: businessStats.value.users?.count ?? 0 },
]);

const recentTasks = computed<any[]>(() => stats.value.recentTasks ?? []);
const unreadList = computed<any[]>(() => stats.value.unreadNotifications?.list ?? []);

function statusLabel(s: string): string {
    return { pending: '待办', in_progress: '进行中', done: '已完成' }[s] ?? s;
}
function statusType(s: string): any {
    return { pending: 'warning', in_progress: 'primary', done: 'success' }[s] ?? 'info';
}

onMounted(async () => {
    try {
        const d = await apiFetch('/api/dashboard/stats');
        stats.value = d.ok ? d : {};
    } catch (e) {
        error.value = '看板加载失败';
    } finally {
        loading.value = false;
    }
});
</script>

<style scoped>
.home-loading { min-height: 200px; }
.stat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; margin-bottom: 20px; }
.stat-card {
    background: white; border: 1px solid var(--border); border-radius: var(--radius-md);
    padding: 16px; box-shadow: var(--shadow-sm);
}
.stat-label { font-size: 12px; color: var(--text-secondary); margin-bottom: 6px; }
.stat-value { font-size: 24px; font-weight: 700; color: var(--primary); }
.home-cols { display: grid; grid-template-columns: 1fr 1fr; gap: 0 20px; }
.list-row { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.list-row:last-child { border-bottom: none; }
.list-main { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.list-date { font-size: 12px; }
@media (max-width: 900px) { .home-cols { grid-template-columns: 1fr; } }
</style>
