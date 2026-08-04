import { createRouter, createWebHashHistory } from 'vue-router';
import { getToken } from '../api/client';
import { useAuthStore } from '../stores/auth';
import MainLayout from '../layouts/MainLayout.vue';
import LoginView from '../views/LoginView.vue';
import HomeView from '../views/HomeView.vue';
import DataRecordsView from '../views/DataRecordsView.vue';
import TasksView from '../views/TasksView.vue';
import RemindersView from '../views/RemindersView.vue';
import ApprovalsView from '../views/ApprovalsView.vue';
import ActivityView from '../views/ActivityView.vue';
import NotificationsView from '../views/NotificationsView.vue';
import KbView from '../views/KbView.vue';
import ChatView from '../views/ChatView.vue';
import ConfigView from '../views/ConfigView.vue';

export const router = createRouter({
    history: createWebHashHistory(),
    routes: [
        { path: '/login', name: 'login', component: LoginView },
        {
            path: '/',
            component: MainLayout,
            redirect: '/home',
            children: [
                { path: 'home', name: 'home', component: HomeView, meta: { title: '首页' } },
                { path: 'datacenter', redirect: '/config' },
                { path: 'data', name: 'data', component: DataRecordsView, meta: { title: '数据中心' } },
                { path: 'tasks', name: 'tasks', component: TasksView, meta: { title: '任务中心' } },
                { path: 'reminders', name: 'reminders', component: RemindersView, meta: { title: '提醒中心' } },
                { path: 'approvals', name: 'approvals', component: ApprovalsView, meta: { title: '审批中心' } },
                { path: 'activity', name: 'activity', component: ActivityView, meta: { title: '动态流' } },
                { path: 'notifications', name: 'notifications', component: NotificationsView, meta: { title: '通知中心' } },
                { path: 'chat', name: 'chat', component: ChatView, meta: { title: 'AI 对话', flush: true } },
                { path: 'kb', name: 'kb', component: KbView, meta: { title: '知识库', flush: true } },
                { path: 'config', name: 'config', component: ConfigView, meta: { title: '系统配置', flush: true } },
            ],
        },
        { path: '/:pathMatch(.*)*', redirect: '/home' },
    ],
});

router.beforeEach(async (to) => {
    if (to.name !== 'login' && !getToken()) {
        return { name: 'login' };
    }
    if (to.name === 'login' && getToken()) {
        return { name: 'home' };
    }
    if (to.name === 'config') {
        const auth = useAuthStore();
        await auth.ensureUserInfo();
        if (!auth.isAdmin) {
            return { name: 'home' };
        }
    }
    return true;
});
