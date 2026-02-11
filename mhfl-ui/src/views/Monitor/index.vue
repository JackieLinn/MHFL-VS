<template>
  <div class="monitor-page">
    <h2 class="page-title">实时监控</h2>
    <p class="page-desc">实时监控联邦学习训练过程</p>
    <div class="page-placeholder">
      <p>实时监控功能开发中...</p>
    </div>
  </div>
</template>

<style scoped>
.monitor-page {
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
