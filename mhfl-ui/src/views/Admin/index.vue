<script setup lang="ts">
import {useI18n} from 'vue-i18n'
import AccountManage from './components/AccountManage.vue'
import DatasetManage from './components/DatasetManage.vue'
import AlgorithmManage from './components/AlgorithmManage.vue'

const {t} = useI18n()
</script>

<template>
  <div class="admin-page">
    <h2 class="page-title">{{ $t('pages.admin.title') }}</h2>
    <p class="page-desc">{{ $t('pages.admin.desc') }}</p>

    <el-tabs type="border-card" class="admin-tabs">
      <el-tab-pane :label="$t('pages.admin.tabMembers')">
        <AccountManage/>
      </el-tab-pane>
      <el-tab-pane :label="$t('pages.admin.tabDatasets')">
        <DatasetManage/>
      </el-tab-pane>
      <el-tab-pane :label="$t('pages.admin.tabAlgorithms')">
        <AlgorithmManage/>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.admin-page {
  padding: 32px 32px 16px 32px;
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--home-text-primary);
  margin-bottom: 8px;
}

.page-desc {
  font-size: 14px;
  color: var(--home-text-muted);
  margin-bottom: 24px;
}

.admin-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--home-card-bg);
  border-color: var(--home-border);
}

.admin-tabs :deep(.el-tabs__header) {
  margin: 0;
  background: var(--home-hover-bg);
  border-bottom: 1px solid var(--home-border);
}

.admin-tabs :deep(.el-tabs__nav-wrap)::after {
  display: none;
}

.admin-tabs :deep(.el-tabs__item) {
  color: var(--home-text-secondary);
}

.admin-tabs :deep(.el-tabs__item.is-active) {
  color: var(--home-text-primary);
  font-weight: 500;
}

.admin-tabs :deep(.el-tabs__ink-bar) {
  background: #6366f1;
}

.admin-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 20px 20px 0 20px;
  display: flex;
  flex-direction: column;
}

/* Tab 内容区（直接子元素）占满高度，便于内部列表滚动 */
.admin-tabs :deep(.el-tabs__content > *) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
