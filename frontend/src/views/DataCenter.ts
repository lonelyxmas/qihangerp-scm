import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { apiFetch, apiError } from '../api/client';

interface FieldDef {
    name: string;
    displayName: string;
    type: string;
    options?: string[];
}

interface DataSetSchema {
    fields?: FieldDef[];
    typeOptions?: string[];
    statusOptions?: string[];
}

interface CollabConfig {
    feishuNotify?: boolean;
}

interface DataModule {
    id: string;
    name: string;
    description?: string;
    icon?: string;
}

interface DataSet {
    id: string;
    moduleId: string;
    name: string;
    description?: string;
    type?: string;
    status?: string;
    schema?: DataSetSchema;
    collabConfig?: CollabConfig;
    recordCount?: number;
}

const FIELD_TYPES: { value: string; label: string }[] = [
    { value: 'text', label: '文本' },
    { value: 'textarea', label: '多行文本' },
    { value: 'number', label: '数字' },
    { value: 'money', label: '金额' },
    { value: 'date', label: '日期' },
    { value: 'select', label: '下拉选项' },
    { value: 'user', label: '成员' },
];

const ICONS = ['📁','📊','📋','📝','📌','📈','📉','🗂️','🏷️','💼','👥','🏢','🛒','📦','🚀','🎯','⭐','🔥','💎','📚','📎','🔧','⚙️','🔄','📅','💰','📞','✉️','🔗','📑'];

const AUTO_KEY_RE = /^field_\d+$/;

interface FieldRow {
    displayName: string;
    name: string;
    type: string;
    options: string[];
    optionsText: string;
}

const modules = ref<DataModule[]>([]);
const allDatasets = ref<DataSet[]>([]);
const loading = ref(false);

const moduleModal = ref(false);
const editingModuleId = ref<string | null>(null);
const moduleForm = ref<{ name: string; description: string; icon: string }>({ name: '', description: '', icon: '📁' });

const datasetModal = ref(false);
const editingDsId = ref<string | null>(null);
const creatingDsModuleId = ref<string | null>(null);
const dsForm = ref<{ name: string; description: string; typeOptions: string; statusOptions: string; feishuNotify: boolean }>({
    name: '',
    description: '',
    typeOptions: '',
    statusOptions: '',
    feishuNotify: false,
});
const fieldRows = ref<FieldRow[]>([]);
const aiGenerating = ref(false);

const statusText = computed(() => `共 ${modules.value.length} 个模块 · ${allDatasets.value.length} 个数据集`);

function datasetsOf(moduleId: string): DataSet[] {
    return allDatasets.value.filter((d) => d.moduleId === moduleId);
}

async function loadDataCenter(): Promise<void> {
    loading.value = true;
    try {
        const [m, ds] = await Promise.all([
            apiFetch('/api/datacenter/modules'),
            apiFetch('/api/datacenter/datasets'),
        ]);
        modules.value = m.ok ? (m.data ?? []) : [];
        allDatasets.value = ds.ok ? (ds.data ?? []) : [];
    } catch (e) {
        ElMessage.error('❌ 加载失败');
    } finally {
        loading.value = false;
    }
}

// ========== 模块 ==========

function openCreateModule(): void {
    editingModuleId.value = null;
    moduleForm.value = { name: '', description: '', icon: '📁' };
    moduleModal.value = true;
}

function openEditModule(id: string): void {
    const m = modules.value.find((x) => x.id === id);
    if (!m) return;
    editingModuleId.value = id;
    moduleForm.value = { name: m.name, description: m.description ?? '', icon: m.icon || '📁' };
    moduleModal.value = true;
}

