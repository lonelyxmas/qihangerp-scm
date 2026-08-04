<template>
    <div class="config-layout">
        <aside class="config-sidebar">
            <div class="config-sidebar-header">
                <div class="config-sidebar-title">配置</div>
            </div>
            <div class="config-nav-list">
                <div class="config-nav-item" :class="{ active: activeTab === 'ai' }" @click="activeTab = 'ai'">🧠 AI 模型</div>
                <div class="config-nav-item" :class="{ active: activeTab === 'feishu' }" @click="activeTab = 'feishu'">🔗 飞书集成</div>
                <div class="config-nav-item" :class="{ active: activeTab === 'system' }" @click="activeTab = 'system'">ℹ️ 系统信息</div>
                <div class="config-nav-item" :class="{ active: activeTab === 'scheduler' }" @click="activeTab = 'scheduler'">⚙️ 定时任务</div>
                <div class="config-nav-item" :class="{ active: activeTab === 'kb' }" @click="switchTabKb">📚 知识库</div>
                <div class="config-nav-item" :class="{ active: activeTab === 'datacenter' }" @click="switchTabDc">📦 数据中心</div>
            </div>
        </aside>

        <main class="config-content">
            <div v-if="!ollamaAvailable" class="alert-warning">
                <strong>⚠️</strong>
                <div>
                    Ollama 未运行 — 语义检索不可用，将回退最近 3 轮对话。请确保 <code>ollama serve</code> 已启动并已拉取
                    <code>bge-m3</code> 模型：
                    <code style="display:block;margin-top:4px;">ollama pull bge-m3</code>
                </div>
            </div>

            <div class="status-bar">
                <div class="status-item">
                    <div class="label">大模型服务</div>
                    <div v-if="profiles.length > 0">
                        <div class="value status-ok">● 已配置</div>
                        <div class="sub">{{ profiles[0].name }} · {{ profiles[0].model }}</div>
                    </div>
                    <div v-else>
                        <div class="value status-down">● 未配置</div>
                        <div class="sub">请在下方的 AI 模型卡片中配置</div>
                    </div>
                </div>
                <div class="status-item">
                    <div class="label">语义检索</div>
                    <div class="value" :class="ollamaAvailable ? 'status-ok' : 'status-down'">
                        {{ ollamaAvailable ? '● 运行中' : '● 未连接' }}
                    </div>
                    <div class="sub">{{ ollamaProvider || '本地 Ollama' }}</div>
                </div>
                <div class="status-item">
                    <div class="label">知识库</div>
                    <div class="value">{{ kbList.length }} 个</div>
                    <div class="sub">在对话页中选择当前笔记库</div>
                </div>
            </div>

            <div v-show="activeTab === 'ai'">
                <div class="card">
                    <div class="card-title">🧠 AI 模型</div>
                    <div class="card-desc">配置 LLM 模型列表。第一个模型将自动设为默认，可在对话页面切换模型。</div>
                    <div style="margin-bottom: 16px;">
                        <el-table :data="profiles" size="small" border>
                            <el-table-column label="名称" min-width="120">
                                <template #default="{ row }"><strong>{{ row.name }}</strong></template>
                            </el-table-column>
                            <el-table-column label="模型" min-width="130">
                                <template #default="{ row }"><code>{{ row.model || '' }}</code></template>
                            </el-table-column>
                            <el-table-column label="API 地址" min-width="180">
                                <template #default="{ row }">
                                    <code class="cell-code">{{ row.baseUrl || '' }}</code>
                                </template>
                            </el-table-column>
                            <el-table-column label="超时" width="70">
                                <template #default="{ row }">{{ row.timeout || 600 }}s</template>
                            </el-table-column>
                            <el-table-column label="类型" width="100">
                                <template #default="{ row }">
                                    <span class="type-badge" :style="{ background: typeColor(row.modelType) }">{{ typeLabel(row.modelType) }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column label="默认" width="80">
                                <template #default="{ row }">{{ row.isDefault ? '✅ 默认' : '—' }}</template>
                            </el-table-column>
                            <el-table-column label="操作" width="200">
                                <template #default="{ row }">
                                    <el-button size="small" text @click="editLlm(row)">编辑</el-button>
                                    <el-button v-if="!row.isDefault" size="small" text type="primary" @click="setDefault(row.id)">设为默认</el-button>
                                    <el-button size="small" text type="danger" @click="deleteLlm(row.id)">删除</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                    <div style="border-top: 1px solid var(--border); padding-top: 16px;">
                        <div class="sub-title">{{ editingLlmId !== null ? '编辑模型: ' + llmForm.name : '添加模型' }}</div>
                        <div class="form-row">
                            <div class="form-group" style="flex: 1; min-width: 120px;">
                                <label>名称</label>
                                <el-input v-model="llmForm.name" placeholder="如 DeepSeek" />
                            </div>
                            <div class="form-group" style="flex: 2; min-width: 200px;">
                                <label>API Key</label>
                                <el-input v-model="llmForm.apiKey" type="password" show-password placeholder="sk-xxxxxxxxxxxx" />
                            </div>
                            <div class="form-group" style="flex: 1; min-width: 160px;">
                                <label>API 地址</label>
                                <el-input v-model="llmForm.baseUrl" placeholder="https://api.deepseek.com" />
                            </div>
                            <div class="form-group" style="flex: 0 0 100px;">
                                <label>模型名</label>
                                <el-input v-model="llmForm.model" placeholder="deepseek-chat" />
                            </div>
                            <div class="form-group" style="flex: 0 0 80px;">
                                <label>超时(秒)</label>
                                <el-input-number v-model="llmForm.timeout" :min="10" :max="900" style="width: 100%;" />
                            </div>
                            <div class="form-group" style="flex: 0 0 130px;">
                                <label>模型类型</label>
                                <el-select v-model="llmForm.modelType" style="width: 100%;">
                                    <el-option label="文本模型" value="text" />
                                    <el-option label="多模态" value="multimodal" />
                                    <el-option label="向量模型" value="embedding" />
                                    <el-option label="生成图片" value="image" />
                                </el-select>
                            </div>
                        </div>
                        <div style="display: flex; align-items: center; gap: 8px; margin-top: 8px;">
                            <el-button type="primary" @click="submitLlm">{{ editingLlmId !== null ? '保存修改' : '添加模型' }}</el-button>
                            <el-button v-if="editingLlmId !== null" @click="resetLlmForm">取消</el-button>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <div class="card-title">🔤 语义向量模型</div>
                    <div class="card-desc">配置用于语义检索的向量模型。填入 API Key 则使用 API 模式（如硅基流动），留空则使用 Ollama 本地模式。保存后立即生效。</div>
                    <div class="form-row">
                        <div class="form-group" style="flex: 1; min-width: 160px;">
                            <label>API 地址</label>
                            <el-input v-model="embeddingForm.baseUrl" />
                        </div>
                        <div class="form-group" style="flex: 0 0 200px;">
                            <label>模型名</label>
                            <el-input v-model="embeddingForm.model" />
                        </div>
                        <div class="form-group" style="flex: 1; min-width: 200px;">
                            <label>API Key <span style="font-weight: 400;">(留空=Ollama)</span></label>
                            <el-input v-model="embeddingForm.apiKey" type="password" show-password placeholder="sk-..." />
                        </div>
                        <div class="form-group" style="flex: 0 0 160px;">
                            <label>供应商名称</label>
                            <el-input v-model="embeddingForm.provider" placeholder="如 硅基流动" />
                        </div>
                    </div>
                    <el-button type="primary" @click="saveEmbedding">保存</el-button>
                </div>
            </div>

            <div v-show="activeTab === 'feishu'">
                <div class="card">
                    <div class="card-title">🔗 飞书 Webhook 配置</div>
                    <div class="card-desc">修改后自动保存到数据库，下次推送生效。</div>
                    <div style="display: flex; gap: 8px;">
                        <el-input v-model="webhookUrl" placeholder="https://open.feishu.cn/open-apis/bot/v2/hook/..." style="flex: 1;" />
                        <el-button type="primary" @click="saveWebhook">保存</el-button>
                    </div>
                </div>
                <div class="card">
                    <div class="card-title">📩 飞书消息接收</div>
                    <div class="card-desc">配置飞书自建应用的凭据，AI助理将轮询指定群的未读消息并自动回复。</div>
                    <div class="form-group">
                        <label>App ID</label>
                        <el-input v-model="feishuForm.appId" placeholder="飞书自建应用的 App ID" />
                    </div>
                    <div class="form-group">
                        <label>App Secret</label>
                        <el-input v-model="feishuForm.appSecret" type="password" show-password placeholder="飞书自建应用的 App Secret" />
                    </div>
                    <div class="form-group">
                        <label>群 Chat ID</label>
                        <div style="display: flex; gap: 8px;">
                            <el-input v-model="feishuForm.chatId" placeholder="群聊的 chat_id" style="flex: 1;" />
                            <el-button :loading="fetchingChats" @click="fetchChats">获取群列表</el-button>
                        </div>
                        <div v-if="chatListOpen" style="margin-top: 8px;">
                            <div v-if="chatListError" style="color: #dc2626; font-size: 13px;">❌ {{ chatListError }}</div>
                            <div v-else-if="!chatList.length" class="text-muted" style="font-size: 13px;">没有找到群聊，请确认应用已添加到目标群</div>
                            <template v-else>
                                <div style="font-size: 13px; font-weight: 500; margin-bottom: 6px;">选择群聊：</div>
                                <div v-for="c in chatList" :key="c.chat_id" class="chat-option" @click="selectChat(c)">
                                    <strong>{{ c.name }}</strong>
                                    <span style="color: #909296; font-size: 11px; margin-left: 4px;">{{ c.type }}</span>
                                    <div style="font-size: 11px; color: var(--primary); word-break: break-all;">{{ c.chat_id }}</div>
                                </div>
                            </template>
                        </div>
                        <div class="text-muted" style="font-size: 12px; margin-top: 4px;">先填入 App ID 和 Secret，点击"获取群列表"选择你要监听的群</div>
                    </div>
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <el-button type="primary" @click="saveFeishu">保存飞书配置</el-button>
                        <el-button :loading="testingFeishu" @click="testFeishu">发送测试消息</el-button>
                    </div>
                </div>
            </div>

            <div v-show="activeTab === 'system'">
                <div class="card">
                    <div class="card-title">ℹ️ 系统信息</div>
                    <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 10px; font-size: 13px;">
                        <div><span class="text-muted">版本：</span>v3.0</div>
                        <div><span class="text-muted">知识库：</span>{{ kbList.length }} 个</div>
                        <div v-if="profiles.length > 0"><span class="text-muted">当前模型：</span>{{ profiles[0].name }}</div>
                        <div>
                            <span class="text-muted">语义检索：</span>
                            <span :style="{ color: ollamaAvailable ? '#16a34a' : '#dc2626' }">{{ ollamaAvailable ? '● 在线' : '● 离线' }}</span>
                        </div>
                    </div>
                </div>
            </div>

            <div v-show="activeTab === 'scheduler'">
                <div class="card">
                    <div class="card-title">⚙️ 定时任务</div>
                    <ul class="scheduler-list">
                        <li>
                            <span>生成综合日报</span>
                            <span class="text-muted">每天 09:00</span>
                        </li>
                    </ul>
                </div>
            </div>

            <div v-show="activeTab === 'kb'">
                <div class="card">
                    <div class="card-title">📚 知识库</div>
                    <div class="card-desc">管理知识库，每个知识库对应一个本地笔记目录。支持笔记索引、语义检索和 AI 分析。</div>
                    <div style="display: flex; gap: 8px; margin-bottom: 12px;">
                        <el-button type="primary" size="small" @click="openKbModal">+ 新建知识库</el-button>
                    </div>
                    <el-table :data="kbList" size="small" border>
                        <el-table-column label="名称" min-width="140">
                            <template #default="{ row }"><strong>{{ row.name }}</strong></template>
                        </el-table-column>
                        <el-table-column label="可见性" width="80">
                            <template #default="{ row }">
                                <span :style="{ color: row.visibility === 'public' ? '#16a34a' : '#f59e0b' }">{{ row.visibility === 'public' ? '公开' : '私有' }}</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="索引状态" min-width="180">
                            <template #default="{ row }">
                                <div v-if="kbStatusMap[row.id] && kbStatusMap[row.id].running">
                                    <el-progress :percentage="kbStatusMap[row.id].progress || 0" :stroke-width="6" style="max-width: 200px;" />
                                    <div v-if="kbStatusMap[row.id].lastIndexTime" class="text-muted" style="font-size: 11px; margin-top: 2px;">{{ kbStatusMap[row.id].lastIndexTime }}</div>
                                </div>
                                <div v-else-if="kbStatusMap[row.id]">
                                    <span v-if="kbStatusMap[row.id].available && kbStatusMap[row.id].chunkCount > 0" style="color: #16a34a;">✅ {{ kbStatusMap[row.id].chunkCount }} 块</span>
                                    <span v-else-if="kbStatusMap[row.id].available" class="text-muted">未索引</span>
                                    <span v-else style="color: #dc2626;">⚠️ 未配置</span>
                                    <div v-if="kbStatusMap[row.id].lastIndexTime" class="text-muted" style="font-size: 11px; margin-top: 2px;">{{ kbStatusMap[row.id].lastIndexTime }}</div>
                                </div>
                                <span v-else class="text-muted">加载中...</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="日报" width="60" align="center">
                            <template #default="{ row }">{{ row.autoReport === 1 || row.autoReport === true ? '✅' : '—' }}</template>
                        </el-table-column>
                        <el-table-column label="飞书推送" width="80" align="center">
                            <template #default="{ row }">{{ row.feishuPush === 1 || row.feishuPush === true ? '✅' : '—' }}</template>
                        </el-table-column>
                        <el-table-column label="操作" width="220">
                            <template #default="{ row }">
                                <el-button size="small" text @click="editKb(row)">编辑</el-button>
                                <el-button size="small" text type="primary" @click="reindexKb(row.id)">🔄 重索引</el-button>
                                <el-button size="small" text type="danger" @click="deleteKb(row)">删除</el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                </div>
            </div>

            <div v-show="activeTab === 'datacenter'">
                <DataCenter />
            </div>
        </main>

        <!-- KB 弹窗 -->
        <el-dialog v-model="kbModalOpen" :title="editingKbId !== null ? '编辑知识库' : '新建知识库'" width="420px" append-to-body>
            <div class="form-group">
                <label class="form-label">名称 *</label>
                <el-input v-model="kbForm.name" placeholder="如：我的笔记库" />
            </div>
            <div class="form-group">
                <label class="form-label">可见性</label>
                <el-select v-model="kbForm.visibility" style="width: 100%;">
                    <el-option label="私有" value="private" />
                    <el-option label="公开" value="public" />
                </el-select>
            </div>
            <div style="display: flex; gap: 20px; margin-top: 12px;">
                <div class="toggle-item">
                    <span style="font-size: 13px;">自动日报</span>
                    <el-switch v-model="kbForm.autoReport" />
                </div>
                <div class="toggle-item">
                    <span style="font-size: 13px;">飞书推送</span>
                    <el-switch v-model="kbForm.feishuPush" />
                </div>
            </div>
            <template #footer>
                <el-button @click="kbModalOpen = false">取消</el-button>
                <el-button type="primary" @click="saveKb">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { apiFetch, apiError } from '../api/client';
import DataCenter from './DataCenter.vue';

const activeTab = ref('ai');

interface LlmProfile {
    id: number;
    name: string;
    apiKey?: string;
    baseUrl?: string;
    model?: string;
    timeout?: number;
    modelType?: string;
    isDefault?: boolean;
}

interface KbBase {
    id: number;
    name: string;
    visibility?: string;
    autoReport?: number | boolean;
    feishuPush?: number | boolean;
}

interface KbStatus {
    running: boolean;
    progress: number;
    available: boolean;
    chunkCount: number;
    lastIndexTime: string;
}

// ========== 状态栏 ==========
const profiles = ref<LlmProfile[]>([]);
const ollamaAvailable = ref(true);
const ollamaProvider = ref('');
const kbList = ref<KbBase[]>([]);

async function loadStatus(): Promise<void> {
    const [o, p, kb] = await Promise.all([
        apiFetch('/api/ollama/status'),
        apiFetch('/api/config/llm-profiles'),
        apiFetch('/api/config/knowledge-bases'),
    ]);
    ollamaAvailable.value = o?.available !== false;
    profiles.value = p?.ok ? (p.profiles ?? []) : [];
    kbList.value = kb?.ok ? (kb.data ?? []) : [];
    const e = await apiFetch('/api/config/embedding-model');
    if (e?.providerLabel) ollamaProvider.value = e.providerLabel;
    ollamaProvider.value = ollamaProvider.value || (ollamaAvailable.value ? '本地 Ollama' : '');
}

// ========== LLM 模型 ==========
const TYPE_MAP: Record<string, { label: string; color: string }> = {
    text: { label: '文本模型', color: '#6366f1' },
    multimodal: { label: '多模态', color: '#10b981' },
    embedding: { label: '向量模型', color: '#f59e0b' },
    image: { label: '生成图片', color: '#ec4899' },
};

function typeLabel(t?: string): string {
    return TYPE_MAP[t || '']?.label || '文本模型';
}
function typeColor(t?: string): string {
    return TYPE_MAP[t || '']?.color || '#909296';
}

const editingLlmId = ref<number | null>(null);
const llmForm = ref<LlmProfile>({ id: 0, name: '', apiKey: '', baseUrl: 'https://api.deepseek.com', model: '', timeout: 600, modelType: 'text' });

function resetLlmForm(): void {
    editingLlmId.value = null;
    llmForm.value = { id: 0, name: '', apiKey: '', baseUrl: 'https://api.deepseek.com', model: '', timeout: 600, modelType: 'text' };
}

function editLlm(row: LlmProfile): void {
    editingLlmId.value = row.id;
    llmForm.value = {
        id: row.id,
        name: row.name,
        apiKey: row.apiKey || '',
        baseUrl: row.baseUrl || 'https://api.deepseek.com',
        model: row.model || '',
        timeout: row.timeout || 600,
        modelType: row.modelType || 'text',
    };
}

async function submitLlm(): Promise<void> {
    const name = llmForm.value.name.trim();
    if (!name) {
        ElMessage.error('❌ 请输入模型名称');
        return;
    }
    const payload: Record<string, any> = {
        name,
        apiKey: llmForm.value.apiKey,
        baseUrl: llmForm.value.baseUrl || 'https://api.deepseek.com',
        model: llmForm.value.model || 'deepseek-chat',
        timeout: llmForm.value.timeout || 600,
        modelType: llmForm.value.modelType || 'text',
    };
    if (editingLlmId.value !== null) {
        payload.id = editingLlmId.value;
        const old = profiles.value.find((m) => m.id === editingLlmId.value);
        if (old) payload.isDefault = old.isDefault;
    } else {
        payload.isDefault = profiles.value.length === 0;
    }
    const d = await apiFetch('/api/config/llm-profiles', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    });
    if (d.ok) {
        ElMessage.success(editingLlmId.value !== null ? '✅ 已更新' : '✅ 已添加');
        resetLlmForm();
        await loadStatus();
    } else {
        ElMessage.error('❌ ' + apiError(d, '保存失败'));
    }
}

