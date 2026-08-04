<template>
    <div>
        <div class="card">
            <div class="card-header">
                <h2 class="card-title">⏰ 定时提醒</h2>
                <div class="header-actions">
                    <span class="enabled-count">已启用: {{ enabledReminderCount }}</span>
                    <el-button type="primary" size="small" @click="showAddReminderModal">+ 添加提醒</el-button>
                </div>
            </div>
            <div v-if="reminders.length === 0" class="empty-box">
                <div class="empty-icon">⏰</div>
                <div class="empty-title">暂无提醒</div>
                <div class="empty-desc">点击上方「添加提醒」按钮创建一个新的定时提醒</div>
            </div>
            <div v-else class="reminder-grid">
                <div v-for="reminder in reminders" :key="reminder.id" class="reminder-card">
                    <div class="reminder-header">
                        <span class="reminder-name">{{ reminder.name }}</span>
                        <el-switch :model-value="!!reminder.enabled" size="small" @change="() => toggleReminder(reminder.id)" />
                    </div>
                    <div v-if="reminder.message" class="reminder-message">{{ reminder.message }}</div>
                    <div class="reminder-schedule">
                        <span>⏰</span>
                        <span>{{ getScheduleText(reminder) }}</span>
                    </div>
                    <div class="reminder-actions">
                        <el-button size="small" plain @click="showEditReminderModal(reminder.id)">编辑</el-button>
                        <el-button size="small" type="danger" plain @click="openDeleteModal(reminder.id)">删除</el-button>
                    </div>
                </div>
            </div>
        </div>

        <el-dialog v-model="reminderModal" :title="isEditingReminder ? '编辑提醒' : '添加提醒'" width="520px" destroy-on-close>
            <el-form label-position="top" size="small">
                <el-form-item label="提醒名称 *">
                    <el-input v-model="editingReminder.name" placeholder="例如：下班日报提醒" />
                </el-form-item>
                <el-form-item label="提醒消息">
                    <el-input v-model="editingReminder.message" type="textarea" :rows="3" placeholder="提醒内容（可选）" />
                </el-form-item>
                <el-form-item label="提醒类型 *">
                    <el-select v-model="editingReminder.type" style="width: 100%" @change="onReminderTypeChange">
                        <el-option value="daily" label="📅 每天 - 每天定时提醒" />
                        <el-option value="once" label="🔔 一次 - 指定日期提醒" />
                        <el-option value="weekly" label="📆 每周 - 每周特定星期几提醒" />
                        <el-option value="monthly" label="📅 每月 - 每月特定几号提醒" />
                        <el-option value="yearly" label="🎂 每年 - 每年特定日期提醒" />
                    </el-select>
                </el-form-item>
                <div class="form-row">
                    <el-form-item label="提醒时间 *">
                        <el-time-picker v-model="editingReminder.time" format="HH:mm" value-format="HH:mm" placeholder="选择时间" style="width: 100%" />
                    </el-form-item>
                    <el-form-item v-if="editingReminder.type === 'weekly'" label="星期几">
                        <el-select v-model="editingReminder.dayOfWeek" style="width: 100%">
                            <el-option v-for="d in weekDays" :key="d.value" :label="d.label" :value="d.value" />
                        </el-select>
                    </el-form-item>
                    <el-form-item v-if="editingReminder.type === 'once'" label="日期">
                        <el-date-picker v-model="editingReminder.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
                    </el-form-item>
                    <el-form-item v-if="editingReminder.type === 'monthly'" label="几号">
                        <el-input-number v-model="editingReminder.dayOfMonth" :min="1" :max="31" style="width: 100%" />
                    </el-form-item>
                    <el-form-item v-if="editingReminder.type === 'yearly'" label="几月几号">
                        <div class="yearly-picker">
                            <el-input-number v-model="reminderYearMonth" :min="1" :max="12" placeholder="月" />
                            <span>月</span>
                            <el-input-number v-model="reminderYearDay" :min="1" :max="31" placeholder="日" />
                            <span>日</span>
                        </div>
                    </el-form-item>
                </div>
            </el-form>
            <template #footer>
                <el-button size="small" @click="reminderModal = false">取消</el-button>
                <el-button size="small" type="primary" :loading="saving" @click="saveReminder">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { apiFetch, apiError } from '../api/client';

