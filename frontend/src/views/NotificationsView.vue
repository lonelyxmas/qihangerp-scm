<template>
    <div>
        <div class="notif-header">
            <div class="notif-tabs">
                <button
                    v-for="t in tabs"
                    :key="t.key"
                    class="notif-tab"
                    :class="{ active: currentTab === t.key }"
                    @click="switchTab(t.key)"
                >
                    {{ t.label }}
                    <span v-if="t.count !== undefined" class="notif-count" :class="{ warn: t.key === 'all' && unreadCount > 0 }">{{ t.count }}</span>
                </button>
            </div>
            <el-button size="small" plain @click="markAllRead">全部已读</el-button>
        </div>

        <div v-if="loading" class="loading-wrap">
            <span class="loading-dot"></span><span class="loading-dot"></span><span class="loading-dot"></span>
            <span class="loading-text">加载中...</span>
        </div>
        <el-empty v-else-if="items.length === 0" description="暂无通知">
            <template #image>
                <div class="empty-icon">🔔</div>
            </template>
        </el-empty>
        <div v-else class="notif-list">
            <div
                v-for="item in items"
                :key="item.id"
                class="notif-item"
                :class="{ unread: item.isRead === 0 }"
                @click="markRead(item)"
            >
                <div class="notif-icon" :style="notifIconStyle(item.type)">{{ notifIcon(item.type) }}</div>
                <div class="notif-body">
                    <div class="notif-title">
                        {{ item.title }}
                        <span class="notif-tag" :class="item.type">{{ typeLabel(item.type) }}</span>
                    </div>
                    <div class="notif-desc">{{ item.content }}</div>
                    <div class="notif-time">{{ item.createdAt }}</div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { apiFetch } from '../api/client';

interface NotificationItem {
    id: number;
    title: string;
    content?: string;
    type: string;
    isRead: number;
    createdAt: string;
}

interface TabDef {
    key: string;
    label: string;
    count?: number;
}

const items = ref<NotificationItem[]>([]);
const loading = ref(true);
const unreadCount = ref(0);
const currentTab = ref('all');
const tabs = ref<TabDef[]>([
    { key: 'all', label: '全部' },
    { key: 'task_assignment', label: '任务' },
    { key: 'approval_request', label: '审批' },
    { key: 'system', label: '系统' },
]);

async function loadData(): Promise<void> {
    loading.value = true;
    try {
        let url = '/api/collab/notification/list?userId=1&limit=50';
        if (currentTab.value !== 'all') url += '&type=' + currentTab.value;
        const d = await apiFetch(url);
        if (d.ok) {
            items.value = Array.isArray(d.data) ? d.data : [];
            unreadCount.value = d.unreadCount || 0;
            updateTabCounts();
        } else {
            items.value = [];
            unreadCount.value = 0;
        }
    } catch (e) {
        items.value = [];
    } finally {
        loading.value = false;
    }
}

function updateTabCounts(): void {
    tabs.value[0].count = unreadCount.value > 0 ? unreadCount.value : undefined;
    const taskCount = items.value.filter((i) => i.type === 'task_assignment' && i.isRead === 0).length;
    const approvalCount = items.value.filter((i) => i.type === 'approval_request' && i.isRead === 0).length;
    tabs.value[1].count = taskCount || undefined;
    tabs.value[2].count = approvalCount || undefined;
}

function switchTab(key: string): void {
    currentTab.value = key;
    loadData();
}

async function markRead(item: NotificationItem): Promise<void> {
    if (item.isRead === 1) return;
    try {
        const d = await apiFetch(`/api/collab/notification/mark-read?id=${item.id}`, { method: 'POST' });
        if (d.ok) {
            item.isRead = 1;
            unreadCount.value = Math.max(0, unreadCount.value - 1);
            updateTabCounts();
        }
    } catch (e) {
        // ignore
    }
}

async function markAllRead(): Promise<void> {
    try {
        const d = await apiFetch('/api/collab/notification/mark-all-read?userId=1', { method: 'POST' });
        if (d.ok) {
            items.value.forEach((i) => (i.isRead = 1));
            unreadCount.value = 0;
            updateTabCounts();
        }
    } catch (e) {
        // ignore
    }
}

function notifIcon(type: string): string {
    if (type === 'task_assignment') return '📋';
    if (type === 'approval_request') return '✅';
    if (type === 'approval_result') return '📌';
    if (type === 'reminder') return '⏰';
    return '🔔';
}

function notifIconStyle(type: string): string {
    if (type === 'task_assignment') return 'background:rgba(245,158,11,0.1)';
    if (type === 'approval_request' || type === 'approval_result') return 'background:rgba(168,85,247,0.1)';
    if (type === 'reminder') return 'background:rgba(59,130,246,0.1)';
    return 'background:rgba(148,163,184,0.1)';
}

function typeLabel(type: string): string {
    if (type === 'task_assignment') return '任务';
    if (type === 'approval_request' || type === 'approval_result') return '审批';
    if (type === 'reminder') return '提醒';
    return '系统';
}

onMounted(loadData);
</script>

<style scoped>
.notif-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.notif-tabs { display: flex; gap: 0; border-bottom: 1px solid var(--border); }
.notif-tab {
    padding: 10px 20px; font-size: 14px; border: none; background: none; cursor: pointer;
    color: var(--text-secondary); border-bottom: 2px solid transparent; transition: all 0.2s;
}
.notif-tab:hover { color: var(--text-primary); }
.notif-tab.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 500; }
.notif-count { margin-left: 6px; padding: 1px 6px; background: var(--primary); color: white; border-radius: 10px; font-size: 11px; }
.notif-count.warn { background: #ef4444; }

.notif-list { display: flex; flex-direction: column; gap: 8px; }
.notif-item { display: flex; gap: 12px; padding: 16px; background: white; border: 1px solid var(--border); border-radius: var(--radius-md); transition: all 0.2s; cursor: pointer; }
.notif-item:hover { box-shadow: var(--shadow-sm); border-color: var(--primary-light); }
.notif-item.unread { border-left: 3px solid var(--primary); background: rgba(99, 102, 241, 0.02); }
.notif-icon { width: 36px; height: 36px; border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }
.notif-body { flex: 1; min-width: 0; }
.notif-title { font-size: 14px; font-weight: 500; color: var(--text-primary); margin-bottom: 4px; }
.notif-desc { font-size: 13px; color: var(--text-secondary); line-height: 1.5; }
.notif-time { font-size: 12px; color: var(--text-muted); margin-top: 6px; }
.notif-tag { font-size: 11px; padding: 1px 6px; border-radius: 4px; margin-left: 8px; }
.notif-tag.task_assignment { background: rgba(245, 158, 11, 0.1); color: #d97706; }
.notif-tag.approval_request, .notif-tag.approval_result { background: rgba(168, 85, 247, 0.1); color: #9333ea; }
.notif-tag.system { background: rgba(148, 163, 184, 0.1); color: #64748b; }

.loading-wrap { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 60px 20px; font-size: 14px; color: var(--text-secondary); }
.loading-dot { width: 8px; height: 8px; background: var(--primary); border-radius: 50%; animation: loadPulse 1.4s infinite ease-in-out both; }
.loading-dot:nth-child(2) { animation-delay: -0.16s; }
.loading-dot:nth-child(3) { animation-delay: -0.32s; }
@keyframes loadPulse { 0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; } 40% { transform: scale(1); opacity: 1; } }
.empty-icon { font-size: 60px; opacity: 0.5; }
</style>