async function deleteLlm(id: number): Promise<void> {
    try {
        await ElMessageBox.confirm('确定删除该模型？', '删除模型', { type: 'warning' });
    } catch {
        return;
    }
    const d = await apiFetch('/api/config/llm-profiles/' + id, { method: 'DELETE' });
    if (d.ok) {
        ElMessage.success('✅ 已删除');
        if (editingLlmId.value === id) resetLlmForm();
        await loadStatus();
    } else {
        ElMessage.error('❌ ' + apiError(d, '删除失败'));
    }
}

async function setDefault(id: number): Promise<void> {
    const d = await apiFetch('/api/config/llm-profiles/' + id + '/default', { method: 'POST' });
    if (d.ok) {
        ElMessage.success('✅ 默认模型已切换');
        await loadStatus();
    } else {
        ElMessage.error('❌ ' + apiError(d, '设置失败'));
    }
}

// ========== 语义向量模型 ==========
const embeddingForm = ref({ model: 'bge-m3', baseUrl: 'http://127.0.0.1:11434', apiKey: '', provider: '' });

async function loadEmbedding(): Promise<void> {
    const d = await apiFetch('/api/config/embedding-model');
    if (!d?.ok) return;
    embeddingForm.value = {
        model: d.model || 'bge-m3',
        baseUrl: d.baseUrl || 'http://127.0.0.1:11434',
        apiKey: d.apiKey || '',
        provider: d.provider || '',
    };
}

