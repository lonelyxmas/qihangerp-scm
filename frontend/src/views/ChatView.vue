<template>
    <div class="chat-layout">
        <aside class="sessions-sidebar">
            <div class="sessions-header">
                <div class="sessions-title">对话</div>
                <el-button type="primary" size="small" style="width: 100%; margin-top: 10px" @click="createNewSession">
                    ＋ 新对话
                </el-button>
            </div>
            <div class="sessions-container">
                <div
                    v-for="s in sessions"
                    :key="s.id"
                    class="session-item"
                    :class="{ active: s.id === currentSessionId }"
                    @click="selectSession(s.id)"
                >
                    <span class="session-icon">💬</span>
                    <span class="session-title-text">{{ s.title }}</span>
                    <span class="session-delete" title="删除" @click.stop="deleteSession(s.id)">🗑</span>
                </div>
                <el-empty v-if="!sessions.length" description="暂无对话" :image-size="40" />
            </div>
        </aside>

        <main class="chat-main">
            <div v-if="!modelConfigured" class="unconfigured-banner">
                <span>⚠️</span>
                <span>
                    尚未配置大模型，请先前往
                    <a :href="legacyConfigUrl" target="_blank">系统配置</a>
                    添加 API Key
                </span>
            </div>

            <div class="chat-toolbar">
                <div class="chat-toolbar-title">{{ currentTitle }}</div>
                <div class="chat-toolbar-actions">
                    <el-button size="small" text :disabled="!currentSessionId" @click="clearCurrentSession">清空</el-button>
                </div>
            </div>

            <div ref="messagesEl" class="chat-messages">
                <div v-if="!messages.length && !sending" class="empty-state">
                    <template v-if="modelConfigured">
                        <div class="empty-icon">💬</div>
                        <div class="empty-title">开始对话</div>
                        <div class="empty-desc">你可以直接与 AI 对话，或使用 @笔记库名 进行知识库问答。</div>
                    </template>
                    <template v-else>
                        <div class="empty-icon no-model">⚙️</div>
                        <div class="empty-title">未配置大模型</div>
                        <div class="empty-desc">请先配置 API Key 后即可开始对话</div>
                    </template>
                </div>

                <div v-for="(m, i) in messages" :key="i" class="message" :class="m.role">
                    <div class="message-avatar">{{ m.role === 'user' ? '👤' : '🤖' }}</div>
                    <div class="message-content-wrapper">
                        <div class="message-header">
                            <span class="message-author">{{ m.role === 'user' ? '我' : '启航 AI 协作平台' }}</span>
                            <span v-if="m.time" class="message-time">{{ formatTime(m.time) }}</span>
                        </div>
                        <div
                            v-if="m.role === 'assistant'"
                            class="message-content markdown-content"
                            v-html="renderMd(m.content)"
                        ></div>
                        <div v-else class="message-content" v-html="highlightMentions(m.content)"></div>
                    </div>
                </div>

                <div v-if="sending && streaming" class="message assistant">
                    <div class="message-avatar">🤖</div>
                    <div class="message-content-wrapper">
                        <div class="message-header">
                            <span class="message-author">启航 AI 协作平台</span>
                        </div>
                        <div v-if="statusText" class="status-area">{{ statusText }}</div>
                        <div class="message-content markdown-content" v-html="renderMd(streamContent)"></div>
                    </div>
                </div>
                <div v-if="sending && !streaming" class="message assistant">
                    <div class="message-avatar">🤖</div>
                    <div class="message-content-wrapper">
                        <div class="message-header">
                            <span class="message-author">启航 AI 协作平台</span>
                        </div>
                        <div v-if="statusText" class="status-area">{{ statusText }}</div>
                        <div v-else class="typing-indicator">
                            <div class="typing-dot"></div>
                            <div class="typing-dot"></div>
                            <div class="typing-dot"></div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="chat-input-area">
                <div class="input-wrapper">
                    <textarea
                        ref="inputEl"
                        v-model="draft"
                        class="chat-input"
                        placeholder="有问题尽管问"
                        :disabled="sending"
                        @keydown.enter.exact.prevent="sendChat"
                        @input="autoGrow"
                        @select="captureSelection"
                    ></textarea>
                    <div class="input-footer">
                        <div class="input-left">
                            <el-dropdown trigger="click" :disabled="!modelConfigured" @command="insertMention">
                                <button class="footer-btn" type="button">@</button>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item v-for="kb in kbList" :key="kb.id" :command="kb.name">
                                            📚 {{ kb.name }}
                                        </el-dropdown-item>
                                        <el-dropdown-item v-if="!kbList.length" disabled>暂无知识库</el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                            <div class="divider"></div>
                            <el-dropdown trigger="click" :disabled="!modelConfigured" @command="selectModel">
                                <button class="footer-btn" type="button">
                                    <span>{{ currentModel || '默认模型' }}</span>
                                    <span class="chevron">▾</span>
                                </button>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item
                                            v-for="m in modelList"
                                            :key="m.name"
                                            :command="m.name"
                                            :class="{ selected: m.name === currentModel }"
                                        >
                                            {{ m.name }}
                                        </el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                        <div class="input-right">
                            <button class="send-btn" type="button" :disabled="sending || !draft.trim()" @click="sendChat">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                                    <line x1="22" y1="2" x2="11" y2="13"/>
                                    <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                                </svg>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { marked } from 'marked';
