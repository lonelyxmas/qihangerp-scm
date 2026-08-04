import { createRouter, createWebHashHistory } from 'vue-router';
import { getToken } from '../api/client';
import MainLayout from '../layouts/MainLayout.vue';
import LoginView from '../views/LoginView.vue';
import HomeView from '../views/HomeView.vue';
import DataCenterView from '../views/DataCenterView.vue';
import DataRecordsView from '../views/DataRecordsView.vue';
import TasksView from '../views/TasksView.vue';
import RemindersView from '../views/RemindersView.vue';
import PlaceholderView from '../views/PlaceholderView.vue';

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
                { path: 'datacenter', name: 'datacenter', component: DataCenterView, meta: { title: '数据模块管理' } },
                { path: 'data', name: 'data', component: DataRecordsView, meta: { title: '数据中心' } },
                { path: 'tasks', name: 'tasks', component: TasksView, meta: { title: '任务中心' } },
                { path: 'reminders', name: 'reminders', component: RemindersView, meta: { title: '提醒中心' } },
                { path: 'chat', component: PlaceholderView, meta: { title: 'AI 对话', legacy: '/chat' } },
                { path: 'kb', component: PlaceholderView, meta: { title: '知识库', legacy: '/notes' } },
                { path: 'approvals', component: PlaceholderView, meta: { title: '审批中心', legacy: '/approvals' } },
                { path: 'activity', component: PlaceholderView, meta: { title: '动态流', legacy: '/activity' } },
                { path: 'notifications', component: PlaceholderView, meta: { title: '通知中心', legacy: '/notifications' } },
                { path: 'config', component: PlaceholderView, meta: { title: '系统配置', legacy: '/config/ai' } },
            ],
        },
        { path: '/:pathMatch(.*)*', redirect: '/home' },
    ],
});

router.beforeEach((to) => {
    if (to.name !== 'login' && !getToken()) {
        return { name: 'login' };
    }
    if (to.name === 'login' && getToken()) {
        return { name: 'home' };
    }
    return true;
});