async function saveEmbedding(): Promise<void> {
    const d = await apiFetch('/api/config/embedding-model', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            model: embeddingForm.value.model || 'bge-m3',
            baseUrl: embeddingForm.value.baseUrl || 'http://127.0.0.1:11434',
            apiKey: embeddingForm.value.apiKey.trim(),
            provider: embeddingForm.value.provider.trim(),
        }),
    });
    if (d.ok) {
        ElMessage.success('✅ 语义向量模型已更新');
        await loadStatus();
    } else {
        ElMessage.error('❌ ' + apiError(d, '保存失败'));
    }
}

// ========== 飞书 ==========
const webhookUrl = ref('');
const feishuForm = ref({ appId: '', appSecret: '', chatId: '' });
const fetchingChats = ref(false);
const chatListOpen = ref(false);
const chatList = ref<{ chat_id: string; name: string; type: string }[]>([]);
const chatListError = ref('');
const testingFeishu = ref(false);

async function loadFeishu(): Promise<void> {
    try {
        const cfg = await apiFetch('/api/config');
        if (cfg) {
            webhookUrl.value = cfg.feishuWebhookUrl || '';
            feishuForm.value.appId = cfg.feishuAppId || '';
            feishuForm.value.appSecret = cfg.feishuAppSecret || '';
            feishuForm.value.chatId = cfg.feishuChatId || '';
        }
    } catch {
        /* ignore */
    }
}