async function saveModule(): Promise<void> {
    const name = moduleForm.value.name.trim();
    if (!name) {
        ElMessage.error('请输入模块名称');
        return;
    }
    const payload = { name, description: moduleForm.value.description, icon: moduleForm.value.icon };
    try {
        const url = editingModuleId.value ? `/api/datacenter/modules/${editingModuleId.value}` : '/api/datacenter/modules';
        const d = await apiFetch(url, {
            method: editingModuleId.value ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        if (d.ok) {
            moduleModal.value = false;
            ElMessage.success(editingModuleId.value ? '✅ 模块已更新' : '✅ 模块已创建');
            await loadDataCenter();
        } else {
            ElMessage.error('❌ ' + apiError(d, '保存失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 保存失败');
    }
}

async function deleteModule(id: string): Promise<void> {
    try {
        await ElMessageBox.confirm('确定删除该模块？', '删除模块', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' });
    } catch (e) {
        return;
    }
    const d = await apiFetch(`/api/datacenter/modules/${id}`, { method: 'DELETE' });
    if (d.ok) {
        ElMessage.success('✅ 模块已删除');
        await loadDataCenter();
    } else {
        ElMessage.error('❌ ' + apiError(d, '删除失败'));
    }
}

// ========== 数据集 ==========

function genFieldKey(idx: number): string {
    return 'field_' + (idx + 1);
}

function newFieldRow(): FieldRow {
    return { displayName: '', name: '', type: 'text', options: [], optionsText: '' };
}

function addFieldRow(): void {
    fieldRows.value.push(newFieldRow());
}

function removeFieldRow(idx: number): void {
    fieldRows.value.splice(idx, 1);
}

function syncOptions(row: FieldRow): void {
    if (row.type === 'select') {
        row.options = row.optionsText.split(',').map((s) => s.trim()).filter(Boolean);
    } else {
        row.options = [];
        row.optionsText = '';
    }
}

function onFieldTypeChange(row: FieldRow): void {
    if (row.type === 'select') {
        row.optionsText = (row.options ?? []).join(',');
    } else {
        row.options = [];
        row.optionsText = '';
    }
}

function onDisplayNameChange(row: FieldRow, idx: number): void {
    if (!row.name || AUTO_KEY_RE.test(row.name)) {
        row.name = genFieldKey(idx);
    }
}

function openCreateDataset(moduleId: string): void {
    editingDsId.value = null;
    creatingDsModuleId.value = moduleId;
    dsForm.value = { name: '', description: '', typeOptions: '', statusOptions: '', feishuNotify: false };
    fieldRows.value = [newFieldRow()];
    datasetModal.value = true;
}

function openEditDataset(id: string): void {
    const ds = allDatasets.value.find((d) => d.id === id);
    if (!ds) return;
    editingDsId.value = id;
    creatingDsModuleId.value = null;
    const schema = ds.schema ?? {};
    dsForm.value = {
        name: ds.name,
        description: ds.description ?? '',
        typeOptions: (schema.typeOptions ?? []).join('\n'),
        statusOptions: (schema.statusOptions ?? []).join('\n'),
        feishuNotify: (ds.collabConfig ?? {}).feishuNotify === true,
    };
    fieldRows.value = (schema.fields ?? []).map((f) => ({
        displayName: f.displayName ?? '',
        name: f.name ?? '',
        type: f.type || 'text',
        options: f.options ?? [],
        optionsText: (f.options ?? []).join(','),
    }));
    if (fieldRows.value.length === 0) fieldRows.value = [newFieldRow()];
    datasetModal.value = true;
}

async function aiGenerateKeys(): Promise<void> {
    const rows = fieldRows.value.filter((f) => (f.displayName || '').trim());
    if (rows.length === 0) {
        ElMessage.error('请先填写字段的中文显示名');
        return;
    }
    const names = rows.map((f) => f.displayName.trim());
    aiGenerating.value = true;
    try {
        const d = await apiFetch('/api/datacenter/llm/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                system: '你是数据建模助手。将中文字段名转换为英文标识符：小写字母开头，仅含字母、数字、下划线，语义清晰，如"客户名称"→customer_name、"报价金额"→quote_amount。',
                user: '为以下字段名生成英文标识符，只输出JSON对象（键为中文字段名，值为英文标识符）：' + JSON.stringify(names),
            }),
        });
        if (d.ok && d.reply) {
            let mapping: Record<string, string> | null = null;
            try {
                const clean = String(d.reply).trim().replace(/^```(?:json)?\s*/, '').replace(/```\s*$/, '');
                const start = clean.indexOf('{');
                const end = clean.lastIndexOf('}');
                if (start >= 0 && end > start) mapping = JSON.parse(clean.substring(start, end + 1));
            } catch (e) {
                mapping = null;
            }
            if (mapping) {
                let updated = 0;
                for (const row of fieldRows.value) {
                    const key = mapping[row.displayName.trim()];
                    if (key && (!row.name || AUTO_KEY_RE.test(row.name))) {
                        row.name = key;
                        updated++;
                    }
                }
                ElMessage.success(updated > 0 ? `✅ 已生成 ${updated} 个英文标识` : '✅ 完成，可手动修改');
            } else {
                ElMessage.error('AI 返回格式无法解析，可手动填写');
            }
        } else {
            ElMessage.error(apiError(d, 'AI 生成失败，请检查 LLM 配置'));
        }
    } catch (e) {
        ElMessage.error('AI 生成失败，请检查 LLM 配置');
    } finally {
        aiGenerating.value = false;
    }
}

async function saveDataset(): Promise<void> {
    const name = dsForm.value.name.trim();
    if (!name) {
        ElMessage.error('请输入数据集名称');
        return;
    }
    fieldRows.value.forEach(syncOptions);
    const fields: FieldDef[] = [];
    fieldRows.value.forEach((f) => {
        const displayName = (f.displayName || '').trim();
        const fname = (f.name || '').trim() || genFieldKey(fields.length);
        if (!displayName && !fname) return;
        const field: FieldDef = { name: fname, displayName: displayName || fname, type: f.type || 'text' };
        if (f.type === 'select' && f.options && f.options.length > 0) field.options = f.options;
        fields.push(field);
    });
    const typeOptions = dsForm.value.typeOptions.split('\n').map((s) => s.trim()).filter(Boolean);
    const statusOptions = dsForm.value.statusOptions.split('\n').map((s) => s.trim()).filter(Boolean);
    const payload: Record<string, any> = {
        name,
        description: dsForm.value.description,
        type: typeOptions.length > 0 ? typeOptions[0] : '',
        status: statusOptions.length > 0 ? statusOptions[0] : '',
        schema: { fields, typeOptions, statusOptions },
        collabConfig: { feishuNotify: dsForm.value.feishuNotify },
    };
    if (!editingDsId.value) payload.moduleId = creatingDsModuleId.value;
    try {
        const url = editingDsId.value ? `/api/datacenter/datasets/${editingDsId.value}` : '/api/datacenter/datasets';
        const d = await apiFetch(url, {
            method: editingDsId.value ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        if (d.ok) {
            datasetModal.value = false;
            ElMessage.success(editingDsId.value ? '✅ 数据集已更新' : '✅ 数据集已创建');
            await loadDataCenter();
        } else {
            ElMessage.error('❌ ' + apiError(d, '保存失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 保存失败');
    }
}

async function deleteDataset(id: string): Promise<void> {
    try {
        await ElMessageBox.confirm('确定删除该数据集？', '删除数据集', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' });
    } catch (e) {
        return;
    }
    const d = await apiFetch(`/api/datacenter/datasets/${id}`, { method: 'DELETE' });
    if (d.ok) {
        ElMessage.success('✅ 数据集已删除');
        await loadDataCenter();
    } else {
        ElMessage.error('❌ ' + apiError(d, '删除失败'));
    }
}

onMounted(loadDataCenter);

export {
    FIELD_TYPES,
    ICONS,
    modules,
    allDatasets,
    loading,
    statusText,
    moduleModal,
    editingModuleId,
    moduleForm,
    datasetModal,
    editingDsId,
    dsForm,
    fieldRows,
    aiGenerating,
    datasetsOf,
    openCreateModule,
    openEditModule,
    deleteModule,
    saveModule,
    openCreateDataset,
    openEditDataset,
    deleteDataset,
    addFieldRow,
    removeFieldRow,
    onFieldTypeChange,
    onDisplayNameChange,
    aiGenerateKeys,
    saveDataset,
};
