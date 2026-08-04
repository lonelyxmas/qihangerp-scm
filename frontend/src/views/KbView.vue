<template>
    <div class="kb-layout">
        <div v-if="!hasKb" class="kb-no-kb">暂无知识库</div>
        <template v-else>
            <aside class="kb-sidebar">
                <div class="kb-sidebar-header">
                    <div class="kb-kb-list">
                        <div
                            v-for="kb in kbList"
                            :key="kb.id"
                            class="kb-kb-card"
                            :class="{ active: currentKbId === kb.id, single: kbList.length === 1 }"
                            @click="selectKb(kb.id)"
                        >{{ kb.name }}</div>
                    </div>
                    <el-button size="small" text circle title="刷新" @click="reloadAll">🔄</el-button>
                </div>
                <div class="kb-search-row">
                    <el-input v-model="searchQuery" size="small" placeholder="搜索文档名称或标签..." clearable @keyup.enter="doSearch" @clear="clearSearch" />
                    <el-button v-if="isSearchMode" size="small" text @click="clearSearch">✕</el-button>
                </div>
                <div class="kb-filter-row" v-if="fileTypes.length > 1">
                    <span class="kb-filter-chip" :class="{ active: !fileTypeFilter }" @click="fileTypeFilter = null">全部</span>
                    <span
                        v-for="ft in fileTypes"
                        :key="ft"
                        class="kb-filter-chip"
                        :class="{ active: fileTypeFilter === ft }"
                        @click="fileTypeFilter = ft"
                    >{{ ft }}</span>
                </div>
                <div class="kb-stats-row">
                    文档 <span>{{ documents.length }}</span> 分块 <span>{{ totalChunks }}</span> 标签 <span>{{ tagCount }}</span> 分类 <span>{{ categories.length }}</span>
                </div>
                <div v-if="isSearchMode" class="kb-search-result">找到 {{ filteredDocuments.length }} 个结果</div>
                <div class="kb-sidebar-body">
                    <el-empty v-if="!categoryTree.length" description="暂无文档" :image-size="50">
                        <el-button type="primary" size="small" @click="openUpload">上传文档</el-button>
                    </el-empty>
                    <template v-else>
                        <div v-for="group in categoryTree" :key="'g-' + group.id">
                            <div class="kb-cat-header" @click="toggleCategory(group.id)">
                                <span class="kb-cat-arrow">{{ expandedCategories[group.id] ? '▼' : '▶' }}</span>
                                <span class="kb-cat-name">{{ group.name }}</span>
                                <span class="kb-cat-count">({{ group.docs.length }})</span>
                                <span class="kb-cat-actions">
                                    <el-button size="small" text title="新建文档" @click.stop="openNewNoteInCategory(group.id)">＋</el-button>
                                    <el-button v-if="group.id > 0" size="small" text title="编辑分类" @click.stop="editCategory(group)">⚙</el-button>
                                </span>
                            </div>
                            <div v-show="expandedCategories[group.id]">
                                <div
                                    v-for="doc in group.docs"
                                    :key="doc.path"
                                    class="kb-doc-item"
                                    :class="{ active: selectedDoc && selectedDoc.path === doc.path }"
                                    @click="selectDoc(doc)"
                                >
                                    <div class="doc-info">
                                        <div class="doc-name">{{ fileIcon(doc.fileType) }} {{ doc.name }}</div>
                                        <div class="doc-meta">
                                            <span>{{ doc.fileType || 'md' }}</span>
                                            <span>{{ formatSize(doc.fileSize) }}</span>
                                            <span>{{ doc.createdBy || '-' }}</span>
                                        </div>
                                    </div>
                                    <div class="doc-actions">
                                        <el-button size="small" text type="danger" title="删除" @click.stop="deleteDoc(doc)">🗑</el-button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </template>
                </div>
                <div class="kb-sidebar-footer">
                    <el-button type="primary" size="small" style="flex: 1" @click="openNewMd">📝 新建 Markdown</el-button>
                    <el-button size="small" style="flex: 1" @click="openUpload">⬆ 上传</el-button>
                    <el-button size="small" style="flex: 1" @click="showAddCategory">＋ 分类</el-button>
                </div>
            </aside>

            <main class="kb-main">
                <template v-if="selectedDoc">
                    <div class="kb-main-header">
                        <div class="kb-main-title">
                            <span>{{ fileIcon(selectedDoc.fileType) }}</span>
                            <span>{{ selectedDoc.name }}</span>
                            <el-tag v-if="editing" size="small" type="warning" effect="plain" style="margin-left: 8px">编辑中</el-tag>
                        </div>
                        <div class="kb-main-path">{{ selectedDoc.path }}</div>
                    </div>
                    <div class="kb-main-body">
                        <div class="kb-meta-grid">
                            <div class="kb-meta-item"><div class="kb-meta-label">类型</div><div class="kb-meta-value">{{ selectedDoc.fileType || '-' }}</div></div>
                            <div class="kb-meta-item"><div class="kb-meta-label">大小</div><div class="kb-meta-value">{{ formatSize(selectedDoc.fileSize) }}</div></div>
                            <div class="kb-meta-item"><div class="kb-meta-label">分块</div><div class="kb-meta-value">{{ chunks.length }}</div></div>
                            <div class="kb-meta-item"><div class="kb-meta-label">创建人</div><div class="kb-meta-value">{{ selectedDoc.createdBy || '-' }}</div></div>
                            <div class="kb-meta-item"><div class="kb-meta-label">创建时间</div><div class="kb-meta-value">{{ selectedDoc.createdAt || '-' }}</div></div>
                            <div class="kb-meta-item"><div class="kb-meta-label">更新时间</div><div class="kb-meta-value">{{ selectedDoc.updatedAt || '-' }}</div></div>
                        </div>

                        <div class="kb-tag-editor">
                            <span class="kb-tag-editor-label">标签</span>
                            <template v-if="selectedDocTags.length">
                                <span v-for="(t, i) in selectedDocTags" :key="i" class="kb-tag" :class="'kb-tag-color-' + (i % 8)">
                                    {{ t }}
                                    <span class="kb-tag-remove" @click="removeTag(t)">✕</span>
                                </span>
                            </template>
                            <span v-else style="font-size: 12px; color: var(--text-muted)">无标签</span>
                            <el-input v-model="tagInput" size="small" class="kb-tag-input" placeholder="+ 添加" @keyup.enter="addTag" />
                        </div>

                        <div v-if="selectedDoc.originalFile" style="margin-bottom: 12px; display: flex; gap: 8px">
                            <el-button size="small" plain @click="downloadDoc(selectedDoc)">⬇ 下载原始文件</el-button>
                        </div>

                        <div class="kb-tabs">
                            <div class="kb-tab" :class="{ active: contentTab === 'content' }" @click="switchTab('content')">📄 文档内容</div>
                            <div class="kb-tab" :class="{ active: contentTab === 'chunks' }" @click="switchTab('chunks')">🧩 分块 ({{ chunks.length }})</div>
                        </div>

                        <div v-show="contentTab === 'content'">
                            <div style="margin-bottom: 10px; display: flex; gap: 8px">
                                <template v-if="!editing">
                                    <el-button type="primary" size="small" @click="startEditing">✏ 编辑</el-button>
                                </template>
                                <template v-else>
                                    <el-button type="success" size="small" :loading="savingDoc" @click="saveEditing">✔ 保存</el-button>
                                    <el-button size="small" @click="cancelEditing">✖ 取消</el-button>
                                </template>
                            </div>
                            <div v-if="docContentLoading" style="text-align: center; padding: 20px">
                                <el-icon class="is-loading" style="font-size: 22px"><Loading /></el-icon>
                            </div>
                            <el-input v-else-if="editing" v-model="editContent" type="textarea" class="kb-editor-textarea" />
                            <div v-else-if="docContent" class="kb-content-view" v-html="renderMd(docContent)"></div>
                            <div v-else style="text-align: center; padding: 40px; color: var(--text-muted); font-size: 13px">无内容</div>
                        </div>

                        <div v-show="contentTab === 'chunks'">
                            <div v-if="chunks.length">
                                <div v-for="(c, i) in chunks" :key="i" class="kb-chunk-item">
                                    <div class="kb-chunk-index">#{{ c.chunkIndex != null ? c.chunkIndex : i + 1 }}</div>
                                    <div class="kb-chunk-content" :class="{ expanded: chunkExpanded[i] }">{{ c.content }}</div>
                                    <span v-if="c.content && c.content.length > 200" class="kb-chunk-expand" @click="toggleChunk(i)">
                                        {{ chunkExpanded[i] ? '收起' : '展开全部' }}
                                    </span>
                                </div>
                            </div>
                            <div v-else-if="!chunksLoading" style="text-align: center; padding: 40px; color: var(--text-muted); font-size: 13px">暂无分块数据</div>
                        </div>
                    </div>
                </template>
                <div v-else class="kb-empty-detail">
                    <div class="kb-empty-detail-icon">📄</div>
                    <div class="kb-empty-detail-title">选择文档查看</div>
                    <div class="kb-empty-detail-desc">在左侧列表中选择一个文档查看详情和分块</div>
                </div>
            </main>
        </template>

        <el-dialog v-model="uploadOpen" title="上传文档" width="480px">
            <div class="kb-upload-zone" :class="{ dragover: dragOver }" @click="triggerFileInput" @dragover.prevent="dragOver = true" @dragleave.prevent="dragOver = false" @drop.prevent="onDrop">
                <div class="kb-upload-zone-icon">📄</div>
                <div class="kb-upload-zone-text">点击或拖拽文件到此处上传</div>
                <div class="kb-upload-zone-hint">支持 PDF、DOCX、TXT、Markdown 等格式</div>
                <input ref="fileInput" type="file" multiple hidden accept=".md,.pdf,.docx,.doc,.txt,.html,.htm,.xml,.csv,.rtf,.odt,.xlsx,.xls,.pptx,.ppt" @change="onFileSelected" />
            </div>
            <div class="kb-upload-list">
                <div v-for="(item, i) in uploadItems" :key="i" class="kb-upload-item" :style="item.status === 'done' ? 'background:#f0fdf4' : item.status === 'error' ? 'background:#fef2f2' : ''">
                    <span>{{ fileIcon(item.ext) }}</span>
                    <span style="flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ item.name }}</span>
                    <span style="font-size: 11px">
                        <span v-if="item.status === 'uploading'" class="kb-loading"></span>
                        {{ item.statusText }}
                    </span>
                </div>
            </div>
            <template #footer>
                <el-button size="small" @click="closeUpload">关闭</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="newNoteOpen" title="新建文档" width="440px">
            <el-form label-position="top" size="small" @submit.prevent="confirmNewNote">
                <el-form-item label="文件名">
                    <el-input v-model="newNoteName" placeholder="例如: my-doc" @keyup.enter="confirmNewNote" />
                </el-form-item>
                <div v-if="newNoteCategoryId" style="font-size: 12px; color: var(--text-secondary)">分类: {{ getCategoryName(newNoteCategoryId) }}</div>
                <div style="color: var(--text-muted); font-size: 12px">将自动添加 .md 扩展名</div>
            </el-form>
            <template #footer>
                <el-button size="small" @click="closeNewNoteDialog">取消</el-button>
                <el-button size="small" type="primary" :disabled="!newNoteName.trim()" @click="confirmNewNote">创建</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="catDialogOpen" :title="catEditId ? '编辑分类' : '新建分类'" width="360px">
            <el-input v-model="catEditName" size="small" placeholder="分类名称" @keyup.enter="confirmCatDialog" />
            <template #footer>
                <el-button v-if="catEditId" size="small" type="danger" plain style="margin-right: auto" @click="deleteCategory">删除</el-button>
                <el-button size="small" @click="closeCatDialog">取消</el-button>
                <el-button size="small" type="primary" :disabled="!catEditName.trim()" @click="confirmCatDialog">{{ catEditId ? '保存' : '创建' }}</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="newMdOpen" title="新建 Markdown 文档" width="1100px" class="kb-newmd-dialog">
            <div class="kb-newmd-bar">
                <span class="kb-newmd-label">文件名</span>
                <el-input v-model="newMdName" size="small" style="flex: 1" placeholder="输入文件名（自动添加 .md）" />
                <span class="kb-newmd-label">分类</span>
                <el-select v-model="newMdCategoryId" size="small" style="width: 140px">
                    <el-option :value="null" label="无" />
                    <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
                </el-select>
            </div>
            <div class="kb-md-split">
                <div class="kb-md-pane">
                    <div class="kb-md-pane-label">Markdown 编辑</div>
                    <el-input v-model="newMdContent" type="textarea" class="kb-md-textarea" placeholder="在此输入 Markdown 内容..." />
                </div>
                <div class="kb-md-pane">
                    <div class="kb-md-pane-label">预览</div>
                    <div class="kb-md-preview" v-html="renderMd(newMdContent)"></div>
                </div>
            </div>
            <template #footer>
                <el-button size="small" @click="closeNewMdDialog">取消</el-button>
                <el-button size="small" type="primary" :disabled="!newMdName.trim()" :loading="creatingMd" @click="confirmNewMd">创建</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { marked } from 'marked';
