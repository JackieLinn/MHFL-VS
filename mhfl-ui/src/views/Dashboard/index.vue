<template>
  <div class="dashboard-page">
    <h2 class="page-title">仪表盘</h2>
    <p class="page-desc">欢迎使用 MHFL-VS 平台</p>
    <div class="page-placeholder">
      <p>仪表盘功能开发中...</p>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page {
  padding: 32px;
  height: 100%;
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
  margin-bottom: 32px;
}

.page-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: var(--home-text-muted);
}

.page-placeholder p {
  font-size: 14px;
  color: var(--home-text-muted);
}
</style>
