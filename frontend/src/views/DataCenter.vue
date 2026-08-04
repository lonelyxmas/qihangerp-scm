<template>
    <div class="card">
        <div class="card-title">📦 数据中心</div>
        <div class="card-desc">管理数据模块和数据集。模块是分组容器，数据集是具体的业务数据表。</div>
        <div style="display: flex; gap: 8px; margin-bottom: 12px;">
            <el-button type="primary" size="small" @click="openCreateModule">+ 新建模块</el-button>
        </div>
        <div class="text-muted" style="font-size: 13px; margin-bottom: 12px;">{{ statusText }}</div>

        <div v-loading="loading" class="dc-loading-wrap">
            <div v-if="modules.length === 0" class="text-muted" style="padding: 12px 0; font-size: 13px;">暂无模块</div>
            <div v-for="m in modules" :key="m.id" class="dc-module">
                <div class="dc-module-header">
                    <span class="dc-module-icon">{{ m.icon || '📁' }}</span>
                    <span class="dc-module-name">{{ m.name }}</span>
                    <span class="text-muted dc-module-count">{{ datasetsOf(m.id).length }} 数据集</span>
                    <el-button text type="primary" size="small" @click="openEditModule(m.id)">✏️</el-button>
                    <el-button text type="danger" size="small" @click="deleteModule(m.id)">🗑️</el-button>
                </div>
                <div v-if="datasetsOf(m.id).length > 0" style="padding: 4px 0;">
                    <div v-for="ds in datasetsOf(m.id)" :key="ds.id" class="dc-ds-row">
                        <span>📋</span>
                        <span class="dc-ds-name">{{ ds.name }}</span>
                        <span class="text-muted dc-ds-count">{{ ds.recordCount || 0 }} 条</span>
                        <el-button text type="primary" size="small" @click="openEditDataset(ds.id)">✏️</el-button>
                        <el-button text type="danger" size="small" @click="deleteDataset(ds.id)">🗑️</el-button>
                    </div>
                </div>
                <div class="dc-module-footer">
                    <el-button type="primary" plain size="small" @click="openCreateDataset(m.id)">+ 新建数据集</el-button>
                </div>
            </div>
        </div>
    </div>

    <!-- 模块弹窗 -->
    <el-dialog v-model="moduleModal" :title="editingModuleId ? '编辑模块' : '新建模块'" width="420px" append-to-body>
        <div class="form-group">
            <label class="form-label">模块名称 *</label>
            <el-input v-model="moduleForm.name" placeholder="如：客户管理" />
        </div>
        <div class="form-group">
            <label class="form-label">描述</label>
            <el-input v-model="moduleForm.description" placeholder="模块用途说明" />
        </div>
        <div class="form-group">
            <label class="form-label">图标</label>
            <div class="icon-grid">
                <span
                    v-for="icon in ICONS"
                    :key="icon"
                    class="icon-option"
                    :class="{ active: moduleForm.icon === icon }"
                    @click="moduleForm.icon = icon"
                >{{ icon }}</span>
            </div>
        </div>
        <template #footer>
            <el-button @click="moduleModal = false">取消</el-button>
            <el-button type="primary" @click="saveModule">保存</el-button>
        </template>
    </el-dialog>

    <!-- 数据集弹窗 -->
    <el-dialog v-model="datasetModal" :title="editingDsId ? '编辑数据集' : '新建数据集'" width="820px" append-to-body>
        <div class="form-group">
            <label class="form-label">名称 *</label>
            <el-input v-model="dsForm.name" placeholder="如：客户信息" />
        </div>
        <div class="form-group">
            <div class="form-label-row">
                <label class="form-label">字段定义</label>
                <span style="display: flex; gap: 6px;">
                    <el-button size="small" :loading="aiGenerating" @click="aiGenerateKeys">✨ AI 生成英文标识</el-button>
                    <el-button size="small" type="primary" plain @click="addFieldRow">+ 添加字段</el-button>
                </span>
            </div>
            <el-table :data="fieldRows" size="small" border>
                <el-table-column label="中文显示名" min-width="150">
                    <template #default="{ row, $index }">
                        <el-input
                            v-model="row.displayName"
                            placeholder="如：客户名称"
                            @change="onDisplayNameChange(row, $index)"
                        />
                    </template>
                </el-table-column>
                <el-table-column label="英文标识" min-width="150">
                    <template #default="{ row }">
                        <el-input v-model="row.name" placeholder="自动生成，可修改" class="mono-input" />
                    </template>
                </el-table-column>
                <el-table-column label="类型" width="130">
                    <template #default="{ row }">
                        <el-select v-model="row.type" style="width: 100%;" @change="onFieldTypeChange(row)">
                            <el-option v-for="t in FIELD_TYPES" :key="t.value" :label="t.label" :value="t.value" />
                        </el-select>
                    </template>
                </el-table-column>
                <el-table-column label="选项（逗号分隔）" min-width="170">
                    <template #default="{ row }">
                        <el-input
                            v-if="row.type === 'select'"
                            v-model="row.optionsText"
                            placeholder="意向客户,已成交"
                        />
                        <span v-else class="text-muted">—</span>
                    </template>
                </el-table-column>
                <el-table-column width="46" align="center">
                    <template #default="{ $index }">
                        <el-button text type="danger" size="small" @click="removeFieldRow($index)">✕</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <div class="text-muted" style="font-size: 12px; margin-top: 4px;">
                英文标识是数据存储的 key（小写字母/数字/下划线），中文显示名用于界面展示。旧数据集的中文标识保留兼容。
            </div>
        </div>
        <div class="form-group">
            <label class="form-label">类型选项（每行一个）</label>
            <el-input v-model="dsForm.typeOptions" type="textarea" :rows="3" placeholder="需求&#10;Bug&#10;优化" />
        </div>
        <div class="form-group">
            <label class="form-label">状态选项（每行一个）</label>
            <el-input v-model="dsForm.statusOptions" type="textarea" :rows="3" placeholder="待办&#10;进行中&#10;已完成" />
        </div>
        <div class="form-group">
            <label class="form-label">描述</label>
            <el-input v-model="dsForm.description" type="textarea" :rows="2" />
        </div>
        <div class="notify-row">
            <div style="font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 8px;">🤝 通知配置</div>
            <el-switch v-model="dsForm.feishuNotify" active-text="飞书通知（数据变更时通过飞书群推送通知）" />
        </div>
        <template #footer>
            <el-button @click="datasetModal = false">取消</el-button>
            <el-button type="primary" @click="saveDataset">保存</el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import {
    modules,
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
    FIELD_TYPES,
    ICONS,
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
    loadDataCenter,
} from './DataCenter';