import { apiFetch, apiError, getToken } from '../api/client';

interface KbItem { id: number; name: string; }
interface Category { id: number; name: string; sortOrder?: number; }
interface DocItem {
    name: string;
    path: string;
    fileType?: string;
    fileSize?: number;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
    categoryId?: number;
    chunkCount?: number;
    tags?: string[];
    originalFile?: boolean;
}
interface Chunk { chunkIndex: number; content: string; }
interface UploadItem { name: string; ext: string; status: string; statusText: string; }

const kbList = ref<KbItem[]>([]);
const currentKbId = ref<number | null>(null);
const documents = ref<DocItem[]>([]);
const searchQuery = ref('');
const isSearchMode = ref(false);
const fileTypeFilter = ref<string | null>(null);

const selectedDoc = ref<DocItem | null>(null);
const selectedDocTags = ref<string[]>([]);
const tagInput = ref('');
const contentTab = ref('content');
const docContent = ref('');
const docContentLoading = ref(false);
const chunks = ref<Chunk[]>([]);
const chunksLoading = ref(false);
const chunkExpanded = ref<Record<number, boolean>>({});

const editing = ref(false);
const editContent = ref('');
const savingDoc = ref(false);

const categories = ref<Category[]>([]);
const expandedCategories = ref<Record<number, boolean>>({});