import { getToken, redirectToLogin } from '../api/client';

interface SessionItem {
    id: string;
    title: string;
    mode?: string;
    updatedAt?: string;
}

interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
    time?: string;
}

interface KbItem {
    id: number;
    name: string;
}

interface ModelItem {
    name: string;
    modelType?: string;
}

const API = '/api/chat';
const legacyConfigUrl = 'http://localhost:6790/config/ai';

const sessions = ref<SessionItem[]>([]);
const currentSessionId = ref<string | null>(null);
const kbList = ref<KbItem[]>([]);
const modelList = ref<ModelItem[]>([]);
const currentModel = ref('');
const modelConfigured = ref(true);
const messages = ref<ChatMessage[]>([]);
const draft = ref('');
const sending = ref(false);
const streaming = ref(false);
const streamContent = ref('');
const statusText = ref('');

const inputEl = ref<HTMLTextAreaElement | null>(null);
const messagesEl = ref<HTMLElement | null>(null);
let selectionStart = 0;
let reader: ReadableStreamDefaultReader<Uint8Array> | null = null;

const currentTitle = computed(() => {
    const s = sessions.value.find(x => x.id === currentSessionId.value);
    return s ? s.title : '对话';
});

async function authGet<T = any>(url: string, options: RequestInit = {}): Promise<T | null> {
    const resp = await fetch(url, { ...options, headers: { ...authHeaders(), ...(options.headers || {}) } });
    if (resp.status === 401 || resp.status === 403) {
        redirectToLogin();
        return null;
    }
    const d = (await resp.json()) as any;
    if (d && d.ok === false && (d.error === '请先登录' || d.msg === '认证失败')) {
        redirectToLogin();
        return null;
    }
    return d as T;
}

function authHeaders(): Record<string, string> {
    const h: Record<string, string> = {};
    const token = getToken();
    if (token) h['Authorization'] = 'Bearer ' + token;
    return h;
}

async function loadSessions(): Promise<void> {
    const d = await authGet<{ ok: boolean; data: SessionItem[] }>(API + '/sessions');
    if (!d || !d.ok) return;
    sessions.value = d.data || [];
    if (sessions.value.length && !sessions.value.some(s => s.id === currentSessionId.value)) {
        await selectSession(sessions.value[0].id);
    }
}

async function loadKbs(): Promise<void> {
    const d = await authGet<{ ok: boolean; data: KbItem[] }>(API + '/kbs');
    if (!d || !d.ok) return;
    kbList.value = d.data || [];
}

async function loadModels(): Promise<void> {
    const d = await authGet<{ ok: boolean; data: ModelItem[]; defaultModel?: string }>(API + '/models');
    if (!d || !d.ok) return;
    modelList.value = d.data || [];
    modelConfigured.value = modelList.value.length > 0;
    if (modelConfigured.value) {
        currentModel.value = d.defaultModel || modelList.value[0].name || '';
    }
}

async function loadMessages(): Promise<void> {
    if (!currentSessionId.value) return;
    const d = await authGet<{ ok: boolean; messages: ChatMessage[] }>(
        API + '/messages?sessionId=' + encodeURIComponent(currentSessionId.value)
    );
    if (!d || !d.ok) return;
    messages.value = d.messages || [];
    scrollToBottom();
}

async function selectSession(id: string): Promise<void> {
    currentSessionId.value = id;
    await loadMessages();
}

async function createNewSession(): Promise<void> {
    const d = await authGet<{ ok: boolean; id: string }>(API + '/sessions', {
        method: 'POST',
        headers: { ...authHeaders(), 'Content-Type': 'application/x-www-form-urlencoded' },
        body: '',
    });
    if (!d || !d.ok) return;
    await loadSessions();
    currentSessionId.value = d.id;
    messages.value = [];
}