onMounted(loadDataCenter);
</script>

<style scoped>
.text-muted {
    color: var(--text-secondary);
    font-size: 13px;
}
.card-desc {
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 1.6;
    margin-bottom: 12px;
}
.card-title {
    margin-bottom: 16px;
}
.dc-loading-wrap {
    min-height: 40px;
}
.dc-module {
    border: 1px solid var(--border);
    border-radius: var(--radius-md);
    margin-bottom: 12px;
    overflow: hidden;
}
.dc-module-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    background: #f8fafc;
    border-bottom: 1px solid var(--border);
}
.dc-module-icon {
    font-size: 15px;
}
.dc-module-name {
    font-weight: 600;
    font-size: 14px;
    flex: 1;
}
.dc-module-count {
    font-size: 12px;
}
.dc-ds-row {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 14px 8px 44px;
    border-bottom: 1px solid #f0f0f0;
    font-size: 13px;
}
.dc-ds-name {
    flex: 1;
}
.dc-ds-count {
    font-size: 12px;
}
.dc-module-footer {
    padding: 8px 14px;
    border-top: 1px solid #f0f0f0;
}
.form-group {
    margin-bottom: 14px;
}
.form-label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    margin-bottom: 6px;
}
.form-label-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;
}
.icon-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    padding: 4px 0;
}
.icon-option {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border: 2px solid var(--border);
    border-radius: var(--radius-sm);
    cursor: pointer;
    font-size: 18px;
    transition: all 0.15s;
}
.icon-option:hover {
    border-color: var(--primary);
}
.icon-option.active {
    border-color: var(--primary);
    background: rgba(99, 102, 241, 0.1);
}
.mono-input :deep(.el-input__inner) {
    font-family: monospace;
}
.notify-row {
    margin-top: 16px;
    border-top: 1px solid var(--border);
    padding-top: 12px;
}
</style>
