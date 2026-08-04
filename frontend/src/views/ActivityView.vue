<template>
    <div>
        <div class="activity-filters">
            <button
                v-for="f in filters"
                :key="f.key"
                class="filter-btn"
                :class="{ active: currentFilter === f.key }"
                @click="switchFilter(f.key)"
            >{{ f.label }}</button>
        </div>

        <div v-if="loading" class="loading-wrap">
            <span class="loading-dot"></span><span class="loading-dot"></span><span class="loading-dot"></span>
            <span class="loading-text">加载中...</span>
        </div>
        <el-empty v-else-if="items.length === 0" description="暂无动态">
            <template #image>
                <div class="empty-icon">📜</div>
            </template>
        </el-empty>
        <div v-else class="activity-timeline">
            <div v-for="item in items" :key="item.id" class="activity-item" :class="'type-' + item.actionType">
                <div class="activity-header">
                    <span class="activity-type-badge" :class="typeClass(item.actionType)">{{ typeLabel(item.actionType) }}</span>
                    <span class="activity-time">{{ item.createdAt }}</span>
                </div>
                <div class="activity-desc">{{ item.actionDesc }}</div>
                <div class="activity-footer">
                    <span v-if="item.triggeredName">👤 {{ item.triggeredName }}</span>
                    <span v-if="item.targetName">→ {{ item.targetName }}</span>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { apiFetch } from '../api/client';

interface ActivityItem {
    id: number;
    actionType: string;
    actionDesc: string;
    triggeredName?: string;
    targetName?: string;
    createdAt: string;
}

const items = ref<ActivityItem[]>([]);
const loading = ref(true);
const currentFilter = ref('all');
const filters = [
    { key: 'all', label: '全部' },
    { key: 'create_note', label: '知识库' },
    { key: 'create_record', label: '数据集' },
    { key: 'assign_task', label: '任务' },
    { key: 'approval_request', label: '审批' },
];

async function loadData(): Promise<void> {
    loading.value = true;
    try {
        let url = '/api/collab/activity/list?limit=50';
        if (currentFilter.value !== 'all') url += '&type=' + currentFilter.value;
        const d = await apiFetch(url);
        items.value = d.ok && Array.isArray(d.data) ? d.data : [];
    } catch (e) {
        items.value = [];
    } finally {
        loading.value = false;
    }
}

function switchFilter(key: string): void {
    currentFilter.value = key;
    loadData();
}

function typeClass(type: string): string {
    if (!type) return 'system';
    if (type.includes('note')) return 'note';
    if (type.includes('record') || type.includes('data')) return 'data';
    if (type.includes('task')) return 'task';
    if (type.includes('approval')) return 'approval';
    return 'system';
}

function typeLabel(type: string): string {
    if (!type) return '系统';
    if (type.includes('create_note') || type.includes('update_note')) return '📄 笔记';
    if (type.includes('create_record')) return '📊 新增记录';
    if (type.includes('update_record')) return '📊 更新记录';
    if (type.includes('assign_task')) return '📋 任务指派';
    if (type.includes('approval_request')) return '✅ 审批请求';
    if (type.includes('approval_result')) return '✅ 审批结果';
    if (type.includes('notification')) return '🔔 通知';
    if (type.includes('report')) return '📈 报告';
    if (type.includes('analyze')) return '🔍 分析';
    return '系统';
}

onMounted(loadData);
</script>

<style scoped>
.activity-filters { display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap; }
.filter-btn {
    padding: 6px 14px; border: 1px solid var(--border); border-radius: var(--radius-sm);
    font-size: 13px; cursor: pointer; background: white; color: var(--text-secondary); transition: all 0.2s;
}
.filter-btn:hover { border-color: var(--primary); color: var(--primary); }
.filter-btn.active { background: var(--primary); color: white; border-color: var(--primary); }

.activity-timeline { position: relative; padding-left: 32px; }
.activity-timeline::before { content: ''; position: absolute; left: 12px; top: 0; bottom: 0; width: 2px; background: var(--border); }
.activity-item { position: relative; margin-bottom: 20px; padding: 16px; background: white; border: 1px solid var(--border); border-radius: var(--radius-md); transition: all 0.2s; }
.activity-item:hover { box-shadow: var(--shadow-sm); }
.activity-item::before { content: ''; position: absolute; left: -24px; top: 20px; width: 10px; height: 10px; border-radius: 50%; background: var(--primary); border: 2px solid white; }
.activity-item.type-create_note::before { background: #22c55e; }
.activity-item.type-create_record::before { background: #3b82f6; }
.activity-item.type-assign_task::before { background: #f59e0b; }
.activity-item.type-approval_request::before { background: #a855f7; }
.activity-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.activity-type-badge { font-size: 11px; padding: 2px 8px; border-radius: 4px; font-weight: 500; }
.activity-type-badge.note { background: rgba(34, 197, 94, 0.1); color: #16a34a; }
.activity-type-badge.data { background: rgba(59, 130, 246, 0.1); color: #2563eb; }
.activity-type-badge.task { background: rgba(245, 158, 11, 0.1); color: #d97706; }
.activity-type-badge.approval { background: rgba(168, 85, 247, 0.1); color: #9333ea; }
.activity-type-badge.system { background: rgba(148, 163, 184, 0.1); color: #64748b; }
.activity-time { font-size: 12px; color: var(--text-muted); margin-left: auto; }
.activity-desc { font-size: 14px; color: var(--text-primary); line-height: 1.6; }
.activity-footer { display: flex; align-items: center; gap: 12px; margin-top: 8px; font-size: 12px; color: var(--text-muted); }

.loading-wrap { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 60px 20px; font-size: 14px; color: var(--text-secondary); }
.loading-dot { width: 8px; height: 8px; background: var(--primary); border-radius: 50%; animation: loadPulse 1.4s infinite ease-in-out both; }
.loading-dot:nth-child(2) { animation-delay: -0.16s; }
.loading-dot:nth-child(3) { animation-delay: -0.32s; }
@keyframes loadPulse { 0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; } 40% { transform: scale(1); opacity: 1; } }
.empty-icon { font-size: 60px; opacity: 0.5; }
</style>