const uploadOpen = ref(false);
const dragOver = ref(false);
const uploadItems = ref<UploadItem[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);

const newNoteOpen = ref(false);
const newNoteName = ref('');
const newNoteCategoryId = ref<number | null>(null);

const catDialogOpen = ref(false);
const catEditId = ref<number | null>(null);
const catEditName = ref('');

const newMdOpen = ref(false);
const newMdName = ref('');
const newMdContent = ref('');
const newMdCategoryId = ref<number | null>(null);
const creatingMd = ref(false);

const hasKb = computed(() => kbList.value.length > 0);

const fileTypes = computed(() => {
    const set: Record<string, boolean> = {};
    documents.value.forEach((d) => { set[d.fileType || 'md'] = true; });
    return Object.keys(set).sort();
});

const filteredDocuments = computed(() => {
    let docs = documents.value;
    if (fileTypeFilter.value) docs = docs.filter((d) => (d.fileType || 'md') === fileTypeFilter.value);
    if (isSearchMode.value && searchQuery.value) {
        const q = searchQuery.value.toLowerCase();
        docs = docs.filter((d) =>
            (d.name || '').toLowerCase().includes(q) || (d.tags || []).some((t) => t.toLowerCase().includes(q)),
        );
    }
    return docs;
});

const categoryTree = computed(() => {
    const grouped: Record<number, DocItem[]> = {};
    filteredDocuments.value.forEach((doc) => {
        const cid = doc.categoryId || -1;
        if (!grouped[cid]) grouped[cid] = [];
        grouped[cid].push(doc);
    });
    const result: { id: number; name: string; docs: DocItem[] }[] = [];
    [...categories.value].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0)).forEach((c) => {
        const docs = grouped[c.id] || [];
        result.push({ id: c.id, name: c.name, docs });
    });
    const uncategorized = grouped[-1] || [];
    if (uncategorized.length) result.push({ id: -1, name: '未分类', docs: uncategorized });
    return result;
});