async function saveWebhook(): Promise<void> {
    const url = webhookUrl.value.trim();
    if (!url.startsWith('https://')) {
        ElMessage.error('❌ URL 必须以 https:// 开头');
        return;
    }
    const form = new FormData();
    form.append('url', url);
    const d = await apiFetch('/api/config/webhook', { method: 'POST', body: form });
    if (d.ok) {
        ElMessage.success('✅ 已保存');
    } else {
        ElMessage.error('❌ ' + apiError(d, '保存失败'));
    }
}

async function saveFeishu(): Promise<void> {
    const form = new FormData();
    form.append('appId', feishuForm.value.appId.trim());
    form.append('appSecret', feishuForm.value.appSecret.trim());
    form.append('chatId', feishuForm.value.chatId.trim());
    const d = await apiFetch('/api/config/feishu', { method: 'POST', body: form });
    if (d.ok) {
        ElMessage.success('✅ 已保存');
    } else {
        ElMessage.error('❌ ' + apiError(d, '保存失败'));
    }
}

async function fetchChats(): Promise<void> {
    const appId = feishuForm.value.appId.trim();
    const appSecret = feishuForm.value.appSecret.trim();
    if (!appId || !appSecret) {
        ElMessage.error('请先填写 App ID 和 App Secret');
        return;
    }
    fetchingChats.value = true;
    chatListError.value = '';
    try {
        const form = new FormData();
        form.append('appId', appId);
        form.append('appSecret', appSecret);
        const d = await apiFetch('/api/config/feishu/chats', { method: 'POST', body: form });
        chatListOpen.value = true;
        if (!d.ok) {
            chatList.value = [];
            chatListError.value = d.error || '查询失败';
            return;
        }
        chatList.value = d.chats || [];
    } catch {
        chatListOpen.value = true;
        chatList.value = [];
        chatListError.value = '查询失败';
    } finally {
        fetchingChats.value = false;
    }
}

