<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import BackToTop from '@/components/BackToTop.vue'
import DashboardRealtimeResource from './components/DashboardRealtimeResource.vue'
import DashboardPlatformOverview from './components/DashboardPlatformOverview.vue'
import DashboardStatCards from './components/DashboardStatCards.vue'
import DashboardCharts from './components/DashboardCharts.vue'
import DashboardRecentTasks from './components/DashboardRecentTasks.vue'
import DashboardSystemHealth from './components/DashboardSystemHealth.vue'
import DashboardQuickActions from './components/DashboardQuickActions.vue'
</script>

<template>
  <div id="dashboard-scroll" class="dashboard-page dashboard-shell p-8 h-full overflow-y-auto">
    <!-- 科技感背景网格 -->
    <div class="tech-grid-bg"></div>

    <PageHeader
        class="mb-6"
        :title="$t('pages.dashboard.title')"
        :desc="$t('pages.dashboard.desc')"
        enter-delay="0.02s"
    />
    <DashboardRealtimeResource/>
    <DashboardPlatformOverview/>
    <DashboardStatCards/>
    <DashboardCharts/>

    <!-- 底部网格 -->
    <div class="dashboard-grid grid gap-4 items-stretch enter-rise" style="--enter-delay: 0.24s">
      <DashboardRecentTasks/>
      <DashboardSystemHealth/>
      <DashboardQuickActions/>
    </div>

    <BackToTop scroll-target="#dashboard-scroll"/>
  </div>
</template>

<style src="./dashboard.css"></style>