const totalChunks = computed(() => documents.value.reduce((s, d) => s + (d.chunkCount || 0), 0));

const tagCount = computed(() => {
    const set: Record<string, boolean> = {};
    documents.value.forEach((d) => (d.tags || []).forEach((t) => { set[t] = true; }));
    return Object.keys(set).length;
});

function expandDefault(): void {
    const map: Record<number, boolean> = {};
    let found = false;
    for (const g of categoryTree.value) {
        if (g.id === -1 && g.docs.length > 0) { map[g.id] = true; found = true; break; }
    }
    if (!found) {
        for (const g of categoryTree.value) {
            if (g.docs.length > 0) { map[g.id] = true; break; }
        }
    }
    expandedCategories.value = map;
}

async function init(): Promise<void> {
    try {
        const d = await apiFetch('/api/chat/kbs');
        if (d.ok && d.data && d.data.length > 0) {
            kbList.value = d.data;
            currentKbId.value = kbList.value[0].id;
            await reloadAll();
        }
    } catch (e) {
        // ignore
    }
}

async function reloadAll(): Promise<void> {
    await Promise.all([loadDocuments(), loadCategories()]);
}

function selectKb(id: number): void {
    if (currentKbId.value === id) return;
    currentKbId.value = id;
    selectedDoc.value = null;
    isSearchMode.value = false;
    searchQuery.value = '';
    fileTypeFilter.value = null;
    reloadAll();
}

async function loadDocuments(): Promise<void> {
    if (currentKbId.value == null) return;
    try {
        const d = await apiFetch(`/api/kb/${currentKbId.value}/notes/list`);
        if (d.ok) documents.value = d.documents || [];
    } catch (e) {
        // ignore
    }
    nextTick(expandDefault);
}

async function loadCategories(): Promise<void> {
    if (currentKbId.value == null) return;
    try {
        const d = await apiFetch(`/api/kb/${currentKbId.value}/categories`);
        if (d.ok) categories.value = d.categories || [];
    } catch (e) {
        // ignore
    }
    nextTick(expandDefault);
}

function doSearch(): void {
    if (!searchQuery.value) { clearSearch(); return; }
    isSearchMode.value = true;
}

function clearSearch(): void {
    searchQuery.value = '';
    isSearchMode.value = false;
}

async function selectDoc(doc: DocItem): Promise<void> {
    if (currentKbId.value == null) return;
    editing.value = false;
    editContent.value = '';
    selectedDoc.value = doc;
    selectedDocTags.value = [...(doc.tags || [])];
    tagInput.value = '';
    contentTab.value = 'content';
    docContent.value = '';
    docContentLoading.value = true;
    chunks.value = [];
    chunkExpanded.value = {};
    chunksLoading.value = true;
    try {
        const [cr, chr] = await Promise.all([
            apiFetch(`/api/kb/${currentKbId.value}/notes/read?path=${encodeURIComponent(doc.path)}`),
            apiFetch(`/api/kb/${currentKbId.value}/notes/chunks?path=${encodeURIComponent(doc.path)}`),
        ]);
        if (cr.ok) docContent.value = cr.content || '';
        if (chr.ok) chunks.value = chr.chunks || [];
    } catch (e) {
        // ignore
    }
    docContentLoading.value = false;
    chunksLoading.value = false;
}

function switchTab(tab: string): void {
    contentTab.value = tab;
    if (tab === 'content') editing.value = false;
}

function toggleChunk(i: number): void {
    chunkExpanded.value[i] = !chunkExpanded.value[i];
}

function startEditing(): void {
    editContent.value = docContent.value;
    editing.value = true;
}

function cancelEditing(): void {
    editing.value = false;
    editContent.value = '';
}

async function saveEditing(): Promise<void> {
    if (!selectedDoc.value || currentKbId.value == null) return;
    savingDoc.value = true;
    try {
        const d = await apiFetch(`/api/kb/${currentKbId.value}/notes/save`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ path: selectedDoc.value.path, content: editContent.value }),
        });
        if (d.ok) {
            docContent.value = editContent.value;
            editing.value = false;
            editContent.value = '';
            ElMessage.success('✅ 已保存');
            await loadDocuments();
        } else {
            ElMessage.error('❌ ' + apiError(d, '保存失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 保存失败');
    } finally {
        savingDoc.value = false;
    }
}

async function addTag(): Promise<void> {
    const tag = tagInput.value.trim();
    if (!tag || !selectedDoc.value) return;
    if (selectedDocTags.value.includes(tag)) { tagInput.value = ''; return; }
    selectedDocTags.value.push(tag);
    tagInput.value = '';
    await saveTags();
}

async function removeTag(tag: string): Promise<void> {
    if (!selectedDoc.value) return;
    selectedDocTags.value = selectedDocTags.value.filter((t) => t !== tag);
    await saveTags();
}

async function saveTags(): Promise<void> {
    if (!selectedDoc.value || currentKbId.value == null) return;
    try {
        const d = await apiFetch(`/api/kb/${currentKbId.value}/notes/tags`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ path: selectedDoc.value.path, tags: selectedDocTags.value }),
        });
        if (d.ok) {
            selectedDoc.value.tags = [...selectedDocTags.value];
            await loadDocuments();
        } else if (d.error) {
            ElMessage.error('❌ ' + d.error);
        }
    } catch (e) {
        // ignore
    }
}

