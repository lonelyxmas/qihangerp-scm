<template>
    <div class="card empty-state">
        <div class="empty-title">「{{ title }}」Vue 版建设中</div>
        <div class="empty-desc" style="margin-bottom: 16px;">该模块已可在原版 Web 中使用，Vue 版将逐步迁移</div>
        <el-button type="primary" @click="openLegacy">打开原版页面</el-button>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const title = computed(() => String(route.meta.title ?? '该模块'));
const legacy = computed(() => String(route.meta.legacy ?? '/'));
const legacyUrl = computed(() => 'http://localhost:6790' + legacy.value);

function openLegacy(): void {
    window.open(legacyUrl.value, '_blank');
}
</script>