function selectChat(c: { chat_id: string; name: string }): void {
    feishuForm.value.chatId = c.chat_id;
    chatListOpen.value = false;
    ElMessage.success('已选择群聊，点击保存即可');
}

async function testFeishu(): Promise<void> {
    const { appId, appSecret, chatId } = feishuForm.value;
    if (!appId.trim() || !appSecret.trim() || !chatId.trim()) {
        ElMessage.error('请先填完 App ID、App Secret 和 Chat ID');
        return;
    }
    testingFeishu.value = true;
    try {
        const form = new FormData();
        form.append('appId', appId);
        form.append('appSecret', appSecret);
        form.append('chatId', chatId);
        const d = await apiFetch('/api/config/feishu/test', { method: 'POST', body: form });
        if (d.ok) {
            ElMessage.success('✅ 测试消息已发送到群，请查看飞书！');
        } else {
            ElMessage.error('❌ ' + apiError(d, '发送失败'));
        }
    } catch {
        ElMessage.error('❌ 发送失败');
    } finally {
        testingFeishu.value = false;
    }
}

// ========== 知识库 ==========
const kbModalOpen = ref(false);
const editingKbId = ref<number | null>(null);
const kbForm = ref({ name: '', visibility: 'private', autoReport: false, feishuPush: false });
const kbStatusMap = ref<Record<number, KbStatus>>({});
const kbPollTimers: Record<number, number> = {};