async function deleteSession(id: string): Promise<void> {
    try {
        await ElMessageBox.confirm('确定要删除这个对话吗？', '删除对话', { type: 'warning' });
    } catch {
        return;
    }
    const d = await authGet<{ ok: boolean }>(API + '/sessions/' + encodeURIComponent(id), { method: 'DELETE' });
    if (!d || !d.ok) {
        ElMessage.error('❌ 删除失败');
        return;
    }
    if (currentSessionId.value === id) {
        currentSessionId.value = null;
        messages.value = [];
    }
    await loadSessions();
    if (!sessions.value.length) {
        messages.value = [];
    }
}

async function clearCurrentSession(): Promise<void> {
    if (!currentSessionId.value) return;
    try {
        await ElMessageBox.confirm('确定要清空当前对话记录吗？', '清空对话', { type: 'warning' });
    } catch {
        return;
    }
    const d = await authGet<{ ok: boolean }>(
        API + '/clear?sessionId=' + encodeURIComponent(currentSessionId.value),
        { method: 'DELETE' }
    );
    if (!d || !d.ok) {
        ElMessage.error('❌ 清空失败');
        return;
    }
    await loadSessions();
    if (!sessions.value.length) {
        currentSessionId.value = null;
    }
    messages.value = [];
}

function parseMentionedKb(message: string): KbItem | null {
    const match = message.match(/@(\S+)/);
    if (match) {
        const kbName = match[1];
        for (const kb of kbList.value) {
            if (kb.name === kbName || kb.name.includes(kbName)) {
                return kb;
            }
        }
    }
    return null;
}

function selectModel(modelName: string): void {
    currentModel.value = modelName;
}

function insertMention(name: string): void {    const value = draft.value;
    draft.value = value.slice(0, selectionStart) + '@' + name + ' ' + value.slice(selectionStart);
    selectionStart += name.length + 2;
    nextTick(() => {
        inputEl.value?.focus();
        inputEl.value?.setSelectionRange(selectionStart, selectionStart);
    });
}

function captureSelection(): void {
    selectionStart = inputEl.value?.selectionStart ?? selectionStart;
}

function autoGrow(): void {
    const el = inputEl.value;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 120) + 'px';
}

function addMessage(role: 'user' | 'assistant', content: string, time?: string): void {
    messages.value.push({ role, content, time });
    scrollToBottom();
}

function parseStreamLine(line: string): void {
    if (!line.trim()) return;
    if (line.startsWith('data:')) line = line.substring(5).trim();
    let data: any;
    try {
        data = JSON.parse(line);
    } catch {
        return;
    }
    if (data.type === 'session') {
        if (!currentSessionId.value) {
            currentSessionId.value = data.sessionId;
            loadSessions();
        }
    } else if (data.type === 'text') {
        streaming.value = true;
        statusText.value = '';
        streamContent.value += data.content || '';
        scrollToBottom();
    } else if (data.type === 'status') {
        statusText.value = data.content || '';
        scrollToBottom();
    } else if (data.type === 'done') {
        finishSend();
        loadSessions();
    } else if (data.type === 'error') {
        resetStreaming();
        sending.value = false;
        addMessage('assistant', '❌ ' + (data.content || '未知错误'), new Date().toISOString());
    }
}

function finishSend(): void {
    if (streaming.value && streamContent.value) {
        messages.value.push({
            role: 'assistant',
            content: streamContent.value,
            time: new Date().toISOString(),
        });
    }
    resetStreaming();
    sending.value = false;
    draft.value = '';
    if (inputEl.value) inputEl.value.style.height = 'auto';
    nextTick(() => inputEl.value?.focus());
    scrollToBottom();
}

function resetStreaming(): void {
    streaming.value = false;
    streamContent.value = '';
    statusText.value = '';
}

