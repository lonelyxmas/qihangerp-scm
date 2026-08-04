<template>
    <div>
        <div class="approval-tabs">
            <button
                v-for="t in tabs"
                :key="t.key"
                class="approval-tab"
                :class="{ active: currentTab === t.key }"
                @click="switchTab(t.key)"
            >
                {{ t.label }}
                <span v-if="t.count !== undefined" class="approval-count">{{ t.count }}</span>
            </button>
        </div>

        <div v-if="loading" class="loading-wrap">
            <span class="loading-dot"></span><span class="loading-dot"></span><span class="loading-dot"></span>
            <span class="loading-text">加载中...</span>
        </div>
        <el-empty v-else-if="items.length === 0" description="暂无审批请求">
            <template #image>
                <div class="empty-icon">✅</div>
            </template>
        </el-empty>
        <div v-else class="approval-list">
            <div v-for="item in items" :key="item.id" class="approval-item" :class="item.status">
                <div class="approval-icon" :style="statusIconStyle(item.status)">{{ statusIcon(item.status) }}</div>
                <div class="approval-body">
                    <div class="approval-title">{{ item.title }}</div>
                    <div v-if="item.description" class="approval-desc">{{ item.description }}</div>
                    <div class="approval-meta">
                        <span>📌 {{ sourceLabel(item.sourceType) }}</span>
                        <span>👤 提交: {{ item.submitterName || '未知' }}</span>
                        <span>👤 审批: {{ item.approverName || '未知' }}</span>
                        <span class="approval-status" :class="item.status">{{ statusLabel(item.status) }}</span>
                        <span>🕐 {{ item.createdAt }}</span>
                    </div>
                    <div v-if="item.comment" class="approval-comment">
                        审批意见: {{ item.comment }}
                    </div>
                    <div v-if="item.status === 'pending'" class="approval-actions">
                        <template v-if="showCommentBox !== item.id">
                            <el-button size="small" type="primary" @click="openCommentBox(item.id, 'approve')">✓ 通过</el-button>
                            <el-button size="small" plain @click="openCommentBox(item.id, 'reject')">✗ 驳回</el-button>
                        </template>
                        <template v-else>
                            <el-input v-model="commentText" size="small" placeholder="输入审批意见（可选）" style="flex: 1" @keyup.enter="processApproval(item)" />
                            <el-button size="small" type="primary" :loading="processing" @click="processApproval(item)">确认</el-button>
                            <el-button size="small" @click="showCommentBox = null">取消</el-button>
                        </template>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { apiFetch, apiError } from '../api/client';

interface ApprovalItem {
    id: number;
    title: string;
    description?: string;
    sourceType: string;
    submitterName?: string;
    approverName?: string;
    status: string;
    comment?: string;
    createdAt: string;
}

interface TabDef {
    key: string;
    label: string;
    count?: number;
}

const items = ref<ApprovalItem[]>([]);
const loading = ref(true);
const currentTab = ref('pending');
const tabs = ref<TabDef[]>([
    { key: 'pending', label: '待我审批' },
    { key: 'submitted', label: '我提交的' },
    { key: 'history', label: '已处理' },
]);
const showCommentBox = ref<number | null>(null);
const pendingAction = ref('approve');
const commentText = ref('');
const processing = ref(false);

async function loadData(): Promise<void> {
    loading.value = true;
    try {
        const d = await apiFetch(`/api/collab/approval/list?userId=1&tab=${currentTab.value}`);
        if (d.ok && Array.isArray(d.data)) {
            items.value = d.data;
            updateTabCounts();
        } else {
            items.value = [];
        }
    } catch (e) {
        items.value = [];
    } finally {
        loading.value = false;
    }
}

function updateTabCounts(): void {
    const pendingCount = items.value.filter((i) => i.status === 'pending').length;
    tabs.value[0].count = pendingCount > 0 ? pendingCount : undefined;
}

function switchTab(key: string): void {
    currentTab.value = key;
    showCommentBox.value = null;
    loadData();
}

function openCommentBox(id: number, action: string): void {
    showCommentBox.value = id;
    pendingAction.value = action;
    commentText.value = '';
}

