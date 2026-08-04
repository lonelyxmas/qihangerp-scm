<template>
    <div>
        <div class="card">
            <div class="card-header">
                <h2 class="card-title">📋 任务看板</h2>
                <el-button type="primary" size="small" @click="showAddTaskModal">+ 添加任务</el-button>
            </div>
            <div class="task-board">
                <div class="task-column column-pending">
                    <div class="column-header">
                        <span class="column-title">📋 待办</span>
                        <span class="column-count pending">{{ pendingTasks.length }}</span>
                    </div>
                    <el-empty v-if="pendingTasks.length === 0" description="暂无任务" :image-size="50" />
                    <div v-else>
                        <div v-for="task in pendingTasks" :key="task.id" class="task-card" @click="showEditTaskModal(task)">
                            <div class="task-title">
                                {{ task.title }}
                                <span v-if="task.action === 'ai'" class="badge badge-ai">🤖 AI</span>
                                <span v-if="task.scheduleType === 'cycle'" class="badge badge-cycle">🔁 {{ cycleLabel(task) }}</span>
                                <span v-if="task.scheduledStart && task.scheduleType !== 'cycle'" class="badge badge-schedule">🚀 {{ formatSchedule(task.scheduledStart) }}</span>
                                <span v-if="task.queuePosition" class="badge badge-queue">⏳ 排队 #{{ task.queuePosition }}</span>
                                <span v-else-if="task.queueStatus === 'RUNNING'" class="badge badge-running">⚙️ 执行中</span>
                                <span v-if="isDueToday(task)" class="badge badge-due">今日到期</span>
                                <span v-if="isOverdue(task)" class="badge badge-overdue">已逾期</span>
                            </div>
                            <div class="task-meta">
                                <span :class="'priority-' + task.priority">{{ priorityEmoji(task.priority) }}</span>
                                <span v-if="task.dueDate" class="task-due">📅 {{ formatDate(task.dueDate) }}</span>
                                <span v-if="task.creatorName" class="task-due">👤 {{ task.creatorName }}</span>
                            </div>
                            <div v-if="task.action === 'ai'" class="task-actions" @click.stop>
                                <el-button v-if="!task.queuePosition && task.queueStatus !== 'RUNNING'" size="small" type="primary" plain @click="executeTask(task)">⚡ 立即执行</el-button>
                                <el-button v-if="task.queuePosition" size="small" type="warning" plain @click="cancelQueuedTask(task)">✖ 取消排队</el-button>
                                <el-button size="small" plain @click="showTaskExecutions(task)">📜 执行记录</el-button>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="task-column column-progress">
                    <div class="column-header">
                        <span class="column-title">🔄 进行中</span>
                        <span class="column-count progress">{{ progressTasks.length }}</span>
                    </div>
                    <el-empty v-if="progressTasks.length === 0" description="暂无任务" :image-size="50" />
                    <div v-else>
                        <div v-for="task in progressTasks" :key="task.id" class="task-card" @click="showEditTaskModal(task)">
                            <div class="task-title">
                                {{ task.title }}
                                <span v-if="task.action === 'ai'" class="badge badge-ai">🤖 AI</span>
                                <span v-if="task.scheduleType === 'cycle'" class="badge badge-cycle">🔁 {{ cycleLabel(task) }}</span>
                                <span v-if="task.scheduledStart && task.scheduleType !== 'cycle'" class="badge badge-schedule">🚀 {{ formatSchedule(task.scheduledStart) }}</span>
                                <span v-if="task.queuePosition" class="badge badge-queue">⏳ 排队 #{{ task.queuePosition }}</span>
                                <span v-else-if="task.queueStatus === 'RUNNING'" class="badge badge-running">⚙️ 执行中</span>
                                <span v-if="isDueToday(task)" class="badge badge-due">今日到期</span>
                                <span v-if="isOverdue(task)" class="badge badge-overdue">已逾期</span>
                            </div>
                            <div class="task-meta">
                                <span :class="'priority-' + task.priority">{{ priorityEmoji(task.priority) }}</span>
                                <span v-if="task.dueDate" class="task-due">📅 {{ formatDate(task.dueDate) }}</span>
                                <span v-if="task.creatorName" class="task-due">👤 {{ task.creatorName }}</span>
                            </div>
                            <div v-if="task.action === 'ai'" class="task-actions" @click.stop>
                                <el-button v-if="!task.queuePosition && task.queueStatus !== 'RUNNING'" size="small" type="primary" plain @click="executeTask(task)">⚡ 立即执行</el-button>
                                <el-button v-if="task.queuePosition" size="small" type="warning" plain @click="cancelQueuedTask(task)">✖ 取消排队</el-button>
                                <el-button size="small" plain @click="showTaskExecutions(task)">📜 执行记录</el-button>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="task-column column-done">
                    <div class="column-header">
                        <span class="column-title">✅ 已完成</span>
                        <span class="column-count done">{{ doneTasks.length }}</span>
                    </div>
                    <el-empty v-if="doneTasks.length === 0" description="暂无任务" :image-size="50" />
                    <div v-else>
                        <div v-for="task in doneTasks" :key="task.id" class="task-card" @click="showEditTaskModal(task)">
                            <div class="task-title">
                                {{ task.title }}
                                <span v-if="task.action === 'ai'" class="badge badge-ai">🤖 AI</span>
                                <span v-if="task.scheduleType === 'cycle'" class="badge badge-cycle">🔁 {{ cycleLabel(task) }}</span>
                                <span v-if="task.scheduledStart && task.scheduleType !== 'cycle'" class="badge badge-schedule">🚀 {{ formatSchedule(task.scheduledStart) }}</span>
                            </div>
                            <div class="task-meta">
                                <span :class="'priority-' + task.priority">{{ priorityEmoji(task.priority) }}</span>
                                <span v-if="task.dueDate" class="task-due">📅 {{ formatDate(task.dueDate) }}</span>
                                <span v-if="task.creatorName" class="task-due">👤 {{ task.creatorName }}</span>
                            </div>
                            <div v-if="task.action === 'ai'" class="task-actions" @click.stop>
                                <el-button size="small" plain @click="showTaskExecutions(task)">📜 执行记录</el-button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="ai-hint">
                💡 <b>AI 任务</b>：任务到期时 AI 会自动执行你填写的指令（查数据、生成报告），结果推送到飞书；多个任务同时触发时会进入<b>执行队列</b>依次处理，可在卡片上查看排队位置与<b>执行记录</b>。
                可设置「一次性」定时启动，或「循环执行」（每天/每周/每月/Cron）让任务按周期自动运行，循环任务执行完成后自动回到待办等待下一周期。
                普通任务到期/逾期会自动推送提醒，完成后自动沉淀到「任务完成记录」数据集，供日报引用。
            </div>
        </div>

        <div class="card">
            <div class="card-header">
                <h2 class="card-title">📜 任务执行记录</h2>
                <span class="exec-total">共 {{ execTotal }} 条</span>
            </div>
            <el-table :data="execPageItems" v-loading="loadingExec" size="small">
                <el-table-column label="状态" width="110">
                    <template #default="{ row }">
                        <span class="exec-status" :class="'exec-status-' + row.status">{{ statusLabel(row.status) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="任务" prop="taskTitle" min-width="180" show-overflow-tooltip />
                <el-table-column label="触发" width="150">
                    <template #default="{ row }">
                        {{ triggerLabel(row.triggerType) }}<br />
                        <span class="exec-muted">{{ row.triggeredBy || '-' }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="开始时间" prop="startTime" width="160">
                    <template #default="{ row }">{{ row.startTime || '-' }}</template>
                </el-table-column>
                <el-table-column label="结束时间" prop="endTime" width="160">
                    <template #default="{ row }">{{ row.endTime || '-' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="90" fixed="right">
                    <template #default="{ row }">
                        <el-button size="small" text type="primary" @click="showExecDetail(row)">查看结果</el-button>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty description="暂无执行记录" :image-size="60" />
                </template>
            </el-table>
            <div class="pager">
                <el-pagination
                    background
                    layout="total, prev, pager, next"
                    :total="execTotal"
                    :page-size="execPageSize"
                    :current-page="execPage"
                    @current-change="onExecPageChange"
                />
            </div>
        </div>

        <el-dialog v-model="taskModal" :title="isEditingTask ? '编辑任务' : '添加任务'" width="560px" destroy-on-close>
            <el-form label-position="top" size="small">
                <el-form-item label="标题 *">
                    <el-input v-model="editingTask.title" placeholder="任务标题" />
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="editingTask.description" type="textarea" :rows="3" placeholder="任务描述（可选）" />
                </el-form-item>
                <div class="form-row">
                    <el-form-item label="优先级">
                        <el-select v-model="editingTask.priority" style="width: 100%">
                            <el-option value="high" label="🔴 高优先级" />
                            <el-option value="mid" label="🟡 中优先级" />
                            <el-option value="low" label="🟢 低优先级" />
                        </el-select>
                    </el-form-item>
                    <el-form-item v-if="editingTask.scheduleType !== 'cycle'" label="截止日期">
                        <el-date-picker v-model="editingTask.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
                    </el-form-item>
                </div>
                <el-form-item label="任务类型">
                    <el-select v-model="editingTask.action" style="width: 100%">
                        <el-option value="" label="📋 普通任务（到期/逾期推送提醒）" />
                        <el-option value="ai" label="🤖 AI 任务（到期自动执行 AI 指令）" />
                    </el-select>
                </el-form-item>
                <el-form-item label="📅 调度类型">
                    <el-select v-model="editingTask.scheduleType" style="width: 100%">
                        <el-option value="" label="⏸ 不自动调度（仅手动/到期触发）" />
                        <el-option value="once" label="⏰ 一次性（指定时间触发一次）" />
                        <el-option value="cycle" label="🔁 循环执行（按周期重复触发）" />
                    </el-select>
                </el-form-item>
                <el-form-item v-if="editingTask.scheduleType === 'once'" label="⏰ 触发时间">
                    <el-date-picker v-model="editingTask.scheduledStart" type="datetime" value-format="YYYY-MM-DDTHH:mm" placeholder="到点自动开始执行/提醒" style="width: 100%" />
                </el-form-item>
                <template v-if="editingTask.scheduleType === 'cycle'">
                    <div class="form-row">
                        <el-form-item label="周期类型">
                            <el-select v-model="editingTask.cycleType" style="width: 100%">
                                <el-option value="daily" label="📆 每天" />
                                <el-option value="weekly" label="📅 每周（指定星期）" />
                                <el-option value="monthly" label="🗓 每月（指定日期）" />
                                <el-option value="cron" label="⚙️ Cron 表达式" />
                            </el-select>
                        </el-form-item>
                        <el-form-item v-if="editingTask.cycleType !== 'cron'" label="执行时间">
                            <el-time-select v-model="editingTask.cycleTime" start="00:00" step="00:30" end="23:30" placeholder="选择时间" style="width: 100%" />
                        </el-form-item>
                    </div>
                    <el-form-item v-if="editingTask.cycleType === 'weekly'" label="每周执行日（可多选）">
                        <el-checkbox-group v-model="cycleWeekDays">
                            <el-checkbox v-for="d in weekDays" :key="d.value" :value="d.value" :label="d.label" />
                        </el-checkbox-group>
                    </el-form-item>
                    <el-form-item v-if="editingTask.cycleType === 'monthly'" label="每月执行日期（逗号分隔，如 1,15）">
                        <el-input v-model="editingTask.cycleValue" placeholder="1,15" />
                    </el-form-item>
                    <el-form-item v-if="editingTask.cycleType === 'cron'" label="Cron 表达式（秒 分 时 日 月 周）">
                        <el-input v-model="editingTask.cycleValue" placeholder="0 0 9 * * MON-FRI" />
                    </el-form-item>
                    <el-form-item label="循环结束日期（选填，到期自动转为已完成）">
                        <el-date-picker v-model="editingTask.cycleEnd" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
                    </el-form-item>
                </template>
                <el-form-item v-if="editingTask.action === 'ai'" label="AI 执行指令 *">
                    <el-input v-model="editingTask.actionPrompt" type="textarea" :rows="3" placeholder="例如：查询本周完成任务，汇总生成周报；或：搜索数据集新增记录并整理汇报" />
                    <div class="ai-hint">到期时 AI 会自动执行此指令，结果推送到飞书。留空则默认按任务标题/描述执行。</div>
                </el-form-item>
                <el-form-item v-if="isEditingTask" label="状态">
                    <el-select v-model="editingTask.status" style="width: 100%">
                        <el-option value="pending" label="📋 待办" />
                        <el-option value="in_progress" label="🔄 进行中" />
                        <el-option value="done" label="✅ 已完成" />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button v-if="isEditingTask" size="small" type="danger" @click="openDeleteModal" style="margin-right: auto">删除</el-button>
                <el-button size="small" @click="taskModal = false">取消</el-button>
                <el-button size="small" type="primary" :loading="saving" @click="saveTask">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="taskExecModal" :title="'📜 执行记录 - ' + executionsTaskTitle" width="820px">
            <el-table :data="executions" size="small">
                <el-table-column label="状态" width="110">
                    <template #default="{ row }">
                        <span class="exec-status" :class="'exec-status-' + row.status">{{ statusLabel(row.status) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="触发" width="170">
                    <template #default="{ row }">
                        {{ triggerLabel(row.triggerType) }}<br />
                        <span class="exec-muted">{{ row.triggeredBy || '-' }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="开始时间" prop="startTime" width="170">
                    <template #default="{ row }">{{ row.startTime || '-' }}</template>
                </el-table-column>
                <el-table-column label="结束时间" prop="endTime" width="170">
                    <template #default="{ row }">{{ row.endTime || '-' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="90">
                    <template #default="{ row }">
                        <el-button size="small" text type="primary" @click="showExecDetail(row)">查看结果</el-button>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty description="暂无执行记录" :image-size="60" />
                </template>
            </el-table>
            <template #footer>
                <el-button size="small" @click="taskExecModal = false">关闭</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="execDetailModal" :title="'📜 执行详情 - ' + (execDetail?.taskTitle || '')" width="720px">
            <div v-if="execDetail">
                <div class="exec-head">
                    <span class="exec-status" :class="'exec-status-' + execDetail.status">{{ statusLabel(execDetail.status) }}</span>
                    <span class="exec-muted">触发: {{ triggerLabel(execDetail.triggerType) }} / {{ execDetail.triggeredBy || '-' }}</span>
                    <span class="exec-muted">开始: {{ execDetail.startTime || '-' }}</span>
                    <span class="exec-muted">结束: {{ execDetail.endTime || '-' }}</span>
                </div>
                <div v-if="execDetail.logText" class="exec-log">{{ execDetail.logText }}</div>
                <div v-if="execDetail.resultText" class="exec-result markdown-content" v-html="renderMd(execDetail.resultText)"></div>
                <div v-if="execDetail.errorMessage" class="exec-error">❌ {{ execDetail.errorMessage }}</div>
            </div>
            <template #footer>
                <el-button size="small" @click="execDetailModal = false">关闭</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { marked } from 'marked';
import { apiFetch, apiError } from '../api/client';

interface TaskItem {
    id: number;
    title: string;
    description?: string;
    status: string;
    priority: string;
    dueDate?: string;
    action?: string;
    actionPrompt?: string;
    scheduledStart?: string;
    creatorName?: string;
    scheduleType?: string;
    cycleType?: string;
    cycleValue?: string;
    cycleTime?: string;
    cycleEnd?: string;
    queuePosition?: number;
    queueStatus?: string;
}

interface TaskExecution {
    executionId: string;
    taskId: number;
    taskTitle: string;
    status: string;
    triggerType: string;
    triggeredBy?: string;
    startTime?: string;
    endTime?: string;
    logText?: string;
    resultText?: string;
    errorMessage?: string;
}

const tasks = ref<TaskItem[]>([]);

const taskModal = ref(false);
const isEditingTask = ref(false);
const saving = ref(false);
const editingTask = reactive({
    id: null as number | null,
    title: '',
    description: '',
    priority: 'mid',
    dueDate: '',
    status: 'pending',
    action: '',
    actionPrompt: '',
    scheduledStart: '',
    scheduleType: '',
    cycleType: 'daily',
    cycleValue: '',
    cycleTime: '',
    cycleEnd: '',
});

const weekDays = [
    { value: '1', label: '周一' },
    { value: '2', label: '周二' },
    { value: '3', label: '周三' },
    { value: '4', label: '周四' },
    { value: '5', label: '周五' },
    { value: '6', label: '周六' },
    { value: '7', label: '周日' },
];
const cycleWeekDays = ref<string[]>([]);

const executions = ref<TaskExecution[]>([]);
const taskExecModal = ref(false);
const executionsTaskTitle = ref('');
const execDetailModal = ref(false);
const execDetail = ref<TaskExecution | null>(null);

const execPage = ref(1);
const execPageSize = 10;
const execPageItems = ref<TaskExecution[]>([]);
const execTotal = ref(0);
const loadingExec = ref(false);

const pendingTasks = computed(() => tasks.value.filter((t) => t.status === 'pending'));
const progressTasks = computed(() => tasks.value.filter((t) => t.status === 'in_progress'));
const doneTasks = computed(() => tasks.value.filter((t) => t.status === 'done'));

function todayStr(): string {
    const d = new Date();
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
}

function isDueToday(task: TaskItem): boolean {
    if (!task.dueDate || task.status === 'done') return false;
    return task.dueDate === todayStr();
}

function isOverdue(task: TaskItem): boolean {
    if (!task.dueDate || task.status === 'done') return false;
    return task.dueDate < todayStr();
}

function priorityEmoji(p: string): string {
    return p === 'high' ? '🔴' : p === 'mid' ? '🟡' : '🟢';
}

function toServerDateTime(v: string): string {
    if (!v) return '';
    const s = String(v).replace('T', ' ');
    return s.length === 16 ? s + ':00' : s;
}

function toLocalDateTime(v: string): string {
    if (!v) return '';
    let s = String(v).replace(' ', 'T');
    if (s.length === 19) s = s.substring(0, 16);
    return s;
}

async function loadTasks(): Promise<void> {
    try {
        const d = await apiFetch('/api/tasks');
        if (d.ok && Array.isArray(d.tasks)) {
            tasks.value = d.tasks;
        } else {
            tasks.value = [];
        }
    } catch (e) {
        tasks.value = [];
    }
}

function showAddTaskModal(): void {
    isEditingTask.value = false;
    Object.assign(editingTask, {
        id: null, title: '', description: '', priority: 'mid', dueDate: '', status: 'pending',
        action: '', actionPrompt: '', scheduledStart: '', scheduleType: '', cycleType: 'daily',
        cycleValue: '', cycleTime: '', cycleEnd: '',
    });
    cycleWeekDays.value = [];
    taskModal.value = true;
}

function showEditTaskModal(task: TaskItem): void {
    isEditingTask.value = true;
    Object.assign(editingTask, {
        id: task.id, title: task.title, description: task.description ?? '', priority: task.priority,
        dueDate: task.dueDate ?? '', status: task.status, action: task.action ?? '',
        actionPrompt: task.actionPrompt ?? '', scheduledStart: toLocalDateTime(task.scheduledStart ?? ''),
        scheduleType: task.scheduleType ?? '', cycleType: task.cycleType || 'daily',
        cycleValue: task.cycleValue ?? '', cycleTime: task.cycleTime ?? '', cycleEnd: task.cycleEnd ?? '',
    });
    cycleWeekDays.value = editingTask.cycleValue ? editingTask.cycleValue.split(',').filter(Boolean) : [];
    taskModal.value = true;
}

async function saveTask(): Promise<void> {
    if (!editingTask.title.trim()) {
        ElMessage.error('请输入任务标题');
        return;
    }
    if (editingTask.action === 'ai' && !editingTask.actionPrompt.trim()) {
        ElMessage.error('AI 任务请填写 AI 执行指令');
        return;
    }
    if (editingTask.scheduleType === 'once' && !editingTask.scheduledStart) {
        ElMessage.error('请选择一次性调度的触发时间');
        return;
    }
    if (editingTask.scheduleType === 'cycle') {
        if (editingTask.cycleType === 'cron') {
            if (!editingTask.cycleValue.trim()) {
                ElMessage.error('请填写 Cron 表达式');
                return;
            }
        } else if (editingTask.cycleType === 'weekly') {
            if (cycleWeekDays.value.length === 0) {
                ElMessage.error('请至少选择一个执行日');
                return;
            }
            editingTask.cycleValue = cycleWeekDays.value.join(',');
        } else if (editingTask.cycleType === 'monthly') {
            if (!editingTask.cycleValue.trim()) {
                ElMessage.error('请填写每月执行日期');
                return;
            }
        }
        if (!editingTask.cycleTime) {
            ElMessage.error('请选择执行时间');
            return;
        }
    }

    const url = editingTask.id ? '/api/tasks/update' : '/api/tasks/add';
    const formData = new FormData();
    formData.append('title', editingTask.title);
    formData.append('description', editingTask.description || '');
    formData.append('priority', editingTask.priority);
    formData.append('dueDate', editingTask.scheduleType === 'cycle' ? '' : (editingTask.dueDate || ''));
    formData.append('action', editingTask.action || '');
    formData.append('actionPrompt', editingTask.actionPrompt || '');
    formData.append('scheduledStart', editingTask.scheduleType === 'once' ? (toServerDateTime(editingTask.scheduledStart) || '') : '');
    formData.append('scheduleType', editingTask.scheduleType || '');
    formData.append('cycleType', editingTask.cycleType || '');
    formData.append('cycleValue', editingTask.cycleValue || '');
    formData.append('cycleTime', editingTask.cycleTime || '');
    formData.append('cycleEnd', editingTask.cycleEnd || '');
    if (editingTask.id) {
        formData.append('id', String(editingTask.id));
        formData.append('status', editingTask.status);
    }
    saving.value = true;
    try {
        const d = await apiFetch(url, { method: 'POST', body: formData });
        if (d.ok) {
            taskModal.value = false;
            ElMessage.success(editingTask.id ? '✅ 任务已更新' : '✅ 任务已创建');
            await loadTasks();
        } else {
            ElMessage.error('❌ ' + apiError(d, '保存失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 保存失败');
    } finally {
        saving.value = false;
    }
}

async function executeTask(task: TaskItem): Promise<void> {
    try {
        await ElMessageBox.confirm(`确定让 AI 立即执行任务「${task.title}」吗？执行结果将推送到飞书。`, '立即执行', {
            type: 'warning',
            confirmButtonText: '执行',
            cancelButtonText: '取消',
        });
    } catch (e) {
        return;
    }
    try {
        const d = await apiFetch(`/api/tasks/execute?id=${task.id}`, { method: 'POST' });
        if (d.ok) {
            if (d.position && d.position > 1) {
                ElMessage.success(`⏳ 已进入执行队列，当前排队位置: #${d.position}（执行完成后结果推送到飞书）`);
            } else {
                ElMessage.success('🤖 已触发 AI 执行，完成后结果会推送到飞书');
            }
            await loadTasks();
        } else {
            ElMessage.error('❌ ' + apiError(d, '触发失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 触发失败');
    }
}

async function cancelQueuedTask(task: TaskItem): Promise<void> {
    try {
        await ElMessageBox.confirm(`确定取消任务「${task.title}」的排队执行吗？`, '取消排队', {
            type: 'warning',
            confirmButtonText: '取消排队',
            cancelButtonText: '再想想',
        });
    } catch (e) {
        return;
    }
    try {
        const d = await apiFetch(`/api/tasks/cancel-queue?id=${task.id}`, { method: 'POST' });
        if (d.ok) {
            ElMessage.success('✅ 已取消排队');
            await loadTasks();
        } else {
            ElMessage.error('❌ ' + apiError(d, '取消失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 取消失败');
    }
}

async function showTaskExecutions(task: TaskItem): Promise<void> {
    executionsTaskTitle.value = task.title;
    executions.value = [];
    taskExecModal.value = true;
    try {
        const d = await apiFetch(`/api/tasks/executions?taskId=${task.id}`);
        executions.value = d.ok && Array.isArray(d.executions) ? d.executions : [];
    } catch (e) {
        executions.value = [];
    }
}

function showExecDetail(ex: TaskExecution): void {
    execDetail.value = ex;
    execDetailModal.value = true;
}

async function loadExecutionsPage(): Promise<void> {
    loadingExec.value = true;
    try {
        const d = await apiFetch(`/api/tasks/executions?page=${execPage.value}&pageSize=${execPageSize}`);
        if (d.ok) {
            execPageItems.value = Array.isArray(d.executions) ? d.executions : [];
            execTotal.value = d.total || 0;
            const totalPages = Math.max(1, Math.ceil(execTotal.value / execPageSize));
            if (execPage.value > totalPages && totalPages >= 1) {
                execPage.value = totalPages;
                return loadExecutionsPage();
            }
        } else {
            execPageItems.value = [];
            execTotal.value = 0;
        }
    } catch (e) {
        execPageItems.value = [];
        execTotal.value = 0;
    } finally {
        loadingExec.value = false;
    }
}

function onExecPageChange(p: number): void {
    execPage.value = p;
    loadExecutionsPage();
}

async function openDeleteModal(): Promise<void> {
    if (editingTask.id == null) return;
    try {
        await ElMessageBox.confirm('确定要删除这个任务吗？此操作不可撤销。', '确认删除', {
            type: 'warning',
            confirmButtonText: '删除',
            cancelButtonText: '取消',
        });
    } catch (e) {
        return;
    }
    try {
        const d = await apiFetch(`/api/tasks/delete?id=${editingTask.id}`, { method: 'POST' });
        if (d.ok) {
            taskModal.value = false;
            ElMessage.success('✅ 任务已删除');
            await loadTasks();
        } else {
            ElMessage.error('❌ ' + apiError(d, '删除失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 删除失败');
    }
}

function renderMd(text: string): string {
    if (!text) return '';
    try {
        return marked.parse(text) as string;
    } catch (e) {
        return String(text);
    }
}

function statusLabel(s: string): string {
    return { QUEUED: '⏳ 排队中', RUNNING: '⚙️ 执行中', SUCCESS: '✅ 成功', FAILED: '❌ 失败', CANCELLED: '🚫 已取消' }[s] || s;
}

function triggerLabel(t: string): string {
    return { manual: '手动', scheduled: '定时', due: '到期自动' }[t] || t || '-';
}

function formatSchedule(v: string): string {
    if (!v) return '';
    return v.substring(5, 16).replace('-', '/');
}

function cycleLabel(task: TaskItem): string {
    if (task.cycleType === 'cron') return '循环 ' + (task.cycleValue || '');
    if (task.cycleType === 'weekly') return '循环 周' + (task.cycleValue || '');
    if (task.cycleType === 'monthly') return '循环 每月' + (task.cycleValue || '') + '号';
    return '循环 ' + (task.cycleTime || '');
}

function formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.getMonth() + 1 + '/' + date.getDate();
}

let refreshTimer: number | null = null;

onMounted(() => {
    loadTasks();
    loadExecutionsPage();
    refreshTimer = window.setInterval(() => {
        if (!document.hidden) {
            loadTasks();
            loadExecutionsPage();
        }
    }, 15000);
});

onUnmounted(() => {
    if (refreshTimer !== null) clearInterval(refreshTimer);
});
</script>

<style scoped>
.card { background: white; border-radius: var(--radius-md); border: 1px solid var(--border); box-shadow: var(--shadow-sm); padding: 24px; margin-bottom: 24px; }
.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.card-title { font-size: 16px; font-weight: 600; margin: 0; }
.exec-total { font-size: 13px; color: var(--text-muted); }

.task-board { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.task-column { background: #f8fafc; border-radius: var(--radius-md); padding: 16px; }
.column-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.column-title { font-size: 14px; font-weight: 600; }
.column-count { padding: 2px 8px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.column-count.pending { background: rgba(251, 146, 60, 0.1); color: #fb923c; }
.column-count.progress { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.column-count.done { background: rgba(34, 197, 94, 0.1); color: #22c55e; }

.task-card { background: white; border-radius: var(--radius-sm); padding: 14px; margin-bottom: 12px; cursor: pointer; transition: all 0.2s; border: 1px solid var(--border); }
.task-card:hover { box-shadow: var(--shadow-md); }
.task-title { font-size: 14px; font-weight: 500; margin-bottom: 8px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.task-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text-muted); flex-wrap: wrap; }
.priority-high { color: #ef4444; }
.priority-mid { color: #fb923c; }
.priority-low { color: #22c55e; }
.task-due { display: flex; align-items: center; gap: 2px; }
.badge { padding: 1px 8px; border-radius: 10px; font-size: 11px; font-weight: 500; }
.badge-ai { background: rgba(139, 92, 246, 0.12); color: #8b5cf6; }
.badge-due { background: rgba(59, 130, 246, 0.12); color: #3b82f6; }
.badge-overdue { background: rgba(239, 68, 68, 0.12); color: #ef4444; }
.badge-queue { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
.badge-schedule { background: rgba(16, 185, 129, 0.12); color: #10b981; }
.badge-cycle { background: rgba(168, 85, 247, 0.12); color: #a855f7; }
.badge-running { background: rgba(139, 92, 246, 0.15); color: #8b5cf6; }
.task-actions { margin-top: 10px; display: flex; gap: 6px; flex-wrap: wrap; }

.exec-status { padding: 1px 8px; border-radius: 10px; font-size: 11px; font-weight: 500; white-space: nowrap; }
.exec-status-QUEUED { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
.exec-status-RUNNING { background: rgba(139, 92, 246, 0.12); color: #8b5cf6; }
.exec-status-SUCCESS { background: rgba(34, 197, 94, 0.12); color: #22c55e; }
.exec-status-FAILED { background: rgba(239, 68, 68, 0.12); color: #ef4444; }
.exec-status-CANCELLED { background: rgba(148, 163, 184, 0.15); color: #64748b; }
.exec-muted { font-size: 12px; color: var(--text-muted); }
.exec-log { font-size: 12px; line-height: 1.7; background: #0f172a; color: #e2e8f0; border-radius: var(--radius-sm); padding: 10px 12px; margin-top: 8px; white-space: pre-wrap; word-break: break-all; max-height: 260px; overflow-y: auto; }
.exec-result { font-size: 12px; line-height: 1.7; background: #ecfdf5; border: 1px solid #a7f3d0; color: #065f46; border-radius: var(--radius-sm); padding: 10px 12px; margin-top: 8px; white-space: pre-wrap; word-break: break-all; max-height: 260px; overflow-y: auto; }
.exec-error { font-size: 12px; line-height: 1.7; background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; border-radius: var(--radius-sm); padding: 10px 12px; margin-top: 8px; white-space: pre-wrap; word-break: break-all; max-height: 200px; overflow-y: auto; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
.form-row { display: flex; gap: 12px; }
.form-row .el-form-item { flex: 1; }
.ai-hint { background: rgba(139, 92, 246, 0.06); border: 1px dashed #c4b5fd; border-radius: var(--radius-sm); padding: 10px 14px; font-size: 12px; color: #7c3aed; margin-top: 8px; line-height: 1.6; }
.markdown-content { white-space: pre-wrap; word-break: break-word; }
.markdown-content p { margin-bottom: 8px; }
.markdown-content ul, .markdown-content ol { padding-left: 20px; margin-bottom: 8px; }
.markdown-content li { margin-bottom: 4px; }
.markdown-content code { background: rgba(0, 0, 0, 0.04); padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 13px; }
.markdown-content pre { background: #1e293b; color: #e2e8f0; padding: 12px; border-radius: 8px; overflow-x: auto; margin-bottom: 8px; font-family: monospace; font-size: 13px; }
.markdown-content pre code { background: transparent; padding: 0; }
.markdown-content blockquote { border-left: 3px solid #6366f1; padding-left: 10px; color: #64748b; margin-bottom: 8px; }
</style>
