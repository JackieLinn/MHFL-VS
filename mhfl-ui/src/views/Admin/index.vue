<template>
  <div class="admin-page">
    <h2 class="page-title">系统管理</h2>
    <p class="page-desc">管理系统成员、算法和数据集</p>
    <div class="page-placeholder">
      <p>系统管理功能开发中...</p>
    </div>
  </div>
</template>

<style scoped>
.admin-page {
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
