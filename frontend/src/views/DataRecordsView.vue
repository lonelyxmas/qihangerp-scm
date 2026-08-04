<template>
    <div class="records-layout">
        <aside class="records-sidebar">
            <div class="records-sidebar-header">
                <span>数据集</span>
                <span class="records-sidebar-count">{{ allDatasets.length }}</span>
            </div>
            <div class="records-sidebar-list">
                <template v-for="m in modules" :key="m.id">
                    <div class="records-module-item">{{ m.icon || '📁' }} {{ m.name }}</div>
                    <div
                        v-for="ds in datasetsOf(m.id)"
                        :key="ds.id"
                        class="records-ds-item"
                        :class="{ active: currentDsId === ds.id }"
                        @click="openDataset(ds)"
                    >
                        <span>📋</span>
                        <span class="records-ds-name">{{ ds.name }}</span>
                        <span class="records-ds-count">{{ ds.recordCount || 0 }} 条</span>
                    </div>
                </template>
                <el-empty v-if="allDatasets.length === 0" description="暂无数据集" :image-size="60" />
            </div>
        </aside>

        <main class="records-main">
            <template v-if="!currentDs">
                <el-empty description="在左侧选择一个数据集查看记录" :image-size="90">
                    <template #image>
                        <div class="records-empty-icon">📦</div>
                    </template>
                </el-empty>
            </template>
            <template v-else>
                <div class="records-toolbar">
                    <el-button type="primary" size="small" @click="openForm()">+ 新增记录</el-button>
                    <el-input
                        v-model="keyword"
                        size="small"
                        placeholder="搜索记录..."
                        clearable
                        class="records-search"
                        @keyup.enter="onSearch"
                        @clear="onSearch"
                    />
                    <el-button size="small" @click="onSearch">搜索</el-button>
                </div>

                <el-table :data="records" v-loading="loadingRecords" size="small" class="records-table">
                    <el-table-column label="编码" min-width="110">
                        <template #default="{ row }">{{ row.recordNum || row['编号'] || '-' }}</template>
                    </el-table-column>
                    <el-table-column label="类型" width="110">
                        <template #default="{ row }">
                            <el-tag size="small" effect="plain">{{ row.type || row['类型'] || '-' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="100">
                        <template #default="{ row }">
                            <el-tag size="small" effect="plain" type="info">{{ row.status || row['状态'] || '-' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column
                        v-for="f in customFields"
                        :key="f.name"
                        :label="f.displayName || f.name"
                        min-width="110"
                        show-overflow-tooltip
                    >
                        <template #default="{ row }">{{ row[f.name] ?? row[f.displayName || f.name] ?? '' }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="110" fixed="right">
                        <template #default="{ row }">
                            <el-button size="small" text type="primary" @click="viewRecord(row)">查看</el-button>
                            <el-button size="small" text type="primary" @click="openForm(row)">编辑</el-button>
                            <el-button size="small" text type="danger" @click="deleteRecord(row)">删除</el-button>
                        </template>
                    </el-table-column>
                    <template #empty>
                        <el-empty description="暂无记录">
                            <el-button type="primary" size="small" @click="openForm()">+ 新增记录</el-button>
                        </el-empty>
                    </template>
                </el-table>

                <div class="records-pager">
                    <el-pagination
                        background
                        layout="total, prev, pager, next"
                        :total="totalRecords"
                        :page-size="pageSize"
                        :current-page="currentPage + 1"
                        @current-change="onPageChange"
                    />
                </div>
            </template>
        </main>

        <el-dialog v-model="formModal" :title="editingRecordId ? '编辑记录' : '新增记录'" width="520px" destroy-on-close>
            <el-form label-position="top" size="small">
                <el-form-item label="编码">
                    <el-input v-model="recordForm.recordNum" placeholder="自动生成" />
                </el-form-item>
                <el-form-item label="类型">
                    <el-select v-model="recordForm.type" placeholder="请选择" clearable style="width: 100%">
                        <el-option v-for="t in schemaTypeOptions" :key="t" :label="t" :value="t" />
                    </el-select>
                </el-form-item>
                <template v-for="f in customFields" :key="f.name">
                    <el-form-item :label="f.displayName || f.name">
                        <el-select v-if="f.type === 'select'" v-model="recordForm[f.name]" placeholder="请选择" clearable style="width: 100%">
                            <el-option v-for="o in f.options || []" :key="o" :label="o" :value="o" />
                        </el-select>
                        <el-input-number
                            v-else-if="f.type === 'number' || f.type === 'money'"
                            v-model="recordForm[f.name]"
                            :precision="2"
                            :step="1"
                            style="width: 100%"
                        />
                        <el-date-picker
                            v-else-if="f.type === 'date'"
                            v-model="recordForm[f.name]"
                            type="date"
                            value-format="YYYY-MM-DD"
                            placeholder="请选择日期"
                            style="width: 100%"
                        />
                        <el-input v-else-if="f.type === 'textarea'" v-model="recordForm[f.name]" type="textarea" :rows="3" />
                        <el-input v-else v-model="recordForm[f.name]" />
                    </el-form-item>
                </template>
                <el-form-item label="状态">
                    <el-select v-model="recordForm.status" placeholder="请选择" clearable style="width: 100%">
                        <el-option v-for="s in schemaStatusOptions" :key="s" :label="s" :value="s" />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button size="small" @click="formModal = false">取消</el-button>
                <el-button size="small" type="primary" :loading="saving" @click="saveRecord">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="detailModal" title="记录详情" width="540px">
            <div class="records-detail">
                <div class="records-detail-row">
                    <span class="records-detail-label">编码</span>
                    <span>{{ viewingRecord?.recordNum || viewingRecord?.['编号'] || '无' }}</span>
                </div>
                <div class="records-detail-row">
                    <span class="records-detail-label">类型</span>
                    <el-tag size="small" effect="plain">{{ viewingRecord?.type || viewingRecord?.['类型'] || '无' }}</el-tag>
                </div>
                <div v-for="f in customFields" :key="f.name" class="records-detail-row">
                    <span class="records-detail-label">{{ f.displayName || f.name }}</span>
                    <span class="records-detail-value">{{ valueOf(viewingRecord, f) || '—' }}</span>
                </div>
                <div class="records-detail-row">
                    <span class="records-detail-label">状态</span>
                    <el-tag size="small" effect="plain" type="info">{{ viewingRecord?.status || viewingRecord?.['状态'] || '无' }}</el-tag>
                </div>
            </div>
            <template #footer>
                <el-button size="small" type="danger" @click="deleteFromDetail">删除</el-button>
                <el-button size="small" type="primary" @click="editFromDetail">编辑</el-button>
                <el-button size="small" @click="detailModal = false">关闭</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { apiFetch, apiError } from '../api/client';
import { allDatasets, datasetsOf, loadDataCenter, modules } from './DataCenter';

interface FieldDef {
    name: string;
    displayName: string;
    type: string;
    options?: string[];
}

interface DataSet {
    id: string;
    name: string;
    schema?: { fields?: FieldDef[]; typeOptions?: string[]; statusOptions?: string[] };
    recordCount?: number;
}

type RecordRow = Record<string, any> & { _id?: string; id?: string };

const records = ref<RecordRow[]>([]);
const totalRecords = ref(0);
const currentPage = ref(0);
const pageSize = 20;
const keyword = ref('');
const loadingRecords = ref(false);

const currentDs = ref<DataSet | null>(null);
const currentDsId = ref<string | null>(null);

const formModal = ref(false);
const detailModal = ref(false);
const editingRecordId = ref<string | null>(null);
const saving = ref(false);
const recordForm = reactive<Record<string, any>>({ recordNum: '', type: '', status: '' });
const viewingRecord = ref<RecordRow | null>(null);

const customFields = computed<FieldDef[]>(() => currentDs.value?.schema?.fields?.filter((f) => f.name !== 'status' && f.name !== 'type') ?? []);
const schemaTypeOptions = computed<string[]>(() => currentDs.value?.schema?.typeOptions ?? []);
const schemaStatusOptions = computed<string[]>(() => currentDs.value?.schema?.statusOptions ?? []);

watch(allDatasets, () => {
    if (currentDsId.value && allDatasets.value.length > 0) {
        const still = allDatasets.value.find((d) => d.id === currentDsId.value);
        if (still) currentDs.value = still;
    }
});

onMounted(async () => {
    await loadDataCenter();
    if (allDatasets.value.length > 0) {
        const lastId = localStorage.getItem('lastDatasetId');
        const target = allDatasets.value.find((d) => d.id === lastId) ?? allDatasets.value[0];
        if (target) await openDataset(target);
    }
});

async function openDataset(ds: DataSet): Promise<void> {
    currentDs.value = ds;
    currentDsId.value = ds.id;
    localStorage.setItem('lastDatasetId', ds.id);
    currentPage.value = 0;
    await loadRecords();
}

async function loadRecords(): Promise<void> {
    const ds = currentDs.value;
    if (!ds) return;
    loadingRecords.value = true;
    try {
        let url = `/api/datacenter/datasets/${ds.id}/records?page=${currentPage.value}&size=${pageSize}`;
        const kw = keyword.value.trim();
        if (kw) url += '&keyword=' + encodeURIComponent(kw);
        const d = await apiFetch(url);
        if (d.ok) {
            records.value = d.data ?? [];
            totalRecords.value = d.total ?? 0;
        } else {
            records.value = [];
            totalRecords.value = 0;
            ElMessage.error('❌ ' + apiError(d, '加载记录失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 加载记录失败');
    } finally {
        loadingRecords.value = false;
    }
}

function onSearch(): void {
    currentPage.value = 0;
    loadRecords();
}

function onPageChange(p: number): void {
    currentPage.value = p - 1;
    loadRecords();
}

function valueOf(row: RecordRow | null, f: FieldDef): string {
    if (!row) return '';
    const v = row[f.name] ?? row[f.displayName || f.name] ?? '';
    return v == null ? '' : String(v);
}

function recordId(row: RecordRow): string {
    return String(row._id ?? row.id ?? '');
}

function openForm(row?: RecordRow): void {
    editingRecordId.value = row ? recordId(row) : null;
    Object.keys(recordForm).forEach((k) => delete (recordForm as Record<string, any>)[k]);
    recordForm.recordNum = row ? String(row.recordNum ?? row['编号'] ?? '') : '';
    recordForm.type = row?.type ?? row?.['类型'] ?? '';
    recordForm.status = row?.status ?? row?.['状态'] ?? '';
    for (const f of customFields.value) {
        recordForm[f.name] = row ? (row[f.name] ?? row[f.displayName || f.name] ?? '') : '';
    }
    formModal.value = true;
}

async function saveRecord(): Promise<void> {
    const ds = currentDs.value;
    if (!ds) return;
    saving.value = true;
    try {
        const payload = { data: { ...recordForm } };
        const url = editingRecordId.value
            ? `/api/datacenter/datasets/${ds.id}/records/${editingRecordId.value}`
            : `/api/datacenter/datasets/${ds.id}/records`;
        const d = await apiFetch(url, {
            method: editingRecordId.value ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        if (d.ok) {
            formModal.value = false;
            ElMessage.success(editingRecordId.value ? '✅ 记录已更新' : '✅ 记录已新增');
            await loadRecords();
            await loadDataCenter();
        } else {
            ElMessage.error('❌ ' + apiError(d, '保存失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 保存失败');
    } finally {
        saving.value = false;
    }
}

async function deleteRecord(row: RecordRow): Promise<void> {
    const ds = currentDs.value;
    if (!ds) return;
    try {
        await ElMessageBox.confirm('确定要删除这条记录吗？', '删除记录', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' });
    } catch (e) {
        return;
    }
    try {
        const d = await apiFetch(`/api/datacenter/datasets/${ds.id}/records/${recordId(row)}`, { method: 'DELETE' });
        if (d.ok) {
            ElMessage.success('✅ 记录已删除');
            await loadRecords();
            await loadDataCenter();
        } else {
            ElMessage.error('❌ ' + apiError(d, '删除失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 删除失败');
    }
}

function viewRecord(row: RecordRow): void {
    viewingRecord.value = row;
    detailModal.value = true;
}

function editFromDetail(): void {
    if (!viewingRecord.value) return;
    const row = viewingRecord.value;
    detailModal.value = false;
    openForm(row);
}

function deleteFromDetail(): void {
    if (!viewingRecord.value) return;
    const row = viewingRecord.value;
    detailModal.value = false;
    deleteRecord(row);
}
</script>

<style scoped>
.records-layout { display: flex; height: 100%; margin: -24px; }
.records-sidebar {
    width: 240px; background: var(--bg-sidebar); border-right: 1px solid var(--border);
    display: flex; flex-direction: column; flex-shrink: 0;
}
.records-sidebar-header {
    padding: 12px 16px; border-bottom: 1px solid var(--border);
    display: flex; align-items: center; justify-content: space-between;
    font-size: 13px; font-weight: 600; color: var(--text-primary);
}
.records-sidebar-count {
    font-size: 11px; color: var(--text-muted); background: var(--hover);
    border-radius: 20px; padding: 1px 8px; font-weight: 400;
}
.records-sidebar-list { flex: 1; overflow-y: auto; padding: 4px 0; }
.records-module-item {
    display: flex; align-items: center; gap: 4px; padding: 4px 12px;
    color: #a0a0a0; font-size: 12px; margin: 0 4px;
}
.records-ds-item {
    display: flex; align-items: center; gap: 6px; padding: 8px 12px 8px 44px;
    cursor: pointer; color: var(--text-primary); font-size: 13px; font-weight: 500;
    border-bottom: 1px solid var(--border); transition: all 0.2s;
}
.records-ds-item:hover { background: var(--hover); }
.records-ds-item.active { background: rgba(99, 102, 241, 0.08); color: var(--primary); font-weight: 600; }
.records-ds-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.records-ds-count { font-size: 11px; color: var(--text-muted); margin-left: auto; font-weight: 400; }

.records-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; padding: 24px; min-width: 0; }
.records-toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.records-search { flex: 1; max-width: 280px; }
.records-table { width: 100%; }
.records-pager { margin-top: 16px; display: flex; justify-content: flex-end; }
.records-empty-icon { font-size: 60px; opacity: 0.5; }

.records-detail-row { display: flex; padding: 8px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.records-detail-row:last-child { border-bottom: none; }
.records-detail-label { width: 100px; color: var(--text-secondary); font-weight: 500; flex-shrink: 0; }
.records-detail-value { color: var(--text-primary); word-break: break-all; }
</style>