function switchTabKb(): void {
    activeTab.value = 'kb';
    kbList.value.forEach((kb) => loadKbIndexStatus(kb.id));
}

function openKbModal(): void {
    editingKbId.value = null;
    kbForm.value = { name: '', visibility: 'private', autoReport: false, feishuPush: false };
    kbModalOpen.value = true;
}

function editKb(row: KbBase): void {
    editingKbId.value = row.id;
    kbForm.value = {
        name: row.name || '',
        visibility: row.visibility || 'private',
        autoReport: row.autoReport === 1 || row.autoReport === true,
        feishuPush: row.feishuPush === 1 || row.feishuPush === true,
    };
    kbModalOpen.value = true;
}

async function saveKb(): Promise<void> {
    const name = kbForm.value.name.trim();
    if (!name) {
        ElMessage.error('请输入知识库名称');
        return;
    }
    const payload: Record<string, any> = {
        name,
        visibility: kbForm.value.visibility,
        autoReport: kbForm.value.autoReport,
        feishuPush: kbForm.value.feishuPush,
    };
    if (editingKbId.value !== null) payload.id = editingKbId.value;
    const d = await apiFetch('/api/config/knowledge-bases', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    });
    if (d.ok) {
        kbModalOpen.value = false;
        ElMessage.success(editingKbId.value !== null ? '✅ 知识库已更新' : '✅ 知识库已创建');
        await loadStatus();
        kbList.value.forEach((kb) => loadKbIndexStatus(kb.id));
    } else {
        ElMessage.error('❌ ' + apiError(d, '保存失败'));
    }
}