interface Reminder {
    id: number;
    name: string;
    message?: string;
    type: string;
    time: string;
    date?: string;
    dayOfWeek?: string;
    dayOfMonth?: string;
    monthDay?: string;
    enabled?: boolean;
}

const reminders = ref<Reminder[]>([]);

const reminderModal = ref(false);
const isEditingReminder = ref(false);
const saving = ref(false);
const editingReminder = reactive({
    id: null as number | null,
    name: '',
    message: '',
    type: 'daily',
    time: '09:00',
    date: '',
    dayOfWeek: '1',
    dayOfMonth: null as number | null,
    monthDay: '',
});
const reminderYearMonth = ref<number | null>(null);
const reminderYearDay = ref<number | null>(null);

const weekDays = [
    { value: '1', label: '周一' },
    { value: '2', label: '周二' },
    { value: '3', label: '周三' },
    { value: '4', label: '周四' },
    { value: '5', label: '周五' },
    { value: '6', label: '周六' },
    { value: '7', label: '周日' },
];

const enabledReminderCount = computed(() => reminders.value.filter((r) => r.enabled).length);

function todayStr(): string {
    const d = new Date();
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
}

async function loadReminders(): Promise<void> {
    try {
        const d = await apiFetch('/api/reminders');
        reminders.value = Array.isArray(d) ? d : [];
    } catch (e) {
        reminders.value = [];
    }
}

function showAddReminderModal(): void {
    isEditingReminder.value = false;
    Object.assign(editingReminder, {
        id: null, name: '', message: '', type: 'daily', time: '09:00',
        date: todayStr(), dayOfWeek: '1', dayOfMonth: null, monthDay: '',
    });
    reminderYearMonth.value = null;
    reminderYearDay.value = null;
    reminderModal.value = true;
}

function showEditReminderModal(id: number): void {
    const reminder = reminders.value.find((r) => r.id === id);
    if (!reminder) return;
    isEditingReminder.value = true;
    Object.assign(editingReminder, {
        id: reminder.id, name: reminder.name, message: reminder.message ?? '',
        type: reminder.type, time: reminder.time || '09:00', date: reminder.date ?? '',
        dayOfWeek: reminder.dayOfWeek || '1', dayOfMonth: reminder.dayOfMonth ? Number(reminder.dayOfMonth) : null,
        monthDay: reminder.monthDay ?? '',
    });
    const parts = (reminder.monthDay || '').split('-');
    reminderYearMonth.value = parts[0] ? Number(parts[0]) : null;
    reminderYearDay.value = parts[1] ? Number(parts[1]) : null;
    reminderModal.value = true;
}

function onReminderTypeChange(): void {
    if (editingReminder.type === 'once' && !editingReminder.date) {
        editingReminder.date = todayStr();
    }
}

