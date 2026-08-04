import { defineStore } from 'pinia';
import { ref } from 'vue';
import { apiFetch, getToken, setToken, clearToken, apiError } from '../api/client';

interface SysUser {
    userName?: string;
    nickName?: string;
    avatar?: string;
}

export const useAuthStore = defineStore('auth', () => {
    const token = ref<string | null>(getToken());
    const user = ref<SysUser | null>(null);
    const initialized = ref(false);

    async function login(username: string, password: string): Promise<void> {
        const d = await apiFetch('/api/sys-api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password }),
        });
        if (d.code === 200 || d.token) {
            const t = d.token || (d.data as string);
            token.value = t;
            setToken(t);
        } else {
            throw new Error(apiError(d, '登录失败'));
        }
        await fetchUserInfo();
    }

    async function fetchUserInfo(): Promise<void> {
        const d = await apiFetch('/api/sys-api/getInfo');
        if (d.code === 200 || d.ok) {
            user.value = (d.user as SysUser) || {};
            initialized.value = true;
        } else {
            throw new Error(apiError(d, '获取用户信息失败'));
        }
    }

    function displayName(): string {
        return user.value?.userName || user.value?.nickName || '用户';
    }

    function logout(): void {
        const t = token.value;
        fetch('/api/sys-api/logout', {
            method: 'POST',
            headers: t ? { Authorization: 'Bearer ' + t } : {},
        }).catch(() => {});
        token.value = null;
        user.value = null;
        clearToken();
    }

    return { token, user, initialized, login, fetchUserInfo, displayName, logout };
});
