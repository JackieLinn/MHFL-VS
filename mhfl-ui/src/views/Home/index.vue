<script setup lang="ts">
import HomeHeader from './components/HomeHeader.vue'
import HomeSidebar from './components/HomeSidebar.vue'
import HomeFooter from './components/HomeFooter.vue'
</script>

<template>
  <div class="home-page">
    <!-- 顶部导航栏 -->
    <HomeHeader/>

    <!-- 主内容区域 -->
    <main class="home-main">
      <!-- 左侧导航栏 -->
      <HomeSidebar/>

      <!-- 右侧内容区域 -->
      <div class="content-area">
        <router-view/>
      </div>
    </main>

    <!-- 底部 -->
    <HomeFooter/>
  </div>
</template>

<style scoped>
.home-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--home-bg);
  overflow: hidden;
}

/* ============ 主内容 ============ */
.home-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ============ 右侧内容区域 ============ */
.content-area {
  flex: 1;
  overflow-y: auto;
  background: var(--home-bg);
}

/* ============ 响应式 ============ */
@media (max-width: 768px) {
  .content-area {
    padding: 24px;
  }
}
</style>