async function sendChat(): Promise<void> {
    if (sending.value) return;
    const message = draft.value.trim();
    if (!message) return;

    addMessage('user', message, new Date().toISOString());
    draft.value = '';
    if (inputEl.value) inputEl.value.style.height = 'auto';
    sending.value = true;
    streaming.value = false;
    streamContent.value = '';
    statusText.value = '';

    const mentioned = parseMentionedKb(message);
    const form = new FormData();
    form.append('message', message);
    if (mentioned) form.append('kbId', String(mentioned.id));
    if (currentModel.value) form.append('modelName', currentModel.value);
    if (currentSessionId.value) form.append('sessionId', currentSessionId.value);

    try {
        const resp = await fetch(API + '/send', {
            method: 'POST',
            headers: authHeaders(),
            body: form,
        });
        if (resp.status === 401 || resp.status === 403) {
            resetStreaming();
            sending.value = false;
            redirectToLogin();
            return;
        }
        if (!resp.ok || !resp.body) {
            throw new Error('请求失败');
        }
        reader = resp.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        for (;;) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop() ?? '';
            for (const line of lines) {
                parseStreamLine(line);
            }
        }
    } catch (e) {
        resetStreaming();
        sending.value = false;
        addMessage('assistant', '❌ 发送失败: ' + ((e as Error).message || '网络错误'), new Date().toISOString());
    }
}

function formatTime(timeStr: string | undefined): string {
    if (!timeStr) return '';
    const date = new Date(timeStr);
    if (isNaN(date.getTime())) return '';
    const diff = Date.now() - date.getTime();
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
    return date.getMonth() + 1 + '月' + date.getDate() + '日 ' +
        String(date.getHours()).padStart(2, '0') + ':' + String(date.getMinutes()).padStart(2, '0');
}

function renderMd(text: string): string {
    if (!text) return '';
    try {
        return marked.parse(text) as string;
    } catch {
        return String(text);
    }
}

function highlightMentions(text: string): string {
    const escaped = String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
    return escaped.replace(/@(\S+)/g, '<span class="mention-tag">@$1</span>');
}

function scrollToBottom(): void {
    nextTick(() => {
        const el = messagesEl.value;
        if (el) el.scrollTop = el.scrollHeight;
    });
}

onMounted(async () => {
    await Promise.all([loadSessions(), loadKbs(), loadModels()]);
    if (sessions.value.length && !currentSessionId.value) {
        await selectSession(sessions.value[0].id);
    }
});
</script>