async function saveReminder(): Promise<void> {
    if (!editingReminder.name.trim()) {
        ElMessage.error('请输入提醒名称');
        return;
    }
    const monthDay = reminderYearMonth.value && reminderYearDay.value
        ? reminderYearMonth.value + '-' + reminderYearDay.value
        : '';

    const url = editingReminder.id ? '/api/reminders/update' : '/api/reminders/add';
    const formData = new FormData();
    formData.append('name', editingReminder.name);
    formData.append('message', editingReminder.message || '');
    formData.append('type', editingReminder.type);
    formData.append('time', editingReminder.time || '09:00');
    formData.append('date', editingReminder.date || '');
    formData.append('dayOfWeek', editingReminder.dayOfWeek);
    formData.append('dayOfMonth', editingReminder.dayOfMonth ? String(editingReminder.dayOfMonth) : '');
    formData.append('monthDay', monthDay);
    if (editingReminder.id) formData.append('id', String(editingReminder.id));
    saving.value = true;
    try {
        const d = await apiFetch(url, { method: 'POST', body: formData });
        if (d.ok) {
            reminderModal.value = false;
            ElMessage.success(editingReminder.id ? '✅ 提醒已更新' : '✅ 提醒已创建');
            await loadReminders();
        } else {
            ElMessage.error('❌ ' + apiError(d, '保存失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 保存失败');
    } finally {
        saving.value = false;
    }
}

async function toggleReminder(id: number): Promise<void> {
    try {
        const d = await apiFetch(`/api/reminders/toggle?id=${id}`, { method: 'POST' });
        if (d.ok) {
            await loadReminders();
        } else {
            ElMessage.error('❌ ' + apiError(d, '切换失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 切换失败');
    }
}

async function openDeleteModal(id: number): Promise<void> {
    try {
        await ElMessageBox.confirm('确定要删除吗？此操作不可撤销。', '确认删除', {
            type: 'warning',
            confirmButtonText: '删除',
            cancelButtonText: '取消',
        });
    } catch (e) {
        return;
    }
    try {
        const d = await apiFetch(`/api/reminders/delete?id=${id}`, { method: 'POST' });
        if (d.ok) {
            ElMessage.success('✅ 提醒已删除');
            await loadReminders();
        } else {
            ElMessage.error('❌ ' + apiError(d, '删除失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 删除失败');
    }
}

function getScheduleText(reminder: Reminder): string {
    const time = reminder.time || '09:00';
    switch (reminder.type) {
        case 'daily':
            return '每天 ' + time;
        case 'once':
            return '一次 ' + (reminder.date || '') + ' ' + time;
        case 'weekly': {
            const days = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'];
            return '每周' + (days[parseInt(reminder.dayOfWeek || '1')] || '') + ' ' + time;
        }
        case 'monthly':
            return '每月' + (reminder.dayOfMonth || '1') + '号 ' + time;
        case 'yearly': {
            const parts = (reminder.monthDay || '').split('-');
            const label = parts[0] && parts[1] ? parts[0] + '月' + parts[1] + '日' : '每年';
            return label + ' ' + time;
        }
        default:
            return time;
    }
}

onMounted(loadReminders);
</script>

<style scoped>
.card { background: white; border-radius: var(--radius-md); border: 1px solid var(--border); box-shadow: var(--shadow-sm); padding: 24px; margin-bottom: 24px; }
.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.card-title { font-size: 16px; font-weight: 600; margin: 0; }
.header-actions { display: flex; align-items: center; gap: 12px; }
.enabled-count { font-size: 13px; color: #94a3b8; }

.reminder-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.reminder-card { background: white; border-radius: var(--radius-md); padding: 16px; border: 1px solid var(--border); transition: all 0.2s; }
.reminder-card:hover { box-shadow: var(--shadow-md); }
.reminder-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.reminder-name { font-size: 14px; font-weight: 600; }
.reminder-message { font-size: 13px; color: var(--text-secondary); margin-bottom: 8px; }
.reminder-schedule { font-size: 12px; color: var(--text-muted); display: flex; align-items: center; gap: 4px; }
.reminder-actions { margin-top: 12px; display: flex; gap: 8px; }

.empty-box { text-align: center; padding: 40px 20px; border: 2px dashed #e2e8f0; background: #f8fafc; border-radius: var(--radius-md); }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-title { font-size: 16px; font-weight: 500; margin-bottom: 8px; color: #64748b; }
.empty-desc { font-size: 14px; color: #94a3b8; }

.form-row { display: flex; gap: 12px; }
.form-row .el-form-item { flex: 1; }
.yearly-picker { display: flex; gap: 8px; align-items: center; }
</style>