function openUpload(): void {
    uploadOpen.value = true;
    uploadItems.value = [];
    dragOver.value = false;
}

function closeUpload(): void {
    uploadOpen.value = false;
    uploadItems.value = [];
}

function triggerFileInput(): void {
    fileInput.value?.click();
}

function onDrop(e: DragEvent): void {
    dragOver.value = false;
    if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length > 0) {
        handleFiles(e.dataTransfer.files);
    }
}

function onFileSelected(e: Event): void {
    const input = e.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
        handleFiles(input.files);
        input.value = '';
    }
}

function handleFiles(files: FileList): void {
    for (const file of Array.from(files)) {
        const ext = getExtension(file.name);
        const idx = uploadItems.value.length;
        uploadItems.value.push({ name: file.name, ext, status: 'uploading', statusText: '上传中...' });
        uploadFile(file, idx);
    }
}

async function uploadFile(file: File, index: number): Promise<void> {
    if (currentKbId.value == null) return;
    const formData = new FormData();
    formData.append('file', file);
    try {
        const d = await apiFetch(`/api/kb/${currentKbId.value}/notes/upload`, { method: 'POST', body: formData });
        if (d.ok) {
            uploadItems.value[index].status = 'done';
            uploadItems.value[index].statusText = '上传成功';
        } else {
            uploadItems.value[index].status = 'error';
            uploadItems.value[index].statusText = d.error || '失败';
        }
        await loadDocuments();
    } catch (e) {
        uploadItems.value[index].status = 'error';
        uploadItems.value[index].statusText = '失败';
    }
}

function toggleCategory(id: number): void {
    expandedCategories.value = { ...expandedCategories.value, [id]: !expandedCategories.value[id] };
}

function getCategoryName(id: number | null): string {
    if (id == null || id === -1) return '未分类';
    return categories.value.find((c) => c.id === id)?.name || '未分类';
}

function openNewNoteInCategory(catId: number): void {
    newNoteName.value = '';
    newNoteCategoryId.value = catId === -1 ? null : catId;
    newNoteOpen.value = true;
}

function closeNewNoteDialog(): void {
    newNoteOpen.value = false;
    newNoteName.value = '';
    newNoteCategoryId.value = null;
}