async function processApproval(item: ApprovalItem): Promise<void> {
    processing.value = true;
    try {
        const formData = new FormData();
        formData.append('id', String(item.id));
        formData.append('action', pendingAction.value);
        formData.append('comment', commentText.value || '');
        const d = await apiFetch('/api/collab/approval/process', { method: 'POST', body: formData });
        if (d.ok) {
            showCommentBox.value = null;
            ElMessage.success(pendingAction.value === 'approve' ? '✅ 已通过' : '✅ 已驳回');
            await loadData();
        } else {
            ElMessage.error('❌ ' + apiError(d, '处理失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 处理失败');
    } finally {
        processing.value = false;
    }
}

function sourceLabel(s: string): string {
    return s === 'dataset_record' ? '数据集' : s === 'knowledge_article' ? '知识库' : '报告';
}

function statusIcon(status: string): string {
    if (status === 'pending') return '⏳';
    if (status === 'approved') return '✅';
    return '❌';
}

function statusIconStyle(status: string): string {
    if (status === 'pending') return 'background:rgba(245,158,11,0.1)';
    if (status === 'approved') return 'background:rgba(34,197,94,0.1)';
    return 'background:rgba(239,68,68,0.1)';
}

function statusLabel(status: string): string {
    if (status === 'pending') return '待审批';
    if (status === 'approved') return '已通过';
    return '已驳回';
}

onMounted(loadData);
</script>

<style scoped>
.approval-tabs { display: flex; gap: 0; margin-bottom: 20px; border-bottom: 1px solid var(--border); }
.approval-tab {
    padding: 10px 20px; font-size: 14px; border: none; background: none; cursor: pointer;
    color: var(--text-secondary); border-bottom: 2px solid transparent; transition: all 0.2s;
}
.approval-tab:hover { color: var(--text-primary); }
.approval-tab.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 500; }
.approval-count { margin-left: 6px; padding: 1px 6px; background: #ef4444; color: white; border-radius: 10px; font-size: 11px; }

.approval-list { display: flex; flex-direction: column; gap: 8px; }
.approval-item { display: flex; gap: 12px; padding: 16px; background: white; border: 1px solid var(--border); border-radius: var(--radius-md); transition: all 0.2s; }
.approval-item:hover { box-shadow: var(--shadow-sm); }
.approval-item.pending { border-left: 3px solid #f59e0b; }
.approval-item.approved { border-left: 3px solid #22c55e; }
.approval-item.rejected { border-left: 3px solid #ef4444; }
.approval-icon { width: 36px; height: 36px; border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }
.approval-body { flex: 1; min-width: 0; }
.approval-title { font-size: 14px; font-weight: 500; color: var(--text-primary); margin-bottom: 4px; }
.approval-desc { font-size: 13px; color: var(--text-secondary); margin-bottom: 8px; line-height: 1.5; }
.approval-meta { display: flex; gap: 16px; font-size: 12px; color: var(--text-muted); flex-wrap: wrap; }
.approval-status { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 500; }
.approval-status.pending { background: rgba(245, 158, 11, 0.1); color: #d97706; }
.approval-status.approved { background: rgba(34, 197, 94, 0.1); color: #16a34a; }
.approval-status.rejected { background: rgba(239, 68, 68, 0.1); color: #dc2626; }
.approval-comment { margin-top: 6px; font-size: 12px; color: var(--text-secondary); background: var(--hover); padding: 6px 10px; border-radius: 4px; }
.approval-actions { display: flex; gap: 8px; margin-top: 10px; }

.loading-wrap { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 60px 20px; font-size: 14px; color: var(--text-secondary); }
.loading-dot { width: 8px; height: 8px; background: var(--primary); border-radius: 50%; animation: loadPulse 1.4s infinite ease-in-out both; }
.loading-dot:nth-child(2) { animation-delay: -0.16s; }
.loading-dot:nth-child(3) { animation-delay: -0.32s; }
@keyframes loadPulse { 0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; } 40% { transform: scale(1); opacity: 1; } }
.empty-icon { font-size: 60px; opacity: 0.5; }
</style>
