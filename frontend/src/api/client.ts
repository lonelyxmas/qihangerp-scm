export interface ApiResponse<T = any> {
    ok?: boolean;
    code?: number;
    data?: T;
    error?: string;
    msg?: string;
    token?: string;
    reply?: string;
}

export const TOKEN_KEY = 'Admin-Token';

export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
    localStorage.removeItem(TOKEN_KEY);
    document.cookie = 'Admin-Token=; path=/; max-age=0; SameSite=Lax';
}

let authErrorHandled = false;

export function redirectToLogin(): void {
    if (authErrorHandled) return;
    authErrorHandled = true;
    clearToken();
    if (window.location.hash !== '#/login') {
        window.location.hash = '#/login';
    }
    setTimeout(() => {
        authErrorHandled = false;
    }, 1500);
}

export async function apiFetch<T = any>(url: string, options: RequestInit = {}): Promise<T> {
    const headers: Record<string, string> = {
        ...(options.headers as Record<string, string>),
    };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const resp = await fetch(url, { ...options, headers });
    if (resp.status === 401) {
        redirectToLogin();
        throw new Error('unauthorized');
    }
    return (await resp.json()) as T;
}

export function apiError(d: { error?: string; msg?: string } | null, fallback: string): string {
    if (d) {
        if (d.error) return d.error;
        if (d.msg) return d.msg;
    }
    return fallback;
}