async function deleteKb(row: KbBase): Promise<void> {
    try {
        await ElMessageBox.confirm(
            `确定删除「${row.name}」？\n\n删除后该知识库的所有笔记、向量索引、对话记录将被一并删除，此操作不可恢复。`,
            '删除知识库',
            { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
        );
    } catch {
        return;
    }
    const d = await apiFetch('/api/config/knowledge-bases/' + row.id, { method: 'DELETE' });
    if (d.ok) {
        ElMessage.success('✅ 知识库已删除');
        await loadStatus();
    } else {
        ElMessage.error('❌ ' + apiError(d, '删除失败'));
    }
}

async function loadKbIndexStatus(kbId: number): Promise<void> {
    try {
        const d = await apiFetch('/api/kb/' + kbId + '/index-status');
        if (!d?.ok) return;
        kbStatusMap.value = { ...kbStatusMap.value, [kbId]: { ...d } };
        if (d.running) {
            kbPollTimers[kbId] = window.setTimeout(() => loadKbIndexStatus(kbId), 2000);
        } else if (kbPollTimers[kbId]) {
            clearTimeout(kbPollTimers[kbId]);
            delete kbPollTimers[kbId];
        }
    } catch {
        /* ignore */
    }
}

async function reindexKb(kbId: number): Promise<void> {
    const d = await apiFetch('/api/kb/' + kbId + '/reindex', { method: 'POST' });
    if (d.ok) {
        ElMessage.success('✅ 重索引已启动');
        loadKbIndexStatus(kbId);
    } else {
        ElMessage.error('❌ ' + apiError(d, '重索引失败'));
        loadKbIndexStatus(kbId);
    }
}

// ========== 数据中心 Tab ==========
function switchTabDc(): void {
    activeTab.value = 'datacenter';
}

onMounted(() => {
    loadStatus();
    loadEmbedding();
    loadFeishu();
});

onBeforeUnmount(() => {
    Object.values(kbPollTimers).forEach((t) => clearTimeout(t));
});
</script>

<style scoped>
.config-layout { flex: 1; min-height: 0; display: flex; }
.config-sidebar {
    width: 180px; background: var(--bg-sidebar); border-right: 1px solid var(--border);
    display: flex; flex-direction: column; flex-shrink: 0; min-height: 0;
}
.config-sidebar-header { padding: 16px; border-bottom: 1px solid var(--border); }
.config-sidebar-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.config-nav-list { flex: 1; overflow-y: auto; padding: 8px 0; }
.config-nav-item {
    display: flex; align-items: center; gap: 8px; padding: 10px 16px; cursor: pointer;
    color: var(--text-secondary); font-size: 13px; transition: all 0.2s; margin: 0 4px; border-radius: var(--radius-sm);
}
.config-nav-item:hover { background: var(--hover); color: var(--text-primary); }
.config-nav-item.active { background: rgba(99, 102, 241, 0.1); color: var(--primary); font-weight: 500; }

.config-content { flex: 1; overflow-y: auto; padding: 24px; min-height: 0; }

.card { background: white; border-radius: var(--radius-lg); border: 1px solid var(--border); box-shadow: var(--shadow-sm); padding: 24px; margin-bottom: 16px; }
.card-title { font-size: 16px; font-weight: 600; margin-bottom: 16px; }
.card-desc { font-size: 13px; color: var(--text-secondary); line-height: 1.6; margin-bottom: 12px; }
.sub-title { font-size: 13px; font-weight: 600; color: var(--text-secondary); margin: 16px 0 8px; }

.status-bar { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 16px; }
.status-item { background: #fff; border: 1px solid var(--border); border-radius: var(--radius-md); padding: 14px 18px; flex: 1; min-width: 160px; }
.status-item .label { font-size: 11px; color: var(--text-muted); margin-bottom: 4px; }
.status-item .value { font-size: 15px; font-weight: 600; }
.status-ok { color: #16a34a; }
.status-down { color: #dc2626; }
.status-item .sub { font-size: 11px; color: var(--text-secondary); margin-top: 2px; }

.alert-warning {
    background: #fef9c3; border: 1px solid #facc15; border-radius: var(--radius-md);
    padding: 12px 16px; font-size: 13px; color: #854d0e; margin-bottom: 16px;
    display: flex; align-items: flex-start; gap: 8px;
}
.alert-warning code { background: #fef08a; padding: 1px 4px; border-radius: 3px; }

.form-group { margin-bottom: 12px; }
.form-group label { display: block; font-size: 13px; font-weight: 500; color: var(--text-secondary); margin-bottom: 4px; }
.form-label { display: block; font-size: 13px; font-weight: 500; color: var(--text-secondary); margin-bottom: 6px; }
.form-row { display: flex; gap: 12px; flex-wrap: wrap; }
.form-row .form-group { flex: 1; }

.text-muted { color: var(--text-secondary); font-size: 13px; }
.type-badge { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 11px; color: #fff; }
.cell-code { max-width: 200px; overflow: hidden; text-overflow: ellipsis; display: inline-block; vertical-align: bottom; }
code { background: #f0edff; padding: 2px 6px; border-radius: 4px; font-size: 12px; color: #5c3dcf; font-family: "SF Mono", "Fira Code", monospace; }

.scheduler-list { list-style: none; }
.scheduler-list li { padding: 10px 0; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
.scheduler-list li:last-child { border-bottom: none; }

.chat-option { padding: 8px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); margin-bottom: 4px; cursor: pointer; font-size: 13px; }
.chat-option:hover { background: var(--hover); }

.toggle-item { display: flex; align-items: center; gap: 8px; }
</style>