<style scoped>
.chat-layout { flex: 1; min-height: 0; display: flex; background: white; overflow: hidden; }
.sessions-sidebar {
    width: 240px; background: var(--bg-sidebar); border-right: 1px solid var(--border);
    display: flex; flex-direction: column; flex-shrink: 0; min-height: 0;
}
.sessions-header { padding: 16px; border-bottom: 1px solid var(--border); }
.sessions-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.sessions-container { flex: 1; overflow-y: auto; padding: 8px 0; min-height: 0; }
.session-item {
    display: flex; align-items: center; gap: 8px; padding: 8px 12px; cursor: pointer;
    color: var(--text-secondary); font-size: 13px; transition: all 0.2s; border-radius: var(--radius-sm);
    margin: 0 4px;
}
.session-item:hover { background: var(--hover); color: var(--text-primary); }
.session-item.active { background: rgba(99, 102, 241, 0.1); color: var(--primary); font-weight: 500; }
.session-icon { flex-shrink: 0; }
.session-title-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-delete { opacity: 0; color: var(--text-muted); transition: all 0.2s; cursor: pointer; }
.session-item:hover .session-delete { opacity: 1; }
.session-delete:hover { color: #ef4444; }

.chat-main { flex: 1; min-height: 0; display: flex; flex-direction: column; background: white; }

.unconfigured-banner {
    padding: 10px 18px; background: #fef3c7; border-bottom: 1px solid #fde68a; color: #92400e;
    font-size: 13px; display: flex; align-items: center; gap: 8px; flex-shrink: 0;
}
.unconfigured-banner a { color: #6366f1; text-decoration: underline; font-weight: 500; }

.chat-toolbar {
    padding: 12px 24px; border-bottom: 1px solid var(--border); flex-shrink: 0;
    display: flex; align-items: center; justify-content: space-between;
}
.chat-toolbar-title { font-size: 15px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chat-toolbar-actions { flex-shrink: 0; }

.chat-messages {
    flex: 1; min-height: 0; overflow-y: auto; padding: 20px 24px;
    display: flex; flex-direction: column; gap: 16px; background: #ffffff;
}

.empty-state { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 60px 20px; color: #94a3b8; }
.empty-icon { font-size: 64px; margin-bottom: 16px; opacity: 0.3; }
.empty-icon.no-model { font-size: 48px; }
.empty-title { font-size: 18px; font-weight: 600; margin-bottom: 8px; color: #1e293b; }
.empty-desc { font-size: 14px; line-height: 1.6; color: #64748b; }

.message { display: flex; gap: 12px; max-width: 100%; animation: fadeIn 0.2s ease-out; margin-bottom: 8px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.message.user { flex-direction: row-reverse; }
.message-avatar {
    width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center;
    justify-content: center; font-size: 12px; flex-shrink: 0;
}
.message.user .message-avatar { background: #5c5c5c; color: white; }
.message.assistant .message-avatar { background: linear-gradient(135deg, #8b5cf6, #6366f1); color: white; }
.message-content-wrapper { display: flex; flex-direction: column; gap: 6px; max-width: 75%; }
.message-header { display: flex; align-items: center; gap: 8px; }
.message-author { font-size: 13px; font-weight: 500; }
.message.user .message-author { color: #64748b; }
.message.assistant .message-author { color: #6366f1; }
.message-content { padding: 14px 18px; border-radius: 16px; line-height: 1.7; font-size: 14px; word-wrap: break-word; }
.message.user .message-content { background: #f1f5f9; color: #1e293b; }
.message.assistant .message-content { background: #ffffff; color: #1e293b; border: 1px solid #e2e8f0; }
.message-time { font-size: 11px; color: #94a3b8; }
.message.user .message-time { text-align: right; }

.status-area {
    display: flex; align-items: center; gap: 6px; padding: 4px 10px;
    background: rgba(99, 102, 241, 0.08); color: #6366f1; border-radius: 12px; font-size: 12px;
    border: 1px solid rgba(99, 102, 241, 0.15); width: fit-content;
}
.typing-indicator { display: flex; align-items: center; gap: 4px; padding: 14px 18px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px; }
.typing-dot { width: 6px; height: 6px; background: #94a3b8; border-radius: 50%; animation: typing 1.4s infinite ease-in-out both; }
.typing-dot:nth-child(2) { animation-delay: -0.32s; }
.typing-dot:nth-child(3) { animation-delay: -0.16s; }
@keyframes typing { 0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; } 40% { transform: scale(1); opacity: 1; } }

.markdown-content { word-break: break-word; }
.markdown-content p { margin-bottom: 8px; }
.markdown-content ul, .markdown-content ol { padding-left: 20px; margin-bottom: 8px; }
.markdown-content li { margin-bottom: 4px; }
.markdown-content code { background: rgba(0,0,0,0.04); padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 13px; }
.markdown-content pre { background: #1e293b; color: #e2e8f0; padding: 12px; border-radius: 8px; overflow-x: auto; margin-bottom: 8px; font-family: monospace; font-size: 13px; }
.markdown-content blockquote { border-left: 3px solid #6366f1; padding-left: 10px; color: #64748b; margin-bottom: 8px; }
.markdown-content strong { font-weight: 600; }
.markdown-content a { color: #6366f1; text-decoration: none; }
.markdown-content a:hover { text-decoration: underline; }

.chat-input-area { flex-shrink: 0; padding: 12px 20px; border-top: 1px solid #e2e8f0; background: #f8fafc; }
.input-wrapper { background: white; border-radius: 16px; border: 1px solid #e2e8f0; overflow: hidden; }
.chat-input {
    width: 100%; padding: 12px 16px; border: none; background: transparent; font-size: 14px;
    outline: none; resize: none; min-height: 40px; max-height: 120px; line-height: 1.5; font-family: inherit;
}
.chat-input::placeholder { color: #94a3b8; }
.chat-input:disabled { background: #f8fafc; cursor: not-allowed; }
.input-footer { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; border-top: 1px solid #f1f5f9; }
.input-left { display: flex; align-items: center; gap: 8px; }
.input-right { display: flex; align-items: center; gap: 4px; }
.footer-btn {
    padding: 6px 12px; border: none; background: transparent; color: #64748b; border-radius: 10px;
    font-size: 13px; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: all 0.2s;
    font-family: inherit;
}
.footer-btn:hover { background: #f1f5f9; color: #1e293b; }
.footer-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.footer-btn .chevron { font-size: 10px; color: #94a3b8; }
.divider { width: 1px; height: 16px; background: #e2e8f0; }
.send-btn {
    width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
    border-radius: 12px; cursor: pointer; background: #6366f1; color: white; transition: all 0.2s; border: none;
}
.send-btn:hover { background: #4f46e5; }
.send-btn:disabled { background: #cbd5e1; cursor: not-allowed; }

.mention-tag { display: inline-block; padding: 1px 6px; background: rgba(99, 102, 241, 0.1); color: #6366f1; font-size: 12px; border-radius: 4px; margin-right: 4px; }
</style>