async function confirmNewNote(): Promise<void> {
    const name = newNoteName.value.trim();
    if (!name || currentKbId.value == null) return;
    const catId = newNoteCategoryId.value;
    closeNewNoteDialog();
    let url = `/api/kb/${currentKbId.value}/notes/new?filename=${encodeURIComponent(name)}`;
    if (catId) url += '&categoryId=' + catId;
    try {
        const d = await apiFetch(url, { method: 'POST' });
        if (d.ok) {
            ElMessage.success('✅ 文档已创建');
            await loadDocuments();
            setTimeout(() => {
                const found = documents.value.find((doc) => doc.path === d.path);
                if (found) selectDoc(found);
            }, 300);
        } else {
            ElMessage.error('❌ ' + apiError(d, '新建失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 新建失败');
    }
}

async function downloadDoc(doc: DocItem): Promise<void> {
    if (currentKbId.value == null) return;
    try {
        const token = getToken();
        const resp = await fetch(`/api/kb/${currentKbId.value}/notes/download?path=${encodeURIComponent(doc.path)}`, {
            headers: token ? { Authorization: 'Bearer ' + token } : {},
        });
        if (!resp.ok) {
            ElMessage.error('❌ 下载失败');
            return;
        }
        const blob = await resp.blob();
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = doc.name;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(a.href);
    } catch (e) {
        ElMessage.error('❌ 下载失败');
    }
}

async function deleteDoc(doc: DocItem): Promise<void> {
    if (currentKbId.value == null) return;
    try {
        await ElMessageBox.confirm(`确认删除「${doc.path}」？`, '删除文档', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' });
    } catch (e) {
        return;
    }
    try {
        const d = await apiFetch(`/api/kb/${currentKbId.value}/notes/delete?path=${encodeURIComponent(doc.path)}`, { method: 'POST' });
        if (d.ok) {
            if (selectedDoc.value && selectedDoc.value.path === doc.path) selectedDoc.value = null;
            ElMessage.success('✅ 文档已删除');
            await loadDocuments();
        } else {
            ElMessage.error('❌ ' + apiError(d, '删除失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 删除失败');
    }
}

function openNewMd(): void {
    newMdName.value = '';
    newMdContent.value = '';
    newMdCategoryId.value = null;
    newMdOpen.value = true;
}

function closeNewMdDialog(): void {
    newMdOpen.value = false;
    newMdName.value = '';
    newMdContent.value = '';
    newMdCategoryId.value = null;
}

async function confirmNewMd(): Promise<void> {
    let name = newMdName.value.trim();
    if (!name || currentKbId.value == null) return;
    if (!name.endsWith('.md')) name += '.md';
    const catId = newMdCategoryId.value;
    creatingMd.value = true;
    try {
        const formData = new FormData();
        formData.append('filename', name);
        formData.append('content', newMdContent.value);
        if (catId) formData.append('categoryId', String(catId));
        const d = await apiFetch(`/api/kb/${currentKbId.value}/notes/new`, { method: 'POST', body: formData });
        closeNewMdDialog();
        if (d.ok) {
            ElMessage.success('✅ 文档已创建');
            isSearchMode.value = false;
            searchQuery.value = '';
            fileTypeFilter.value = null;
            await loadDocuments();
            const found = documents.value.find((doc) => doc.path === d.path);
            if (found) selectDoc(found);
        } else {
            ElMessage.error('❌ ' + apiError(d, '新建失败'));
        }
    } catch (e) {
        closeNewMdDialog();
        ElMessage.error('❌ 新建失败');
    } finally {
        creatingMd.value = false;
    }
}

function editCategory(group: { id: number; name: string }): void {
    catEditId.value = group.id;
    catEditName.value = group.name;
    catDialogOpen.value = true;
}

function showAddCategory(): void {
    catEditId.value = null;
    catEditName.value = '';
    catDialogOpen.value = true;
}

function closeCatDialog(): void {
    catDialogOpen.value = false;
    catEditId.value = null;
    catEditName.value = '';
}

async function confirmCatDialog(): Promise<void> {
    const name = catEditName.value.trim();
    if (!name || currentKbId.value == null) return;
    try {
        if (catEditId.value) {
            const d = await apiFetch(`/api/kb/${currentKbId.value}/categories/${catEditId.value}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name }),
            });
            if (d.ok) { await loadCategories(); closeCatDialog(); ElMessage.success('✅ 分类已更新'); }
            else ElMessage.error('❌ ' + apiError(d, '更新失败'));
        } else {
            const d = await apiFetch(`/api/kb/${currentKbId.value}/categories`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name }),
            });
            if (d.ok) { await loadCategories(); closeCatDialog(); ElMessage.success('✅ 分类已创建'); }
            else ElMessage.error('❌ ' + apiError(d, '创建失败'));
        }
    } catch (e) {
        ElMessage.error('❌ 操作失败');
    }
}

async function deleteCategory(): Promise<void> {
    if (!catEditId.value || currentKbId.value == null) return;
    try {
        await ElMessageBox.confirm(`确认删除分类「${catEditName.value}」？该分类下的文档将变为未分类。`, '删除分类', {
            type: 'warning',
            confirmButtonText: '删除',
            cancelButtonText: '取消',
        });
    } catch (e) {
        return;
    }
    try {
        const d = await apiFetch(`/api/kb/${currentKbId.value}/categories/${catEditId.value}`, { method: 'DELETE' });
        if (d.ok) {
            ElMessage.success('✅ 分类已删除');
            await reloadAll();
            closeCatDialog();
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

function fileIcon(type?: string): string {
    const map: Record<string, string> = {
        pdf: '📕', docx: '📘', doc: '📘', xlsx: '📊', xls: '📊', pptx: '💽', ppt: '💽',
        txt: '📄', html: '🌐', htm: '🌐', xml: '📋', csv: '📋', rtf: '📄', md: '📝',
    };
    return map[(type || '').toLowerCase()] || '📄';
}

function getExtension(name: string): string {
    if (!name) return '';
    const idx = name.lastIndexOf('.');
    return idx >= 0 ? name.substring(idx + 1).toLowerCase() : '';
}

function formatSize(bytes?: number): string {
    if (!bytes || bytes === 0) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
}

onMounted(init);
</script>

<style scoped>
.kb-layout { flex: 1; min-height: 0; display: flex; overflow: hidden; }
.kb-sidebar { width: 340px; background: white; border-right: 1px solid var(--border); display: flex; flex-direction: column; flex-shrink: 0; min-height: 0; }
.kb-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; background: #f8fafc; min-height: 0; }
.kb-sidebar-header { padding: 12px 16px; border-bottom: 1px solid var(--border); display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.kb-kb-list { display: flex; flex-wrap: wrap; gap: 6px; flex: 1; }
.kb-kb-card { padding: 6px 12px; border: 1px solid var(--border); border-radius: 6px; font-size: 12px; cursor: pointer; background: white; color: var(--text-secondary); transition: all 0.15s; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 140px; font-weight: 500; }
.kb-kb-card:hover { border-color: var(--primary); color: var(--primary); background: rgba(99, 102, 241, 0.08); }
.kb-kb-card.active { border-color: var(--primary); background: var(--primary); color: white; }
.kb-kb-card.single { cursor: default; border-color: transparent; background: transparent; color: var(--text-primary); font-size: 14px; font-weight: 600; padding: 4px 0; max-width: none; }
.kb-search-row { display: flex; gap: 4px; padding: 8px 16px 4px; }
.kb-filter-row { display: flex; gap: 4px; padding: 4px 16px; overflow-x: auto; flex-shrink: 0; }
.kb-filter-chip { padding: 2px 10px; border-radius: 10px; font-size: 11px; cursor: pointer; white-space: nowrap; border: 1px solid var(--border); color: var(--text-secondary); background: transparent; transition: all 0.15s; flex-shrink: 0; }
.kb-filter-chip:hover { background: #f8fafc; border-color: var(--primary); color: var(--primary); }
.kb-filter-chip.active { background: var(--primary); color: white; border-color: var(--primary); }
.kb-stats-row { display: flex; gap: 16px; padding: 6px 16px 8px; font-size: 12px; color: var(--text-muted); border-bottom: 1px solid var(--border); }
.kb-stats-row span { font-weight: 600; color: var(--text-primary); }
.kb-search-result { padding: 4px 16px 2px; font-size: 12px; color: var(--text-muted); }
.kb-sidebar-body { flex: 1; overflow-y: auto; padding: 4px 0; min-height: 0; }
.kb-sidebar-footer { padding: 8px 16px; border-top: 1px solid var(--border); display: flex; gap: 6px; flex-shrink: 0; }

.kb-cat-header { display: flex; align-items: center; gap: 4px; padding: 6px 12px; font-size: 12px; font-weight: 600; color: var(--text-secondary); cursor: pointer; border-bottom: 1px solid var(--border); background: #f8fafc; user-select: none; }
.kb-cat-header:hover { background: #f8fafc; }
.kb-cat-arrow { font-size: 9px; width: 14px; flex-shrink: 0; text-align: center; color: var(--text-muted); }
.kb-cat-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.kb-cat-count { font-weight: 400; color: var(--text-muted); font-size: 11px; }
.kb-cat-actions { flex-shrink: 0; display: flex; }

.kb-doc-item { display: flex; align-items: center; gap: 8px; padding: 8px 16px; cursor: pointer; font-size: 13px; color: var(--text-primary); border-bottom: 1px solid var(--border); transition: background 0.1s; }
.kb-doc-item:hover { background: #f8fafc; }
.kb-doc-item.active { background: rgba(99, 102, 241, 0.08); border-left: 3px solid var(--primary); }
.kb-doc-item .doc-info { flex: 1; min-width: 0; }
.kb-doc-item .doc-name { font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.kb-doc-item .doc-meta { font-size: 11px; color: var(--text-muted); margin-top: 2px; display: flex; gap: 8px; flex-wrap: wrap; }

.kb-main-header { padding: 16px 24px; border-bottom: 1px solid var(--border); background: white; flex-shrink: 0; }
.kb-main-title { font-size: 15px; font-weight: 600; color: var(--text-primary); display: flex; align-items: center; gap: 8px; }
.kb-main-path { font-size: 12px; color: var(--text-muted); margin-top: 2px; word-break: break-all; }
.kb-main-body { flex: 1; overflow-y: auto; padding: 20px 24px; min-height: 0; }

.kb-meta-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; margin-bottom: 16px; }
.kb-meta-item { padding: 10px 14px; background: white; border: 1px solid var(--border); border-radius: var(--radius-sm); }
.kb-meta-label { font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 2px; }
.kb-meta-value { font-size: 13px; color: var(--text-primary); font-weight: 500; }

.kb-tag-editor { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; padding: 10px 14px; background: white; border: 1px solid var(--border); border-radius: var(--radius-sm); margin-bottom: 16px; }
.kb-tag-editor-label { font-size: 11px; color: var(--text-muted); font-weight: 500; margin-right: 4px; text-transform: uppercase; letter-spacing: 0.5px; }
.kb-tag-input { width: 90px; }
.kb-tag { display: inline-flex; align-items: center; gap: 2px; padding: 1px 7px; border-radius: 10px; font-size: 11px; font-weight: 500; }
.kb-tag-color-0 { background: #f0f9ff; color: #0284c7; }
.kb-tag-color-1 { background: #f0fdf4; color: #16a34a; }
.kb-tag-color-2 { background: #fefce8; color: #ca8a04; }
.kb-tag-color-3 { background: #fdf2f8; color: #db2777; }
.kb-tag-color-4 { background: #f5f3ff; color: #7c3aed; }
.kb-tag-color-5 { background: #fff7ed; color: #ea580c; }
.kb-tag-color-6 { background: #ecfeff; color: #0891b2; }
.kb-tag-color-7 { background: #f1f5f9; color: #475569; }
.kb-tag-remove { cursor: pointer; opacity: 0.6; margin-left: 2px; }
.kb-tag-remove:hover { opacity: 1; }

.kb-tabs { display: flex; border-bottom: 1px solid var(--border); margin-bottom: 16px; }
.kb-tab { padding: 6px 14px; font-size: 13px; font-weight: 500; cursor: pointer; border-bottom: 2px solid transparent; color: var(--text-muted); transition: all 0.15s; }
.kb-tab.active { color: var(--primary); border-bottom-color: var(--primary); }

.kb-chunk-item { padding: 10px 14px; background: white; border: 1px solid var(--border); border-radius: var(--radius-sm); margin-bottom: 8px; border-left: 3px solid var(--primary); }
.kb-chunk-index { font-size: 11px; color: var(--text-muted); font-weight: 500; margin-bottom: 4px; }
.kb-chunk-content { font-size: 13px; color: var(--text-primary); line-height: 1.6; white-space: pre-wrap; word-break: break-word; max-height: 100px; overflow-y: auto; }
.kb-chunk-content.expanded { max-height: none; }
.kb-chunk-expand { font-size: 11px; color: var(--primary); cursor: pointer; margin-top: 2px; display: inline-block; }

.kb-content-view { font-size: 14px; line-height: 1.8; color: var(--text-primary); }
.kb-content-view h1 { font-size: 22px; font-weight: 700; margin: 0 0 16px; padding-bottom: 8px; border-bottom: 2px solid var(--border); }
.kb-content-view h2 { font-size: 18px; font-weight: 600; margin: 24px 0 12px; }
.kb-content-view h3 { font-size: 16px; font-weight: 600; margin: 20px 0 10px; }
.kb-content-view p { margin-bottom: 12px; }
.kb-content-view ul, .kb-content-view ol { padding-left: 24px; margin-bottom: 12px; }
.kb-content-view code { background: #f8fafc; padding: 2px 6px; border-radius: 4px; font-size: 13px; }
.kb-content-view pre { background: #1e293b; color: #e2e8f0; padding: 12px 16px; border-radius: var(--radius-sm); overflow-x: auto; margin: 12px 0; font-size: 13px; }
.kb-content-view pre code { background: none; color: inherit; padding: 0; }
.kb-content-view blockquote { border-left: 4px solid var(--primary); padding: 6px 14px; color: var(--text-secondary); margin: 12px 0; background: rgba(99, 102, 241, 0.08); border-radius: 0 var(--radius-sm) var(--radius-sm) 0; }
.kb-content-view table { width: 100%; border-collapse: collapse; margin: 12px 0; font-size: 13px; }
.kb-content-view th { background: rgba(99, 102, 241, 0.08); padding: 8px 12px; border: 1px solid var(--border); text-align: left; font-weight: 600; }
.kb-content-view td { padding: 6px 12px; border: 1px solid var(--border); }
.kb-content-view a { color: var(--primary); text-decoration: none; }
.kb-content-view a:hover { text-decoration: underline; }
.kb-content-view hr { border: none; border-top: 1px solid var(--border); margin: 20px 0; }
.kb-content-view img { max-width: 100%; border-radius: var(--radius-sm); }

.kb-empty-detail { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: var(--text-muted); text-align: center; padding: 40px; }
.kb-empty-detail-icon { font-size: 48px; margin-bottom: 12px; opacity: 0.5; }
.kb-empty-detail-title { font-size: 16px; font-weight: 500; color: var(--text-secondary); margin-bottom: 4px; }
.kb-empty-detail-desc { font-size: 13px; }
.kb-no-kb { display: flex; align-items: center; justify-content: center; height: 100%; color: var(--text-muted); font-size: 14px; }

.kb-upload-zone { border: 2px dashed var(--border); border-radius: var(--radius-sm); padding: 30px 20px; text-align: center; cursor: pointer; transition: all 0.2s; background: #f8fafc; }
.kb-upload-zone:hover, .kb-upload-zone.dragover { border-color: var(--primary); background: rgba(99, 102, 241, 0.08); }
.kb-upload-zone-icon { font-size: 36px; color: var(--text-muted); margin-bottom: 6px; }
.kb-upload-zone-text { font-size: 14px; color: var(--text-secondary); }
.kb-upload-zone-hint { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.kb-upload-list { margin-top: 10px; }
.kb-upload-item { display: flex; align-items: center; gap: 8px; padding: 6px 10px; background: #f8fafc; border-radius: var(--radius-sm); margin-bottom: 4px; font-size: 12px; }
.kb-editor-textarea { font-family: monospace; font-size: 14px; line-height: 1.7; }

.kb-newmd-bar { display: flex; gap: 8px; align-items: center; margin-bottom: 10px; }
.kb-newmd-label { font-size: 12px; color: var(--text-secondary); white-space: nowrap; }
.kb-md-split { display: flex; gap: 12px; height: 420px; }
.kb-md-pane { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.kb-md-pane-label { font-size: 11px; font-weight: 600; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; }
.kb-md-textarea { flex: 1; font-family: monospace; font-size: 13px; line-height: 1.6; }
.kb-md-textarea :deep(textarea) { height: 100% !important; }
.kb-md-preview { flex: 1; overflow-y: auto; padding: 10px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: white; font-size: 14px; line-height: 1.7; }
.kb-md-preview h1 { font-size: 20px; font-weight: 700; margin: 0 0 12px; padding-bottom: 6px; border-bottom: 2px solid var(--border); }
.kb-md-preview h2 { font-size: 17px; font-weight: 600; margin: 20px 0 10px; }
.kb-md-preview h3 { font-size: 15px; font-weight: 600; margin: 16px 0 8px; }
.kb-md-preview p { margin-bottom: 10px; }
.kb-md-preview ul, .kb-md-preview ol { padding-left: 22px; margin-bottom: 10px; }
.kb-md-preview code { background: #f8fafc; padding: 2px 6px; border-radius: 4px; font-size: 12px; }
.kb-md-preview pre { background: #1e293b; color: #e2e8f0; padding: 10px 14px; border-radius: var(--radius-sm); overflow-x: auto; margin: 10px 0; font-size: 12px; }
.kb-md-preview pre code { background: none; color: inherit; padding: 0; }
.kb-md-preview blockquote { border-left: 4px solid var(--primary); padding: 4px 12px; color: var(--text-secondary); margin: 10px 0; background: rgba(99, 102, 241, 0.08); border-radius: 0 var(--radius-sm) var(--radius-sm) 0; }
.kb-md-preview table { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 13px; }
.kb-md-preview th { background: rgba(99, 102, 241, 0.08); padding: 6px 10px; border: 1px solid var(--border); text-align: left; font-weight: 600; }
.kb-md-preview td { padding: 4px 10px; border: 1px solid var(--border); }
.kb-md-preview a { color: var(--primary); text-decoration: none; }
.kb-md-preview hr { border: none; border-top: 1px solid var(--border); margin: 16px 0; }
.kb-md-preview img { max-width: 100%; border-radius: var(--radius-sm); }
.kb-loading { display: inline-block; width: 12px; height: 12px; border: 2px solid var(--border); border-top-color: var(--primary); border-radius: 50%; animation: kb-spin 0.6s linear infinite; }
@keyframes kb-spin { to { transform: rotate(360deg); } }
</style>
